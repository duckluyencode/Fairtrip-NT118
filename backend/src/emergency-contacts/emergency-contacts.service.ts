import { HttpStatus, Injectable } from '@nestjs/common';
import { AppError } from '../common/errors/app-error.js';
import { ErrorCode } from '../common/errors/error-code.js';
import { Prisma } from '../generated/prisma/client.js';
import { PrismaService } from '../prisma/prisma.service.js';
import {
  parsePhone,
  type CreateEmergencyContactDto,
  type UpdateEmergencyContactDto,
} from './emergency-contact.dto.js';
import {
  EmergencyContactResponse,
  type EmergencyContactList,
} from './emergency-contact.response.js';

/** An SOS goes out as one SMS per contact, so the list is kept short. */
export const EMERGENCY_CONTACT_LIMIT = 5;

function notFound(): AppError {
  return new AppError(
    HttpStatus.NOT_FOUND,
    ErrorCode.ContactNotFound,
    'No such emergency contact',
  );
}

function duplicate(): AppError {
  return new AppError(
    HttpStatus.CONFLICT,
    ErrorCode.ContactExists,
    'This number is already an emergency contact',
  );
}

const isDuplicate = (error: unknown) =>
  error instanceof Prisma.PrismaClientKnownRequestError &&
  error.code === 'P2002';

/**
 * The user's emergency contacts ("Liên hệ khẩn cấp"). The Android app sends the SOS SMS
 * itself (SmsManager, works without internet); the server keeps the list so it survives
 * a new phone, and the app keeps a copy for offline use.
 */
@Injectable()
export class EmergencyContactsService {
  constructor(private readonly prisma: PrismaService) {}

  async list(userId: number): Promise<EmergencyContactList> {
    const rows = await this.prisma.emergencyContact.findMany({
      where: { userId },
      orderBy: [{ createdAt: 'asc' }, { id: 'asc' }],
    });
    return {
      items: rows.map((row) => EmergencyContactResponse.from(row)),
      limit: EMERGENCY_CONTACT_LIMIT,
    };
  }

  async create(
    userId: number,
    dto: CreateEmergencyContactDto,
  ): Promise<EmergencyContactResponse> {
    const count = await this.prisma.emergencyContact.count({
      where: { userId },
    });
    if (count >= EMERGENCY_CONTACT_LIMIT) {
      throw new AppError(
        HttpStatus.CONFLICT,
        ErrorCode.ContactLimitReached,
        `At most ${EMERGENCY_CONTACT_LIMIT} emergency contacts`,
      );
    }
    try {
      const row = await this.prisma.emergencyContact.create({
        data: {
          userId,
          name: dto.name,
          relation: dto.relation || null,
          phone: parsePhone(dto.phone) ?? dto.phone,
          receivesSos: dto.receivesSos ?? true,
        },
      });
      return EmergencyContactResponse.from(row);
    } catch (error) {
      throw isDuplicate(error) ? duplicate() : error;
    }
  }

  async update(
    userId: number,
    id: number,
    dto: UpdateEmergencyContactDto,
  ): Promise<EmergencyContactResponse> {
    await this.mine(userId, id);
    try {
      const row = await this.prisma.emergencyContact.update({
        where: { id },
        data: {
          name: dto.name,
          relation:
            dto.relation === undefined ? undefined : dto.relation || null,
          phone:
            dto.phone === undefined
              ? undefined
              : (parsePhone(dto.phone) ?? dto.phone),
          receivesSos: dto.receivesSos,
        },
      });
      return EmergencyContactResponse.from(row);
    } catch (error) {
      throw isDuplicate(error) ? duplicate() : error;
    }
  }

  async remove(userId: number, id: number): Promise<void> {
    await this.mine(userId, id);
    await this.prisma.emergencyContact.delete({ where: { id } });
  }

  /** Someone else's contact is reported as missing, so ids cannot be probed. */
  private async mine(userId: number, id: number) {
    const row = await this.prisma.emergencyContact.findUnique({
      where: { id },
    });
    if (!row || row.userId !== userId) {
      throw notFound();
    }
    return row;
  }
}
