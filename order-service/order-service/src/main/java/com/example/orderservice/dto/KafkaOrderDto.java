package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/*
Kafka의 메시지를 직렬화하고 역직렬화하기 위한 클래스
 */
@Data
@AllArgsConstructor
public class KafkaOrderDto implements Serializable {
    // 메시지의 스키마 정보
    private Schema schema;
    // 실제 메시지의 데이터
    private Payload payload;
}
