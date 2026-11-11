import type { User } from '../generated/prisma/client.js';

export class UserResponse {
  id: number;
  fullName: string;
  email: string | null;
  /** E.164, e.g. +84905128846. */
  phone: string | null;
  avatarUrl: string | null;
  /** False for an account that only signs in with Google. */
  hasPassword: boolean;
  /** True once the account has signed in with Google. */
  googleLinked: boolean;
  createdAt: Date;

  static from(user: User): UserResponse {
    return {
      id: user.id,
      fullName: user.fullName,
      email: user.email,
      phone: user.phone,
      avatarUrl: user.avatarUrl,
      hasPassword: user.passwordHash !== null,
      googleLinked: user.googleSub !== null,
      createdAt: user.createdAt,
    };
  }
}
