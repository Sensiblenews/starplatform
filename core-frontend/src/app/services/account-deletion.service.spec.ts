import { AccountDeletionService, DELETE_CONFIRM_WORD } from './account-deletion.service';

describe('AccountDeletionService (계정 삭제 판정)', () => {
  it('확인 문구는 대소문자·앞뒤 공백을 무시하고 정확히 DELETE 여야 한다', () => {
    expect(AccountDeletionService.isConfirmed(DELETE_CONFIRM_WORD)).toBeTrue();
    expect(AccountDeletionService.isConfirmed('  delete ')).toBeTrue();
    expect(AccountDeletionService.isConfirmed('DELET')).toBeFalse();
    expect(AccountDeletionService.isConfirmed('')).toBeFalse();
    expect(AccountDeletionService.isConfirmed(undefined)).toBeFalse();
    expect(AccountDeletionService.isConfirmed(123)).toBeFalse();
  });

  it('Apple 로그인 여부는 providerData 의 apple.com 으로 판단한다', () => {
    expect(AccountDeletionService.usesApple({ providerData: [{ providerId: 'apple.com' }] })).toBeTrue();
    expect(AccountDeletionService.usesApple({ providerData: [{ providerId: 'google.com' }] })).toBeFalse();
    expect(AccountDeletionService.usesApple({ providerData: [] })).toBeFalse();
    expect(AccountDeletionService.usesApple(null)).toBeFalse();
    expect(AccountDeletionService.usesApple(undefined)).toBeFalse();
  });
});
