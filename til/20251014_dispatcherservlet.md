# Spring 응답처리 과정 (DispatcherServlet 동작 원리)

## 서론

스프링 프레임워크로 웹 애플리케이션을 개발할 때, 사용자는 브라우저나 클라이언트에서 `HTTP 요청`을 보냅니다. 이 요청이 컨트롤러의 메서드까지 도달하기까지는 수많은 단계를 거치게 됩니다. 이러한 과정의 중심에는 **DispatcherServlet**이라는 핵심 컴포넌트가 있습니다.
DispatcherServlet은 요청을 받아 적절한 컨트롤러에 전달하고, 응답을 다시 클라이언트에게 반환하는 역할을 수행합니다.
이 글에서는 요청이 들어와 응답이 반환되기까지의 흐름을 시각적으로 정리하고, 각 단계의 역할과 동작 원리를 구체적으로 설명합니다.

---

## 본론

### 1. 전체 요청 흐름 개요

다음은 스프링 MVC의 요청-응답 흐름을 한눈에 볼 수 있는 다이어그램입니다.

```
Client → FilterChain → DispatcherServlet
          ↓
      HandlerMapping → HandlerAdapter → Controller
          ↓
   ReturnValueHandler → HttpMessageConverter / ViewResolver
          ↓
      HandlerExceptionResolver
          ↓
       Response Commit
```

| 단계 | 주요 컴포넌트                             | 역할 요약                  |
| -- | ----------------------------------- | ---------------------- |
| 1  | Filter                              | 보안, 로깅, 인코딩 등 공통 기능 처리 |
| 2  | DispatcherServlet                   | 중앙 진입점. 요청 위임 및 응답 조율  |
| 3  | HandlerMapping                      | 요청에 맞는 컨트롤러 탐색         |
| 4  | HandlerAdapter                      | 컨트롤러 호출 수행             |
| 5  | Controller                          | 비즈니스 로직 수행             |
| 6  | ReturnValueHandler                  | 반환값 타입 분석 및 처리         |
| 7  | HttpMessageConverter / ViewResolver | JSON 직렬화 또는 템플릿 렌더링    |
| 8  | ExceptionResolver                   | 예외 변환 및 처리             |
| 9  | Response Commit                     | 실제 HTTP 응답 전송          |

---

### 2. Filter 단계

Filter는 **서블릿 컨테이너 수준**에서 실행되는 전처리 단계입니다. 모든 요청이 DispatcherServlet에 도달하기 전에 필터 체인을 통과합니다.
주요 용도는 다음과 같습니다.

* 보안 인증 (예: Spring Security Filter Chain)
* 요청/응답 로깅
* CORS 및 인코딩 처리
* 특정 조건 시 요청 차단 또는 예외 처리

예시 그림:

```
[Client]
   ↓
[SecurityFilterChain]
   ↓
[DispatcherServlet]
```

필터는 cross-cutting concern을 처리하며, 필요시 요청을 조기 종료시킬 수도 있습니다.

---

### 3. DispatcherServlet: 요청의 중앙 진입점

DispatcherServlet은 모든 HTTP 요청의 진입점입니다. 요청이 들어오면 다음 순서로 내부 컴포넌트를 호출합니다.

1. HandlerMapping에게 적합한 핸들러(Controller 메서드)를 요청
2. HandlerAdapter에게 해당 핸들러 실행을 위임
3. HandlerExecutionChain을 통해 Interceptor 및 Controller 호출
4. 결과를 ReturnValueHandler로 전달하여 응답 생성
5. ViewResolver 또는 HttpMessageConverter를 통해 응답 본문 생성
6. 예외 발생 시 HandlerExceptionResolver 체인에 위임

---

### 4. HandlerMapping과 HandlerAdapter

HandlerMapping은 **URL, HTTP 메서드, 미디어타입 조건**을 기반으로 적절한 컨트롤러 메서드를 선택합니다.
대표 구현체는 `RequestMappingHandlerMapping`이며, `@RequestMapping` 또는 `@GetMapping` 등의 애노테이션 정보를 사용합니다.

HandlerAdapter는 선택된 핸들러를 실제로 실행시키는 역할을 담당합니다.
주로 `RequestMappingHandlerAdapter`가 사용되며, 실행 전후 다양한 부가 처리가 수행됩니다.

#### (1) 메서드 호출 전 처리

* HandlerInterceptor의 `preHandle()` 실행
* WebDataBinder를 통한 파라미터 → 객체 바인딩
* HandlerMethodArgumentResolver를 통한 인자 주입

  * @RequestParam, @PathVariable, @RequestBody, Principal 등
* Validator(@Valid, @Validated)를 통한 유효성 검증

#### (2) 메서드 호출 후 처리

* HandlerMethodReturnValueHandler가 반환값을 분석
* @ResponseBody 또는 ResponseEntity인 경우 → HttpMessageConverter로 JSON 직렬화
* 문자열(View 이름)인 경우 → ViewResolver로 템플릿 렌더링
* Interceptor의 postHandle, afterCompletion 실행

---

### 5. HttpMessageConverter

HttpMessageConverter는 **요청 본문과 객체 간 변환**을 담당합니다.

* @RequestBody → JSON → 객체 변환
* @ResponseBody → 객체 → JSON 변환

스프링 부트는 기본적으로 `MappingJackson2HttpMessageConverter`를 등록하며, Jackson을 이용해 JSON 변환을 수행합니다.

예시 코드:

```kotlin
@PostMapping("/user")
@ResponseBody
fun createUser(@RequestBody userRequest: UserRequest): UserResponse {
    return userService.create(userRequest)
}
```

이때 요청 본문(JSON)은 HttpMessageConverter를 통해 `UserRequest`로 역직렬화되고, 응답은 JSON 형태로 직렬화되어 반환됩니다.

---

### 6. 예외 처리 체인 (HandlerExceptionResolver)

요청 처리 중 발생한 예외는 `HandlerExceptionResolver` 체인으로 전달되어 처리됩니다.
대표 구현은 다음과 같습니다.

| Resolver                          | 설명                                              |
| --------------------------------- | ----------------------------------------------- |
| ExceptionHandlerExceptionResolver | @ExceptionHandler 또는 @ControllerAdvice 기반 예외 처리 |
| ResponseStatusExceptionResolver   | @ResponseStatus, ResponseStatusException 매핑     |
| DefaultHandlerExceptionResolver   | 스프링 내부 예외를 HTTP 상태코드로 변환                        |

이 과정을 통해 개발자는 통일된 예외 응답 포맷을 관리할 수 있습니다.

---

### 7. 비동기 요청 처리 (Async)

스프링 MVC는 Servlet 3.1 기반의 비동기 처리를 지원합니다.
컨트롤러가 `Callable`, `DeferredResult`, `CompletableFuture`를 반환하면 요청 스레드를 즉시 반환하고, 별도의 비동기 스레드에서 로직이 수행됩니다.

* WebAsyncManager가 비동기 수명주기를 관리
* 결과가 완료되면 DispatcherServlet이 다시 ViewResolver 또는 HttpMessageConverter를 통해 응답을 마무리합니다

---

### 8. JSON 응답이 기본으로 선택되는 이유

Accept 헤더가 `*/*`일 경우, 모든 미디어 타입을 수용할 수 있음을 의미합니다.
스프링 부트는 **MappingJackson2HttpMessageConverter(JSON)** 를 **우선순위 가장 앞에 등록**하기 때문에 기본 응답이 JSON으로 처리됩니다.
반대로 `@ResponseBody`가 없는 컨트롤러는 반환값을 **뷰 이름**으로 간주하여 ViewResolver로 전달되며, 템플릿을 찾지 못하면 예외가 발생합니다.

---

## 결론

DispatcherServlet은 스프링 MVC의 중심 축으로, 요청의 라우팅, 실행, 예외 처리, 응답 생성을 조율합니다.
각 단계는 명확한 책임을 가지고 분리되어 있으며, 이를 이해하면 스프링의 동작 원리를 깊이 있게 파악할 수 있습니다.

이 구조를 잘 이해하면 다음과 같은 이점이 있습니다.

* 요청 흐름 디버깅 및 성능 최적화에 유리함
* 인터셉터, 필터, 어댑터 등 확장 포인트를 정확히 제어 가능
* 커스텀 HandlerMethodArgumentResolver나 ExceptionResolver를 통한 유연한 확장 가능

결국, DispatcherServlet은 **스프링 MVC의 심장**이며, 이를 이해하는 것은 안정적이고 예측 가능한 웹 애플리케이션 개발의 출발점입니다.
