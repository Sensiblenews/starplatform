import { Router } from '@angular/router';
import { HelperService } from '../../services/helper.service';
import { Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { IonSearchbar, NavController } from '@ionic/angular';
import { ScrollSyncService } from 'src/app/services/scroll-sync.service';
import { MoveTabService } from 'src/app/services/move-tab.service';
import { CheckMessageService } from 'src/app/services/check-message.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
})
export class HeaderComponent implements OnInit, OnDestroy {
  @Input() pageTitle: string;
  @ViewChild('searchBar') searchBar: IonSearchbar;
  private startY = 0;
  public isSearchMode = false;
  private messageStateSubscription: Subscription;

  public newMessage = false;

  constructor(
    public navController: NavController,
    private helper: HelperService,
    private router: Router,
    private scrollService: ScrollSyncService,
    private moveTabService: MoveTabService,
    private checkMessageService: CheckMessageService,
  ) {}

  ngOnInit(): void {
    this.messageStateSubscription = this.checkMessageService.newMessageState$.subscribe(
      (hasNewMessage: boolean) => {
        this.newMessage = hasNewMessage;
      }
    );
  }

  ngOnDestroy(): void {
    if (this.messageStateSubscription) {
      this.messageStateSubscription.unsubscribe();
    }
  }

  changeIsSearchMode() {
    this.isSearchMode = !this.isSearchMode;
    if (this.isSearchMode) {
      setTimeout((ev) => {
        this.searchBar.setFocus();
      }, 500);
    }
  }

  movePeterPan(){
    this.router.navigate(['peter-pan'], {
    });
  }

  moveTinkerBell(){
    this.router.navigate(['tinker-bell'], {
    });
  }

  moveFollowers(){
    this.moveTabService.moveToNewTab('witch');
  }

  moveMonster() {
    this.router.navigate(['monster'], {});
  }

  searchByKeyword(event: KeyboardEvent): boolean {
    if(event.key === 'Enter'){
      const target = event.target as HTMLIonSearchbarElement;
      const keyword = String(target.value);

      if (!keyword.length) {
        this.helper.alert({
          header: '검색어가 없습니다.',
          subHeader: '검색어를 입력해주세요.',
        }).then(_ => _.present());
        return false;
      }

      target.blur();
      target.value = '';
      this.router.navigate(['/search-results'], {
        state: { keyword },
      });
      return true;
    }
    return false;
  }

  movePage() {
    this.navController.navigateForward('/more');
  }

  onTouchStart(event: TouchEvent) {
    this.startY = event.touches[0].clientY;
  }

  onTouchMove(event: TouchEvent) {
    const currentY = event.touches[0].clientY;
    const deltaY = this.startY - currentY; // 위로 스와이프하면 deltaY > 0

    this.startY = currentY; // 다음 move를 위해 갱신

    // 서비스로 delta 전달
    this.scrollService.emitScrollDelta(deltaY);
  }

  onTouchEnd(event: TouchEvent) {
    this.startY = 0;
  }
}
