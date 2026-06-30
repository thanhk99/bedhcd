---
name: domain-model-design
description: Dùng khi thiết kế hoặc chỉnh sửa Domain Model (Aggregate Root, Entity, Value Object), viết factory method, thêm business logic vào domain, hoặc định nghĩa Exception nghiệp vụ. Đảm bảo Domain Layer không phụ thuộc framework và chứa đúng business rules.
---

# Skill: Thiết Kế Domain Model Chuẩn DDD

## Nguyên tắc Domain Layer

1. **Không import Spring, JPA, Hibernate** — Domain model phải là POJO thuần.
2. **Business logic thuộc về Domain** — Các quy tắc kinh doanh (validation, state transition) viết ở đây, KHÔNG ở Application Service.
3. **Dùng Factory Method** thay vì gọi `new ModelName()` trực tiếp từ bên ngoài.
4. **Enum cho trạng thái** — Luôn dùng Enum thay vì String cho status/type.

## Template Domain Model

```java
package api.exchange.modules.{module}.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class {ModelName} {
    private Long id;
    private String userUid;
    private {ModelName}Status status; // Dùng Enum, không dùng String

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Factory method — tạo {model} mới với giá trị mặc định
     */
    public static {ModelName} createNew(/* params */) {
        return {ModelName}.builder()
            .userUid(userUid)
            .status({ModelName}Status.ACTIVE) // default state
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    // Business methods — logic nghiệp vụ đặt ở đây
    public void activate() {
        if (this.status != {ModelName}Status.INACTIVE) {
            throw new IllegalStateException("Chỉ kích hoạt được khi đang INACTIVE");
        }
        this.status = {ModelName}Status.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
}
```

## Template Enum Status

```java
package api.exchange.modules.{module}.domain.model;

public enum {ModelName}Status {
    ACTIVE,
    INACTIVE,
    CANCELLED,
    COMPLETED
}
```

## Template Domain Exception

```java
package api.exchange.modules.{module}.domain.exception;

import org.springframework.http.HttpStatus;

public class {Module}Exception extends RuntimeException {
    private final HttpStatus status;

    public {Module}Exception(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    // Static factory methods cho từng loại lỗi
    public static {Module}Exception notFound(Long id) {
        return new {Module}Exception(HttpStatus.NOT_FOUND,
            "{Model} không tồn tại với id: " + id);
    }

    public static {Module}Exception unauthorized(String action) {
        return new {Module}Exception(HttpStatus.FORBIDDEN,
            "Bạn không có quyền: " + action);
    }

    public static {Module}Exception invalidState(String message) {
        return new {Module}Exception(HttpStatus.CONFLICT, message);
    }
}
```

## Template Domain Repository Interface

```java
package api.exchange.modules.{module}.domain.repository;

import api.exchange.modules.{module}.domain.model.{ModelName};
import java.util.List;
import java.util.Optional;

// Interface thuần — không có Spring annotation
public interface {ModelName}Repository {
    Optional<{ModelName}> findById(Long id);
    List<{ModelName}> findByUserUid(String userUid);
    {ModelName} save({ModelName} entity);
    void deleteById(Long id);
}
```

## Quy tắc Value Object

Khi một nhóm fields luôn đi cùng nhau và có ý nghĩa nghiệp vụ riêng → tạo Value Object:

```java
// Ví dụ: Money, Address, PriceRange
@Value // Lombok — immutable
public class Money {
    BigDecimal amount;
    String currency;

    public Money add(Money other) {
        if (!this.currency.equals(other.currency))
            throw new IllegalArgumentException("Không thể cộng khác tiền tệ");
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

## Checklist Domain Model

- [ ] Không có import Spring (`@Service`, `@Repository`, `@Autowired`)
- [ ] Không có import JPA (`@Entity`, `@Table`, `@Column`)
- [ ] Status/Type dùng Enum, không dùng String
- [ ] Có Factory Method thay vì constructor công khai dùng trực tiếp
- [ ] Business rules (validate, state transition) nằm trong model
- [ ] Exception được định nghĩa trong `domain/exception/`
- [ ] Repository là interface thuần trong `domain/repository/`
