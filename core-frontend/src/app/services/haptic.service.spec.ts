import { HapticService } from './haptic.service';

describe('HapticService', () => {

  describe('isWithinCooldown', () => {

    it('같은 시각에 다시 부르면 막는다', () => {
      expect(HapticService.isWithinCooldown(1_000, 1_000, 400)).toBeTrue();
    });

    it('쿨다운 이내면 막는다', () => {
      expect(HapticService.isWithinCooldown(1_000, 1_399, 400)).toBeTrue();
    });

    it('쿨다운 경계값은 통과시킨다', () => {
      expect(HapticService.isWithinCooldown(1_000, 1_400, 400)).toBeFalse();
    });

    it('쿨다운을 넘기면 통과시킨다', () => {
      expect(HapticService.isWithinCooldown(1_000, 5_000, 400)).toBeFalse();
    });

    // 로비 조회수 틱은 setTimeout 으로 최대 29초 뒤에 흩어져 재생된다.
    // 초기 상태(lastFiredAt = 0)에서는 반드시 첫 발이 나가야 한다.
    it('한 번도 울린 적 없으면 통과시킨다', () => {
      expect(HapticService.isWithinCooldown(0, Date.now(), 400)).toBeFalse();
    });
  });

  describe('tap', () => {

    it('네이티브가 아니면 아무것도 하지 않는다 (웹에서 예외가 나지 않는다)', async () => {
      const service = new HapticService();
      await expectAsync(service.tap()).toBeResolved();
    });
  });
});
