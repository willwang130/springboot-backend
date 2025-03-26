package com.example.demo.service.RabbitMQ;

import com.example.demo.util.RedisUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class RabbitMQConsumer {

    private final RedisUtil redisUtil;

    public RabbitMQConsumer(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue}", ackMode = "MANUAL", containerFactory = "rabbitListenerContainerFactory")
    public void receiveMessage(
            Message message, Channel channel) {

            long tag  = message.getMessageProperties().getDeliveryTag(); // 获取唯一 deliverTag
            String shortKey = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            log.info("Consumer received message from queue:{}", shortKey);

            redisUtil.pushToRedisListBuffer("short_url_buffer",shortKey);
            // 1.tag 2. multiple=false->只确认当前信息 true->确认比tag小的未确认信息
            channel.basicAck(tag, false);
        } catch (Exception e) {

            log.error("RabbitMQ failed 消费失败: {}", e.getMessage());
            try {
                if(channel.isOpen()) {
                    // 1.tag 2.true=是否拒绝所有比tag小的确认消息 3.true=是否重新入队,否则丢掉或死信
                    channel.basicNack(tag, false, true);
                }
            } catch (IOException ioException) {
                log.error("RabbitMQ basicNack 失败:{}", ioException.getMessage());
            }
        }
    }

}
