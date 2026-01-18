import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'isPlaying', // 템플릿에서 사용할 이름 ( | isPlaying )
  pure: false     // [중요] 외부 변수(playingItemId) 변경 시에도 파이프를 재실행하기 위해 false로 설정
})
export class IsPlayingPipe implements PipeTransform {

  /**
   * 값을 변환하는 메인 로직
   * @param value 파이프 바로 앞의 값 (예: item.CON_ID)
   * @param playingItemId 파이프에 전달하는 추가 인자 (예: playingItemId)
   * @returns boolean 값 (true 또는 false)
   */
  transform(value: string | number, playingItemId: string | undefined): boolean {
    if (!playingItemId) {
      return false;
    }

    return value.toString() === playingItemId;
  }
}