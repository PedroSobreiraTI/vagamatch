package com.vagamatch.analiseservice.config;

import com.vagamatch.analiseservice.exception.GeminiPermanenteException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitListenerRetrySettingsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia de mensageria do analise-service.
 * Consome vaga.criada e publica vaga.analisada. Falha após o retry vai pra DLQ (FalhaAnaliseRecoverer).
 * Erro permanente do Gemini (GeminiPermanenteException) pula o retry e vai direto pro recoverer.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "vagamatch.events";
    public static final String DLX = "vagamatch.dlx";

    public static final String RK_VAGA_CRIADA = "vaga.criada";
    public static final String RK_VAGA_ANALISADA = "vaga.analisada";

    public static final String QUEUE_VAGA_CRIADA = "analise.vaga-criada";
    public static final String DLQ_VAGA_CRIADA = QUEUE_VAGA_CRIADA + ".dlq";

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
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * O retry do listener (retry.* no application.yml) vale só pra erro transitório.
     * Quando o predicate recusa, o RetryTemplate do Spring 7 desiste na hora e o recoverer entra.
     */
    @Bean
    RabbitListenerRetrySettingsCustomizer naoRetentarErroPermanente() {
        return settings -> settings.setExceptionPredicate(RabbitConfig::deveRetentar);
    }

    /** A exceção chega embrulhada (ListenerExecutionFailedException), então procura na cadeia de causas. */
    static boolean deveRetentar(Throwable erro) {
        for (Throwable atual = erro; atual != null; atual = atual.getCause()) {
            if (atual instanceof GeminiPermanenteException) {
                return false;
            }
        }
        return true;
    }
}
