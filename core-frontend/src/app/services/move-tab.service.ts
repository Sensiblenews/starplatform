import { Injectable } from "@angular/core";
import { Subject } from "rxjs";

@Injectable({providedIn: 'root'})
export class MoveTabService {
  private newTabName = new Subject<string>();

  newTabName$ = this.newTabName.asObservable();

  moveToNewTab(tabName: string) {
    this.newTabName.next(tabName);
  }
}
