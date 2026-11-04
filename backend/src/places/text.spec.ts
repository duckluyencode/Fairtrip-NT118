import { normalizeText, searchTextOf } from './text.js';

describe('normalizeText', () => {
  it.each([
    ['Đà Nẵng', 'da nang'],
    ['  Cầu   Vàng! ', 'cau vang'],
    ["Le Jardin d'Amour", 'le jardin d amour'],
    ['BÁNH XÈO bà Dưỡng', 'banh xeo ba duong'],
    ['Phở 24h', 'pho 24h'],
    ['%_', ''],
  ])('turns %j into %j', (input, expected) => {
    expect(normalizeText(input)).toBe(expected);
  });

  it('gives the same text for composed and decomposed input', () => {
    expect(normalizeText('Mỹ Khê'.normalize('NFD'))).toBe(
      normalizeText('Mỹ Khê'.normalize('NFC')),
    );
  });
});

describe('searchTextOf', () => {
  it('joins name, area and city', () => {
    expect(
      searchTextOf({
        name: 'Cầu Vàng',
        area: 'Bà Nà Hills · Hòa Vang',
        city: 'Đà Nẵng',
      }),
    ).toBe('cau vang ba na hills hoa vang da nang');
  });
});
