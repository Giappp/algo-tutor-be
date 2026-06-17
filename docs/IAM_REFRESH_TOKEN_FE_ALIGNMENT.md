# FE Alignment - Refresh Token Cookie

Tài liệu này mô tả contract refresh token hiện tại của BE và các thay đổi FE cần thực hiện để tránh gọi `/iam/refresh` mà không có cookie.

## 1. Thay đổi phía BE

Khi `refresh-token` bị thiếu, rỗng hoặc sai định dạng, BE không còn phát sinh `NullPointerException` và trả HTTP 500.

BE hiện trả lỗi chuẩn:

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json
```

```json
{
  "success": false,
  "errors": "Invalid token or already logged out",
  "code": 1005
}
```

Các mã lỗi FE cần xử lý:

| HTTP | Code | Ý nghĩa | Hành động FE |
|---|---:|---|---|
| `401` | `1004` | Refresh token đã hết hạn | Xóa auth state và chuyển về màn hình đăng nhập |
| `401` | `1005` | Refresh token thiếu, sai hoặc đã bị thu hồi | Xóa auth state và chuyển về màn hình đăng nhập |
| `401` | Khác/rỗng | Access token thiếu hoặc hết hạn | Thử refresh đúng một lần |

## 2. Contract API

Base URL mặc định:

```text
/api/v1
```

### Sign in

```http
POST /api/v1/iam/signin
Content-Type: application/json
```

```json
{
  "username": "username",
  "password": "password"
}
```

BE trả hai header `Set-Cookie`:

- `access-token`: JWT dùng cho request authenticated.
- `refresh-token`: UUID opaque token dùng để rotate session. Đây không phải JWT.

FE không đọc hoặc lưu hai token này trong `localStorage`, `sessionStorage`, Zustand persisted state hoặc Redux persisted state. Trình duyệt tự quản lý cookie vì chúng có `HttpOnly=true`.

### Refresh

```http
POST /api/v1/iam/refresh
Cookie: refresh-token=<uuid>
```

- Không có request body.
- FE không tự thêm giá trị refresh token vào header hoặc body.
- Khi thành công, BE rotate refresh token cũ và trả lại hai cookie mới.

### Logout

```http
POST /api/v1/iam/logout
Cookie: refresh-token=<uuid>
```

Khi thành công, BE thu hồi refresh token và trả cookie hết hạn để trình duyệt xóa session.

## 3. Thay đổi bắt buộc phía FE

### Axios instance

`withCredentials: true` phải được cấu hình trên instance dùng cho cả sign in, refresh và các API authenticated:

```ts
import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? "/api/v1",
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});
```

Không tạo một Axios instance riêng cho `/iam/refresh` mà thiếu `withCredentials`.

### Fetch

Nếu dùng Fetch API:

```ts
await fetch("/api/v1/iam/refresh", {
  method: "POST",
  credentials: "include",
});
```

`credentials: "include"` cũng phải có trên request sign in để trình duyệt nhận cookie khi FE gọi BE cross-origin.

## 4. Axios interceptor khuyến nghị

Interceptor phải đáp ứng các điều kiện:

- Không gọi refresh khi chính request `/iam/refresh` trả `401`.
- Mỗi request chỉ được retry một lần.
- Nhiều request cùng trả `401` chỉ tạo một request refresh.
- Refresh thất bại thì clear auth state và chuyển về sign in.

```ts
import type { AxiosError, InternalAxiosRequestConfig } from "axios";
import { api } from "./api";

type RetryableRequest = InternalAxiosRequestConfig & {
  _retry?: boolean;
};

type ApiError = {
  code?: number;
};

let refreshPromise: Promise<void> | null = null;

async function refreshSession(): Promise<void> {
  await api.post("/iam/refresh");
}

function clearSessionAndRedirect(): void {
  // Clear user/profile state only. HttpOnly cookies cannot be removed by JS.
  window.location.assign("/signin");
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const request = error.config as RetryableRequest | undefined;
    const status = error.response?.status;
    const errorCode = (error.response?.data as ApiError | undefined)?.code;
    const isRefreshRequest = request?.url?.includes("/iam/refresh") ?? false;
    const isTerminalTokenError = errorCode === 1004 || errorCode === 1005;

    if (status === 401 && (isRefreshRequest || isTerminalTokenError)) {
      clearSessionAndRedirect();
      return Promise.reject(error);
    }

    if (!request || status !== 401 || request._retry) {
      return Promise.reject(error);
    }

    request._retry = true;

    try {
      refreshPromise ??= refreshSession().finally(() => {
        refreshPromise = null;
      });
      await refreshPromise;
      return api(request);
    } catch (refreshError) {
      clearSessionAndRedirect();
      return Promise.reject(refreshError);
    }
  },
);
```

FE không nên chủ động gọi refresh ngay sau khi sign in thành công. Sau sign in, có thể gọi `/iam/me` để lấy thông tin user; chỉ gọi refresh khi một request authenticated trả `401`.

## 5. Cookie và môi trường chạy

Cookie hiện có các thuộc tính:

```text
HttpOnly=true
SameSite=Strict
Path=/
Secure=<theo cấu hình BE>
```

`withCredentials: true` chỉ cho phép browser gửi cookie; nó không vượt qua chính sách `SameSite`.

Với `SameSite=Strict`, FE và BE cần chạy cùng site. Cách ổn định nhất:

- Development: FE proxy `/api/v1` tới BE và FE gọi bằng relative URL `/api/v1/...`.
- Production: reverse proxy FE và BE dưới cùng domain/site, ví dụ `https://algotutor.vn` và `https://algotutor.vn/api/v1`.

Nếu FE và BE bắt buộc chạy cross-site, hai team cần thống nhất thay đổi cookie sang `SameSite=None; Secure=true` trước khi deploy.

## 6. Checklist nghiệm thu FE

- [ ] Request sign in có `withCredentials: true` hoặc `credentials: "include"`.
- [ ] Browser lưu cả `access-token` và `refresh-token` sau sign in.
- [ ] Request `/iam/refresh` gửi kèm cookie `refresh-token`.
- [ ] Refresh thành công cập nhật cookie và retry request ban đầu đúng một lần.
- [ ] Nhiều request cùng `401` chỉ tạo một request refresh.
- [ ] Refresh trả `401` không tạo vòng lặp refresh.
- [ ] Lỗi `1004` hoặc `1005` đưa user về trạng thái logged-out.
- [ ] FE không đọc hoặc lưu token vào browser storage.

## 7. Kiểm tra nhanh trên DevTools

1. Mở tab `Network`, gọi `POST /api/v1/iam/signin`.
2. Kiểm tra response có hai header `Set-Cookie`.
3. Mở `Application > Cookies`, xác nhận có `access-token` và `refresh-token`.
4. Gọi `POST /api/v1/iam/refresh`, kiểm tra request header có cookie `refresh-token`.
5. Xóa `refresh-token` rồi gọi refresh; kết quả mong đợi là HTTP `401`, body có `code: 1005`, không phải HTTP `500`.
