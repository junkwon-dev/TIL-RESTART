## Spring Framework 7.0의 새로운 Resilience 기능: @Retryable과 @ConcurrencyLimit Deep Dive

### 서론

Spring Boot 4.0과 Spring Framework 7.0이 정식 출시를 앞두고 있습니다. M(Milestone) 버전과 스냅샷을 통해 새로운 기능들이 공개되면서 개발자들의 많은 기대를 받고 있습니다.

이번 릴리스에는 주목할 만한 여러 변경 사항이 포함되어 있습니다. 예를 들어, Spring Boot 4.0에서는 기존에 사용되던 `spring-boot-starter-web` 의존성이 `spring-boot-starter-webmvc`로 이름이 변경되었습니다. 이는 모듈의 역할을 명확히 하기 위한 개편의 일환입니다.

하지만 더 중요한 변화는 **Spring Framework 7.0** 자체에 있습니다. 기존에 `Spring Retry`라는 별도 프로젝트로 제공되던 **회복력(Resilience)** 관련 기능이 **`spring-core`** 모듈로 통합되었습니다.

이 글에서는 Spring Framework 7.0의 핵심 기능으로 자리 잡은 Resilience 기능 중, 가장 활용도가 높은 \*\*`@Retryable`\*\*과 **`@ConcurrencyLimit`** 어노테이션에 대해 실용적인 예제 코드와 함께 자세히 알아보겠습니다.

-----

### Spring Boot 4.0: Deprecated Starters

먼저 간단하지만 중요한 변경 사항인 Starter 의존성 이름 변경을 짚고 넘어가겠습니다. Spring Boot 4.0 마이그레이션 가이드에 따르면, 여러 Starter POM의 이름이 해당 모듈과 더 잘 부합하도록 변경되었습니다.

기존 Starter도 당분간 유지되지만, deprecated 되었으므로 향후 릴리스에서 제거될 예정입니다. `pom.xml` 또는 `build.gradle` 파일의 의존성을 다음과 같이 업데이트하는 것이 권장됩니다.

| Deprecated Starter | **Replacement (대체)** |
| :--- | :--- |
| `spring-boot-starter-oauth2-authorization-server` | `spring-boot-starter-security-oauth2-authorization-server` |
| `spring-boot-starter-oauth2-client` | `spring-boot-starter-security-oauth2-client` |
| `spring-boot-starter-oauth2-resource-server` | `spring-boot-starter-security-oauth2-resource-server` |
| `spring-boot-starter-web` | **`spring-boot-starter-webmvc`** |
| `spring-boot-starter-web-services` | `spring-boot-starter-webservices` |

`spring-boot-starter-web`이 `webmvc`로 변경된 것은, Spring이 WebFlux와 같은 반응형 웹 스택(Reactive Web Stack)과 구분하여 기존 서블릿 기반의 MVC 스택임을 명확히 하려는 의도로 보입니다.

-----

### Spring Framework 7.0의 새로운 Resilience 기능

본격적으로 Spring Framework 7.0의 Resilience 기능을 살펴보겠습니다. 이는 외부 시스템 장애나 일시적인 네트워크 문제 등, 예측 불가능한 상황에서 애플리케이션이 견고하게 동작하도록 돕는 패턴을 의미합니다.

기존에는 `Spring Retry` 라이브러리를 별도로 추가해야 했지만, 이제 Spring Core에 기본 내장되어 추가 의존성 없이 사용할 수 있습니다.

#### 1\. 예제 프로젝트 설정 (Getting Started)

먼저 Spring Boot 4.0.0-SNAPSHOT (Spring Framework 7.0.0-M7 기반)으로 프로젝트를 생성합니다.

**build.gradle.kts**

```kotlin
plugins {
    id("java")
    id("org.springframework.boot") version "4.0.0-SNAPSHOT"
    id("io.spring.dependency-management") version "1.1.7"
}

// ... (group, version, java toolchain)

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/snapshot") } // 스냅샷 저장소 필요
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc") // 변경된 webmvc 사용
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

Resilience 기능을 테스트하기 위해 간단한 컨트롤러와 서비스를 작성합니다.

**ResilienceService.java**

```java
@Service
public class ResilienceService {
    private static Logger log = LoggerFactory.getLogger(ResilienceService.class);

    private int retryCount = 0;
    private int concurrencyCount = 0;

    // 3의 배수일 때만 성공하는 메서드
    int retry() {
        this.retryCount++;
        log.info("retryCount {}", this.retryCount);

        if (this.retryCount % 3 != 0) {
            throw new RuntimeException("retryCount " + this.retryCount);
        }
        return this.retryCount;
    }

    // 동시성 제어가 없는 메서드
    int concurrency() {
        return concurrencyCount++;
    }
}
```

**ResilienceController.java**

```java
@RestController
public class ResilienceController {
    private final ResilienceService resilienceService;

    public ResilienceController(ResilienceService resilienceService) {
        this.resilienceService = resilienceService;
    }

    @GetMapping("/retry")
    Map<String, Object> retry() {
        return Map.of("code", this.resilienceService.retry());
    }

    @GetMapping("/concurrency")
    Map<String, Object> concurrency() {
        return Map.of("code", this.resilienceService.concurrency());
    }
}
```

현재 상태에서 `/retry` API는 3번 중 2번 실패하며, `/concurrency` API는 동시 요청 시 `concurrencyCount` 값의 무결성을 보장하지 못합니다.

-----

### 2\. @Retryable: 선언적 재시도

`@Retryable` 어노테이션은 메서드 실행 중 예외가 발생하면, 설정된 정책에 따라 자동으로 메서드를 다시 실행합니다.

#### 기능 활성화

이 기능을 사용하려면 메인 애플리케이션 클래스에 **`@EnableResilientMethods`** 어노테이션을 추가해야 합니다.

```java
@SpringBootApplication
@EnableResilientMethods // Resilience 기능 활성화
public class Spring7ResilienceApplication {
    public static void main(String[] args) {
        SpringApplication.run(Spring7ResilienceApplication.class, args);
    }
}
```

#### @Retryable 적용

이제 `ResilienceService`의 `retry()` 메서드에 `@Retryable`을 추가합니다.

```java
@Service
public class ResilienceService {
    // ... (logger,
    private int retryCount = 0;

    @Retryable // 어노테이션 추가
    int retry() {
        this.retryCount++;
        log.info("retryCount {}", this.retryCount);

        if (this.retryCount % 3 != 0) {
            throw new RuntimeException("retryCount " + this.retryCount);
        }
        return this.retryCount;
    }
    // ...
}
```

#### 결과 확인

서버를 재시작하고 `/retry` API를 호출하면, 이전과 달리 `RuntimeException`이 발생하지 않습니다. 대신 약 2\~3초의 지연 후 다음과 같은 정상 응답을 받게 됩니다.

```json
{"code":3}
```

서버 로그를 확인하면 그 이유를 명확히 알 수 있습니다.

```
INFO ... org.igooo.resilience.ResilienceService   : retryCount 1
INFO ... org.igooo.resilience.ResilienceService   : retryCount 2
INFO ... org.igooo.resilience.ResilienceService   : retryCount 3
```

`@Retryable`의 **기본 설정은 3번 시도(maxAttempts = 3)**, \*\*재시도 간격 1초(delay = 1000)\*\*입니다. 따라서 첫 번째 호출(retryCount 1)이 실패하고, 1초 후 두 번째 호출(retryCount 2)도 실패하고, 다시 1초 후 세 번째 호출(retryCount 3)에서 성공하여 정상 응답을 반환한 것입니다.

이러한 옵션은 어노테이션에서 직접 수정할 수 있습니다.

```java
@Retryable(maxAttempts = 5, delay = 100, jitter = 10, multiplier = 2)
```
-----

### 결론

Spring Framework 7.0은 `spring-retry`와 같은 외부 라이브러리 없이도 \*\*`@Retryable`\*\*과 \*\*`@ConcurrencyLimit`\*\*을 통해 강력한 **회복력(Resilience)과 동시성 제어** 기능을 프레임워크 핵심 수준에서 제공합니다.

* **`@Retryable`**: 일시적인 네트워크 오류나 외부 API 장애 시, 간단한 어노테이션 추가만으로 선언적인 재시도 로직을 구현하여 서비스의 안정성을 높입니다.
* **`@ConcurrencyLimit`**: 스레드에 안전하지 않은 코드 블록을 `synchronized` 없이도 간단하게 보호하여 데이터 정합성을 보장합니다.

Spring Boot 4.0과 Spring Framework 7.0의 정식 릴리스 이후, 이 기능들을 적극 활용하여 더욱 견고하고 안정적인 애플리케이션을 구축해 보시기를 권장합니다.