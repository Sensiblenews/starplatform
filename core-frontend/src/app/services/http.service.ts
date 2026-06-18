import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, retry } from 'rxjs/operators';
import { environment } from 'src/environments/environment';
import { APP_KEY } from '../constants/Keys';
import type { CreateHeaderOptions, RequestHeader } from '../types/http';

@Injectable({
  providedIn: 'root',
})
export class HttpService {
  protected userToken = new BehaviorSubject('');
  private retryCount = 0;
  private apiBaseURL = environment.apiBaseURL;
  // private appVersion = new BehaviorSubject(null);

  constructor(
    private http: HttpClient,
  ) { }

  setUserToken(token: string): Promise<string> {
    this.userToken.next(token);
    return Promise.resolve(token);
  }

  createHeader({
    contentType = 'url-encoded',
    needToken = false,
  }: CreateHeaderOptions): RequestHeader {
    let headers = new HttpHeaders();

    headers = headers.set('app_authorization', APP_KEY);

    switch (contentType) {
      case 'json':
        headers = headers.set('Content-Type', 'application/json');
        break;
      case 'multipart':
        break;
      case 'multipart-file':
        break;
    }

    headers = headers.set('Accept', 'application/json');

    if (needToken) {
      headers = headers.set('authorization', this.getToken());
    }

    return { headers };
  }

  getToken(): string {
    return this.userToken.getValue();
  }

  get<T>(
    path: string,
    headerOption: CreateHeaderOptions = {
      needToken: true,
    },
  ): Observable<T> {
    const header = this.createHeader(headerOption);
    return this.http
      .get<T>(`${this.apiBaseURL}${path}`, header)
      .pipe(retry(this.retryCount), catchError(this.errorHandler));
  }

  post<T>(
    path: string,
    param: any,
    headerOption: CreateHeaderOptions = {
      needToken: true,
    },
  ): Observable<T> {
    const header = this.createHeader(headerOption);
    return this.http
      .post<T>(`${this.apiBaseURL}${path}`, param, header)
      .pipe(retry(this.retryCount), catchError(this.errorHandler));
  }

  postFile<T>(
    path: string,
    formData: FormData,
    headerOption: CreateHeaderOptions = {
      needToken: true,
      contentType: 'multipart-file',
    },
  ): Observable<T> {
    const header = this.createHeader(headerOption);

    return this.http
      .post<T>(`${this.apiBaseURL}${path}`, formData, header)
      .pipe(retry(this.retryCount), catchError(this.errorHandler));
  }

  postMultipartFile(
    path: string,
    formData: FormData
  ) {
    const header = {
      headers: {
        'app_authorization': APP_KEY,
        'authorization': this.getToken(),
        'Accept': 'application/json',
      }
    }

    return this.http
      .post<any>(`${this.apiBaseURL}${path}`, formData, header)
      .pipe(retry(this.retryCount), catchError(this.errorHandler));
  }

  put<T>(
    path: string,
    param: any,
    headerOption: CreateHeaderOptions = {
      needToken: true,
    },
  ): Observable<T> {
    const header = this.createHeader(headerOption);
    return this.http
      .put<T>(`${this.apiBaseURL}${path}`, param, header)
      .pipe(retry(this.retryCount), catchError(this.errorHandler));
  }

  delete<T>(
    path: string,
    headerOption: CreateHeaderOptions = {
      needToken: true,
    },
  ): Observable<T> {
    const header = this.createHeader(headerOption);
    return this.http
      .delete<T>(`${this.apiBaseURL}${path}`, header)
      .pipe(retry(this.retryCount), catchError(this.errorHandler));
  }

  // Todo: add ErrorHandler
  errorHandler(error: any): Observable<never> {
    if (error.error instanceof ErrorEvent) {
      console.log('client Error', error);
    } else {
      console.log('server Error', error);
    }
    return throwError(error);
  }
}
