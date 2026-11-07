import type { OtpMessage } from './otp-delivery.js';

// User-facing text is Vietnamese, like the app.
const subjects: Record<OtpMessage['purpose'], string> = {
  REGISTER: 'TripFair: mã xác thực đăng ký',
  RESET_PASSWORD: 'TripFair: mã đặt lại mật khẩu',
};

const intros: Record<OtpMessage['purpose'], string> = {
  REGISTER: 'Mã xác thực đăng ký TripFair của bạn là',
  RESET_PASSWORD: 'Mã đặt lại mật khẩu TripFair của bạn là',
};

export function otpEmail({ code, purpose, validMinutes }: OtpMessage): {
  subject: string;
  text: string;
} {
  return {
    subject: subjects[purpose],
    text: [
      `${intros[purpose]}: ${code}`,
      '',
      `Mã có hiệu lực trong ${validMinutes} phút. Không chia sẻ mã này với bất kỳ ai.`,
      'Nếu bạn không yêu cầu mã này, hãy bỏ qua email.',
    ].join('\n'),
  };
}

export function otpSms({ code, purpose, validMinutes }: OtpMessage): string {
  return `${intros[purpose]} ${code}. Hiệu lực ${validMinutes} phút, không chia sẻ mã này.`;
}
