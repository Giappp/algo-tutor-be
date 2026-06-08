# Learning Module Admin API Documentation

This document describes the REST API endpoints for managing the Learning Module (admin only). All admin endpoints require authentication with the `ADMIN` role.

**Base URL:** `/api/v1`

**Authentication:** JWT token stored in HTTP-Only Cookie.

---

## Table of Contents

1. [Learning Paths](#1-learning-paths)
2. [Topics](#2-topics)
3. [Lessons](#3-lessons)
4. [Editorials](#4-editorials)
5. [Test Cases](#5-test-cases)
6. [Quiz Questions](#6-quiz-questions)
7. [Common Types](#7-common-types)

---

## 1. Learning Paths

### 1.1 List All Learning Paths

Retrieve a paginated list of all learning paths.

```
GET /learning-paths
```

**Authorization:** `ADMIN` role required

**Query Parameters:**

| Parameter | Type   | Required | Default | Description                        |
|-----------|--------|----------|---------|------------------------------------|
| `page`    | int    | No       | 0       | Page number (0-indexed)            |
| `size`    | int    | No       | 10      | Number of items per page           |
| `level`   | string | No       | -       | Filter by level: `BEGINNER`, `INTERMEDIATE`, `ADVANCED` |
| `search`  | string | No       | -       | Search by name (partial match)     |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": 1,
      "name": "Data Structures Fundamentals",
      "slug": "data-structures-fundamentals",
      "level": "BEGINNER",
      "description": "Master the basics of arrays, linked lists, and trees",
      "goal": "Build a strong foundation in fundamental data structures",
      "thumbnailUrl": "https://example.com/images/ds-fundamentals.png",
      "deleted": false,
      "topicCount": 5,
      "totalLessonCount": 25,
      "publishedLessonCount": 20,
      "enrollmentCount": 150,
      "isPublished": true,
      "createdAt": "2026-01-15T10:30:00Z",
      "updatedAt": "2026-04-20T14:45:00Z",
      "topics": []
    }
  ],
  "pageSize": 10,
  "totalPages": 3,
  "totalElements": 25,
  "currentPage": 0
}
```

---

### 1.2 Create Learning Path

Create a new learning path.

```
POST /learning-paths
```

**Authorization:** `ADMIN` role required

**Request Body:**

```json
{
  "name": "Data Structures Fundamentals",
  "description": "Master the basics of arrays, linked lists, and trees",
  "goal": "Build a strong foundation in fundamental data structures",
  "level": "BEGINNER",
  "thumbnailUrl": "https://example.com/images/ds-fundamentals.png"
}
```

| Field         | Type   | Required | Description                                          |
|---------------|--------|----------|------------------------------------------------------|
| `name`        | string | Yes      | Name of the learning path                            |
| `description` | string | Yes      | Detailed description                                 |
| `goal`        | string | Yes      | Learning objective                                   |
| `level`       | enum   | Yes      | One of: `BEGINNER`, `INTERMEDIATE`, `ADVANCED`      |
| `thumbnailUrl`| string | No       | URL to thumbnail image                               |

**Response:** `200 OK`

```json
{
  "data": {
    "id": 1,
    "name": "Data Structures Fundamentals",
    "slug": "data-structures-fundamentals",
    "level": "BEGINNER",
    "description": "Master the basics of arrays, linked lists, and trees",
    "goal": "Build a strong foundation in fundamental data structures",
    "thumbnailUrl": "https://example.com/images/ds-fundamentals.png",
    "deleted": false,
    "topicCount": 0,
    "totalLessonCount": 0,
    "publishedLessonCount": 0,
    "enrollmentCount": 0,
    "isPublished": false,
    "createdAt": "2026-05-03T11:00:00Z",
    "updatedAt": "2026-05-03T11:00:00Z",
    "topics": []
  },
  "success": true
}
```

---

### 1.3 Get Learning Path by ID

Retrieve a single learning path by its ID.

```
GET /learning-paths/{id}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter | Type | Required | Description     |
|-----------|------|----------|-----------------|
| `id`      | long | Yes      | Learning path ID|

**Response:** `200 OK`

```json
{
  "data": {
    "id": 1,
    "name": "Data Structures Fundamentals",
    "slug": "data-structures-fundamentals",
    "level": "BEGINNER",
    "description": "Master the basics of arrays, linked lists, and trees",
    "goal": "Build a strong foundation in fundamental data structures",
    "thumbnailUrl": "https://example.com/images/ds-fundamentals.png",
    "deleted": false,
    "topicCount": 5,
    "totalLessonCount": 25,
    "publishedLessonCount": 20,
    "enrollmentCount": 150,
    "isPublished": true,
    "createdAt": "2026-01-15T10:30:00Z",
    "updatedAt": "2026-04-20T14:45:00Z",
    "topics": [
      {
        "id": 1,
        "name": "Arrays",
        "description": "Array fundamentals and operations",
        "scopeTags": "arrays,indexing,slicing",
        "orderIndex": 1,
        "isLocked": false,
        "learningPathId": 1,
        "lessonCount": 8,
        "createdAt": "2026-01-15T10:35:00Z",
        "updatedAt": "2026-01-15T10:35:00Z",
        "lessons": []
      }
    ]
  },
  "success": true
}
```

**Errors:**

- `404 Not Found` - Learning path not found

---

### 1.4 Update Learning Path

Update an existing learning path.

```
PUT /learning-paths/{id}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter | Type | Required | Description     |
|-----------|------|----------|-----------------|
| `id`      | long | Yes      | Learning path ID|

**Request Body:**

```json
{
  "name": "Updated Name",
  "description": "Updated description",
  "goal": "Updated goal",
  "level": "INTERMEDIATE",
  "thumbnailUrl": "https://example.com/images/new-thumbnail.png"
}
```

*All fields are required.*

**Response:** `200 OK`

```json
{
  "data": { /* Updated LearningPathResponseDTO */ },
  "success": true
}
```

**Errors:**

- `404 Not Found` - Learning path not found

---

### 1.5 Delete Learning Path

Soft-delete a learning path.

```
DELETE /learning-paths/{id}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter | Type | Required | Description     |
|-----------|------|----------|-----------------|
| `id`      | long | Yes      | Learning path ID|

**Response:** `200 OK`

```json
{
  "data": "Learning path deleted successfully",
  "success": true
}
```

**Errors:**

- `404 Not Found` - Learning path not found

---

### 1.6 Toggle Publish Status

Toggle the publish status of a learning path (publish/unpublish).

```
PATCH /learning-paths/{id}/publish
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter | Type | Required | Description     |
|-----------|------|----------|-----------------|
| `id`      | long | Yes      | Learning path ID|

**Response:** `200 OK`

```json
{
  "data": {
    "id": 1,
    "name": "Data Structures Fundamentals",
    "isPublished": false,
    /* ... other fields */
  },
  "success": true
}
```

**Errors:**

- `404 Not Found` - Learning path not found

---

### 1.7 Get Public Learning Path by Slug (Public Endpoint)

Retrieve a published learning path by its slug.

```
GET /learning-paths/public/{slug}
```

**Authorization:** None (public endpoint)

**Path Parameters:**

| Parameter | Type   | Required | Description            |
|-----------|--------|----------|------------------------|
| `slug`    | string | Yes      | URL-friendly slug name |

**Response:** `200 OK`

```json
{
  "data": { /* LearningPathResponseDTO with published topics and lessons */ },
  "success": true
}
```

**Errors:**

- `404 Not Found` - Learning path not found or not published

---

## 2. Topics

### 2.1 Create Topic

Create a new topic within a learning path.

```
POST /topics/learning-paths/{pathId}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter | Type | Required | Description      |
|-----------|------|----------|------------------|
| `pathId`  | long | Yes      | Learning path ID |

**Request Body:**

```json
{
  "name": "Arrays",
  "description": "Array fundamentals and operations",
  "scopeTags": "arrays,indexing,slicing",
  "isLocked": false
}
```

| Field         | Type    | Required | Description                              |
|---------------|---------|----------|------------------------------------------|
| `name`        | string  | No       | Name of the topic                        |
| `description` | string  | No       | Description of the topic                  |
| `scopeTags`   | string  | No       | Comma-separated tags for AI scope control|
| `isLocked`    | boolean | No       | Whether the topic is locked (default: false) |

**Response:** `200 OK`

```json
{
  "data": {
    "id": 1,
    "name": "Arrays",
    "description": "Array fundamentals and operations",
    "scopeTags": "arrays,indexing,slicing",
    "orderIndex": 1,
    "isLocked": false,
    "learningPathId": 1,
    "lessonCount": 0,
    "createdAt": "2026-05-03T11:00:00Z",
    "updatedAt": "2026-05-03T11:00:00Z",
    "lessons": []
  },
  "success": true
}
```

---

### 2.2 Get Topics by Learning Path (Public)

Retrieve all topics for a learning path.

```
GET /topics/learning-paths/{pathId}
```

**Authorization:** None (public endpoint)

**Path Parameters:**

| Parameter | Type | Required | Description      |
|-----------|------|----------|------------------|
| `pathId`  | long | Yes      | Learning path ID |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": 1,
      "name": "Arrays",
      "description": "Array fundamentals and operations",
      "scopeTags": "arrays,indexing,slicing",
      "orderIndex": 1,
      "isLocked": false,
      "learningPathId": 1,
      "lessonCount": 8,
      "createdAt": "2026-01-15T10:35:00Z",
      "updatedAt": "2026-01-15T10:35:00Z",
      "lessons": []
    }
  ],
  "success": true
}
```

---

### 2.3 Get Topic by ID (Public)

Retrieve a single topic by its ID.

```
GET /topics/{topicId}
```

**Authorization:** None (public endpoint)

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `topicId` | long | Yes      | Topic ID    |

**Response:** `200 OK`

```json
{
  "data": {
    "id": 1,
    "name": "Arrays",
    "description": "Array fundamentals and operations",
    "scopeTags": "arrays,indexing,slicing",
    "orderIndex": 1,
    "isLocked": false,
    "learningPathId": 1,
    "lessonCount": 8,
    "createdAt": "2026-01-15T10:35:00Z",
    "updatedAt": "2026-01-15T10:35:00Z",
    "lessons": []
  },
  "success": true
}
```

---

### 2.4 Update Topic

Update an existing topic.

```
PUT /topics/{topicId}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `topicId` | long | Yes      | Topic ID    |

**Request Body:**

```json
{
  "name": "Updated Topic Name",
  "description": "Updated description",
  "scopeTags": "arrays,indexing,slicing,sorting",
  "isLocked": true
}
```

*All fields are optional.*

**Response:** `200 OK`

```json
{
  "data": { /* Updated TopicResponseDTO */ },
  "success": true
}
```

**Errors:**

- `404 Not Found` - Topic not found

---

### 2.5 Delete Topic

Delete a topic (and all its lessons).

```
DELETE /topics/{topicId}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `topicId` | long | Yes      | Topic ID    |

**Response:** `200 OK`

```json
{
  "data": "Topic deleted successfully",
  "success": true
}
```

**Errors:**

- `404 Not Found` - Topic not found

---

## 3. Lessons

### 3.1 Create Lesson

Create a new lesson within a topic. The lesson type is determined by the `type` field in the request body.

```
POST /lessons/topics/{topicId}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `topicId` | long | Yes      | Topic ID    |

**Request Body (Theory Lesson):**

```json
{
  "title": "Introduction to Arrays",
  "type": "THEORY",
  "difficulty": "EASY",
  "content": "# Introduction to Arrays\n\nArrays are contiguous blocks of memory..."
}
```

**Request Body (Coding Lesson):**

```json
{
  "title": "Two Sum",
  "type": "CODING",
  "difficulty": "EASY",
  "statement": "Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.",
  "baseTimeLimitMs": 2000,
  "baseMemoryLimitMb": 256,
  "constraints": [
    "2 <= nums.length <= 10^4",
    "-10^9 <= nums[i] <= 10^9"
  ],
  "starterCode": {
    "java": "class Solution { public int[] twoSum(int[] nums, int target) { } }",
    "python": "class Solution: def twoSum(self, nums: List[int], target: int) -> List[int]: pass"
  },
  "testCases": [
    {
      "stdin": "[2,7,11,15]\n9",
      "expectedStdout": "[0,1]",
      "isHidden": false,
      "orderIndex": 1,
      "explanation": "Because nums[0] + nums[1] == 9, we return [0, 1]."
    }
  ],
  "examples": [
    {
      "input": "[2,7,11,15]\n9",
      "output": "[0,1]",
      "explanation": "Because nums[0] + nums[1] == 9, we return [0, 1]."
    }
  ],
  "hints": ["Try using a hash map to store seen values."],
  "keyInsights": ["Use a hash map for O(n) solution."]
}
```

**Request Body (Quiz Lesson):**

```json
{
  "title": "Arrays Quiz",
  "type": "QUIZ",
  "difficulty": "EASY",
  "passingScore": 70,
  "timeLimitMinutes": 10,
  "questions": [
    {
      "question": "What is the time complexity of accessing an element in an array by index?",
      "type": "SINGLE_CHOICE",
      "points": 10,
      "explanation": "Array elements can be accessed directly using their index.",
      "choices": [
        { "text": "O(1)", "isCorrect": true, "explanation": "Correct! Array access is constant time." },
        { "text": "O(n)", "isCorrect": false },
        { "text": "O(log n)", "isCorrect": false },
        { "text": "O(n^2)", "isCorrect": false }
      ]
    }
  ]
}
```

> **Note:** `isCorrect` is accepted on input but never returned in any response. `QuizChoice` has no `id` field; choices are identified by their array position.
```

**Response:** `200 OK`

```json
{
  "data": {
    "id": 1,
    "title": "Two Sum",
    "slug": "two-sum",
    "type": "CODING",
    "orderIndex": 1,
    "isPublished": false,
    "difficulty": "EASY",
    "createdAt": "2026-05-03T11:00:00Z",
    "updatedAt": "2026-05-03T11:00:00Z"
  },
  "success": true
}
```

---

### 3.2 Get Lesson by ID

Retrieve a single lesson by its ID.

```
GET /lessons/{lessonId}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `lessonId`| long | Yes      | Lesson ID   |

**Response:** `200 OK`

The response varies by lesson type:

**Theory Lesson Response:**

```json
{
  "data": {
    "id": 1,
    "title": "Introduction to Arrays",
    "content": "# Introduction to Arrays\n\nArrays are contiguous blocks of memory...",
    "orderIndex": 1,
    "isPublished": false,
    "difficulty": "EASY"
  },
  "success": true
}
```

**Coding Lesson Response:**

```json
{
  "data": {
    "id": 1,
    "title": "Two Sum",
    "statement": "Given an array of integers nums...",
    "orderIndex": 1,
    "isPublished": false,
    "difficulty": "EASY",
    "baseTimeLimitMs": 2000,
    "baseMemoryLimitMb": 256,
    "constraints": ["2 <= nums.length <= 10^4"],
    "starterCode": {
      "java": "class Solution { public int[] twoSum(int[] nums, int target) { } }",
      "python": "class Solution: def twoSum(self, nums: List[int], target: int) -> List[int]: pass"
    },
    "hints": ["Try using a hash map."],
    "examples": [
      {
        "input": "[2,7,11,15]\n9",
        "output": "[0,1]",
        "explanation": "..."
      }
    ],
    "keyInsights": ["Use a hash map for O(n) solution."],
    "testCases": [
      {
        "id": 1,
        "stdin": "[2,7,11,15]\n9",
        "expectedStdout": "[0,1]",
        "isHidden": false,
        "orderIndex": 1,
        "explanation": "..."
      }
    ],
    "editorials": []
  },
  "success": true
}
```

**Quiz Lesson Response:**

```json
{
  "data": {
    "id": 1,
    "title": "Arrays Quiz",
    "orderIndex": 1,
    "isPublished": false,
    "difficulty": "EASY",
    "passingScore": 70,
    "timeLimitMinutes": 10,
    "questions": [
      {
        "id": 1,
        "question": "What is the time complexity of accessing an element in an array by index?",
        "type": "SINGLE_CHOICE",
        "points": 10,
        "explanation": "Array elements can be accessed directly using their index.",
        "orderIndex": 1,
        "choices": [
          { "text": "O(1)", "explanation": "Correct! Array access is constant time." },
          { "text": "O(n)", "explanation": null },
          { "text": "O(log n)", "explanation": null },
          { "text": "O(n^2)", "explanation": null }
        ]
      }
    ],
    "createdAt": "2026-05-03T11:00:00Z",
    "updatedAt": "2026-05-03T11:00:00Z"
  },
  "success": true
}
```
```

**Errors:**

- `404 Not Found` - Lesson not found

---

### 3.3 Get Lesson by Slug

Retrieve a single lesson by its slug.

```
GET /lessons/slug/{slug}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter | Type   | Required | Description   |
|-----------|--------|----------|---------------|
| `slug`    | string | Yes      | URL-friendly slug |

**Response:** Same as [Get Lesson by ID](#32-get-lesson-by-id)

**Errors:**

- `404 Not Found` - Lesson not found

---

### 3.4 Get Lessons by Topic

Retrieve lessons for a specific topic with pagination.

```
GET /lessons/topics/{topicId}
```

**Authorization:** None (public endpoint)

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `topicId` | long | Yes      | Topic ID    |

**Query Parameters:**

| Parameter      | Type | Required | Default | Description                        |
|----------------|------|----------|---------|------------------------------------|
| `page`         | int  | No       | 0       | Page number (0-indexed)            |
| `size`         | int  | No       | 20      | Number of items per page           |
| `publishedOnly`| bool | No       | false   | Only return published lessons      |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": 1,
      "title": "Introduction to Arrays",
      "slug": "introduction-to-arrays",
      "type": "THEORY",
      "orderIndex": 1,
      "isPublished": true,
      "difficulty": "EASY",
      "createdAt": "2026-01-15T10:40:00Z",
      "updatedAt": "2026-01-15T10:40:00Z"
    }
  ],
  "pageSize": 20,
  "totalPages": 2,
  "totalElements": 25,
  "currentPage": 0
}
```

---

### 3.5 Update Lesson

Update an existing lesson.

```
PUT /lessons/{lessonId}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `lessonId`| long | Yes      | Lesson ID   |

**Request Body:** Same as [Create Lesson](#31-create-lesson), include all fields.

**Response:** `200 OK`

```json
{
  "data": { /* Updated lesson response based on type */ },
  "success": true
}
```

**Errors:**

- `404 Not Found` - Lesson not found

---

### 3.6 Delete Lesson

Delete a lesson.

```
DELETE /lessons/{lessonId}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `lessonId`| long | Yes      | Lesson ID   |

**Response:** `200 OK`

```json
{
  "data": "Lesson deleted successfully",
  "success": true
}
```

**Errors:**

- `404 Not Found` - Lesson not found

---

### 3.7 Toggle Lesson Publish Status

Toggle the publish status of a lesson.

```
PATCH /lessons/{lessonId}/publish
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `lessonId`| long | Yes      | Lesson ID   |

**Response:** `200 OK`

```json
{
  "data": {
    "id": 1,
    "title": "Two Sum",
    "isPublished": true,
    /* ... other fields */
  },
  "success": true
}
```

**Errors:**

- `404 Not Found` - Lesson not found

---

### 3.8 Get Published Lesson by ID (Public)

Retrieve a published lesson by its ID.

```
GET /lessons/public/{lessonId}
```

**Authorization:** None (public endpoint)

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `lessonId`| long | Yes      | Lesson ID   |

**Response:** `200 OK` - Returns lesson data without hidden test cases.

**Errors:**

- `404 Not Found` - Lesson not found or not published

---

### 3.9 Get Published Lesson by Slug (Public)

Retrieve a published lesson by its slug.

```
GET /lessons/public/slug/{slug}
```

**Authorization:** None (public endpoint)

**Path Parameters:**

| Parameter | Type   | Required | Description   |
|-----------|--------|----------|---------------|
| `slug`    | string | Yes      | Lesson slug   |

**Response:** `200 OK` - Returns lesson data without hidden test cases.

**Errors:**

- `404 Not Found` - Lesson not found or not published

---

## 4. Editorials

Editorials contain solution code for coding lessons, supporting multiple programming languages.

### 4.1 Create Editorial

Add an editorial to a lesson.

```
POST /editorials/lessons/{lessonId}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `lessonId`| long | Yes      | Lesson ID   |

**Request Body:**

```json
{
  "language": "JAVA",
  "sourceCode": "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        Map<Integer, Integer> map = new HashMap<>();\n        for (int i = 0; i < nums.length; i++) {\n            int complement = target - nums[i];\n            if (map.containsKey(complement)) {\n                return new int[] { map.get(complement), i };\n            }\n            map.put(nums[i], i);\n        }\n        return new int[] {};\n    }\n}"
}
```

| Field       | Type   | Required | Description                                      |
|-------------|--------|----------|--------------------------------------------------|
| `language`  | enum   | Yes      | One of: `JAVA`, `PYTHON`                         |
| `sourceCode`| string | Yes      | The solution code                                |

**Response:** `200 OK`

```json
{
  "data": {
    "id": 1,
    "language": "JAVA",
    "sourceCode": "class Solution { ... }"
  },
  "success": true
}
```

---

### 4.2 Get Editorials by Lesson (Public)

Retrieve all editorials for a lesson.

```
GET /editorials/lessons/{lessonId}
```

**Authorization:** None (public endpoint)

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `lessonId`| long | Yes      | Lesson ID   |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": 1,
      "language": "JAVA",
      "sourceCode": "class Solution { ... }"
    },
    {
      "id": 2,
      "language": "PYTHON",
      "sourceCode": "class Solution:\n    def twoSum(self, nums: List[int], target: int) -> List[int]:\n        ..."
    }
  ],
  "success": true
}
```

---

### 4.3 Update Editorial

Update an existing editorial.

```
PUT /editorials/{editorialId}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter   | Type | Required | Description |
|-------------|------|----------|-------------|
| `editorialId`| long | Yes      | Editorial ID|

**Request Body:**

```json
{
  "language": "JAVA",
  "sourceCode": "Updated source code here..."
}
```

**Response:** `200 OK`

```json
{
  "data": { /* Updated EditorialResponseDTO */ },
  "success": true
}
```

**Errors:**

- `404 Not Found` - Editorial not found

---

### 4.4 Delete Editorial

Delete an editorial.

```
DELETE /editorials/{editorialId}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter   | Type | Required | Description |
|-------------|------|----------|-------------|
| `editorialId`| long | Yes      | Editorial ID|

**Response:** `200 OK`

```json
{
  "data": "Editorial deleted successfully",
  "success": true
}
```

**Errors:**

- `404 Not Found` - Editorial not found

---

## 5. Test Cases

Test cases define inputs and expected outputs for coding lessons.

### 5.1 Create Test Case

Add a test case to a lesson.

```
POST /testcases/lessons/{lessonId}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `lessonId`| long | Yes      | Lesson ID   |

**Request Body:**

```json
{
  "stdin": "[2,7,11,15]\n9",
  "expectedStdout": "[0,1]",
  "isHidden": false,
  "orderIndex": 1,
  "explanation": "Because nums[0] + nums[1] == 9, we return [0, 1]."
}
```

| Field            | Type    | Required | Description                              |
|------------------|---------|----------|------------------------------------------|
| `stdin`          | string  | Yes      | Standard input for the test case         |
| `expectedStdout` | string  | Yes      | Expected standard output                 |
| `isHidden`       | boolean | No       | Hidden from users (default: false)       |
| `orderIndex`     | integer | No       | Display order                            |
| `explanation`    | string  | No       | Explanation of the test case             |

**Response:** `200 OK`

```json
{
  "data": {
    "id": 1,
    "stdin": "[2,7,11,15]\n9",
    "expectedStdout": "[0,1]",
    "isHidden": false,
    "orderIndex": 1,
    "explanation": "Because nums[0] + nums[1] == 9, we return [0, 1]."
  },
  "success": true
}
```

---

### 5.2 Get Test Cases by Lesson

Retrieve all test cases for a lesson.

```
GET /testcases/lessons/{lessonId}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `lessonId`| long | Yes      | Lesson ID   |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": 1,
      "stdin": "[2,7,11,15]\n9",
      "expectedStdout": "[0,1]",
      "isHidden": false,
      "orderIndex": 1,
      "explanation": "..."
    },
    {
      "id": 2,
      "stdin": "[3,2,4]\n6",
      "expectedStdout": "[1,2]",
      "isHidden": true,
      "orderIndex": 2,
      "explanation": null
    }
  ],
  "success": true
}
```

---

### 5.3 Update Test Case

Update an existing test case.

```
PUT /testcases/{testCaseId}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter   | Type | Required | Description |
|-------------|------|----------|-------------|
| `testCaseId`| long | Yes      | Test case ID|

**Request Body:**

```json
{
  "stdin": "[2,7,11,15]\n9",
  "expectedStdout": "[0,1]",
  "isHidden": false,
  "orderIndex": 1,
  "explanation": "Updated explanation..."
}
```

**Response:** `200 OK`

```json
{
  "data": { /* Updated TestCaseResponseDTO */ },
  "success": true
}
```

**Errors:**

- `404 Not Found` - Test case not found

---

### 5.4 Delete Test Case

Delete a test case.

```
DELETE /testcases/{testCaseId}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter   | Type | Required | Description |
|-------------|------|----------|-------------|
| `testCaseId`| long | Yes      | Test case ID|

**Response:** `200 OK`

```json
{
  "data": "Test case deleted successfully",
  "success": true
}
```

**Errors:**

- `404 Not Found` - Test case not found

---

## 6. Quiz Questions

Quiz questions are part of quiz lessons and contain multiple choice options stored as embedded JSON. For a complete reference with detailed data shapes and design notes, see the [Quiz API Documentation](./quiz-api.md).

### 6.1 Add Question to Quiz

Add a question to a quiz lesson.

```
POST /questions/lessons/{lessonId}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `lessonId`| long | Yes      | Quiz lesson ID|

**Request Body:**

```json
{
  "question": "What is the time complexity of accessing an element in an array by index?",
  "type": "MULTIPLE_CHOICE",
  "points": 10,
  "explanation": "Array elements can be accessed directly using their index in O(1) time.",
  "choices": [
    { "text": "O(1)", "isCorrect": true, "explanation": "Correct! Array access is constant time." },
    { "text": "O(n)", "isCorrect": false, "explanation": "Incorrect. Array access doesn't require traversal." },
    { "text": "O(log n)", "isCorrect": false },
    { "text": "O(n^2)", "isCorrect": false }
  ]
}
```

| Field         | Type   | Required | Description                                              |
|---------------|--------|----------|----------------------------------------------------------|
| `question`    | string | Yes      | The question text                                        |
| `type`        | enum   | Yes      | One of: `MULTIPLE_CHOICE`, `SINGLE_CHOICE`, `TRUE_FALSE` |
| `points`      | integer| No       | Points for correct answer (default: 1)                  |
| `explanation` | string | No       | Explanation shown after answering                        |
| `choices`     | array  | Yes      | List of answer choices                                   |

**Choice Object:**

| Field         | Type    | Required | Description                          |
|---------------|---------|----------|--------------------------------------|
| `text`        | string  | Yes      | Choice text                          |
| `isCorrect`   | boolean | Yes      | Whether this is the correct answer   |
| `explanation` | string  | No       | Explanation for this choice          |

> **Note:** `QuizChoice` has no `id` field. Choices are identified by their position in the array.

**Response:** `200 OK`

```json
{
  "data": {
    "id": 1,
    "question": "What is the time complexity of accessing an element in an array by index?",
    "type": "SINGLE_CHOICE",
    "points": 10,
    "explanation": "Array elements can be accessed directly using their index in O(1) time.",
    "orderIndex": 1,
    "choices": [
      { "text": "O(1)", "explanation": "Correct! Array access is constant time." },
      { "text": "O(n)", "explanation": "Incorrect. Array access doesn't require traversal." },
      { "text": "O(log n)", "explanation": null },
      { "text": "O(n^2)", "explanation": null }
    ]
  },
  "success": true
}
```

---

### 6.2 Get Questions by Quiz Lesson (Public)

Retrieve all questions for a quiz lesson.

```
GET /questions/lessons/{lessonId}
```

**Authorization:** None (public endpoint)

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `lessonId`| long | Yes      | Quiz lesson ID|

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": 1,
      "question": "What is the time complexity of accessing an element in an array by index?",
      "type": "SINGLE_CHOICE",
      "points": 10,
      "explanation": null,
      "orderIndex": 1,
      "choices": [
        { "text": "O(1)", "explanation": null },
        { "text": "O(n)", "explanation": null },
        { "text": "O(log n)", "explanation": null },
        { "text": "O(n^2)", "explanation": null }
      ]
    }
  ],
  "success": true
}
```

> **Note:** `explanation` is always `null` in public responses to prevent answer leaking.

---

### 6.3 Update Question

Update an existing question.

```
PUT /questions/{questionId}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter   | Type | Required | Description |
|-------------|------|----------|-------------|
| `questionId`| long | Yes      | Question ID |

**Request Body:** Same as [Add Question to Quiz](#61-add-question-to-quiz)

**Response:** `200 OK`

```json
{
  "data": { /* Updated QuizQuestionResponseDTO */ },
  "success": true
}
```

**Errors:**

- `404 Not Found` - Question not found

---

### 6.4 Delete Question

Delete a question.

```
DELETE /questions/{questionId}
```

**Authorization:** `ADMIN` role required

**Path Parameters:**

| Parameter   | Type | Required | Description |
|-------------|------|----------|-------------|
| `questionId`| long | Yes      | Question ID |

**Response:** `200 OK`

```json
{
  "data": "Question deleted successfully",
  "success": true
}
```

**Errors:**

- `404 Not Found` - Question not found

---

## 7. Common Types

### 7.1 ApiResponse

The standard wrapper for all API responses.

```json
{
  "data": { ... },
  "message": "Optional message",
  "success": true
}
```

| Field    | Type    | Description                           |
|----------|---------|---------------------------------------|
| `data`   | any     | Response payload (null if message only)|
| `message`| string  | Optional success/info message         |
| `success`| boolean | Whether the request was successful     |

### 7.2 PageResponse

Paginated list response.

```json
{
  "data": [ ... ],
  "pageSize": 10,
  "totalPages": 5,
  "totalElements": 50,
  "currentPage": 0
}
```

| Field          | Type  | Description                      |
|----------------|-------|----------------------------------|
| `data`         | array | List of items for current page  |
| `pageSize`     | int   | Number of items per page         |
| `totalPages`   | int   | Total number of pages            |
| `totalElements`| long  | Total number of items           |
| `currentPage`  | int   | Current page number (0-indexed)  |

### 7.3 Enums

**Level:**

| Value        | Description            |
|--------------|------------------------|
| `BEGINNER`   | Beginner level         |
| `INTERMEDIATE`| Intermediate level     |
| `ADVANCED`   | Advanced level         |

**LessonType:**

| Value    | Description          |
|----------|----------------------|
| `THEORY` | Theory lesson        |
| `CODING` | Coding challenge     |
| `QUIZ`   | Quiz assessment      |

**Difficulty:**

| Value     | Description      |
|-----------|------------------|
| `EASY`    | Easy difficulty  |
| `MEDIUM`  | Medium difficulty|
| `HARD`    | Hard difficulty  |

**ProgrammingLanguage:**

| Value   | Description         |
|---------|---------------------|
| `JAVA`  | Java                |
| `PYTHON`| Python              |

**QuestionType:**

| Value             | Description                    |
|-------------------|--------------------------------|
| `SINGLE_CHOICE`   | Single correct answer          |
| `MULTIPLE_CHOICE` | Multiple correct answers       |
| `TRUE_FALSE`      | True or false question         |

> **Note:** Quiz choices are stored as embedded JSON within the question entity (no separate `QuizChoice` entity or repository).

---

## Authentication

All admin endpoints require a valid JWT token in the Cookie, FE should automatically check and send it to backend

---

## Error Responses

All endpoints may return the following error responses:

| Status Code | Description                          |
|-------------|--------------------------------------|
| `400`       | Bad Request - Invalid input           |
| `401`       | Unauthorized - Missing/invalid token  |
| `403`       | Forbidden - Insufficient permissions  |
| `404`       | Not Found - Resource doesn't exist   |
| `500`       | Internal Server Error                |

**Error Response Format:**

```json
{
  "data": null,
  "message": "Error description",
  "success": false
}
```

---

## Pagination

All list endpoints that return `PageResponse` support Spring Data pagination:

| Parameter | Type | Description                    |
|-----------|------|--------------------------------|
| `page`    | int  | Page number (0-indexed)        |
| `size`    | int  | Items per page (default varies)|
| `sort`    | string| Sort field and direction      |

**Example:** `GET /learning-paths?page=0&size=20&sort=createdAt,desc`
