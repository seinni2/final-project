# 등산 크루 모집 커뮤니티 ‘올라!’
스프링 프로젝트 ‘올라’ 입니다. 생활 커뮤니티 서비스로, 등산과 관련된 관심사를 해시태그로 만들어 다양하게 공유할 수 있는 방법을 구현했습니다.

## 팀 구성
백엔드 5명(이재원, 한세인, 김두영, 김주성, 김민선)

<!-- [프로젝트 포트폴리오 상세 보기](http://jenyglee93.com/) -->
![리드미 대문 이미지](https://user-images.githubusercontent.com/86715916/222955463-95fad7f8-1959-46d1-b556-e550469bff0e.jpg)

구현 기술 스택
- Redis 캐싱을 이용한 로그아웃
- QueryDSL
- PresignedURL 이미지 업로드
- JWT Access/Refresh Token
- Websocket, Socket js, Stomp 
- OAuth2 카카오 로그인
- RDS Database(MySQL)
- Github Action CI/CD

<img width="793" alt="스크린샷 2023-03-07 오후 1 43 06" src="https://user-images.githubusercontent.com/86715916/223626203-d7098c7c-846f-413e-bcd8-af908cee1f44.png">

<img width="1355" alt="스크린샷 2023-03-07 오후 3 24 49" src="https://user-images.githubusercontent.com/86715916/223626267-a2f9762e-c314-43d4-9474-8b0f6336231c.png">


## 주요기능
1. 관심사가 비슷한 친구를 랜덤으로 추천
2. 검색조건과 해시태그를 이용한 필터링 조회
3. 실시간 채팅을 통한 크루원 모집
4. 자동 등업 시스템
5. 커뮤니티 게시글/댓글/좋아요
6. 등산 검색 쇼핑(네이버 쇼핑)
7. Admin 기능(회원, 커뮤니티, 코스추천, 공지사항 관리)

* * *

## 구현 이슈 정리

### 이미지
#### Presigned URL 연결하기
> Presigned URL 연결하기


### 게시물
#### 한방쿼리로 연관관계 싹 지우기
> 커뮤니티 프로젝트를 하면서 게시글/댓글/댓글좋아요까지 연관되어있는 상태에서 게시글을 삭제했을 때 그 안에 있는 많은 댓글과 댓글 안에 있는 더 많은 좋아요들을 전부 삭제해야 하는 상황에서, 쿼리최적화를 할 수 있는 방법을 찾아보았습니다. Spring Data JPA에서 제공하는 deleteByXXX 메소드는 기본적으로 삭제 대상을 조회하는 쿼리 1번, 삭제하는 1건이 실행되어 N+1에서 벗어날 수 없었습니다. Bulk Delete 방식을 사용하면 많은 댓글에 있는 더많은 좋아요들을 한방 쿼리로 싹 지워줄 수 있었습니다. 
> @Query("delete from CommentLike cl where cl.comment.Id in :commentIds")
> 위의 쿼리문을 작성하여 SQL의 In절을 이용해 수많은 댓글들의 ID 배열 안에 댓글좋아요의 댓글 ID가 포함되는 것을 한방에 싹 지워주었습니다.


#### 채팅방 구현하기

> 채팅방 구현하기 내용

#### 쿼리문을 썼더니 게시글이 증식한다!?(feat.distinct)

> 쿼리문을 썼더니 게시글이 증식한다! 내용


### 친구
#### N:N 관계에서 매칭시스템 
> N:N 관계에서 매칭시스템 내용
