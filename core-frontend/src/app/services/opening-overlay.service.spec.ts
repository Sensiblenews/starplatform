import { OpeningOverlayService } from './opening-overlay.service';

// 오프닝 오버레이 "프로세스당 1회" 판정 검증 (2-29차)
describe('OpeningOverlayService', () => {

  describe('shouldPlay', () => {

    it('아직 재생하지 않았고 포그라운드면 재생한다', () => {
      expect(OpeningOverlayService.shouldPlay(false, true)).toBeTrue();
    });

    it('이미 재생했으면 다시 재생하지 않는다 (뒤로 가기 복귀·백그라운드 복귀 포함)', () => {
      expect(OpeningOverlayService.shouldPlay(true, true)).toBeFalse();
    });

    it('백그라운드에서는 재생하지 않는다', () => {
      expect(OpeningOverlayService.shouldPlay(false, false)).toBeFalse();
    });
  });

  describe('consumeFirstPlay', () => {

    it('첫 호출만 true이고 두 번째부터는 false다', () => {
      const s = new OpeningOverlayService();
      expect(s.consumeFirstPlay(true)).toBeTrue();
      expect(s.consumeFirstPlay(true)).toBeFalse();
      expect(s.consumeFirstPlay(true)).toBeFalse();
    });

    it('백그라운드에서 부르면 소진되지 않아 다음 포그라운드 진입에서 재생한다', () => {
      const s = new OpeningOverlayService();
      expect(s.consumeFirstPlay(false)).toBeFalse();
      expect(s.consumeFirstPlay(true)).toBeTrue();
    });

    it('reset 후에는 다시 1회 재생한다 (콜드 스타트 모사)', () => {
      const s = new OpeningOverlayService();
      s.consumeFirstPlay(true);
      s.reset();
      expect(s.consumeFirstPlay(true)).toBeTrue();
    });
  });
});
