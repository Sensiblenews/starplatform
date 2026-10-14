import { NgZone } from '@angular/core';
import { MediaAudioService } from './media-audio.service';

/** play() 결과를 고를 수 있는 가짜 video — 소리 켠 재생 거부(NotAllowedError) 등을 흉내 낸다 */
class FakeVideo {
  paused = true;
  muted = false;
  defaultMuted = false;
  /** 소리 켠 재생을 막을지 (브라우저 자동재생 정책 흉내) */
  blockSound = false;
  play(): Promise<void> {
    if (this.blockSound && !this.muted) {
      const err: any = new Error('blocked');
      err.name = 'NotAllowedError';
      return Promise.reject(err);
    }
    this.paused = false;
    return Promise.resolve();
  }
  pause(): void {
    this.paused = true;
  }
}

describe('MediaAudioService (2-34차 로비 자동 사운드)', () => {
  const zone = { run: (fn: any) => fn(), runOutsideAngular: (fn: any) => fn() } as unknown as NgZone;
  let audio: MediaAudioService;

  beforeEach(() => {
    audio = new MediaAudioService(zone);
  });

  it('기본은 소리 켬으로 재생한다', async () => {
    const v = new FakeVideo();
    expect(await audio.play(v as any)).toBe(true);
    expect(v.paused).toBe(false);
    expect(v.muted).toBe(false);
    expect(audio.isCurrent(v as any)).toBe(true);
  });

  it('한 번에 하나만 — 새 영상이 재생되면 앞의 영상은 멈춘다', async () => {
    const a = new FakeVideo();
    const b = new FakeVideo();
    await audio.play(a as any);
    await audio.play(b as any);
    expect(a.paused).toBe(true);
    expect(b.paused).toBe(false);
    expect(audio.isCurrent(b as any)).toBe(true);
  });

  it('소리 켠 재생이 막히면 음소거로 재생하고 첫 터치에 소리를 켠다', async () => {
    const v = new FakeVideo();
    v.blockSound = true;
    expect(await audio.play(v as any)).toBe(true);
    expect(v.muted).toBe(true);
    expect(audio.isEffectivelyMuted()).toBe(true);

    v.blockSound = false; // 제스처 뒤에는 허용된다
    document.dispatchEvent(new Event('pointerdown'));
    expect(v.muted).toBe(false);
    expect(audio.isEffectivelyMuted()).toBe(false);
  });

  it('음소거 토글은 앱 전체에 적용되고 다음 영상도 음소거로 시작한다', async () => {
    const a = new FakeVideo();
    await audio.play(a as any);
    audio.toggleSound();
    expect(a.muted).toBe(true);
    expect(audio.soundOn).toBe(false);

    const b = new FakeVideo();
    await audio.play(b as any);
    expect(b.muted).toBe(true);

    audio.toggleSound();
    expect(b.muted).toBe(false);
  });

  it('pause 는 재생 담당에서 뺀다', async () => {
    const v = new FakeVideo();
    await audio.play(v as any);
    audio.pause(v as any);
    expect(v.paused).toBe(true);
    expect(audio.isCurrent(v as any)).toBe(false);
  });
});
