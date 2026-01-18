import { Observable, BehaviorSubject } from 'rxjs';
import { Content } from 'src/app/types/Content';
import { Injectable } from '@angular/core';
import { HttpService } from './http.service';
import type {
  SensibleEvent,
  SensibleEventResponse,
  SensiblePopupManageObject,
} from '../types/SensibleEvent';
import { StorageService } from './storage.service';

@Injectable({
  providedIn: 'root',
})
export class SensibleEventService {
  private sensibleEvent: BehaviorSubject<any> = new BehaviorSubject(null);
  constructor(private http: HttpService, private storage: StorageService) {}

  public get eventItem(): Observable<SensibleEvent> {
    return this.sensibleEvent.asObservable();
  }

  async checkEventIndex(
    EventArray: number[],
    EVT_ID: number,
  ): Promise<boolean> {
    if (!EventArray.length) {
      return false;
    }
    return EventArray.findIndex((ID) => ID === EVT_ID) > -1;
  }

  async isElapsed(timeStamp: number): Promise<boolean> {
    const dayTimestamp = 86400000;
    const blockingPeriod = 7;
    const week = dayTimestamp * blockingPeriod;
    const dateDiff = Date.now() - timeStamp;
    return dateDiff >= week;
  }

  async isAppliedPopup(CON_ID: number): Promise<boolean> {
    const applied = await this.getAppliedPopup();

    if (!applied) {
      return false;
    }

    if (CON_ID !== applied.CON_ID) {
      return true;
    }

    if (await this.isElapsed(applied.timestamp)) {
      return false;
    }

    return true;
  }

  async getAppliedPopup(): Promise<SensiblePopupManageObject> {
    const saved = await this.storage.get('lastPopup');
    return saved?.length ? JSON.parse(saved) : null;
  }

  async saveAppliedPopup(popup: SensiblePopupManageObject): Promise<void> {
    await this.storage.set('lastPopup', JSON.stringify(popup));
  }

  async updatePopup(): Promise<Content> {
    try {
      const popupContent = await this.getPopupList();
      if (!popupContent) {
        return null;
      }
      if (await this.isAppliedPopup(popupContent.CON_ID)) {
        return null;
      }
      return popupContent;
    } catch (error) {
      console.warn(error.message);
    }
  }

  getPopupList(): Promise<Content> {
    const url = `/app/popup`;
    return this.http.post<Content>(url, null).toPromise();
  }

  async isAppliedEvent(EVT_ID: number): Promise<boolean> {
    const appliedEvent = await this.getAppliedEvent();
    return await this.checkEventIndex(appliedEvent, EVT_ID);
  }

  async getAppliedEvent(): Promise<number[]> {
    const saved = await this.storage.get('applied_event');
    return saved?.length ? JSON.parse(saved) : [];
  }

  async saveAppliedEvent(EVT_ID: number): Promise<void> {
    const appliedEvent = await this.getAppliedEvent();
    const exist = await this.checkEventIndex(appliedEvent, EVT_ID);
    if (exist) {
      return;
    }
    appliedEvent.push(EVT_ID);
    await this.storage.set('applied_event', JSON.stringify(appliedEvent));
    this.sensibleEvent.next(null);
  }

  async updateSensibleEvent(): Promise<void> {
    const { RESULT, item } = await this.getSensibleEvents();

    if (RESULT !== 'OK') {
      return;
    }

    if (this.sensibleEvent.value?.EVT_ID === item.EVT_ID) {
      return;
    }

    if (await this.isAppliedEvent(item.EVT_ID)) {
      return;
    }

    this.sensibleEvent.next(item);
  }

  getSensibleEvents(): Promise<SensibleEventResponse> {
    const url = `/app/sensibleEvent`;
    return this.http
      .post<SensibleEventResponse>(url, null, { needToken: true })
      .toPromise();
  }

  postSensibleEvents(eventInfo: {
    EVT_ID: number;
  }): Promise<SensibleEventResponse> {
    const url = `/api/sensibleEvent`;
    return this.http
      .post<SensibleEventResponse>(url, eventInfo, { needToken: true })
      .toPromise();
  }
}
