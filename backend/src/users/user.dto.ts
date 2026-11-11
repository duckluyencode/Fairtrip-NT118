import { IsNotEmpty, IsString, MaxLength, ValidateIf } from 'class-validator';
import { TrimSpaces } from '../common/validation/trim-spaces.js';

export class UpdateProfileDto {
  @TrimSpaces()
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  fullName: string;
}

/** Proof that the account's owner is asking: the password, or a new Google sign-in. */
export class DeleteAccountDto {
  /** The current password, asked again before deleting. */
  @ValidateIf((dto: DeleteAccountDto) => dto.googleIdToken === undefined)
  @IsString()
  @IsNotEmpty()
  @MaxLength(128)
  password?: string;

  /** For an account without a password: a fresh ID token of its Google account. */
  @ValidateIf((dto: DeleteAccountDto) => dto.password === undefined)
  @IsString()
  @IsNotEmpty()
  @MaxLength(4096)
  googleIdToken?: string;
}
