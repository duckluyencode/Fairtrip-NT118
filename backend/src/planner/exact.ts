import { scheduleDay } from './schedule.js';
import { jainIndex, placeValue } from './scoring.js';
import type {
  PlannedStop,
  PlannerInput,
  PlannerMember,
  PlannerPlace,
  PlannerResult,
} from './types.js';

/** Above this the search takes too long; the exact plan is for small benchmark instances. */
export const EXACT_MAX_PLACES = 12;
const EPSILON = 1e-9;

const bitsOf = (mask: number) => {
  const bits: number[] = [];
  for (let i = 0; mask >> i; i++) if ((mask >> i) & 1) bits.push(i);
  return bits;
};

function* permutations<T>(items: T[]): Generator<T[]> {
  if (items.length <= 1) {
    yield items;
    return;
  }
  for (const [i, first] of items.entries()) {
    const rest = [...items.slice(0, i), ...items.slice(i + 1)];
    for (const tail of permutations(rest)) yield [first, ...tail];
  }
}

/**
 * The visiting order that ends the day earliest for every set of places that fits in one
 * day (closed days aside), keyed by the set as a bit mask. Sets are tried from small to
 * large and a set is skipped when one of its subsets already fails: removing a visit
 * never makes a day end later, so a superset of a failing set fails too.
 */
function dayOrders(input: PlannerInput): Map<number, PlannerPlace[]> {
  const n = input.places.length;
  const orders = new Map<number, PlannerPlace[]>([[0, []]]);
  const masks = Array.from({ length: (1 << n) - 1 }, (_, i) => i + 1).sort(
    (a, b) => bitsOf(a).length - bitsOf(b).length || a - b,
  );
  for (const mask of masks) {
    const bits = bitsOf(mask);
    if (bits.some((bit) => !orders.has(mask & ~(1 << bit)))) continue;
    let best: { order: PlannerPlace[]; finish: number } | null = null;
    for (const order of permutations(bits.map((b) => input.places[b]))) {
      const timed = scheduleDay(order, input);
      if (timed && (!best || timed.finishMinute < best.finish)) {
        best = { order, finish: timed.finishMinute };
      }
    }
    if (best) orders.set(mask, best.order);
  }
  return orders;
}

/** Every set of places that can be spread over the trip's days, with one way to do it. */
function tripSplits(
  input: PlannerInput,
  orders: Map<number, PlannerPlace[]>,
): Map<number, number[]> {
  let reachable = new Map<number, number[]>([[0, []]]);
  for (let day = 1; day <= input.days; day++) {
    const closed = input.places.reduce(
      (mask, p, i) => (p.closedDays?.includes(day) ? mask | (1 << i) : mask),
      0,
    );
    const next = new Map<number, number[]>();
    for (const [used, split] of reachable) {
      for (const dayMask of orders.keys()) {
        if (dayMask & used || dayMask & closed) continue;
        const all = used | dayMask;
        if (!next.has(all)) next.set(all, [...split, dayMask]);
      }
    }
    reachable = next;
  }
  return reachable;
}

interface MemberChoice {
  /** The member's visits, as a mask of place indexes. */
  visits: number;
  spent: number;
  /** Sum of placeValue over the visits. */
  value: number;
}

interface Winner {
  set: number;
  split: number[];
  choices: MemberChoice[];
  satisfaction: number[];
  total: number;
  fairness: number;
}

/**
 * Exact plan for small instances, used to measure how far the heuristic is from the
 * optimum (instead of an ILP solver, so the benchmark needs no Python). It maximizes the
 * objective an ILP would: the total value of every member's visits. It tries every set
 * of places that fits the days and, for each set, every member's best affordable subset
 * (a knapsack solved by enumeration). Ties go to the fairer plan, then to fewer stops.
 * Same rules as planTrip: hard budgets, time window, opening hours, ticket coverage.
 */
export function solveExact(input: PlannerInput): PlannerResult {
  const { places, members } = input;
  if (places.length > EXACT_MAX_PLACES) {
    throw new Error(`solveExact handles at most ${EXACT_MAX_PLACES} places`);
  }
  const index = new Map(places.map((p, i) => [p.id, i]));
  const values = members.map((m) => places.map((p) => placeValue(m, p)));
  // ideal[m][k]: the member's k favourite candidates together (satisfaction's denominator).
  const ideal = values.map((row) =>
    [...row]
      .sort((a, b) => b - a)
      .reduce<number[]>((sums, v) => [...sums, sums[sums.length - 1] + v], [0]),
  );
  const isCovered = (place: PlannerPlace, visits: number) =>
    place.coveredBy !== null &&
    ((visits >> (index.get(place.coveredBy) ?? -1)) & 1) === 1;

  const best = (m: number, member: PlannerMember, set: number) => {
    let choice: MemberChoice = { visits: 0, spent: 0, value: 0 };
    // Every subset of the set, the empty one last.
    for (let visits = set; ; visits = (visits - 1) & set) {
      let spent = 0;
      let value = 0;
      for (const i of bitsOf(visits)) {
        spent += isCovered(places[i], visits) ? 0 : places[i].cost;
        value += values[m][i];
      }
      const better =
        value > choice.value + EPSILON ||
        (Math.abs(value - choice.value) <= EPSILON && spent < choice.spent);
      if (spent <= member.available && better) {
        choice = { visits, spent, value };
      }
      if (visits === 0) break;
    }
    return choice;
  };

  // Same definition as satisfaction() in scoring.ts.
  const satisfactionOf = (m: number, choice: MemberChoice, size: number) => {
    const denominator = ideal[m][Math.min(size, places.length)];
    return choice.visits === 0 || denominator === 0
      ? 0
      : Math.min(1, choice.value / denominator);
  };

  const orders =
    input.days > 0 ? dayOrders(input) : new Map<number, PlannerPlace[]>();
  const splits =
    input.days > 0 ? tripSplits(input, orders) : new Map([[0, [] as number[]]]);
  let winner: Winner | null = null;
  for (const [set, split] of splits) {
    const choices = members.map((member, m) => best(m, member, set));
    // Sets with a stop nobody goes to are skipped: the same set without it is tried too.
    const visited = choices.reduce((all, c) => all | c.visits, 0);
    if (visited !== set) continue;
    const size = bitsOf(set).length;
    const total = choices.reduce((s, c) => s + c.value, 0);
    const satisfaction = choices.map((c, m) => satisfactionOf(m, c, size));
    const fairness = jainIndex(satisfaction);
    const wins =
      !winner ||
      total > winner.total + EPSILON ||
      (Math.abs(total - winner.total) <= EPSILON &&
        (fairness > winner.fairness + EPSILON ||
          (Math.abs(fairness - winner.fairness) <= EPSILON &&
            size < bitsOf(winner.set).length)));
    if (wins) {
      winner = { set, split, choices, satisfaction, total, fairness };
    }
  }
  return toResult(input, winner, orders, isCovered);
}

function toResult(
  input: PlannerInput,
  winner: Winner | null,
  orders: Map<number, PlannerPlace[]>,
  isCovered: (place: PlannerPlace, visits: number) => boolean,
): PlannerResult {
  if (!winner) {
    return {
      stops: [],
      members: input.members.map((m) => ({
        memberId: m.id,
        spent: 0,
        satisfaction: 0,
      })),
      totalValue: 0,
      totalSatisfaction: 0,
      fairness: 1,
    };
  }
  const index = new Map(input.places.map((p, i) => [p.id, i]));
  const stops: PlannedStop[] = [];
  winner.split.forEach((dayMask, d) => {
    const timed = scheduleDay(orders.get(dayMask) ?? [], input);
    timed?.visits.forEach((visit, position) => {
      const i = index.get(visit.place.id) ?? -1;
      const costByMember: Record<number, number> = {};
      const participants: number[] = [];
      input.members.forEach((member, m) => {
        const visits = winner.choices[m].visits;
        if ((visits >> i) & 1) {
          participants.push(member.id);
          costByMember[member.id] = isCovered(visit.place, visits)
            ? 0
            : visit.place.cost;
        }
      });
      stops.push({
        placeId: visit.place.id,
        day: d + 1,
        position: position + 1,
        startMinute: visit.startMinute,
        travelMinutes: visit.travelMinutes,
        costByMember,
        participants: participants.sort((a, b) => a - b),
      });
    });
  });
  return {
    stops,
    members: input.members.map((m, i) => ({
      memberId: m.id,
      spent: winner.choices[i].spent,
      satisfaction: winner.satisfaction[i],
    })),
    totalValue: winner.total,
    totalSatisfaction: winner.satisfaction.reduce((a, b) => a + b, 0),
    fairness: winner.fairness,
  };
}
