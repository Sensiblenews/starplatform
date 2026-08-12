package kr.co.sensiblenews.witchHuntingVU2D7F2P7E;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.splashscreen.SplashScreen;


import com.getcapacitor.BridgeActivity;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;


public class MainActivity extends BridgeActivity {

  WebView webView;
  private FrameLayout wrapper;

  private NativeAd currentNativeAd1;
  private NativeAd currentNativeAd2;
  private NativeAd currentNativeAd3;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    registerPlugin(NativeBridge.class);

    SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
    super.onCreate(savedInstanceState);
    registerPlugin(
      com.getcapacitor.community.facebooklogin.FacebookLogin.class);
    registerPlugin(com.getcapacitor.community.admob.AdMob.class);

    webView = this.bridge.getWebView();
    webView.setOnApplyWindowInsetsListener((v, insets) -> {
      int sbHeight = 0;
      int nbHeight = 0;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        sbHeight = insets.getInsets(WindowInsets.Type.statusBars()).top;
        nbHeight = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
      }

      ViewGroup.LayoutParams lp = v.getLayoutParams();
      if (lp instanceof ViewGroup.MarginLayoutParams mlp) {
        mlp.topMargin = sbHeight;
        mlp.bottomMargin = nbHeight;
        v.setLayoutParams(mlp);
      }

      return insets;

    });

    addAdWrapper();
    addAdOverlays();

    EdgeToEdge.enable(this);
  }

  private void addAdWrapper() {
    wrapper = new FrameLayout(MainActivity.this);
    wrapper.setId(R.id.ad_wrapper);
    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT
    );

    wrapper.setClickable(false);
    wrapper.setFocusable(false);

    int adTopMargin = (int) getResources().getDimension(R.dimen.ad_top_margin) + getStatusBarHeight(this);
    int adBotMargin = (int) getResources().getDimension(R.dimen.ad_bot_margin) + getNavigationBarHeight(this);

    params.topMargin = adTopMargin;
    params.bottomMargin = adBotMargin;

    getWindow().addContentView(wrapper, params);
  }

  private void addAdOverlays() {
    FrameLayout adContainer1 = new FrameLayout(this);
    FrameLayout adContainer2 = new FrameLayout(this);
    FrameLayout adContainer3 = new FrameLayout(this);
    setUpContainer(adContainer1, R.id.ad_container_1);
    setUpContainer(adContainer2, R.id.ad_container_2);
    setUpContainer(adContainer3, R.id.ad_container_3);
  }

  public void loadNativeAd(final FrameLayout adContainer, final int adId) {
//    AdLoader adLoader = new AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110")
    AdLoader adLoader = new AdLoader.Builder(this, "ca-app-pub-9109251900558498/4011939762")
      .forNativeAd(nativeAd -> {
        NativeAd oldAd = null;
        if (adId == R.id.ad_container_1) {
          oldAd = currentNativeAd1;
          currentNativeAd1 = nativeAd; // 새 광고 객체 저장
        }
        else if (adId == R.id.ad_container_2) {
          oldAd = currentNativeAd2;
          currentNativeAd2 = nativeAd; // 새 광고 객체 저장
        } else if (adId == R.id.ad_container_3) {
          oldAd = currentNativeAd3;
          currentNativeAd3 = nativeAd; // 새 광고 객체 저장
        }

        if (oldAd != null) {
          oldAd.destroy();
        }

        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        ConstraintLayout wrappingView = (ConstraintLayout) inflater.inflate(R.layout.ad_layout, adContainer, false);
        populateNativeAdView(nativeAd, wrappingView);
        runOnUiThread(() -> {
          adContainer.removeAllViews();
          adContainer.addView(wrappingView);
        });
      })
      .withAdListener(new AdListener() {
        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
          System.out.println("Native ad failed to load: " + loadAdError.getMessage());
        }

        @Override
        public void onAdClicked() {
          super.onAdClicked();
          Log.d("AdClick", "⚡️ Android Native Ad Click Detected!");

          // 웹뷰(Angular)로 "클릭 발생했다"는 이벤트를 쏘아줍니다.
          runOnUiThread(() -> {
            if (webView != null) {
              webView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('ad_click_detected'));",
                null
              );
            }
          });
        }
      })
      .withNativeAdOptions(new NativeAdOptions.Builder().build())
      .build();

    adLoader.loadAds(new AdRequest.Builder().build(), 2);
  }

  private void populateNativeAdView(NativeAd nativeAd, ConstraintLayout adView) {
    ImageView imageView = adView.findViewById(R.id.ad_media);

    if (nativeAd.getMediaContent() != null) {
      Drawable image = nativeAd.getMediaContent().getMainImage();
      imageView.setImageDrawable(image);
    } else {
      imageView.setVisibility(View.GONE);
    }

    TextView headlineView = adView.findViewById(R.id.ad_headline);
    headlineView.setText(nativeAd.getHeadline());
    ImageView iconView = adView.findViewById(R.id.ad_icon);
    if (nativeAd.getIcon() != null) {
      iconView.setImageDrawable(nativeAd.getIcon().getDrawable());
      iconView.setVisibility(View.VISIBLE);
    } else {
      iconView.setVisibility(View.GONE);
    }

    TextView advertiserView = adView.findViewById(R.id.ad_advertiser);
    if (nativeAd.getBody() != null) {
      advertiserView.setText(nativeAd.getBody());
      advertiserView.setVisibility(View.VISIBLE);
    } else {
      advertiserView.setVisibility(View.GONE);
    }

//    Button callToActionView = adView.findViewById(R.id.ad_call_to_action);
    NativeAdView ctaView = adView.findViewById(R.id.ad_call_to_action);
    if (nativeAd.getCallToAction() != null) {
      ctaView.setNativeAd(nativeAd);


      Button ctaButton = ctaView.findViewById(R.id.ad_call_to_action_button);
      ctaView.setCallToActionView(ctaButton);
      ctaButton.setText(nativeAd.getCallToAction());
      ctaButton.setVisibility(View.VISIBLE);
    }

    if (nativeAd.getMediaContent() != null && nativeAd.getMediaContent().hasVideoContent()) {
      nativeAd.getMediaContent().getVideoController().mute(true);
    }
  }

  private int getStatusBarHeight(Context context) {
    @SuppressLint("InternalInsetResource") int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
    return resourceId > 0 ? context.getResources().getDimensionPixelSize(resourceId) : 0;
  }

  private int getNavigationBarHeight(Context context) {
    @SuppressLint("InternalInsetResource") int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
    return resourceId > 0 ? context.getResources().getDimensionPixelSize(resourceId) : 0;
  }

  private void setUpContainer(FrameLayout container,int id) {
    DisplayMetrics metrics = getResources().getDisplayMetrics();
    int adHeight = (int) (315 * metrics.density);

//    container.setWebView(webView);
    container.setId(id);
    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      adHeight
    );

    params.gravity = Gravity.TOP;
    container.setClickable(false);
    container.setFocusable(false);

    wrapper.addView(container, params);
  }

  // 외부(NativeBridge)에서 호출
  public void triggerAdPlayback(int adId) {
    NativeAd targetAd = null;
    if (adId == R.id.ad_container_1) {
      targetAd = currentNativeAd1;
    }
    else if (adId == R.id.ad_container_2) {
      targetAd = currentNativeAd2;
    } else if (adId == R.id.ad_container_3) {
      targetAd = currentNativeAd3;
    }

    if (targetAd != null && targetAd.getMediaContent() != null && targetAd.getMediaContent().hasVideoContent()) {
      targetAd.getMediaContent().getVideoController().mute(true);
      Log.d("AdVisibility", "Ad container " + adId + " is visible. Triggering playback.");
    }
  }
}
