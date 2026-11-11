import {
  buildMessage,
  IsBoolean,
  IsNotEmpty,
  IsOptional,
  IsString,
  MaxLength,
  ValidateBy,
  type ValidationOptions,
} from 'class-validator';
import { parseContact } from '../auth/contact.js';
import { TrimSpaces } from '../common/validation/trim-spaces.js';

const E164 = /^\+[1-9]\d{6,14}$/;

/**
 * A phone number in E.164: Vietnamese numbers may be typed the local way
 * ("0905 128 846"), foreign ones need their country code ("+65 8123 4567").
 */
export function parsePhone(input: string): string | null {
  const vietnamese = parseContact(input);
  if (vietnamese?.kind === 'phone') {
    return vietnamese.value;
  }
  const compact = input.trim().replace(/[\s.\-()]/g, '');
  return E164.test(compact) && !compact.startsWith('+84') ? compact : null;
}

function IsPhone(options?: ValidationOptions): PropertyDecorator {
  return ValidateBy(
    {
      name: 'isPhone',
      validator: {
        validate: (value) =>
          typeof value === 'string' && parsePhone(value) !== null,
        defaultMessage: buildMessage(
          (each) =>
            `${each}$property must be a Vietnamese mobile number or a +country number`,
          options,
        ),
      },
    },
    options,
  );
}

export class CreateEmergencyContactDto {
  /** e.g. "Nguyễn Thị Lan" */
  @TrimSpaces()
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  name: string;

  /** e.g. "Mẹ" */
  @IsOptional()
  @TrimSpaces()
  @IsString()
  @MaxLength(50)
  relation?: string;

  /** "0905128846" or "+84905128846"; stored as E.164. */
  @IsPhone()
  phone: string;

  /** Whether the SOS message goes to this person; default true. */
  @IsOptional()
  @IsBoolean()
  receivesSos?: boolean;
}

export class UpdateEmergencyContactDto {
  @IsOptional()
  @TrimSpaces()
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  name?: string;

  /** Send null to clear it. */
  @IsOptional()
  @TrimSpaces()
  @IsString()
  @MaxLength(50)
  relation?: string | null;

  @IsOptional()
  @IsPhone()
  phone?: string;

  @IsOptional()
  @IsBoolean()
  receivesSos?: boolean;
}
