# Judge API - FE Alignment

Tài liệu này là contract cho luồng chạy thử và nộp bài qua Piston.

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
- Các field không áp dụng trong WebSocket final event sẽ bị bỏ khỏi JSON.
- BE gửi memory limit tới Piston bằng `run_memory_limit` theo đơn vị bytes.

Piston contract tham chiếu: <https://github.com/engineer-man/piston/blob/master/docs/api-v2.md#execute>

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

Submit tạo submission và chấm async trên toàn bộ testcase. HTTP response chỉ xác nhận submission đã được tạo:

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

FE subscribe topic:

```text
/topic/submissions/{submissionId}
```

### WebSocket testcase event

```json
{
  "type": "TEST_CASE",
  "submissionId": "c973b055-f6ec-47bc-bac8-388838248ada",
  "testCaseId": 12,
  "status": "RUNTIME_ERROR",
  "timeMs": 31,
  "memoryKb": 2048,
  "stdout": "partial output",
  "stderr": "division by zero",
  "sortOrder": 2,
  "isCompleted": true
}
```

`isCompleted=true` nghĩa là judge đã dừng hoặc đã chạy testcase cuối. FE vẫn phải chờ event `FINAL_RESULT` để chốt summary.

### WebSocket final event

```json
{
  "type": "FINAL_RESULT",
  "submissionId": "c973b055-f6ec-47bc-bac8-388838248ada",
  "status": "RUNTIME_ERROR",
  "passed": 1,
  "total": 10,
  "maxTimeMs": 31,
  "maxMemoryKb": 2048,
  "compilationError": null,
  "isCompleted": true
}
```

Submit dừng ở testcase lỗi đầu tiên. Vì vậy `passed` có thể nhỏ hơn `total` và FE không nên tự suy ra các testcase còn lại là `WRONG_ANSWER`.

## Submission detail

`GET /submissions/{submissionId}`

Dùng endpoint này để recover state khi FE reload hoặc mất WebSocket event.

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

## FE display rules

1. Hiển thị `stderr` ưu tiên cho `COMPILATION_ERROR`, `RUNTIME_ERROR`, `SYSTEM_ERROR`.
2. Hiển thị `stdout` cho `WRONG_ANSWER` để người dùng thấy output thực tế.
3. Với `TIME_LIMIT_EXCEEDED` hoặc `MEMORY_LIMIT_EXCEEDED`, hiển thị verdict cùng `timeMs`/`memoryKb`; vẫn hiển thị `stderr` nếu có.
4. Không đổi tên hoặc tự map verdict dựa trên exit code ở FE; verdict từ BE là nguồn chính xác.
5. Dùng `sortOrder`/`index` để sắp xếp testcase, không dùng thứ tự nhận WebSocket event.
