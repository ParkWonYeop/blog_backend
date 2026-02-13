# 관리자 대시보드 백엔드 개발 문서

작성일: 2026-05-28

대상: WYPark Blog backend API

## 1. 목표

프론트엔드의 Apple 스타일 리뉴얼과 함께 관리자 대시보드를 실제 운영 도구로 만들기 위한 백엔드 API를 추가한다. 공개 홈에는 운영 집계를 노출하지 않고, 관리자 권한을 가진 사용자만 `/admin`에서 하루 조회수, 최근 7일 조회수, 최근 30일 조회수, 인기 글, 카테고리 상태, 답변 필요한 댓글 등을 모니터링할 수 있게 한다.

프론트엔드는 먼저 `/api/admin/dashboard` 계약을 기준으로 개발할 수 있다. 백엔드는 이 문서의 응답 구조를 맞추면 프론트 변경을 최소화하고 바로 연결할 수 있다.

## 2. 현재 프론트에서 사용하는 기존 API

현재 프론트엔드는 다음 API를 사용한다.

공개 API:

```http
GET /api/posts
GET /api/posts/{slug}
GET /api/categories
GET /api/profile
GET /api/comments?postSlug={slug}
POST /api/comments
DELETE /api/comments/{id}
```

관리자 API:

```http
POST /api/admin/posts
PUT /api/admin/posts/{id}
DELETE /api/admin/posts/{id}
GET /api/admin/comments?page={page}&size={size}
DELETE /api/admin/comments/{id}
POST /api/admin/categories
PUT /api/admin/categories/{id}
DELETE /api/admin/categories/{id}
PUT /api/admin/profile
POST /api/admin/images
```

