# Judge & Coding Completion API - FE Alignment

Tài liệu này là contract cho luồng chạy thử, nộp bài coding, và tự động đánh dấu hoàn thành bài học coding.

## Quy ước chung

- Ngôn ngữ request: `java`, `python`, `cpp`.
- Verdict luôn là uppercase:
  - `PENDING`
  - `PROCESSING`
  - `ACCEPTED`
  - `WRONG_ANSWER`
  - `TIME_LIMIT_EXCEEDED`
  - `MEMORY_LIMIT_EXCEEDED`
  - `RUNTIME_ERROR`
  - `COMPILATION_ERROR`
  - `SYSTEM_ERROR`
- Thời gian luôn dùng milliseconds (`timeMs`, `maxTimeMs`).
- Bộ nhớ luôn dùng kilobytes (`memoryKb`, `maxMemoryKb`).
- `stdout` là output do code người dùng in ra.
- `stderr` là compile/runtime error hoặc warning từ code người dùng.
- `stdout`/`stderr` giữ nguyên nội dung và xuống dòng; FE nên render bằng `white-space: pre-wrap`.
- BE gửi memory limit tới Piston bằng `run_memory_limit` theo đơn vị bytes.
- Các endpoint bên dưới yêu cầu user đã đăng nhập.

Piston contract tham chiếu: <https://github.com/engineer-man/piston/blob/master/docs/api-v2.md#execute>

## Luồng FE chuẩn

1. FE mở bài coding bằng `GET /lessons/{slug}/coding`.
2. User bấm Run: gọi `POST /judge/run`. Kết quả trả ngay, chỉ chạy sample testcases, không lưu submission, không update progress.
3. User bấm Submit: gọi `POST /judge/submit`. Response trả ngay với `verdict=PENDING` và `submissionId`.
4. FE poll `GET /judge/submissions/{submissionId}` cho tới khi `status` là terminal.
5. Khi poll thấy `status=ACCEPTED`, hiển thị accepted. Nếu `progressUpdated=true`, refresh dữ liệu progress/lesson list/current lesson/enrollment.
6. Nếu reload trang, FE tiếp tục poll cùng endpoint hoặc dùng `GET /submissions/{submissionId}` để lấy trạng thái persisted mới nhất.

Polling gợi ý:

- Poll mỗi 800-1500ms khi status là `PENDING` hoặc `PROCESSING`.
- Dừng poll khi status là một terminal verdict: `ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `MEMORY_LIMIT_EXCEEDED`, `RUNTIME_ERROR`, `COMPILATION_ERROR`, `SYSTEM_ERROR`.
- Có thể timeout client sau 60-120s và hiển thị trạng thái đang xử lý/lỗi mạng; không tự đổi verdict.

## Run sample testcases

`POST /judge/run`

Run chạy toàn bộ sample testcases và không tạo submission.

Request:

```json
{
  "lessonSlug": "two-sum",
  "language": "python",
  "code": "print('hello')"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "submissionId": null,
    "verdict": "WRONG_ANSWER",
    "summary": {
      "passed": 1,
      "total": 2,
      "failed": 1,
      "executed": 2
    },
    "performance": {
      "maxTimeMs": 18,
      "maxMemoryKb": 4096
    },
    "testCases": [
      {
        "index": 0,
        "status": "WRONG_ANSWER",
        "timeMs": 18,
        "memoryKb": 4096,
        "stdout": "hello",
        "stderr": ""
      }
    ],
    "compilationError": null,
    "progressUpdated": false
  }
}
```

Lưu ý:

- `total` là tổng sample testcases.
- `executed` là số testcase thực sự đã chạy.
- `failed` chỉ đếm testcase đã chạy và không accepted.
- Khi compile/system error, judge dừng sớm nên `executed` có thể nhỏ hơn `total`.
- Khi `COMPILATION_ERROR`, lỗi có ở cả `compilationError` và `testCases[0].stderr`.

## Submit

`POST /judge/submit`

Submit tạo submission và chấm async trên toàn bộ testcase. HTTP response chỉ xác nhận submission đã được tạo, chưa phải kết quả cuối:

```json
{
  "success": true,
  "data": {
    "submissionId": "c973b055-f6ec-47bc-bac8-388838248ada",
    "verdict": "PENDING",
    "summary": null,
    "performance": null,
    "testCases": null,
    "compilationError": null,
    "progressUpdated": false
  }
}
```

Sau đó FE poll detail:

`GET /judge/submissions/c973b055-f6ec-47bc-bac8-388838248ada`

Response khi còn đang chấm:

```json
{
  "success": true,
  "data": {
    "id": "c973b055-f6ec-47bc-bac8-388838248ada",
    "language": "python",
    "status": "PENDING",
    "sourceCode": "print('hello')",
    "passedTestCases": 0,
    "totalTestCases": null,
    "executionTime": null,
    "memoryUsed": null,
    "compileOutput": null,
    "progressUpdated": false,
    "submittedAt": "2026-06-13T08:00:00Z",
    "testCases": []
  }
}
```

Submit dừng ở testcase lỗi đầu tiên. Vì vậy `passed` có thể nhỏ hơn `total` và FE không nên tự suy ra các testcase còn lại là `WRONG_ANSWER`.

## Auto-complete coding lesson

BE tự đánh dấu bài coding là hoàn thành trong lúc finalize submission.

Điều kiện:

- Submission final verdict phải là `ACCEPTED`.
- User phải có enrollment `IN_PROGRESS` của learning path chứa lesson.
- Lesson phải đang unlock với user hiện tại.
- Lesson chưa ở trạng thái `COMPLETED` trước đó.

Khi đủ điều kiện, BE:

- Tạo hoặc cập nhật `lesson_progresses` của lesson sang `status=COMPLETED`.
- Set `isCompleted=true` và `completedAt`.
- Tính lại `enrollments.progressPercentage`.
- Nếu toàn bộ published lessons trong learning path đã hoàn thành, set enrollment `status=COMPLETED`.

Ý nghĩa `progressUpdated` trong REST submission detail/history:

- `true`: submit này vừa làm thay đổi progress từ chưa hoàn thành sang hoàn thành. FE nên refresh sidebar/topic/current lesson/enrollment progress.
- `false`: không có thay đổi progress trong lần submit này. Ví dụ submission không accepted, lesson đã completed từ trước, user không có enrollment hợp lệ, hoặc lesson đang locked.

FE vẫn có thể coi `status=ACCEPTED` là trạng thái bài làm thành công để hiển thị accepted. `progressUpdated` chỉ nói lần submit này có cập nhật lesson progress hay không.

## Submission detail

Ưu tiên dùng endpoint dưới đây khi poll ngay sau submit:

`GET /judge/submissions/{submissionId}`

Endpoint lịch sử submission cũng hỗ trợ lấy detail theo UUID:

`GET /submissions/{submissionId}`

Dùng endpoint này để recover state khi FE reload hoặc cần tiếp tục polling sau khi submit.

```json
{
  "success": true,
  "data": {
    "id": "c973b055-f6ec-47bc-bac8-388838248ada",
    "language": "python",
    "status": "RUNTIME_ERROR",
    "sourceCode": "print('hello')",
    "passedTestCases": 1,
    "totalTestCases": 10,
    "executionTime": 31,
    "memoryUsed": 2048,
    "compileOutput": null,
    "progressUpdated": false,
    "submittedAt": "2026-06-13T08:00:00Z",
    "testCases": [
      {
        "index": 1,
        "status": "ACCEPTED",
        "timeMs": 12,
        "memoryKb": 1024,
        "stdout": "2",
        "stderr": ""
      }
    ]
  }
}
```

## Submission history

Lấy toàn bộ submission của một lesson:

`GET /submissions/{lessonSlug}`

Lấy danh sách submission có phân trang/filter:

`GET /submissions?lessonSlug=two-sum&status=ACCEPTED&language=python&page=0&limit=20`

Các filter đều optional. `status` dùng verdict uppercase hoặc legacy lowercase đều được BE parse.

Item trong history có thêm `progressUpdated` để FE có thể hiển thị submission nào đã mở khóa/hoàn thành progress:

```json
{
  "id": "c973b055-f6ec-47bc-bac8-388838248ada",
  "language": "python",
  "status": "ACCEPTED",
  "passedTestcases": 10,
  "totalTestcases": 10,
  "executionTime": 31,
  "memoryUsed": 2048,
  "progressUpdated": true,
  "submittedAt": "2026-06-13T08:00:00Z"
}
```

## FE display rules

1. Hiển thị `stderr` ưu tiên cho `COMPILATION_ERROR`, `RUNTIME_ERROR`, `SYSTEM_ERROR`.
2. Hiển thị `stdout` cho `WRONG_ANSWER` để người dùng thấy output thực tế.
3. Với `TIME_LIMIT_EXCEEDED` hoặc `MEMORY_LIMIT_EXCEEDED`, hiển thị verdict cùng `timeMs`/`memoryKb`; vẫn hiển thị `stderr` nếu có.
4. Không đổi tên hoặc tự map verdict dựa trên exit code ở FE; verdict từ BE là nguồn chính xác.
5. Dùng `index` để sắp xếp testcase trong detail response.
6. Không tự đánh dấu lesson completed ở FE trước khi poll thấy terminal result.
7. Sau terminal result, nếu `progressUpdated=true`, refresh dữ liệu progress thay vì tự mutate toàn bộ roadmap phức tạp ở client.
