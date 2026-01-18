package kr.co.sensiblenews.witchHuntingVU2D7F2P7E;

import android.app.Application;
import com.woot.plugins.kakao.CapacitorKakaoPlugin;

public class GlobalApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    CapacitorKakaoPlugin.initKakaoSdk(this, getString(R.string.kakao_app_key));
  }
}