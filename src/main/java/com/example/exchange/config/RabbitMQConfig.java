package com.example.exchange.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String AUDIT_EXCHANGE = "exchange-audit-exchange";
    public static final String AUDIT_DLX = "exchange-audit-dlx";

    public static final String AUDIT_COMPLETED_QUEUE = "exchange-audit-completed-queue";
    public static final String AUDIT_FAILED_QUEUE = "exchange-audit-failed-queue";
    public static final String AUDIT_DLQ = "exchange-audit-dlq";

    public static final String AUDIT_COMPLETED_ROUTING_KEY = "audit.completed";
    public static final String AUDIT_FAILED_ROUTING_KEY = "audit.failed";
    public static final String AUDIT_DLQ_ROUTING_KEY = "audit.dead";

    @Bean
    public Queue auditCompletedQueue() {
        return QueueBuilder.durable(AUDIT_COMPLETED_QUEUE)
                .withArgument("x-dead-letter-exchange", AUDIT_DLX)
                .withArgument("x-dead-letter-routing-key", AUDIT_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue auditFailedQueue() {
        return QueueBuilder.durable(AUDIT_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", AUDIT_DLX)
                .withArgument("x-dead-letter-routing-key", AUDIT_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue auditDeadLetterQueue() {
        return QueueBuilder.durable(AUDIT_DLQ).build();
    }

    @Bean
    public TopicExchange auditExchange() {
        return new TopicExchange(AUDIT_EXCHANGE);
    }

    @Bean
    public DirectExchange auditDeadLetterExchange() {
        return new DirectExchange(AUDIT_DLX);
    }

    @Bean
    public Binding auditCompletedBinding(Queue auditCompletedQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(auditCompletedQueue).to(auditExchange).with(AUDIT_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Binding auditFailedBinding(Queue auditFailedQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(auditFailedQueue).to(auditExchange).with(AUDIT_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding auditDeadLetterBinding(Queue auditDeadLetterQueue, DirectExchange auditDeadLetterExchange) {
        return BindingBuilder.bind(auditDeadLetterQueue).to(auditDeadLetterExchange).with(AUDIT_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
