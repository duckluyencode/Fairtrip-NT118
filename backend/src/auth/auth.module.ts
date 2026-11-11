import { Module } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { JwtModule } from '@nestjs/jwt';
import { AuthController } from './auth.controller.js';
import { AuthGuard } from './auth.guard.js';
import { AuthService } from './auth.service.js';
import {
  GoogleIdTokenVerifier,
  HttpGoogleIdTokenVerifier,
} from './google/google-id-token.verifier.js';
import { DefaultOtpDelivery } from './otp/default-otp-delivery.js';
import { OtpDelivery } from './otp/otp-delivery.js';
import { OtpService } from './otp/otp.service.js';
import { TokenService } from './token.service.js';

@Module({
  imports: [
    JwtModule.registerAsync({
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        secret: config.getOrThrow<string>('JWT_SECRET'),
        signOptions: { algorithm: 'HS256' },
        verifyOptions: { algorithms: ['HS256'] },
      }),
    }),
  ],
  controllers: [AuthController],
  providers: [
    AuthService,
    OtpService,
    TokenService,
    AuthGuard,
    { provide: OtpDelivery, useClass: DefaultOtpDelivery },
    { provide: GoogleIdTokenVerifier, useClass: HttpGoogleIdTokenVerifier },
  ],
  exports: [AuthGuard, GoogleIdTokenVerifier],
})
export class AuthModule {}
