import type { PlannerMember, PlannerPlace } from './types.js';

/**
 * How much a member would enjoy a place, between 0.2 and 1.5:
 * 70 % interest match, 30 % rating, +0.5 when the member voted for it.
 */
export function placeValue(member: PlannerMember, place: PlannerPlace): number {
  const matched = place.interests.filter((i) =>
    member.interests.includes(i),
  ).length;
  const interest =
    member.interests.length === 0
      ? 0.5
      : matched > 0
        ? 0.6 + 0.4 * Math.min(1, matched / 2)
        : 0.2;
  const rating = (place.rating ?? 3.5) / 5;
  const vote = place.votes.includes(member.id) ? 0.5 : 0;
  return 0.7 * interest + 0.3 * rating + vote;
}

/** Jain's fairness index: (Σx)² / (n·Σx²); 1 when everyone has the same value. */
export function jainIndex(values: number[]): number {
  const sum = values.reduce((a, b) => a + b, 0);
  const squares = values.reduce((a, b) => a + b * b, 0);
  if (values.length === 0 || squares === 0) {
    return 1;
  }
  return (sum * sum) / (values.length * squares);
}

/**
 * Value the member gets from the places they visit, over the value of their favourite
 * `planSize` candidates (planSize = stops in the whole plan). 1 means every stop is one
 * of the member's favourites; skipping stops lowers it.
 */
export function satisfaction(
  member: PlannerMember,
  visited: PlannerPlace[],
  candidates: PlannerPlace[],
  planSize: number,
): number {
  if (visited.length === 0 || planSize === 0) {
    return 0;
  }
  const achieved = visited.reduce((s, p) => s + placeValue(member, p), 0);
  const ideal = candidates
    .map((p) => placeValue(member, p))
    .sort((a, b) => b - a)
    .slice(0, planSize)
    .reduce((a, b) => a + b, 0);
  return ideal === 0 ? 0 : Math.min(1, achieved / ideal);
}
