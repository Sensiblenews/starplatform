import { Injectable } from "@angular/core";
import { Subject } from "rxjs";

@Injectable({ providedIn: 'root' })
export class ScrollSyncService {
  private deltaSubject = new Subject<number>();
  delta$ = this.deltaSubject.asObservable();

  emitScrollDelta(delta: number) {
    this.deltaSubject.next(delta);
  }
}