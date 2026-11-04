import {
  buildMessage,
  ValidateBy,
  type ValidationOptions,
} from 'class-validator';

export interface Contact {
  kind: 'email' | 'phone';
  /** Lowercased email, or phone in E.164 (+84…). */
  value: string;
}

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
/** Vietnamese mobile number: 10 digits starting with 0. */
const LOCAL_PHONE = /^0\d{9}$/;

/**
 * Same rules as AuthValidation in the Android app, so "Abc@Gmail.com " and
 * "abc@gmail.com", or "+84 901 234 567" and "0901234567", find the same account.
 */
export function parseContact(input: string): Contact | null {
  const trimmed = input.trim();
  if (trimmed.includes('@')) {
    return EMAIL.test(trimmed) && trimmed.length <= 254
      ? { kind: 'email', value: trimmed.toLowerCase() }
      : null;
  }
  const compact = trimmed.replace(/[\s.\-()]/g, '');
  const local = compact.startsWith('+84') ? `0${compact.slice(3)}` : compact;
  return LOCAL_PHONE.test(local)
    ? { kind: 'phone', value: `+84${local.slice(1)}` }
    : null;
}

/** Prisma `where` that finds the user owning this contact. */
export function contactWhere(contact: Contact) {
  return contact.kind === 'email'
    ? { email: contact.value }
    : { phone: contact.value };
}

/** DTO rule: an email or a Vietnamese mobile number. */
export function IsContact(options?: ValidationOptions): PropertyDecorator {
  return ValidateBy(
    {
      name: 'isContact',
      validator: {
        validate: (value) =>
          typeof value === 'string' && parseContact(value) !== null,
        defaultMessage: buildMessage(
          (each) =>
            `${each}$property must be an email or a Vietnamese mobile number`,
          options,
        ),
      },
    },
    options,
  );
}
