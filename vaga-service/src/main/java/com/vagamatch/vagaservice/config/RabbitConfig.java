package com.vagamatch.vagaservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia de mensageria do vaga-service.
 *
 * vaga-service  --(vaga.criada)-->    [vagamatch.events] --> analise.vaga-criada --> analise-service
 * analise-service --(vaga.analisada)--> [vagamatch.events] --> vaga.vaga-analisada --> vaga-service
 *
 * Mensagens que falham vão pra uma DLQ em vez de se perderem.
 *
 * A fila analise.vaga-criada também é declarada aqui (com os mesmos argumentos do
 * analise-service): sem fila, o RabbitMQ descarta o que o vaga-service publica
 * enquanto o analise-service estiver fora do ar.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "vagamatch.events";
    public static final String DLX = "vagamatch.dlx";

    public static final String RK_VAGA_CRIADA = "vaga.criada";
    public static final String RK_VAGA_ANALISADA = "vaga.analisada";

    public static final String QUEUE_VAGA_CRIADA = "analise.vaga-criada";
    public static final String DLQ_VAGA_CRIADA = QUEUE_VAGA_CRIADA + ".dlq";

    public static final String QUEUE_VAGA_ANALISADA = "vaga.vaga-analisada";
    public static final String DLQ_VAGA_ANALISADA = QUEUE_VAGA_ANALISADA + ".dlq";

    @Bean
    TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    Queue vagaCriadaQueue() {
        return QueueBuilder.durable(QUEUE_VAGA_CRIADA)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ_VAGA_CRIADA)
                .build();
    }

    @Bean
    Queue vagaCriadaDlq() {
        return QueueBuilder.durable(DLQ_VAGA_CRIADA).build();
    }

    @Bean
    Binding vagaCriadaBinding() {
        return BindingBuilder.bind(vagaCriadaQueue()).to(eventsExchange()).with(RK_VAGA_CRIADA);
    }

    @Bean
    Binding vagaCriadaDlqBinding() {
        return BindingBuilder.bind(vagaCriadaDlq()).to(deadLetterExchange()).with(DLQ_VAGA_CRIADA);
    }

    @Bean
    Queue vagaAnalisadaQueue() {
        return QueueBuilder.durable(QUEUE_VAGA_ANALISADA)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ_VAGA_ANALISADA)
                .build();
    }

    @Bean
    Queue vagaAnalisadaDlq() {
        return QueueBuilder.durable(DLQ_VAGA_ANALISADA).build();
    }

    @Bean
    Binding vagaAnalisadaBinding() {
        return BindingBuilder.bind(vagaAnalisadaQueue()).to(eventsExchange()).with(RK_VAGA_ANALISADA);
    }

    @Bean
    Binding vagaAnalisadaDlqBinding() {
        return BindingBuilder.bind(vagaAnalisadaDlq()).to(deadLetterExchange()).with(DLQ_VAGA_ANALISADA);
    }

    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
