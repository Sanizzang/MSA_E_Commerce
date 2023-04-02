package com.example.apigatewayservice.filter;

import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;

/*
HTTP 요청에 포함된 Authorization Header를 검증하는 필터.
만약 Authorization Header가 없거나, JWT 토큰이 유효하지 않으면 HTTP 응답으로 UNAUTHORIZED 상태 코드를 반환한다.
1. HTTP 요청에서 Authorization Header를 가져옴.
2. Authorization Header에 JWT 토큰이 포함되어 있는지 확인하고, JWT 토큰이 없으면 UNAUTHORIZED 상태 코드를 반환
3. JWT 토큰이 포함되어 있으면, JWT 토큰이 유효한지 검증하고, 유효하지 않으면 UNAUTHORIZED 상태 코드를 반환
4. JWT 토큰이 유효하면, 필터 체인을 계속 진행한다.
 */
@Component
@Slf4j
// Custom Filter는 AbstractGatewayFilterFactory를 상속 받아야 한다.
// Configuration 정보가 있다면 자신의 클래스 안에서 Config라는 내부클래스를 매개변수로 등록한다.
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {
    Environment env;

    public AuthorizationHeaderFilter(Environment env) {
        super(Config.class);
        this.env = env;
    }

    // Inner Class로써 설정과 관련되어 있는 작업을 전달하기 위한 Config 등록
    public static class Config {

    }

    // login -> token -> users (with token) -> header(include token)
    // CustomFilter 구현을 위한 apply 메소드 구현
    @Override
    public GatewayFilter apply(Config config) {
        // exchange: GatewayFilter에서 제공하는 ServerWebExchange 객체 (Spring WebFlux에서 사용). HTTP 요청 및 응답을 나태냄
        // chain: 필터 체인을 나타내는 객체
        return (exchange, chain) -> {
            // Pre Filter
            // spring cloud gateway는 비동식 방식(Netty)이기 때문에 ServerHttpRequest라는 객체를 사용한다.(ServletRequest가 아님)
            ServerHttpRequest request = exchange.getRequest();

            // HTTP 요청의 Authorization 헤더가 있는지 확인
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                // 만약 헤더가 없다면 onError() 메서드를 호출하여 HTTP 401 Unauthorized 상태코드와 함께 오류 메시지 반환
                return onError(exchange, "No authorization header", HttpStatus.UNAUTHORIZED);
            }

            // HTTP 요청 헤더에서 Authorization 필드에 해당하는 값을 가져옴
            String authorizationHeader = request.getHeaders().get(org.springframework.http.HttpHeaders.AUTHORIZATION).get(0);
            // authorizationHeader 문자열에 Bearer 문자열을 제거하여 jwt 변수에 할당
            String jwt = authorizationHeader.replace("Bearer", "");

            // JWT 토큰의 서명을 검증하고, 만료 시간을 확인하여 JWT 토큰이 유효한지 검증
            if (!isJwtValid(jwt)) {
                return onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED);
            }

            // Post Filter
            return chain.filter(exchange);
        };
    }

    // jwt 문자열을 파싱하여 JWT 토큰이 유효한지 검증
    private boolean isJwtValid(String jwt) {
        boolean returnValue = true;
        String subject = null;

        try {
            // JWT 토큰 서명에 사용될 비밀 키 생성. 이때, 비밀 키는 애플리케이션에서 미리 설정한 token.secret 값으로부터 생성되며,
            // hmacShaKeyFor 메서드는 HMAC-SHA 알고리즘을 사용하여 비밀 키를 생성
            Key secretKey = Keys.hmacShaKeyFor(env.getProperty("token.secret").getBytes(StandardCharsets.UTF_8));
            // JWT 토큰을 파싱하기 위한 빌더 객체 생성
            JwtParserBuilder jwtParserBuilder = Jwts.parserBuilder();
            // JWT 토큰에 사용될 서명 키 설정
            JwtParserBuilder jwtParserBuilder1 = jwtParserBuilder.setSigningKey(secretKey);
            subject = jwtParserBuilder1
                    .build()
//                    .parseClaimsJwt(jwt)
                    // 파싱 대상 JWT 토큰을 Jws(JWT Signature를 포함하는 객체) 객체로 파싱
                    .parseClaimsJws(jwt)
                    // 파싱된 JWT 내용을 가져옴
                    .getBody()
                    // JWT의 subject 값을 가져옴
                    .getSubject();
        } catch (Exception ex) {
            log.error("jwtParser = {}", ex.getMessage());
            returnValue = false;
        }

        // JWT subject 값이 존재하지 않거나 빈 문자열인 경우, 해당 JWT는 유효하지 않은 것으로 판단하고 false를 반환
        if (subject == null || subject.isEmpty()) {
            returnValue = false;
        }

        return returnValue;
    }

    // ServerWebExchange: Spring WebFlux에서 사용되는 HTTP 요청 및 응답 객체
    // err: 오류 메시지
    // httpStatus: HTTP 상태 코드
    // Mono -> WebFlux라고 해서 Spring 5 부터 추가된 기능
    // 기존에 동기화 방식의 서버가 아니라 비동기 방식의 서버를 지원할 때 단일값 전달할 때 사용
    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        // ServerWebExchange 객체에서 ServerHttpResponse 객체를 가져옴
        ServerHttpResponse response = exchange.getResponse();
        // 상태코드 설정
        response.setStatusCode(httpStatus);

        log.error(err);
        // 응답 반환
        return response.setComplete();
    }
}
