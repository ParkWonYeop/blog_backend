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

interface DashboardPostStat {
  id: number;
  title: string;
  slug: string;
  categoryName: string;
  viewCount: number;
  rangeViewCount: number;
  commentCount?: number;
  createdAt: string;
  updatedAt?: string | null;
}

interface DashboardCategoryStat {
  id: number;
  name: string;
  parentId?: number | null;
  postCount: number;
  viewCount: number;
  recentViewCount: number;
  lastPublishedAt?: string | null;
  childrenCount: number;
}

interface DashboardActionItems {
  unansweredComments: number;
  uncategorizedPosts: number;
  stalePopularPosts: number;
}

interface AdminDashboardResponse {
  overview: DashboardOverview;
  traffic: DashboardTrafficPoint[];
  topPosts: DashboardPostStat[];
  risingPosts: DashboardPostStat[];
  stalePopularPosts: DashboardPostStat[];
  recentPosts: PostSummary[];
  recentComments: AdminCommentSummary[];
  categoryStats: DashboardCategoryStat[];
  actionItems: DashboardActionItems;
}
```

`PostSummary`는 기존 `/api/posts`의 `Post` 응답과 최대한 맞춘다.

```ts
interface PostSummary {
  id: number;
  title: string;
  slug: string;
  categoryName: string;
  viewCount: number;
  createdAt: string;
  tags: string[];
}
```

`AdminCommentSummary`는 현재 프론트의 `AdminComment`와 맞춘다.

```ts
interface AdminCommentSummary {
  id: number;
  content: string;
  author?: string;
  guestNickname?: string;
  memberNickname?: string;
  postSlug?: string;
  postTitle?: string;
  createdAt: string;
}
```

### 8.3 Response Example

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "overview": {
      "todayViews": {
        "value": 311,
        "previousValue": 277,
        "changeRate": 12.27
      },
      "weekViews": {
        "value": 1830,
        "previousValue": 1504,
        "changeRate": 21.68
      },
      "monthViews": {
        "value": 7124,
        "previousValue": 6302,
        "changeRate": 13.04
      },
      "totalPosts": 102,
      "totalComments": 18,
      "totalCategories": 14,
      "lastPublishedAt": "2026-01-02T12:04:00+09:00",
      "generatedAt": "2026-05-28T20:00:00+09:00"
    },
    "traffic": [
      { "date": "2026-05-22", "views": 210 },
      { "date": "2026-05-23", "views": 245 },
      { "date": "2026-05-24", "views": 199 },
      { "date": "2026-05-25", "views": 287 },
      { "date": "2026-05-26", "views": 301 },
      { "date": "2026-05-27", "views": 277 },
      { "date": "2026-05-28", "views": 311 }
    ],
    "topPosts": [
      {
        "id": 1,
        "title": "RTR (Refresh Token Rotation)",
        "slug": "rtr-(refresh-token-rotation)",
        "categoryName": "Network",
        "viewCount": 126,
        "rangeViewCount": 44,
        "commentCount": 0,
        "createdAt": "2026-01-02T12:04:00+09:00",
        "updatedAt": null
      }
    ],
    "risingPosts": [
      {
        "id": 2,
        "title": "JWT - 토큰처리 방법",
        "slug": "jwt---토큰처리-방법",
        "categoryName": "Network",
        "viewCount": 500,
        "rangeViewCount": 38,
        "commentCount": 1,
        "createdAt": "2023-07-15T09:00:00+09:00",
        "updatedAt": null
      }
    ],
    "stalePopularPosts": [],
    "recentPosts": [],
    "recentComments": [],
    "categoryStats": [
      {
        "id": 10,
        "name": "Network",
        "parentId": 3,
        "postCount": 12,
        "viewCount": 1200,
        "recentViewCount": 210,
        "lastPublishedAt": "2026-01-02T12:04:00+09:00",
        "childrenCount": 0
      }
    ],
    "actionItems": {
      "unansweredComments": 0,
      "uncategorizedPosts": 0,
      "stalePopularPosts": 0
    }
  }
}
```

## 9. 필드별 계산 방법

### 9.1 overview

`todayViews.value`:

```sql
SELECT COALESCE(SUM(view_count), 0)
FROM post_view_daily_stats
WHERE stat_date = :today;
```

`todayViews.previousValue`:

```sql
SELECT COALESCE(SUM(view_count), 0)
FROM post_view_daily_stats
WHERE stat_date = :yesterday;
```

`weekViews.value`:

```sql
SELECT COALESCE(SUM(view_count), 0)
FROM post_view_daily_stats
WHERE stat_date BETWEEN :todayMinus6 AND :today;
```

`monthViews.value`:

```sql
SELECT COALESCE(SUM(view_count), 0)
FROM post_view_daily_stats
WHERE stat_date BETWEEN :todayMinus29 AND :today;
```

`totalPosts`:

- 삭제되지 않은 공개 글 수
- 비공개/임시저장 개념이 생기면 `published` 글만 count하거나 별도 필드 추가

`totalComments`:

- 삭제되지 않은 댓글 수

`totalCategories`:

- 전체 카테고리 수, 하위 포함

### 9.2 traffic

range 기준 날짜마다 point를 반드시 채운다.

예:

- 조회수가 없는 날짜도 `{ "date": "2026-05-23", "views": 0 }` 포함
- 프론트 차트가 날짜 간격을 안정적으로 그릴 수 있다.

### 9.3 topPosts

range 기간 내 조회수 기준 상위 글.

```sql
SELECT p.id, p.title, p.slug, c.name AS category_name,
       p.view_count,
       SUM(s.view_count) AS range_view_count,
       p.created_at,
       p.updated_at
FROM posts p
JOIN post_view_daily_stats s ON s.post_id = p.id
LEFT JOIN categories c ON c.id = p.category_id
WHERE s.stat_date BETWEEN :fromDate AND :toDate
GROUP BY p.id
ORDER BY range_view_count DESC
LIMIT 5;
```

### 9.4 risingPosts

최근 기간과 이전 같은 길이 기간을 비교해 증가량이 큰 글.

계산:

```text
currentRangeViews = 이번 기간 조회수
previousRangeViews = 직전 같은 기간 조회수
growth = currentRangeViews - previousRangeViews
```

정렬:

1. `growth DESC`
2. `currentRangeViews DESC`

조회수가 아주 낮은 글이 0에서 1이 되어 상승률 100%처럼 보이는 문제를 피하려면 최소 조회수 기준을 둔다.

권장:

- `currentRangeViews >= 5`

### 9.5 stalePopularPosts

최근에도 많이 읽히지만 오래 업데이트되지 않은 글.

MVP 기준:

- `rangeViewCount >= 10`
- `updatedAt`이 있으면 `updatedAt <= today - 180 days`
- `updatedAt`이 없으면 `createdAt <= today - 180 days`

정렬:

1. `rangeViewCount DESC`
2. 오래된 순

### 9.6 categoryStats

카테고리별 글 수, 누적 조회수, 최근 조회수.

하위 카테고리 포함 여부:

- MVP는 해당 카테고리에 직접 속한 글만 계산
- 추후 옵션으로 하위 포함 계산 가능

응답 필드:

- `postCount`: 직접 속한 글 수
- `viewCount`: 직접 속한 글의 누적 조회수 합
- `recentViewCount`: range 기간 직접 속한 글의 조회수 합
- `lastPublishedAt`: 직접 속한 글 중 최신 발행일
- `childrenCount`: 바로 아래 하위 카테고리 수

### 9.7 actionItems

`unansweredComments`:

- 7.3 기준 참고

`uncategorizedPosts`:

- category가 null이거나 `미분류` 카테고리에 속한 글 수

`stalePopularPosts`:

- 9.5 기준으로 계산된 글 수

## 10. 선택 분리 엔드포인트

통합 대시보드가 무거워지면 다음 엔드포인트를 추가한다. 프론트는 1차에서 쓰지 않아도 된다.

### 10.1 Traffic

```http
GET /api/admin/dashboard/traffic?range=30d&timezone=Asia/Seoul
```

응답:

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": [
    { "date": "2026-05-22", "views": 210 }
  ]
}
```

### 10.2 Top posts

```http
GET /api/admin/dashboard/posts/top?range=7d&size=10&timezone=Asia/Seoul
```

응답:

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": []
}
```

### 10.3 Category stats

```http
GET /api/admin/dashboard/categories?range=30d&timezone=Asia/Seoul
```

응답:

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": []
}
```

### 10.4 Action items

```http
GET /api/admin/dashboard/action-items?timezone=Asia/Seoul
```

응답:

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "unansweredComments": 0,
    "uncategorizedPosts": 0,
    "stalePopularPosts": 0
  }
}
```

## 11. 성능과 캐싱

관리자 대시보드는 실시간 초단위 정확도가 필요하지 않다.

권장:

- `/api/admin/dashboard` 응답은 30-60초 서버 캐싱 가능
- 조회수 증가 write는 가볍게 처리
- 일별 통계 upsert는 DB index를 반드시 둔다
- traffic range가 90일이어도 row 수가 작으므로 부담이 낮다

동시성:

- 여러 요청이 동시에 같은 글을 조회할 수 있으므로 `(post_id, stat_date)` upsert는 atomic해야 한다.
- MySQL이면 `INSERT ... ON DUPLICATE KEY UPDATE view_count = view_count + 1`
- PostgreSQL이면 `INSERT ... ON CONFLICT ... DO UPDATE`

예:

```sql
INSERT INTO post_view_daily_stats (post_id, stat_date, view_count)
VALUES (:postId, :statDate, 1)
ON DUPLICATE KEY UPDATE
  view_count = view_count + 1,
  updated_at = CURRENT_TIMESTAMP;
```

## 12. 보안과 개인정보

- 관리자 대시보드 API는 public cache에 저장하지 않는다.
- access token, refresh token, IP, User-Agent를 로그에 직접 남기지 않는다.
- unique visitor를 도입하기 전에는 개인정보성 식별자를 저장하지 않는다.
- IP 기반 unique 집계를 도입한다면 salt hash와 보관 기간을 명확히 정한다.
- public API에는 오늘/주간/월간 조회수 집계를 추가하지 않는다.

## 13. 백엔드 구현 순서

### Phase 1: 일별 조회수 집계 기반

1. `post_view_daily_stats` migration 추가
2. 글 상세 조회 시 daily stats upsert 추가
3. 기존 `Post.viewCount` 증가 로직 유지
4. 인덱스와 unique key 확인
5. 기존 글 상세 API 회귀 테스트

### Phase 2: `/api/admin/dashboard` MVP
