# MSA_E_Commerce
Spring Cloud로 개발하는 이커머스 마이크로서비스 애플리케이션

## 애플리케이션 구성 요소

|구성요소|설명|
|------|---|
|Git Repository|마이크로서비스 소스 관리 및 프로파일 관리|
|Config Server|Git 저장소에 등록된 프로파일 정보 및 설정 정보|
|Eureka Server|마이크로서비스 등록 및 검색|
|API Gateway Server|마이크로서비스 부하 분산 및 서비스 라우팅|
|Microservices|회원 MS, 주문 MS, 상품(카테고리) MS|
|Queuing System|마이크로서비스 간 메시지 발행 및 구독|

## 애플리케이션 API 목록

|마이크로서비스|RESTful API|HTTP Method|
|------|---|---|
|Catalog Service|/catalog-service/catalogs : 상품 목록 제공|GET|
|User Service|/user-service/users : 사용자 정보 등록<br>/user-service/users : 전체 사용자 조회<br>/user-service/users/{user_id} : 사용자 정보, 주문 내역 조회|POST<br>GET<br>GET|
|Order Service|/order-service/users/{user_id}/orders : 주문 등록<br>/order-service/users/{user_id}/orders : 주문 확인|POST<br>GET|
