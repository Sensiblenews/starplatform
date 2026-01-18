import { Injectable } from '@angular/core';
import { Subject, BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CheckMessageService {
  // --- 기존 코드 ---
  // 이 Subject는 외부에서 메시지 확인을 '요청'할 때 사용합니다.
  private triggerCheckSource = new Subject<void>();
  public triggerCheck$ = this.triggerCheckSource.asObservable();

  // --- 👇 추가된 코드 ---
  // BehaviorSubject를 사용하여 '새 메시지 유무' 상태를 저장하고 공유합니다.
  // 초기값은 false(새 메시지 없음)입니다.
  private newMessageState = new BehaviorSubject<boolean>(false);
  public newMessageState$ = this.newMessageState.asObservable();

  constructor() { }

  // 외부에서 메시지 확인을 요청하는 메서드
  triggerCheck(): void {
    this.triggerCheckSource.next();
  }
  
  // TabsPage에서 API 호출 후 이 메서드를 호출하여 상태를 업데이트합니다.
  updateNewMessageState(hasNewMessage: boolean): void {
    this.newMessageState.next(hasNewMessage);
  }
}