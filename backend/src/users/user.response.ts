import type { User } from '../generated/prisma/client.js';

export class UserResponse {
  id: number;
  fullName: string;
  email: string | null;
  /** E.164, e.g. +84905128846. */
  phone: string | null;
  avatarUrl: string | null;
  createdAt: Date;

  static from(user: User): UserResponse {
    return {
      id: user.id,
      fullName: user.fullName,
      email: user.email,
      phone: user.phone,
      avatarUrl: user.avatarUrl,
      createdAt: user.createdAt,
    };
  }
}
