---
name: infrastructure-persistence
description: Dùng khi tạo JPA Entity, JPA Repository (extends JpaRepository), hoặc RepositoryImpl (implements Domain Repository interface). Đảm bảo Infrastructure Layer tách biệt hoàn toàn khỏi Domain Model và implement đúng contract.
---

# Skill: Viết Infrastructure Persistence Chuẩn

## Nguyên tắc

1. **JPA Entity ≠ Domain Model** — Tạo class riêng biệt, không dùng Domain Model làm `@Entity`.
2. **JPA Repository extends JpaRepository** — Spring Data JPA, đặt trong `infrastructure/persistence/`.
3. **RepositoryImpl implements Domain Repository** — Adapter chuyển đổi giữa JPA Entity và Domain Model.
4. **Domain không biết JPA** — Không import `javax.persistence` trong `domain/`.

## Template JPA Entity

```java
package api.exchange.modules.{module}.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "{module}_{table_name}s")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class {Model}Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private {Model}StatusEnum status; // Dùng Enum, không dùng String

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

## Template JPA Repository

```java
package api.exchange.modules.{module}.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface {Model}JpaRepository extends JpaRepository<{Model}Entity, Long> {

    // Dùng derived query method khi có thể
    List<{Model}Entity> findByUserUidOrderByCreatedAtDesc(String userUid);

    // Dùng @Query chỉ khi cần query phức tạp
    @Query("SELECT e FROM {Model}Entity e WHERE e.status = :status AND e.userUid = :uid")
    List<{Model}Entity> findActiveByUser(@Param("uid") String uid,
                                         @Param("status") String status);

    // @Modifying cho UPDATE/DELETE query
    @Modifying
    @Query("UPDATE {Model}Entity e SET e.status = :status WHERE e.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") String status);
}
```

## Template Repository Implementation

```java
package api.exchange.modules.{module}.infrastructure.persistence;

import api.exchange.modules.{module}.domain.model.{Model};
import api.exchange.modules.{module}.domain.repository.{Model}Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class {Model}RepositoryImpl implements {Model}Repository {

    private final {Model}JpaRepository jpaRepository;

    @Override
    public Optional<{Model}> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<{Model}> findByUserUid(String userUid) {
        return jpaRepository.findByUserUidOrderByCreatedAtDesc(userUid)
            .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public {Model} save({Model} domain) {
        {Model}Entity entity = toEntity(domain);
        {Model}Entity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    // Chuyển Entity -> Domain Model
    private {Model} toDomain({Model}Entity entity) {
        return {Model}.builder()
            .id(entity.getId())
            .userUid(entity.getUserUid())
            // .status(mapStatus(entity.getStatus()))
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    // Chuyển Domain Model -> Entity
    private {Model}Entity toEntity({Model} domain) {
        return {Model}Entity.builder()
            .id(domain.getId())
            .userUid(domain.getUserUid())
            // .status(mapStatusEnum(domain.getStatus()))
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .build();
    }
}
```

## Quy ước đặt tên bảng

```
{module}_{resource}s

VD:
- p2p_ads
- p2p_orders
- wallet_transactions
- auth_users
```

## Quy tắc Column

| Trường hợp | Convention |
|------------|-----------|
| Status / Type | `@Enumerated(EnumType.STRING)` — lưu tên Enum |
| BigDecimal tiền | `@Column(precision = 36, scale = 8)` |
| Timestamp tạo | `@Column(updatable = false)` |
| Foreign key | Lưu `{model}Id` (Long/String) — KHÔNG dùng `@OneToMany` liên module |

## Quy tắc Query liên Module

- **KHÔNG JOIN** bảng giữa các module trong `@Query`
- Nếu cần dữ liệu của module khác → gọi Port interface trong Application Service
- Chỉ JOIN các bảng trong cùng một module

## Checklist Infrastructure

- [ ] `{Model}Entity` có `@Entity` + `@Table`
- [ ] `{Model}JpaRepository` extends `JpaRepository<{Model}Entity, Long>`
- [ ] `{Model}RepositoryImpl` implements `{Model}Repository` (Domain interface)
- [ ] `RepositoryImpl` có `@Repository`
- [ ] Có 2 method riêng: `toDomain()` và `toEntity()` để chuyển đổi
- [ ] Không JOIN bảng giữa các module
- [ ] Status dùng `@Enumerated(EnumType.STRING)`
