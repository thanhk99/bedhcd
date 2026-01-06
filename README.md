# Spring Boot Backend - Quick Start Guide

## 📋 Tóm Tắt

Cấu trúc Spring Boot backend hoàn chỉnh với:
- ✅ JWT Authentication (Access + Refresh token)
- ✅ BCrypt password encryption
- ✅ HTTP-only cookie cho refresh token
- ✅ RESTful API endpoints
- ✅ Global exception handling

---

## 🚀 Chạy Ứng Dụng

### 1. Cấu hình Database

Tạo PostgreSQL database:
```sql
CREATE DATABASE bedhcd;
```

Cập nhật password trong `src/main/resources/application.properties`:
```properties
spring.datasource.password=your_password_here
```

### 2. Build & Run

```bash
# Build
.\mvnw.cmd clean compile

# Run
.\mvnw.cmd spring-boot:run
```

Server: `http://localhost:8080/api`

---

## 📡 API Endpoints

### Authentication (`/api/auth`)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/auth/register` | Đăng ký user mới |
| POST | `/auth/login` | Đăng nhập (trả về access token + set refresh token cookie) |
| POST | `/auth/refresh` | Làm mới access token từ cookie |
| POST | `/auth/logout` | Đăng xuất và xóa cookie |

### User Management (`/api/users`) - Cần Authentication

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/users/profile` | Lấy thông tin user hiện tại |
| PUT | `/users/profile` | Cập nhật profile |
| PUT | `/users/password` | Đổi mật khẩu |

---

## 🧪 Test với Postman

### 1. Register
```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123",
  "fullName": "Test User"
}
```

### 2. Login
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

Response sẽ chứa `accessToken` - copy token này.

### 3. Access Protected Endpoint
```http
GET http://localhost:8080/api/users/profile
Authorization: Bearer <paste_access_token_here>
```

---

## 📁 Cấu Trúc Project

```
src/main/java/com/api/bedhcd/
├── config/          # JWT, Security, Cookie configuration
├── controller/      # REST endpoints
├── dto/            # Request/Response objects
├── entity/         # Database entities
├── exception/      # Error handling
├── repository/     # Database access
└── service/        # Business logic
```

---

## 🔐 Security Features

- **JWT Access Token:** 15 phút
- **JWT Refresh Token:** 7 ngày (HTTP-only cookie)
- **Password:** BCrypt encryption
- **CORS:** Configured cho localhost:3000, localhost:5173

---

## ⚙️ Configuration

Tất cả cấu hình trong `application.yml`:
- Database connection
- JWT secret & expiration
- Cookie settings
- Server port (8080)

---

## 📝 Next Steps

1. ✅ Cấu hình database password
2. ✅ Chạy application
3. ✅ Test API endpoints
4. 🔜 Thêm business logic của bạn
5. 🔜 Deploy to production

---

## 🐛 Troubleshooting

**Database connection error?**
→ Kiểm tra MySQL đang chạy và password đúng

**401 Unauthorized?**
→ Verify access token trong Authorization header

**Cookie không được set?**
→ Kiểm tra CORS configuration

---

## 📚 Dependencies

- Spring Boot 3.5.9
- Spring Security
- Spring Data JPA
- MySQL Connector
- JJWT 0.12.3
- Lombok

**Build Status:** ✅ SUCCESS
