---
name: error-handling-pattern
description: Dùng khi cần xử lý lỗi: tạo Domain Exception, viết GlobalExceptionHandler, hoặc trả về lỗi theo chuẩn ApiResponse. Đảm bảo lỗi nghiệp vụ được định nghĩa ở Domain và được map sang HTTP status đúng kỹ thuật tại tầng Shared.
---

# Skill: Xử Lý Lỗi Chuẩn

## Nguyên tắc

1. **Exception nghiệp vụ định nghĩa trong `domain/exception/`** — Không throw `RuntimeException` thẳng.
2. **Global exception handler** nằm trong `shared/` hoặc `api/advice/` — Map exception sang HTTP status.
3. **Luôn trả `ApiResponse`** — Kể cả khi lỗi.
4. **Static factory method cho exception** — Tạo `{Module}Exception.notFound()`, `.unauthorized()`, ...

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

    // Factory methods — mỗi loại lỗi 1 method riêng
    public static {Module}Exception notFound(Long id) {
        return new {Module}Exception(HttpStatus.NOT_FOUND,
            "{Model} không tồn tại: " + id);
    }

    public static {Module}Exception unauthorized(String action) {
        return new {Module}Exception(HttpStatus.FORBIDDEN,
            "Không có quyền thực hiện: " + action);
    }

    public static {Module}Exception invalidState(String message) {
        return new {Module}Exception(HttpStatus.CONFLICT, message);
    }

    public static {Module}Exception badRequest(String message) {
        return new {Module}Exception(HttpStatus.BAD_REQUEST, message);
    }
}
```

## Template Global Exception Handler (trong shared/)

```java
package api.exchange.shared.exception;

import api.exchange.shared.dto.ApiResponse;
// Import các exception của từng module
import api.exchange.modules.p2p.domain.exception.P2PException;
import api.exchange.modules.wallet.domain.exception.WalletException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Xử lý tất cả Domain Exception theo pattern chung
    // VD: P2PException
    @ExceptionHandler(P2PException.class)
    public ResponseEntity<ApiResponse<Void>> handleP2P(P2PException ex) {
        return ResponseEntity
            .status(ex.getStatus())
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(WalletException.class)
    public ResponseEntity<ApiResponse<Void>> handleWallet(WalletException ex) {
        return ResponseEntity
            .status(ex.getStatus())
            .body(ApiResponse.error(ex.getMessage()));
    }

    // Validation error từ @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .findFirst().orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    // Catch-all cho unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        return ResponseEntity.internalServerError()
            .body(ApiResponse.error("Lỗi hệ thống, vui lòng thử lại"));
    }
}
```

## Template ApiResponse (shared/)

```java
package api.exchange.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Thành công", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
```

## Cách sử dụng trong Application Service

```java
// Cách đúng — dùng factory method
{Model} model = repository.findById(id)
    .orElseThrow(() -> {Module}Exception.notFound(id));

// Cách đúng — throw với message rõ ràng
if (!order.getBuyerUid().equals(userUid)) {
    throw {Module}Exception.unauthorized("Xác nhận thanh toán lệnh này");
}

// SAI — không throw RuntimeException trực tiếp
throw new RuntimeException("not found"); // ❌

// SAI — không throw exception của module khác
throw new WalletException(...); // ❌ trong P2PApplicationService
```

## Mapping HTTP Status

| Loại lỗi | HTTP Status |
|----------|------------|
| Không tìm thấy | `404 NOT_FOUND` |
| Không có quyền | `403 FORBIDDEN` |
| Token không hợp lệ | `401 UNAUTHORIZED` |
| Dữ liệu sai | `400 BAD_REQUEST` |
| Trạng thái không hợp lệ | `409 CONFLICT` |
| Dịch vụ ngoài lỗi | `503 SERVICE_UNAVAILABLE` |
| Lỗi server | `500 INTERNAL_SERVER_ERROR` |

## Checklist Error Handling

- [ ] Exception được khai báo trong `domain/exception/{Module}Exception.java`
- [ ] Có static factory method cho từng loại lỗi cụ thể
- [ ] `GlobalExceptionHandler` có `@RestControllerAdvice` trong `shared/`
- [ ] Mọi Domain Exception đều được handle và trả `ApiResponse.error()`
- [ ] Không throw `RuntimeException` trực tiếp trong Application Service
- [ ] HTTP status khớp với loại lỗi nghiệp vụ
