import { CapacitorConfig } from '@capacitor/cli';
import { KeyboardResize, KeyboardStyle } from '@capacitor/keyboard';

let appId = 'kr.co.sensiblenews.witchHunting';

const config: CapacitorConfig = {
  appId,
  appName: 'Star Platform',
  webDir: 'www',
  bundledWebRuntime: false,
  loggingBehavior: 'debug',
  cordova: {
    accessOrigins: ['*'],
  },
  android: {
    appendUserAgent: "android:application",
    webContentsDebuggingEnabled: true,
    allowMixedContent: true
  },
  ios: {
    appendUserAgent: "ios:application",
    webContentsDebuggingEnabled: true,
    // [2-31차 후속] 실행 직후 깜빡임: 시스템 런치 화면(검정 스플래시) → 웹뷰 기본 흰색 → 스플래시 오버레이 순으로
    // 흰 화면이 한 프레임 비친다. 웹뷰 바탕을 스플래시와 같은 색으로 맞춘다 (android colors.xml splash_background_color)
    backgroundColor: '#111013'
  },
  plugins: {
    SplashScreen: {
      launchAutoHide: false,
      showSpinner: false,
      backgroundColor: '#111013',
      // only Android
      androidSplashResourceName: 'splash',
      androidScaleType: 'FIT_XY',
      splashFullScreen: false,
      splashImmersive: false,
      layoutName: 'launch_screen',
      // useDialog: true,
    },
    PushNotifications: {
      presentationOptions: ['badge', 'sound', 'alert'],
    },
    GoogleAuth: {
      scopes: ["profile", "email"],
      serverClientId: "1094374921494-j8drr5uml8ujvdfad1ikojique03ghnk.apps.googleusercontent.com",
      // 디버그용 clientId
      // androidClientId: "1094374921494-ej88hn0oo3pjss7bpgrm1bdhmeglg98r.apps.googleusercontent.com",
      // 릴리즈용 clientId
      // androidClientId: "1094374921494-90j19holrudcjkgsun93e7trrh1igo8g.apps.googleusercontent.com",
      iosClientId: "1094374921494-pgl0pildk5iebhp6anktag31ir384apb.apps.googleusercontent.com",
      forceCodeForRefreshToken: true
    },
    CapacitorHttp: {
      enabled: true,
    },
    Keyboard: {
      // resize 는 iOS 전용 설정 (Android 는 매니페스트 windowSoftInputMode 를 따른다)
      resize: KeyboardResize.Body,
      style: KeyboardStyle.Dark,
      // Android 전용. EdgeToEdge 환경에서 플러그인이 getWindowVisibleDisplayFrame 으로 콘텐츠 높이를 바꾸던 우회를 끈다.
      // 키보드 인셋은 MainActivity 의 인셋 리스너가 웹뷰 여백으로 직접 반영한다 (2026-10-23 첫 스크롤 튐 수정)
      resizeOnFullScreen: false,
    },
    FirebaseAuthentication: {
      skipNativeAuth: false,
      providers: ['google.com', 'apple.com'],
    }
  }
};

export default config;
