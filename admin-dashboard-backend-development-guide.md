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

대상 요청:

```http
GET /api/posts/{slug}
```

처리:

1. 게시글 조회
2. 조회수 증가 정책에 따라 `post.viewCount` 증가
3. `post_view_daily_stats`에서 `(post_id, stat_date)` row upsert
4. 관리자 대시보드는 이 daily stats를 조회

주의:

- 새로고침마다 증가되는 현재 정책이 있다면 MVP에서는 그대로 따른다.
- 중복 조회 방지는 별도 정책이므로 2차에서 다룬다.
- 검색 봇 제외 정책이 있다면 view 증가 전에 적용한다.

### 6.2 선택적 고도화

정확한 unique visitor가 필요해지면 다음 중 하나를 선택한다.

- 익명 daily visitor key cookie 발급
- IP + User-Agent + 날짜를 salt와 함께 hash
- 로그인 회원 ID 기준 unique 집계

개인 블로그 운영 통계 목적이라면 MVP에서는 단순 view count로 충분하다.

## 7. 데이터 모델 제안

기존 테이블 이름은 백엔드 프로젝트 관례에 맞춘다. 아래는 개념 모델이다.

### 7.1 post_view_daily_stats

글별 일간 조회수 집계 테이블.

```sql
CREATE TABLE post_view_daily_stats (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  stat_date DATE NOT NULL,
  view_count BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_post_view_daily_stats_post_date (post_id, stat_date),
  INDEX idx_post_view_daily_stats_date (stat_date),
  INDEX idx_post_view_daily_stats_post_id (post_id)
);
```

설명:

- `stat_date`는 `timezone` 기준 날짜다.
- 한국 블로그 운영만 고려하면 `Asia/Seoul` 기준 LocalDate를 저장한다.
- 다국가 timezone을 엄격히 지원하려면 UTC timestamp event table이 필요하지만 MVP에서는 과하다.

### 7.2 site_view_daily_stats, 선택

전체 조회수를 빠르게 보여주고 싶다면 별도 site daily table을 둘 수 있다.

```sql
CREATE TABLE site_view_daily_stats (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  stat_date DATE NOT NULL,
  view_count BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_site_view_daily_stats_date (stat_date)
);
```

하지만 `post_view_daily_stats`를 날짜별로 sum해도 되므로 처음에는 생략 가능하다.

### 7.3 comment reply 계산

답변 필요한 댓글을 계산하려면 댓글 구조에 다음 정보가 필요하다.

필수:

- `comment.id`
- `comment.post_id`
- `comment.parent_id`
- `comment.member_id` 또는 작성자 식별 정보
- `comment.created_at`

권장:

- 댓글 작성자의 role 또는 `isPostAuthor`
- 삭제 여부

MVP 기준:

- 루트 댓글이고 삭제되지 않음
- 작성자가 관리자/글 작성자가 아님
- 해당 댓글의 자식 댓글 중 관리자/글 작성자 댓글이 없음

이 조건을 만족하는 댓글 수를 `actionItems.unansweredComments`로 반환한다.

## 8. 통합 대시보드 API

### 8.1 Request

```http
GET /api/admin/dashboard?range=30d&timezone=Asia/Seoul
Authorization: Bearer {accessToken}
```

Query params:

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `range` | `7d`, `30d`, `90d` | 아니오 | `30d` | 트래픽 차트와 성과 위젯 기준 기간 |
| `timezone` | string | 아니오 | `Asia/Seoul` | 날짜 집계 기준 timezone |

Validation:

- `range`가 허용값이 아니면 `30d`로 fallback하거나 400을 반환한다.
- 권장: 400보다 fallback이 운영 UI에는 부드럽다.
- `timezone`이 유효하지 않으면 `Asia/Seoul` 사용.

### 8.2 Response Type

```ts
type DashboardRange = '7d' | '30d' | '90d';

interface DashboardMetric {
  value: number;
  previousValue?: number;
  changeRate?: number | null;
}

interface DashboardOverview {
  todayViews: DashboardMetric;
  weekViews: DashboardMetric;
  monthViews: DashboardMetric;
  totalPosts: number;
  totalComments: number;
  totalCategories: number;
  lastPublishedAt?: string | null;
  generatedAt: string;
}

interface DashboardTrafficPoint {
  date: string;
  views: number;
}

