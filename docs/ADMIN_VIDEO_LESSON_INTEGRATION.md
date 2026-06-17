# Tài Liệu Tích Hợp Admin Video Lesson

Tài liệu này mô tả contract dành cho ứng dụng Admin quản lý video lesson, upload video trực tiếp lên AWS S3 bằng multipart upload và publish lesson.

## 1. Thông Tin Chung

- **Base URL:** `/api/v1`
- **Xác thực:** JWT trong HttpOnly cookie `access-token`
- **CRUD lesson:** chỉ vai trò `ADMIN`
- **Upload video:** vai trò `ADMIN` hoặc `EDITOR`
- **Content-Type API backend:** `application/json`
- **Upload file:** Front-End upload trực tiếp từng part lên presigned S3 URL, không gửi file qua backend

```ts
const api = axios.create({
  baseURL: "/api/v1",
  withCredentials: true,
});
```

## 2. Video Lesson State

```ts
type VideoProcessingStatus =
  | "PENDING_UPLOAD"
  | "UPLOADING"
  | "READY"
  | "FAILED";
```

| Trạng thái | Ý nghĩa | Có thể publish |
|---|---|---|
| `PENDING_UPLOAD` | Lesson chưa có video hoặc upload đã bị hủy | Không |
| `UPLOADING` | Multipart upload đã được khởi tạo | Không |
| `READY` | Upload hoàn tất và backend đã xác minh object trên S3 | Có |
| `FAILED` | Kích thước object sau upload không khớp | Không |

Backend từ chối publish video lesson chưa ở trạng thái `READY`.

## 3. CRUD Video Lesson

### 3.1. Tạo Video Lesson

```http
POST /api/v1/lessons/topics/{topicId}
Content-Type: application/json
```

```json
{
  "title": "Giới thiệu Binary Search",
  "type": "VIDEO",
  "difficulty": "EASY",
  "description": "Video giải thích trực quan thuật toán Binary Search"
}
```

`displayOrder` trong request hiện không được dùng khi tạo. Backend tự gán vị trí tiếp theo trong topic.

Response:

```json
{
  "success": true,
  "data": {
    "id": 42,
    "title": "Giới thiệu Binary Search",
    "isPublished": false,
    "type": "VIDEO",
    "displayOrder": 3,
    "difficulty": "EASY",
    "description": "Video giải thích trực quan thuật toán Binary Search",
    "sourceObjectKey": null,
    "thumbnailObjectKey": null,
    "durationSeconds": null,
    "fileSizeBytes": null,
    "mimeType": null,
    "processingStatus": "PENDING_UPLOAD"
  }
}
```

### 3.2. Lấy Chi Tiết

```http
GET /api/v1/lessons/{lessonId}
GET /api/v1/lessons/slug/{slug}
```

Hai endpoint chỉ dành cho `ADMIN` và trả `VideoLessonResponseDTO` như response tạo lesson.

### 3.3. Cập Nhật Metadata

```http
PUT /api/v1/lessons/{lessonId}
Content-Type: application/json
```

```json
{
  "title": "Binary Search Trực Quan",
  "type": "VIDEO",
  "difficulty": "EASY",
  "description": "Mô tả mới"
}
```

Lưu ý:

- Không thể đổi type của lesson hiện có.
- Update metadata không thay đổi video đã upload.
- Backend tạo lại `slug` từ title mới.

### 3.4. Publish hoặc Unpublish

```http
PATCH /api/v1/lessons/{lessonId}/publish
```

Endpoint toggle trạng thái publish:

- `false → true`: chỉ thành công khi video `READY`.
- `true → false`: luôn được phép.

### 3.5. Xóa Lesson

```http
DELETE /api/v1/lessons/{lessonId}
```

Hiện endpoint xóa database lesson. Front-End không nên giả định object video trên S3 đã được xóa cùng lúc.

## 4. Multipart Upload Flow

Giới hạn mặc định:

- MIME hỗ trợ: `video/mp4`, `video/quicktime`
- Dung lượng tối đa: `2 GiB`
- Part size: `10 MiB`
- Presigned URL của từng part có hiệu lực khoảng 15 phút

### Bước 1: Khởi Tạo Upload

```http
POST /api/v1/lessons/{lessonId}/video/uploads
Content-Type: application/json
```

```json
{
  "fileName": "binary-search.mp4",
  "contentType": "video/mp4",
  "fileSize": 52428800
}
```

Response:

```json
{
  "success": true,
  "data": {
    "uploadId": "multipart-upload-id",
    "objectKey": "lessons/42/video/source/uuid.mp4",
    "partSize": 10485760,
    "totalParts": 5
  }
}
```

Front-End phải giữ `uploadId` và `objectKey` đến khi complete hoặc abort.

### Bước 2: Lấy Presigned URL Cho Các Part

```http
POST /api/v1/lessons/{lessonId}/video/uploads/parts
Content-Type: application/json
```

```json
{
  "uploadId": "multipart-upload-id",
  "objectKey": "lessons/42/video/source/uuid.mp4",
  "partNumbers": [1, 2, 3, 4, 5]
}
```

Response:

```json
{
  "success": true,
  "data": [
    {
      "partNumber": 1,
      "uploadUrl": "https://s3-presigned-url..."
    }
  ]
}
```

Có thể yêu cầu URL theo từng batch nhỏ để dễ refresh URL hết hạn.

### Bước 3: Upload Từng Part Trực Tiếp Lên S3

```ts
interface UploadedPart {
  partNumber: number;
  eTag: string;
}

async function uploadPart(
  file: File,
  partNumber: number,
  partSize: number,
  uploadUrl: string,
): Promise<UploadedPart> {
  const start = (partNumber - 1) * partSize;
  const end = Math.min(start + partSize, file.size);
  const blob = file.slice(start, end);

  const response = await fetch(uploadUrl, {
    method: "PUT",
    body: blob,
  });

  if (!response.ok) throw new Error(`Upload part ${partNumber} failed`);

  const eTag = response.headers.get("ETag");
  if (!eTag) throw new Error("S3 response does not expose ETag");

  return { partNumber, eTag };
}
```

Quan trọng:

- Lưu chính xác `ETag` S3 trả về cho từng part.
- S3 bucket CORS phải expose header `ETag`.
- Nên giới hạn upload song song khoảng 3–5 part.
- Retry chỉ part thất bại; không cần upload lại toàn bộ file.

### Bước 4: Lấy Duration Video

Frontend có thể đọc duration trước khi complete:

```ts
function readVideoDuration(file: File): Promise<number> {
  return new Promise((resolve, reject) => {
    const video = document.createElement("video");
    video.preload = "metadata";
    video.onloadedmetadata = () => {
      URL.revokeObjectURL(video.src);
      resolve(Math.ceil(video.duration));
    };
    video.onerror = reject;
    video.src = URL.createObjectURL(file);
  });
}
```

`durationSeconds` hiện do Admin Front-End cung cấp. Backend chưa trích xuất hoặc xác minh duration từ media file.

### Bước 5: Complete Upload

```http
POST /api/v1/lessons/{lessonId}/video/uploads/complete
Content-Type: application/json
```

```json
{
  "uploadId": "multipart-upload-id",
  "objectKey": "lessons/42/video/source/uuid.mp4",
  "durationSeconds": 720,
  "parts": [
    { "partNumber": 1, "eTag": "\"etag-1\"" },
    { "partNumber": 2, "eTag": "\"etag-2\"" }
  ]
}
```

Phải gửi đủ tất cả part, không trùng `partNumber`.

Response thành công:

```json
{
  "success": true,
  "data": {
    "lessonId": 42,
    "objectKey": "lessons/42/video/source/uuid.mp4",
    "fileSize": 52428800,
    "contentType": "video/mp4",
    "durationSeconds": 720,
    "processingStatus": "READY"
  }
}
```

Backend complete multipart upload, gọi S3 `HeadObject`, so sánh kích thước thực tế và chỉ chuyển sang `READY` khi hợp lệ.

### Bước 6: Abort Upload

```http
DELETE /api/v1/lessons/{lessonId}/video/uploads
Content-Type: application/json
```

```json
{
  "uploadId": "multipart-upload-id",
  "objectKey": "lessons/42/video/source/uuid.mp4"
}
```

Với Axios, body của request `DELETE` phải đặt trong `data`:

```ts
await api.delete(`/lessons/${lessonId}/video/uploads`, {
  data: { uploadId, objectKey },
});
```

Sau abort, trạng thái lesson trở về `PENDING_UPLOAD`.

## 5. TypeScript Types

```ts
type LessonType = "THEORY" | "QUIZ" | "CODING" | "VIDEO";
type Difficulty = "EASY" | "MEDIUM" | "HARD";

interface VideoLessonResponse {
  id: number;
  title: string;
  isPublished: boolean;
  type: "VIDEO";
  displayOrder: number;
  difficulty: Difficulty;
  description: string | null;
  sourceObjectKey: string | null;
  thumbnailObjectKey: string | null;
  durationSeconds: number | null;
  fileSizeBytes: number | null;
  mimeType: string | null;
  processingStatus: VideoProcessingStatus;
}
```

## 6. Error Codes Liên Quan

| Code | HTTP | Ý nghĩa |
|---|---:|---|
| `4004` | 400 | Lesson type không hợp lệ |
| `4005` | 404 | Không tìm thấy lesson |
| `4018` | 400 | Endpoint upload được gọi cho lesson không phải video |
| `4019` | 400 | MIME video không được hỗ trợ |
| `4020` | 400 | Dung lượng video không hợp lệ |
| `4021` | 404 | `objectKey` không thuộc phiên upload của lesson |
| `4022` | 400 | Thiếu part, trùng part hoặc object S3 không hợp lệ |
| `4023` | 409 | Video chưa `READY`, không thể publish |

Error response:

```json
{
  "success": false,
  "code": 4023,
  "errors": "Video is not ready"
}
```

## 7. Checklist Admin Front-End

1. Tạo lesson với `type: "VIDEO"`.
2. Validate MIME và dung lượng trước khi initiate.
3. Hiển thị progress upload dựa trên tổng bytes các part đã hoàn tất.
4. Giữ `uploadId`, `objectKey`, `partSize` và danh sách ETag trong state.
5. Cho phép retry part lỗi và abort upload.
6. Chỉ bật nút publish khi `processingStatus === "READY"`.
7. Không lưu presigned upload URL lâu dài vì URL sẽ hết hạn.

