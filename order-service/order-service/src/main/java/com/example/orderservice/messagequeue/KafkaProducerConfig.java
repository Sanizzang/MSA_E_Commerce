package com.example.orderservice.messagequeue;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

/*
Order Service에서 Kafka Topic으로 메시지 전송 -> Producer
 */

@EnableKafka
@Configuration
public class KafkaProducerConfig {
    // Kafka Producer를 생성하기 위한 설정 정보를 포함하는 팩토리 클래스
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> properties = new HashMap<>();
        // 사용하고자하는 Kafka서버의 주소
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return new DefaultKafkaProducerFactory<>(properties);
    }

    // Kafka Producer를 템플릿으로 사용하여 Kafka 메시지를 보내는 데 사용
    @Bean
    public KafkaTemplate<String, String> keyTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
