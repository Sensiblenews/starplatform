package kr.co.sensiblenews.witchHuntingVU2D7F2P7E;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "NativeBridge")
public class NativeBridge extends Plugin {
  private int initialPositionY1;
  private int initialPositionY2, initialPositionY3;

  FrameLayout adContainer1;
  FrameLayout adContainer2, adContainer3;
  private boolean ad1Triggered = false;
  private boolean ad2Triggered = false;
  private boolean ad3Triggered = false;

  // 로비 모드: 위치를 하드코딩 수식이 아니라 웹이 보내는 좌표(setSlotPosition)로 제어한다
  private boolean lobbyMode = false;



  @Override
  public void load() {
    super.load();
    DisplayMetrics metrics = getActivity().getResources().getDisplayMetrics();
    float screenWidth = metrics.widthPixels / metrics.density;

    // 1. CSS 요소들의 정확한 높이 도출 (단위: dp)
    float headerProfileHeight = 178f; // 상단 헤더(43) + 프로필 영역(137)

    // 일반 피드 카드 1개의 전체 높이: 여백(16) + 사진(screenWidth) + 본문(37) + 푸터(48) + 코멘트창(53)
    float feedItemHeight = screenWidth + 136f;

    // 광고 슬롯 전체 높이: 여백(16) + 패딩(20) + 빈칸박스(screenWidth + 85)
    float adSlotHeight = screenWidth + 101f;

    // 2. 각 광고 슬롯의 시작점(Y 좌표) 계산
    // Ad 1 (배열 인덱스 3): 앞에 일반 피드 3개가 있음
    float slot1Y = headerProfileHeight + (feedItemHeight * 3);

    // Ad 2 (배열 인덱스 10): 앞에 일반 피드 9개 + Ad 1개 있음
    float slot2Y = headerProfileHeight + (feedItemHeight * 9) + (adSlotHeight * 1);

    // Ad 3 (배열 인덱스 15): 앞에 일반 피드 13개 + Ad 2개 있음
    float slot3Y = headerProfileHeight + (feedItemHeight * 16) + (adSlotHeight * 2);

    // 3. 네이티브 광고(315dp)를 웹의 빈칸 박스(screenWidth + 85) 수직 중앙에 배치하기 위한 보정치
    float placeholderTopMargin = 0f; // 슬롯 상단 여백 (margin 16 + padding 10)
    float placeholderBoxHeight = screenWidth + 85f;
    float centeringOffset = (placeholderBoxHeight - 315f) / 2f; // 중앙 정렬

    // 4. 최종 위치 적용
    initialPositionY1 = (int) ((slot1Y + placeholderTopMargin + centeringOffset) * metrics.density);
    initialPositionY2 = (int) ((slot2Y + placeholderTopMargin + centeringOffset) * metrics.density);
    initialPositionY3 = (int) ((slot3Y + placeholderTopMargin + centeringOffset) * metrics.density);
  }

  @PluginMethod()
  public void updateAdPosition(@NonNull PluginCall call) {
    call.setKeepAlive(false);
    Double scrollOffset = call.getDouble("value");

    if (scrollOffset != null) {
      this.updateAd(scrollOffset);
    }
  }

  @PluginMethod()
  public void setShow(@NonNull PluginCall call) {
    getActivity().runOnUiThread(() -> {
      Boolean bool = call.getBoolean("show");
      String page = call.getString("page");
      FrameLayout adWrapper = getActivity().findViewById(R.id.ad_wrapper);

      if (adContainer1 == null || adContainer2 == null) {
        adContainer1 = getActivity().findViewById(R.id.ad_container_1);
        adContainer2 = getActivity().findViewById(R.id.ad_container_2);
        adContainer3 = getActivity().findViewById(R.id.ad_container_3);
      }

      if (Boolean.TRUE.equals(bool)) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) adWrapper.getLayoutParams();
        params.topMargin = (int) getActivity().getResources().getDimension(R.dimen.ad_top_margin) + getStatusBarHeight(getContext());
        adWrapper.setLayoutParams(params);

        if ("lobby".equals(page)) {
          // 로비 모드: 슬롯 1개만 사용. 웹 좌표(setSlotPosition) 수신 전까지 화면 밖에 대기
          lobbyMode = true;
          adContainer1.setVisibility(View.INVISIBLE);
          adContainer1.setTranslationY(100000f);
          adContainer2.setVisibility(View.GONE);
          adContainer3.setVisibility(View.GONE);
          ad1Triggered = false;

          MainActivity activity = (MainActivity) getActivity();
          activity.loadNativeAd(adContainer1, R.id.ad_container_1);
        } else {
          // 스타 페이지: 기존 3슬롯 하드코딩 좌표 경로 그대로
          lobbyMode = false;
          adContainer1.setVisibility(View.VISIBLE);
          adContainer2.setVisibility(View.VISIBLE);
          adContainer3.setVisibility(View.VISIBLE);
          reinitializePosition();
        }
      } else {
        lobbyMode = false;
        adContainer1.setVisibility(View.GONE);
        adContainer2.setVisibility(View.GONE);
        adContainer3.setVisibility(View.GONE);
      }
    });
  }

  // 로비 슬롯 위치 갱신 — 웹이 플레이스홀더의 뷰포트 기준 좌표(css px)를 직접 보낸다.
  // wrapper 상단을 hideAbove(sticky 탭 바 하단)에 맞춰, 광고가 탭 바 밑으로
  // 스크롤될 때 잘려 나가듯 자연스럽게 클리핑되게 한다 (전체 숨김으로 인한 빈 구멍 방지)
  @PluginMethod()
  public void setSlotPosition(@NonNull PluginCall call) {
    call.setKeepAlive(false);
    Double y = call.getDouble("y");
    Double hideAbove = call.getDouble("hideAbove");
    if (y == null) {
      call.resolve();
      return;
    }
    final float yCss = y.floatValue();
    final float hideCss = hideAbove != null ? hideAbove.floatValue() : 0f;

    getActivity().runOnUiThread(() -> {
      if (!lobbyMode || adContainer1 == null) {
        return;
      }
      float density = getActivity().getResources().getDisplayMetrics().density;

      // 웹뷰는 상태바 아래에서 시작하므로 css px→물리 px 변환에 상태바 높이를 더하면 화면 좌표가 된다
      FrameLayout adWrapper = getActivity().findViewById(R.id.ad_wrapper);
      int targetTop = (int) (hideCss * density) + getStatusBarHeight(getContext());
      FrameLayout.LayoutParams wp = (FrameLayout.LayoutParams) adWrapper.getLayoutParams();
      if (Math.abs(wp.topMargin - targetTop) > 1) {
        wp.topMargin = targetTop;
        adWrapper.setLayoutParams(wp);
      }

      adContainer1.setVisibility(View.VISIBLE);
      adContainer1.setTranslationY((yCss - hideCss) * density);
    });
    call.resolve();
  }

  // 웹 랜딩 → 스토어 설치 경로로 전달된 리퍼러 문자열 조회 (디퍼드 딥링크용)
  @PluginMethod()
  public void getInstallReferrer(@NonNull PluginCall call) {
    final InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(getContext()).build();
    referrerClient.startConnection(new InstallReferrerStateListener() {
      @Override
      public void onInstallReferrerSetupFinished(int responseCode) {
        try {
          if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
            ReferrerDetails details = referrerClient.getInstallReferrer();
            JSObject ret = new JSObject();
            ret.put("referrer", details.getInstallReferrer());
            call.resolve(ret);
          } else {
            call.reject("Install referrer unavailable: " + responseCode);
          }
        } catch (Exception e) {
          call.reject("Install referrer error", e);
        } finally {
          referrerClient.endConnection();
        }
      }

      @Override
      public void onInstallReferrerServiceDisconnected() {
        // 첫 실행 1회 조회 용도이므로 재연결하지 않음
      }
    });
  }

  @PluginMethod()
  public void setViewportHeight(@NonNull PluginCall call) {
//    DisplayMetrics metrics = getActivity().getResources().getDisplayMetrics();
//    Integer height = call.getInt("height");
//    if (height != null) {
//      int viewportHeight = (int) (height * metrics.density);
//    }
  }

  private void updateAd(Double scrollOffset) {
    // 로비 모드에서는 위치를 setSlotPosition이 전담한다 (스타 페이지 수식 오염 방지)
    if (lobbyMode) {
      return;
    }

    if (adContainer1 != null  && adContainer2 != null && adContainer3 != null) {
      float density = getActivity().getResources().getDisplayMetrics().density;
      float offsetInPixels = scrollOffset.floatValue();
      float scrollDp = offsetInPixels * density;

      adContainer1.setTranslationY(initialPositionY1 - scrollDp);
      adContainer2.setTranslationY(initialPositionY2 - scrollDp);
      adContainer3.setTranslationY(initialPositionY3 - scrollDp);

      checkVisibilityAndTrigger();
    } else {
      Log.e("AdContainer", "adContainer is null");
    }
  }

  public void reinitializePosition() {
    adContainer1.setTranslationY(initialPositionY1);
    adContainer2.setTranslationY(initialPositionY2);
    adContainer3.setTranslationY(initialPositionY3);

    ad1Triggered = false;
    ad2Triggered = false;
    ad3Triggered = false;

    MainActivity activity = (MainActivity) getActivity();
    activity.runOnUiThread(() -> {
      activity.loadNativeAd(adContainer1, R.id.ad_container_1);
      activity.loadNativeAd(adContainer2, R.id.ad_container_2);
      activity.loadNativeAd(adContainer3, R.id.ad_container_3);
    });
  }

  private int getStatusBarHeight(Context context) {
    int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
    return resourceId > 0 ? context.getResources().getDimensionPixelSize(resourceId) : 0;
  }

  // 👇 가시성을 체크하고, 처음 화면에 들어왔을 때만 재생을 트리거하는 메서드
  private void checkVisibilityAndTrigger() {
    MainActivity activity = (MainActivity) getActivity();
    DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
    int screenHeight = metrics.heightPixels;

    // 광고 1 체크
    if (!ad1Triggered && isViewVisible(adContainer1, screenHeight)) {
      activity.triggerAdPlayback(R.id.ad_container_1);
      ad1Triggered = true; // 한 번만 트리거되도록 플래그 설정
    }

    // 광고 2 체크
    if (!ad2Triggered && isViewVisible(adContainer2, screenHeight)) {
      activity.triggerAdPlayback(R.id.ad_container_2);
      ad2Triggered = true;
    }

    // 광고 3 체크
    if (!ad3Triggered && isViewVisible(adContainer3, screenHeight)) {
      activity.triggerAdPlayback(R.id.ad_container_3);
      ad3Triggered = true;
    }
  }

  // 👇 뷰가 화면 안에 보이는지 수학적으로 계산하는 헬퍼 메서드
  private boolean isViewVisible(View view, int screenHeight) {
    if (view == null || view.getVisibility() != View.VISIBLE) {
      return false;
    }
    // 뷰의 화면상 실제 Y 좌표
    float viewTopY = view.getY();
    float viewBottomY = viewTopY + view.getHeight();

    // 뷰의 상단이 화면 하단보다 위에 있고, 뷰의 하단이 화면 상단보다 아래에 있으면 보이는 것
    return viewTopY < screenHeight && viewBottomY > 0;
  }
}
