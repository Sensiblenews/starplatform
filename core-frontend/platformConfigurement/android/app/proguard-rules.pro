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

# --- AdMob 미디에이션 어댑터 (2-29차 Liftoff Monetize) ---
# AdMob이 어댑터를 클래스 이름 문자열로 찾아 리플렉션으로 올리므로 난독화되면 No Fill이 된다.
# 어댑터 jar의 패키지가 두 갈래다 — com.google.ads.mediation.vungle(Bidding 구현체),
# com.vungle.mediation(Waterfall 진입점). 콘솔 설정에 따라 어느 쪽이든 조회될 수 있어 둘 다 지킨다.
# SDK 본체(com.vungle.ads)와 OMID는 vungle-ads AAR의 consumer 규칙이 이미 지킨다.
-keep class com.google.ads.mediation.** { *; }
-dontwarn com.google.ads.mediation.**
-keep class com.vungle.mediation.** { *; }

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

# --- Retrofit (카카오 SDK v2-network가 retrofit 2.9.0 사용) — 2026-09-06 Crashlytics 대응 ---
# 10.0.61/62(R8 첫 적용)에서 "retrofit2.DefaultCallAdapterFactory.get: Call return type must be parameterized" 급증.
# R8 full mode가 인터페이스 메서드의 제네릭 시그니처(Call<T>)를 지워서 나는 예외.
# retrofit 2.9.0의 consumer 규칙에는 full mode용 keep이 없다(2.11에서 추가됨) → 여기서 직접 넣는다.
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
# 카카오 SDK API 인터페이스는 메서드·시그니처까지 그대로
-keep interface com.kakao.sdk.**.*Api { *; }

# --- Capacitor 코어·플러그인 전체 keep — 2026-09-07 Crashlytics 대응 ---
# 10.0.63/64(R8 compat + Retrofit 규칙 빌드)에서 "Bridge.getPermissionStates(Bridge.java:1178) NullPointerException"
# (PluginHandle.invoke → Plugin.checkPermissions 경로, 삼성 기기). 플러그인 어노테이션(@CapacitorPlugin/@Permission)
# 정보가 난독화 과정에서 유실돼 annotation이 null이 되는 알려진 증상 — Ionic 포럼 해결책 그대로 적용.
# Capacitor 코드는 전체 앱에서 작은 비중이라 통째로 지켜도 난독화 비율에 영향이 거의 없다.
-keep class com.getcapacitor.** { *; }
-keep class com.capacitorjs.plugins.** { *; }
-keep class com.getcapacitor.community.** { *; }
-keep class io.capawesome.** { *; }
-keep class com.nerdfrenz.kakao.** { *; }
-keepclassmembers class * {
    @com.getcapacitor.annotation.Permission *;
    @com.getcapacitor.PluginMethod *;
}
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault
