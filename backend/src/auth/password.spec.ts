import { hashPassword, verifyPassword } from './password.js';

describe('password hashing', () => {
  it('verifies the right password and rejects a wrong one', async () => {
    const hash = await hashPassword('matkhau123');

    expect(hash).toMatch(/^scrypt\$17\$8\$1\$[^$]+\$[^$]+$/);
    await expect(verifyPassword('matkhau123', hash)).resolves.toBe(true);
    await expect(verifyPassword('matkhau124', hash)).resolves.toBe(false);
  });

  it('salts every hash', async () => {
    const [first, second] = await Promise.all([
      hashPassword('matkhau123'),
      hashPassword('matkhau123'),
    ]);
    expect(first).not.toBe(second);
  });

  it('treats composed and decomposed Vietnamese letters as the same password', async () => {
    const composed = 'Đức2026'.normalize('NFC');
    const decomposed = 'Đức2026'.normalize('NFD');
    const hash = await hashPassword(composed);

    await expect(verifyPassword(decomposed, hash)).resolves.toBe(true);
  });

  it('rejects hashes in an unknown format', async () => {
    await expect(verifyPassword('matkhau123', 'plain-text')).resolves.toBe(
      false,
    );
  });
});
