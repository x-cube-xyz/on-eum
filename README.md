# 온음 · on:eum

유튜브 영상 링크로 음원과 선택한 화질의 영상을 기기에 저장하고, 오프라인으로 재생하는 Android 앱입니다.

## 주요 기능

- 앱 내 YouTube 검색: 제목 / 아티스트 검색, 최대 20개 결과, 썸네일 / 채널 / 재생 시간 표시
- 검색 결과를 선택하여 음원 및 뮤비 다운로드 옵션으로 이동
- YouTube 일반 영상, Shorts, 라이브 다시보기, YouTube Music 및 공유 링크 입력
- MP3 음원 다운로드 및 앱 전용 로컬 보관함 저장
- 음원과 함께 MP4 영상 다운로드: **720p 또는 1080p** 선택
- 선택한 영상 해상도가 없는 경우 오류 표시, 임의 화질 변경 없음
- 다운로드 알림의 실제 퍼센트 / 진행 막대 / 음원 또는 영상 저장 단계 표시, 취소, 실패 후 재시도, 완료 기록 정리
- 로컬 보관함 검색, 최근 추가순 / 이름순 정렬, 좋아요, 뮤비 필터
- 재생 / 일시정지, 탐색, 이전 / 다음 곡, 셔플, 반복, 뮤비 재생
- Media3를 통한 백그라운드 재생, 잠금 화면 및 이어폰 제어
- 별도 색상표를 사용하는 다크 / 라이트 테마, 기기 설정 자동 적용
- 노치 / 시스템 내비게이션 / 키보드 안전 여백, 작은 화면과 큰 글자에 맞춘 줄바꿈 및 재생 화면 스크롤
- YouTube 앱의 공유 메뉴에서 링크 받기
- Android 파일 선택기를 통한 음원 및 영상 내보내기
- 첫 사용 및 24시간 간격의 yt-dlp 자동 갱신, HTTP 403 / 서명 오류 발생 시 갱신 후 한 차례 재시도
- 앱 내 yt-dlp 수동 업데이트, 알림 설정 바로가기

## 기술 구성

네이티브 Android 앱으로 작성되었습니다. Kotlin, Jetpack Compose, Material 3, Media3, WorkManager, Coil, [youtubedl-android](https://github.com/yausername/youtubedl-android)를 사용합니다. 웹 서버나 PC의 다운로드 서버 없이 기기에서 다운로드와 재생을 처리합니다. Tailwind 계열의 간결한 여백과 색상 구성을 Compose 테마로 구현했습니다.

- Android 8.0 / API 26 이상
- `arm64-v8a`, `armeabi-v7a`, `x86_64` 지원
- compile / target SDK 36
- JDK 17, Gradle 8.14.5, Android Gradle Plugin 8.12.0

## 빌드

Android Studio에서 프로젝트를 열거나 다음 명령을 사용합니다. Android SDK Platform 36 및 Build Tools 35.0.0 이상이 필요합니다.

1. JDK 17을 설치하고 `JAVA_HOME`을 설정합니다.
2. `ANDROID_HOME`을 설정하거나, `local.properties`에 SDK 위치를 지정합니다. Windows 예시는 `sdk.dir=C\:/Users/사용자/AppData/Local/Android/Sdk`입니다. 이 파일은 Git에 포함하지 않습니다.
3. 프로젝트 루트에서 빌드합니다.

```powershell
.\gradlew.bat :app:assembleDebug
```

macOS / Linux에서는 `sh gradlew :app:assembleDebug`를 사용합니다.

설치용 결과물은 다음 경로에 생성됩니다.

| 파일 | 용도 |
| --- | --- |
| `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk` | 일반적인 최신 Android 스마트폰 |
| `app/build/outputs/apk/debug/app-armeabi-v7a-debug.apk` | 32비트 ARM 기기 |
| `app/build/outputs/apk/debug/app-x86_64-debug.apk` | x86_64 에뮬레이터 |
| `app/build/outputs/apk/debug/app-universal-debug.apk` | 모든 지원 아키텍처를 포함한 APK |

Debug APK는 개발용 서명으로 설치할 수 있습니다. 정식 배포에는 소유자의 릴리스 키를 별도로 설정해야 합니다. FFmpeg, Python, JavaScript 런타임이 포함되므로 APK 크기가 일반적인 UI 앱보다 큽니다.

현재 버전은 **0.2.1**입니다. 기존 0.1.0 / 0.2.0과 같은 개발용 서명을 사용하므로 기존 앱 위에 업데이트할 수 있습니다.

## 검색 및 알림 사용

하단의 **검색**에서 노래 제목이나 아티스트를 입력합니다. 검색 결과의 영상을 선택하면 다운로드 화면에 링크와 영상 정보가 자동으로 입력됩니다. 음원만 저장하거나 뮤비를 함께 저장하도록 선택한 뒤 다운로드합니다. 기존 링크 입력 방식도 사용할 수 있습니다.

알림에는 곡 제목, 현재 저장 단계, 해당 단계의 다운로드 퍼센트가 표시됩니다. 엔진 확인 / 음원 변환 / 영상 병합처럼 정확한 퍼센트를 알 수 없는 단계는 처리 중으로 표시합니다. 음원과 뮤비는 각 단계의 진행률을 표시하므로 뮤비 다운로드를 시작하면 퍼센트가 다시 0부터 시작합니다. Android 13 이상에서는 알림 권한을 허용해야 알림창에서 확인할 수 있습니다. 설정의 **알림 설정 열기**로 권한을 변경할 수 있습니다.

## 저장 위치

기본 파일은 앱 전용 외부 저장소의 `Music/Oneum/<영상 ID>/`에 저장됩니다. 외부 저장소를 사용할 수 없는 경우 내부 저장소를 사용합니다. 별도의 전체 저장소 접근 권한은 요청하지 않습니다.

- `audio.mp3`: 음원
- `video-720.mp4` 또는 `video-1080.mp4`: 선택한 영상
- `files/library.json`: 내부 저장소의 보관함 인덱스, 좋아요, 추가 날짜

**앱 삭제 시 앱 전용 파일도 삭제됩니다.** 영구 보관할 파일은 보관함의 음악 메뉴에서 **음원 파일 내보내기 / 뮤비 파일 내보내기**로 다른 폴더에 저장하십시오. 영상 다운로드가 실패하거나 취소되어도 이미 저장을 완료한 음원은 보관함에 남습니다.

## 검증

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

단위 테스트는 유튜브 링크 정규화, 유사 도메인 및 비정상 링크 거부, 공유 텍스트 처리, 정확한 영상 해상도 선택을 검증합니다. 기기 테스트는 화면 이동, 입력 검증, 화질 선택, 로컬 재생과 탐색, 좋아요 저장, 테마 화면 캡처 및 네이티브 엔진 초기화를 검증합니다.

레이아웃 회귀 검사는 기기의 화면 크기와 글자 크기를 변경한 후 다음 명령으로 실행합니다. `ResponsiveLayoutTest`는 홈 버튼, 화질 선택, 키보드가 열린 상태의 다운로드 버튼, 재생 제어 및 대화상자 접근을 검증합니다. 가로 방향과 노치를 함께 확인하려면 프로필 이름을 `landscape-cutout`으로 지정합니다. 이 프로필은 실제 화면 방향과 노치 적용 여부도 검사합니다. 기본 검사에는 해당 설정이 필요하지 않습니다.

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.selfspotify.oneum.ResponsiveLayoutTest `
  -Pandroid.testInstrumentationRunnerArguments.oneum.layoutProfile=compact
```

화면 캡처 및 적용된 기기 설정은 앱의 내부 `files/layout-screenshots/`에 저장됩니다.

이 버전의 실행 결과는 [VALIDATION.md](VALIDATION.md)에 기록했습니다.

외부 네트워크를 사용하는 다운로드 검증은 기본적으로 제외됩니다. yt-dlp의 공개 테스트 영상을 실제 다운로드하려면 별도로 실행합니다.

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.selfspotify.oneum.DownloaderSmokeTest `
  -Pandroid.testInstrumentationRunnerArguments.oneum.network=true
```

뮤비까지 검증하려면 같은 명령에 `-Pandroid.testInstrumentationRunnerArguments.oneum.videoHeight=720` 또는 `1080`을 추가합니다. 다운로드 테스트에는 yt-dlp 저장소에서 사용하는 5초 테스트 영상(`gHKT4uU8Zng`)을 사용합니다.

유튜브의 요청 제한이나 영상 접근 제한이 있는 네트워크에서는 실제 다운로드 검증이 실패할 수 있습니다. 설정의 **엔진 업데이트 확인**에서 추출기를 갱신할 수 있습니다. 로그인 / 쿠키 가져오기, 제한된 영상 접근, 플레이리스트 일괄 다운로드는 구현 범위에 포함되지 않습니다.

검색과 실제 알림 갱신 및 제보된 403 오류의 영상은 다음 기기 테스트로 검증합니다.

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.selfspotify.oneum.SearchIntegrationTest `
  -Pandroid.testInstrumentationRunnerArguments.oneum.network=true
```

## 소스 구조

```text
app/src/main/java/com/selfspotify/oneum/
├── data/       로컬 파일 및 AtomicFile 기반 보관함
├── domain/     링크 검증과 영상 화질 선택
├── download/   yt-dlp / FFmpeg 초기화 및 WorkManager 작업
├── playback/   MediaSessionService 기반 재생
├── search/     YouTube 검색, 결과 파싱, 검색 취소 및 재시도
└── ui/         화면, 컴포넌트, 다크 / 라이트 테마
```

의존성 라이선스 및 소스 링크는 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)를 참조하십시오.
