import {
  IsIn,
  IsNotEmpty,
  IsString,
  Matches,
  MaxLength,
} from 'class-validator';
import { TrimSpaces } from '../../common/validation/trim-spaces.js';
import { OtpPurpose } from '../../generated/prisma/enums.js';
import { IsContact } from '../contact.js';
import { IsPassword } from '../password.js';

export class RegisterDto {
  @TrimSpaces()
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  fullName: string;

  /** Email or Vietnamese mobile number, e.g. 0905128846. */
  @IsContact()
  contact: string;

  /** At least 8 characters with letters and digits. */
  @IsPassword()
  password: string;
}

export class VerifyCodeDto {
  @IsContact()
  contact: string;

  /** The 6-digit code that was sent. */
  @Matches(/^\d{6}$/, { message: 'code must be 6 digits' })
  code: string;
}

export class ResendCodeDto {
  @IsContact()
  contact: string;

  @IsIn(Object.values(OtpPurpose))
  purpose: OtpPurpose;
}

export class LoginDto {
  @IsContact()
  contact: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(128)
  password: string;
}

export class ForgotPasswordDto {
  @IsContact()
  contact: string;
}

export class ResetPasswordDto {
  /** Token returned by POST /auth/password/verify. */
  @IsString()
  @IsNotEmpty()
  resetToken: string;

  /** At least 8 characters with letters and digits. */
  @IsPassword()
  password: string;
}

export class RefreshTokenDto {
  @IsString()
  @IsNotEmpty()
  refreshToken: string;
}
