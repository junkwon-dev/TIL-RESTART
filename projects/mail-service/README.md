# Mail Service

메일 전송/수신 기능을 여러 메일 서비스 제공자(SendGrid, Mailgun, DirectSend 등)로 분산하여 사용할 수 있도록 설계된 데모 프로젝트입니다. 기능 플래그(Feature Flag)를 이용해 서비스별 트래픽 분배 비율을 동적으로 조정하고, 장애 시 재시도 및 대체 라우팅으로 가용성을 높이는 것을 목적으로 합니다.

- 프로젝트 목적
  - 여러 메일 서비스 제공자 간 트래픽 분산(리스크 분산, 비용 최적화, 기능 특화 활용)
  - Feature Flag를 통한 런타임 구성 변경(비율 조정, 토글)
  - 장애 발생 시 재시도 및 다른 서비스로 폴백
- 로컬 실행
  - 내장 H2 인메모리 DB를 사용하므로, 추가 DB 설치 없이 즉시 실행 가능
  - Gradle Wrapper로 실행: `./gradlew bootRun`

자세한 요구사항 배경은 `PRD.md`를 참고하세요.

---

## 기술 스택(Detected Stack)
- 언어: Kotlin 2.2.10
- 프레임워크: Spring Boot 4.0.0-M3 (WebMVC, Data JPA)
- 재시도: Spring Retry
- 데이터베이스: H2 인메모리 DB (JPA/Hibernate)
- 빌드/패키지 관리자: Gradle (Kotlin DSL)
- JDK: Java 24 Toolchain

참고 파일: `build.gradle.kts`, `settings.gradle.kts`, `src/main/resources/application.properties`

## 엔트리 포인트(Entry Points)
- 애플리케이션 메인: `src/main/kotlin/com/jun/mail/MailApplication.kt`
  - 클래스: `mailApplication`
  - 실행: `fun main(args: Array<String>) { runApplication<mailApplication>(*args) }`

## 요구 사항(Requirements)
- JDK 24 (Gradle Toolchain이 자동으로 맞춰줄 수 있으나, 로컬 JDK 21+/24 권장)
- Gradle Wrapper 사용 권장: `./gradlew`

## 설정(Configurations & Env Vars)
기본 설정은 `src/main/resources/application.properties`에 정의되어 있습니다.

```
spring.application.name=mail
spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```

- 로컬 실행은 위 기본값으로 충분합니다.
- 외부 메일 서비스 계정/키는 현재 코드에 직접 사용하지 않습니다. 실제 연동 시 아래를 참고해 환경 변수/시크릿을 추가하세요.
  - TODO: `SENDGRID_API_KEY`
  - TODO: `MAILGUN_API_KEY`, `MAILGUN_DOMAIN`
  - TODO: `DIRECTSEND_API_KEY`

## 빌드 & 실행(Setup & Run)
- 의존성 다운로드 및 빌드
  - `./gradlew build`
- 애플리케이션 실행
  - 개발 모드: `./gradlew bootRun`
  - 또는 JAR 실행: `./gradlew bootJar` 후 `java -jar build/libs/mail-0.0.1-SNAPSHOT.jar`
- 기본 포트: 8080

## 제공 API(Controllers)
- Mail API: `src/main/kotlin/com/jun/mail/controller/MailController.kt`
  - `POST /mail` — 메일 전송
    - Body: `{ "from": "...", "to": "...", "userId": 1, "content": "..." }`
  - `POST /mail/receive` — 메일 수신 흐름 트리거(데모)
    - Body: `SendMailCommand` 동일 스키마
  - `POST /mail/bulk` — 벌크 전송(1000건 테스트)
  - `GET /mail` — 발송 통계 조회(간단 문자열 통계)

- Feature Flag API: `src/main/kotlin/com/jun/mail/controller/FeatureFlagConfigController.kt`
  - `POST /feature-flag-config` — 플래그 생성 (예: `MAIL_SERVICE`/`RECEIVE_MAIL_SERVICE` 등)
  - `PUT /feature-flag-config/{id}` — 플래그 수정
  - `PUT /feature-flag-config/{id}/toggle` — 활성/비활성 토글
  - `GET /feature-flag-config` — 전체 조회
  - `DELETE /feature-flag-config/{id}` — 삭제

요청 예시는 저장소의 `test.http` 파일을 참고하세요.

## 동작 개요
- Feature Flag로 라우팅 키를 계산하여 서비스 선택
  - 예시: 옵션 `['sendgrid','sendgrid','mailgun','directsend']`이면 사용자 `userId % 옵션수`로 인덱스를 선택하여 해당 키로 매핑된 메일 서비스 빈을 사용
- 재시도 로직(Spring Retry)
  - 실패 시 재시도하며, 재시도 컨텍스트에서 모든 서비스에 순차 시도하여 성공 시 즉시 반환(간단 폴백)
- 로그 기록
  - 각 서비스 구현은 `MailSentLogRepository`를 통해 `MailSentLog` 엔터티를 저장 (H2)

## 스크립트(Scripts)
- `./gradlew bootRun` — 애플리케이션 실행
- `./gradlew build` — 빌드
- `./gradlew test` — 테스트 실행
- `./gradlew clean` — 빌드 산출물 삭제

## 테스트(Tests)
- 현재 별도의 테스트 코드 디렉터리는 비어 있습니다. 의존성(`spring-boot-starter-test`, `kotlin-test-junit5`)은 추가되어 있습니다.
  - 테스트 실행: `./gradlew test`
  - TODO: 단위/통합 테스트 추가
- `test.http` 파일로 HTTP 클라이언트(IDE 또는 REST Client)에서 수동 테스트가 가능합니다.

## 로컬 실행 가이드(Local Run)
1. 저장소 클론 후 프로젝트 루트에서 실행
   - `./gradlew bootRun`
2. 서버 기동 확인
   - `http://localhost:8080` (기본 포트 8080)
3. 기능 플래그 생성(예시)
   - `test.http`의 "피쳐 플래그 생성" 요청 사용
4. 메일 전송 및 통계 확인
   - `POST /mail`, `GET /mail` 요청으로 확인

## 프로젝트 구조(Project Structure)
```
.
├── PRD.md
├── README.md
├── build.gradle.kts
├── gradlew / gradlew.bat / gradle/wrapper
├── settings.gradle.kts
├── src
│   └── main
│       ├── kotlin
│       │   └── com/jun/mail
│       │       ├── MailApplication.kt
│       │       ├── application/MailingApplication.kt
│       │       ├── controller
│       │       │   ├── FeatureFlagConfigController.kt
│       │       │   └── MailController.kt
│       │       ├── domain
│       │       │   ├── command/*.kt
│       │       │   ├── entity/*.kt
│       │       │   ├── featureflagconfig/FeatureFlagConfigService.kt
│       │       │   └── mail/*.kt (Sendgrid, Mailgun, DirectSend 등 구현)
│       │       └── infrastructure/*.kt (Repository)
│       └── resources
│           └── application.properties
└── test.http
```

## 라이선스(License)
- TODO: 라이선스 명시 필요 (예: MIT, Apache-2.0 등). 명시 전까지는 기본적으로 사내/개인용으로만 사용하세요.

## 향후 과제(TODOs)
- 실제 외부 메일 서비스 연동 및 시크릿 관리(.env, Spring Config 또는 Vault 등)
- 테스트 코드 작성(단위/통합), 컨트롤러/서비스 레벨 검증
- 관측성 추가(로그 구조화, 메트릭/트레이싱)
- 에러 핸들링 고도화 및 재시도/서킷브레이커 정책 정교화
- 관리용 백오피스 UI 혹은 API 인증/인가
