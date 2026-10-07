package com.vagamatch.analiseservice.config;

import com.vagamatch.analiseservice.exception.AnaliseException;
import com.vagamatch.analiseservice.exception.GeminiPermanenteException;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.boot.retry.RetryPolicySettings;
import org.springframework.core.retry.RetryPolicy;

import static org.assertj.core.api.Assertions.assertThat;

/** A policy de retry que o Spring Boot monta pro listener, já com o nosso customizer aplicado. */
class RabbitConfigTest {

    private final RetryPolicy policy = policyDoListener();

    private static RetryPolicy policyDoListener() {
        RetryPolicySettings settings = new RetryPolicySettings();
        new RabbitConfig().naoRetentarErroPermanente().customize(settings);
        return settings.createRetryPolicy();
    }

    /** O container entrega a exceção pro retry embrulhada. */
    private static Throwable doListener(Throwable causa) {
        return new ListenerExecutionFailedException("Listener threw exception", causa);
    }

    @Test
    void erroPermanenteNaoRetenta() {
        assertThat(policy.shouldRetry(doListener(new GeminiPermanenteException("401")))).isFalse();
    }

    @Test
    void erroPermanenteMaisFundoNaCadeiaTambemNaoRetenta() {
        Throwable erro = doListener(new IllegalStateException("x", new GeminiPermanenteException("chave")));
        assertThat(policy.shouldRetry(erro)).isFalse();
    }

    @Test
    void erroTransitorioRetenta() {
        assertThat(policy.shouldRetry(doListener(new AnaliseException("503 em todos os modelos")))).isTrue();
        assertThat(policy.shouldRetry(doListener(new RuntimeException("qualquer outra")))).isTrue();
    }
}
