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
    const query = event.target.value.toLowerCase(); // 입력된 검색어 (소문자 변환)

    if (query && query.trim() !== '') {
      // 검색어가 있으면: 원본(allStarsOriginal)에서 이름이 일치하는 것만 골라냄
      this.allStars = this.allStarsOriginal.filter((star) => {
        return (star.name.toLowerCase().indexOf(query) > -1);
      });
    } else {
      // 검색어가 없으면(지웠으면): 원본 데이터를 다시 보여줌
      this.allStars = this.allStarsOriginal;
    }
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