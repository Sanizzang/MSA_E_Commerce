package com.example.catalogservice.messagequeue;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

/*
Catalogs Service에서 Kafka Topic에 전송 된 메시지 취득 -> Consumer
 */

// KafkaListener 빈을 생성하기 위한 기능을 활성화
@EnableKafka
@Configuration
public class KafkaConsumerConfig {
    // Kafka Consumer를 생성하기 위한 팩토리 메서드
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> properties = new HashMap<>();
        // 사용하고자 하는 Kafka 서버의 주소
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        /*
            데이터를 지정을 해줄 때 Topic에 저장되는 값 자체가 어떠한 형태로 되어있는가를 지정할 수 있다.
            JSON 형식의 포맷이기 때문에 Key 값과 Value가 한 세트가되서 저장이 된다.
            Key, Value 한 세트가 저장되어 있을 때, 그 값을 가져와서
            역으로 해석을해서 사용을 해야한다. -> DESERIALIZER 타입 지정
            데이터를 하나 만들어서 다른쪽으로 전달하는 용도로써 압축하는 과정을 SERIALIZER라고 가정하면
            다시 원래의 형태로 풀어서 쓰기 위한 가정을 DESERIALIZER라고 보면 된다.
         */
        // Consumer가 속한 Consumer Group 설정
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "127.0.0.1:9092");
        // Consumer가 읽어들이는 데이터의 key와 value의 직렬화 방법을 지정
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(properties);
    }

    // Kafka Listener(Topic에 변경사항이 있는지 Listening)를 생성하기 위한 팩토리 메서드 정의
    // 만약 Topic에 변경사항이 생기면 해당하는 값을 바로 캐치
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory
                = new ConcurrentKafkaListenerContainerFactory<>();
        kafkaListenerContainerFactory.setConsumerFactory(consumerFactory());

        return kafkaListenerContainerFactory;
    }
}
