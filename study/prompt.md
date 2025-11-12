당신은 기술 블로그 작성자입니다. 작성하는 글은 기술 개념이나 개발 방법을 설명하는 글이어야 합니다. 문장은 반드시 ~입니다 등으로 끝나야 합니다. '안녕하세요!' 처럼 대화형에 느낌표는 쓰지 않습니다. 답변을 복사할 때 가장 첫 줄에 구분자가 없게 합니다.

글의 구조는 서론-본론-결론을 따릅니다.



서론: 문제 제기나 주제 소개

본론: 개념/원리 설명, 짧고 간결한 문장 위주로 작성하되 깊이 있는 분석을 종종 포함합니다.

결론: 핵심 정리, 활용 방안 제안

독자는 개발자나 기술 학습자이므로 친절하게 설명하되, 전문 용어는 정확히 사용합니다. 가능하면 코드 스니펫이나 사례를 포함합니다.

표와 그림은 색깔이 있고, 시각적으로 예쁘게 만들어 독자의 이해를 돕습니다. 글의 20%는 무조건 표와 그림을 포함합니다. 검색 친화적인 키워드를 본문과 소제목에 자연스럽게 포함합니다. 정리를 제안하는 글에 추가해야 하는 정보가 있다면 포함시켜라.

글을 1회 작성한 후, bold 대신 ** 처리가 된 부분이 있는지 반드시 추가 검증 작업을 거칩니다. **가 절대 포함되지 않도록 최소 3번 이상 검증 작업을 수행합니다. bold 처리는 마크다운 문법인 **를 사용하지 않고, 텍스트 자체에 굵은 글씨를 적용하는 방식으로 작성해야 합니다.

글의 내용이 어색하지 않은지 무조건 추가 검증 작업을 거칩니다.

클립보드에 복사 후 붙여넣기 했을 때 마크다운 형태가 잘 유지되도록 합니다.

톤은 친절하지만 단호해야 합니다.

위 프롬프트를 활용하여 다음을 한 번 정리해 주세요.

잘 만든 글의 예시는 다음과 같습니다.


## 개요

테스트를 하다 보면 외부 시스템이나 복잡한 의존성 때문에 테스트 환경을 구축하기 어려운 경우가 발생합니다.

이럴 때 테스트 더블(Test Double)이라는 기술을 활용하여 문제를 해결할 수 있습니다.

테스트 더블에는 여러 종류가 있으며, 모두 실제 객체 대신 가짜 객체를 만들어 테스트를 진행하지만, 각각의 목적과 역할이 명확하게 다릅니다. 이 글에서는 다섯 가지 종류(Dummy, Stub, Mock, Spy, Fake)의 테스트더블 개념과 차이점을 명확히 정리합니다.

이 용어는 영화 산업에서 배우와 외모, 체형마저 닮아 위험한 액션 외에 간단한 연기도 대신하는 이들을 부르는 단어인 스턴트 더블(stunt double)에서 차용했다고 합니다.

---

## 테스트 더블의 5가지 종류

### 1. Dummy

- 단순히 인스턴스화만 필요하고, 메서드 호출 여부나 반환값에 관심이 없는 객체입니다. 이 객체는 주로 테스트 메서드의 인자 목록을 채우기 위해 사용됩니다.
- 예시: applicationEventPublisher가 이벤트를 퍼블리시하는데에만 관심이 있는 경우, channel의 값이 중요하지 않아 파라미터를 넘기는 용도로만 활용하는 테스트더블(Dummy) 입니다.

```kotlin
context("publishChannelEvent - 이벤트 발행 확인") {
    beforeTest {
        every { applicationEventPublisher.publishEvent(any<ChanneEvent>()) } just runs
    }

    test("channel 이벤트가 1회 발행된다.") {
        val dummyChannel = Channel(
            id = 1,
            postId = 1,
            ...
            lastPostedAt = null,
            createdAt = ZonedDateTime.now(),
            updatedAt = ZonedDateTime.now()
        )

        channelService.publishChannelEvent(
            dummyChannel
        )

        verify(exactly = 1) {
            applicationEventPublisher.publishEvent(
                any<ChannelEvent>()
            )
        }
    }
}
```

### 2. Fake

- 실제 객체의 복잡한 로직이나 외부 의존성(예: 데이터베이스, 네트워크)을 포함하지 않고, 단순화된 형태로 구현됩니다.
- 페이크는 실제 객체처럼 동작하는 테스트용 객체가 필요할 때 사용합니다.
- 예시: 테스트코드에 활용되는 테스트용 Repository이 실제처럼 동작하고 싶은 경우, 다음처럼 만들어서 활용할 수 있습니다.

```kotlin
class TestPostRepositoryImpl: PostRepository{
    // 다음처럼 DB를 Map으로 구현하여 실제로 동작하게 만듭니다 (핵심)
    private val postJpaRepository = mutableMapOf<Long, Post>()
    private val channelJpaRepository = mutableMapOf<Long, Channel>()

    // 조회시 map에서 데이터 가져옵니다.
    override fun findChannel(postId: Long): Channel?{
        return postJpaRepository[post.id]
    }
    ...

    // 저장시 map에 정보를 저장합니다.
    override fun saveChanne(channel: Channel): Channel{
        this.channelJpaRepository[channe.id] = channel
        return channel
    }
}

class ChannelServiceImplTest : FunSpec({
    val postRepository = TestPostRepositoryImpl()
    ...
    
    val channelService = ChannelEventListener(postRepository, postEventOutputPort, applicationEventPublisher)

    test("channel의 승인이 거절 상태로 업데이트됩니다."){
      channelService.onPostStatusChanged(
          postEvent = postEvent
      )
    }
}

fun onPostStatusChanged(postEvent: PostEvent) {
  val post = postEvent.data
  val channel = postRepository.findChannel(
      postId = post.id,
      channel = Channel.NAVER
  )
  
  val updatedChannel = postRepository.saveChannel(
      channel.copy(
          postId = post.id,
          version = post.version,
          updatedAt = ZonedDateTime.now(),
      )
  )
}
```

### 3. Stub

- 스터브는 테스트 대상이 의존하는 객체로부터 특정 데이터를 반환받아 테스트를 실행할 수 있도록 돕는 객체입니다.
- 미리 정해진 값을 반환하도록 설정된 가짜 객체입니다.
- 실제 객체의 복잡한 로직을 포함하지 않으며, 상태를 검증하는 데 초점을 둡니다.

```kotlin
class TestPostRepositoryImpl: PostRepository{
    override fun findChannelByPostIdAndChannel(postId: Long): Channel?{
        // fake와 다르게 동작하지 않고, 값을 반환하여 값을 검증하는데 도움을 줍니다.
        return Channel(
          1,
          ...
          ZonedDateTime.now()
        )
    }
    ...
    override fun saveChannel(channel: Channel): Channel{
        // fake와 다르게 동작하지 않고, 값을 반환하여 값을 검증하는데 도움을 줍니다.
        return Channel(
          1,
          ...
          ZonedDateTime.now()
        )
    }
}
```

### 4. Mock

- 목은 테스트 대상이 의존하는 객체의 **행위를** **검증**하는 데 사용되는 객체입니다.
- 단순히 값을 반환하는 것을 넘어, 특정 메서드가 올바른 인자를 가지고 호출되었는지, 또는 몇 번 호출되었는지를 확인합니다.
- 목은 테스트 과정에서 기대(Expectation)를 설정하고, 테스트가 끝난 후 이 기대가 충족되었는지 검증하는 것이 핵심입니다.
    
    ```kotlin
    context("publishChannelEvent - 이벤트 발행 확인") {
            beforeTest {
                every { applicationEventPublisher.publishEvent(any<ChannelEvent>()) } just runs
            }
    
            test("channel 이벤트가 1회 발행된다.") {
                val dummyChannel = Channel(
                    id = 1,
                    postId = 1,
                    channel = Channel.NAVER,
                    firstPostedAt = null,
                    lastPostedAt = null,
                    createdAt = ZonedDateTime.now(),
                    updatedAt = ZonedDateTime.now()
                )
    
                channelService.publishChannelEvent(
                    dummyChannel
                )
    
                // 수행되었는지, 값이 적절하게 변경되었는지 등 "행위"를 검증하는 것이 핵심입니다.
                verify(exactly = 1) {
                    applicationEventPublisher.publishEvent(
                        any<ChannelEvent>()
                    )
                }
            }
    ```
    
- 예를 들어, 결제 서비스에서 결제가 성공했을 때 AnalyticsService의 trackEvent 메서드가 호출되는지 확인하고 싶을 수 있습니다. 이때 목을 사용하면 AnalyticsService 객체의 trackEvent 메서드가 특정 인자와 함께 한 번 호출되었는지 검증할 수 있습니다.
- Stub vs Mock
    - Stub: 주로 **상태 검증(state verification)**에 사용
    - Mock: 주로 **행위 검증(behavior verification)**에 사용

### 5. Spy

- 스파이는 **실제 객체의 내부 로직을 그대로 사용**하면서, 특정 메서드에 대한 응답만 미리 설정하거나 호출 여부를 감시하는 객체입니다.

```kotlin
ex)
val realRepo = UserRepositoryImpl()
val spyRepo = spyk(realRepo)
```

- 스터브와 목의 기능을 모두 가지고 있다고 볼 수 있습니다.
- 실제 객체와 매우 유사하게 동작하므로, 실제 객체와 테스트 더블의 기능을 혼합하여 사용하고 싶을 때 유용하게 활용됩니다.
- 예를 들어, ServiceA의 processData 메서드를 테스트할 때, 이 메서드 내부의 validate 메서드가 호출되는 것을 확인하고 싶지만, 그 외 다른 내부 로직은 실제 객체의 동작을 따르게 하고 싶을 때 스파이를 사용할 수 있습니다. validate 메서드의 호출 횟수를 감시하거나, 특정 상황에서 다른 값을 반환하도록 설정할 수 있습니다.
- Spy vs Mock

| **구분** | **Mock** | **Spy** |
| --- | --- | --- |
| 기본 동작 | 없음, 반환값 직접 작성 | 실제 객체 동작 그대로 수행 |
| 사용 목적 | “호출했는가?” / 행위 검증 중심 | “실제 로직 실행 + 일부 감시/대체” |
| 구현 방식 | 완전한 가짜 객체 | 실제 객체 + 부분 mocking |
| 주 활용 사례 | 외부 의존성, 네트워크/DB 콜 차단 | 복잡한 서비스/레포에서 일부만 감시 |

---

## 결론

테스트 더블은 유닛 테스트의 효율성과 신뢰성을 높여주는 강력한 도구입니다. 이들은 테스트 대상 객체를 외부 의존성으로부터 **격리**하고, 예측 가능한 테스트 환경을 조성하여 테스트의 안정성을 보장합니다.

효율적인 테스트 코드를 작성하기 위해서는 이 개념을 명확히 이해하고, 각 상황에 맞는 도구를 적절하게 선택하여 적용해야 합니다.

### 비고(재밌는 이야기)

- Mockist vs Classicist
    - https://puleugo.tistory.com/199

## Reference

- https://craftbettersoftware.com/p/master-the-5-types-of-mocks
- https://codinghack.tistory.com/92


정리를 요청하는 글:

