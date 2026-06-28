# WITCH (core-backend)

Spring MVC 기반 백엔드 API 서버입니다.

## 기술 스택

| 항목 | 버전 |
|---|---|
| Java | 1.8 (OpenJDK 8) |
| Spring Framework | 4.1.1.RELEASE |
| 전자정부프레임워크 | 3.5.0 |
| MyBatis | 3.2.2 |
| MySQL Connector | 8.0.11 |
| Tiles | 3.0.3 |
| Tomcat (내장) | 7.x (Maven Plugin) |
| 빌드 도구 | Maven |
| 패키징 | WAR |

## 사전 요구 사항

- **JDK 8** (`/usr/lib/jvm/java-8-openjdk-amd64`)
- **Maven 3.8+** (`mvn --version`으로 확인)

> JDK 8이 설치되어 있지 않다면:
> ```bash
> sudo apt install openjdk-8-jdk
> ```

## ⚠️ 최초 설정: JDK 경로 확인

**JDK 8 설치 경로는 컴퓨터마다 다릅니다.** 프로젝트를 처음 클론한 후 반드시 `mvnw.sh`의 `JAVA_HOME` 경로를 본인 환경에 맞게 수정하세요:

```bash
# mvnw.sh 파일 내 아래 줄을 찾아서 수정
JAVA_HOME="/usr/lib/jvm/java-8-openjdk-amd64"  # ← 본인의 JDK 8 경로로 변경
```

JDK 8 경로 확인 방법:

```bash
# Linux / macOS
/usr/libexec/java_home -v 1.8  2>/dev/null || readlink -f $(which javac) | sed 's|/bin/javac||'

# 또는 직접 확인
ls /usr/lib/jvm/ | grep java-8
```

## 빌드 & 실행

`mvnw.sh` 래퍼 스크립트를 사용하면 `JAVA_HOME` 설정 없이 간편하게 실행할 수 있습니다:

```bash
./mvnw.sh <command>
```

### 명령어 요약

| 작업 | 명령어 |
|---|---|
| 컴파일 | `./mvnw.sh compile` |
| WAR 빌드 (테스트 스킵) | `./mvnw.sh build` |
| WAR 빌드 (테스트 포함) | `./mvnw.sh build:test` |
| 로컬 Tomcat 실행 | `./mvnw.sh run` |
| Clean | `./mvnw.sh clean` |
| 테스트 | `./mvnw.sh test` |
| 의존성 다운로드 | `./mvnw.sh deps` |
| 의존성 트리 | `./mvnw.sh deps:tree` |
| 도움말 | `./mvnw.sh help` |

등록되지 않은 명령어는 그대로 Maven에 전달됩니다:

```bash
./mvnw.sh clean install -Pprod
```

> **참고**: 로컬 실행(`run`) 시 포트 80을 사용합니다. 관리자 권한이 필요할 수 있으며, `pom.xml`의 `<port>80</port>`를 `8080` 등으로 변경할 수 있습니다.

### WAR 빌드 결과물

빌드 후 WAR 파일은 아래 경로에 생성됩니다:

```
target/witch-1.0.0-BUILD-SNAPSHOT.war
```

<details>
<summary>mvnw.sh 없이 직접 Maven 실행하기</summary>

모든 Maven 명령어 앞에 `JAVA_HOME`을 지정해야 합니다:

```bash
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 mvn clean package -DskipTests
```

또는 환경변수를 미리 설정하세요:

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
mvn clean package -DskipTests
```

</details>

## 프로젝트 구조

```
core-backend/
├── mvnw.sh                          # Maven 래퍼 스크립트
├── pom.xml                          # Maven 빌드 설정
├── lib/                             # 로컬 JAR 라이브러리 (Maven 로컬 레포)
│   └── jbit/core/1.0/               # jbit-core 커스텀 라이브러리
├── src/
│   ├── main/
│   │   ├── java/com/sensible/       # Java 소스 코드
│   │   ├── resources/
│   │   │   ├── META-INF/spring/     # Spring XML 설정 파일
│   │   │   ├── META-INF/sqlmap/     # MyBatis SQL 매퍼
│   │   │   ├── META-INF/props/      # 프로퍼티 파일
│   │   │   └── META-INF/tiles/      # Tiles 레이아웃 설정
│   │   └── webapp/
│   │       └── WEB-INF/
│   │           ├── web.xml          # 서블릿 배포 디스크립터
│   │           ├── config/spring/   # 서블릿 컨텍스트 설정
│   │           └── jsp/             # JSP 뷰 파일
│   └── test/                        # 테스트 코드
└── target/                          # 빌드 산출물 (gitignore)
```