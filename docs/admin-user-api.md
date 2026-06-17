# Tài Liệu Tích Hợp API Quản Trị Người Dùng

Tài liệu này mô tả contract tích hợp Front-End cho các API trong `AdminUserController`.

## 1. Thông Tin Chung

- **Base URL:** `/api/v1`
- **Base path:** `/admin/users`
- **Xác thực:** JWT trong HttpOnly cookie `access-token`
- **Phân quyền:** Chỉ tài khoản có vai trò `ADMIN`
- **Content-Type:** `application/json`

Front-End phải gửi cookie trong mọi request:

```ts
const api = axios.create({
  baseURL: "/api/v1",
  withCredentials: true,
});
```

Nếu chưa đăng nhập hoặc cookie không hợp lệ, API trả về lỗi xác thực. Nếu tài khoản không có quyền `ADMIN`, API từ chối truy cập.

## 2. Kiểu Dữ Liệu Chung

### 2.1. UserResponse

| Trường | Kiểu | Ý nghĩa |
|---|---|---|
| `id` | UUID string | ID người dùng |
| `username` | string | Tên đăng nhập |
| `email` | string | Email |
| `role` | string | Tên vai trò của người dùng |
| `avatar` | string hoặc `null` | URL ảnh đại diện |
| `enabled` | boolean | `true` nếu tài khoản đang hoạt động |
| `blockReason` | string hoặc `null` | Lý do khóa tài khoản |

Ví dụ:

```json
{
  "id": "d72d242c-a2b1-4f81-80a1-7c9b83b38151",
  "username": "coder_pro",
  "email": "coder_pro@example.com",
  "role": "USER",
  "avatar": null,
  "enabled": true,
  "blockReason": null
}
```

### 2.2. Vai Trò Hợp Lệ

Các request tạo người dùng và đổi vai trò chỉ chấp nhận:

- `USER`
- `ADMIN`
- `EDITOR`

Giá trị enum phân biệt chữ hoa/chữ thường.

### 2.3. ApiResponse

Các API tạo mới, khóa, mở khóa và đổi vai trò trả về:

```json
{
  "data": {},
  "success": true
}
```

Trường `message` không xuất hiện nếu không có nội dung.

### 2.4. ErrorResponse

Lỗi nghiệp vụ có dạng:

```json
{
  "errors": "User not found",
  "success": false,
  "code": 1006
}
```

Lỗi validation có `errors` là object, mỗi field chứa danh sách thông báo:

```json
{
  "errors": {
    "email": ["must be a well-formed email address"],
    "password": ["Password is invalid"]
  },
  "success": false,
  "code": 1
}
```

Thông báo lỗi có thể là tiếng Anh hoặc tiếng Việt tùy locale của request.

## 3. Danh Sách API

### 3.1. Lấy Danh Sách Người Dùng

```http
GET /api/v1/admin/users
```

**Query parameters:**

| Tên | Kiểu | Bắt buộc | Mặc định | Mô tả |
|---|---|---|---|---|
| `search` | string | Không | Không có | Tìm gần đúng, không phân biệt hoa thường theo `username` hoặc `email` |
| `page` | integer | Không | `0` | Trang hiện tại, bắt đầu từ `0` |
| `size` | integer | Không | `10` | Số bản ghi mỗi trang |
| `sort` | string | Không | Không có | Sắp xếp theo format `field,direction`, ví dụ `username,asc` |

Có thể gửi nhiều tham số `sort`:

```http
GET /api/v1/admin/users?search=coder&page=0&size=10&sort=enabled,desc&sort=username,asc
```

**Response: `200 OK`**

> Endpoint danh sách trả trực tiếp `PageResponse`, không bọc trong `ApiResponse`.

```json
{
  "data": [
    {
      "id": "d72d242c-a2b1-4f81-80a1-7c9b83b38151",
      "username": "coder_pro",
      "email": "coder_pro@example.com",
      "role": "USER",
      "avatar": null,
      "enabled": true,
      "blockReason": null
    }
  ],
  "pageSize": 10,
  "totalPages": 1,
  "totalElements": 1,
  "currentPage": 0
}
```

### 3.2. Tạo Người Dùng

```http
POST /api/v1/admin/users
```

**Request body:**

```json
{
  "username": "new_editor",
  "email": "new_editor@example.com",
  "password": "Editor@123",
  "confirmPassword": "Editor@123",
  "role": "EDITOR",
  "enabled": true
}
```

| Trường | Kiểu | Bắt buộc | Quy tắc |
|---|---|---|---|
| `username` | string | Có | Không được rỗng và chưa tồn tại |
| `email` | string | Có | Email hợp lệ và chưa tồn tại |
| `password` | string | Có | Từ 8 đến 16 ký tự, có chữ hoa, chữ thường, số, ký tự đặc biệt và không chứa khoảng trắng |
| `confirmPassword` | string | Có | Cùng quy tắc với `password` và phải khớp `password` |
| `role` | enum | Có | `USER`, `ADMIN` hoặc `EDITOR` |
| `enabled` | boolean | Không | Mặc định `true` nếu không gửi hoặc gửi `null` |

**Response: `201 Created`**

```json
{
  "data": {
    "id": "34ef7804-c019-4ce8-8548-85ea6f748d40",
    "username": "new_editor",
    "email": "new_editor@example.com",
    "role": "EDITOR",
    "avatar": null,
    "enabled": true,
    "blockReason": null
  },
  "success": true
}
```

**Lỗi nghiệp vụ:**

| HTTP | Code | Trường hợp |
|---|---:|---|
| `400 Bad Request` | `1` | Body hoặc field không hợp lệ |
| `400 Bad Request` | `1001` | Email đã được sử dụng |
| `400 Bad Request` | `1002` | Username đã tồn tại |
| `400 Bad Request` | `1003` | Password và confirmPassword không khớp |

### 3.3. Khóa Người Dùng

```http
POST /api/v1/admin/users/{id}/block
```

**Path parameter:** `id` là UUID của người dùng cần khóa.

**Request body:**

```json
{
  "reason": "Vi phạm điều khoản sử dụng"
}
```

`reason` là bắt buộc và không được rỗng. Khi thành công, `enabled` được đặt thành `false` và `blockReason` chứa lý do khóa.

**Response: `200 OK`**

```json
{
  "data": {
    "id": "d72d242c-a2b1-4f81-80a1-7c9b83b38151",
    "username": "coder_pro",
    "email": "coder_pro@example.com",
    "role": "USER",
    "avatar": null,
    "enabled": false,
    "blockReason": "Vi phạm điều khoản sử dụng"
  },
  "success": true
}
```

Admin không thể tự khóa chính mình. Trường hợp này trả về `409 Conflict`, code `4`.

### 3.4. Mở Khóa Người Dùng

```http
POST /api/v1/admin/users/{id}/unblock
```

Request không có body. Khi thành công, `enabled` được đặt thành `true` và `blockReason` được xóa.

**Response: `200 OK`**

```json
{
  "data": {
    "id": "d72d242c-a2b1-4f81-80a1-7c9b83b38151",
    "username": "coder_pro",
    "email": "coder_pro@example.com",
    "role": "USER",
    "avatar": null,
    "enabled": true,
    "blockReason": null
  },
  "success": true
}
```

### 3.5. Đổi Vai Trò Người Dùng

```http
PUT /api/v1/admin/users/{id}/role
```

**Request body:**

```json
{
  "role": "EDITOR"
}
```

`role` là bắt buộc và phải là `USER`, `ADMIN` hoặc `EDITOR`.

**Response: `200 OK`**

```json
{
  "data": {
    "id": "d72d242c-a2b1-4f81-80a1-7c9b83b38151",
    "username": "coder_pro",
    "email": "coder_pro@example.com",
    "role": "EDITOR",
    "avatar": null,
    "enabled": true,
    "blockReason": null
  },
  "success": true
}
```

Admin không thể đổi vai trò của chính mình. Trường hợp này trả về `409 Conflict`, code `4`.

## 4. Lỗi Chung

| HTTP | Code | Trường hợp |
|---|---:|---|
| `400 Bad Request` | `1` | Validation field, JSON hoặc enum không hợp lệ |
| `401 Unauthorized` | `2` hoặc `3` | Thiếu xác thực hoặc truy cập bị từ chối ở lớp method security |
| `404 Not Found` | `1006` | Không tìm thấy người dùng |
| `409 Conflict` | `4` | Admin tự khóa hoặc tự đổi vai trò |
| `500 Internal Server Error` | `9999` | Lỗi hệ thống hoặc không tìm thấy role tương ứng trong dữ liệu hệ thống |

Front-End cần đảm bảo `id` đúng định dạng UUID trước khi gọi API. Hiện tại path UUID sai định dạng không có error contract riêng.

## 5. Gợi Ý Tích Hợp Front-End

```ts
export const adminUserApi = {
  list: (params?: {
    search?: string;
    page?: number;
    size?: number;
    sort?: string | string[];
  }) => api.get("/admin/users", { params }),

  create: (payload: {
    username: string;
    email: string;
    password: string;
    confirmPassword: string;
    role: "USER" | "ADMIN" | "EDITOR";
    enabled?: boolean;
  }) => api.post("/admin/users", payload),

  block: (id: string, reason: string) =>
    api.post(`/admin/users/${id}/block`, { reason }),

  unblock: (id: string) =>
    api.post(`/admin/users/${id}/unblock`),

  changeRole: (id: string, role: "USER" | "ADMIN" | "EDITOR") =>
    api.put(`/admin/users/${id}/role`, { role }),
};
```

Checklist phía Front-End:

- Luôn bật `withCredentials: true`.
- Dùng `currentPage` theo chỉ số bắt đầu từ `0`.
- Sau thao tác khóa, mở khóa hoặc đổi role, cập nhật row bằng `data` trả về thay vì tự suy đoán state.
- Ẩn hoặc vô hiệu hóa nút khóa và đổi role trên tài khoản admin đang đăng nhập.
- Hiển thị lỗi validation theo từng field khi `errors` là object.
- Khi nhận `401`, thực hiện refresh session theo contract IAM rồi thử lại request phù hợp.
