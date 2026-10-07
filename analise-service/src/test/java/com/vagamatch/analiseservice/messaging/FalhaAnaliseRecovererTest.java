package com.vagamatch.analiseservice.messaging;

import com.vagamatch.analiseservice.config.RabbitConfig;
import com.vagamatch.analiseservice.dto.VagaAnalisadaEvent;
import com.vagamatch.analiseservice.exception.GeminiPermanenteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FalhaAnaliseRecovererTest {

    @Mock
    private RabbitTemplate rabbitTemplate;
    private FalhaAnaliseRecoverer recoverer;

    @BeforeEach
    void setUp() {
        recoverer = new FalhaAnaliseRecoverer(rabbitTemplate, JsonMapper.builder().build());
    }

    private static Message mensagem(String corpo) {
        return new Message(corpo.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }

    private static Throwable doListener(Throwable causa) {
        return new ListenerExecutionFailedException("Listener threw exception", causa);
    }

    @Test
    void mandaPraDlqEAvisaOVagaServiceComOErroOriginal() {
        Message mensagem = mensagem("{\"vagaId\": 42, \"titulo\": \"Dev\", \"descricao\": \"...\"}");

        recoverer.recover(mensagem, doListener(new GeminiPermanenteException("Erro ao chamar o Gemini: 401")));

        verify(rabbitTemplate).send(eq(RabbitConfig.DLX), eq(RabbitConfig.DLQ_VAGA_CRIADA), eq(mensagem));
        verify(rabbitTemplate).convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_VAGA_ANALISADA,
                VagaAnalisadaEvent.falha(42L, "Erro ao chamar o Gemini: 401"));
    }

    @Test
    void cortaMensagemDeErroMuitoLonga() {
        recoverer.recover(mensagem("{\"vagaId\": 1}"), doListener(new RuntimeException("x".repeat(2000))));

        ArgumentCaptor<Object> evento = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.RK_VAGA_ANALISADA),
                evento.capture());
        assertThat(((VagaAnalisadaEvent) evento.getValue()).erro()).hasSize(500);
    }

    @Test
    void mensagemIlegivelVaiPraDlqSemAvisarOVagaService() {
        Message mensagem = mensagem("isso não é json");

        recoverer.recover(mensagem, doListener(new RuntimeException("conversão")));

        verify(rabbitTemplate).send(eq(RabbitConfig.DLX), eq(RabbitConfig.DLQ_VAGA_CRIADA), eq(mensagem));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
