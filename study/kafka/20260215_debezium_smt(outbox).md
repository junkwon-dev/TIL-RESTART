## 분산 시스템의 데이터 일관성 해결을 위한 Outbox 패턴과 Debezium SMT 활용 전략

마이크로서비스 아키텍처(MSA)에서 가장 빈번하게 발생하는 문제 중 하나는 데이터베이스 업데이트와 메시지 발행 사이의 원자성 보장입니다. 서비스가 DB에 데이터를 저장한 직후 네트워크 장애나 프로세스 종료로 인해 메시지 큐에 이벤트를 전달하지 못하면 시스템 간 데이터 불일치가 발생합니다.

이러한 문제를 해결하기 위해 Transactional Outbox 패턴을 도입합니다. 특히 CDC(Change Data Capture) 도구인 Debezium과 SMT(Single Message Transform) 기능을 결합하면 애플리케이션의 복잡도를 낮추면서도 강력한 이벤트 전달 신뢰성을 확보할 수 있습니다.

---

## Transactional Outbox 패턴의 원리와 구조

### 1. 패턴의 핵심 개념

Transactional Outbox 패턴은 비즈니스 로직과 관련된 데이터 업데이트와 외부로 발행할 메시지를 동일한 데이터베이스 트랜잭션 내에서 처리하는 방식입니다. 이를 통해 DB 저장에는 성공했지만 메시지 발행에는 실패하는 상황을 원천적으로 차단합니다.

### 2. 구성 요소 및 흐름

| 구성 요소 | 역할 설명 |
| --- | --- |
| **Outbox Table** | 발행할 메시지 데이터를 임시로 저장하는 DB 테이블입니다. |
| **Message Relay** | Outbox 테이블에 쌓인 데이터를 읽어 메시지 브로커(Kafka 등)로 전달하는 컴포넌트입니다. |
| **Idempotent Consumer** | 메시지가 중복 전달될 경우를 대비하여 중복을 제거하는 수신측 로직입니다. |

### 3. 코드 적용 사례 (Spring Boot & JPA)

애플리케이션 개발자는 비즈니스 엔티티를 저장할 때 Outbox 엔티티도 함께 영속화합니다.

```java
@Transactional
public void createOrder(OrderRequest request) {
    // 1. 비즈니스 로직 수행
    Order order = orderRepository.save(new Order(request));

    // 2. 동일한 트랜잭션 내에서 Outbox 테이블에 이벤트 저장
    OutboxEvent event = new OutboxEvent(
        "OrderCreated",
        order.getId().toString(),
        JsonUtils.toJson(order)
    );
    outboxRepository.save(event);
}

```

---

## Debezium SMT를 활용한 데이터 변환 최적화

Debezium은 DB 로그를 실시간으로 읽어 Kafka로 전송하는 대표적인 CDC 도구입니다. 하지만 기본적으로 Debezium이 생성하는 메시지는 DB의 변경 전/후 상태를 모두 포함하는 복잡한 구조를 가집니다. 이때 SMT(Single Message Transform)를 활용하면 메시지 구조를 단순화하고 원하는 형태로 가공할 수 있습니다.

### 1. Outbox Event Router SMT

Debezium은 Outbox 패턴을 공식적으로 지원하기 위해 `io.debezium.transforms.outbox.EventRouter`라는 전용 SMT를 제공합니다. 이 기능을 사용하면 다음과 같은 이점을 얻을 수 있습니다.

* **토픽 라우팅**: Outbox 테이블의 특정 컬럼 값을 기반으로 메시지를 서로 다른 Kafka 토픽으로 자동 분산합니다.
* **페이로드 추출**: 복잡한 CDC 메타데이터를 제거하고 실제 전송하고자 하는 JSON 바디만 메시지로 구성합니다.
* **이벤트 키 설정**: 메시지의 Key를 지정하여 Kafka의 파티셔닝 전략을 제어합니다.

### 2. 커넥터 설정 예시

아래 설정은 Debezium 커넥터에서 Outbox SMT를 활성화하는 예시입니다.

```json
{
  "name": "outbox-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.table.fields.additional.placement": "type:header:eventType",
    "transforms.outbox.route.topic.replacement": "events.${routedByValue}",
    "transforms.outbox.route.by.field": "aggregate_type"
  }
}

```

## Outbox 패턴 구성을 위한 데이터베이스 설계 (ERD)

Outbox 패턴을 성공적으로 구현하기 위해서는 비즈니스 로직이 담긴 테이블과 별개로, 메시지 발행을 관리할 전용 테이블 설계가 필요합니다. Debezium의 **EventRouter SMT**가 인식할 수 있는 표준적인 테이블 구조를 제안합니다.

---

### Outbox 테이블 상세 명세

일반적으로 다음과 같은 컬럼 구성을 권장하며, 각 필드는 Debezium SMT 설정에 의해 Kafka 메시지의 구성 요소로 매핑됩니다.

| 컬럼명 | 데이터 타입 | 설명 및 역할 |
| --- | --- | --- |
| **id** | UUID / BIGINT | 각 이벤트의 고유 식별자이며 Primary Key로 사용합니다. |
| **aggregatetype** | VARCHAR | 이벤트가 발생한 도메인 분류입니다. (예: Order, Inventory) |
| **aggregateid** | VARCHAR | 관련 엔티티의 ID입니다. Kafka 메시지의 Key로 활용됩니다. |
| **type** | VARCHAR | 이벤트의 구체적인 종류입니다. (예: OrderCreated, OrderCancelled) |
| **payload** | JSON / TEXT | 실제 메시지 본문 데이터입니다. SMT가 이를 추출하여 Value로 보냅니다. |
| **timestamp** | TIMESTAMP | 이벤트가 발생한 시각입니다. 순서 보장 및 추적용으로 사용합니다. |

---

### ERD 설계 시 고려 사항

* **트랜잭션 바운더리**: 비즈니스 데이터(예: `Orders` 테이블)와 `Outbox` 테이블은 반드시 동일한 데이터베이스 스키마 내에 존재해야 하며, 하나의 로컬 트랜잭션으로 묶여야 합니다.
* **인덱스 전략**: `timestamp` 컬럼에 인덱스를 생성하여 오래된 데이터를 정리(Purge)하는 배치 작업의 성능을 확보해야 합니다.
* **페이로드 형식**: JSON 타입을 지원하는 DB(PostgreSQL, MySQL 5.7+ 등)를 사용한다면 `payload` 컬럼을 JSON 타입으로 지정하여 데이터 구조를 명확히 정의하는 것이 좋습니다.

---

## 결론 및 활용 제언

Transactional Outbox 패턴과 Debezium SMT의 조합은 데이터 무결성이 중요한 결제, 주문, 재고 관리 시스템에서 필수적인 아키텍처입니다.

### 핵심 정리

* **원자성 보장**: DB 트랜잭션을 활용하여 '전부 성공' 또는 '전부 실패'를 보장합니다.
* **성능 최적화**: 애플리케이션이 직접 메시지 브로커와 통신하지 않으므로 응답 속도가 향상됩니다.
* **유연성**: SMT를 통해 인프라 계층에서 메시지 포맷을 자유롭게 변경할 수 있습니다.