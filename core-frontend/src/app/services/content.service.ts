import { Content, SendHeartResult } from 'src/app/types/Content';
import { HttpService } from './http.service';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import type { BlockUser, BlockUserResponse, Comment, ContentResponse } from '../types/Content';
@Injectable({
  providedIn: 'root',
})
export class ContentService {
  constructor(private http: HttpService) {}

  getInstDetail(detailId: number): Observable<ContentResponse> {
    const url = `/api/appInstDetail?index=${detailId}`;
    return this.http.post(url, null, {
      needToken: true,
    });
  }

  viewContentDetails(CON_ID: number): Observable<ContentResponse> {
    const url = `/api/content?CON_ID=${CON_ID}`;
    return this.http.get<ContentResponse>(url, {
      needToken: true,
    });
  }

  getFairyContents(page: number, count = 10): Observable<ContentResponse> {
    const offset = page * count;
    const url = `/app/fairyContents?offset=${offset}&limit=${count}`;
    return this.http.post<ContentResponse>(url, null, {
      needToken: true,
    });
  }

  getMonsterContents(page: number, count = 10): Observable<ContentResponse> {
    const offset = page * count;
    const url = `/app/monsterContents?offset=${offset}&limit=${count}`;
    return this.http.post<ContentResponse>(url, null, {
      needToken: true,
    });
  }

  getGoblinContents(page: number, count = 10): Observable<ContentResponse> {
    const offset = page * count;
    const url = `/app/goblinContents?offset=${offset}&limit=${count}`;
    return this.http.post<ContentResponse>(url, null, {
      needToken: true,
    });
  }

  getWitchContents(page: number, count = 10): Observable<ContentResponse> {
    const offset = page * count;
    const url = `/app/witchContents?offset=${offset}&limit=${count}`;
    return this.http.post<ContentResponse>(url, null, {
      needToken: true,
    });
  }

  getExclusiveContents(page: number, count = 10): Observable<ContentResponse> {
    const offset = page * count;
    const url = `/app/exclusiveContents?offset=${offset}&limit=${count}`;
    return this.http.post<ContentResponse>(url, null, {
      needToken: true,
    });
  }

  getStoryContents(
    category: number,
    page: number,
    count = 10,
  ): Observable<ContentResponse> {
    const offset = page * count;
    const url = `/app/storyContents?CON_CATEGORY=${category}&offset=${offset}&limit=${count}`;
    return this.http.post<ContentResponse>(url, null, {
      needToken: true,
    });
  }

  getFAQContents(page: number, count = 10): Observable<ContentResponse> {
    const offset = page * count;
    const url = `/app/FAQContents?offset=${offset}&limit=${count}`;
    return this.http.post<ContentResponse>(url, null, {
      needToken: true,
    });
  }

  getPromotionContents(page: number, count = 10): Observable<ContentResponse> {
    const offset = page * count;
    const url = `/app/promotionContents?offset=${offset}&limit=${count}`;
    return this.http.post<ContentResponse>(url, null, {
      needToken: true,
    });
  }

  getSearchResults(
    page: number,
    keywords: string,
    count = 10,
  ): Observable<ContentResponse> {
    const offset = page * count;
    const url = `/app/SearchResultContents?keyword=${encodeURI(
      encodeURIComponent(keywords),
    )}&offset=${offset}&limit=${count}&MEM_ID=2022031000001`;
    return this.http.post<ContentResponse>(url, null, {
      needToken: true,
    });
  }

  getNonriGooGooDan(page: number, count = 10): Observable<ContentResponse> {
    const offset = page * count;
    const url = `/app/NonRiContents?&offset=${offset}&limit=${count}&MEM_ID=2022031000001`;
    return this.http.post<ContentResponse>(url, null, {
      needToken: true,
    });
  }

  postContent(contentInfo: Content): Observable<ContentResponse> {
    const url = `/api/insertContents`;
    return this.http.post<ContentResponse>(url, contentInfo, {
      needToken: true,
    });
  }

  updateContent(contentInfo: Content): Observable<ContentResponse> {
    const url = `/api/updateContents`;
    return this.http.post<ContentResponse>(url, contentInfo, {
      needToken: true,
    });
  }

  postHeart(contentInfo: {
    [key: string]: number | string;
  }): Observable<SendHeartResult> {
    const url = `/api/heart`;
    return this.http.post<SendHeartResult>(url, contentInfo, {
      needToken: true,
    });
  }

  postComment(tempComment: Comment): Observable<Comment[]> {
    const url = `/api/comment`;
    return this.http.post(url, tempComment, {
      needToken: true,
    });
  }

  getComments(CON_ID: number): Observable<Comment[]> {
    const url = `/api/comments?CON_ID=${CON_ID}`;
    return this.http.post<Comment[]>(url, null, {
      needToken: true,
    });
  }

  updateComment(tempComment: Comment): Observable<Comment[]> {
    const url = `/api/commentMod`;
    return this.http.post<Comment[]>(url, tempComment, {
      needToken: true,
    });
  }

  deleteComment({ CON_ID, COM_ID }: Comment): Observable<Comment[]> {
    const url = `/api/comment?CON_ID=${CON_ID}&COM_ID=${COM_ID}`;
    return this.http.delete<Comment[]>(url, {
      needToken: true,
    });
  }

  getMyContents(): Observable<Comment[]> {
    const url = `/api/myContents`;
    return this.http.post<Comment[]>(url, {
      needToken: true,
    });
  }

  blockUser(blockInfo: BlockUser): Observable<BlockUserResponse> {
    const url = `/api/blockUser`;
    return this.http.post<BlockUserResponse>(url, blockInfo, {
      needToken: true,
    });
  }
}