import { IsNotEmpty, IsString, MaxLength } from 'class-validator';
import { TrimSpaces } from '../common/validation/trim-spaces.js';

export class UpdateProfileDto {
  @TrimSpaces()
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  fullName: string;
}

export class DeleteAccountDto {
  /** The current password, asked again before deleting. */
  @IsString()
  @IsNotEmpty()
  @MaxLength(128)
  password: string;
}
