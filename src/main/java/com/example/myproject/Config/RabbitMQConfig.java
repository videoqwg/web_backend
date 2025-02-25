package com.example.myproject.Config;


import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String INSTRUCTION_EXCHANGE = "multi-user-instruction-exchange";

    public static final String GROUP_EXCHANGE = "multi-group-exchange";

    /**
     * 声明一个 TopicExchange，供所有用户的队列动态绑定
     */
    @Bean
    public TopicExchange instructionExchange() {
        return ExchangeBuilder.topicExchange(INSTRUCTION_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange groupExchange() {
        return ExchangeBuilder
                .topicExchange(GROUP_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 用于动态声明队列、Exchange、Binding
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * 使用 Jackson2JsonMessageConverter 来序列化/反序列化
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
