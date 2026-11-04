import { CATALOG, validateCatalog } from './catalog.js';

describe('sample catalog', () => {
  it('gives every place a price or a parent with one', () => {
    expect(() => validateCatalog(CATALOG)).not.toThrow();
  });

  it('refuses a place without a price', () => {
    const [first] = CATALOG;
    expect(() =>
      validateCatalog([{ ...first, slug: 'no-price', price: undefined }]),
    ).toThrow(/no-price/);
  });

  it('uses unique slugs', () => {
    const slugs = CATALOG.map((place) => place.slug);
    expect(new Set(slugs).size).toBe(slugs.length);
  });
});
