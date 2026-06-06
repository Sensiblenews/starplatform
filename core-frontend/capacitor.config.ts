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
    webContentsDebuggingEnabled: true
  },
  plugins: {
    SplashScreen: {
      launchAutoHide: false,
      showSpinner: false,
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
      resize: KeyboardResize.Body,
      style: KeyboardStyle.Dark,
      resizeOnFullScreen: true,
    },
    FirebaseAuthentication: {
      skipNativeAuth: false,
      providers: ['google.com', 'apple.com'],
    }
  }
};

export default config;
