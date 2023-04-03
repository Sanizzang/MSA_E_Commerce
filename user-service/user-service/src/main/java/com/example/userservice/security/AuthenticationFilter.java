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

// Spring Security를 이용한 로그인 요청 발생 시 작업을 처리해 주는 Custom Filter 클래스
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

    // 사용자가 로그인 시도시 실행
    // 사용자가 전달한 로그인 정보를 인증
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        try {
            // 클라이언트에서 전송한 로그인 정보를 읽어옴
            // ObjectMapper: JSON 데이터를 자바 객체로 변환하는 Jackson 라이브러리 클래스
            // reqeust.getInputStream(): HTTP 요청 바디(body)에 포함된 데이터를 읽어오기 위한 메서드
            RequestLogin creds = new ObjectMapper().readValue(request.getInputStream(), RequestLogin.class);

            // getAuthenticationManager: 인증 처리 메서드
            return getAuthenticationManager().authenticate(
                    // 사용자가 입력했던 email과 id 값을 spring security에서 사용할 수 있는 형태의 값으로 변환하기 위해서
                    // UsernamePasswordAuthenticationToken 형태로 바꿔줄 필요가 있다
                    new UsernamePasswordAuthenticationToken(
                            creds.getEmail(),
                            creds.getPassword(),
                            // 권한과 관련된 값
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
        // (User) authResult.getPrincipal()를 통해 사용자 정보를 가져온 뒤 getUsername()을 통해 사용자의 이메일 정보를 가져온다.
        String username = ((User) authResult.getPrincipal()).getUsername();
        // 여기서는 email로 토큰 값을 만들 것이 아니라 userId를 통해 토큰을 만들 것이기 때문에 email을 통해 사용자 정보를 가져온다.
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

        // 응답헤더에 token과 userId 추가
        response.addHeader("token", token);
        // userId를 반환시켜주는 이유는 우리가 가지고 있는 token과 userId가 동일한지 확인하기 위함
        response.addHeader("userId", userDetails.getUserId());
    }
}

/*
로그인 처리 과정

사용자가 email, password 입력
{
  "email": "tksk@google.com",
  "password": "test"
}

-> AuthenticationFilter에 전달
attemptAuthentication()이 이 데이터를 처리

-> 입력되어진 email, password 값을 UsernamePasswordAuthenticaitonToken 값으로 바꿔서 사용

-> UserDetailService를 구현하고 있는 클래스(UserServiceImpl)에 loadUserByUsername() 실행
UserRepository의 findByEmail()을 통해 데이터베이스에서 email을 통해 사용자 데이터(UserEntity)를 가져옴
그 값을 Spring Security의 UserDetails에 있는 User라는 객체로 변경을해서 사용을 함

-> 마지막으로 모든 과정이 다 끝나서 정상적으로 로그인이 되어진걸로 확인되면 successfulAuthentication()에서
해당하는 값을 가지고 토큰을 발행을 해야하는데 사용자의 정보를 확인하기 위해서는
(User) authResult.getPrincipal()를 통해 사용자의 정보를 가져와야한다.

해당하는 이메일 정보를 토대로 사용자 정보를 불러와 userId를 가지고 토큰(JWT)을 만든다.

- JWT
  - 인증 헤더 내에서 사용되는 트큰 포맷
  - 두 개의 시스템끼리 안전한 방법으로 통신 가능
  - HEADER, PAYLOAD, VERIFY SIGNATURE로 이루어짐

- JWT(JSON Web Token) 장점
  - 클라이언트 독립적인 서비스(stateless)
  - CDN
  - No Cookie-Session (No CSRF, 사이트간 요청 위조)
  - 지속적인 토큰 저장장
*/