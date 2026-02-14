## Transactional Outbox Pattern: 분산 시스템의 데이터 일관성 보장 전략

마이크로서비스 아키텍처(MSA)에서 서비스 간의 데이터 정합성을 유지하는 것은 매우 까다로운 작업입니다.

특히 특정 비즈니스 로직을 수행한 후 메시지 큐(Kafka, RabbitMQ 등)로 이벤트를 발행할 때, 데이터베이스 저장과 메시지 발행이 하나의 트랜잭션으로 묶이지 않아 데이터 불일치가 발생하는 문제가 빈번하게 나타납니다. 데이터베이스에는 정보가 저장되었으나 네트워크 오류로 메시지가 전송되지 않거나, 반대로 메시지는 발행되었으나 데이터베이스 트랜잭션이 롤백되는 상황이 대표적입니다.

이러한 문제를 근본적으로 해결하고 메시지 발행의 원자성을 보장하기 위해 도입되는 디자인 패턴이 바로 Transactional Outbox Pattern입니다.

---

## Transactional Outbox Pattern의 개념과 원리

### 1. 원자성 결여 문제의 분석

데이터베이스 저장과 외부 메시지 브로커 전송은 서로 다른 트랜잭션 범위를 가집니다. 일반적인 방식으로는 두 작업의 완전한 성공 혹은 실패를 보장할 수 없습니다.

| 시나리오 | 발생 상황 | 결과 |
| --- | --- | --- |
| DB 성공 / 메시지 실패 | 비즈니스 로직 완료 후 네트워크 장애로 Kafka 전송 실패 | 시스템 간 데이터 불일치 발생 |
| 메시지 성공 / DB 롤백 | 메시지 전송 후 DB 커밋 단계에서 제약 조건 위반 발생 | 유령 데이터(Ghost Data) 이벤트 발생 |

### 2. Outbox 패턴의 동작 원리

이 패턴의 핵심은 메시지 발행 정보를 데이터베이스 내부의 전용 테이블(Outbox Table)에 로컬 트랜잭션의 일부로 함께 저장하는 것입니다.

* Step 1: 비즈니스 데이터 저장과 전송할 메시지 데이터를 하나의 로컬 DB 트랜잭션으로 묶어 처리합니다.
* Step 2: DB 커밋이 완료되면 메시지는 안전하게 Outbox 테이블에 적재됩니다.
* Step 3: 별도의 프로세스(Message Relay/Processor)가 Outbox 테이블을 폴링하거나 DB 로그를 읽어 메시지 브로커로 이벤트를 발행합니다.
* Step 4: 발행이 성공하면 Outbox 테이블의 상태를 업데이트하거나 레코드를 삭제합니다.

### 3. 구현 방식: Polling vs Transaction Log Mining

Outbox Processor가 데이터를 읽어가는 방식은 크게 두 가지로 나뉩니다.

* Polling Publisher: 주기적으로 Outbox 테이블을 쿼리하여 전송되지 않은 메시지를 가져옵니다. 구현이 단순하지만 DB 부하가 발생할 수 있습니다.
* Transaction Log Mining: Debezium과 같은 CDC(Change Data Capture) 도구를 사용하여 DB의 트랜잭션 로그(Binlog, WAL 등)를 추적합니다. DB 부하가 낮고 실시간성이 높습니다.

---

## 구현 사례 및 코드 가이드

비즈니스 로직에서 주문 정보를 저장함과 동시에 Outbox 테이블에 이벤트를 기록하는 예시입니다.

```kotlin
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val outboxRepository: OutboxRepository
) {
    @Transactional
    fun createOrder(orderRequest: OrderRequest) {
        // 1. 비즈니스 로직 수행 및 엔티티 생성
        val order = Order.create(orderRequest)
        orderRepository.save(order)

        // 2. 동일한 트랜잭션 내에서 Outbox 테이블에 이벤트 저장
        val outboxEvent = OutboxEvent(
            aggregateType = "ORDER",
            aggregateId = order.id,
            payload = order.toJsonString(),
            status = OutboxStatus.INIT
        )
        outboxRepository.save(outboxEvent)
        
        // 트랜잭션 커밋 시 DB 저장과 Outbox 저장이 원자적으로 완료됨
    }
}

```

이후 별도의 스케줄러가 미발송된 이벤트를 처리합니다.

```kotlin
@Component
class OutboxProcessor(
    private val outboxRepository: OutboxRepository,
    private val messageProducer: MessageProducer
) {
    @Scheduled(fixedDelay = 1000)
    fun processOutbox() {
        val pendingEvents = outboxRepository.findByStatus(OutboxStatus.INIT)
        
        pendingEvents.forEach { event ->
            try {
                messageProducer.send(event.topic, event.payload)
                event.markAsSent()
                outboxRepository.save(event)
            } catch (e: Exception) {
                event.incrementRetryCount()
                outboxRepository.save(event)
            }
        }
    }
}

```

---

## 결론

Transactional Outbox Pattern은 분산 시스템에서 "최소 한 번 전달(At-least-once delivery)"을 보장하는 가장 신뢰도 높은 방법입니다. 이를 통해 DB 저장과 메시지 발행 사이의 간극을 메우고 시스템의 결합도를 낮추면서도 데이터의 무결성을 지킬 수 있습니다.

### 핵심 정리 및 활용 제안

* 메시지 발행의 원자성 보장: 로컬 트랜잭션을 활용하여 DB와 메시지 상태를 동기화합니다.
* 장애 내성: 메시지 브로커가 일시적으로 중단되어도 Outbox 테이블에 데이터가 보존되므로 추후 재처리가 가능합니다.
* 활용 방안: 주문-결제 시스템, 알림 서비스 연동 등 데이터 유실이 치명적인 도메인에 우선적으로 적용할 것을 권장합니다.

추가적으로, 메시지가 중복 발행될 가능성이 있으므로 수신 측에서는 반드시 멱등성(Idempotency)을 보장하는 로직을 설계해야 합니다.