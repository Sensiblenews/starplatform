// src/app/services/global-feedback.service.ts
import { Injectable } from '@angular/core';
import { HttpService } from './http.service'; // 기존에 쓰시는 HTTP 서비스
import { Haptics, ImpactStyle } from '@capacitor/haptics';

@Injectable({ providedIn: 'root' })
export class GlobalFeedbackService {
  private pollingIntervalId: any;
  private lastCheckTime: string = '';
  private isPolling = false;

  // 카톡 느낌의 짧은 사운드 (에셋 경로에 맞게 수정)
  private audio = new Audio('assets/sounds/tick.mp3'); 
  private lastPlay = 0;

  constructor(private http: HttpService) {
    this.audio.volume = 0.65; // 귀에 거슬리지 않는 볼륨
  }

  // 🌟 앱 실행 시 호출할 폴링 시작 함수
  startPolling() {
    if (this.isPolling) return;
    this.isPolling = true;

    // 30초(30000ms)마다 백그라운드에서 실행
    this.pollingIntervalId = setInterval(() => {
      this.checkMyVisitors();
    }, 30000);
  }

  // 🌟 로그아웃 시 폴링을 멈추는 함수
  stopPolling() {
    if (this.pollingIntervalId) {
      clearInterval(this.pollingIntervalId);
    }
    this.isPolling = false;
  }

  // 🌟 내 스타페이지 방문자 확인 통신
  private checkMyVisitors() {
    const isStar = localStorage.getItem('isStar') === 'true';
    const starId = localStorage.getItem('starId');

    // 로그인된 크리에이터(스타)가 아니라면 서버에 물어볼 필요 없음
    if (!isStar || !starId) return;

    // TODO: 내 페이지의 방문자 증가수만 가볍게 물어보는 API 호출
    this.http.post('/api/super/star/my-visitors-poll', { 
      starId: starId,
      lastCheckTime: this.lastCheckTime || new Date().toISOString()
    }).subscribe((res: any) => {
      if (res.result === 'OK') {
        this.lastCheckTime = res.currentTime;

        // 새 방문자가 1명이라도 생겼다면 즉시 도파민 트리거!
        if (res.newViews && res.newViews > 0) {
          this.triggerDopamine();
        }
      }
    });
  }

  // 🌟 방문자 증가 시 호출되는 피드백 함수 (쿨다운 적용)
  private async triggerDopamine() {
    const now = Date.now();
    // 0.4초 이내 연속 호출 방지
    if (now - this.lastPlay < 400) return;
    this.lastPlay = now;

    // 우측 상단 토글에서 설정한 값 읽기 (기본값: true)
    const hapticOn = localStorage.getItem('hapticOn') !== 'false';
    const soundOn = localStorage.getItem('soundOn') !== 'false';

    // 1. 진동
    if (hapticOn) {
      try {
        await Haptics.impact({ style: ImpactStyle.Light });
      } catch (e) {}
    }

    // 2. 소리
    if (soundOn) {
      try {
        this.audio.pause();
        this.audio.currentTime = 0;
        this.audio.play().catch(() => {});
      } catch (e) {}
    }
  }
}