import { HttpException, HttpStatus } from '@nestjs/common';

/** An expected failure with a stable `code` the client can act on. */
export class AppError extends HttpException {
  constructor(
    status: HttpStatus,
    readonly code: string,
    message: string,
    readonly details?: unknown,
  ) {
    super(message, status);
  }
}
