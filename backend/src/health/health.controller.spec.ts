import { ServiceUnavailableException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { PrismaService } from '../prisma/prisma.service.js';
import { HealthController } from './health.controller.js';

describe('HealthController', () => {
  let controller: HealthController;
  const prisma = { $queryRaw: vi.fn() };

  beforeEach(async () => {
    prisma.$queryRaw.mockReset();
    const module: TestingModule = await Test.createTestingModule({
      controllers: [HealthController],
      providers: [{ provide: PrismaService, useValue: prisma }],
    }).compile();

    controller = module.get(HealthController);
  });

  it('reports the database as up when the query succeeds', async () => {
    prisma.$queryRaw.mockResolvedValue([{ 1: 1 }]);

    await expect(controller.check()).resolves.toEqual({
      status: 'ok',
      database: 'up',
    });
  });

  it('returns 503 when the database cannot be reached', async () => {
    prisma.$queryRaw.mockRejectedValue(new Error('connect ECONNREFUSED'));

    await expect(controller.check()).rejects.toBeInstanceOf(
      ServiceUnavailableException,
    );
  });
});
