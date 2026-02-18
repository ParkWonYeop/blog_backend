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

신규 대시보드 기능은 기존 API를 깨지 않고 관리자 API만 추가한다.

## 3. 신규 API 요약

MVP에서 반드시 구현할 엔드포인트:

```http
GET /api/admin/dashboard?range=30d&timezone=Asia/Seoul
```

향후 세부 화면이 커질 때 선택적으로 분리할 수 있는 엔드포인트:

```http
GET /api/admin/dashboard/traffic?range=30d&timezone=Asia/Seoul
GET /api/admin/dashboard/posts/top?range=7d&size=10&timezone=Asia/Seoul
GET /api/admin/dashboard/categories?range=30d&timezone=Asia/Seoul
GET /api/admin/dashboard/action-items?timezone=Asia/Seoul
```

프론트 1차 구현은 통합 엔드포인트인 `/api/admin/dashboard`만 사용한다. 분리 엔드포인트는 응답이 커지거나 위젯별 캐싱이 필요할 때 추가한다.

## 4. 공통 응답 규격

프론트는 기존 `ApiResponse<T>` 규격을 사용한다.

```ts
interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
}
```

성공 예:

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {}
}
```

권장 오류:

```json
{
  "code": "FORBIDDEN",
  "message": "관리자 권한이 필요합니다.",
  "data": null
}
```

권한:

- 모든 `/api/admin/dashboard*` 엔드포인트는 관리자 전용이다.
- 프론트에서 관리자 UI를 숨기더라도 백엔드가 최종 권한 검사를 수행한다.
- 비로그인: 401
- 로그인했지만 관리자 아님: 403

## 5. 날짜와 집계 기준

기본 timezone:

```text
Asia/Seoul
```

쿼리 파라미터:

```http
timezone=Asia/Seoul
range=7d | 30d | 90d
```

기간 정의:

- 오늘 조회수: `timezone` 기준 오늘 00:00:00부터 23:59:59까지
- 최근 7일 조회수: 오늘 포함 7일, 즉 오늘 + 이전 6일
- 최근 30일 조회수: 오늘 포함 30일, 즉 오늘 + 이전 29일
- 최근 90일 조회수: 오늘 포함 90일, 즉 오늘 + 이전 89일

전 기간 대비:

- 오늘 비교값: 어제
- 최근 7일 비교값: 직전 7일
- 최근 30일 비교값: 직전 30일
- 최근 90일 비교값: 직전 90일

`changeRate` 계산:

```text
previousValue = 0이고 currentValue > 0이면 changeRate는 null 또는 100으로 고정하지 않는다.
권장: null

previousValue > 0이면:
((currentValue - previousValue) / previousValue) * 100
소수점 둘째 자리까지 반올림
```

프론트는 `changeRate`가 없으면 변화율 badge를 숨긴다.

## 6. 조회수 수집 방식

현재 `Post.viewCount`는 총 누적 조회수로 사용된다. 대시보드에는 일별 집계가 필요하므로 누적 카운트만으로는 충분하지 않다.

### 6.1 권장 MVP 방식

글 상세 조회 시 기존 누적 조회수를 증가시키는 흐름에 일별 집계 upsert를 추가한다.

