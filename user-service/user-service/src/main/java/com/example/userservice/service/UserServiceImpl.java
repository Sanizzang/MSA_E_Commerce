package com.example.userservice.service;

import com.example.userservice.client.OrderServiceClient;
import com.example.userservice.dto.UserDto;
import com.example.userservice.jpa.UserEntity;
import com.example.userservice.jpa.UserRepository;
import com.example.userservice.vo.ResponseOrder;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    UserRepository userRepository;
    BCryptPasswordEncoder passwordEncoder;

    Environment env;
    RestTemplate restTemplate;

    // Feign Client 사용을 위한 의존성 주입
    OrderServiceClient orderServiceClient;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           BCryptPasswordEncoder passwordEncoder,
                           Environment env,
                           RestTemplate restTemplate,
                           OrderServiceClient orderServiceClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.env = env;
        this.restTemplate = restTemplate;
        this.orderServiceClient = orderServiceClient;
    }

    // UserDetailsService를 상속받아서 재정의 해줘야함
    // email을 가지고 사용자를 찾아오는 메서드
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(username);

        // 해당하는 사용자가 없다면
        if(userEntity == null)
            // spring security-core에 사용자 정보를 담을 수 있는 UsernamePasswordAuthenticationToken이 있음
            // 마찬가지로 사용자 검색이 안되었을 때, 발생할 수 있는 예외 클래스도 제공
            throw new UsernameNotFoundException(username);

        // User: Spring Security에서 제공해주는 User 모델
        return new User(userEntity.getEmail(), userEntity.getEncryptedPwd(), true, true, true, true, new ArrayList<>());
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        userDto.setUserId(UUID.randomUUID().toString());

        ModelMapper mapper = new ModelMapper();
        // modelMapper가 변경시킬 수 있는 환경설정정보 설정
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserEntity userEntity = mapper.map(userDto, UserEntity.class);
        userEntity.setEncryptedPwd(passwordEncoder.encode(userDto.getPwd()));

        userRepository.save(userEntity);

        UserDto returnUserDto = mapper.map(userEntity, UserDto.class);

        return returnUserDto;
    }

    @Override
    public UserDto getUserByUserId(String userId) {
        // 사용자 정보를 나타내는 JPA 엔티티
        // JPA repository를 사용하여 사용자 ID를 기반으로 사용자 정보를 가져옴
        UserEntity userEntity = userRepository.findByUserId(userId);
        
        if (userEntity == null)
            throw new UsernameNotFoundException("User not found");

        // userEntity 객체를 UserDto 객체로 매핑하여 반환
        UserDto userDto = new ModelMapper().map(userEntity, UserDto.class);

//        List<ResponseOrder> orders = new ArrayList<>();

        /* Using as RestTemplate */
        // 주문 서비스의 엔드포인트 URL을 동적으로 생성
        // URL의 일부로 사용되는 userId는 메소드의 매개변수로 전달
        // 환경 변수에서 order_service.url 키에 해당하는 값을 가져와 URL 템플릿의 %s 위체에 대체
//        String orderUrl = String.format(env.getProperty("order_service.url"), userId);
//        ResponseEntity<List<ResponseOrder>> orderListResponse =
//                // restTemplate.exchange RestTemplate 클래스를 사용하여 HTTP 요청을 보내고, 응답을 받아옴
//                // ParameterizedTypeReference<List<Response>>()는 RestTemplate에서 제공하는 제네릭 타입을 사용하는 방법으로 List<ReponseOrder> 형식의 응답을 받기 위해 사용된다.
//                restTemplate.exchange(orderUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<ResponseOrder>>() {
//        });
//
//        List<ResponseOrder> orderList = orderListResponse.getBody();

        /*
            FeignClient -> HTTP Client
            - REST Call을 추상화 한 Spring Cloud Netflix 라이브러리

            사용법
            - 호출하려는 HTTP Endpoint에 대한 Interface를 생성
            - @FeignClient 선언

            Load balanced 지원
         */

        /* Using feign client */
        /* Feign exception handling */

//        List<ResponseOrder> orderList = null;
//
//        try{
//            orderList = orderServiceClient.getOrders(userId);
//        } catch(FeignException ex) {
//            log.error(ex.getMessage());
//        }

        /* ErrorDecoder 사용 */
        List<ResponseOrder> orderList = orderServiceClient.getOrders(userId);
        userDto.setOrders(orderList);

        return userDto;
    }

    @Override
    public Iterable<UserEntity> getUserByAll() {
        return userRepository.findAll();
    }

    @Override
    public UserDto getUserDetailsByEmails(String email) {
        UserEntity userEntity = userRepository.findByEmail(email);

        if (userEntity == null)
            throw new UsernameNotFoundException(email);

        UserDto userDto = new ModelMapper().map(userEntity, UserDto.class);
        return userDto;
    }
}
