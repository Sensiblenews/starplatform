package kr.co.sensiblenews.witchHuntingVU2D7F2P7E;

import android.app.Application;
import android.os.Build;
import android.webkit.WebView;

import com.kakao.sdk.common.KakaoSdk;
public class GlobalApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    KakaoSdk.init(this, getString(R.string.kakao_app_key));

    // 웹뷰 디버깅 활성화 코드
    WebView.setWebContentsDebuggingEnabled(true);
  }
}
