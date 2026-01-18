import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { Content } from '../types/Content';

type contentAction = { action: string; content: Content };
@Injectable({
  providedIn: 'root',
})
export class ContentManagerService {
  private changedContent: Subject<contentAction> = new Subject();
  constructor() {}
  get changed(): Observable<contentAction> {
    return this.changedContent.asObservable();
  }

  setChange({ action, content }: contentAction) {
    this.changedContent.next({ action, content });
  }
}
