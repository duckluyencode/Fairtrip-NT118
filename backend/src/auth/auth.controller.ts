import { Body, Controller, HttpCode, HttpStatus, Post } from '@nestjs/common';
import { ApiTags } from '@nestjs/swagger';
import { Throttle } from '@nestjs/throttler';
import { Public } from './auth.decorators.js';
import { AuthService } from './auth.service.js';
import {
  ForgotPasswordDto,
  LoginDto,
  RefreshTokenDto,
  RegisterDto,
  ResendCodeDto,
  ResetPasswordDto,
  VerifyCodeDto,
} from './dto/auth.dto.js';
import {
  CodeSentResponse,
  ResetTokenResponse,
  SessionResponse,
  TokensResponse,
} from './dto/auth.response.js';

@ApiTags('auth')
@Public()
// Stricter than the global limit: these routes send codes or check passwords.
@Throttle({ default: { limit: 10, ttl: 60_000 } })
@Controller('auth')
export class AuthController {
  constructor(private readonly auth: AuthService) {}

  /** Sends a sign-up code; the account can sign in once the code is verified. */
  @Post('register')
  @HttpCode(HttpStatus.ACCEPTED)
  register(@Body() dto: RegisterDto): Promise<CodeSentResponse> {
    return this.auth.register(dto.fullName, dto.contact, dto.password);
  }

  /** Confirms the sign-up code and signs in. */
  @Post('register/verify')
  @HttpCode(HttpStatus.OK)
  verifyRegistration(@Body() dto: VerifyCodeDto): Promise<SessionResponse> {
    return this.auth.verifyRegistration(dto.contact, dto.code);
  }

  /** Sends a new code for a pending sign-up or password reset. */
  @Post('code/resend')
  @HttpCode(HttpStatus.ACCEPTED)
  resendCode(@Body() dto: ResendCodeDto): Promise<CodeSentResponse> {
    return this.auth.resendCode(dto.contact, dto.purpose);
  }

  @Post('login')
  @HttpCode(HttpStatus.OK)
  login(@Body() dto: LoginDto): Promise<SessionResponse> {
    return this.auth.login(dto.contact, dto.password);
  }

  /** Swaps a refresh token for new tokens; the old refresh token stops working. */
  @Post('refresh')
  @HttpCode(HttpStatus.OK)
  refresh(@Body() dto: RefreshTokenDto): Promise<TokensResponse> {
    return this.auth.refresh(dto.refreshToken);
  }

  @Post('logout')
  @HttpCode(HttpStatus.NO_CONTENT)
  logout(@Body() dto: RefreshTokenDto): Promise<void> {
    return this.auth.logout(dto.refreshToken);
  }

  /** Sends a password reset code to an existing account. */
  @Post('password/forgot')
  @HttpCode(HttpStatus.ACCEPTED)
  forgotPassword(@Body() dto: ForgotPasswordDto): Promise<CodeSentResponse> {
    return this.auth.forgotPassword(dto.contact);
  }

  /** Checks the reset code and returns a token for POST /auth/password/reset. */
  @Post('password/verify')
  @HttpCode(HttpStatus.OK)
  verifyPasswordReset(@Body() dto: VerifyCodeDto): Promise<ResetTokenResponse> {
    return this.auth.verifyPasswordReset(dto.contact, dto.code);
  }

  /** Sets the new password and signs out every device. */
  @Post('password/reset')
  @HttpCode(HttpStatus.NO_CONTENT)
  resetPassword(@Body() dto: ResetPasswordDto): Promise<void> {
    return this.auth.resetPassword(dto.resetToken, dto.password);
  }
}
