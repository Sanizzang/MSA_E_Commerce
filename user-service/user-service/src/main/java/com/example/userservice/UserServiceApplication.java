package com.example.userservice;

import com.example.userservice.error.FeignErrorDecoder;
import feign.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
// Spring Cloud Discovery Client를 사용하여 서비스 인스턴스를 등록하기 위해 사용
@EnableDiscoveryClient
// Feign Client를 사용하기 위해 사용. (HTTP API를 쉽게 작성하고 호출하기 위한 라이브러리)
@EnableFeignClients
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

	// Spring Security에서 제공하는 암호화 도구
	// 랜덤 Salt를 부여하여 여러번 Hash를 적용한 암호화 방식
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

//	// Spring Security에서 인증을 수행하는데 사용됨
//	@Bean
//	AuthenticationManager authenticationManager(AuthenticationConfiguration authConfiguration) throws Exception {
//		return authConfiguration.getAuthenticationManager();
//	}


	@Bean
	// RestTemplate에서 Load Balancing을 사용하기 위해 사용됨
	// -> 불러오고 싶은 API의 주소를 Microservice 이름으로 가져올 수 있음
	// ex) http://127.0.0.1:8000/order-service/%s/orders
	// -> http://order-service/order-service/%s/orders
	@LoadBalanced
	public RestTemplate getRestTemplate() {
		return new RestTemplate();
	}

	// Feign Client의 로그레벨 사용
	@Bean
	public Logger.Level feignLoggerLevel() {
		return Logger.Level.FULL;
	}

	// Feign Client에서 오류를 처리하기 위한 사용자 정의 오류 디코더
//	@Bean
//	public FeignErrorDecoder getFeignErrorDecoder() {
//		return new FeignErrorDecoder();
//	}
}
