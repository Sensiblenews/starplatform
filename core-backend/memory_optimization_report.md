# StarPlatform 서버 메모리 절감 및 고성능 인프라 최적화 분석 보고서
### (KT 클라우드 VM 자체 호스팅 및 오픈소스 기반 아키텍처)

본 보고서는 KT 클라우드 가상 머신(VM) 환경에서 클라우드 제공사 별도의 관리형 서비스(SaaS/PaaS 등)를 사용하지 않고, 오직 자체 설치한 오픈소스 패키지(Nginx, Redis, ffmpeg 등) 및 Java 8 백엔드 최적화만을 사용하여 **StarPlatform 백엔드(Java 8 + Spring 4 + eGovFrame 3.5.0 + Tomcat + MyBatis + MySQL)**의 메모리를 절감하고 성능을 극대화하는 방안을 제시합니다.

---

## 1. 인프라 구성 방향 (자체 호스팅 모델)

클라우드의 관리형 서비스(AWS S3, ElastiCache, Lambda 등)를 일체 사용하지 않는 제약 조건 하에, 모든 서비스는 KT 클라우드 VM 위에 수동 설치(Self-Hosted)하여 운영합니다.

* **Web/Proxy 서버**: KT 클라우드 VM에 **Nginx**를 직접 설치하여 리버스 프록시 및 대용량 정적 리소스(이미지, 동영상 등) 서비스를 담당하게 합니다.
* **Storage**: 별도의 S3 대신, KT 클라우드의 **블록 스토리지(Block Storage)**를 추가 구매하여 이미지/비디오 전용 디렉토리(`/var/lib/tomcat7/webapps/img`, `/video` 등)에 마운트하여 사용합니다.
* **Cache**: VM 내에 오픈소스 **Redis Server**(`redis-server`) 패키지를 직접 설치하여 메모리 캐시 및 세션 저장소로 활용합니다.
* **비디오/이미지 처리**: 외부 이미지 리사이징 API나 트랜스코더 서비스 대신, 서버 내부에서 Java 소스코드 및 **ffmpeg** CLI 명령어를 스케줄러를 통해 호출하여 로컬 가공 처리합니다.

---

## 2. 현 소스 코드 내 치명적인 메모리 취약점 및 개선 사항 (반영 완료)

인프라 튜닝에 앞서 WAS(Tomcat)의 OOM을 유발하던 심각한 소스 코드 수준의 메모리 누수를 이미 다음과 같이 수정 조치 완료했습니다.

### ① Multipart 업로드의 인메모리 버퍼 축소
* **파일**: [context-common.xml](file:///home/hagangmin/Documents/works/kmong/starplatform/core-backend/src/main/resources/META-INF/spring/context-common.xml)
* **내용**: 기존 **100MB**(`100000000`)의 `maxInMemorySize` 설정을 **256KB**(`262144`)로 크게 하향 조정했습니다.
* **효과**: 대형 파일 업로드 시 파일 전체가 JVM Heap에 상주하여 OOM을 발생시키던 문제를 차단하고, 256KB를 초과하는 데이터는 즉시 디스크 임시 파일로 격리하여 메모리를 비우도록 개선했습니다.

### ② 업로드 파일 스트리밍 복사 처리
* **파일**: [ImageUploadController.java](file:///home/hagangmin/Documents/works/kmong/starplatform/core-backend/src/main/java/com/sensible/admin/controller/ImageUploadController.java)
* **내용**: 다중 파일 업로드 시 업로드될 파일 크기 전체를 힙 메모리에 한 번에 할당하던 기존 로직(`new byte[file-size]`)을 **고정 8KB 크기의 버퍼 바이트 배열** 및 **try-with-resources** 문법을 적용한 스트림 순차 읽기/쓰기 구조로 전면 수정했습니다.
* **효과**: 파일 크기와 관계없이 항상 약 8KB의 미미한 메모리만 할당받아 쓰기 작업을 완료하며, 커넥션 유실 시 자원 누수를 완벽히 방지합니다.

---

## 3. 20단계 제안의 Java/eGovFrame 및 KT 클라우드 VM 맞춤형 매핑

| 단계 | 기존 제안 (Node.js/SaaS 기준) | **KT 클라우드 VM 자체 호스팅 구현 방안** | 난이도 / 효과 |
| :--- | :--- | :--- | :--- |
| **1~3** | Cloudflare DNS, CDN, Gzip/Brotli 압축 활성화 | 인프라 DNS를 Cloudflare로 위임하여 웹 트래픽 캐싱 및 압축을 적용합니다. 자체 VM 서버의 트래픽 부담을 Edge 단에서 차단하여 성능을 높입니다. (코드 수정 없음) | 쉬움 / **최상** |
| **4~5** | Nginx 리버스 프록시 및 정적 파일 직접 제공 | Tomcat 앞단에 **Nginx**를 Reverse Proxy로 구성합니다. 사용자가 요청하는 JS, CSS, 이미지, 동영상 경로를 Nginx가 가로채서 **로컬 디스크(/var/lib/tomcat7/webapps/img 등)에서 직접 응답**하게 설정하여 Tomcat의 스레드와 JVM 힙 소모를 완전히 방지합니다. | 보통 / **최상** |
| **6~7** | AWS S3 저장 및 WebP/썸네일 사용 | AWS S3 대신, **KT 클라우드 추가 블록 스토리지를 VM에 마운트**하여 대용량 파일을 분리 저장합니다. 업로드 시 Java 백엔드 단에서 `Thumbnailator`나 `webp-imageio` 라이브러리를 사용하여 이미지를 WebP로 자체 변환하고 썸네일을 생성한 후 스토리지에 씁니다. | 보통 / **상** |
| **8~9** | 동영상 HLS 스트리밍 및 Node.js Stream 사용 | 동영상 업로드 시 Spring의 비동기 스레드(`@Async`)를 사용해 로컬에 설치된 **ffmpeg** CLI 명령어를 실행하여 동영상을 HLS 프로토콜 형식(`.m3u8` 파일 및 `.ts` 조각 파일)로 즉시 분할 저장합니다. 클라이언트는 Nginx를 통해 인메모리 로드 없이 이 조각 파일들을 순차적으로 스트리밍 재생합니다. | 어려움 / **상** |
| **10~11** | 페이지네이션 및 무한 스크롤 적용 | MyBatis mapper XML 쿼리 수행 시 반드시 `LIMIT #{start}, #{length}`와 같은 페이징 처리를 보장합니다. 전체 목록 조회(Full Scan)를 전면 금지합니다. | 쉬움 / **상** |
| **12~13** | MySQL 인덱스 추가 및 필요한 컬럼만 조회 | 슬로우 쿼리를 모니터링하여 Where 절 및 Order By 절에 자주 쓰이는 컬럼에 인덱스를 부여합니다. MyBatis XML에서 `SELECT *` 사용을 지양하고, 필요한 컬럼만 명시하여 데이터 전송 및 매핑 메모리를 아낍니다. | 보통 / **상** |
| **14~15** | Redis 캐시 및 세션 저장소 도입 | KT 클라우드 VM에 **Redis Server** 패키지를 직접 설치(`sudo apt install redis-server`)합니다. 이후 **Spring Data Redis** 라이브러리를 통해 실시간 스타 랭킹 및 방문자 데이터를 Redis 메모리에 올려 DB 조회를 생략합니다. Tomcat의 클러스터링 및 세션 메모리 오버헤드를 막기 위해 세션을 Redis로 이관합니다. | 보통 / **상** |
| **16** | DB Connection Pool 최적화 | 현재 사용 중인 오래된 `Commons DBCP1`([context-datasource.xml](file:///home/hagangmin/Documents/works/kmong/starplatform/core-backend/src/main/resources/META-INF/spring/context-datasource.xml#L36))을 고성능·저메모리 커넥션 풀인 **HikariCP**로 교체하고, 최적의 pool size(기본 10~20) 및 Timeout 설정을 적용합니다. | 보통 / **상** |
| **17** | PM2 Cluster 사용 | Java/Tomcat 환경에서는 **Tomcat의 Executor(스레드 풀) 최적화** 및 JVM의 GC 옵션(예: 멀티코어 환경에 최적화된 **G1GC** 활성화 `-XX:+UseG1GC`)을 통해 멀티코어를 효율적으로 활용합니다. | 보통 / **중** |
| **18** | 백그라운드 작업 분리 | 이미지 변환, 푸시 알림 발송 등 무거운 작업은 Spring의 `@Async` 어노테이션과 커스텀 ThreadPoolTaskExecutor를 구성하여 백그라운드 스레드에서 비동기 처리하거나, RabbitMQ/Redis Queue를 둔 별도의 경량 배치 프로세스로 분리합니다. | 보통 / **상** |
| **19** | Cloudflare WAF 및 DDoS 방어 | Cloudflare 대시보드에서 WAF(웹 방화벽) 및 DDoS 방어(Under Attack 모드 등)를 설정하여 무차별 스팸 및 봇 요청이 WAS(Tomcat) 메모리를 점유하지 못하도록 에지(Edge) 단에서 차단합니다. | 쉬움 / **상** |
| **20** | 모니터링 (자체 모니터링 구축) | 유료 SaaS APM 대신, 오픈소스인 **Prometheus + Grafana** 스택을 KT 클라우드 내 모니터링 전용 VM에 설치하거나, Spring Boot Actuator 및 Micrometer 라이브러리를 활용해 JVM 내부 리소스(힙, 쓰레드, GC 횟수)의 시각화 파이프라인을 구축합니다. | 보통 / **상** |

---

## 4. 관리자용 메모리 제어 패널(System Controller Panel) 구현 분석

### ① 타당성 및 구현 범위 분석
관리자 페이지에서 시스템 성능 및 메모리 상태를 한눈에 모니터링하고 제어하는 패널 개발은 **매우 유용하며 기술적으로 충분히 구현 가능**합니다. (현재 구현 반영 완료)

이 패널은 다음과 같은 핵심 기능을 가집니다:
1. **실시간 JVM 메모리 상태 조회**: 전체/사용 중/남은 힙 메모리 정보 및 Non-Heap 메모리 상태 시각화.
2. **시스템 메모리 정리 실행하기 (Garbage Collection)**: 버튼 클릭 시 WAS JVM에 즉각적인 GC 요청 수행.
3. **어플리케이션 캐시 초기화**: Redis 캐시 또는 내부 메모리 캐시(Ehcache 등) 선택적 클리어.
4. **활성 커넥션 풀 상태 모니터링**: 현재 사용 중인 DB 커넥션 개수 조회.

### ② "메모리 정리 실행하기 (System.gc())"에 대한 전문가 경고 및 제언
* **작동 원리**: Java의 `System.gc()`를 호출하면 JVM에게 가비지 컬렉션을 즉시 실행하도록 **요청**합니다.
* **장점**: 개발 완료 후 메모리 누수가 의심되는 영역을 테스트하거나, 관리자가 야간 시간대 등 서비스 유휴 시간에 수동으로 사용되지 않는 메모리를 회수하여 서버 자원을 확보할 수 있습니다.
* **주의점 (중요)**:
  * JVM 환경에서 explicit GC(`System.gc()`) 호출은 **Stop-the-World (STW)**를 유발할 수 있습니다. 즉, 가비지 컬렉터가 작동하는 동안 WAS의 모든 스레드가 일시 정지되므로, 동시 접속자가 많을 때 호출하면 서비스 응답 지연이나 타임아웃 오류가 발생할 수 있습니다.
  * 일부 상용 JVM 기동 옵션 중 `-XX:+DisableExplicitGC`가 설정되어 있는 경우, 코드 상에서 `System.gc()`를 호출해도 아무런 동작을 하지 않습니다. 따라서 이 기능이 작동하려면 Tomcat startup 옵션을 체크해야 합니다.
* **권장 대안**: 관리자 수동 정리 버튼은 제공하되, 백그라운드에서는 JVM의 GC 방식 자체를 처리 속도와 일시 정지 시간이 가장 우수한 **G1GC(`-XX:+UseG1GC`)**로 변경하고 힙 크기(`-Xms`, `-Xmx`)를 최적화하여 둠으로써 JVM이 스스로 자동 청소하게 만드는 것이 가장 안전합니다.

---

## 5. 관리자 제어 패널 구현 설계 (반영 완료)

슈퍼 관리자(SM) 전용 기능으로 구현 완료되었으며, 주요 파일 경로는 다음과 같습니다.

* **백엔드 컨트롤러**: [SuperAdminController.java](file:///home/hagangmin/Documents/works/kmong/starplatform/core-backend/src/main/java/com/sensible/admin/controller/SuperAdminController.java#L806)
* **메뉴 아이템 통합**: [sidebar.jsp](file:///home/hagangmin/Documents/works/kmong/starplatform/core-backend/src/main/webapp/WEB-INF/jsp/super/sidebar.jsp#L45-L50)
* **대시보드 바로가기**: [dashboard_global.jsp](file:///home/hagangmin/Documents/works/kmong/starplatform/core-backend/src/main/webapp/WEB-INF/jsp/super/dashboard_global.jsp#L110)
* **뷰 화면 템플릿**: [system_panel.jsp](file:///home/hagangmin/Documents/works/kmong/starplatform/core-backend/src/main/webapp/WEB-INF/jsp/super/system_panel.jsp)

---

## 6. 결론 및 향후 로드맵 추천

관리형 클라우드 서비스 없이 KT 클라우드 VM에 자체 구축하여 시스템을 최적화하기 위한 구체적인 액션 아이템 로드맵입니다:

1. **[반영 완료] 코드 레벨 힙 메모리 최적화**:
   * CommonsMultipartResolver 크기 축소 및 Multipart 업로드 스트리밍 청크 구조 개편 적용 완료.
2. **[인프라 튜닝] Nginx 정적 서비스 전환 (1~2일 소요)**:
   * 마운트된 로컬 스토리지 볼륨의 이미지/비디오 폴더를 Nginx static block에 매핑하여 Tomcat WAS의 트래픽 부하 제거.
3. **[인프라 튜닝] 로컬 Redis 및 ffmpeg 연동 (2~3일 소요)**:
   * KT 클라우드 VM 내 `redis-server` 및 `ffmpeg` 바이너리 설치 및 Spring `@Async` 비동기 트랜스코딩 구현.
4. **[성능 고도화] HikariCP 적용 (1일 소요)**:
   * DBCP1 드라이버 제거 및 고성능 HikariCP 연동을 통해 커넥션 메모리 오버헤드 감소.
