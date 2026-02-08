import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpService } from '../../services/http.service';
// import { register } ... <-- 삭제

@Component({
  selector: 'app-feed-detail',
  templateUrl: './feed-detail.page.html',
  styleUrls: ['./feed-detail.page.scss'],
})
export class FeedDetailPage implements OnInit {
  conId: string;
  contentInfo: any = {};
  mediaList: any[] = [];
  
  starName: string = 'Unknown';
  starProfileImg: string = 'assets/img/defaultImg/avatar.svg';

  constructor(
    private route: ActivatedRoute,
    private http: HttpService
  ) {}

  ngOnInit() {
    this.conId = this.route.snapshot.paramMap.get('conId');
    this.loadFeedDetail();
  }

  loadFeedDetail() {
    this.http.post(`/api/super/feed/${this.conId}`, {}).subscribe((res: any) => {
      if (res.result === 'OK') {
        this.contentInfo = res.content;
        
        // [핵심] 받아온 미디어 리스트를 순회하며, 비디오일 경우 초기 음소거 상태(true)를 넣어줍니다.
        const rawMedias = res.medias || [];
        this.mediaList = rawMedias.map((media: any) => {
            if (media.MEDIA_TYPE === 'VIDEO') {
                media.isMuted = true; // 기본 음소거 상태 추가
            }
            return media;
        });
        
        if (res.content.PRS_NAME) this.starName = res.content.PRS_NAME;
        if (res.content.STORED_FILE_NM) this.starProfileImg = res.content.STORED_FILE_NM;
      }
    });
  }

  toggleSound(media: any, event: Event) {
    event.stopPropagation(); // 버블링 방지
    media.isMuted = !media.isMuted;
  }
  
  handleImgError(ev: any) {
    ev.target.src = 'assets/img/defaultImg/avatar.svg';
  }
}