## 🛠️ Spring Data JPA의 핵심, SimpleJpaRepository 파헤치기

### 서론

Spring Data JPA는 **Repository** 인터페이스를 통해 데이터 접근 계층(Data Access Layer)을 매우 편리하게 구축할 수 있도록 지원합니다. 개발자는 기본적인 CRUD 메서드를 직접 구현할 필요 없이 인터페이스 정의만으로도 기능을 사용할 수 있습니다. 이러한 마법 같은 기능의 중심에는 바로 **SimpleJpaRepository**라는 기본 구현체가 있습니다. 본 글에서는 Spring Data JPA의 Repository 상속 구조를 명확히 이해하고, 이 핵심 구현체인 SimpleJpaRepository가 어떻게 동작하며 기본 구현체로 선택되는 이유와 커스터마이징 방안을 깊이 있게 분석합니다.

-----

### 본론

#### 1\. Repository 상속 구조 분석: 계층적 설계의 이해

Spring Data JPA의 Repository 인터페이스들은 상속을 통해 점진적으로 기능을 확장하는 계층적 구조를 가지고 있습니다. 이 구조를 이해하는 것은 Spring Data JPA를 효과적으로 활용하는 첫걸음입니다.

* **Repository:** 마커 인터페이스(Marker Interface)입니다. Spring Data의 모든 Repository의 최상위 인터페이스이며, 특별한 메서드를 정의하지 않고, 단순히 도메인 타입과 ID 타입을 지정하는 역할을 합니다.
* **CrudRepository:** 기본적인 CRUD(Create, Read, Update, Delete) 기능을 제공하는 핵심 인터페이스입니다. `save()`, `findById()`, `findAll()`, `delete()` 등의 메서드를 정의하고 있습니다.
* **ListCrudRepository:** Spring Data 3.x 버전부터 추가된 인터페이스로, `CrudRepository`를 상속받아 `findAll()` 등의 반환 타입을 `Iterable` 대신 `List`로 제공하는 등 목록 조회에 특화된 기능을 추가합니다.
* **JpaRepository:** `ListCrudRepository`를 상속받으며, JPA 특화 기능(예: flush, batch delete, 쿼리 메서드 기반 페이징/정렬)을 제공합니다. 이는 실제 JPA 영속성 컨텍스트와 상호작용하는 데 필요한 고수준 기능을 담당합니다.

| 인터페이스 | 주요 역할 | 상위 인터페이스 |
| :---: | :---: | :---: |
| **Repository** | 마커 역할 (최상위) | 없음 |
| **CrudRepository** | 기본 CRUD 메서드 제공 | Repository |
| **ListCrudRepository** | 목록 조회 관련 기능 강화 | CrudRepository |
| **JpaRepository** | JPA 특화 기능 제공 | ListCrudRepository |

#### 2\. SimpleJpaRepository: JpaRepository의 기본 구현체

**SimpleJpaRepository** 클래스는 **JpaRepository** 인터페이스에 정의된 모든 기능을 실제로 구현하는 클래스입니다. 이 클래스는 내부적으로 **EntityManager**를 사용하여 JPA의 기능을 수행하며, Repository 인터페이스의 추상 메서드들을 구체화하여 데이터베이스와의 상호작용을 처리합니다.

**SimpleJpaRepository가 기본 구현체로 선택되는 이유**는 다음과 같습니다.

* **Repository Bean 생성 과정:** Spring 애플리케이션이 시작될 때, Spring Data JPA는 **JpaRepositoryFactory**를 사용하여 개발자가 정의한 Repository 인터페이스들을 스캔합니다.
* **구현체 매핑:** 이 팩토리는 인터페이스의 타입 인자(도메인 클래스와 ID 클래스)를 추출한 뒤, `JpaRepository` 계층 구조의 표준 구현체로 **SimpleJpaRepository**를 선택하고 해당 인스턴스를 생성하여 Spring Bean으로 등록합니다.
* **단일 구현체:** Spring Data JPA는 `JpaRepository`에 대한 표준화되고 검증된 단일 구현체로 **SimpleJpaRepository**만을 제공합니다. 개발자는 이 클래스의 기능을 확장하거나 커스터마이징하여 사용합니다.

<!-- end list -->

```java
// SimpleJpaRepository 내부 구조의 핵심 (예시)
public class SimpleJpaRepository<T, ID> implements JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
    
    private final JpaEntityInformation<T, ?> entityInformation;
    private final EntityManager entityManager;
    
    public SimpleJpaRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        // 엔티티 정보 및 EntityManager를 주입받아 사용합니다.
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public <S extends T> S save(S entity) {
        // 실제로 EntityManager의 persist 또는 merge를 호출합니다.
        if (entityInformation.isNew(entity)) {
            entityManager.persist(entity);
            return entity;
        } else {
            return entityManager.merge(entity);
        }
    }
    // ... 나머지 JpaRepository 메서드 구현 ...
}
```

#### 3\. 커스텀 Repository 구현 방안

때로는 **SimpleJpaRepository**가 제공하는 기본 메서드 외에 특정 비즈니스 로직에 특화된 메서드가 필요할 수 있습니다. 이 경우, 개발자는 다음과 같은 방식으로 Repository를 커스터마이징하여 사용할 수 있습니다.

1.  **커스텀 인터페이스 정의:** 원하는 커스텀 메서드를 정의하는 인터페이스를 만듭니다. (예: `UserRepositoryCustom`)
2.  **커스텀 구현체 작성:** 해당 인터페이스를 구현하는 클래스를 작성하고, 일반적으로 `Impl` 접미사를 붙입니다. (예: `UserRepositoryCustomImpl`) 이 구현체 내에서 **EntityManager**를 직접 주입받아 복잡한 JPQL, Criteria API 또는 네이티브 쿼리를 사용하여 로직을 구현할 수 있습니다.
3.  **기존 Repository 인터페이스 확장:** 기존의 **JpaRepository**를 상속받는 인터페이스가 새로 정의한 커스텀 인터페이스를 함께 상속받도록 선언합니다.

<!-- end list -->

```java
// 1. 커스텀 인터페이스 정의
public interface UserRepositoryCustom {
    List<User> findUsersByComplexCriteria(String keyword);
}

// 2. 커스텀 구현체 작성 (관례상 Impl을 붙입니다)
@Repository
public class UserRepositoryCustomImpl implements UserRepositoryCustom {
    private final EntityManager em;

    public UserRepositoryCustomImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<User> findUsersByComplexCriteria(String keyword) {
        // 복잡한 쿼리 로직을 구현합니다.
        // 예: return em.createQuery("SELECT u FROM User u WHERE ...").getResultList();
        return Collections.emptyList();
    }
}

// 3. 기존 JpaRepository 인터페이스 확장
public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {
    // JpaRepository의 기본 기능 + UserRepositoryCustom의 메서드를 모두 사용 가능합니다.
}
```

Spring Data JPA는 Repository 인터페이스를 스캔할 때, **UserRepositoryCustomImpl** 클래스를 찾아 이를 **UserRepository**의 기본 구현체인 **SimpleJpaRepository**와 연결하여 하나의 Bean으로 제공합니다.

-----

### 결론

**SimpleJpaRepository**는 Spring Data JPA의 Repository 인터페이스 계층 구조를 완성하는 핵심 구현체이며, 개발자에게 강력한 데이터 접근 기능을 추상화하여 제공합니다. 이 클래스를 통해 우리는 복잡한 JPA API 대신 간결한 인터페이스를 사용하여 애플리케이션의 데이터 접근 계층을 안정적이고 빠르게 구축할 수 있습니다. 효율적인 Spring Data JPA 활용을 위해서는 Repository의 상속 구조와 기본 구현체의 역할, 그리고 커스터마이징 방안을 정확히 이해하고 적용해야 합니다. 이 지식은 유지보수가 용이하고 테스트하기 쉬운 영속성 코드를 작성하는 데 큰 도움이 됩니다.