package com.example.demo.service.RabbitMQ;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;
    @Value("${app.rabbitmq.routingKey}")
    private String routingKey;

    public RabbitMQProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(String shortKey) {
        rabbitTemplate.convertAndSend(exchange, routingKey, shortKey);
        log.info("Sent message to RabbitMQ: {}", shortKey);
    }

}
