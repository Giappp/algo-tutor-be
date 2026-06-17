# Tài Liệu Tích Hợp Client Video Lesson

Tài liệu này mô tả contract dành cho ứng dụng người học: lấy nội dung video private, resume vị trí xem, gửi heartbeat tiến độ và nhận trạng thái hoàn thành tự động.

## 1. Thông Tin Chung

- **Base URL:** `/api/v1`
- **Xác thực:** JWT trong HttpOnly cookie `access-token`
- Người dùng phải enroll learning path chứa lesson.
- Lesson phải được unlock theo tiến độ learning path.
- Video phải ở trạng thái `READY`.
- Playback hiện dùng presigned S3 URL có thời hạn mặc định 30 phút.

```ts
const api = axios.create({
  baseURL: "/api/v1",
  withCredentials: true,
});
```

## 2. Luồng Tích Hợp Đề Xuất

1. Lấy roadmap/topic để xác định video lesson đã unlock.
2. Gọi API video content để lấy playback URL.
3. Gọi API progress để lấy vị trí resume.
4. Khi metadata player đã load, seek tới `positionSeconds`.
5. Trong lúc video thực sự đang phát, gửi heartbeat định kỳ.
6. Backend tự chuyển lesson thành `COMPLETED` khi tổng thời gian xem đạt ngưỡng.
7. Khi playback URL hết hạn hoặc player nhận `403`, gọi lại API video content.

Client không được dùng endpoint progress chung của roadmap để tự gửi trạng thái `COMPLETED` cho video.

## 3. Lấy Nội Dung Và Playback URL

```http
GET /api/v1/lessons/{slug}/video
```

Response:

```json
{
  "success": true,
  "data": {
    "id": 42,
    "slug": "gioi-thieu-binary-search",
    "title": "Giới thiệu Binary Search",
    "description": "Video giải thích trực quan thuật toán Binary Search",
    "durationSeconds": 720,
    "playbackUrl": "https://s3-presigned-url...",
    "expiresAt": "2026-06-15T15:30:00Z"
  }
}
```

Quy tắc:

- `playbackUrl` là URL tạm thời, không lưu vào database/local storage.
- Dùng trực tiếp URL này làm `src` của video player.
- Nên refresh URL trước `expiresAt` hoặc khi player nhận lỗi truy cập.
- Backend kiểm tra authentication, enrollment và lesson unlock trước khi trả URL.

## 4. Lấy Tiến Độ Để Resume

```http
GET /api/v1/lessons/{slug}/video/progress
```

Response khi chưa xem:

```json
{
  "success": true,
  "data": {
    "lessonId": 42,
    "lessonSlug": "gioi-thieu-binary-search",
    "durationSeconds": 720,
    "positionSeconds": 0,
    "watchedSeconds": 0,
    "watchedPercentage": 0.0,
    "status": "NOT_STARTED",
    "completed": false,
    "updatedAt": null
  }
}
```

Sau khi player phát event `loadedmetadata`, seek tới vị trí resume:

```ts
video.currentTime = Math.min(progress.positionSeconds, video.duration);
```

## 5. Cập Nhật Tiến Độ

```http
PATCH /api/v1/lessons/{slug}/video/progress
Content-Type: application/json
```

```json
{
  "positionSeconds": 125,
  "watchedDeltaSeconds": 10
}
```

| Trường | Kiểu | Quy tắc |
|---|---|---|
| `positionSeconds` | integer | Vị trí hiện tại của player, từ `0` đến `durationSeconds` |
| `watchedDeltaSeconds` | integer | Số giây người dùng thực sự xem kể từ heartbeat trước, từ `0` đến `30` mặc định |

Response:

```json
{
  "success": true,
  "data": {
    "lessonId": 42,
    "lessonSlug": "gioi-thieu-binary-search",
    "durationSeconds": 720,
    "positionSeconds": 125,
    "watchedSeconds": 650,
    "watchedPercentage": 90.2777777778,
    "status": "COMPLETED",
    "completed": true,
    "updatedAt": "2026-06-15T15:05:10Z"
  }
}
```

Backend:

- Cộng dồn `watchedDeltaSeconds`, tối đa bằng duration video.
- Lưu `positionSeconds` để resume.
- Tự đặt `IN_PROGRESS` khi người dùng bắt đầu xem.
- Tự đặt `COMPLETED` khi tổng thời gian xem đạt ngưỡng mặc định 90%.
- Cập nhật progress của enrollment sau mỗi heartbeat hợp lệ.
- Không giảm trạng thái sau khi lesson đã `COMPLETED`.

## 6. Heartbeat Player Đề Xuất

Chỉ cộng thời gian khi video đang thực sự phát:

- Không paused.
- Không ended.
- Tab/page đang visible.
- Playback không buffering.

Ví dụ đơn giản:

```ts
const HEARTBEAT_SECONDS = 10;
let watchedSinceLastHeartbeat = 0;
let lastTick = performance.now();

function tick(video: HTMLVideoElement) {
  const now = performance.now();
  const elapsed = Math.floor((now - lastTick) / 1000);
  lastTick = now;

  const activelyWatching =
    !video.paused &&
    !video.ended &&
    video.readyState >= HTMLMediaElement.HAVE_FUTURE_DATA &&
    document.visibilityState === "visible";

  if (activelyWatching && elapsed > 0) {
    watchedSinceLastHeartbeat += elapsed;
  }
}

async function flushProgress(slug: string, video: HTMLVideoElement) {
  const delta = Math.min(watchedSinceLastHeartbeat, 30);
  watchedSinceLastHeartbeat = 0;

  return api.patch(`/lessons/${slug}/video/progress`, {
    positionSeconds: Math.floor(video.currentTime),
    watchedDeltaSeconds: delta,
  });
}
```

Khuyến nghị:

- Gọi `tick` mỗi giây.
- Gọi `flushProgress` mỗi 10–15 giây.
- Flush khi pause, route change, tab hidden và trước khi component unmount.
- Không tăng `watchedDeltaSeconds` khi người dùng chỉ seek.
- Có thể gửi `watchedDeltaSeconds: 0` để chỉ lưu vị trí seek/resume.
- Nếu request thất bại, giữ delta chưa gửi để retry, nhưng không gửi quá giới hạn 30 giây trong một request.

## 7. Ví Dụ React Hook Rút Gọn

```ts
async function loadVideoLesson(slug: string) {
  const [contentResponse, progressResponse] = await Promise.all([
    api.get(`/lessons/${slug}/video`),
    api.get(`/lessons/${slug}/video/progress`),
  ]);

  return {
    content: contentResponse.data.data as VideoContent,
    progress: progressResponse.data.data as VideoProgress,
  };
}
```

Player nên dùng `positionSeconds` từ progress API làm nguồn resume chính, không dùng local storage làm nguồn chính xác.

## 8. TypeScript Types

```ts
type ProgressStatus = "NOT_STARTED" | "IN_PROGRESS" | "COMPLETED";

interface VideoContent {
  id: number;
  slug: string;
  title: string;
  description: string | null;
  durationSeconds: number;
  playbackUrl: string;
  expiresAt: string;
}

interface VideoProgress {
  lessonId: number;
  lessonSlug: string;
  durationSeconds: number;
  positionSeconds: number;
  watchedSeconds: number;
  watchedPercentage: number;
  status: ProgressStatus;
  completed: boolean;
  updatedAt: string | null;
}

interface VideoProgressUpdate {
  positionSeconds: number;
  watchedDeltaSeconds: number;
}
```

## 9. Các Endpoint Không Được Dùng Cho Video

Không dùng endpoint sau để đánh dấu video hoàn thành:

```http
PATCH /api/v1/roadmaps/{roadmapSlug}/lessons/{lessonSlug}/progress
```

Backend sẽ trả error code `4025` vì progress video được tự động quản lý.

Endpoint start lesson chung vẫn có thể được dùng để thể hiện người dùng mở lesson:

```http
POST /api/v1/roadmaps/{roadmapSlug}/lessons/{lessonSlug}/start
```

Tuy nhiên heartbeat video cũng sẽ tự tạo progress và chuyển sang `IN_PROGRESS`, nên gọi endpoint start không phải điều kiện bắt buộc.

## 10. Error Codes Liên Quan

| Code | HTTP | Ý nghĩa | Xử lý phía Client |
|---|---:|---|---|
| `3` | 401 | Chưa xác thực | Chuyển tới đăng nhập |
| `4005` | 404 | Không tìm thấy lesson | Hiển thị not found |
| `4017` | 403 | Lesson đang khóa | Quay lại roadmap |
| `4018` | 400 | Slug không thuộc video lesson | Không render video player |
| `4023` | 409 | Video chưa sẵn sàng | Hiển thị trạng thái đang xử lý |
| `4024` | 400 | Position hoặc watched delta không hợp lệ | Reset heartbeat state và tải lại progress |
| `4025` | 400 | Đã gọi endpoint progress chung cho video | Chuyển sang API video progress |
| `6002` | 403 | Người dùng chưa enroll | Hiển thị CTA enroll |

Error response:

```json
{
  "success": false,
  "code": 4024,
  "errors": "Video progress is invalid"
}
```

## 11. Checklist Client Front-End

1. Chỉ mở video lesson khi roadmap báo lesson đã unlock.
2. Load đồng thời video content và progress.
3. Seek sau khi player load metadata.
4. Chỉ cộng watched time khi video đang phát thật.
5. Gửi heartbeat mỗi 10–15 giây, mỗi request tối đa 30 giây.
6. Dùng response backend để cập nhật trạng thái completed.
7. Không tự gửi `COMPLETED`.
8. Refresh playback URL khi hết hạn hoặc nhận lỗi `403`.
9. Flush progress khi pause, chuyển trang hoặc đóng player.

