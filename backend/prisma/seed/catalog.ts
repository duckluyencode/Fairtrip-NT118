import type { PrismaClient } from '../../src/generated/prisma/client.js';
import type {
  Interest,
  PlaceCategory,
  PriceSource,
} from '../../src/generated/prisma/enums.js';
import { searchTextOf } from '../../src/places/text.js';

// Sample catalog for development. Coordinates are approximate; ratings, rating counts
// and prices are sample values, not fetched from Google or partners. The Android app's
// sample screens use the same places (Cầu Vàng, Mì Quảng Bà Mua, Mỹ Khê, Chợ đêm Sơn Trà).

export const CATALOG_CITY = 'Đà Nẵng';

interface CatalogPlace {
  slug: string;
  name: string;
  area: string;
  category: PlaceCategory;
  /** Other categories besides the main one. */
  alsoIn?: PlaceCategory[];
  lat: number;
  lng: number;
  rating: number;
  ratingCount: number;
  /** "HH:mm-HH:mm", or "all-day". */
  hours: string;
  visitMinutes: number;
  /** VND per person. Omitted when the parent's ticket covers the place. */
  price?: {
    min: number;
    max: number;
    source: PriceSource;
    provider: string;
  };
  /** Slug of the ticketed area this place is inside. */
  parent?: string;
  description: string;
}

const free = {
  min: 0,
  max: 0,
  source: 'ESTIMATED',
  provider: 'TripFair',
} as const;

export const CATALOG: CatalogPlace[] = [
  {
    slug: 'ba-na-hills',
    name: 'Bà Nà Hills',
    area: 'Núi Chúa · Hòa Vang',
    category: 'FUN',
    alsoIn: ['SCENIC'],
    lat: 15.9977,
    lng: 107.9881,
    rating: 4.6,
    ratingCount: 30500,
    hours: '07:00-22:00',
    visitMinutes: 360,
    price: {
      min: 900000,
      max: 900000,
      source: 'PARTNER_API',
      provider: 'Klook',
    },
    description:
      'Khu du lịch trên đỉnh núi Chúa với cáp treo, làng Pháp và khu vui chơi Fantasy Park. Vé gồm cáp treo khứ hồi và các điểm trong khu.',
  },
  {
    slug: 'golden-bridge',
    name: 'Cầu Vàng',
    area: 'Bà Nà Hills · Hòa Vang',
    category: 'SCENIC',
    alsoIn: ['FUN'],
    lat: 15.9952,
    lng: 107.9966,
    rating: 4.8,
    ratingCount: 12450,
    hours: '07:00-22:00',
    visitMinutes: 60,
    parent: 'ba-na-hills',
    description:
      'Cây cầu giữa mây nổi tiếng với đôi bàn tay khổng lồ, nằm trong khu Bà Nà Hills. Hợp ngắm cảnh và chụp ảnh cùng nhóm.',
  },
  {
    slug: 'le-jardin-damour',
    name: "Vườn hoa Le Jardin d'Amour",
    area: 'Bà Nà Hills · Hòa Vang',
    category: 'SCENIC',
    lat: 15.9985,
    lng: 107.9903,
    rating: 4.6,
    ratingCount: 2100,
    hours: '07:00-22:00',
    visitMinutes: 45,
    parent: 'ba-na-hills',
    description:
      'Chuỗi vườn hoa theo phong cách Pháp trong khu Bà Nà Hills, có hầm rượu và nhiều góc chụp ảnh.',
  },
  {
    slug: 'mi-quang-ba-mua',
    name: 'Mì Quảng Bà Mua',
    area: 'Trần Bình Trọng · Hải Châu',
    category: 'FOOD',
    lat: 16.0679,
    lng: 108.2186,
    rating: 4.6,
    ratingCount: 3210,
    hours: '06:00-21:00',
    visitMinutes: 45,
    price: { min: 40000, max: 60000, source: 'GOOGLE_API', provider: 'Google' },
    description:
      'Quán mì Quảng lâu năm, tô đầy đặn với tôm, thịt và bánh tráng giòn. Hợp cho bữa trưa nhanh của cả nhóm.',
  },
  {
    slug: 'my-khe-beach',
    name: 'Bãi biển Mỹ Khê',
    area: 'Phước Mỹ · Sơn Trà',
    category: 'SCENIC',
    lat: 16.0544,
    lng: 108.2469,
    rating: 4.7,
    ratingCount: 8930,
    hours: 'all-day',
    visitMinutes: 120,
    price: free,
    description:
      'Bãi biển cát trắng dài, nước trong và sóng êm. Hợp tắm biển buổi sáng và ngắm hoàng hôn.',
  },
  {
    slug: 'son-tra-night-market',
    name: 'Chợ đêm Sơn Trà',
    area: 'Mai Hắc Đế · Sơn Trà',
    category: 'FUN',
    alsoIn: ['FOOD'],
    lat: 16.0614,
    lng: 108.2311,
    rating: 4.5,
    ratingCount: 5120,
    hours: '18:00-23:30',
    visitMinutes: 120,
    price: {
      min: 150000,
      max: 250000,
      source: 'ESTIMATED',
      provider: 'TripFair',
    },
    description:
      'Khu chợ đêm cạnh cầu Rồng với nhiều quầy hải sản và đồ lưu niệm. Hợp dạo chơi buổi tối.',
  },
  {
    slug: 'dragon-bridge',
    name: 'Cầu Rồng',
    area: 'Sông Hàn · Sơn Trà',
    category: 'SCENIC',
    alsoIn: ['FUN'],
    lat: 16.0612,
    lng: 108.2272,
    rating: 4.7,
    ratingCount: 20100,
    hours: 'all-day',
    visitMinutes: 45,
    price: free,
    description:
      'Cây cầu hình rồng bắc qua sông Hàn, phun lửa và nước vào tối thứ Bảy, Chủ nhật.',
  },
  {
    slug: 'love-bridge',
    name: 'Cầu Tình Yêu',
    area: 'Trần Hưng Đạo · Sơn Trà',
    category: 'SCENIC',
    lat: 16.0628,
    lng: 108.2297,
    rating: 4.5,
    ratingCount: 3400,
    hours: 'all-day',
    visitMinutes: 30,
    price: free,
    description:
      'Cây cầu đi bộ ven sông Hàn với tượng cá chép hóa rồng, sáng đèn về đêm.',
  },
  {
    slug: 'marble-mountains',
    name: 'Ngũ Hành Sơn',
    area: 'Hòa Hải · Ngũ Hành Sơn',
    category: 'SCENIC',
    alsoIn: ['FUN'],
    lat: 16.0034,
    lng: 108.2637,
    rating: 4.6,
    ratingCount: 15800,
    hours: '07:00-17:30',
    visitMinutes: 150,
    price: {
      min: 40000,
      max: 40000,
      source: 'ESTIMATED',
      provider: 'TripFair',
    },
    description:
      'Cụm núi đá vôi với hang động, chùa cổ và đường lên đỉnh Thủy Sơn nhìn ra biển.',
  },
  {
    slug: 'linh-ung-pagoda',
    name: 'Chùa Linh Ứng Sơn Trà',
    area: 'Bán đảo Sơn Trà',
    category: 'SCENIC',
    lat: 16.1003,
    lng: 108.2779,
    rating: 4.7,
    ratingCount: 9800,
    hours: '06:00-21:00',
    visitMinutes: 60,
    price: free,
    description:
      'Ngôi chùa trên bán đảo Sơn Trà với tượng Quan Âm cao nhìn ra vịnh Đà Nẵng.',
  },
  {
    slug: 'cham-museum',
    name: 'Bảo tàng Điêu khắc Chăm',
    area: '2 Tháng 9 · Hải Châu',
    category: 'FUN',
    lat: 16.0603,
    lng: 108.2233,
    rating: 4.5,
    ratingCount: 4300,
    hours: '07:30-17:00',
    visitMinutes: 90,
    price: {
      min: 60000,
      max: 60000,
      source: 'ESTIMATED',
      provider: 'TripFair',
    },
    description:
      'Bảo tàng trưng bày hiện vật điêu khắc Chăm Pa bằng sa thạch, gần sông Hàn.',
  },
  {
    slug: 'han-market',
    name: 'Chợ Hàn',
    area: 'Trần Phú · Hải Châu',
    category: 'FOOD',
    alsoIn: ['FUN'],
    lat: 16.0682,
    lng: 108.2242,
    rating: 4.2,
    ratingCount: 9100,
    hours: '06:00-19:00',
    visitMinutes: 60,
    price: {
      min: 30000,
      max: 100000,
      source: 'ESTIMATED',
      provider: 'TripFair',
    },
    description:
      'Chợ trung tâm với khu ăn vặt, đặc sản khô và vải may đo lấy nhanh.',
  },
  {
    slug: 'asia-park',
    name: 'Sun World Asia Park',
    area: 'Phan Đăng Lưu · Hải Châu',
    category: 'FUN',
    lat: 16.0393,
    lng: 108.2268,
    rating: 4.4,
    ratingCount: 11200,
    hours: '15:00-22:00',
    visitMinutes: 180,
    price: {
      min: 200000,
      max: 300000,
      source: 'PARTNER_API',
      provider: 'Klook',
    },
    description:
      'Công viên giải trí với vòng quay Sun Wheel và các trò chơi cảm giác mạnh, mở buổi chiều tối.',
  },
  {
    slug: 'banh-xeo-ba-duong',
    name: 'Bánh xèo Bà Dưỡng',
    area: 'Hoàng Diệu · Hải Châu',
    category: 'FOOD',
    lat: 16.0545,
    lng: 108.2121,
    rating: 4.4,
    ratingCount: 6500,
    hours: '09:00-21:30',
    visitMinutes: 45,
    price: {
      min: 50000,
      max: 100000,
      source: 'GOOGLE_API',
      provider: 'Google',
    },
    description:
      'Quán bánh xèo trong hẻm Hoàng Diệu, ăn kèm nem lụi và nước chấm gan heo.',
  },
  {
    slug: 'non-nuoc-beach',
    name: 'Bãi biển Non Nước',
    area: 'Hòa Hải · Ngũ Hành Sơn',
    category: 'SCENIC',
    lat: 16.0004,
    lng: 108.2725,
    rating: 4.6,
    ratingCount: 2900,
    hours: 'all-day',
    visitMinutes: 120,
    price: free,
    description:
      'Bãi biển yên tĩnh dưới chân Ngũ Hành Sơn, ít đông hơn Mỹ Khê.',
  },
];

/** Preferences each place suits, used by the trip planner to score it for each member. */
export const INTERESTS_BY_SLUG: Record<string, Interest[]> = {
  'ba-na-hills': ['FUN', 'SCENERY', 'PHOTO', 'EXPLORE'],
  'golden-bridge': ['SCENERY', 'PHOTO'],
  'le-jardin-damour': ['SCENERY', 'PHOTO', 'RELAX'],
  'mi-quang-ba-mua': ['FOOD'],
  'my-khe-beach': ['BEACH', 'RELAX', 'PHOTO'],
  'son-tra-night-market': ['FOOD', 'FUN', 'EXPLORE'],
  'dragon-bridge': ['SCENERY', 'PHOTO', 'FUN'],
  'love-bridge': ['SCENERY', 'PHOTO', 'RELAX'],
  'marble-mountains': ['SCENERY', 'CULTURE', 'EXPLORE'],
  'linh-ung-pagoda': ['CULTURE', 'SCENERY', 'RELAX'],
  'cham-museum': ['CULTURE', 'EXPLORE'],
  'han-market': ['FOOD', 'EXPLORE'],
  'asia-park': ['FUN', 'PHOTO'],
  'banh-xeo-ba-duong': ['FOOD'],
  'non-nuoc-beach': ['BEACH', 'RELAX'],
};

function toMinutes(hhmm: string): number {
  const [h, m] = hhmm.split(':').map(Number);
  return h * 60 + m;
}

function hoursOf(hours: string): {
  openMinute: number | null;
  closeMinute: number | null;
} {
  if (hours === 'all-day') {
    return { openMinute: null, closeMinute: null };
  }
  const [open, close] = hours.split('-');
  return { openMinute: toMinutes(open), closeMinute: toMinutes(close) };
}

/** Every place must have interests and a price, or be inside a parent that has a price. */
export function validateCatalog(places: CatalogPlace[]): void {
  const bySlug = new Map(places.map((p) => [p.slug, p]));
  for (const place of places) {
    if (!INTERESTS_BY_SLUG[place.slug]?.length) {
      throw new Error(`Catalog place ${place.slug} needs interests`);
    }
    if (place.price) continue;
    const parent = place.parent ? bySlug.get(place.parent) : undefined;
    if (!parent?.price) {
      throw new Error(
        `Catalog place ${place.slug} needs a price or a parent with a price`,
      );
    }
  }
}

/** Inserts or updates the catalog by slug; safe to run again. Returns the number of places. */
export async function seedCatalog(prisma: PrismaClient): Promise<number> {
  validateCatalog(CATALOG);
  const ids = new Map<string, number>();
  // Parents first, so children can point at them.
  const ordered = [...CATALOG].sort(
    (a, b) => Number(Boolean(a.parent)) - Number(Boolean(b.parent)),
  );
  for (const place of ordered) {
    const data = {
      source: 'CATALOG' as const,
      name: place.name,
      area: place.area,
      city: CATALOG_CITY,
      category: place.category,
      lat: place.lat,
      lng: place.lng,
      rating: place.rating,
      ratingCount: place.ratingCount,
      ...hoursOf(place.hours),
      description: place.description,
      visitMinutes: place.visitMinutes,
      priceMin: place.price?.min ?? null,
      priceMax: place.price?.max ?? null,
      priceSource: place.price?.source ?? null,
      priceProvider: place.price?.provider ?? null,
      priceUpdatedAt: place.price ? new Date() : null,
      parentId: place.parent ? (ids.get(place.parent) ?? null) : null,
      searchText: searchTextOf({
        name: place.name,
        area: place.area,
        city: CATALOG_CITY,
      }),
    };
    const saved = await prisma.place.upsert({
      where: { slug: place.slug },
      create: { slug: place.slug, ...data },
      update: data,
    });
    ids.set(place.slug, saved.id);
    const categories = [place.category, ...(place.alsoIn ?? [])];
    await prisma.placeCategoryLink.deleteMany({ where: { placeId: saved.id } });
    await prisma.placeCategoryLink.createMany({
      data: categories.map((category) => ({ placeId: saved.id, category })),
    });
    await prisma.placeInterest.deleteMany({ where: { placeId: saved.id } });
    await prisma.placeInterest.createMany({
      data: INTERESTS_BY_SLUG[place.slug].map((interest) => ({
        placeId: saved.id,
        interest,
      })),
    });
  }
  return CATALOG.length;
}
