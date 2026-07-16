---
name: 사양 제안 (Spec Proposal)
about: 새 요구사항이나 기능 사양을 제안한다 (Spec Kit /speckit-specify 진입점)
title: "[Spec] "
labels: spec
assignees: ''
---

## 배경 / 문제

<!-- 왜 이 기능이 필요한가? 어떤 사용자/판매자 문제를 푸는가? -->

## 제안하는 요구사항

<!-- 무엇을 만들고자 하는가? 사용자 관점에서 간결하게. -->

## 수락 기준 (Given / When / Then)

- Given ...
- When ...
- Then ...

## 헌장 관련 원칙 점검

<!-- 관련되는 원칙에 체크. 자세한 내용은 .specify/memory/constitution.md 참고 -->

- [ ] I. 테스트 우선 품질
- [ ] II. 결제·보안 신뢰성
- [ ] III. 데이터 무결성·트랜잭션 일관성
- [ ] IV. 관측 가능성·감사 추적성
- [ ] V. 단순성·계층형 아키텍처
- [ ] VI. 한글·직관적 표현
- [ ] VII. 마켓플레이스 정산·주문 생명주기
- [ ] VIII. 이슈 → 브랜치 → PR 워크플로

## 다음 단계

- [ ] 이 이슈 승인 후 `/speckit-specify`로 사양 문서 작성
- [ ] `feature/<이슈번호>-<슬러그>` 브랜치 생성
- [ ] 완료 시 PR로 `main` 병합 (`Closes #<이슈번호>`)
