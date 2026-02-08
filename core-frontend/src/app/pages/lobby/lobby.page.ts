import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { HttpService } from '../../services/http.service'; // HttpService 임포트 확인

@Component({
  selector: 'app-lobby',
  templateUrl: './lobby.page.html',
  styleUrls: ['./lobby.page.scss'],
})
export class LobbyPage implements OnInit {
  public defaultAvatar = 'assets/img/defaultImg/avatar.svg';
  
  // 데이터가 올 때까지 빈 배열로 초기화
  popularStars: any[] = [];
  allStars: any[] = [];

  allStarsOriginal: any[] = [];

  searchResults: any[] = [];
  isSearching: boolean = false;

  constructor(
    private router: Router,
    private http: HttpService // 서비스 주입
  ) {}

  ngOnInit() {
    this.loadLobbyData();
  }

  // 백엔드 API 호출: /api/super/lobby
  loadLobbyData(event?: any) {
    this.http.post('/api/super/lobby', {}).subscribe(
      (res: any) => {
        if (res.result === 'OK') {
          console.log('로비 데이터 갱신 완료');
          this.popularStars = res.popularStars || [];
          this.allStars = res.allStars || [];
          
          // 검색 원본 데이터도 최신으로 갱신
          this.allStarsOriginal = [...this.allStars]; 
        } else {
          console.error('데이터 로드 실패');
        }

        // [핵심] 새로고침 중이었다면 스피너 멈추기
        if (event) {
          event.target.complete();
        }
      },
      (error) => {
        console.error('API 에러:', error);
        // 에러가 나더라도 스피너는 멈춰줘야 함
        if (event) {
          event.target.complete();
        }
      }
    );
  }

  // [추가] HTML에서 호출하는 함수
  doRefresh(event: any) {
    console.log('새로고침 시작');
    // 위에서 만든 로드 함수에 event를 넘겨줌
    this.loadLobbyData(event);
  }

  filterStars(event: any) {
    const query = event.target.value.toLowerCase();

    if (query && query.trim() !== '') {
      this.isSearching = true; // 검색 모드 ON
      
      // 원본 데이터에서 검색하여 'searchResults'에 저장 (하단 리스트는 건드리지 않음)
      this.searchResults = this.allStarsOriginal.filter((star) => {
        return (star.name.toLowerCase().indexOf(query) > -1);
      });
    } else {
      this.isSearching = false; // 검색 모드 OFF
      this.searchResults = [];
    }
    
    // [옵션] 하단 리스트는 항상 전체 목록을 유지하도록 초기화
    // (이렇게 해야 상단엔 검색결과, 하단엔 전체목록이 유지됨)
    this.allStars = this.allStarsOriginal;
  }

  getStarImage(imageUrl: string | null): string {
    // 서버에서 이미지 경로가 전체 URL이 아니라 파일명만 온다면 앞에 도메인을 붙여야 할 수도 있습니다.
    // 예: return imageUrl ? `http://your-server.com/img/${imageUrl}` : this.defaultAvatar;
    return (imageUrl && imageUrl.length > 0) ? imageUrl : this.defaultAvatar;
  }

  handleImageError(event: any) {
    event.target.src = this.defaultAvatar;
  }

  goToStarPage(starId: string) {
    this.router.navigate(['/star', starId]);
  }
}