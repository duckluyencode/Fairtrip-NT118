import { Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { DefaultOtpDelivery } from './default-otp-delivery.js';
import type { OtpMessage } from './otp-delivery.js';

const email: OtpMessage = {
  contact: { kind: 'email', value: 'duc@example.com' },
  code: '123456',
  purpose: 'REGISTER',
  validMinutes: 15,
};
const sms: OtpMessage = {
  ...email,
  contact: { kind: 'phone', value: '+84905128846' },
};

function delivery(env: Record<string, string>) {
  return new DefaultOtpDelivery(new ConfigService(env));
}

describe('DefaultOtpDelivery without providers', () => {
  let warn: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    warn = vi.spyOn(Logger.prototype, 'warn').mockImplementation(() => {});
  });

  afterEach(() => {
    warn.mockRestore();
  });

  it('logs the code outside production', async () => {
    const otp = delivery({ NODE_ENV: 'development' });
    await otp.send(email);
    await otp.send(sms);
    expect(warn).toHaveBeenCalledWith(expect.stringContaining('123456'));
  });

  it('fails in production and never logs the code', async () => {
    const otp = delivery({ NODE_ENV: 'production' });
    await expect(otp.send(email)).rejects.toThrow('No email provider');
    await expect(otp.send(sms)).rejects.toThrow('No SMS provider');
    for (const [line] of warn.mock.calls) {
      expect(String(line)).not.toContain('123456');
    }
  });
});
