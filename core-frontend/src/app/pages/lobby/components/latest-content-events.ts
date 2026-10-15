import { HttpService } from 'src/app/services/http.service';
import { DeviceIdService } from 'src/app/services/device-id.service';
import { LatestContentEventType, LatestContentItem, eventKey } from '../latest-contents-logic';

/**
 * 로비 편성 구역 이벤트(노출·재생·클릭·프로필 클릭) 기록 — Star Shorts·세로형·가로형 공용 (2-34차).
 * 편성 ID(lcId)가 구역마다 달라 서버 집계는 구역별로 자연히 나뉜다.
 * 세션 안에서 편성·종류당 1회만 보낸다. 실패는 무시 — 통계가 로비를 방해하면 안 된다
 */
export function sendLatestContentEvent(
  http: HttpService,
  deviceIdService: DeviceIdService,
  sent: Set<string>,
  item: LatestContentItem,
  type: LatestContentEventType,
): void {
  const key = eventKey(item.lcId, type);
  if (sent.has(key)) return;
  sent.add(key);

  const send = (deviceId: string) => {
    if (!deviceId) return;
    http.post('/api/super/lobby/latest-contents/event', { lcId: item.lcId, eventType: type, deviceId })
      .subscribe({ next: () => { }, error: () => { } });
  };
  const cached = deviceIdService.get();
  if (cached) {
    send(cached);
  } else {
    deviceIdService.resolve().then(send).catch(() => { });
  }
}
