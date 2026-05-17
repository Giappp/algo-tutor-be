---
name: api-docs
description: Generate API documentation from source code. Analyze REST endpoints,
  GraphQL schemas, or OpenAPI specs. Use when user mentions: API docs, documentation
  generation, endpoint documentation, REST API, GraphQL, OpenAPI, Swagger.
allowed-tools: Read, Grep, Bash, Write
model: claude-sonnet-4-5-20250929
---

# API Documentation Generator

## Overview
This skill helps generate comprehensive API documentation from source code,
focusing on REST endpoints, request/response formats, and usage examples.

## Instructions

### Step 1: Analyze Source Code
1. Use Grep to find controller/route files
2. Read files to understand endpoint structure
3. Identify request/response types

### Step 2: Extract Information
For each endpoint, document:
- HTTP method and path
- Request parameters (query, path, body)
- Response format
- Error codes
- Authentication requirements

### Step 3: Generate Documentation
Create markdown documentation with:
- Endpoint overview table
- Detailed endpoint descriptions
- Code examples in multiple languages
- Response schemas

## Example

### Finding Spring Boot Endpoints
\```bash
grep -r "@GetMapping\\|@PostMapping\\|@PutMapping\\|@DeleteMapping" src/
\```

### Documenting an Endpoint
\```markdown
## GET /api/users/{id}

Retrieve user by ID.

**Parameters:**
- `id` (path, required): User ID

**Response:** `200 OK`
\```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com"
}
\```

**Errors:**
- `404`: User not found
\```