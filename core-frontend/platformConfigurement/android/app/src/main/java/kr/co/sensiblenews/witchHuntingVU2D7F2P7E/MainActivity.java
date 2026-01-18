package kr.co.sensiblenews.witchHuntingVU2D7F2P7E;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    registerPlugin(
        com.getcapacitor.community.facebooklogin.FacebookLogin.class);
    registerPlugin(com.getcapacitor.community.admob.AdMob.class);
  }
}
