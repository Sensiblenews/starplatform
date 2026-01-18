# Kakao Login

https://github.com/steadev/capacitor-kakao-plugin  

repo readme 랑 실제 패키지명이 다름
npm i capacitor-kakao-plugin 으로 설치,

**KAKAO Android 실행시 keystore 등록**  
관리자에 디버그 haskey가 등록되어 있어야 한다.
https://developers.kakao.com/docs/latest/ko/getting-started/sdk-android#add-key-hash-using-keytool

*installation*

```node
npm i capacitor-kakao-plugin  
npx cap sync
```

### android
---

app/src/main/AndroidManifest.xml 내에 주석으로 감싸진 부분 추가

```xml
  <!-- AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="io.ionic.starter">
  ...
  <!-- Add For Kakao Link (only if targeting Android 11) -->
  <queries>
    <package android:name="com.kakao.talk" />
  </queries>
  <!-- Add Kakao Link End-->
  ...
  <!-- Add Android Global Context -->
  <application 
    android:name=".GlobalApplication"
    ... >
    <!-- Add Metadata -->
    <meta-data
        android:name="com.kakao.sdk.AppKey"
        android:value="@string/kakao_app_key" />

    <!-- Add Auth Handler -->
    <activity 
        android:name="com.kakao.sdk.auth.AuthCodeHandlerActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.DEFAULT" />
            <category android:name="android.intent.category.BROWSABLE" />
            <data android:host="oauth"
                    android:scheme="@string/kakao_scheme" />
        </intent-filter>
    </activity>
    <!-- Add Auth Handler End -->

    <activity
            android:configChanges="orientation|keyboardHidden|keyboard|screenSize|locale|smallestScreenSize|screenLayout|uiMode"
            android:name="io.ionic.starter.MainActivity"
            android:label="@string/title_activity_main"
            android:theme="@style/AppTheme.NoActionBarLaunch"
            android:launchMode="singleTask">
      ...
      <!-- Add For Kakao Link -->
      <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
              android:host="@string/kakaolink_host"
              android:scheme="@string/kakao_scheme" />
      </intent-filter>
      <!-- Add For Kakao Link -->
      ...
    </activity>
  </application>
</manifest>
```

android/build.gradle 내에 카카오 repository 추가

```gradle
allprojects {
  repositories {
      google()
      jcenter()
      maven { url 'https://devrepo.kakao.com/nexus/content/groups/public/' }
  }
}
```

app/src/main/java/package 내 MainActivity 옆에 GlobalApplication.java 생성 후 코드 추가

```java
package 패키지명;
import android.app.Application;
import com.woot.plugins.kakao.CapacitorKakaoPlugin;

public class GlobalApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CapacitorKakaoPlugin.initKakaoSdk(this,getString(R.string.kakao_app_key));
    }
}
```

app/src/main/res/values/strings.xml 내에 카카오 정보 추가
```xml
<string name="kakao_app_key">{ANDROID_APP_KEY}</string>
<string name="kakao_scheme">kakao{ANDROID_APP_KEY}</string>
<string name="kakaolink_host">kakaolink</string>
````

### iOS
---

add dictionary to info.plist

```plist
  <dict>
  <array>
     <dict>
	<key>CFBundleURLSchemes</key>
   	<array>
	    <string>kakao{IOS_APP_KEY}</string>
	    <string>io.ionic.starter</string>
	</array>
     </dict>
  </array>
  
  <key>KAKAO_APP_KEY</key>
  <string>{IOS_APP_KEY}</string>
  <key>LSApplicationQueriesSchemes</key>
  <array>
     <string>kakao{IOS_APP_KEY}</string>
     <string>kakaokompassauth</string>
     <string>storykompassauth</string>
     <string>kakaolink</string>
     <string>storylink</string>
     <string>kakaotalk</string>
     <string>kakaotalk-5.9.7</string>
     <string>kakaostory-2.9.0</string>
  </array>
</dict>
```


add kakao initial code to AppDelegate.swift

```swift
import UIKit
import Capacitor
import KakaoSDKAuth
import KakaoSDKCommon

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
  
  ...
  
  func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
    
    	  // Initialize Kakao
        let key = Bundle.main.infoDictionary?["KAKAO_APP_KEY"] as? String
        KakaoSDK.initSDK(appKey: key!)
        // Initialize Kakao END
        
        return true
  }
  
  ...
  
  func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool {
      ...
    
    	  // Need for Login with KakaoTalk
        if (AuthApi.isKakaoTalkLoginUrl(url)) {
            return AuthController.handleOpenUrl(url: url)
        }
        // Need for Login with KakaoTalk END

        return ApplicationDelegateProxy.shared.application(app, open: url, options: options)
  }
  
  ...
}
```