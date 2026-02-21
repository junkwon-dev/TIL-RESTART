## 트랜잭셔널 아웃박스 패턴과 Debezium SMT PoC 결과

마이크로서비스 아키텍처에서 서비스 간 데이터 일관성을 유지하는 것은 매우 까다로운 과제입니다. 특히 데이터베이스 업데이트와 메시지 발행이 하나의 원자적 단위로 처리되지 않을 경우, 데이터 불일치 문제가 발생할 수 있습니다.

이러한 문제를 해결하기 위해 Transactional Outbox 패턴을 도입합니다. 이 글에서는 Debezium Outbox Event Router(SMT)를 활용하여 이벤트를 발행할 때 발생했던 실무적인 이슈와 그 해결 방안을 다룹니다.

---

### 1. 스키마 인식 오류 및 Snapshot Mode 해결

개발 환경에서 Debezium 커넥터를 운용하다 보면 다음과 같은 에러를 마주할 수 있습니다.
`Encountered change event for table greeting.outbox whose schema isn't known to this connector`

이 에러는 커넥터가 캡처하려는 테이블의 스키마 정보를 히스토리 토픽에서 찾지 못할 때 발생합니다. 주로 DB 스키마 변경 후 커넥터 설정이 꼬이거나, 히스토리 토픽이 삭제되었을 때 나타납니다.

| 원인 | 해결 방안 | 상세 설명 |
| --- | --- | --- |
| **Schema Mismatch** | **Snapshot Mode: recovery** | 커넥터가 기존 히스토리 대신 현재 DB 상태를 다시 읽도록 설정합니다. |
| **Topic Cleanup** | **Schema History Rebuild** | 내부 스키마 히스토리 토픽을 재생성하여 동기화를 맞춥니다. |

```json
{
  "name": "outbox-connector",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "snapshot.mode": "recovery",
    "schema.history.internal.kafka.topic": "schema-changes.inventory"
  }
}

```

---
데이터베이스의 변경 사항을 캡처하는 **CDC(Change Data Capture)** 커넥터(예: Debezium)는 처음에 기존 데이터를 한 번 훑고 지나가야 합니다. 이를 **스냅샷(Snapshot)** 작업이라고 하죠.

커넥터 설정의 `snapshot.mode` 옵션에 따라 초기 데이터를 어떻게 처리할지 결정할 수 있습니다. 주요 모드들을 정리해 드릴게요.

---

### 2. 주요 스냅샷 모드 종류

| 모드 이름            | 특징 | 권장 사용 사례                            |
|------------------| --- |-------------------------------------|
| **initial**      | **기본값.** 실행 시 기존 데이터를 모두 읽고, 이후 변경분(Binlog/WAL)을 추적합니다. | 처음 커넥터를 연동할 때 사용                    |
| **initial_only** | 초기 데이터만 한 번 싹 긁어오고 커넥터를 종료합니다. | 일회성 데이터 마이그레이션                      |
| **schema_only**  | 데이터는 가져오지 않고, **테이블 구조(Schema)** 정보만 캡처합니다. | 데이터는 필요 없고 지금부터 발생하는 변경 사항만 중요할 때   |
| **never**        | 스냅샷을 아예 찍지 않습니다. | 이미 스트리밍 로그가 완벽하게 관리되고 있는 특수 상황      |
| **recovery**     | 현재 DB 상태를 기반으로 커넥터의 메타데이터를 재구성합니다. | 위와 같이 DB Schema에 대한 변경분에 추적이 어려운 경우 |
| **always**       | 커넥터가 재시작될 때마다 매번 스냅샷을 다시 찍습니다. | (거의 사용 안 함) 데이터 정합성 테스트 용도          |

---

### 3. 스냅샷의 작동 프로세스

스냅샷 모드가 실행될 때 커넥터는 보통 다음과 같은 단계를 거칩니다.

1. **테이블 락(Lock) 및 스키마 읽기:** 일관성을 위해 테이블 구조를 먼저 파악합니다.
2. **데이터 스캔:** `SELECT * FROM table`과 유사한 방식으로 기존 레코드를 읽어 타겟(Kafka 등)으로 보냅니다.
3. **오프셋 기록:** 스냅샷이 끝난 시점의 로그 위치(LSN 또는 Binlog position)를 기록합니다.
4. **스트리밍 전환:** 기록된 시점 이후부터 실시간 변경 데이터(CDC)를 읽기 시작합니다.

---

### 4. 설정 시 주의사항

* **부하 관리:** `initial` 모드로 수백만 건의 데이터를 읽을 때 데이터베이스에 부하가 가거나 네트워크 트래픽이 튈 수 있습니다. 이럴 때는 `snapshot.fetch.size` 같은 옵션으로 한 번에 가져올 양을 조절해야 합니다.
* **권한 문제:** 스냅샷을 찍으려면 대상 테이블에 대한 `SELECT` 권한뿐만 아니라, 메타데이터 접근을 위한 권한이 추가로 필요할 수 있습니다.
* **증분 스냅샷(Incremental Snapshot):** 최신 버전의 Debezium은 서비스 중단 없이 데이터를 조금씩 나눠서 가져오는 증분 스냅샷 기능도 지원합니다.

---

혹시 **특정 데이터베이스(MySQL, PostgreSQL 등)**에서 이 설정을 적용하려고 하시나요? 사용 중인 DB 환경을 알려주시면 더 구체적인 설정값을 가이드해 드릴 수 있습니다.

## Debezium SMT 상태 관리 및 중복 발행 방지

Outbox 패턴 구현 시, 단순한 Insert 외에 Update나 Delete 작업이 발생할 때 이벤트가 중고복 발행되는 것을 경계해야 합니다.

### 1. Update 이벤트 무시 동작 확인

기본적으로 Debezium Outbox Event Router는 Insert(Create) 이벤트만 처리하도록 설계되어 있습니다. 실제 시스템 운영 중 상태값 업데이트(Status Update)를 수행했을 때 로그를 확인한 결과는 다음과 같습니다.

> 2026-02-13T04:56:49,296 WARN || Unexpected update message received [REDACTED] and ignored [io.debezium.transforms.outbox.EventRouterDelegate]

위 로그에서 알 수 있듯이, SMT는 Update 메시지를 수신하면 이를 무시하고 이벤트를 재발행하지 않습니다. 이는 의도치 않은 중복 메시지 전송을 방지하는 안전장치 역할을 합니다.

### 2. 명시적 Insert 설정 권장

Debezium SMT에서 기본으로 제공하는 테이블 외에 이벤트에 대한 상태관리를 추가로 수행하는 경우, 
Create 외에 Update Delete를 기준으로 이벤트를 발행하는 지 확인해야합니다. 비록 기본 동작이 Update를 무시하더라도, 시스템의 명확성을 위해 Insert 동작 시에만 이벤트를 발행하도록 커넥터 설정을 명시하는 것이 좋습니다.

---

## 핵심 정리 및 제언

트랜잭셔널 아웃박스 패턴은 분산 시스템에서 메시지 발행의 신뢰성을 보장하는 가장 표준적인 방법입니다. Debezium SMT를 활용하면 별도의 발행 서비스 없이도 효율적인 아키텍처를 구축할 수 있습니다.

* 스키마 인식 에러 발생 시 **snapshot.mode=recovery**를 통해 커넥터의 상태를 복구하십시오.
* 메시지 순서 보장이 중요하다면 Outbox 테이블의 Partition Key를 적절히 설정하여 카프카의 동일 파티션으로 인입되도록 구성하십시오.

실제 운영 환경에 적용하기 전, 스테이징 환경에서 다양한 DML(Insert, Update, Delete) 시나리오를 테스트하여 의도한 대로 이벤트가 라우팅되는지 검증하는 절차를 반드시 거치시기 바랍니다.

추가적으로 Debezium의 **Heartbeat** 기능을 활성화하여 커넥터의 생존 여부를 실시간으로 모니터링하는 방안도 고려해 보시기 바랍니다.

더 궁금한 점이 있으시다면 커넥터 로그 레벨을 DEBUG로 설정하여 SMT의 내부 동작 과정을 추적해 보시는 것을 추천합니다.