package com.example.userservice.security;

import com.example.userservice.service.UserService;
import com.example.userservice.service.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    // 사용자 데이터 제공
    private final UserService userService;
    // 패스워드 인코딩
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    // 환경 변수 제공
    private final Environment env;

    // WebSecurityConfigurerAdapter 클래스가 deprecated 되면서,
    // config 메소드를 구현하는 대신 SecurityFilterChain을 반환하고 직접 Bean으로 등록하도록 설정 방법이 바뀜
    // 권한과 관련한 메서드
    // HttpSecurity: HTTP 요청에 대한 보안 구성 지정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // 사용자 인증 매니저, 인증 매니저는 사용자의 인증 정보를 확인
        AuthenticationManager authenticationManager = getAuthenticationFilter(http);

        http.csrf().disable();
        // 요청을 승인하는 방법 지정
        // permitAll(): 모든 사용자가 요청을 수행할 수 있도록 함
        // hasIpAddress(): 해당 IP 주소에서만 요청을 수행할 수 있도록 함
//        http.authorizeRequests().antMatchers("/users/**").permitAll();
        http.authorizeRequests()
                .antMatchers("/actuator/**").permitAll() // actuator permitAll
                .antMatchers("/error/**").permitAll()
//                .antMatchers("/**").hasIpAddress("172.17.110.148")
                .antMatchers("/**").permitAll()
                .and()
                .authenticationManager(authenticationManager)
                // 이 필터를 통과시킨 데이터에 한에서만 권한을 부여하고 작업을 진행하겠다.
                .addFilter(getAuthenticationFilter(authenticationManager));

        // HTTP 응답 헤더 구성
        // frame load 하게 해줌
        // 해당 코드를 추가하지 않으면 H2가 실행이 안됨
        http.headers().frameOptions().disable();

        return http.build();
    }

    // 인증 매니저 생성 메서드
    private AuthenticationManager getAuthenticationFilter(HttpSecurity http) throws Exception {
        // 인증 매니저를 구성하는 빌더 클래스
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        // userDetailService: 사용자 인증 정보를 검색할 때 사용하는 서비스 (userService)
        // passwordEncoder: 패스워드 인코딩을 위해 사용
        authenticationManagerBuilder.userDetailsService(userService).passwordEncoder(bCryptPasswordEncoder);
        return authenticationManagerBuilder.build();
    }

    // 사용자 인증을 처리하는 필터(우리가 정의한 CustomFilter). 사용자 이름과 비밀번호를 인증
    private AuthenticationFilter getAuthenticationFilter(AuthenticationManager authenticationManager) {
        return new AuthenticationFilter(authenticationManager, userService, env);
    }


}
