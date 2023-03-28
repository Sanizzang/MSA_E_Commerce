package com.example.userservice.security;

import com.example.userservice.dto.UserDto;
import com.example.userservice.service.UserService;
import com.example.userservice.vo.RequestLogin;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;

// 사용자 인증 필터를 구현
@Slf4j
public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private UserService userService;
    private Environment env;

    public AuthenticationFilter(AuthenticationManager authenticationManager) {
        super.setAuthenticationManager(authenticationManager);
    }

    public AuthenticationFilter(AuthenticationManager authenticationManager, UserService userService, Environment env) {
        super.setAuthenticationManager(authenticationManager);
        this.userService = userService;
        this.env = env;
    }

    // 사용자가 전달한 로그인 정보를 인증
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        try {
            // 클라이언트에서 정송한 로그인 정보를 읽어옴
            // ObjectMapper: JSON 데이터를 자바 객체로 변환하는 Jackson 라이브러리 클래스
            // reqeust.getInputStream(): HTTP 요청 바디(body)에 포함된 데이터를 읽어오기 위한 메서드
            RequestLogin creds = new ObjectMapper().readValue(request.getInputStream(), RequestLogin.class);

            // 인증 수행
            return getAuthenticationManager().authenticate(
                    // 사용자 이름과 비밀번호를 전달
                    new UsernamePasswordAuthenticationToken(
                            creds.getEmail(),
                            creds.getPassword(),
                            new ArrayList<>()));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    // 사용자 인증에 성공하면 호출되는 메서드
    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {

        // User: 인증에 성공한 사용자 정보를 담는 클래스
        String username = ((User) authResult.getPrincipal()).getUsername();
        UserDto userDetails = userService.getUserDetailsByEmails(username);

        // Key: JWT 토큰 서명에 사용되는 비밀 키
        // Key.hmacShaKeyFor: Key 객체를 생성하는 유틸리티 메서드
        Key secretKey = Keys.hmacShaKeyFor(env.getProperty("token.secret").getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                // JWT 토큰의 subject를 설정
                .setSubject(userDetails.getUserId())
                // JWT 토큰의 만료 시간 설정(현재 시간 + token.expiration_time 값)
                .setExpiration(new Date(System.currentTimeMillis() + Long.parseLong(env.getProperty("token.expiration_time"))))
                // JWT 토큰에 서명 추가
                .signWith(secretKey, SignatureAlgorithm.HS512)
                // JWT 토큰을 문자열로 변환
                .compact();
        // 응답헤더에 token과 userId 추간
        response.addHeader("token", token);
        response.addHeader("userId", userDetails.getUserId());
    }
}