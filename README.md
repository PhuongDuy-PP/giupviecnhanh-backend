# GVN Backend - Authentication API

API xác thực cho ứng dụng Giúp Việc Nhanh, xây dựng bằng Spring Boot với các tính năng đăng ký và đăng nhập bằng số điện thoại.

## Yêu cầu hệ thống

- Java 21
- Maven 3.6 trở lên
- PostgreSQL (khuyến nghị chạy bằng Docker)

## Cài đặt và chạy

### 1. Chuẩn bị database

Chạy PostgreSQL container:

```bash
docker run -d \
  --name postgres-gvn \
  -p 5434:5432 \
  -e POSTGRES_DB=giupviecnhanh_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=giupviecnhanh123 \
  postgres
```

### 2. Cài đặt dependencies

```bash
mvn clean install
```

### 3. Chạy ứng dụng

```bash
mvn spring-boot:run
```

Ứng dụng sẽ chạy tại: http://localhost:8080

## Cấu trúc thư mục

```
src/main/java/com/gvn/
├── config/          # Cấu hình Security, JWT
├── controller/      # REST Controllers
├── dto/            
│   ├── request/    # Request DTOs
│   └── response/   # Response DTOs
├── entity/         # JPA Entities
├── exception/      # Exception handlers
├── repository/     # JPA Repositories
└── service/        # Business Logic
```

## API Documentation

### 1. Đăng ký tài khoản

**Endpoint:** POST /api/v1/auth/signup

**Request body:**
```json
{
  "phone_number": "0901234567",
  "password": "password123",
  "device_info": "android",
  "user_type": "customer"
}
```

**Response thành công (200):**
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "expires_in": 3600,
    "expires_at": 1234567890,
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_expires_in": 7200,
    "refresh_expires_at": 1234567890,
    "session_id": "550e8400-e29b-41d4-a716-446655440000",
    "user": {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "phone": "0901234567",
      "full_name": null,
      "has_partner_profile": false
    }
  },
  "message": "Registration successful"
}
```

**Lỗi số điện thoại đã tồn tại (409):**
```json
{
  "success": false,
  "message": "This phone number is already registered. Please use a different number or try logging in.",
  "error": {
    "message": "This phone number is already registered...",
    "return_code": 409
  }
}
```

**Lỗi validation (400):**
```json
{
  "success": false,
  "message": "Validation error",
  "error": {
    "message": "Phone number format is invalid",
    "return_code": 400
  }
}
```

### 2. Đăng nhập

**Endpoint:** POST /api/v1/auth/login

**Request body:**
```json
{
  "phone_number": "0901234567",
  "password": "password123",
  "device_info": "android",
  "user_type": "customer"
}
```

**Response thành công (200):**
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "expires_in": 3600,
    "expires_at": 1234567890,
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_expires_in": 7200,
    "refresh_expires_at": 1234567890,
    "session_id": "550e8400-e29b-41d4-a716-446655440000",
    "user": {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "phone": "0901234567",
      "full_name": "Nguyen Van A"
    }
  },
  "message": "Login successful"
}
```

**Lỗi thông tin đăng nhập sai (401):**
```json
{
  "success": false,
  "message": "Phone number or password is incorrect",
  "error": {
    "message": "Phone number or password is incorrect",
    "return_code": 401
  }
}
```

### 3. Làm mới token

**Endpoint:** POST /api/v1/auth/refresh-token

**Request body:**
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response thành công (200):**
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "expires_in": 3600,
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "session_id": "550e8400-e29b-41d4-a716-446655440000",
    "user": {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "phone": "0901234567"
    }
  },
  "message": "Token refreshed successfully"
}
```

## Hướng dẫn test API

### Sử dụng cURL

**Đăng ký:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -H "X-Platform: android" \
  -d '{
    "phone_number": "0901234567",
    "password": "password123",
    "device_info": "android",
    "user_type": "customer"
  }'
```

**Đăng nhập:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Platform: android" \
  -d '{
    "phone_number": "0901234567",
    "password": "password123",
    "device_info": "android",
    "user_type": "customer"
  }'
```

**Làm mới token:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{
    "refresh_token": "YOUR_REFRESH_TOKEN_HERE"
  }'
```

### Sử dụng Postman

**Đăng ký:**
- Method: POST
- URL: http://localhost:8080/api/v1/auth/signup
- Headers:
  - Content-Type: application/json
  - X-Platform: android
- Body (raw JSON): như ví dụ cURL ở trên

**Đăng nhập:** Tương tự như đăng ký, chỉ thay endpoint thành /login

**Làm mới token:** Tương tự, endpoint /refresh-token

## Quy tắc validation

**Số điện thoại:**
- Bắt buộc nhập
- Định dạng: 10-11 chữ số
- Ví dụ hợp lệ: 0901234567, 0123456789
- Phải là duy nhất trong hệ thống

**Mật khẩu:**
- Bắt buộc nhập
- Tối thiểu 6 ký tự
- Chưa có yêu cầu về độ phức tạp

## Mã lỗi

- 400: Bad Request - Lỗi validation hoặc request không hợp lệ
- 401: Unauthorized - Sai thông tin đăng nhập hoặc token không hợp lệ
- 409: Conflict - Số điện thoại đã tồn tại (chỉ khi đăng ký)
- 500: Internal Server Error - Lỗi server

## Cấu hình JWT

Cấu hình trong file application.yml:
- Secret key: Có thể thay đổi trong application.yml
- Access token: hết hạn sau 3600 giây (1 giờ)
- Refresh token: hết hạn sau 7200 giây (2 giờ)

## Bảo mật

- Mật khẩu được mã hóa bằng BCrypt trước khi lưu database
- Sử dụng JWT token cho xác thực
- Access token có thời hạn 1 giờ
- Refresh token có thời hạn 2 giờ
- Session được lưu trong database để quản lý phiên đăng nhập
- CSRF protection đã được tắt cho API endpoints
- CORS có thể cấu hình thêm nếu cần

## Luồng hoạt động

**Đăng ký:**
1. User gửi thông tin đăng ký (số điện thoại, mật khẩu)
2. Hệ thống kiểm tra số điện thoại đã tồn tại chưa
3. Nếu chưa tồn tại, tạo user mới với mật khẩu đã mã hóa
4. Tự động đăng nhập: tạo access token và refresh token
5. Trả về tokens và thông tin user
6. User có thể sử dụng ứng dụng ngay không cần đăng nhập lại

**Đăng nhập:**
1. User gửi số điện thoại và mật khẩu
2. Hệ thống xác thực thông tin đăng nhập
3. Nếu hợp lệ, tạo access token và refresh token mới
4. Lưu session vào database
5. Trả về tokens và thông tin user

**Làm mới token:**
1. Khi access token hết hạn, client gửi refresh token
2. Hệ thống validate refresh token
3. Nếu hợp lệ, tạo cặp tokens mới
4. Cập nhật session trong database
5. Trả về tokens mới