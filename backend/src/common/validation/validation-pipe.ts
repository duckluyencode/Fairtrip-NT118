import { HttpStatus, ValidationError, ValidationPipe } from '@nestjs/common';
import { AppError } from '../errors/app-error.js';
import { ErrorCode } from '../errors/error-code.js';

export interface FieldError {
  field: string;
  messages: string[];
}

/** Flattens nested class-validator errors into `{ field: "a.b", messages }` entries. */
export function toFieldErrors(
  errors: ValidationError[],
  parent = '',
): FieldError[] {
  return errors.flatMap((error) => {
    const field = parent ? `${parent}.${error.property}` : error.property;
    const own = error.constraints
      ? [{ field, messages: Object.values(error.constraints) }]
      : [];
    return [...own, ...toFieldErrors(error.children ?? [], field)];
  });
}

/**
 * Validates and converts every DTO. Unknown fields are rejected so typos in a request
 * fail loudly instead of being ignored.
 */
export function createValidationPipe(): ValidationPipe {
  return new ValidationPipe({
    whitelist: true,
    forbidNonWhitelisted: true,
    transform: true,
    exceptionFactory: (errors) =>
      new AppError(
        HttpStatus.BAD_REQUEST,
        ErrorCode.ValidationFailed,
        'Request validation failed',
        toFieldErrors(errors),
      ),
  });
}
