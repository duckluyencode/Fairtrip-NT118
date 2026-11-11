import { MY_KHE, place } from './planner.fixtures.js';
import { jainIndex, placeValue, satisfaction } from './scoring.js';

const lover = { id: 1, available: 0, interests: ['BEACH' as const] };

describe('scoring', () => {
  it('values matching interests, ratings and votes', () => {
    const plain = place(9, { interests: ['CULTURE'], rating: 4.5 });
    expect(placeValue(lover, MY_KHE)).toBeGreaterThan(placeValue(lover, plain));
    expect(placeValue(lover, { ...plain, votes: [1] })).toBeCloseTo(
      placeValue(lover, plain) + 0.5,
    );
  });

  it("computes Jain's fairness index", () => {
    expect(jainIndex([0.8, 0.8, 0.8])).toBeCloseTo(1);
    expect(jainIndex([1, 0])).toBeCloseTo(0.5);
    expect(jainIndex([])).toBe(1);
  });

  it('measures satisfaction against the member favourites for the plan size', () => {
    const other = place(10, { interests: ['CULTURE'] });
    expect(satisfaction(lover, [MY_KHE], [MY_KHE, other], 1)).toBe(1);
    // Skipping one of two stops halves (at most) what the member gets.
    expect(satisfaction(lover, [MY_KHE], [MY_KHE, other], 2)).toBeLessThan(1);
    expect(satisfaction(lover, [], [MY_KHE], 1)).toBe(0);
  });
});
