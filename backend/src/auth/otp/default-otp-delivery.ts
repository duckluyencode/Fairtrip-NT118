import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createTransport, type Transporter } from 'nodemailer';
import { OtpDelivery, type OtpMessage } from './otp-delivery.js';
import { otpEmail, otpSms } from './otp-messages.js';

/**
 * Email goes through SMTP (Gmail App Password locally, any SMTP provider later).
 * A channel with no provider (SMS, or email without SMTP_HOST) writes the message to
 * the log outside production. In production it fails, so the API answers
 * CODE_DELIVERY_FAILED and codes never end up in production logs.
 */
@Injectable()
export class DefaultOtpDelivery extends OtpDelivery {
  private readonly logger = new Logger(DefaultOtpDelivery.name);
  private readonly mailer?: Transporter;
  private readonly from?: string;
  private readonly production: boolean;

  constructor(config: ConfigService) {
    super();
    this.production = config.get<string>('NODE_ENV') === 'production';
    const host = config.get<string>('SMTP_HOST');
    if (host) {
      const port = Number(config.getOrThrow<string>('SMTP_PORT'));
      const user = config.getOrThrow<string>('SMTP_USER');
      this.mailer = createTransport({
        host,
        port,
        // 465 is SMTP over TLS; other ports upgrade with STARTTLS.
        secure: port === 465,
        auth: { user, pass: config.getOrThrow<string>('SMTP_PASSWORD') },
      });
      this.from = config.get<string>('SMTP_FROM') || user;
    } else if (this.production) {
      this.logger.warn(
        'SMTP_HOST is not set: sign-up and password reset by email fail until it is',
      );
    }
  }

  async send(message: OtpMessage): Promise<void> {
    if (message.contact.kind === 'email' && this.mailer) {
      const { subject, text } = otpEmail(message);
      await this.mailer.sendMail({
        from: { name: 'TripFair', address: this.from ?? '' },
        to: message.contact.value,
        subject,
        text,
      });
      return;
    }
    const channel = message.contact.kind === 'email' ? 'email' : 'SMS';
    if (this.production) {
      throw new Error(`No ${channel} provider is configured`);
    }
    const text =
      message.contact.kind === 'email'
        ? otpEmail(message).text
        : otpSms(message);
    this.logger.warn(
      `No ${channel} provider; ${channel} to ${message.contact.value}: ${text}`,
    );
  }
}
