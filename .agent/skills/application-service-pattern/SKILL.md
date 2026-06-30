---
name: application-service-pattern
description: Dùng khi viết Application Service (use case logic), tạo Mapper, hoặc định nghĩa Port cho giao tiếp liên module. Skill này đảm bảo Application Layer đúng trách nhiệm: điều phối domain, quản lý transaction, và KHÔNG chứa business rules.
---

# Skill: Viết Application Service Chuẩn

## Nguyên tắc Application Service

1. **Điều phối** — Orchestrate domain objects và infrastructure, KHÔNG tự tính toán business logic.
2. **Transaction** — Quản lý `@Transactional` tại đây.
3. **Không có business rules** — Validation nghiệp vụ thuộc về Domain model.
4. **Giao tiếp liên module qua Port** — Không inject Service/Repository của module khác trực tiếp.

## Template Application Service

```java
package api.exchange.modules.{module}.application.service;

import api.exchange.modules.{module}.domain.exception.{Module}Exception;
import api.exchange.modules.{module}.domain.model.{Model};
import api.exchange.modules.{module}.domain.repository.{Model}Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class {Feature}ApplicationService {

    private final {Model}Repository {model}Repository;
    // Port của module khác (không inject Service của module khác)
    // private final OtherModulePort otherPort;

    @Transactional
    public {Model} create{Model}(/* params */) {
        // 1. Lấy dữ liệu cần thiết
        // 2. Gọi factory method của Domain Model
        {Model} model = {Model}.createNew(/* params */);
        // 3. Gọi port liên module nếu cần
        // 4. Lưu và trả về
        return {model}Repository.save(model);
    }

    @Transactional(readOnly = true)
    public {Model} get{Model}(Long id) {
        return {model}Repository.findById(id)
            .orElseThrow(() -> {Module}Exception.notFound(id));
    }

    @Transactional(readOnly = true)
    public List<{Model}> getAll{Model}s(String userUid) {
        return {model}Repository.findByUserUid(userUid);
    }

    @Transactional
    public {Model} update{Model}(Long id, /* params */) {
        {Model} model = {model}Repository.findById(id)
            .orElseThrow(() -> {Module}Exception.notFound(id));
        // Gọi method trên domain model để thay đổi trạng thái
        // model.activate();
        return {model}Repository.save(model);
    }
}
```

## Template Mapper

```java
package api.exchange.modules.{module}.application.mapper;

import api.exchange.modules.{module}.api.v1.dto.response.{Model}Response;
import api.exchange.modules.{module}.api.v1.dto.request.{Action}Request;
import api.exchange.modules.{module}.domain.model.{Model};
import org.springframework.stereotype.Component;

@Component
public class {Model}Mapper {

    // Domain Model -> Response DTO
    public {Model}Response toResponse({Model} model) {
        if (model == null) return null;
        return {Model}Response.builder()
            .id(model.getId())
            // map các field
            .createdAt(model.getCreatedAt())
            .build();
    }

    // Request DTO -> tham số để gọi factory method (không tạo Domain từ DTO)
    // Mapper không nên tạo Domain Model từ DTO,
    // Application Service mới gọi Model.createNew() với các field trích từ Request
}
```

## Template Port Interface (giao tiếp liên module)

```java
package api.exchange.modules.{module}.application.port;

// Module này expose interface để module khác sử dụng
public interface {Feature}Port {
    // Chỉ expose những gì cần thiết
    SomeData getData(String userUid);
    void performAction(String userUid, BigDecimal amount);
}
```

## Thứ tự inject dependency

```java
@RequiredArgsConstructor
public class MyApplicationService {
    // 1. Domain Repositories của chính module
    private final OrderRepository orderRepository;
    // 2. Port của module khác (không inject Service/Repo khác)
    private final WalletPort walletPort;
    private final AuthPort authPort;
    // 3. Shared infrastructure (SSE, EventPublisher, ...)
    private final SseEmitterRegistry sseEmitterRegistry;
    private final ApplicationEventPublisher eventPublisher;
    // 4. Mapper của chính module
    private final OrderMapper orderMapper;
}
```

## Quy tắc Transaction

| Method | Annotation |
|--------|-----------|
| Tạo/Cập nhật/Xóa | `@Transactional` |
| Chỉ đọc | `@Transactional(readOnly = true)` |
| Không cần DB | Không cần annotation |

## Checklist Application Service

- [ ] Dùng `@Service` + `@RequiredArgsConstructor`
- [ ] `@Transactional` cho write, `@Transactional(readOnly = true)` cho read
- [ ] Không chứa business logic (validation, state rule) — để ở Domain Model
- [ ] Inject module khác qua Port interface, không dùng `@Autowired` Service/Repo khác
- [ ] Exception lấy từ `domain/exception/`, không throw `RuntimeException` trực tiếp
- [ ] Dùng `orElseThrow()` với factory method của exception
