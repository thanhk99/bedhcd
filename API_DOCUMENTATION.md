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
  "cccd": "string",
  "investorCode": "string",
  "password": "string",
  "fullName": "string",
  "email": "string",
  "sharesOwned": 1000
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

### 1.2. Đăng nhập

**POST** `/auth/login`

**Request Body:**
```json
{
  "username": "string",
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
  "cccd": "string",
  "investorCode": "string",
  "password": "string",
  "fullName": "string",
  "email": "string",
  "sharesOwned": 1000
}
```

### 2.3. Lấy thông tin profile hiện tại

**GET** `/users/profile`

**Response:**
```json
{
  "id": "string",
  "cccd": "string",
  "investorCode": "string",
  "fullName": "string",
  "email": "string",
  "sharesOwned": 1000,
  "roles": ["ROLE_USER"]
}
```

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
  "id": "string",
  "fullName": "string",
  "email": "string",
  ...
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
  "sharesOwned": 1000
}
```

### 2.9. Xóa người dùng

**DELETE** `/users/{id}`

🔒 Yêu cầu: `ROLE_ADMIN`

**Response:** 204 No Content

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

### 4.3. Lấy thông tin lựa chọn biểu quyết

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

### 4.4. Cập nhật lựa chọn/ứng viên

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

### 4.5. Xóa lựa chọn/ứng viên

**DELETE** `/options/{optionId}`

🔒 Yêu cầu: `ROLE_ADMIN`

Xóa một lựa chọn hoặc ứng viên. Các phiếu bầu liên quan cũng sẽ bị xóa.


**Response:** 204 No Content

### 4.6. Biểu quyết nghị quyết

**POST** `/resolutions/{resolutionId}/vote`

**Request Body:**
```json
{
  "optionVotes": [
    {
      "votingOptionId": "789012",
      "voteWeight": 1000
    }
  ]
}
```

**Note:** 
- `voteWeight` là số cổ phiếu gán cho lựa chọn này
- Nếu không truyền `voteWeight`, hệ thống sẽ tự động gán toàn bộ quyền biểu quyết (vì nghị quyết chỉ cho bầu 1 phương án)

**Response:** 200 OK

### 4.5. Lưu nháp biểu quyết

**POST** `/resolutions/{resolutionId}/draft`

**Request Body:** Giống như `/resolutions/{resolutionId}/vote`

**Response:** 200 OK

### 4.6. Xem kết quả biểu quyết

**GET** `/resolutions/{resolutionId}/results`

**Response:**
```json
{
  "meetingId": "123456",
  "meetingTitle": "Đại hội cổ đông thường niên 2026",
  "resolutionId": "654321",
  "resolutionTitle": "Nghị quyết về thông qua báo cáo tài chính năm 2025",
  "results": [
    {
      "votingOptionId": "789012",
      "votingOptionName": "Đồng ý",
      "voteCount": 45,
      "totalWeight": 450000,
      "percentage": 75.5
    },
    {
      "votingOptionId": "789013",
      "votingOptionName": "Không đồng ý",
      "voteCount": 12,
      "totalWeight": 146000,
      "percentage": 24.5
    }
  ],
  "totalVoters": 57,
  "totalWeight": 596000,
  "createdAt": "2026-01-07T21:30:00"
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
  "votingOptions": []
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
      "name": "Nguyễn Văn A",
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
  ]
}
```

### 5.3. Thêm ứng viên/lựa chọn vào bầu cử

**POST** `/elections/{electionId}/options`

🔒 Yêu cầu: `ROLE_ADMIN`

**Request Body:**
```json
{
  "name": "Nguyễn Văn A",
  "position": "Ứng viên HĐQT",
  "bio": "Kinh nghiệm 15 năm trong lĩnh vực tài chính...",
  "photoUrl": "https://example.com/photos/nguyen-van-a.jpg",
  "displayOrder": 1
}
```

**Response:**
```json
{
  "id": "333444",
  "name": "Nguyễn Văn A",
  "position": "Ứng viên HĐQT",
  "bio": "...",
  "photoUrl": "...",
  "displayOrder": 1
}
```

### 5.4. Bỏ phiếu bầu cử

**POST** `/elections/{electionId}/vote`

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
- Có thể bỏ phiếu cho nhiều lựa chọn/ứng viên
- `voteWeight` là số cổ phiếu gán cho lựa chọn này
- Tổng `voteWeight` không được vượt quá quyền biểu quyết của người dùng

**Response:** 200 OK

### 5.5. Lưu nháp bỏ phiếu

**POST** `/elections/{electionId}/draft`

**Request Body:** Giống như `/elections/{electionId}/vote`

**Response:** 200 OK

### 5.6. Xem kết quả bầu cử

**GET** `/elections/{electionId}/results`

**Response:**
```json
{
  "meetingId": "123456",
  "meetingTitle": "Đại hội cổ đông thường niên 2026",
  "electionId": "111222",
  "electionTitle": "Bầu cử Hội đồng quản trị nhiệm kỳ 2026-2030",
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
      "totalWeight": 720000,
      "percentage": 29.8
    }
  ],
  "totalVoters": 65,
  "totalWeight": 2415000,
  "createdAt": "2026-01-07T21:35:00"
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
  "principalId": "user_123",
  "proxyId": "user_456",
  "sharesDelegated": 1000
}
```

**Response:**
```json
{
  "id": 1,
  "principalId": "user_123",
  "principalName": "Nguyễn Văn A",
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
    "principalId": "user_123",
    "principalName": "Nguyễn Văn A",
    "proxyId": "user_456",
    "proxyName": "Trần Văn B",
    "sharesDelegated": 1000,
    "status": "ACTIVE",
    "createdAt": "2026-01-07T10:00:00"
  }
]
```

### 6.3. Thu hồi ủy quyền

**DELETE** `/meetings/{meetingId}/proxy/{delegationId}`

**Response:** 204 No Content

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
    ROLE_USER,    // Người dùng thường (Cổ đông)
    ROLE_ADMIN    // Quản trị viên
}
```
