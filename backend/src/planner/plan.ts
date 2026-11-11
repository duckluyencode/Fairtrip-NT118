import {
  cheapestInsertion,
  scheduleDay,
  type DaySettings,
} from './schedule.js';
import { jainIndex, placeValue, satisfaction } from './scoring.js';
import type {
  PlannedStop,
  PlannerInput,
  PlannerMember,
  PlannerOptions,
  PlannerPlace,
  PlannerResult,
} from './types.js';

const FAIRNESS_ROUNDS = 20;
const SWAP_ROUNDS = 50;
const EPSILON = 1e-9;

/** Working state: places per day in visiting order, and who joins each place. */
class Plan {
  readonly days: PlannerPlace[][];
  readonly participants = new Map<number, Set<number>>();
  private readonly settings: DaySettings;

  constructor(readonly input: PlannerInput) {
    this.days = Array.from({ length: input.days }, () => []);
    this.settings = input;
  }

  get selected(): PlannerPlace[] {
    return this.days.flat();
  }

  isSelected(place: PlannerPlace): boolean {
    return this.participants.has(place.id);
  }

  /** What the member pays for a place, after ticket coverage by a visited parent area. */
  costFor(member: PlannerMember, place: PlannerPlace): number {
    if (
      place.coveredBy !== null &&
      this.participants.get(place.coveredBy)?.has(member.id)
    ) {
      return 0;
    }
    return place.cost;
  }

  spent(member: PlannerMember): number {
    return this.selected
      .filter((p) => this.participants.get(p.id)?.has(member.id))
      .reduce((sum, p) => sum + this.costFor(member, p), 0);
  }

  /**
   * How much the member's spending changes if they join the place: its price, minus the
   * tickets they already paid for places inside it, which its ticket now covers.
   */
  joinCost(member: PlannerMember, place: PlannerPlace): number {
    let delta = this.costFor(member, place);
    for (const inner of this.selected) {
      if (
        inner.coveredBy === place.id &&
        this.participants.get(inner.id)?.has(member.id)
      ) {
        delta -= inner.cost;
      }
    }
    return delta;
  }

  canAfford(member: PlannerMember, place: PlannerPlace): boolean {
    return (
      this.spent(member) + this.joinCost(member, place) <= member.available
    );
  }

  /** Members who would join a new place: everyone who can afford it. */
  joiners(place: PlannerPlace): PlannerMember[] {
    return this.input.members.filter((m) => this.canAfford(m, place));
  }

  insertion(place: PlannerPlace) {
    return cheapestInsertion(this.days, place, this.settings);
  }

  add(
    place: PlannerPlace,
    day: number,
    position: number,
    members: PlannerMember[],
  ) {
    this.days[day].splice(position, 0, place);
    this.participants.set(place.id, new Set(members.map((m) => m.id)));
  }

  satisfactionOf(member: PlannerMember): number {
    const visited = this.selected.filter((p) =>
      this.participants.get(p.id)?.has(member.id),
    );
    return satisfaction(
      member,
      visited,
      this.input.places,
      this.selected.length,
    );
  }

  settingsForDays(): DaySettings {
    return this.settings;
  }

  remove(place: PlannerPlace, day: number) {
    this.days[day].splice(this.days[day].indexOf(place), 1);
    this.participants.delete(place.id);
  }

  goes(member: PlannerMember, place: PlannerPlace): boolean {
    return this.participants.get(place.id)?.has(member.id) ?? false;
  }

  /** Sum over members of the value of the places they go to. */
  totalValue(): number {
    return this.input.members.reduce(
      (sum, member) =>
        sum +
        this.selected
          .filter((p) => this.goes(member, p))
          .reduce((s, p) => s + placeValue(member, p), 0),
      0,
    );
  }

  withinBudgets(): boolean {
    return this.input.members.every((m) => this.spent(m) <= m.available);
  }
}

/**
 * Greedy construction: repeatedly add the place with the best value-to-cost ratio
 * for the members who can afford it; free places come first. Each place goes to the
 * day and position that lengthen the plan the least.
 */
function greedy(plan: Plan): void {
  for (;;) {
    let best: {
      place: PlannerPlace;
      members: PlannerMember[];
      day: number;
      position: number;
      free: boolean;
      ratio: number;
      value: number;
      added: number;
    } | null = null;
    for (const place of plan.input.places) {
      if (plan.isSelected(place)) continue;
      const members = plan.joiners(place);
      if (members.length === 0) continue;
      const insertion = plan.insertion(place);
      if (!insertion) continue;
      const value = members.reduce((s, m) => s + placeValue(m, place), 0);
      const cost = members.reduce((s, m) => s + plan.joinCost(m, place), 0);
      const candidate = {
        place,
        members,
        day: insertion.day,
        position: insertion.position,
        free: cost <= 0,
        // Cost-free places cannot divide by zero: they rank above every paid one.
        ratio: cost <= 0 ? Number.POSITIVE_INFINITY : value / cost,
        value,
        added: insertion.addedMinutes,
      };
      if (!best || isBetter(candidate, best)) {
        best = candidate;
      }
    }
    if (!best) return;
    plan.add(best.place, best.day, best.position, best.members);
  }
}

function isBetter(
  a: { free: boolean; ratio: number; value: number; added: number },
  b: { free: boolean; ratio: number; value: number; added: number },
): boolean {
  if (a.free !== b.free) return a.free;
  if (!a.free && a.ratio !== b.ratio) return a.ratio > b.ratio;
  if (a.value !== b.value) return a.value > b.value;
  return a.added < b.added;
}

/** 2-opt within each day: reverse a run of visits when it makes the day end earlier. */
function improveRoutes(plan: Plan): void {
  const settings = plan.settingsForDays();
  for (const order of plan.days) {
    let improved = true;
    while (improved) {
      improved = false;
      const current = scheduleDay(order, settings);
      if (!current) break;
      for (let i = 0; i < order.length - 1 && !improved; i++) {
        for (let j = i + 1; j < order.length && !improved; j++) {
          const candidate = [
            ...order.slice(0, i),
            ...order.slice(i, j + 1).reverse(),
            ...order.slice(j + 1),
          ];
          const timed = scheduleDay(candidate, settings);
          if (timed && timed.finishMinute < current.finishMinute) {
            order.splice(0, order.length, ...candidate);
            improved = true;
          }
        }
      }
    }
  }
}

/**
 * Local search, 1-swap: replace a planned place with one that is not planned when the
 * group's total value goes up and every budget still holds. Greedy picks by value per
 * cost one place at a time, so an early cheap pick can block a better use of the money
 * or the time; this undoes such picks.
 */
function swapPlaces(plan: Plan): void {
  for (let round = 0; round < SWAP_ROUNDS; round++) {
    const before = plan.totalValue();
    let improved = false;
    for (const [day, order] of plan.days.entries()) {
      // A copy: the day changes while a swap is tried.
      for (const out of order.slice()) {
        const position = order.indexOf(out);
        const going = plan.participants.get(out.id) ?? new Set<number>();
        plan.remove(out, day);
        for (const candidate of plan.input.places) {
          if (candidate === out || plan.isSelected(candidate)) continue;
          const members = plan.joiners(candidate);
          const insertion =
            members.length > 0 ? plan.insertion(candidate) : null;
          if (!insertion) continue;
          plan.add(candidate, insertion.day, insertion.position, members);
          if (plan.withinBudgets() && plan.totalValue() > before + EPSILON) {
            improved = true;
            break;
          }
          plan.remove(candidate, insertion.day);
        }
        if (improved) break;
        // No better place: put this one back where it was.
        plan.days[day].splice(position, 0, out);
        plan.participants.set(out.id, going);
      }
      if (improved) break;
    }
    if (!improved) return;
  }
}

/** Members join every planned stop they can afford (a ticket bought may cover it). */
function joinAffordable(plan: Plan): void {
  let joined = true;
  while (joined) {
    joined = false;
    for (const place of plan.selected) {
      const going = plan.participants.get(place.id);
      if (!going) continue;
      for (const member of plan.input.members) {
        if (!going.has(member.id) && plan.canAfford(member, place)) {
          going.add(member.id);
          joined = true;
        }
      }
    }
  }
}

/**
 * A member leaves a stop they like less to afford one they like more, starting with the
 * least satisfied member. Costs are per person, so nobody else's spending changes. A
 * stop is never left empty (the others still go there).
 */
function swapParticipation(plan: Plan): void {
  for (
    let round = 0;
    round < FAIRNESS_ROUNDS * plan.input.members.length;
    round++
  ) {
    let improved = false;
    const members = [...plan.input.members].sort(
      (a, b) => plan.satisfactionOf(a) - plan.satisfactionOf(b),
    );
    for (const member of members) {
      const value = (p: PlannerPlace) => placeValue(member, p);
      const joined = plan.selected
        .filter((p) => plan.goes(member, p))
        .sort((a, b) => value(a) - value(b));
      const others = plan.selected
        .filter((p) => !plan.goes(member, p))
        .sort((a, b) => value(b) - value(a));
      for (const into of others) {
        for (const out of joined) {
          if (value(out) >= value(into) - EPSILON) break;
          const outGoing = plan.participants.get(out.id);
          const intoGoing = plan.participants.get(into.id);
          if (!outGoing || !intoGoing || outGoing.size <= 1) continue;
          outGoing.delete(member.id);
          intoGoing.add(member.id);
          if (plan.spent(member) <= member.available) {
            improved = true;
            break;
          }
          outGoing.add(member.id);
          intoGoing.delete(member.id);
        }
        if (improved) break;
      }
      if (improved) break;
    }
    if (!improved) return;
  }
}

/**
 * Fairness check. Budgets are a hard limit throughout. Members join the stops they can
 * now afford, then reallocate their own money towards the stops they like (least
 * satisfied first), and finally the least satisfied member gets the best extra place
 * they can afford, as long as that raises the lowest satisfaction.
 */
function fairness(plan: Plan): void {
  joinAffordable(plan);
  swapParticipation(plan);
  joinAffordable(plan);

  for (let round = 0; round < FAIRNESS_ROUNDS; round++) {
    const lowest = lowestMember(plan);
    if (!lowest) return;
    const floor = plan.satisfactionOf(lowest);
    let best: {
      place: PlannerPlace;
      day: number;
      position: number;
      value: number;
    } | null = null;
    for (const place of plan.input.places) {
      if (plan.isSelected(place) || !plan.canAfford(lowest, place)) continue;
      const insertion = plan.insertion(place);
      if (!insertion) continue;
      const value = placeValue(lowest, place);
      if (!best || value > best.value) {
        best = {
          place,
          day: insertion.day,
          position: insertion.position,
          value,
        };
      }
    }
    if (!best) return;
    plan.add(best.place, best.day, best.position, plan.joiners(best.place));
    const newLowest = lowestMember(plan);
    if (!newLowest || plan.satisfactionOf(newLowest) <= floor) {
      // No gain for the worst-off member: undo and stop.
      plan.remove(best.place, best.day);
      return;
    }
  }
}

function lowestMember(plan: Plan): PlannerMember | undefined {
  return [...plan.input.members].sort(
    (a, b) => plan.satisfactionOf(a) - plan.satisfactionOf(b),
  )[0];
}

function toResult(plan: Plan): PlannerResult {
  const settings = plan.settingsForDays();
  const stops: PlannedStop[] = [];
  for (const [index, order] of plan.days.entries()) {
    const timed = scheduleDay(order, settings);
    if (!timed) {
      throw new Error(`Day ${index + 1} no longer fits; this is a planner bug`);
    }
    for (const [position, visit] of timed.visits.entries()) {
      const going = [...(plan.participants.get(visit.place.id) ?? [])].sort(
        (a, b) => a - b,
      );
      const costByMember: Record<number, number> = {};
      for (const member of plan.input.members) {
        if (going.includes(member.id)) {
          costByMember[member.id] = plan.costFor(member, visit.place);
        }
      }
      stops.push({
        placeId: visit.place.id,
        day: index + 1,
        position: position + 1,
        startMinute: visit.startMinute,
        travelMinutes: visit.travelMinutes,
        costByMember,
        participants: going,
      });
    }
  }
  const members = plan.input.members.map((member) => ({
    memberId: member.id,
    spent: plan.spent(member),
    satisfaction: plan.satisfactionOf(member),
  }));
  for (const member of plan.input.members) {
    if (plan.spent(member) > member.available) {
      throw new Error(
        `Member ${member.id} is over budget; this is a planner bug`,
      );
    }
  }
  const values = members.map((m) => m.satisfaction);
  return {
    stops,
    members,
    totalValue: plan.totalValue(),
    totalSatisfaction: values.reduce((a, b) => a + b, 0),
    fairness: jainIndex(values),
  };
}

/**
 * Plans a trip for one or more members: Greedy, then Local search (2-opt on each day's
 * route, place swaps, and greedy passes for the time and money they free), then the
 * Fairness check. A solo trip is a group of one. Every member stays within their
 * available money and every day fits its time window and the places' opening hours.
 */
export function planTrip(
  input: PlannerInput,
  options: PlannerOptions = {},
): PlannerResult {
  const plan = new Plan(input);
  if (input.days > 0) {
    greedy(plan);
    if (options.localSearch ?? true) {
      improveRoutes(plan);
      greedy(plan);
      swapPlaces(plan);
      improveRoutes(plan);
      greedy(plan);
    }
    if (options.fairness ?? true) {
      fairness(plan);
    }
  }
  return toResult(plan);
}
