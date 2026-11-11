// Algorithm evaluation (roadmap step A11): compares pure Greedy, Greedy + Local search +
// Fairness (what the app uses) and the exact optimum on small random instances.
//
//   npm run bench:planner                 # prints Markdown tables
//   npm run bench:planner -- --seed 7     # another set of random instances
//
// Instances are generated from a seed, so the same seed always gives the same numbers.
import { solveExact } from '../src/planner/exact.js';
import { planTrip } from '../src/planner/plan.js';
import {
  INTERESTS,
  type Interest,
  type PlannerInput,
  type PlannerMember,
  type PlannerPlace,
  type PlannerResult,
} from '../src/planner/types.js';

/** Small deterministic PRNG (mulberry32), so runs are reproducible. */
function random(seed: number) {
  let state = seed >>> 0;
  const next = () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let t = state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
  return {
    next,
    pick: <T>(items: readonly T[]): T =>
      items[Math.floor(next() * items.length)],
    between: (min: number, max: number) => min + next() * (max - min),
  };
}
type Random = ReturnType<typeof random>;

function interests(rng: Random): Interest[] {
  const count = 1 + Math.floor(rng.next() * 3);
  return [...new Set(Array.from({ length: count }, () => rng.pick(INTERESTS)))];
}

// Prices in VND per person, like the app's data: many free places, a few big tickets.
const COSTS = [0, 0, 0, 30_000, 50_000, 100_000, 150_000, 300_000, 600_000];
const VISITS = [30, 45, 60, 90, 120, 180];
const MEAL_COSTS = [30_000, 50_000, 100_000, 150_000];
const BUDGETS = [0, 150_000, 300_000, 600_000, 1_200_000];
const CENTER = { lat: 16.05, lng: 108.22 }; // Đà Nẵng

function instance(
  rng: Random,
  placeCount: number,
  memberCount: number,
  days: number,
): PlannerInput {
  const members: PlannerMember[] = Array.from(
    { length: memberCount },
    (_, i) => ({
      id: i + 1,
      available: rng.pick(BUDGETS) * days,
      interests: interests(rng),
    }),
  );
  const places: PlannerPlace[] = [];
  for (let i = 0; i < placeCount; i++) {
    const hours = rng.next() < 0.3;
    // Sometimes a place inside a ticketed area: its price is the area's ticket.
    const parent =
      rng.next() < 0.15
        ? places.find((p) => p.cost >= 300_000 && p.coveredBy === null)
        : undefined;
    // A quarter of the places are restaurants, planned at lunch or dinner time.
    const meal = !parent && rng.next() < 0.25;
    const placeInterests = interests(rng);
    places.push({
      id: i + 1,
      lat: CENTER.lat + rng.between(-0.06, 0.06),
      lng: CENTER.lng + rng.between(-0.06, 0.06),
      visitMinutes: meal ? 60 : rng.pick(VISITS),
      cost: parent ? parent.cost : rng.pick(meal ? MEAL_COSTS : COSTS),
      coveredBy: parent ? parent.id : null,
      interests: meal
        ? [...new Set<Interest>(['FOOD', ...placeInterests])]
        : placeInterests,
      meal,
      rating: Math.round(rng.between(3.5, 5) * 10) / 10,
      openMinute: hours ? Math.round(rng.between(420, 540)) : null,
      closeMinute: hours ? Math.round(rng.between(1020, 1320)) : null,
      votes:
        memberCount > 1
          ? members.filter(() => rng.next() < 0.2).map((m) => m.id)
          : [],
    });
  }
  return {
    members,
    places,
    days,
    dayStartMinute: 480,
    dayEndMinute: 1260,
    gapMinutes: 30,
    base: CENTER,
  };
}

interface Metrics {
  totalValue: number;
  satisfaction: number;
  fairness: number;
  /** Share of each member's money left unused (0 = budget fully used). */
  budgetDeviation: number;
  overBudget: number;
  stops: number;
  ms: number;
}

function measure(
  trip: PlannerInput,
  solve: (trip: PlannerInput) => PlannerResult,
  repeat: number,
): Metrics {
  let result = solve(trip);
  const start = performance.now();
  for (let i = 0; i < repeat; i++) result = solve(trip);
  const ms = (performance.now() - start) / repeat;
  const deviation = trip.members.map((m) => {
    const spent = result.members.find((r) => r.memberId === m.id)?.spent ?? 0;
    return m.available === 0 ? 0 : Math.abs(m.available - spent) / m.available;
  });
  return {
    totalValue: result.totalValue,
    satisfaction: result.totalSatisfaction / trip.members.length,
    fairness: result.fairness,
    budgetDeviation: deviation.reduce((a, b) => a + b, 0) / deviation.length,
    overBudget: trip.members.filter(
      (m) =>
        (result.members.find((r) => r.memberId === m.id)?.spent ?? 0) >
        m.available,
    ).length,
    stops: result.stops.length,
    ms,
  };
}

const ALGORITHMS = [
  {
    name: 'Greedy thuần',
    solve: (t: PlannerInput) =>
      planTrip(t, { localSearch: false, fairness: false }),
    repeat: 20,
  },
  {
    name: 'Greedy + Local search + Fairness',
    solve: (t: PlannerInput) => planTrip(t),
    repeat: 20,
  },
  { name: 'Tối ưu (vét cạn)', solve: solveExact, repeat: 1 },
];

const SCENARIOS = [
  { name: '6 điểm, 1 người, 1 ngày', places: 6, members: 1, days: 1 },
  { name: '8 điểm, 3 người, 1 ngày', places: 8, members: 3, days: 1 },
  { name: '10 điểm, 4 người, 2 ngày', places: 10, members: 4, days: 2 },
];
const INSTANCES = 20;

const average = (values: number[]) =>
  values.reduce((a, b) => a + b, 0) / values.length;
const fixed = (value: number, digits = 2) => value.toFixed(digits);

function main() {
  const seedArg = process.argv.indexOf('--seed');
  const seed = seedArg > 0 ? Number(process.argv[seedArg + 1]) : 2026;
  console.log(
    `# Đánh giá thuật toán lập lộ trình\n\nSeed ${seed}, ${INSTANCES} bộ dữ liệu ngẫu nhiên mỗi kịch bản. ` +
      'Tổng giá trị = tổng placeValue các điểm mỗi người đi (mục tiêu của ILP); ' +
      '% tối ưu so với lời giải vét cạn.\n',
  );
  for (const scenario of SCENARIOS) {
    const rng = random(seed + scenario.places * 100 + scenario.members);
    const trips = Array.from({ length: INSTANCES }, () =>
      instance(rng, scenario.places, scenario.members, scenario.days),
    );
    const results = ALGORITHMS.map((algorithm) =>
      trips.map((trip) => measure(trip, algorithm.solve, algorithm.repeat)),
    );
    const optimum = results[results.length - 1];
    console.log(`## ${scenario.name}\n`);
    console.log(
      '| Thuật toán | Tổng giá trị | % tối ưu | Hài lòng TB | Jain | Lệch ngân sách | Vượt ngân sách | Số điểm | Thời gian (ms) |',
    );
    console.log('| --- | --- | --- | --- | --- | --- | --- | --- | --- |');
    ALGORITHMS.forEach((algorithm, a) => {
      const rows = results[a];
      const ratio = average(
        rows.map((r, i) =>
          optimum[i].totalValue === 0
            ? 1
            : r.totalValue / optimum[i].totalValue,
        ),
      );
      console.log(
        `| ${algorithm.name} | ${fixed(average(rows.map((r) => r.totalValue)))} | ${fixed(ratio * 100, 1)}% | ` +
          `${fixed(average(rows.map((r) => r.satisfaction)) * 100, 1)}% | ${fixed(average(rows.map((r) => r.fairness)), 3)} | ` +
          `${fixed(average(rows.map((r) => r.budgetDeviation)) * 100, 1)}% | ${rows.reduce((s, r) => s + r.overBudget, 0)} | ` +
          `${fixed(average(rows.map((r) => r.stops)), 1)} | ${fixed(average(rows.map((r) => r.ms)), 3)} |`,
      );
    });
    console.log('');
  }
}

main();
