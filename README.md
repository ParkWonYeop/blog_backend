# 🛠️ Spring Boot Blog API Server

## 🚀 Project Overview
기존 **Jekyll(GitHub Pages)** 로 운영하던 정적 블로그의 한계를 극복하고, 확장 가능한 블로그 시스템을 구축하기 위해 개발한 **Spring Boot 기반의 REST API 서버**입니다.

정적 사이트에서는 구현하기 어려웠던 **동적 포스팅 관리, 관리자 대시보드(Admin), 댓글 시스템** 등의 기능을 직접 구현하여 블로그 운영의 효율성을 높였습니다. **RESTful 원칙**에 입각한 API 설계와 **Spring Security**를 활용한 보안/인증 로직을 적용하여 백엔드 아키텍처 역량을 강화하는 데 중점을 두었습니다.

### 🎯 Key Objectives
* **Migration**: 정적 파일(Jekyll) 의존성을 제거하고 DB 기반의 동적 시스템으로 전환
* **Feature Expansion**: 웹 에디터, 게시글 관리, 방문자 통계 등 관리자(Admin) 기능 추가
* **Architecture**: 프론트엔드와 백엔드를 분리한 REST API 서버 구축

### 🛠️ Tech Stack
* **Java 21, Spring Boot 3.5.9**
* **Spring Security, JWT** (Auth)
* **JPA (Hibernate), QueryDSL** (ORM)
* **PostgreSQL, Redis** (Cache/Session)
* **Markdown Parser** (Content)

---

## 🔌 API Reference

**Base URL**: `http://localhost:8080`  
**Auth**: `Authorization: Bearer {Access_Token}`

### 1. 공통 응답 규격 (Common Response)
모든 API 응답은 아래 표준 포맷을 따릅니다.

```json
{
  "code": "SUCCESS",       // 결과 코드 (SUCCESS 또는 ERROR_CODE)
  "message": "요청 성공",   // 응답 메시지
  "data": { ... }          // 실제 데이터 (없을 경우 null)
}
```

## 관련 문서
- [**ENDPOINT**](https://affine.wypark.me/workspace/f85df0c4-a315-4166-94a8-6558cdafff1d/p13joh_beKNJ6R-I9KrFW)  
- [**API 명세서**](https://affine.wypark.me/workspace/f85df0c4-a315-4166-94a8-6558cdafff1d/7fRNXK9utxG4mcekWivGK)