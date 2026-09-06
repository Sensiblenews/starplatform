# =========================================================
# 릴리스 빌드 R8 규칙 (2026-09-06)
#
# 배경: Play Console "앱 최적화" 경고 — 난독화 비율 2%. 릴리스에 minifyEnabled가 꺼져 있어
#       코드 축소·난독화가 전혀 안 됐다. 25% 미만이면 공개 상태에 영향을 줄 수 있다고 안내.
#
# 원칙: 라이브러리가 consumer 규칙으로 알아서 지키는 것(Capacitor·Firebase·Play Services·AdMob)은
#       중복하지 않고, 리플렉션·JS 브리지·매니페스트 밖에서 이름으로 찾는 것만 명시한다.
#       이 파일은 platformConfigurement/android/app/proguard-rules.pro 와 같아야 한다
#       (clean-build 시 android/ 가 재생성되고 스냅샷이 덮어쓴다).
# =========================================================

# --- 크래시 리포트 가독성 (Crashlytics가 mapping을 올리므로 이름은 숨기고 줄 번호만 남긴다) ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# --- 앱 자체 코드: Capacitor 플러그인(NativeBridge)은 @PluginMethod를 리플렉션으로 찾는다.
#     MainActivity/GlobalApplication은 매니페스트에 있어 유지되지만 광고 레이아웃이 참조하는
#     내부 클래스까지 한 번에 묶는다 (앱 코드는 3파일뿐이라 난독화 이득이 없다) ---
-keep class kr.co.sensiblenews.witchHuntingVU2D7F2P7E.** { *; }

# --- Capacitor 코어/플러그인 (consumer 규칙이 있지만 JS 브리지 메서드는 이름이 바뀌면 안 되므로 명시) ---
-keep @com.getcapacitor.annotation.CapacitorPlugin public class * {
    @com.getcapacitor.annotation.PermissionCallback <methods>;
    @com.getcapacitor.annotation.ActivityCallback <methods>;
    @com.getcapacitor.PluginMethod public <methods>;
}
-keep public class * extends com.getcapacitor.Plugin { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# --- Cordova 플러그인 (capacitor-cordova-android-plugins 모듈): 플러그인 클래스를 이름으로 로드한다 ---
-keep class org.apache.cordova.** { *; }
-keep class com.getcapacitor.cordova.** { *; }
-keep class cc.fovea.** { *; }
-keep class nl.xservices.plugins.** { *; }
-keep class de.appplant.cordova.** { *; }
-dontwarn org.apache.cordova.**

# --- 카카오 SDK v2 (공식 가이드 규칙) ---
-keep class com.kakao.sdk.**.model.* { <fields>; }
-keep class * extends com.google.gson.TypeAdapter
-keep interface com.kakao.sdk.**.*Api
-dontwarn com.kakao.sdk.**

# --- Google 네이티브 광고 템플릿 모듈 (레이아웃 XML에서 클래스 이름으로 참조) ---
-keep class com.google.android.ads.nativetemplates.** { *; }

# --- Kotlin 메타데이터 (카카오·AndroidX 내부에서 리플렉션) ---
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn org.jetbrains.annotations.**

# --- R8가 보고한 누락 클래스 (okhttp의 선택적 TLS 제공자 등, 앱에 없는 게 정상) ---
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
