package kr.co.sensiblenews.witchHuntingVU2D7F2P7E;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.splashscreen.SplashScreen;


import com.getcapacitor.BridgeActivity;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MediaContent;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;


public class MainActivity extends BridgeActivity {

  // 동영상 광고 진단 로그 태그 (2-32차). adb logcat -s AdMobVideo 로 모아 본다
  private static final String AD_LOG = "AdMobVideo";

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

    // 진단 로그 ① 하드웨어 가속 — 동영상 네이티브 광고의 전제 조건. 매니페스트 application 에 true 로 선언돼 있다
    boolean hwAccelerated = (getWindow().getAttributes().flags
      & WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED) != 0;
    Log.d(AD_LOG, "hardwareAccelerated=" + hwAccelerated);
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

  /**
   * 네이티브 광고 로드.
   *
   * @param landscapeOnly 로비 슬롯처럼 카드가 315dp 로 고정돼 미디어 상자가 가로로 긴 곳은
   *                      가로 크리에이티브만 요청한다. 세로·정사각 동영상이 오면 MediaView 가
   *                      자르지 않고 맞춰 넣기만 해서 양옆이 비고 작게 보였다(클라이언트 보고).
   */
  public void loadNativeAd(final FrameLayout adContainer, final int adId, final boolean landscapeOnly) {
//    AdLoader adLoader = new AdLoader.Builder(this, "ca-app-pub-3940256099942544/1044960115")
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

        // 진단 로그 ② 로드 성공 ③ mediaContent 유무 ④ 동영상 여부·비율·길이
        MediaContent mc = nativeAd.getMediaContent();
        Log.d(AD_LOG, "loaded container=" + adId + " headline=" + nativeAd.getHeadline()
          + " mediaContent=" + (mc != null));
        if (mc != null) {
          Log.d(AD_LOG, "hasVideoContent=" + mc.hasVideoContent()
            + " aspectRatio=" + mc.getAspectRatio() + " duration=" + mc.getDuration());
        }

        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        NativeAdView wrappingView = (NativeAdView) inflater.inflate(R.layout.ad_layout, adContainer, false);
        populateNativeAdView(nativeAd, wrappingView);
        runOnUiThread(() -> {
          adContainer.removeAllViews();
          adContainer.addView(wrappingView);

          // 진단 로그 ⑤ MediaView 실측 크기 — 0×0 이면 제약 문제, 보이지 않으면 레이어 문제
          MediaView mediaView = wrappingView.findViewById(R.id.ad_media);
          if (mediaView != null) {
            mediaView.post(() -> Log.d(AD_LOG, "mediaView size=" + mediaView.getWidth() + "x" + mediaView.getHeight()
              + " shown=" + mediaView.isShown()));
          }

          // 로드 성공을 웹에 알림 — 로비 슬롯이 이 이벤트를 받아야 플레이스홀더를 펼친다 (no-fill 시 공백 방지)
          if (webView != null) {
            webView.evaluateJavascript(
              "window.dispatchEvent(new CustomEvent('ad_loaded'));",
              null
            );
          }
        });
      })
      .withAdListener(new AdListener() {
        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
          Log.w(AD_LOG, "native ad failed container=" + adId + " code=" + loadAdError.getCode()
            + " msg=" + loadAdError.getMessage());
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
      // 동영상 크리에이티브는 음소거로 자동 재생한다 (피드 안 광고라 소리가 나면 안 된다).
      // 미디어 비율: 로비(315dp 고정)는 가로만, 스타 페이지(화면 폭+85 카드)는 모든 비율
      .withNativeAdOptions(new NativeAdOptions.Builder()
        .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
        .setMediaAspectRatio(landscapeOnly
          ? NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE
          : NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY)
        .build())
      .build();

    // 단건 로드. 예전의 loadAds(…, 2) 는 미디에이션이 붙은 광고 단위에서 동작하지 않는다(구글 문서)
    // — 2026-09 에 Liftoff·Meta·Pangle 을 붙인 뒤로 미디에이션 수요를 못 받고 있었을 수 있다.
    // 두 번째 응답이 첫 번째를 덮어쓰는 구조라 다중 로드의 이득도 없었다.
    adLoader.loadAd(new AdRequest.Builder().build());
  }

  /**
   * 광고 자산을 NativeAdView 에 등록한다 (2-32차 — AdMob 규격대로 재작성).
   *
   * 예전 코드는 getMainImage() 를 ImageView 에 넣어 동영상 광고가 정지 이미지로만 보였다.
   * MediaView 에 setMediaContent 를 하면 SDK 가 이미지·동영상을 알아서 그린다.
   * setNativeAd 는 자산 등록이 끝난 뒤 마지막에 불러야 노출·클릭이 정상 측정된다.
   */
  private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
    MediaView mediaView = adView.findViewById(R.id.ad_media);
    mediaView.setImageScaleType(ImageView.ScaleType.FIT_CENTER);
    adView.setMediaView(mediaView);
    if (nativeAd.getMediaContent() != null) {
      mediaView.setMediaContent(nativeAd.getMediaContent());
      mediaView.setVisibility(View.VISIBLE);
    } else {
      mediaView.setVisibility(View.GONE);
    }

    TextView headlineView = adView.findViewById(R.id.ad_headline);
    headlineView.setText(nativeAd.getHeadline());
    adView.setHeadlineView(headlineView);

    ImageView iconView = adView.findViewById(R.id.ad_icon);
    if (nativeAd.getIcon() != null) {
      iconView.setImageDrawable(nativeAd.getIcon().getDrawable());
      iconView.setVisibility(View.VISIBLE);
    } else {
      iconView.setVisibility(View.GONE);
    }
    adView.setIconView(iconView);

    // 광고주 자리에는 본문(body)을 넣어 왔다 — 기존 표시 그대로 두고 등록만 body 로 한다
    TextView advertiserView = adView.findViewById(R.id.ad_advertiser);
    if (nativeAd.getBody() != null) {
      advertiserView.setText(nativeAd.getBody());
      advertiserView.setVisibility(View.VISIBLE);
    } else {
      advertiserView.setVisibility(View.GONE);
    }
    adView.setBodyView(advertiserView);

    Button ctaButton = adView.findViewById(R.id.ad_call_to_action);
    if (nativeAd.getCallToAction() != null) {
      ctaButton.setText(nativeAd.getCallToAction());
      ctaButton.setVisibility(View.VISIBLE);
    } else {
      ctaButton.setVisibility(View.INVISIBLE);
    }
    adView.setCallToActionView(ctaButton);

    adView.setNativeAd(nativeAd);
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
//    container.setWebView(webView);
    container.setId(id);
    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      lobbyCardHeightPx()
    );

    params.gravity = Gravity.TOP;
    container.setClickable(false);
    container.setFocusable(false);

    wrapper.addView(container, params);
  }

  /** 로비 슬롯 카드 높이(px). 웹 플레이스홀더 315px·iOS 315pt 와 통일 */
  public int lobbyCardHeightPx() {
    return (int) (315 * getResources().getDisplayMetrics().density);
  }

  /**
   * 스타 페이지 카드 높이(px) = 화면 폭 + 85dp. iOS(MainViewController.adHeight)와 같은 공식이며
   * 웹 플레이스홀더 빈칸 박스(screenWidth + 85)와 같다. 예전에는 315dp 카드를 그 박스 가운데 띄워
   * 위아래가 놀고 미디어 상자가 가로로 눌려 세로·정사각 동영상이 작게 보였다.
   */
  public int starCardHeightPx() {
    DisplayMetrics metrics = getResources().getDisplayMetrics();
    float screenWidthDp = metrics.widthPixels / metrics.density;
    return (int) ((screenWidthDp + 85f) * metrics.density);
  }

  /** 페이지 모드에 따라 컨테이너 높이를 바꾼다 (NativeBridge.setShow 에서 호출) */
  public void setContainerHeight(FrameLayout container, int heightPx) {
    ViewGroup.LayoutParams lp = container.getLayoutParams();
    if (lp != null && lp.height != heightPx) {
      lp.height = heightPx;
      container.setLayoutParams(lp);
    }
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

    // 재생은 SDK 가 MediaView 안에서 알아서 한다(시작 음소거는 VideoOptions). 여기서는 가시성 진입만 기록한다
    if (targetAd != null && targetAd.getMediaContent() != null && targetAd.getMediaContent().hasVideoContent()) {
      Log.d(AD_LOG, "video ad container " + adId + " entered viewport");
    }
  }
}
