import { environment } from 'src/environments/environment';
import { AuthService } from 'src/app/services/auth.service';
import { Observable } from 'rxjs';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { Injectable } from '@angular/core';
/**
  publicURL && 로그인 했을 시 MEM_ID 붙여줌
 */
@Injectable()
export class MemberIntercepter implements HttpInterceptor {
  constructor(private auth: AuthService) {}
  intercept(
    req: HttpRequest<any>,
    next: HttpHandler,
  ): Observable<HttpEvent<any>> {
    if (!this.auth.isAuthed()) {
      return next.handle(req);
    }
    const publicAPIScheme = `${environment.apiBaseURL}/app/`;
    if (!req.url.includes(publicAPIScheme)) {
      return next.handle(req);
    }
    const apiName = req.url.split(publicAPIScheme)[1].split('?')[0];
    if (!apiName.toLowerCase().includes('content')) {
      return next.handle(req);
    }
    const interceptedReq = req.clone({
      url: `${req.url}&MEM_ID=${this.auth.getUserProperty('MEM_ID')}`,
    });
    return next.handle(interceptedReq);
  }
}
