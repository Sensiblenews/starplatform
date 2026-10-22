package kr.co.sensiblenews.witchHuntingVU2D7F2P7E;

import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@CapacitorPlugin(name = "NativeBridge")
public class NativeBridge extends Plugin {
  private int initialPositionY1;
  private int initialPositionY2, initialPositionY3;

  /** 광고 컨테이너 수. 로비는 6개 모두(2-34차 3→6, 세로형 Latest Content 안 3개 추가), 스타 페이지는 1~3만 */
  static final int CONTAINER_COUNT = 6;

  FrameLayout adContainer1;
  FrameLayout adContainer2, adContainer3;
  // 2-34차 로비 전용 (세로형 Latest Content 안의 광고 3개)
  FrameLayout adContainer4, adContainer5, adContainer6;
  private boolean ad1Triggered = false;
  private boolean ad2Triggered = false;
  private boolean ad3Triggered = false;

  // 로비 모드: 위치를 하드코딩 수식이 아니라 웹이 보내는 좌표(setSlotPositions)로 제어한다.
  // 슬롯 6개(Today's TOP 위 / Discover 위 / New Creators 위 / 세로형 Latest Content 안 3개)가 컨테이너 1~6 에 1:1 로 대응한다
  private boolean lobbyMode = false;

  /**
   * 빠른 스크롤 중 페이드 상태 (2026-10-23 클라이언트 10번). 좌표가 브리지를 건너 늦게 적용되는 동안
   * 광고가 주변과 어긋나 떨려 보이므로 웹이 faded=true 를 보내면 투명하게 두고, 멈추면 제자리에서 되살린다.
   * 뷰는 그대로 붙어 있어 광고 로드·측정에는 영향이 없다
   */
  private boolean lobbyFaded = false;
  private static final long FADE_DURATION_MS = 120L;

  /** 웹이 보내는 로비 슬롯 한 개의 좌표 (css px) */
  private static final class SlotPosition {
    final int index;
    final float y;
    final float hideAbove;

    SlotPosition(int index, float y, float hideAbove) {
      this.index = index;
      this.y = y;
      this.hideAbove = hideAbove;
    }
  }



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

    // 3. 네이티브 광고 카드를 웹의 빈칸 박스(screenWidth + 85) 수직 중앙에 배치하기 위한 보정치.
    //    카드 높이를 iOS 와 같이 빈칸 박스와 똑같이(화면 폭+85) 잡으므로 보정치는 0 이 된다 — 식은 남겨둔다
    float placeholderTopMargin = 0f; // 슬롯 상단 여백 (margin 16 + padding 10)
    float placeholderBoxHeight = screenWidth + 85f;
    float cardHeight = screenWidth + 85f; // MainActivity.starCardHeightPx 와 같은 값(dp)
    float centeringOffset = (placeholderBoxHeight - cardHeight) / 2f; // 중앙 정렬

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

      if (adContainer1 == null || adContainer2 == null || adContainer4 == null) {
        adContainer1 = getActivity().findViewById(R.id.ad_container_1);
        adContainer2 = getActivity().findViewById(R.id.ad_container_2);
        adContainer3 = getActivity().findViewById(R.id.ad_container_3);
        adContainer4 = getActivity().findViewById(R.id.ad_container_4);
        adContainer5 = getActivity().findViewById(R.id.ad_container_5);
        adContainer6 = getActivity().findViewById(R.id.ad_container_6);
      }

      if (Boolean.TRUE.equals(bool)) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) adWrapper.getLayoutParams();
        params.topMargin = (int) getActivity().getResources().getDimension(R.dimen.ad_top_margin) + getStatusBarHeight(getContext());
        adWrapper.setLayoutParams(params);

        MainActivity activity = (MainActivity) getActivity();
        if ("lobby".equals(page)) {
          // 로비 모드: 슬롯 6개 모두 사용. 웹 좌표(setSlotPositions) 수신 전까지 화면 밖에 대기.
          // 카드는 315dp 고정이라 가로 크리에이티브만 요청한다
          lobbyMode = true;
          resetLobbyFade(lobbyContainers());
          int lobbyHeight = activity.lobbyCardHeightPx();
          for (FrameLayout c : lobbyContainers()) {
            activity.setContainerHeight(c, lobbyHeight);
            parkOffscreen(c);
          }
          ad1Triggered = false;
          ad2Triggered = false;
          ad3Triggered = false;

          FrameLayout[] lobby = lobbyContainers();
          int[] ids = MainActivity.adContainerIds();
          for (int i = 0; i < lobby.length; i++) {
            activity.loadNativeAd(lobby[i], ids[i], true);
          }
        } else {
          // 스타 페이지: 기존 3슬롯 하드코딩 좌표 경로 그대로. 카드는 iOS 와 같이 화면 폭+85
          lobbyMode = false;
          resetLobbyFade(lobbyContainers());
          int starHeight = activity.starCardHeightPx();
          for (FrameLayout c : starContainers()) {
            activity.setContainerHeight(c, starHeight);
            c.setClipBounds(null); // 로비 모드의 슬롯별 클리핑 해제
            c.setVisibility(View.VISIBLE);
          }
          // 로비 전용 컨테이너 4~6 은 스타 페이지에서 쓰지 않는다
          for (FrameLayout c : lobbyOnlyContainers()) {
            c.setClipBounds(null);
            c.setVisibility(View.GONE);
          }
          reinitializePosition();
        }
      } else {
        lobbyMode = false;
        resetLobbyFade(lobbyContainers());
        for (FrameLayout c : lobbyContainers()) {
          c.setClipBounds(null);
          c.setVisibility(View.GONE);
        }
      }
    });
  }

  // 로비 슬롯 위치 갱신 — 웹이 펼쳐진 슬롯마다 플레이스홀더의 뷰포트 기준 좌표(css px)와 클리핑 경계를 보낸다.
  // wrapper 상단은 가장 위 경계(헤더 하단)에 맞춰 광고가 헤더 밑으로 잘려 들어가게 하고,
  // 경계가 더 아래인 슬롯(sticky 탭 바 아래의 슬롯 3)은 컨테이너 자체의 clipBounds 로 그 차이만큼 상단을 더 잘라낸다.
  // 목록에 없는 슬롯(아직 no-fill 또는 로드 중)은 화면 밖에 둔다 — 전체 숨김으로 인한 빈 구멍 방지
  @PluginMethod()
  public void setSlotPositions(@NonNull PluginCall call) {
    call.setKeepAlive(false);
    final boolean faded = Boolean.TRUE.equals(call.getBoolean("faded", false));
    JSArray raw = call.getArray("slots");
    final List<SlotPosition> slots = new ArrayList<>();
    if (raw != null) {
      for (int i = 0; i < raw.length(); i++) {
        JSONObject item = raw.optJSONObject(i);
        if (item == null || !item.has("y")) {
          continue;
        }
        slots.add(new SlotPosition(
          item.optInt("index", -1),
          (float) item.optDouble("y", 0),
          (float) item.optDouble("hideAbove", 0)
        ));
      }
    }

    getActivity().runOnUiThread(() -> {
      if (!lobbyMode || adContainer1 == null) {
        return;
      }
      float density = getActivity().getResources().getDisplayMetrics().density;
      FrameLayout[] containers = lobbyContainers();

      // wrapper 경계 = 받은 경계 중 가장 위 (슬롯 1·2 의 헤더 하단). 아무 슬롯도 없으면 0
      float wrapperHideCss = 0f;
      boolean first = true;
      for (SlotPosition s : slots) {
        if (first || s.hideAbove < wrapperHideCss) {
          wrapperHideCss = s.hideAbove;
          first = false;
        }
      }

      // 웹뷰는 상태바 아래에서 시작하므로 css px→물리 px 변환에 상태바 높이를 더하면 화면 좌표가 된다
      FrameLayout adWrapper = getActivity().findViewById(R.id.ad_wrapper);
      int targetTop = (int) (wrapperHideCss * density) + getStatusBarHeight(getContext());
      FrameLayout.LayoutParams wp = (FrameLayout.LayoutParams) adWrapper.getLayoutParams();
      // 스크롤마다 호출되므로 값이 바뀔 때만 레이아웃을 건드린다 (requestLayout 은 프레임 비용이 크다)
      if (Math.abs(wp.topMargin - targetTop) > 1) {
        wp.topMargin = targetTop;
        adWrapper.setLayoutParams(wp);
      }

      boolean[] placed = new boolean[containers.length];
      for (SlotPosition s : slots) {
        if (s.index < 0 || s.index >= containers.length) {
          continue;
        }
        FrameLayout c = containers[s.index];
        if (c.getVisibility() != View.VISIBLE) {
          c.setVisibility(View.VISIBLE);
        }
        c.setTranslationY((s.y - wrapperHideCss) * density);

        // 이 슬롯만의 추가 경계(탭 바 하단)가 wrapper 경계보다 아래이고 카드 상단이 그 경계 위로 올라갔으면 그만큼 자른다.
        // clipBounds 는 뷰 자신의 좌표계라 translationY 와 무관하다. 같은 값이면 다시 설정하지 않는다(invalidate 절약)
        float extraClipPx = (s.hideAbove - s.y) * density;
        if (s.hideAbove > wrapperHideCss + 0.5f && extraClipPx > 0f) {
          int h = c.getLayoutParams() != null ? c.getLayoutParams().height : c.getHeight();
          int w = c.getWidth() > 0 ? c.getWidth() : adWrapper.getWidth();
          Rect clip = new Rect(0, (int) Math.min(extraClipPx, h), w, h);
          if (!clip.equals(c.getClipBounds())) {
            c.setClipBounds(clip);
          }
        } else if (c.getClipBounds() != null) {
          c.setClipBounds(null);
        }
        placed[s.index] = true;
      }
      for (int i = 0; i < containers.length; i++) {
        if (!placed[i]) {
          parkOffscreen(containers[i]);
        }
      }
      applyLobbyFade(containers, faded);
    });
    call.resolve();
  }

  /** 로비 슬롯 순서(0~5)대로의 컨테이너 배열 — MainActivity.adContainerIds() 와 같은 순서 */
  private FrameLayout[] lobbyContainers() {
    return new FrameLayout[]{ adContainer1, adContainer2, adContainer3, adContainer4, adContainer5, adContainer6 };
  }

  /** 스타 페이지가 쓰는 컨테이너 (기존 3슬롯 하드코딩 좌표 경로) */
  private FrameLayout[] starContainers() {
    return new FrameLayout[]{ adContainer1, adContainer2, adContainer3 };
  }

  /** 로비에서만 쓰는 컨테이너 (2-34차 세로형 Latest Content 안) */
  private FrameLayout[] lobbyOnlyContainers() {
    return new FrameLayout[]{ adContainer4, adContainer5, adContainer6 };
  }

  /** 로비 모드에서 좌표를 아직 못 받은(또는 광고가 없는) 컨테이너를 화면 밖에 둔다 */
  private void parkOffscreen(FrameLayout c) {
    if (c.getClipBounds() != null) {
      c.setClipBounds(null);
    }
    if (c.getVisibility() != View.INVISIBLE) {
      c.setVisibility(View.INVISIBLE);
    }
    c.setTranslationY(100000f);
  }

  /** 페이드 상태가 바뀔 때만 모든 로비 컨테이너의 알파를 짧게 애니메이션한다. 위치는 이미 맞춰진 뒤라 제자리에서 나타난다 */
  private void applyLobbyFade(FrameLayout[] containers, boolean faded) {
    if (faded == lobbyFaded) {
      return;
    }
    lobbyFaded = faded;
    float target = faded ? 0f : 1f;
    for (FrameLayout c : containers) {
      if (c == null) {
        continue;
      }
      c.animate().cancel();
      c.animate().alpha(target).setDuration(FADE_DURATION_MS).start();
    }
  }

  /** 로비 모드를 벗어나거나 광고를 숨길 때 페이드 상태를 원래대로 (스타 페이지는 페이드를 쓰지 않는다) */
  private void resetLobbyFade(FrameLayout[] containers) {
    lobbyFaded = false;
    for (FrameLayout c : containers) {
      if (c != null) {
        c.animate().cancel();
        c.setAlpha(1f);
      }
    }
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
    // 로비 모드에서는 위치를 setSlotPositions가 전담한다 (스타 페이지 수식 오염 방지)
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
      activity.loadNativeAd(adContainer1, R.id.ad_container_1, false);
      activity.loadNativeAd(adContainer2, R.id.ad_container_2, false);
      activity.loadNativeAd(adContainer3, R.id.ad_container_3, false);
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
