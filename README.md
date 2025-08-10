# Welcomer project
## 계획

<details>
<summary>change log</summary>

- 2025-08-08: 프로젝트 시작
- 2025-08-09: task-master, claude code로 kotlin spring 예제 만들기 시작
- 2025-08-10: kotlin-in-action, spring 공식 문서 학습
    - 책이 두꺼운 관계로 별도 일정 분리 후 진행
</details>

### 1. task-master, claude code로 실제 동작하는 kotlin spring 예제를 만들어 보기
- 기술 스택을 최대한 맞춰서 만들어보고, 동작/구성확인, 추후 참조를 할 수 있도록 한다.
- 기본적인 개념은 NestJS와 동일(NestJS가 따라 만듦)하므로 빠르게 익힌다.
- 기한: 2일

### 2. kotlin, spring 학습
- kotlin-in-action을 [본다](https://github.com/CrispyReader/kotlin-in-action).
- spring 공식 문서를 본다.
- 기한: 4일

### 3. welcomer 작업 시작
- OCI에서 무료로 사용할 수 있는 한계인 instance, mysql까지만 사용해서 간단한 프로젝트를 진행한다.
- 방명록 사이트로, 개인 사이트에 방문하는 사람들이 방명록을 남기면, 이를 알림의 형태로 발행하는 식이다.
- 구체적인 기획은 추후 PRD, 기능 명세서를 작성하여 공유한다.
- 기한: 4일

## 작업 진행 상황
### 1. 임시 구현 (진행중)
1. cucumber까지 해서 테스트 만들기
2. docker compose로 동작 시키기
3. 각각의 시스템이 어떻게 동작하는지 로그를 따라가며 확인하기
4. 모니터링 등은 어떻게 하는지 확인하기

### 2. welcomer 기획서 작성
1. PRD 작성
    1. task-master 사용 없이 직접 구현할 예정
    2. claude code 또한 사용하지 않을 예정
    3. 자동완성의 경우만 적극 활용
2. 기능 명세서 작성
    1. 기능 명세서에서 api 정의 까지 수행
3. 기능 구현
    1. cucumber를 통한 시나리오 테스트를 먼저 작성하여 ATDD 스타일을 일부 차용해 진행
    2. unit은 coverage 생각 말고 작성 (private method의 경우, 필요한게 아니면 테스트를 위해 노출하지 않기)

## 작업 참고 문서
### PRD
- change log
    1. 작성 예정
### 기능 명세서
- change log
    1. 작성 예정
