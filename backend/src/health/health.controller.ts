import { Controller, Get, HttpStatus } from '@nestjs/common';
import { Public } from '../auth/auth.decorators.js';
import { AppError } from '../common/errors/app-error.js';
import { ErrorCode } from '../common/errors/error-code.js';
import { PrismaService } from '../prisma/prisma.service.js';

export interface HealthStatus {
  status: 'ok';
  database: 'up';
}

@Public()
@Controller('health')
export class HealthController {
  constructor(private readonly prisma: PrismaService) {}

  @Get()
  async check(): Promise<HealthStatus> {
    try {
      await this.prisma.$queryRaw`SELECT 1`;
    } catch {
      throw new AppError(
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorCode.DatabaseUnavailable,
        'The database cannot be reached',
      );
    }
    return { status: 'ok', database: 'up' };
  }
}
