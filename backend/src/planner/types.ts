// Inputs and outputs of the trip planner. The planner is plain TypeScript (no NestJS,
// no database) so it can be unit-tested and benchmarked on its own.

export const INTERESTS = [
  'FOOD',
  'BEACH',
  'SCENERY',
  'RELAX',
  'FUN',
  'PHOTO',
  'CULTURE',
  'EXPLORE',
] as const;
export type Interest = (typeof INTERESTS)[number];

export interface LatLng {
  lat: number;
  lng: number;
}

export interface PlannerMember {
  id: number;
  /** Money for places: the member's budget minus their share of the room, VND. */
  available: number;
  interests: Interest[];
}

export interface PlannerPlace extends LatLng {
  id: number;
  visitMinutes: number;
  /** VND per person (the top of the price range); 0 when free. */
  cost: number;
  /** Ticketed area that includes this place: free for members who also visit it. */
  coveredBy: number | null;
  interests: Interest[];
  /** 0–5, null when unknown. */
  rating: number | null;
  /** Minutes after midnight; both null means open all day. */
  openMinute: number | null;
  closeMinute: number | null;
  /**
   * Hours of each trip day (index 0 = day 1), overriding openMinute/closeMinute on days a
   * schedule is built for; null means open all day (or unknown).
   */
  dayHours?: ({ open: number; close: number } | null)[];
  /** 1-based trip days on which the place is closed (e.g. museums on Mondays). */
  closedDays?: number[];
  /**
   * A place to have a meal (a restaurant): visited only at lunch or dinner time, and at
   * most one per meal of a day. Cafés and snacks are not meals.
   */
  meal?: boolean;
  /** Members who voted for the place (group trips). */
  votes: number[];
}

export interface PlannerInput {
  members: PlannerMember[];
  places: PlannerPlace[];
  days: number;
  dayStartMinute: number;
  dayEndMinute: number;
  /** Minimum pause between two visits ("thời gian giữa các lần đi chơi"). */
  gapMinutes: number;
  /** Where every day starts and ends (the hotel), or null. */
  base: LatLng | null;
}

export interface PlannedStop {
  placeId: number;
  /** 1-based. */
  day: number;
  /** 1-based order within the day. */
  position: number;
  startMinute: number;
  /** Travel from the previous stop (or the base). */
  travelMinutes: number;
  /** VND per participant after ticket coverage, per member id. */
  costByMember: Record<number, number>;
  participants: number[];
}

export interface MemberResult {
  memberId: number;
  /** Spent on places, VND. Never above the member's available money. */
  spent: number;
  /** 0–1: achieved value over the member's best possible value for the same number of stops. */
  satisfaction: number;
}

export interface PlannerResult {
  stops: PlannedStop[];
  members: MemberResult[];
  /** Sum over members of the value of the places they visit (the objective of an ILP). */
  totalValue: number;
  /** Sum of the members' satisfaction (each 0–1). */
  totalSatisfaction: number;
  /** Jain's fairness index of the members' satisfaction, 1 = perfectly even. */
  fairness: number;
}

export interface PlannerOptions {
  /** Run 2-opt and re-insertion after the greedy pass (default true). */
  localSearch?: boolean;
  /** Run the fairness pass (default true). */
  fairness?: boolean;
}
