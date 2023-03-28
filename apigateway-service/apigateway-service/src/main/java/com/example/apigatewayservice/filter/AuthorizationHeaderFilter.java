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

@Component
@Slf4j
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {
    Environment env;

    public AuthorizationHeaderFilter(Environment env) {
        this.env = env;
    }

    // Inner Class로써 설정과 관련되어 있는 작업을 전달하기 위한 Config 등록
    public static class Config {

    }

    // login -> token -> users (with token) -> header(include token)
    @Override
    public GatewayFilter apply(Config config) {
        // exchange: GatewayFilter에서 제공하는 ServerWebExchange 객체. HTTP 요청 및 응답을 나태냄
        // chain: 필터 체인을 나타내는 객체
        return (((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "No authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authorizationHeader = request.getHeaders().get(org.springframework.http.HttpHeaders.AUTHORIZATION).get(0);
            String jwt = authorizationHeader.replace("Bearer", "");

            if (!isJwtValid(jwt)) {
                return onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED);
            }

            return chain.filter(exchange);
        }));
    }

    // jwt 문자열을 파싱하여 JWT 토큰이 유효한지 검증
    private boolean isJwtValid(String jwt) {
        boolean returnValue = true;
        String subject = null;

        try {
            // JWT 토큰 서명에 사용되는 비밀 키
            Key secretKey = Keys.hmacShaKeyFor(env.getProperty("token.secret").getBytes(StandardCharsets.UTF_8));
            // JWT 토큰을 파싱하기 위한 빌더 객체 생성
            JwtParserBuilder jwtParserBuilder = Jwts.parserBuilder();
            // JWT 토큰에 사용될 서명 키 설정
            JwtParserBuilder jwtParserBuilder1 = jwtParserBuilder.setSigningKey(secretKey);
            subject = jwtParserBuilder1
                    .build()
//                    .parseClaimsJwt(jwt)
                    .parseClaimsJws(jwt)
                    .getBody()
                    .getSubject();
        } catch (Exception ex) {
            log.error("jwtParser = {}", ex.getMessage());
            returnValue = false;
        }

        if (subject == null || subject.isEmpty()) {
            returnValue = false;
        }

        return returnValue;
    }

    // ServerWebExchange: Spring WebFlux에서 사용되는 HTTP 요청 및 응답 객체
    // err: 오류 메시지
    // httpStatus: HTTP 상태 코드
    // Mono, Flux -> Spring WebFlux
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
