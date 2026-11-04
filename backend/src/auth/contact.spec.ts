import { parseContact } from './contact.js';

describe('parseContact', () => {
  it('lowercases and trims emails', () => {
    expect(parseContact('  Duc.Do@Gmail.COM ')).toEqual({
      kind: 'email',
      value: 'duc.do@gmail.com',
    });
  });

  it.each(['0905128846', '090 512 8846', '090.512.8846', '+84 905-128-846'])(
    'turns the Vietnamese mobile number %s into E.164',
    (input) => {
      expect(parseContact(input)).toEqual({
        kind: 'phone',
        value: '+84905128846',
      });
    },
  );

  it.each([
    '',
    'abc',
    'a@b',
    '090512884',
    '09051288461',
    '1905128846',
    '+1 202 555 0101',
  ])('rejects %j', (input) => {
    expect(parseContact(input)).toBeNull();
  });
});
