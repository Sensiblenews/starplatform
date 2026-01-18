import { HelperService } from './../../services/helper.service';
import { Subscription } from 'rxjs';
import { AuthService } from 'src/app/services/auth.service';
import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { SensibleEventService } from 'src/app/services/sensible-event.service';
import type { SensibleEvent } from 'src/app/types/SensibleEvent';

@Component({
  selector: 'app-event-banner',
  templateUrl: './event-banner.component.html',
  styleUrls: ['./event-banner.component.scss'],
})
export class EventBannerComponent implements OnInit, OnDestroy {
  public event: SensibleEvent = null;
  private eventSubscriber: Subscription;
  constructor(
    private auth: AuthService,
    private eventSvc: SensibleEventService,
    private helper: HelperService,
  ) {}

  @HostListener('unloaded')
  ngOnDestroy() {
    this.eventSubscriber.unsubscribe();
    this.eventSubscriber = null;
  }

  ngOnInit() {
    this.eventSubscriber = this.eventSvc.eventItem.subscribe(
      (item: SensibleEvent) => {
        if (this.event?.EVT_ID !== item?.EVT_ID) {
          this.event = item;
        }
      },
    );
    this.eventSvc.updateSensibleEvent();
  }

  async applyEvent(event: SensibleEvent) {
    try {
      if (!(await this.auth.checkAuthState())) {
        return;
      }

      const eventDescription = await this.helper.alert({
        message: event.EVT_TEXT,
      });
      eventDescription.present();
      await eventDescription.onDidDismiss();

      await this.helper.loading();

      const { RESULT, MEM_STAR, MEM_STAR_CHG } =
        await this.eventSvc.postSensibleEvents({
          EVT_ID: event.EVT_ID,
        });

      if (RESULT !== 'OK') {
        await this.eventSvc.saveAppliedEvent(event.EVT_ID);
        throw new Error('이미 참여한 이벤트 입니다');
      }

      if (event.EVT_URL.length) {
        this.helper.openURL({ url: event.EVT_URL });
      }

      await this.eventSvc.saveAppliedEvent(event.EVT_ID);
      this.auth.updateUserProperty({ MEM_STAR, MEM_STAR_CHG });
    } catch (error) {
      this.helper
        .alert({
          message: error.message,
        })
        .then((_) => _.present());
    } finally {
      await this.helper.loadEnd();
    }
  }
}
