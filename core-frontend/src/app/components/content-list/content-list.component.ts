import { switchMap, take, throttleTime } from 'rxjs/operators';
import {
  BehaviorSubject,
  EMPTY,
  forkJoin,
  Observable,
  of,
  Subscription,
} from 'rxjs';
import {
  AfterViewChecked,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  Input,
  NgZone,
  OnDestroy,
  OnInit,
  QueryList,
  ViewChild,
  ViewChildren,
} from '@angular/core';
import { Content, ContentResponse } from 'src/app/types/Content';
import { CdkVirtualScrollViewport } from '@angular/cdk/scrolling';
import { IonContent, IonInfiniteScroll, Platform } from '@ionic/angular';
import { ContentManagerService } from 'src/app/services/content-manager.service';
import { ContentType } from 'src/app/constants/ContentTypes';
import { Router } from '@angular/router';
import { HelperService } from 'src/app/services/helper.service';
import { HttpService } from 'src/app/services/http.service';
import { AuthService } from 'src/app/services/auth.service';
import { ContentService } from 'src/app/services/content.service';
import NativeBridge from 'src/app/plugins/native-bridge';
import { ScrollSyncService } from 'src/app/services/scroll-sync.service';

@Component({
  selector: 'app-content-list',
  templateUrl: './content-list.component.html',
  styleUrls: ['./content-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContentListComponent
  implements OnInit, OnDestroy, AfterViewChecked
{
  @ViewChild(IonContent) content: IonContent;
  @ViewChild(IonInfiniteScroll) infScroll: IonInfiniteScroll;
  @ViewChild(CdkVirtualScrollViewport, { static: false })
  virtualView: CdkVirtualScrollViewport;

  @ViewChildren('contentItemRef', { read: ElementRef })
  contentItems!: QueryList<ElementRef>;
  scrollSub?: Subscription;

  @Input() viewType = 'default';
  @Input() contentType: ContentType;
  @Input() contentSubscriber: (
    page: number,
    itemCount: number,
  ) => Observable<ContentResponse>;
  @Input() itemCount = 10;

  playingItemId: string | null = null;

  public initialized: boolean;
  public contentAllLoaded: boolean;
  public enableRefresher = true;

  private pageContents: BehaviorSubject<Content[]> = new BehaviorSubject([]);
  private itemSizeMap = {
    default: 460,
    minimal: 54,
  };
  private currentPage = 0;
  private contentChangeSubscriber: Subscription;
  private defaultThumbnailPath = '../../../assets/img/defaultImg/thumbnail.svg';
  private scrollSubscription: Subscription;
  private isScrollEventSubscribed = false;
  private prevValue = 0;

  constructor(
    private cm: ContentManagerService,
    private router: Router,
    private zone: NgZone,
    private helper: HelperService,
    private http: HttpService,
    private auth: AuthService,
    private contentService: ContentService,
    private platform: Platform,
    private scrollService: ScrollSyncService,
  ) {}

  public get contentLists(): Observable<Content[]> {
    return this.pageContents.asObservable();
  }

  get itemSize(): number {
    return window.innerWidth * 0.8 + 115;
  }

  // ngAfterViewInit(): void {
  //   console.log('hello');
  //   this.scrollSub = fromEvent(window, 'scroll')
  //     .pipe(debounceTime(300))
  //     .subscribe(() => {
  //       console.log('hmm');
  //       this.setTopVisibleItemAsPlaying();
  //     });
  // }

  ngAfterViewChecked() {
    if (this.virtualView && !this.isScrollEventSubscribed) {
      this.isScrollEventSubscribed = true;
      const viewportElement = this.virtualView.elementRef.nativeElement;
      NativeBridge.setViewportHeight({ height: viewportElement.clientHeight });

      (window as any).handleNativeAdScroll = (deltaY: number) => {
        let currentValue = this.virtualView.measureScrollOffset();
        currentValue += deltaY;
        this.virtualView.scrollToOffset(currentValue);
      };

      this.scrollService.delta$.subscribe((delta) => {
        let currentValue = this.virtualView.measureScrollOffset();
        currentValue += delta;
        this.virtualView.scrollToOffset(currentValue);
      });

      const tTime = this.platform.is('android') ? 5 : 10;

      this.scrollSubscription = this.virtualView
        .elementScrolled()
        .pipe(throttleTime(tTime))
        .subscribe(() => {
          const currentValue = this.virtualView.measureScrollOffset();
          if (currentValue !== this.prevValue) {
            NativeBridge.updateAdPosition({
              value: currentValue,
              direction: currentValue > this.prevValue ? 'down' : 'up',
            });
            this.prevValue = currentValue;
          }
        });

      this.scrollSub = this.virtualView
        .elementScrolled()
        .pipe(throttleTime(300))
        .subscribe(() => {
          this.setTopVisibleItemAsPlaying();
        });

      setTimeout(() => this.setTopVisibleItemAsPlaying(), 50);
    }
  }

  @HostListener('unloaded')
  ngOnDestroy(): void {
    if (this.contentChangeSubscriber) {
      this.contentChangeSubscriber.unsubscribe();
      this.contentChangeSubscriber = null;
    }
    if (this.scrollSubscription) {
      this.scrollSubscription.unsubscribe();
    }

    this.scrollSub.unsubscribe();
  }

  ngOnInit(): void {
    this.getContents().then((res) => {
      this.initialized = true;
      this.contentAllLoaded = res?.end;
    });

    this.contentChangeSubscriber = this.cm.changed.subscribe(
      ({ action, content }) => {

        if (action === 'changed') {
          this.updateContent(content);
        }

        if (action === 'created') {
          const { CON_TYPE } = content;
          if (CON_TYPE !== this.contentType) {
            return;
          }
          // this.updateContentList();
        }
        if (action === 'deleted') {
          const { CON_TYPE } = content;
          if (CON_TYPE !== this.contentType) {
            return;
          }
          this.deleteContent(content.CON_ID);
        }

        if (action === 'goblin-writed') {
          // refresh contents
          this.refreshContents(null);
        }

        if (action === 'blocked') {
          this.refreshContents(null);
        }
      },
    );
  }

  async getContents(option?: {
    reset: boolean;
  }): Promise<{ end: boolean; error: boolean }> {
    return new Promise((resolve) => {
      
      forkJoin([
        this.pageContents.pipe(take(1)),
        this.contentSubscriber(this.currentPage, this.itemCount),
      ]).subscribe(
        ([origin, received]) => {
          const { contents } = received;
          console.log("data loaded")
          console.log(contents);
          if (contents.length) {
            if (option?.reset) {
              contents.splice(3, 0, {});
              contents.splice(9, 0, {});
              if (contents.length > 14) {
                contents.splice(16, 0, {});
              }
              this.pageContents.next(contents);
            } else {
              const newList = origin.concat(contents);
              const arr = newList.filter(
                (item) => item.CON_TITLE !== undefined,
              );
              arr.splice(3, 0, {});
              arr.splice(9, 0, {});
              if (arr.length > 14) {
                arr.splice(16, 0, {});
              }
              this.pageContents.next(arr);
            }
          }
          resolve({ end: contents?.length < this.itemCount, error: false });
        },
        (_) => {
          resolve({ end: false, error: true });
        },
      );
    });
  }

  updateContent(content: Content) {
    this.contentLists
      .pipe(
        take(1),
        switchMap((contentList) => {
          const index = contentList.findIndex(
            ({ CON_ID }) => CON_ID === content.CON_ID,
          );
          if (index < 0) {
            return EMPTY;
          }
          Object.assign(contentList[index], content);
          return of(contentList);
        }),
      )
      .subscribe((changedContent) => {
        const arr = changedContent.filter(
          (item) => item.CON_TITLE !== undefined,
        );
        arr.splice(3, 0, {});
        arr.splice(9, 0, {});
        if (arr.length > 14) {
          arr.splice(16, 0, {}); 
        }
        this.pageContents.next(arr);
      });
  }

  updateContentList() {
    forkJoin([
      this.pageContents.pipe(take(1)),
      this.contentSubscriber(0, this.itemCount),
    ]).subscribe(([origin, received]) => {
      const { contents } = received;
      const addedIndex = contents.findIndex(
        ({ CON_ID }, index) => CON_ID === origin[index].CON_ID,
      );
      const newContents = contents.slice(0, addedIndex).concat(origin);
      this.pageContents.next(newContents);
    });
  }

  deleteContent(deletedConId: number) {
    this.contentLists
      .pipe(
        take(1),
        switchMap((contentList) =>
          of(contentList.filter(({ CON_ID }) => CON_ID !== deletedConId)),
        ),
      )
      .subscribe((deletedList) => {
        this.pageContents.next(deletedList);
      });
  }

  getContentsImage(imageUrl) {
    return imageUrl?.length ? imageUrl : this.defaultThumbnailPath;
  }

  async refreshContents(event) {
    this.currentPage = 0;
    this.contentAllLoaded = false;
    const { end } = await this.getContents({ reset: true });
    this.contentAllLoaded = end;
    if (event != null) {
      event.target.complete();
    }

    setTimeout(() => {
      if (this.virtualView) {
        const height = this.virtualView.elementRef.nativeElement.clientHeight;
        NativeBridge.setViewportHeight({ height });
      }
    }, 100);
  }

  async loadMoreContents(event) {
    this.currentPage++;
    const scrollOffsetBefore = this.virtualView.measureScrollOffset();
    const { end } = await this.getContents();
    this.contentAllLoaded = end;
    event.target.complete();

    setTimeout(() => {
      if (this.virtualView) {
        const height = this.virtualView.elementRef.nativeElement.clientHeight;
        NativeBridge.setViewportHeight({ height });
      }
    }, 100);

    setTimeout(() => {
      if (this.virtualView) {
        this.virtualView.scrollToOffset(scrollOffsetBefore);
    
        const height = this.virtualView.elementRef.nativeElement.clientHeight;
        NativeBridge.setViewportHeight({ height });
      }
    }, 50);
  }

  checkUniqueContentId(index: number, content: Content) {
    return content.CON_ID ?? `ad-${index}`;
  }

  createDummy(count: number = 1) {
    return new Array(Math.floor(count));
  }

  checkScrollIdx(cursor) {
    if (cursor === 0 && !this.enableRefresher) {
      this.enableRefresher = true;
    } else if (this.enableRefresher) {
      this.enableRefresher = false;
    }
  }

  writerClick(event: Event, item: any) {
    const { PRS_ID, MEM_NAME, press } = item
    let isNormalUser = false;
    let pfp: string;

    if (!press) {
      isNormalUser = true;
      pfp = item.MEM_PICTURE;
    } else {
      pfp = item.press.STORED_FILE_NM;
    }

    event.stopPropagation();
    event.preventDefault();
    const i = isNormalUser ? 1 : 0

    if (PRS_ID) {
      this.router.navigate(['profile-page'], {
        state: { prsId: PRS_ID, memName: MEM_NAME, isNormalUser: i, pfp },
      });
    }
  }

  isPlayingItem(item: any): boolean {
    return this.playingItemId === item.CON_ID.toString(); // 또는 item 인덱스 비교 등
  }

  setTopVisibleItemAsPlaying() {
    const visible = this.contentItems
      .map((elRef) => {
        const el = elRef.nativeElement as HTMLElement;
        const rect = el.getBoundingClientRect();
        const isVisible = rect.top >= 360 / window.devicePixelRatio && rect.bottom <= window.innerHeight;
        return isVisible ? { id: el.dataset['id'], top: rect.top } : null;
      })
      .filter((v): v is { id: string; top: number } => v !== null)
      .sort((a, b) => a.top - b.top);

    if (visible.length > 0) {
      this.zone.run(() => {
        this.playingItemId = visible[0].id.toString();
      });
    }
  }

  onContentItemClick(item: Content) {
    this.playingItemId = null;
    setTimeout(() => this.router.navigate(['/contents', item.CON_ID], {
      queryParams: { tab: 'content' },
    }), 100);
  }

  onContentCommentClick(event: Event, item: Content) {
    event.stopPropagation();
    this.playingItemId = null;
    setTimeout(() => this.router.navigate(['/contents', item.CON_ID,], {
      queryParams: { tab: 'comment' },
    }), 100);
  }

  onCommentIconClick(event: Event, item: Content) {
    event.stopPropagation();
    this.playingItemId = null;
    setTimeout(() => this.router.navigate(['/contents', item.CON_ID,], {
      queryParams: { tab: 'comment' },
    }), 100);
  }

  async sendHearts(event: Event, content: Content) {
    event.stopPropagation();
    console.log(content);
    const heartModal = await this.helper.alert({
      header: '하트 보내기',
      inputs: [
        {
          name: 'hearts',
          type: 'number',
          placeholder: '하트 입력(10~10,000)'
        }
      ],
      buttons: [
        {
          text: '취소',
          role: 'cancel'
        },
        {
          text: '확인',
          role: 'confirm',
          handler: (data) => data
        }
      ]
    });
    await heartModal.present();
    const { role, data } = await heartModal.onDidDismiss();

    if (role === 'confirm' && data?.hearts) {
      const hearts = data.hearts;

      const payload = {
        MEM_ID: this.auth.getUser().MEM_ID,
        CON_ID: content.CON_ID,
        PRS_ID: content.PRS_ID,
        HEART: hearts,
      }

      this.http.post("/api/heart", payload, { needToken: true }).subscribe({
        next: () => {
          this.showSuccess();
          content.HEART += parseInt(hearts);
        },
        error: this.showFailed,
      });
    }
  }

  async showSuccess() {
    const modal = await this.helper.alert({
      header: '성공',
      message: '하트를 성공적으로 보냈습니다.',
      buttons: ['확인']
    });
    await modal.present();
    await modal.onDidDismiss();
  }

  async showFailed() {
    const modal = await this.helper.alert({
      header: '실패',
      message: '하트 전송을 실패했습니다.',
      buttons: ['확인']
    });
    await modal.present();
    await modal.onDidDismiss();
  }

  async giveHeart(event: Event, heartCount: number, item: Content): Promise<void> {
    event.stopPropagation();
    try {
      const { MEM_ID, MEM_STAR, MEM_STAR_CHG } = this.auth.getUser();
      const { CON_ID } = item;

      if (MEM_STAR + MEM_STAR_CHG < heartCount) {
        const noPointAlert = await this.helper.alert({
          header: '앗! 별이 부족합니다.',
          message: '별똥별 이동해 보세요!',
          buttons: [
            { text: '아니오', role: 'cancel' },
            {
              text: '네',
              handler: () => this.router.navigate(['/more/charge-star']),
            },
          ],
        });
        return noPointAlert.present();
      }

      const heartResult = await this.contentService
        .postHeart({ MEM_ID, CON_ID, HEART: heartCount })
        .toPromise();

      item.HEART = heartResult.HEART;
      item.HEART_CHG = heartResult.HEART_CHG;

      // this.cm.setChange({
      //   action: 'changed',
      //   content: {
      //     CON_ID,
      //     HEART: heartResult.HEART,
      //     HEART_CHG: heartResult.HEART_CHG,
      //   },
      // });

      this.auth.updateUserProperty({
        MEM_STAR: heartResult.MEM_STAR,
        MEM_STAR_CHG: heartResult.MEM_STAR_CHG,
      });

      const sendHeartAlert = await this.helper.alert({
        cssClass: 'my-custom-class',
        header: '감사합니다!',
        message: `하트 ${heartCount} 개가 전달됐습니다.^^`,
        buttons: ['확인'],
      });
      sendHeartAlert.present();
    } catch (error) {
      this.helper.toast(error.message, 'middle');
    }
  }

  getPfp(item: any) {
    if (item.press === null) {
      return item.MEM_PICTURE
    }

    return item.press.STORED_FILE_NM
  }
}
