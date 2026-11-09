# WOOWANGTHON_TEAM_23_FE

프론트엔드 구현 레포

## 실행 방법

안드로이드 앱은 `android/` 폴더를 Android Studio로 열어 실행함. 빌드와 단위 테스트는 `android/`에서 `./gradlew testDebugUnitTest assembleDebug`로 함.

### 개발 서버에 연결

- 서버 주소는 `android/gradle.properties`의 `apiBaseUrl`로 정함. 기본값 `http://10.0.2.2:8080`은 에뮬레이터에서 PC(호스트)의 8080 포트에 닿는 주소임
- 서버(BE) 실행 방법은 BE 레포의 README 참고
- 실기기에서 PC의 서버에 붙으려면 `apiBaseUrl`을 PC의 같은 망 IP로 바꿈

### Android 17(API 37)에서 로컬 네트워크 권한

Android 17은 앱이 사설 주소(`10.0.2.2`, 같은 망 IP)에 접속할 때 `ACCESS_LOCAL_NETWORK` 권한을 요구함. 디버그 빌드의 매니페스트에는 선언해 두었지만, 설치 뒤에 권한을 허용해야 연결됨. 허용하지 않으면 연결이 10초 뒤 시간 초과로 실패하고 서버에는 요청이 찍히지 않음.

```
adb shell pm grant com.woowang.scrollmate android.permission.ACCESS_LOCAL_NETWORK
```

- 앱을 다시 설치하면 권한이 초기화되므로 다시 허용해야 함
- 배포는 터널의 공개 주소(https)를 쓰므로 이 권한이 필요 없고, 릴리스 빌드에는 선언하지 않음
