## database 설정
| 일자   | 결정 사항                    |
|------|--------------------------|
| 2025.08.19 | flyway로 데이터베이스 마이그레이션 관리 |
### 옵션
1. flyway로 데이터베이스 마이그레이션을 관리
    - 장점: 데이터베이스 스키마 변경을 코드와 함께 버전 관리할 수 있어, 팀원 간의 협업이 용이
    - 단점: 초기 설정이 복잡할 수 있으며, flyway 설정 파일을 추가로 관리해야 함
2. 매번 띄울때 transaction으로 스키마 반영
    - 장점: 간단하게 스키마를 반영할 수 있으며, flyway 설정 파일이 필요 없음
    - 단점: 데이터베이스 스키마 변경 이력을 관리하기 어려워, 팀원 간의 협업이 복잡해질 수 있음

### 선택 사항
1. flyway로 데이터베이스 마이그레이션을 관리
   - 실무에서 사용할 법한 기술 스택

## DB 스키마
| 일자   | 결정 사항             |
|------|-------------------|
| 2025.08.19 | messages table 결정 |
### 구조
```sql
CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    author VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 선택 사항
1. varchar(255)로 author 필드 설정
   - 나중에 사용자 추가할 예정인데, 이때 uuidv7로 author를 설정할 예정
2. id 필드 bigint로 설정
   - 나중에 메시지 수가 많아질 경우를 대비하여, bigint로 설정
3. content 필드 text로 설정
   - 메시지 내용이 길어질 수 있으므로, text로 설정
4. created_at, updated_at 필드 추가
   - created_at 필드는 메시지 작성 시 자동으로 현재 시간으로 설정
   - updated_at 필드는 메시지 작성 시 자동으로 현재 시간으로 설정, 이후 수정 시 직접 업데이트
       - 필요에 따라 메세지를 업데이트 하는 상황이 있을 것이라고 생각해 직접 업데이트 하도록 처리
   - timestamp로 설정하여, 날짜와 시간을 모두 저장
