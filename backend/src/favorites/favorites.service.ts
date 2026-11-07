import { Injectable } from '@nestjs/common';
import type { PlaceCategory } from '../generated/prisma/enums.js';
import type { PlaceSummary } from '../places/dto/place.response.js';
import { PlacesService } from '../places/places.service.js';
import { PrismaService } from '../prisma/prisma.service.js';

@Injectable()
export class FavoritesService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly places: PlacesService,
  ) {}

  /** Most recently saved first, optionally only one category. */
  async list(
    userId: number,
    category?: PlaceCategory,
  ): Promise<PlaceSummary[]> {
    const favorites = await this.prisma.favorite.findMany({
      where: {
        userId,
        // Google places have no stored categories; they are filtered after fetching.
        ...(category && {
          place: {
            OR: [{ source: 'GOOGLE' }, { categories: { some: { category } } }],
          },
        }),
      },
      include: { place: { include: { categories: true } } },
      orderBy: [{ createdAt: 'desc' }, { placeId: 'desc' }],
    });
    const summaries = await this.places.summaries(
      favorites.map((favorite) => favorite.place),
      () => true,
    );
    return category
      ? summaries.filter((summary) => summary.categories.includes(category))
      : summaries;
  }

  /** Saving twice keeps one favorite. */
  async add(userId: number, placeId: number): Promise<void> {
    await this.places.assertExists(placeId);
    await this.prisma.favorite.createMany({
      data: [{ userId, placeId }],
      skipDuplicates: true,
    });
  }

  /** Removing a place that is not saved is not an error. */
  async remove(userId: number, placeId: number): Promise<void> {
    await this.prisma.favorite.deleteMany({ where: { userId, placeId } });
  }
}
