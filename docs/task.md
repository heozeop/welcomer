# Welcomer

### change log
- 2025-08-19: 초안 작성

## 1. 개요
웹 페이지에 간단한 메세지를 남기고, 다른 사람의 메세지를 확인할 수 있는 기능

## 2. 목표
1. 메세지의 CRUD 기능
2. 데이터 무결성과 일관성 보장

## 3. 핵심 기능
1. 메세지 작성
   - 작성자, 내용, 작성일시
   - 작성자는 입력 받은 사용자 명
   - 유효성:
       - 작성자: 입력 받은 사용자 명, 필수, 일단 중복 허용
       - 내용: 1자 이상 100자 이하, 공백 불가
       - 작성일시: 현재 시간
2. 메세지 조회
   - 작성자, 내용, 작성일시
   - 작성일시 기준으로 내림차순 정렬
3. 메세지 수정
   - 작성자만 수정 가능
   - 내용 수정
4. 메세지 삭제
   - 작성자만 삭제 가능

## 4. 사용자 시나리오
1. 페이지 접속
2. 메세지 작성
3. 최신순 반영

## 5. 기술 스택 & 구조
### 기술 스택
- kotlin spring
- mysql
- exposed, jooq
- kotest
- docker compose
### 폴더 구조
- [폴더구조](./folder-structure.md)
- change log
  - 2025-08-19: 초안 작성

### 의사 결정
- [의사결정](./decisions.md)
- change log
  - 2025-08-19: 초안 작성, 데이터베이스 설정, DB 스키마 결정

## 6. API 명세
### 메세지 작성
- **URL**: `/api/messages`
- **Method**: `POST`
- **Request Body**:
```json
{
  "author": "사용자명",
  "content": "메세지 내용"
}   
```
- **Response**:
```json
{
  "id": 1,
  "author": "사용자명",
  "content": "메세지 내용",
  "createdAt": "2025-08-19T12:00:00Z"
}
```
### 메세지 조회
- **URL**: `/api/messages`
- **Method**: `GET`
- **Response**:
```json
[
  {
    "id": 1,
    "author": "사용자명",
    "content": "메세지 내용",
    "createdAt": "2025-08-19T12:00:00Z"
  },
  {
    "id": 2,
    "author": "다른 사용자명",
    "content": "다른 메세지 내용",
    "createdAt": "2025-08-19T12:01:00Z"
  }
]
```
### 메세지 수정
- **URL**: `/api/messages/{id}`
- **Method**: `PUT`
- **Request Body**:
```json
{
  "content": "수정된 메세지 내용"
}
```
- **Response**:
```json
{
    "id": 1,
    "author": "사용자명",
    "content": "수정된 메세지 내용",
    "createdAt": "2025-08-19T12:00:00Z"
}
```
### 메세지 삭제
- **URL**: `/api/messages/{id}`
- **Method**: `DELETE`
- **Response**: `204 No Content`

## 비기능 요구사항
- 성능
  - 페이지 로딩 시간: 2초 이내
  - 메세지 작성 후 반영 시간: 1초 이내
- 보안
  - XSS, CSRF 공격 방지
- 확장성
  - 메세지 수가 많아져도 성능 저하가 없도록 설계
- 유지보수성
  - 코드 가독성 및 주석 작성
- 테스트
  - 단위 테스트 및 통합 테스트 작성
- 문서화
  - API 문서화
