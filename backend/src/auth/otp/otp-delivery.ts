import type { OtpPurpose } from '../../generated/prisma/enums.js';
import type { Contact } from '../contact.js';

export interface OtpMessage {
  contact: Contact;
  code: string;
  purpose: OtpPurpose;
  /** Minutes the code stays valid, shown in the message. */
  validMinutes: number;
}

/** Sends a code to its contact. Injected as a token so tests can replace it. */
export abstract class OtpDelivery {
  abstract send(message: OtpMessage): Promise<void>;
}
