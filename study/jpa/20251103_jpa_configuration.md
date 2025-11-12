## 서론

애플리케이션의 성능과 안정성을 높이기 위해 데이터베이스를 Read(조회)와 Write(수정/입력) 용도로 분리하는 **Replication** 구조를 도입하는 경우가 많습니다. Spring Data JPA 환경에서는 `AbstractRoutingDataSource`를 사용하여 이러한 다중 DataSource 환경을 구성할 수 있습니다.

하지만 단순히 라우팅 설정만 적용할 경우, `@Transactional(readOnly = true)` 어노테이션을 사용한 읽기 전용 트랜잭션이 의도와 다르게 **Write(Master) DB**로 라우팅되는 문제가 발생할 수 있습니다.

이 글에서는 해당 문제가 발생하는 원인과 `LazyConnectionDataSourceProxy`를 통해 이 문제를 해결하는 방법을 정리합니다.

-----

## 본론

### 1\. 기본 설정: `AbstractRoutingDataSource`를 이용한 DB 라우팅

먼저, Read/Write DataSource를 각각 빈(Bean)으로 등록하고, 이를 라우팅하는 `AbstractRoutingDataSource`를 설정해야 합니다.

* `writeDataSource`: Master DB (CUD 작업용)
* `readDataSource`: Replica DB (R 작업용)
* `routingDataSource`: 트랜잭션의 속성(Read-Only 여부)에 따라 적절한 DataSource를 선택합니다.

`AbstractRoutingDataSource`를 상속받은 커스텀 라우터(예: `MySQLRoutingDataSource`)는 현재 트랜잭션의 `readOnly` 속성을 확인하여 사용할 DataSource의 키(Key)를 반환해야 합니다.

```kotlin
// MySQLRoutingDataSource.kt
class MySQLRoutingDataSource : AbstractRoutingDataSource() {
    override fun determineCurrentLookupKey(): Any? {
        // 트랜잭션이 읽기 전용인지 확인하여 라우팅 키 반환
        return if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            MySQLRoutingKey.READ
        } else {
            MySQLRoutingKey.WRITE
        }
    }
}

enum class MySQLRoutingKey {
    READ, WRITE
}

// JpaConfig.kt (일부)
@Configuration
@EnableJpaRepositories(basePackages = ["com.example.package"]) // 엔티티/리포지토리 패키지 경로
@EnableTransactionManagement
class JpaConfig {
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari.write")
    fun writeDataSource(): DataSource {
        return DataSourceBuilder
            .create()
            .type(HikariDataSource::class.java)
            .build()
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari.read")
    fun readDataSource(): DataSource {
        return DataSourceBuilder
            .create()
            .type(HikariDataSource::class.java)
            .build()
    }

    @Bean
    fun routingDataSource(
        @Qualifier("writeDataSource") writeDataSource: DataSource,
        @Qualifier("readDataSource") readDataSource: DataSource
    ): DataSource {
        val routingDataSource = MySQLRoutingDataSource()

        val dataSourceMap = mapOf<Any, Any>(
            MySQLRoutingKey.WRITE to writeDataSource,
            MySQLRoutingKey.READ to readDataSource
        )
        // 사용할 DataSource 들을 설정합니다.
        routingDataSource.setTargetDataSources(dataSourceMap)
        // 기본값은 Write DB로 설정합니다.
        routingDataSource.setDefaultTargetDataSource(writeDataSource)

        return routingDataSource
    }
}
```

### 2\. 문제 상황: 커넥션의 '조기 획득' 문제

위와 같이 설정하고 `@Transactional(readOnly = true)` 테스트를 수행하면, `TransactionSynchronizationManager.isCurrentTransactionReadOnly()`가 항상 `false`를 반환하여 **Read DB가 아닌 Write DB로 쿼리가 전달되는 현상**이 발생합니다.

이는 Spring의 트랜잭션 매니저(예: `JpaTransactionManager`)가 동작하는 방식 때문입니다.

1.  `@Transactional` 메서드가 호출됩니다.
2.  트랜잭션 매니저(`JpaTransactionManager`)가 트랜잭션을 시작합니다 (`doBegin`).
3.  이 과정에서 `EntityManager`를 생성하며, 이때 **데이터베이스 커넥션을 획득**하려 시도합니다.
4.  `AbstractRoutingDataSource`의 `determineCurrentLookupKey`가 호출되어 DB 라우팅이 발생합니다.
5.  **문제:** 이 시점은 트랜잭션의 `readOnly` 속성이 `TransactionSynchronizationManager`에 설정되기 **전**입니다.
6.  `readOnly` 속성은 커넥션 획득 이후에 `txObject.setReadOnly(definition.isReadOnly())`를 통해 설정됩니다.
7.  따라서 라우팅 시점에는 `isCurrentTransactionReadOnly()`가 항상 `false`를 반환하여 `setDefaultTargetDataSource`(Write DB)로 라우팅됩니다.

### 3\. 해결책: `LazyConnectionDataSourceProxy` 도입

이 문제를 해결하기 위해 \*\*`LazyConnectionDataSourceProxy`\*\*를 사용합니다. 이름 그대로 DataSource를 프록시(Proxy) 패턴으로 감싸, 실제 데이터베이스 커넥션이 필요한 시점(즉, 실제 SQL 쿼리가 실행되는 시점)까지 **커넥션 획득을 지연**시킵니다.

`routingDataSource`를 `LazyConnectionDataSourceProxy`로 감싸고, 이 프록시 객체를 애플리케이션의 기본(`@Primary`) DataSource로 사용하도록 설정합니다.

```kotlin
// JpaConfig.kt (LazyConnectionDataSourceProxy 추가)

    // ... (writeDataSource, readDataSource, routingDataSource 빈 설정은 동일) ...

    @Primary // 이 프록시를 기본 DataSource로 사용합니다.
    @Bean
    fun dataSource(
        @Qualifier("routingDataSource") routingDataSource: DataSource
    ): DataSource {
        // 실제 커넥션 획득을 지연시키는 프록시를 반환합니다.
        return LazyConnectionDataSourceProxy(routingDataSource)
    }
}
```

### 4\. `LazyConnectionDataSourceProxy`의 동작 원리

`LazyConnectionDataSourceProxy`를 적용하면 트랜잭션 처리 흐름이 다음과 같이 변경됩니다.

| 순서 | 작업 내용 | `JpaTransactionManager` | `LazyConnectionDataSourceProxy` | `AbstractRoutingDataSource` |
| :--- | :--- | :--- | :--- | :--- |
| 1. | `@Transactional` 메서드 호출 | 시작 | - | - |
| 2. | 트랜잭션 시작 | `doBegin()` 호출 | - | - |
| 3. | 커넥션 획득 시도 (차단됨) | `createEntityManagerForTransaction()` 호출 | **커넥션 획득을 가로채고 지연시킴** | 라우팅 발생 $\rightarrow$ **X** |
| 4. | `readOnly` 상태 설정 | `TransactionSynchronizationManager.setCurrentTransactionReadOnly(true)`를 호출하여 `readOnly` 상태를 스레드에 저장 | - | - |
| 5. | 메서드 로직 실행 | - | - | - |
| 6. | **실제 SQL 쿼리 실행** | - | - | - |
| 7. | **실제 커넥션 획득** | - | 지연되었던 커넥션 획득 요청을 실제 `routingDataSource`로 전달 | 라우팅 발생 $\rightarrow$ **O** |
| 8. | 정확한 라우팅 | - | - | `determineCurrentLookupKey()` 호출 시, `isCurrentTransactionReadOnly()`가 **true**를 반환 (4번 단계에서 설정됨) $\rightarrow$ **Read DB 선택** |

이처럼 커넥션 획득 시점을 트랜잭션 속성이 모두 설정된 이후(실제 쿼리 실행 직전)로 지연시킴으로써, `AbstractRoutingDataSource`가 `readOnly` 여부를 정확하게 판단하여 올바른 DB로 라우팅할 수 있게 됩니다.

### 5\. (심화) `@Primary`를 사용하지 않는 수동 설정

만약 여러 개의 `EntityManagerFactory`를 사용하거나 `@Primary` 어노테이션 사용을 피해야 하는 경우, `DataSource` 뿐만 아니라 `EntityManagerFactory`와 `TransactionManager`까지 **수동으로 명시적인 관계를 설정**해 주어야 합니다.

`@EnableJpaRepositories`에 `entityManagerFactoryRef`와 `transactionManagerRef`를 지정하고, 각 빈을 생성할 때 `@Qualifier`를 사용하여 주입받을 `DataSource` 프록시를 명확히 지정해야 합니다.

```kotlin
@Configuration
@EnableJpaRepositories(
    basePackages = ["com.example.package"], // 엔티티/리포지토리 패키지 경로
    entityManagerFactoryRef = "appEntityManagerFactory", // 수동 등록한 이름 명시
    transactionManagerRef = "appTransactionManager"      // 수동 등록한 이름 명시
)
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class]) // 자동 설정 제외
@EnableTransactionManagement
class JpaConfig {

    // ... (writeDataSource, readDataSource, routingDataSource 빈 설정) ...

    // @Primary 어노테이션 제거
    @Bean
    fun dataSource(
        @Qualifier("routingDataSource") routingDataSource: DataSource
    ): DataSource {
        return LazyConnectionDataSourceProxy(routingDataSource)
    }

    // 1. EntityManagerFactory 수동 등록
    @Bean(name = ["appEntityManagerFactory"])
    fun appEntityManagerFactory(
        // @Qualifier로 사용할 DataSource 빈을 명시적으로 지정
        @Qualifier("dataSource") dataSource: DataSource
    ): LocalContainerEntityManagerFactoryBean {
        val factory = LocalContainerEntityManagerFactoryBean()
        factory.setDataSource(dataSource) // 라우팅 프록시 주입
        factory.setPackagesToScan("com.example.package") // 엔티티 패키지 설정

        val vendorAdapter = HibernateJpaVendorAdapter()
        factory.jpaVendorAdapter = vendorAdapter
        // ... (기타 JPA 프로퍼티 설정) ...
        return factory
    }

    // 2. TransactionManager 수동 등록
    @Bean(name = ["appTransactionManager"])
    fun appTransactionManager(
        // 수동 등록한 EntityManagerFactory를 명시적으로 주입
        @Qualifier("appEntityManagerFactory") entityManagerFactory: EntityManagerFactory
    ): PlatformTransactionManager {
        val transactionManager = JpaTransactionManager()
        transactionManager.entityManagerFactory = entityManagerFactory
        return transactionManager
    }
}
```

-----

## 결론

Spring Data JPA 환경에서 DB Replication을 위한 **Read/Write 분리**를 구현할 때, `AbstractRoutingDataSource`만으로는 트랜잭션의 `readOnly` 속성을 정확히 감지하기 어렵습니다.

트랜잭션 시작 시 발생하는 **커넥션 조기 획득** 문제를 해결하기 위해, `LazyConnectionDataSourceProxy`를 함께 사용하여 실제 쿼리가 실행되는 시점까지 커넥션 획득을 지연시켜야 합니다.

이 설정을 통해 `@Transactional(readOnly = true)` 어노테이션이 올바르게 동작하여 Read 쿼리는 Replica DB로, Write 쿼리는 Master DB로 정확하게 라우팅하여 시스템의 부하를 분산시키고 성능을 향상시킬 수 있습니다.