import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createTransport, type Transporter } from 'nodemailer';
import { OtpDelivery, type OtpMessage } from './otp-delivery.js';
import { otpEmail, otpSms } from './otp-messages.js';

/**
 * Email goes through SMTP (Gmail App Password locally, any SMTP provider later).
 * SMS has no provider yet, so outside production the message is written to the log.
 * Without SMTP_HOST (development only, enforced by env validation) emails are logged too.
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
    }
  }

  async send(message: OtpMessage): Promise<void> {
    if (message.contact.kind === 'email') {
      await this.sendEmail(message);
    } else {
      this.sendSms(message);
    }
  }

  private async sendEmail(message: OtpMessage): Promise<void> {
    if (!this.mailer) {
      this.logger.warn(
        `SMTP_HOST is not set; email to ${message.contact.value}: ${otpEmail(message).text}`,
      );
      return;
    }
    const { subject, text } = otpEmail(message);
    await this.mailer.sendMail({
      from: { name: 'TripFair', address: this.from ?? '' },
      to: message.contact.value,
      subject,
      text,
    });
  }

  private sendSms(message: OtpMessage): void {
    if (this.production) {
      throw new Error('No SMS provider is configured');
    }
    this.logger.warn(
      `No SMS provider; SMS to ${message.contact.value}: ${otpSms(message)}`,
    );
  }
}
