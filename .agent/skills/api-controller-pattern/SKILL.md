---
name: api-controller-pattern
description: Dùng khi tạo REST Controller, viết DTO Request/Response, hoặc xử lý authentication từ SecurityContext. Skill này đảm bảo Presentation Layer đúng trách nhiệm: chỉ nhận request, ủy thác cho Application Service, và trả về ApiResponse chuẩn.
---

# Skill: Viết API Controller Chuẩn

## Nguyên tắc Presentation Layer

1. **Không chứa business logic** — Controller chỉ nhận request, gọi Service, trả response.
2. **Dùng `ApiResponse<T>` thống nhất** — Mọi endpoint đều wrap trong `ApiResponse`.
3. **Authentication từ SecurityContext** — Lấy email user từ `Authentication` object.
4. **DTO riêng** — Request và Response DTO riêng, không return Domain Model trực tiếp.

## Template Controller

```java
package api.exchange.modules.{module}.api.v1;

import api.exchange.modules.{module}.application.mapper.{Model}Mapper;
import api.exchange.modules.{module}.application.service.{Feature}ApplicationService;
import api.exchange.modules.{module}.api.v1.dto.request.{Action}Request;
import api.exchange.modules.{module}.api.v1.dto.response.{Model}Response;
import api.exchange.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/{module-path}")
@RequiredArgsConstructor
public class {Feature}Controller {

    private final {Feature}ApplicationService {feature}Service;
    private final {Model}Mapper {model}Mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<{Model}Response>>> getAll(Authentication auth) {
        String userEmail = auth.getName();
        List<{Model}Response> responses = {feature}Service.getAll{Model}s(userEmail)
            .stream().map({model}Mapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<{Model}Response>> getById(@PathVariable Long id) {
        {Model}Response response = {model}Mapper.toResponse({feature}Service.get{Model}(id));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<{Model}Response>> create(
            @RequestBody {Create}Request request,
            Authentication auth) {
        String userEmail = auth.getName();
        {Model} model = {feature}Service.create{Model}(userEmail, request);
        return ResponseEntity.ok(ApiResponse.success({model}Mapper.toResponse(model)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<{Model}Response>> update(
            @PathVariable Long id,
            @RequestBody {Update}Request request,
            Authentication auth) {
        String userEmail = auth.getName();
        {Model} updated = {feature}Service.update{Model}(id, userEmail, request);
        return ResponseEntity.ok(ApiResponse.success({model}Mapper.toResponse(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            Authentication auth) {
        String userEmail = auth.getName();
        {feature}Service.delete{Model}(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

## Template Request DTO

```java
package api.exchange.modules.{module}.api.v1.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class Create{Model}Request {
    @NotBlank(message = "Tài sản không được rỗng")
    private String asset;

    @NotNull(message = "Số tiền không được null")
    private BigDecimal amount;

    // Thêm các field cần thiết
}
```

## Template Response DTO

```java
package api.exchange.modules.{module}.api.v1.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class {Model}Response {
    private Long id;
    private String userUid;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
    // chỉ expose field cần thiết cho client
}
```

## URL Convention

```
GET    /api/v1/{module}              — Lấy danh sách
GET    /api/v1/{module}/{id}         — Lấy chi tiết
POST   /api/v1/{module}              — Tạo mới
PUT    /api/v1/{module}/{id}         — Cập nhật toàn bộ
PATCH  /api/v1/{module}/{id}/action  — Thay đổi trạng thái (VD: /cancel, /confirm)
DELETE /api/v1/{module}/{id}         — Xóa
```

## Lấy user từ Authentication

```java
// Trong method của Controller
public ResponseEntity<?> someMethod(Authentication auth) {
    String userEmail = auth.getName(); // Lấy email từ JWT token
    // Truyền email vào Application Service
}
```

## Checklist Controller

- [ ] `@RestController` + `@RequestMapping("/api/v1/...")`
- [ ] Dùng `@RequiredArgsConstructor` + inject ApplicationService và Mapper
- [ ] Mọi response wrap trong `ApiResponse<T>` và `ResponseEntity`
- [ ] Không inject Repository hay Domain trực tiếp vào Controller
- [ ] Không có business logic trong Controller
- [ ] Lấy user thông qua `Authentication auth` parameter
- [ ] Request DTO dùng `@Data`, Response DTO dùng `@Data @Builder`
- [ ] Validation dùng `@Valid` + annotation trên DTO fields
