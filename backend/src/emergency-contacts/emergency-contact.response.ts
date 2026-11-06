import type { EmergencyContact } from '../generated/prisma/client.js';

export class EmergencyContactResponse {
  id: number;
  name: string;
  relation: string | null;
  /** E.164, e.g. +84905128846. */
  phone: string;
  receivesSos: boolean;
  createdAt: Date;
  updatedAt: Date;

  static from(row: EmergencyContact): EmergencyContactResponse {
    return {
      id: row.id,
      name: row.name,
      relation: row.relation,
      phone: row.phone,
      receivesSos: row.receivesSos,
      createdAt: row.createdAt,
      updatedAt: row.updatedAt,
    };
  }
}

export class EmergencyContactList {
  items: EmergencyContactResponse[];
  /** Most contacts a user can keep. */
  limit: number;
}
