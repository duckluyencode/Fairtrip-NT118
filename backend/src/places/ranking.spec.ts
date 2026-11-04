import { matchRank, rankByQuery } from './ranking.js';

const place = (name: string, rating: number, ratingCount = 100) => ({
  name,
  rating,
  ratingCount,
});

describe('matchRank', () => {
  it.each([
    ['cau vang', 0],
    ['cau', 1],
    ['vang', 2],
    ['ang', 3],
    ['ba na', 4],
    ['da nang', 5],
  ])('ranks query %j against Cầu Vàng as %i', (query, rank) => {
    expect(matchRank(query, 'Cầu Vàng', 'Bà Nà Hills · Hòa Vang')).toBe(rank);
  });
});

describe('rankByQuery', () => {
  it('puts better matches first, then higher ratings', () => {
    const ranked = rankByQuery('cau', [
      place('Vườn Cầu', 4.9),
      place('Cầu Rồng', 4.7),
      place('Bà Nà Hills', 5),
      place('Cầu Vàng', 4.8),
    ]);
    expect(ranked.map((p) => p.name)).toEqual([
      'Cầu Vàng',
      'Cầu Rồng',
      'Vườn Cầu',
      'Bà Nà Hills',
    ]);
  });

  it('breaks rating ties with the number of ratings', () => {
    const ranked = rankByQuery('cho', [
      place('Chợ Hàn', 4.5, 100),
      place('Chợ đêm Sơn Trà', 4.5, 5000),
    ]);
    expect(ranked[0].name).toBe('Chợ đêm Sơn Trà');
  });
});
