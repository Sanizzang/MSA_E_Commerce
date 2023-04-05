package com.example.orderservice.messagequeue;

import com.example.orderservice.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/*
{
  "shema":{"
    type":"struct",
    "fields":[
      {"type":"string","optional":true,"field":"order_id"},
      {"type":"string","optional":true,"field":"user_id"},
      {"type":"string","optional":true,"field":"product_id"},
      {"type":"int32","optional":true,"field":"qty_id"},
      {"type":"int32","optional":true,"field":"total_price"},
      {"type":"int32","optional":true,"field":"unit_price"},
    ],
    "optional":false,
    "name":"orders"
  },
  "payload":{
    "order_id":"asdjflasjdf;lasjdlfjas;dfj",
    "user_id":"a;ldfj;asldjf;lksdjfldsj",
    "product_id":"CATALOG-001",
    "qty":5,
    "total_price":6000,
    "unit_price":1200
  }
}
 */

// Kafka Producer
// Kafka Template을 사용하여 메시지를 Kafka에 보내는 역할
@Service
@Slf4j
public class OrderProducer {
    // Kafka에서 제공하는 메시지 전송을 쉽게 해주는 템플릿
    // 첫 번째 제네릭 타입: Key의 타입
    // 두 번째 제네릭 타입: Value의 타입
    private KafkaTemplate<String, String> kafkaTemplate;

    // Kafka의 Schema에 들어갈 필드를 정의
    List<Field> fields = Arrays.asList(new Field("stirng", true, "order_id"),
            new Field("string", true, "user_id"),
            new Field("string", true, "product_id"),
            new Field("int32", true, "qty"),
            new Field("int32", true, "unit_price"),
            new Field("int32", true, "total_price"));

    // Kafka 메시지에서 사용될 스키마를 정의
    Schema schema = Schema.builder()
            .type("struct")
            .fields(fields)
            .optional(false)
            .name("orders")
            .build();

    @Autowired

    public OrderProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Kafka에 메세지를 전송하는 메소드
    // 전송할 Topic과 OrderDto 객체를 인자로 받아 KafkaOrderDto 객체를 생성
    // 이후 KafkaTemplate의 send() 메소드를 사용하여 메시지를 전송
    public KafkaOrderDto send(String topic, OrderDto orderDto) {
        Payload payload = Payload.builder()
                .order_id(orderDto.getOrderId())
                .user_id(orderDto.getUserId())
                .product_id(orderDto.getProductId())
                .qty(orderDto.getQty())
                .unit_price(orderDto.getUnitPrice())
                .total_price(orderDto.getTotalPrice())
                .build();

        KafkaOrderDto kafkaOrderDto = new KafkaOrderDto(schema, payload);

        // ObjectMapper 객체를 사용하여 KafkaOrderDto 객체를 JSON 문자열로 변환
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = "";
        try {
            jsonInString = mapper.writeValueAsString(kafkaOrderDto);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
        }

        kafkaTemplate.send(topic, jsonInString);
        log.info("Order Producer sent data from the Order microservice: " + kafkaOrderDto);

        return kafkaOrderDto;
    }
}
