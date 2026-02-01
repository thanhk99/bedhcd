# 📚 TÀI LIỆU API - HỆ THỐNG QUẢN LÝ ĐẠI HỘI CỔ ĐÔNG

## Mục lục
1. [Authentication](#1-authentication)
2. [Users](#2-users)
3. [Meetings](#3-meetings)
4. [Resolutions (Nghị quyết)](#4-resolutions-nghị-quyết)
5. [Elections (Bầu cử)](#5-elections-bầu-cử)
6. [Proxy Delegation (Ủy quyền)](#6-proxy-delegation-ủy-quyền)
7. [Dashboard](#7-dashboard)
8. [Enums & Models](#8-enums--models)

---

## 1. Authentication

Base URL: `/auth`

### 1.1. Đăng ký

**POST** `/auth/register`

Đăng ký tài khoản mới.

**Request Body:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "fullName": "string",
  "phoneNumber": "string",
  "investorCode": "string",
  "cccd": "string",
  "dateOfIssue": "string",
  "placeOfIssue": "string",
  "address": "string",
  "sharesOwned": 1000,
  "meetingId": "string"
}
```

**Response:**
```json
{
  "accessToken": "string",
  "tokenType": "Bearer",
  "userId": "string",
  "username": "string",
  "email": "string",
  "roles": ["ROLE_USER"]
}
```

### 1.2. Đăng nhập

**POST** `/auth/login`

**Request Body:**
```json
{
  "identifier": "string",
  "password": "string"
}
```

**Response:**
```json
{
  "accessToken": "string",
  "tokenType": "Bearer",
  "userId": "string",
  "fullName": "string",
  "email": "string",
  "roles": ["ROLE_USER"]
}
```

**Note:** Refresh token được trả về trong HTTP-only cookie.

### 1.3. Làm mới token

**POST** `/auth/refresh`

Làm mới access token sử dụng refresh token từ cookie.

**Response:**
```json
{
  "accessToken": "string",
  "tokenType": "Bearer",
  "userId": "string",
  "fullName": "string",
  "email": "string",
  "roles": ["ROLE_USER"]
}
```

### 1.4. Đăng xuất

**POST** `/auth/logout`

**Response:**
```json
{
  "message": "Logged out successfully"
}
```

---

### 1.5. Đăng nhập bằng mã QR (Magic Login)

#### 1.5.1. Tạo mã QR (Admin)

**POST** `/auth/qr/generate`

🔒 Yêu cầu: `ROLE_ADMIN`

Action Admin tạo mã QR (Magic Link) cho một người dùng cụ thể.

**Request Body:**
```json
{
  "userId": "string",
  "expiresAt": "2026-03-25T17:00:00" 
}
```
*(`expiresAt` là tùy chọn, nếu không gửi sẽ mặc định hết hạn sau 24h)*

**Response:**
```json
{
  "token": "string",
  "qrContent": "http://frontend-url/login/qr?token=..."
}
```

#### 1.5.2. Đăng nhập bằng Magic Token

**POST** `/auth/qr/magic-login`

Người dùng (hoặc thiết bị) sử dụng token từ mã QR để đăng nhập không cần mật khẩu.

**Request Body:**
```json
{
  "token": "string"
}
```

**Response:**
```json
{
  "accessToken": "string",
  "tokenType": "Bearer",
  "refreshToken": "string",
  "userId": "string",
  "email": "string",
  "roles": ["ROLE_USER"]
}
```

### 1.5.3. Lấy lại Token QR (Admin)

**GET** `/auth/qr/token/{userId}`

🔒 Yêu cầu: `ROLE_ADMIN`

Admin lấy token QR đang còn hiệu lực của người dùng (nếu có) để tạo lại mã QR mà không cần tạo token mới.

**Response:**
```json
{
  "token": "string",
  "qrContent": "http://frontend-url/login/qr?token=..."
}
```

---

## 2. Users

Base URL: `/users`

### 2.1. Lấy danh sách người dùng

**GET** `/users`

🔒 Yêu cầu: `ROLE_ADMIN`

**Response:**
```json
[
  {
    "id": "string",
    "cccd": "string",
    "investorCode": "string",
    "fullName": "string",
    "email": "string",
    "sharesOwned": 1000,
    "roles": ["ROLE_USER"]
  }
]
```

### 2.2. Tạo người dùng mới

**POST** `/users`

🔒 Yêu cầu: `ROLE_ADMIN`

**Request Body:**
```json
{
  "email": "string",
  "password": "string",
  "fullName": "string",
  "phoneNumber": "string",
  "investorCode": "string",
  "cccd": "string",
  "dateOfIssue": "string",
  "address": "string",
  "nation": "string",
  "sharesOwned": 1000,
  "meetingId": "string"
}
```

### 2.3. Tìm kiếm người dùng theo CCCD
**GET** `/users/search`

🔒 Yêu cầu: `ROLE_ADMIN`

**Query Parameters:**
- `keyword`: string (Số CCCD hoặc một phần số CCCD)

**Response:**
```json
[
  {
    "id": "string",
    "cccd": "string",
    "investorCode": "string",
    "fullName": "string",
    "email": "string",
    "sharesOwned": 1000,
    "roles": ["ROLE_USER"]
  }
]
```

### 2.4. Lấy thông tin profile hiện tại

**GET** `/users/profile`

**Response:**
```json
{
  "id": "string",
  "username": "string",
  "email": "string",
  "fullName": "string",
  "phoneNumber": "string",
  "investorCode": "string",
  "cccd": "string",
  "dateOfIssue": "string",
  "placeOfIssue": "string",
  "address": "string",
  "sharesOwned": 1000,
  "receivedProxyShares": 500,
  "delegatedShares": 0,
  "totalShares": 1500,
  "roles": ["ROLE_USER"],
  "enabled": true,
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2024-01-01T00:00:00"
}
```

> [!IMPORTANT]
> Từ phiên bản này, các trường `receivedProxyShares`, `delegatedShares` và `totalShares` sẽ được trả về dựa trên ngữ cảnh cuộc họp đang diễn ra (`ONGOING`). Nếu không có cuộc họp nào đang diễn ra, các giá trị này sẽ mặc định về dữ liệu tổng quát (thường là 0 nếu không có uỷ quyền mặc định).

### 2.4. Lấy thông tin người dùng theo ID

**GET** `/users/{id}`

**Response:** Giống như `/users/profile`

### 2.5. Cập nhật profile

**PUT** `/users/profile`

**Query Parameters:**
- `fullName` (optional): string
- `email` (optional): string

**Response:**
```json
{
  "id": "123456",
  "email": "user@example.com",
  "fullName": "Nguyễn Văn A",
  "sharesOwned": 1000,
  "receivedProxyShares": 0,
  "delegatedShares": 0,
  "totalShares": 1000,
  "phoneNumber": "0987654321",
  "investorCode": "VIX123",
  "cccd": "012345678901",
  "dateOfIssue": "2020-01-01",
  "placeOfIssue": "CA Hà Nội",
  "address": "Số 1 Đại Cồ Việt, Hà Nội",
  "nation": "Việt Nam",
  "roles": ["ROLE_SHAREHOLDER"],
  "enabled": true,
  "createdAt": "2026-01-23T00:00:00",
  "updatedAt": "2026-01-23T00:41:00"
}
```

### 2.6. Đổi mật khẩu

**PUT** `/users/password`

**Query Parameters:**
- `oldPassword`: string
- `newPassword`: string

**Response:**
```json
{
  "message": "Password changed successfully"
}
```

### 2.7. Cập nhật vai trò người dùng

**PUT** `/users/{id}/roles`

🔒 Yêu cầu: `ROLE_ADMIN`

**Request Body:**
```json
["ROLE_USER", "ROLE_ADMIN"]
```

### 2.8. Cập nhật thông tin người dùng

**PUT** `/users/{id}`

🔒 Yêu cầu: `ROLE_ADMIN`

**Request Body:**
```json
{
  "cccd": "string",
  "investorCode": "string",
  "fullName": "string",
  "email": "string",
  "phoneNumber": "string",
  "dateOfIssue": "string",
  "address": "string",
  "nation": "string",
  "sharesOwned": 1000
}
```

### 2.9. Xóa người dùng

**DELETE** `/users/{id}`

🔒 Yêu cầu: `ROLE_ADMIN`

**Response:** 204 No Content

### 2.10. Cấp lại mật khẩu (Admin)

**POST** `/users/{id}/reset-password`

🔒 Yêu cầu: `ROLE_ADMIN`

Sinh mật khẩu mới cho người dùng.

**Response:**
```json
{
  "newPassword": "string"
}
```

### 2.11. Lấy lịch sử biểu quyết của tôi

**GET** `/users/me/votes`

**Response:**
```json
[
  {
    "voteId": "string",
    "resolutionId": "string",
    "resolutionTitle": "string",
    "meetingId": "string",
    "meetingTitle": "string",
    "votingOptionId": "string",
    "votingOptionName": "string",
    "voteWeight": 1000,
    "ipAddress": "192.168.1.1",
    "userAgent": "Mozilla/5.0...",
    "votedAt": "2026-01-10T10:00:00",
    "action": "VOTE_CAST"
  }
]
```

### 2.12. Lấy lịch sử đăng nhập của tôi

**GET** `/users/me/login-history`

**Response:**
```json
[
  {
    "id": 1,
    "loginTime": "2026-01-10T09:00:00",
    "logoutTime": "2026-01-10T11:00:00",
    "ipAddress": "192.168.1.1",
    "userAgent": "Mozilla/5.0...",
    "location": "Unknown",
    "status": "SUCCESS",
    "failureReason": null
  }
]
```

### 2.13. Lấy lịch sử biểu quyết của người dùng (Admin)

**GET** `/users/{id}/votes`

🔒 Yêu cầu: `ROLE_ADMIN`

**Response:** Giống như `/users/me/votes`

### 2.14. Lấy lịch sử đăng nhập của người dùng (Admin)

**GET** `/users/{id}/login-history`

🔒 Yêu cầu: `ROLE_ADMIN`

**Response:** Giống như `/users/me/login-history`

---

## 3. Meetings

Base URL: `/meetings`

### 3.1. Tạo cuộc họp mới

**POST** `/meetings`

🔒 Yêu cầu: `ROLE_ADMIN`

**Request Body:**
```json
{
  "title": "Đại hội cổ đông thường niên 2026",
  "description": "Đại hội cổ đông thường niên năm 2026",
  "location": "Hội trường A, Tầng 5, Tòa nhà ABC",
  "startTime": "2026-03-15T09:00:00",
  "endTime": "2026-03-15T17:00:00"
}
```

**Response:**
```json
{
  "id": "123456",
  "title": "Đại hội cổ đông thường niên 2026",
  "description": "Đại hội cổ đông thường niên năm 2026",
  "location": "Hội trường A, Tầng 5, Tòa nhà ABC",
  "startTime": "2026-03-15T09:00:00",
  "endTime": "2026-03-15T17:00:00",
  "status": "SCHEDULED",
  "resolutions": [],
  "elections": []
}
```

### 3.2. Lấy danh sách tất cả cuộc họp

**GET** `/meetings`

**Response:**
```json
[
  {
    "id": "123456",
    "title": "Đại hội cổ đông thường niên 2026",
    "description": "Nghị quyết và bầu cử nhiệm kỳ mới",
    "location": "Hội trường Thống Nhất",
    "startTime": "2026-03-15T09:00:00",
    "endTime": "2026-03-15T17:00:00",
    "status": "SCHEDULED",
    "resolutions": [
      {
        "id": "654321",
        "title": "Thông qua BCTC 2025",
        "description": "...",
        "votingOptions": [...],
        "userVotes": [...]
      }
    ],
    "elections": [
      {
        "id": "111222",
        "title": "Bầu HĐQT",
        "electionType": "BOARD_OF_DIRECTORS",
        "votingOptions": [...],
        "userVotes": [...]
      }
    ]
  }
]
```

### 3.3. Lấy cuộc họp đang diễn ra

**GET** `/meetings/ongoing`

**Response:**
```json
{
  "id": "123456",
  "title": "Đại hội cổ đông thường niên 2026",
  "status": "ONGOING",
  "resolutions": [
    {
      "id": "654321",
      "title": "Nghị quyết 1",
      "userVotes": [
        {
          "votingOptionId": "789012",
          "votingOptionName": "Đồng ý",
          "voteWeight": 1000
        }
      ]
    }
  ],
  "elections": [
    {
      "id": "111222",
      "title": "Bầu cử BKS",
      "userVotes": [
        {
          "votingOptionId": "333444",
          "votingOptionName": "Ứng viên A",
          "voteWeight": 500
        }
      ]
    }
  ]
}
```

**Note:** Trả về 204 No Content nếu không có cuộc họp nào đang diễn ra.

### 3.4. Lấy thông tin cuộc họp theo ID

**GET** `/meetings/{id}`

**Response:** Giống như response của POST `/meetings`

### 3.5. Cập nhật cuộc họp

**PUT** `/meetings/{id}`

🔒 Yêu cầu: `ROLE_ADMIN`

**Request Body:**
```json
{
  "title": "string",
  "description": "string",
  "location": "string",
  "startTime": "2026-03-15T09:00:00",
  "endTime": "2026-03-15T17:00:00"
}
```

### 3.6. Xóa cuộc họp

**DELETE** `/meetings/{id}`

🔒 Yêu cầu: `ROLE_ADMIN`

**Response:** 204 No Content

### 3.7. Cập nhật trạng thái cuộc họp

**PATCH** `/meetings/{id}/status`

🔒 Yêu cầu: `ROLE_ADMIN`

**Query Parameters:**
- `status`: enum (SCHEDULED, ONGOING, COMPLETED, CANCELLED)

**Response:** 200 OK

### 3.8. Lấy danh sách cổ đông của cuộc họp

**GET** `/meetings/{id}/shareholders`

Lấy danh sách tất cả người dùng kèm theo thông tin cổ phần và uỷ quyền trong ngữ cảnh của cuộc họp cụ thể này.

**Response:**
```json
[
  {
    "id": "string",
    "cccd": "string",
    "investorCode": "string",
    "fullName": "string",
    "email": "string",
    "sharesOwned": 1000,
    "receivedProxyShares": 500,
    "delegatedShares": 0,
    "totalShares": 1500,
    "roles": ["ROLE_USER"]
  }
]
```

> [!NOTE]
> Thông tin `receivedProxyShares` và `delegatedShares` được tính toán riêng biệt cho từng cuộc họp.

### 3.9. Lấy trạng thái biểu quyết Realtime (Snapshot)

**GET** `/meetings/{id}/realtime`

Trả về dữ liệu tổng hợp kết quả biểu quyết và bầu cử hiện tại. API này dùng để load dữ liệu ban đầu trước khi kết nối WebSocket.

**Response:**
```json
{
  "meetingId": "123456",
  "resolutionResults": [
    {
       "resolutionId": "...",
       "results": [...]
    }
  ],
  "electionResults": [
    {
       "electionId": "...",
       "results": [...]
    }
  ]
}
```

---

## 4. Resolutions (Nghị quyết)

### 4.1. Tạo nghị quyết mới

**POST** `/meetings/{meetingId}/resolutions`

🔒 Yêu cầu: `ROLE_ADMIN`

**Request Body:**
```json
{
  "title": "Nghị quyết về thông qua báo cáo tài chính năm 2025",
  "description": "Nghị quyết về việc phê duyệt báo cáo tài chính...",
  "displayOrder": 1
}
```

**Response:**
```json
{
  "id": "654321",
  "title": "Nghị quyết về thông qua báo cáo tài chính năm 2025",
  "description": "...",
  "displayOrder": 1,
  "votingOptions": []
}
```

### 4.2. Lấy thông tin nghị quyết

**GET** `/resolutions/{resolutionId}`

**Response:**
```json
{
  "id": "654321",
  "title": "Nghị quyết về thông qua báo cáo tài chính năm 2025",
  "description": "...",
  "displayOrder": 1,
  "votingOptions": [
    {
      "id": "789012",
      "name": "Đồng ý",
      "position": null,
      "bio": null,
      "photoUrl": null,
      "displayOrder": 1
    },
    {
      "id": "789013",
      "name": "Không đồng ý",
      "position": null,
      "bio": null,
      "photoUrl": null,
      "displayOrder": 2
    }
  ],
  "userVotes": [
    {
      "votingOptionId": "789012",
      "votingOptionName": "Đồng ý",
      "voteWeight": 1000,
      "votedAt": "2026-01-07T22:00:00"
    }
  ]
}
```

### 4.3. Cập nhật nghị quyết

**PUT** `/resolutions/{resolutionId}`

🔒 Yêu cầu: `ROLE_ADMIN`

**Request Body:**
```json
{
  "title": "string",
  "description": "string",
  "displayOrder": 1
}
```

**Response:**
```json
{
  "id": "654321",
  "title": "Nghị quyết...",
  "description": "...",
  "displayOrder": 1,
  "createdAt": "..."
}
```
*(Chỉ trả về thông tin cơ bản)*

### 4.4. Lấy thông tin lựa chọn biểu quyết

**GET** `/options/{optionId}`

**Response:**
```json
{
  "id": "789012",
  "name": "Đồng ý",
  "position": null,
  "bio": null,
  "photoUrl": null,
  "displayOrder": 1
}
```

### 4.5. Cập nhật lựa chọn/ứng viên

**PUT** `/options/{optionId}`

🔒 Yêu cầu: `ROLE_ADMIN`

Cập nhật thông tin của một lựa chọn biểu quyết hoặc ứng viên bầu cử.

**Request Body:**
```json
{
  "name": "string",
  "position": "string",
  "bio": "string",
  "photoUrl": "string",
  "displayOrder": 1
}
```

**Response:** [VotingOptionResponse](#8-enums--models)

### 4.6. Xóa lựa chọn/ứng viên

**DELETE** `/options/{optionId}`

🔒 Yêu cầu: `ROLE_ADMIN`

Xóa một lựa chọn hoặc ứng viên. Các phiếu bầu liên quan cũng sẽ bị xóa.


**Response:** 204 No Content

### 4.7. Biểu quyết nghị quyết

**POST** `/resolutions/{resolutionId}/vote`

Người dùng thực hiện biểu quyết cho nghị quyết.

**Request Body:**
```json
{
  "optionVotes": [
    {
      "votingOptionId": "789012"
    }
  ]
}
```
*(Ghi chú: Đối với Nghị quyết, chỉ được phép chọn duy nhất 1 lựa chọn: Đồng ý, Không đồng ý hoặc Không ý kiến. Nếu không truyền `voteWeight`, hệ thống tự động gán toàn bộ quyền biểu quyết)*

**Response:** 200 OK

### 4.8. Lưu bản nháp biểu quyết

**POST** `/resolutions/{resolutionId}/draft`

Lưu lại lựa chọn biểu quyết dưới dạng bản nháp (chưa tính vào kết quả chính thức).

**Request Body:** Giống như API biểu quyết.

**Response:** 200 OK

### 4.9. Lấy kết quả biểu quyết

**GET** `/resolutions/{resolutionId}/results`

**Response:**
```json
{
  "meetingId": "123456",
  "meetingTitle": "Đại hội cổ đông 2026",
  "resolutionId": "654321",
  "resolutionTitle": "Thông qua BCTC 2025",
  "results": [
    {
      "votingOptionId": "789012",
      "votingOptionName": "Đồng ý",
      "voteCount": 150,
      "totalWeight": 1500000,
      "percentage": 75.0
    },
    {
      "votingOptionId": "789013",
      "votingOptionName": "Không đồng ý",
      "voteCount": 50,
      "totalWeight": 500000,
      "percentage": 25.0
    }
  ],
  "totalVoters": 200,
  "totalWeight": 2000000,
  "createdAt": "2026-01-23T01:00:00"
}
```

---

## 5. Elections (Bầu cử)

### 5.1. Tạo bầu cử mới

**POST** `/meetings/{meetingId}/elections`

🔒 Yêu cầu: `ROLE_ADMIN`

**Request Body:**
```json
{
  "title": "Bầu cử Hội đồng quản trị nhiệm kỳ 2026-2030",
  "description": "Bầu cử 5 thành viên Hội đồng quản trị",
  "electionType": "BOARD_OF_DIRECTORS",
  "displayOrder": 1
}
```

**Response:**
```json
{
  "id": "111222",
  "title": "Bầu cử Hội đồng quản trị nhiệm kỳ 2026-2030",
  "description": "Bầu cử 5 thành viên Hội đồng quản trị",
  "electionType": "BOARD_OF_DIRECTORS",
  "displayOrder": 1,
  "votingOptions": [],
  "userVotes": null,
  "votingPower": 0
}
```

### 5.2. Lấy thông tin bầu cử

**GET** `/elections/{electionId}`

**Response:**
```json
{
  "id": "111222",
  "title": "Bầu cử Hội đồng quản trị nhiệm kỳ 2026-2030",
  "description": "Bầu cử 5 thành viên Hội đồng quản trị",
  "electionType": "BOARD_OF_DIRECTORS",
  "displayOrder": 1,
  "votingOptions": [
    {
      "id": "333444",
      "name": "Nguyên Văn A",
      "position": "Ứng viên HĐQT",
      "bio": "Kinh nghiệm 15 năm trong lĩnh vực tài chính...",
      "photoUrl": "https://example.com/photos/nguyen-van-a.jpg",
      "displayOrder": 1
    }
  ],
  "userVotes": [
    {
      "votingOptionId": "333444",
      "votingOptionName": "Nguyễn Văn A",
      "voteWeight": 500,
      "votedAt": "2026-01-07T22:10:00"
    }
  ],
  "votingPower": 1500
}
```

### 5.3. Cập nhật bầu cử

**POST** `/elections/{electionId}/edit`

🔒 Yêu cầu: `ROLE_ADMIN`

**Request Body:**
```json
{
  "title": "Bầu cử HĐQT 2026-2030 (Cập nhật)",
  "description": "Cập nhật mô tả",
  "electionType": "BOARD_OF_DIRECTORS",
  "displayOrder": 1
}
```

**Response:**
```json
{
  "id": "111222",
  "title": "Bầu cử HĐQT 2026-2030 (Cập nhật)",
  "description": "Cập nhật mô tả",
  "electionType": "BOARD_OF_DIRECTORS",
  "displayOrder": 1
}
```

### 5.4. Thêm ứng viên/lựa chọn vào bầu cử

**POST** `/elections/{electionId}/options`

🔒 Yêu cầu: `ROLE_ADMIN`

**Request Body:**
```json
{
  "name": "Trần Thị B",
  "position": "Ứng viên HĐQT",
  "bio": "Thạc sĩ Kinh tế...",
  "photoUrl": "http://...",
  "displayOrder": 2
}
```

**Response:** [VotingOptionResponse](#8-enums--models)

### 5.5. Bỏ phiếu bầu cử

**POST** `/elections/{electionId}/vote`

Người dùng thực hiện bầu cử dồn phiếu hoặc bầu thông thường.

**Request Body:**
```json
{
  "optionVotes": [
    {
      "votingOptionId": "333444",
      "voteWeight": 500
    },
    {
      "votingOptionId": "333445",
      "voteWeight": 500
    }
  ]
}
```

**Note:**
- Có thể bỏ phiếu cho nhiều lựa chọn/ứng viên.
- `voteWeight` là số cổ phiếu gán cho lựa chọn này.
- Tổng `voteWeight` không được vượt quá `votingPower` của người dùng.

**Response:** 200 OK

### 5.6. Lưu nháp bỏ phiếu bầu cử

**POST** `/elections/{electionId}/draft`

**Request Body:** Giống như API bỏ phiếu bầu cử.

**Response:** 200 OK

### 5.7. Xem kết quả bầu cử

**GET** `/elections/{electionId}/results`

**Response:**
```json
{
  "meetingId": "123456",
  "meetingTitle": "Đại hội cổ đông 2026",
  "electionId": "111222",
  "electionTitle": "Bầu cử HĐQT",
  "results": [
    {
      "votingOptionId": "333444",
      "votingOptionName": "Nguyễn Văn A",
      "voteCount": 42,
      "totalWeight": 850000,
      "percentage": 35.2
    },
    {
      "votingOptionId": "333445",
      "votingOptionName": "Trần Thị B",
      "voteCount": 38,
      "totalWeight": 750000,
      "percentage": 31.0
    }
  ],
  "totalVoters": 80,
  "totalWeight": 2400000,
  "createdAt": "2026-01-23T01:10:00"
}
```


---

## 6. Proxy Delegation (Ủy quyền)

Base URL: `/meetings/{meetingId}/proxy`

### 6.1. Tạo ủy quyền

**POST** `/meetings/{meetingId}/proxy`

**Request Body:**
```json
{
  "delegatorId": "user_123",
  "proxyId": "user_456",
  "sharesDelegated": 1000,
  "authorizationDocument": "Optional string"
}
```

**Response:**
```json
{
  "id": 1,
  "delegatorId": "user_123",
  "delegatorName": "Nguyễn Văn A",
  "proxyId": "user_456",
  "proxyName": "Trần Văn B",
  "sharesDelegated": 1000,
  "status": "ACTIVE",
  "createdAt": "2026-01-07T10:00:00"
}
```

### 6.2. Lấy danh sách ủy quyền của cuộc họp

**GET** `/meetings/{meetingId}/proxy`

**Response:**
```json
[
  {
    "id": 1,
    "delegatorId": "user_123",
    "delegatorName": "Nguyễn Văn A",
    "proxyId": "user_456",
    "proxyName": "Trần Văn B",
    "sharesDelegated": 1000,
    "status": "ACTIVE",
    "createdAt": "2026-01-07T10:00:00"
  }
]
```

### 6.3. Lấy danh sách uỷ quyền theo người uỷ quyền

Lấy danh sách các uỷ quyền mà người dùng này là NGƯỜI UỶ QUYỀN.

**GET** `/meetings/{meetingId}/proxy/delegator/{userId}`

**Response:** Giống như 6.2

### 6.4. Lấy danh sách uỷ quyền theo người được uỷ quyền

Lấy danh sách các uỷ quyền mà người dùng này là NGƯỜI ĐƯỢC UỶ QUYỀN.

**GET** `/meetings/{meetingId}/proxy/proxy/{userId}`

**Response:** Giống như 6.2

### 6.5. Thu hồi ủy quyền

**POST** `/meetings/{meetingId}/proxy/{delegationId}/revoke`

**Response:** 204 No Content

### 6.6. Thêm người được uỷ quyền (không phải cổ đông)

**POST** `/api/representatives`

🔒 Yêu cầu: `ROLE_ADMIN`

Tạo tài khoản người đại diện mới (nếu chưa có) và thực hiện uỷ quyền từ cổ đông sang người này. Hệ thống sẽ tự động sinh mật khẩu cho người đại diện.

**Request Body:**
```json
{
  "fullName": "Nguyễn Văn Đại Diện",
  "cccd": "012345678901",
  "dateOfIssue": "2020-01-01",
  "address": "Hà Nội",
  "email": "daidien@example.com",
  "phoneNumber": "0987654321",
  "meetingId": "123456",
  "delegatorCccd": "987654321098",
  "sharesDelegated": 500
}
```

**Response:**
```json
{
  "id": "654321",
  "fullName": "Nguyễn Văn Đại Diện",
  "cccd": "012345678901",
  "generatedPassword": "87654321",
  "meetingId": "123456",
  "sharesDelegated": 500
}
```

> [!CAUTION]
> `generatedPassword` chỉ trả về một lần duy nhất trong phản hồi này. Admin cần lưu lại để cấp cho người đại diện.

---

## 7. Dashboard

Base URL: `/dashboard`

### 7.1. Lấy thống kê tổng quan

**GET** `/dashboard/summary`

🔒 Yêu cầu: `ROLE_ADMIN`

**Response:**
```json
{
  "totalUsers": 150,
  "totalMeetings": 5,
  "upcomingMeetings": 2,
  "completedMeetings": 3,
  "totalResolutions": 25,
  "totalElections": 3,
  "totalVotesCast": 1250,
  "averageParticipationRate": 78.5
}
```

---

## 8. Enums & Models

### 8.1. MeetingStatus

```java
public enum MeetingStatus {
    SCHEDULED,   // Đã lên lịch
    ONGOING,     // Đang diễn ra
    COMPLETED,   // Đã hoàn thành
    CANCELLED    // Đã hủy
}
```

### 8.2. VotingType

```java
public enum VotingType {
    YES_NO,           // Tán thành / Không tán thành
    YES_NO_ABSTAIN,   // Tán thành / Không tán thành / Không ý kiến
    MULTIPLE_CHOICE   // Lựa chọn nhiều phương án (chưa dùng cho resolution)
}
```

### 8.3. ElectionType

```java
public enum ElectionType {
    BOARD_OF_DIRECTORS,      // Bầu cử Hội đồng quản trị
    SUPERVISORY_BOARD,       // Bầu cử Ban kiểm soát
    OTHER                    // Bầu cử khác
}
```

### 8.4. DelegationStatus

```java
public enum DelegationStatus {
    ACTIVE,     // Đang hoạt động
    REVOKED,    // Đã thu hồi
    EXPIRED     // Đã hết hạn
}
```

### 8.5. Role

```java
public enum Role {
    ADMIN,
    SHAREHOLDER,
    REPRESENTATIVE
}
```

---

## 9. Realtime WebSocket (Socket.IO/STOMP)

Hệ thống sử dụng cơ chế **Asynchronous Broadcasting** để đảm bảo tốc độ phản hồi API cực nhanh. Khi người dùng vote, API chỉ xác thực và lưu vào DB, sau đó bắn event lên Kafka. Consumer sẽ tính toán lại và đẩy kết quả qua WebSocket.

### 9.1. Thông tin kết nối
- **URL**: `http://localhost:8085/api/ws`
- **Protocol**: SockJS + STOMP
- **Security**: Endpoint `/api/ws` yêu cầu xác thực hoặc được cho phép tùy theo cấu hình Security công ty.

### 9.2. Subscribe Channels

#### Meeting Updates (Realtime Results)
- **Topic**: `/topic/meeting/{meetingId}`
- **Payload**: `MeetingRealtimeStatus`
- **Mô tả**: Nhận cập nhật kết quả biểu quyết (Nghị quyết) và bầu cử theo thời gian thực.
- **Tần suất**: Đẩy ngay sau khi Kafka Consumer xử lý xong sự kiện vote (thường < 100ms).

**Dữ liệu trả về (Example):**
```json
{
  "meetingId": "123456",
  "resolutionResults": [
    {
      "resolutionId": "RES_01",
      "results": [
        { "votingOptionId": "1", "votingOptionName": "Đồng ý", "voteCount": 10, "totalWeight": 1000, "percentage": 83.3 },
        { "votingOptionId": "2", "votingOptionName": "Không đồng ý", "voteCount": 2, "totalWeight": 200, "percentage": 16.7 }
      ],
      "totalVoters": 12,
      "totalWeight": 1200
    }
  ],
  "electionResults": [
    {
       "electionId": "ELE_01",
       "results": [
         { "votingOptionId": "3", "votingOptionName": "Ứng viên A", "voteCount": 5, "totalWeight": 500, "percentage": 100.0 }
       ],
       "totalVoters": 5,
       "totalWeight": 500
    }
  ]
}
```

---

## 10. Kiến trúc Kafka (Hệ thống nội bộ)

Phần này dành cho việc phát triển và giám sát hệ thống. Frontend không tương tác trực tiếp với Kafka nhưng cần hiểu cơ chế để xử lý UI hợp lý.

### 10.1. Topics & Partitions
- **Topic**: `vote_events`
- **Số partitions**: 3
- **Partition Key**: `meetingId` (Đảm bảo thứ tự phiếu bầu trong cùng 1 cuộc họp luôn đúng).

### 10.2. Cấu trúc Event (VoteEvent)
Khi có người vote, backend bắn event lên topic `vote_events` với định dạng:

```json
{
  "meetingId": "string",
  "itemId": "string (resolutionId hoặc electionId)",
  "type": "RESOLUTION / ELECTION",
  "action": "VOTE_CAST / VOTE_CHANGED",
  "timestamp": "ISO8601"
}
```

### 10.3. Lợi ích cho FE
- **API Latency**: Giảm từ ~2s xuống < 50ms. FE có thể đóng modal hoặc chuyển trạng thái "Đã vote" ngay lập tức mà không cần đợi server tính toán xong.
- **Data Consistency**: Nhờ Partition Key, các cập nhật dồn dập sẽ không gây xung đột số liệu. FE luôn nhận được bản snapshot mới nhất từ WebSocket.
