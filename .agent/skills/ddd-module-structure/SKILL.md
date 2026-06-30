---
name: ddd-module-structure
description: Dùng khi tạo module mới, thêm feature mới vào một module, hoặc khi cần biết nên đặt code ở layer nào (api/application/domain/infrastructure). Skill này giúp đảm bảo cấu trúc folder và phụ thuộc giữa các layer tuân theo chuẩn DDD Layered Architecture của project.
---

# Skill: Tạo Module Theo Chuẩn DDD

## Package gốc
```
api.exchange.modules.{module}
```

## Cấu trúc bắt buộc

```
modules/{module}/
├── api/v1/
│   ├── {Module}Controller.java       # REST endpoints, chỉ gọi Application Service
│   └── dto/
│       ├── request/   {Action}Request.java
│       └── response/  {Data}Response.java
│
├── application/
│   ├── service/       {Feature}ApplicationService.java
│   ├── port/          {Feature}Port.java   (interface giao tiếp liên module)
│   └── mapper/        {Model}Mapper.java
│
├── domain/
│   ├── model/         {Model}.java   (Aggregate Root, Entity, ValueObject)
│   ├── repository/    {Model}Repository.java  (interface thuần)
│   └── exception/     {Module}Exception.java
│
└── infrastructure/
    └── persistence/
        ├── {Model}Entity.java           (JPA Entity)
        ├── {Model}JpaRepository.java    (extends JpaRepository)
        └── {Model}RepositoryImpl.java   (implements Domain Repository)
```

## Quy tắc phụ thuộc (Dependency Rule)

| Layer | Được phép import | KHÔNG được import |
|-------|-----------------|-------------------|
| `domain/` | Không được import Spring, JPA, hoặc bất kỳ framework nào | Spring, JPA, Hibernate, bất kỳ module khác |
| `application/` | `domain/`, `shared/`, Port interface của module khác | `infrastructure/` của BẤT KỲ module nào |
| `api/` | `application/service`, `api/dto/` | `domain/` trực tiếp, `infrastructure/` |
| `infrastructure/` | `domain/repository`, `application/port`, Spring/JPA | Application Service của module khác |

## Quy tắc đặt tên

- **Controller**: `{Feature}Controller.java` — VD: `WalletController.java`
- **Application Service**: `{Feature}ApplicationService.java` — VD: `WalletApplicationService.java`
- **Domain Model**: Danh từ nghiệp vụ — VD: `Wallet`, `Order`, `P2PAd`
- **Domain Repository (interface)**: `{Model}Repository.java` — VD: `WalletRepository.java`
- **JPA Repository**: `{Model}JpaRepository.java` — VD: `WalletJpaRepository.java`
- **Repository Impl**: `{Model}RepositoryImpl.java` — VD: `WalletRepositoryImpl.java`
- **JPA Entity**: `{Model}Entity.java` — VD: `WalletEntity.java`
- **Port (giao tiếp liên module)**: `{Feature}Port.java` — VD: `FundingWalletPort.java`
- **Request DTO**: `{Action}Request.java` — VD: `CreateOrderRequest.java`
- **Response DTO**: `{Data}Response.java` — VD: `WalletResponse.java`
- **Mapper**: `{Model}Mapper.java` — VD: `WalletMapper.java`
- **Exception**: `{Module}Exception.java` — VD: `WalletException.java`

## Quy tắc giao tiếp liên Module

- **Module A muốn dùng dữ liệu của Module B:**
  1. Module B định nghĩa `interface` trong `application/port/`
  2. Module B cài đặt Port đó trong `application/service/` hoặc `infrastructure/`
  3. Module A chỉ inject Port interface (KHÔNG inject Service/Repository của B trực tiếp)

- **KHÔNG JOIN bảng giữa các Module trong Query.**
- **KHÔNG import class nội bộ (non-Port) của module khác.**

## Ví dụ Port Interface (application/port/)

```java
// Trong module wallet - định nghĩa public interface
public interface FundingWalletPort {
    BigDecimal getAvailableBalance(String userUid, String asset);
    void lockBalance(String userUid, String asset, BigDecimal amount);
    void unlockBalance(String userUid, String asset, BigDecimal amount);
}
```

## Ví dụ Domain Model (domain/model/)

```java
// KHÔNG có annotation Spring hay JPA ở đây
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Order {
    private Long id;
    private String userUid;
    private OrderStatus status;
    private LocalDateTime createdAt;

    // Factory method thay vì new Order() trực tiếp
    public static Order createNew(String userUid) {
        return Order.builder()
            .userUid(userUid)
            .status(OrderStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();
    }
}
```

## Checklist tạo module mới

- [ ] Tạo đủ 4 layer: `api/`, `application/`, `domain/`, `infrastructure/`
- [ ] Domain model không có Spring annotation
- [ ] Repository interface nằm trong `domain/repository/`
- [ ] JPA Entity và JPA Repository nằm trong `infrastructure/persistence/`
- [ ] Controller chỉ gọi Application Service, không gọi Repository
- [ ] Giao tiếp với module khác qua Port interface
- [ ] Exception nghiệp vụ nằm trong `domain/exception/`
- [ ] Shared utilities nằm trong `shared/` (không đặt vào module cụ thể)
