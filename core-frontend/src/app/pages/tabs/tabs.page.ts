import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { IonTabs } from '@ionic/angular';
import { StorageService } from 'src/app/services/storage.service';
import { AuthService } from './../../services/auth.service';
import { SystemService } from './../../services/system.service';
import { Subscription } from 'rxjs';
import { MoveTabService } from 'src/app/services/move-tab.service';
import { HttpService } from 'src/app/services/http.service';
import { CheckMessageService } from 'src/app/services/check-message.service';

@Component({
  selector: 'app-tabs',
  templateUrl: 'tabs.page.html',
  styleUrls: ['tabs.page.scss'],
})
export class TabsPage implements OnInit, OnDestroy {
  private moveTabSubscription: Subscription;
  private messageCheckSubscription: Subscription;
  @ViewChild(IonTabs) tabs: IonTabs;
  constructor(
    private auth: AuthService,
    private storage: StorageService,
    private system: SystemService,
    private moveTabService: MoveTabService,
    private http: HttpService,
    private checkMessageService: CheckMessageService,
  ) {
  }

  ngOnInit(): void {
    this.moveTabSubscription = this.moveTabService.newTabName$.subscribe(tabName => {
      this.tabs.select(tabName);
    });

    this.messageCheckSubscription = this.checkMessageService.triggerCheck$.subscribe(() => {
      const memId = this.auth.getUser()?.MEM_ID;
      if (memId) {
        this.checkNewMessages(memId);
      }
    });
  }
  ngOnDestroy(): void {
    if (this.moveTabSubscription) {
      this.moveTabSubscription.unsubscribe();
    }
    if (this.messageCheckSubscription) {
      this.messageCheckSubscription.unsubscribe();
    }
  }

  checkNewMessages(memId: number | string) {
    const body = { MEM_ID: memId };
    console.log("checking new messages for MEM_ID:", memId);
    this.http.post('/api/newMessage', body).subscribe((res: any) => {
      console.log("newMessage API response: ", res.UNREAD_COUNT);
      const hasNewMessage = res.UNREAD_COUNT && res.UNREAD_COUNT > 0;
      this.checkMessageService.updateNewMessageState(hasNewMessage);
    });
  }

  public get userInfo() {
    return this.auth.getUserAsObservable();
  }

  tabChanged() {
    if (!this.system.isInitialized()) {
      return;
    }
    const currentTab = this.tabs.getSelected();
    console.log('Current selected tab:', currentTab);

    this.storage.set('lastTab', currentTab);
  }
}
