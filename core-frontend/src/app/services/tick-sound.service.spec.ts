import { TickSoundService } from './tick-sound.service';

describe('TickSoundService', () => {

  afterEach(() => {
    localStorage.removeItem('soundOn');
  });

  describe('isWithinCooldown', () => {

    it('같은 시각에 다시 부르면 막는다', () => {
      expect(TickSoundService.isWithinCooldown(1_000, 1_000, 400)).toBeTrue();
    });

    it('쿨다운 이내면 막는다', () => {
      expect(TickSoundService.isWithinCooldown(1_000, 1_399, 400)).toBeTrue();
    });

    it('쿨다운 경계값은 통과시킨다', () => {
      expect(TickSoundService.isWithinCooldown(1_000, 1_400, 400)).toBeFalse();
    });

    // 한 번도 재생한 적 없는 초기 상태(0)에서는 반드시 첫 발이 나가야 한다
    it('한 번도 재생한 적 없으면 통과시킨다', () => {
      expect(TickSoundService.isWithinCooldown(0, Date.now(), 400)).toBeFalse();
    });
  });

  describe('isSoundOn', () => {

    it('값이 없으면 켜진 것으로 본다', () => {
      expect(TickSoundService.isSoundOn()).toBeTrue();
    });

    it("'false' 일 때만 꺼진 것으로 본다", () => {
      localStorage.setItem('soundOn', 'false');
      expect(TickSoundService.isSoundOn()).toBeFalse();
    });

    it('다른 값은 켜진 것으로 본다', () => {
      localStorage.setItem('soundOn', 'true');
      expect(TickSoundService.isSoundOn()).toBeTrue();
    });
  });

  describe('play', () => {

    it('토글이 꺼져 있으면 재생하지 않는다', async () => {
      localStorage.setItem('soundOn', 'false');
      const service = new TickSoundService();
      const audio = service['audio'] as HTMLAudioElement;
      const spy = spyOn(audio, 'play');

      await service.play(1_000);

      expect(spy).not.toHaveBeenCalled();
    });

    it('쿨다운에 걸리면 재생하지 않는다', async () => {
      const service = new TickSoundService();
      const audio = service['audio'] as HTMLAudioElement;
      const spy = spyOn(audio, 'play').and.returnValue(Promise.resolve());

      await service.play(1_000);
      await service.play(1_200);

      expect(spy).toHaveBeenCalledTimes(1);
    });

    // 재생이 거부돼도(브라우저 정책) 호출부가 깨지면 안 된다
    it('재생이 거부돼도 예외를 밖으로 내보내지 않는다', async () => {
      const service = new TickSoundService();
      const audio = service['audio'] as HTMLAudioElement;
      spyOn(audio, 'play').and.returnValue(Promise.reject(new Error('NotAllowedError')));

      await expectAsync(service.play(1_000)).toBeResolved();
    });
  });
});
