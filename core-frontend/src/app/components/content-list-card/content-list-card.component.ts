import { take, switchMap } from 'rxjs/operators';
import {
  Observable,
  Subscription,
  BehaviorSubject,
  forkJoin,
  of,
  EMPTY,
} from 'rxjs';
import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { Content, ContentResponse } from 'src/app/types/Content';
import { CdkVirtualScrollViewport } from '@angular/cdk/scrolling';
import { IonInfiniteScroll } from '@ionic/angular';
import { ContentManagerService } from 'src/app/services/content-manager.service';
import { ContentType } from 'src/app/constants/ContentTypes';
import { Router } from '@angular/router';
import { HelperService } from 'src/app/services/helper.service';
import { SubscribeModalPage } from './modal/subscribe-modal.page';

@Component({
  selector: 'app-content-list-card',
  templateUrl: './content-list-card.component.html',
  styleUrls: ['./content-list-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContentListCardComponent implements OnInit, OnDestroy {

  @ViewChild(IonInfiniteScroll) infScroll: IonInfiniteScroll;
  @ViewChild(CdkVirtualScrollViewport) virtualView: CdkVirtualScrollViewport;

  @Input() viewType = 'default';
  @Input() contentType: ContentType;
  @Input() contentSubscriber: (
    page: number,
    itemCount: number,
  ) => Observable<ContentResponse>;
  @Input() itemCount = 10;

  public initialized: boolean;
  public contentAllLoaded: boolean;
  public enableRefresher = true;

  public defaultProfilePath = '../../../assets/img/defaultImg/thumbnail.svg';

  private pageContents: BehaviorSubject<Content[]> = new BehaviorSubject([]);
  private itemSizeMap = {
    default: 80,
    minimal: 54,
  };

  private currentPage = 0;
  private contentChangeSubscriber: Subscription;
  private defaultThumbnailPath = '../../../assets/img/defaultImg/thumbnail.svg';

  constructor(
    private cm: ContentManagerService,
    private router: Router,
    private helper: HelperService,
  ) { }

  public get contentLists(): Observable<Content[]> {
    return this.pageContents.asObservable();
  }

  get itemSize(): number {
    const itemSizeKey =
      this.viewType in this.itemSizeMap ? this.viewType : 'default';
    return this.itemSizeMap[itemSizeKey];
  }

  @HostListener('unloaded')
  ngOnDestroy(): void {
    if (this.contentChangeSubscriber) {
      this.contentChangeSubscriber.unsubscribe();
      this.contentChangeSubscriber = null;
    }
  }

  ngOnInit(): void {
    this.getContents().then((res) => {
      this.initialized = true;
      this.contentAllLoaded = res?.end;
    });

    this.contentChangeSubscriber = this.cm.changed.subscribe(
      ({ action, content }) => {

        console.log(action);
        console.log(content);

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

        if(action === 'goblin-writed') {
          // refresh contents
          this.refreshContents(null);
        }

        if(action === 'blocked') {
          this.refreshContents(null);
        }
      },
    );
  }
  getContents(option?: {
    reset: boolean;
  }): Promise<{ end: boolean; error: boolean }> {
    return new Promise((resolve) => {
      forkJoin([
        this.pageContents.pipe(take(1)),
        this.contentSubscriber(this.currentPage, this.itemCount),
      ]).subscribe(
        ([origin, received]) => {
          const { contents } = received;
          console.log("내용 보자 !");
          console.log(contents);
          if (contents.length) {
            if (option?.reset) {
              this.pageContents.next(contents);
            } else {
              const newList = origin.concat(contents);
              this.pageContents.next(newList);
            }
          }
          resolve({ end: contents?.length < this.itemCount, error: false });
        },
        (error) => {
          console.warn(error);
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
        this.pageContents.next(changedContent);
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
    if(event != null){
      event.target.complete();
    }
  }

  async loadMoreContents(event) {
    this.currentPage++;
    const { end } = await this.getContents();
    this.contentAllLoaded = end;
    event.target.complete();
  }

  checkUniqueContentId(index: number, content: Content) {
    return content.CON_ID ?? `no-${index}`;
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

  writerClick(event, prsId, memName){
    if(this.viewType === 'goblin'){
      event.stopPropagation();
      event.preventDefault();

      if(prsId){
        console.log(prsId);
        this.router.navigate(['profile-page'], {
          state: { prsId, memName },
        });
      }
    }
  }

  async onCardClick(item: any) { // Content 타입이지만 IS_SUBSCRIBED 접근 위해 any로 잠시 처리하거나 인터페이스 수정 필요
    
    // 1. 이미 구독 중이라면 -> 프로필 페이지로 이동
    if (item.IS_SUBSCRIBED === 1) {
      this.goToProfile(item);
      return;
    }

    // 2. 구독하지 않았다면 -> 구독 모달 띄우기
    const modal = await this.helper.callModal({
      component: SubscribeModalPage,
      componentProps: { 
        content: item 
      },
      cssClass: 'transparent-modal',
      backdropDismiss: true
    });

    modal.present();

    const { data } = await modal.onDidDismiss();
    
    // 3. 모달에서 구독 성공하고 돌아왔을 때
    if (data?.subscribed) {
      // 즉시 UI 상태 업데이트 (다음 클릭 시 프로필로 가도록)
      item.IS_SUBSCRIBED = 1;
      
      // 선택 사항: 구독하자마자 바로 프로필로 보내고 싶다면 아래 주석 해제
      // this.goToProfile(item);
      
      // 선택 사항 2: 구독하자마자 상세 컨텐츠로 보내고 싶다면 (기존 로직)
      // this.router.navigate(['/contents', item.CON_ID]);
    }
  }

  // [추가] 프로필 페이지 이동 메서드
  goToProfile(item: any) {
    const { PRS_ID, MEM_NAME, press } = item;
    let isNormalUser = 0;
    let pfp: string;

    // press 정보가 있으면 press 이미지, 없으면 유저 이미지
    if (press) {
      pfp = press.STORED_FILE_NM;
      isNormalUser = 0;
    } else {
      pfp = item.MEM_PICTURE;
      isNormalUser = 1;
    }

    if (PRS_ID) {
      this.router.navigate(['profile-page'], {
        state: { 
          prsId: PRS_ID, 
          memName: MEM_NAME, 
          isNormalUser: isNormalUser, 
          pfp: pfp 
        },
      });
    }
  }

  onProfileImgError(event: any) {
    event.target.src = this.defaultProfilePath;
  }
}
