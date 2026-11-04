import { IsNotEmpty, IsString, MaxLength } from 'class-validator';
import { TrimSpaces } from '../common/validation/trim-spaces.js';

export class UpdateProfileDto {
  @TrimSpaces()
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  fullName: string;
}
