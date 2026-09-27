import { GlobalOpeningOverlayComponent } from './global-opening-overlay.component';

// 로비 오프닝 오버레이 타임라인·모션 감소·중단 규칙 검증 (2-29차). TestBed 없이 인스턴스만 만든다.
describe('GlobalOpeningOverlayComponent', () => {

  const makeHaptic = () => jasmine.createSpyObj<{ tap: () => Promise<void> }>('HapticService', ['tap']);
  const make = (haptic = makeHaptic()) => ({ c: new GlobalOpeningOverlayComponent(haptic as any), haptic });

  describe('prefersReducedMotion', () => {

    it('모션 감소가 켜져 있으면 true', () => {
      expect(GlobalOpeningOverlayComponent.prefersReducedMotion({ matchMedia: () => ({ matches: true }) })).toBeTrue();
    });

    it('꺼져 있으면 false', () => {
      expect(GlobalOpeningOverlayComponent.prefersReducedMotion({ matchMedia: () => ({ matches: false }) })).toBeFalse();
    });

    it('matchMedia가 없는 환경에서는 false (예외 없음)', () => {
      expect(GlobalOpeningOverlayComponent.prefersReducedMotion({})).toBeFalse();
      expect(GlobalOpeningOverlayComponent.prefersReducedMotion(null)).toBeFalse();
    });
  });

  describe('타임라인', () => {

    beforeEach(() => jasmine.clock().install());
    afterEach(() => jasmine.clock().uninstall());

    it('햅틱 → 소거 → 종료 순서이며 총 1.5초다', () => {
      expect(GlobalOpeningOverlayComponent.HAPTIC_AT_MS).toBeLessThan(GlobalOpeningOverlayComponent.FADE_AT_MS);
      expect(GlobalOpeningOverlayComponent.FADE_AT_MS).toBeLessThan(GlobalOpeningOverlayComponent.TOTAL_MS);
      expect(GlobalOpeningOverlayComponent.TOTAL_MS).toBe(1500);
    });

    it('재생하면 보이고, 0.4초에 햅틱 1회, 1.2초에 소거, 1.5초에 사라지며 finished가 1회 난다', () => {
      const { c, haptic } = make();
      spyOn(GlobalOpeningOverlayComponent, 'prefersReducedMotion').and.returnValue(false);
      let finished = 0;
      c.finished.subscribe(() => finished++);

      c.play();
      expect(c.visible).toBeTrue();
      expect(c.fading).toBeFalse();
      expect(haptic.tap).not.toHaveBeenCalled();

      jasmine.clock().tick(GlobalOpeningOverlayComponent.HAPTIC_AT_MS);
      expect(haptic.tap).toHaveBeenCalledTimes(1);

      jasmine.clock().tick(GlobalOpeningOverlayComponent.FADE_AT_MS - GlobalOpeningOverlayComponent.HAPTIC_AT_MS);
      expect(c.fading).toBeTrue();
      expect(c.visible).toBeTrue();

      jasmine.clock().tick(GlobalOpeningOverlayComponent.TOTAL_MS - GlobalOpeningOverlayComponent.FADE_AT_MS);
      expect(c.visible).toBeFalse();
      expect(finished).toBe(1);

      // 이후로는 더 아무 일도 없다
      jasmine.clock().tick(5000);
      expect(haptic.tap).toHaveBeenCalledTimes(1);
      expect(finished).toBe(1);
    });

    it('재생 중 다시 play를 불러도 무시한다 (햅틱 중복 없음)', () => {
      const { c, haptic } = make();
      spyOn(GlobalOpeningOverlayComponent, 'prefersReducedMotion').and.returnValue(false);
      c.play();
      c.play();
      jasmine.clock().tick(GlobalOpeningOverlayComponent.TOTAL_MS);
      expect(haptic.tap).toHaveBeenCalledTimes(1);
    });

    it('중간에 끊으면(백그라운드·화면 이탈) 즉시 사라지고 남은 햅틱·finished가 나오지 않는다', () => {
      const { c, haptic } = make();
      spyOn(GlobalOpeningOverlayComponent, 'prefersReducedMotion').and.returnValue(false);
      let finished = 0;
      c.finished.subscribe(() => finished++);

      c.play();
      jasmine.clock().tick(100);
      c.stop(false);
      expect(c.visible).toBeFalse();

      jasmine.clock().tick(GlobalOpeningOverlayComponent.TOTAL_MS);
      expect(haptic.tap).not.toHaveBeenCalled();
      expect(finished).toBe(0);
    });

    it('모션 감소 설정이면 정적 모드로 보이고 햅틱을 울리지 않는다', () => {
      const { c, haptic } = make();
      spyOn(GlobalOpeningOverlayComponent, 'prefersReducedMotion').and.returnValue(true);

      c.play();
      expect(c.staticMode).toBeTrue();
      jasmine.clock().tick(GlobalOpeningOverlayComponent.TOTAL_MS);
      expect(haptic.tap).not.toHaveBeenCalled();
      expect(c.visible).toBeFalse();
    });

    it('파괴 시 타이머를 정리한다', () => {
      const { c, haptic } = make();
      spyOn(GlobalOpeningOverlayComponent, 'prefersReducedMotion').and.returnValue(false);
      c.play();
      c.ngOnDestroy();
      jasmine.clock().tick(GlobalOpeningOverlayComponent.TOTAL_MS);
      expect(haptic.tap).not.toHaveBeenCalled();
    });
  });
});
