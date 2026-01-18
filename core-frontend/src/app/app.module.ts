import { MemberIntercepter } from './guards/MemberIntercepter';
import { PopupModalPageModule } from './modals/popup-modal/popup-modal.module';
import { TermsAgreementPageModule } from './modals/terms-agreement/terms-agreement.module';
import { ItemCountPopoverComponentModule } from './components/item-count-popover/item-count-popover.module';
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { RouteReuseStrategy } from '@angular/router';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { IonicModule, IonicRouteStrategy } from '@ionic/angular';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

import { InAppPurchase2 } from '@awesome-cordova-plugins/in-app-purchase-2/ngx';
import { EmailComposer } from '@awesome-cordova-plugins/email-composer/ngx';
import { WriteModalPageModule } from './modals/write-modal/write-modal.module';
import { SocialSharing } from '@awesome-cordova-plugins/social-sharing/ngx';
import { InAppBrowser } from '@awesome-cordova-plugins/in-app-browser/ngx';
import { ChatWriteModalPageModule } from './pages/list/witch/chat-write/chat-write-modal.module';
import { getApp, initializeApp, provideFirebaseApp } from '@angular/fire/app';
import { browserLocalPersistence, getAuth, indexedDBLocalPersistence, initializeAuth, inMemoryPersistence, provideAuth } from '@angular/fire/auth';
import { getFirestore, provideFirestore } from '@angular/fire/firestore';
import { Capacitor } from '@capacitor/core';
@NgModule({
    declarations: [AppComponent],
    imports: [
        BrowserModule,
        IonicModule.forRoot({
            mode: 'ios',
            innerHTMLTemplatesEnabled: true,
            // hardwareBackButton: true
        }),
        HttpClientModule,
        AppRoutingModule,
        ItemCountPopoverComponentModule,
        TermsAgreementPageModule,
        WriteModalPageModule,
        PopupModalPageModule,
        ChatWriteModalPageModule,
      // eslint-disable-next-line max-len
        provideFirebaseApp(() => initializeApp({
          "projectId":"push-44795",
          "appId":"1:1094374921494:web:c9273ff77b22e7eb2aec37",
          "storageBucket":"push-44795.firebasestorage.app",
          "apiKey":"AIzaSyD8k64GFDQD-6__HfIsqpTIw-R8ZzV5m7g",
          "authDomain":"push-44795.firebaseapp.com",
          "messagingSenderId":"1094374921494",
          "measurementId":"G-5B8C0G3C1B"
        })),
        provideAuth(() => {
          const app = getApp();

          // 네이티브 플랫폼(iOS/Android의 Capacitor WebView)
          if (Capacitor.isNativePlatform()) {
            return initializeAuth(app, {
              // iOS WKWebView에서 가장 안정적인 로컬 퍼시스턴스
              persistence: indexedDBLocalPersistence,
            });
          }

          // 일반 웹 브라우저
          try {
            return initializeAuth(app, { persistence: browserLocalPersistence });
          } catch {
            // 로컬 퍼시스턴스가 불가한 환경의 폴백
            return initializeAuth(app, { persistence: inMemoryPersistence });
          }
        }),
        provideFirestore(() => getFirestore()),
    ],
    providers: [
        { provide: RouteReuseStrategy, useClass: IonicRouteStrategy },
        { provide: HTTP_INTERCEPTORS, useClass: MemberIntercepter, multi: true },
        InAppPurchase2,
        EmailComposer,
        SocialSharing,
        InAppBrowser,
    ],
    bootstrap: [AppComponent],

})
export class AppModule {}
