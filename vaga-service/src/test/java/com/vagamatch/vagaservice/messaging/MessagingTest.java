package com.vagamatch.vagaservice.messaging;

import com.vagamatch.vagaservice.config.RabbitConfig;
import com.vagamatch.vagaservice.dto.VagaAnalisadaEvent;
import com.vagamatch.vagaservice.dto.VagaCriadaEvent;
import com.vagamatch.vagaservice.service.VagaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.ConnectException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/** Listener e publisher são finos: o teste garante que delegam e não derrubam o fluxo. */
@ExtendWith(MockitoExtension.class)
class MessagingTest {

    @Mock
    private VagaService vagaService;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void listenerDelegaProService() {
        VagaAnalisadaEvent event = new VagaAnalisadaEvent(1L, true, null, List.of());

        new VagaAnalisadaListener(vagaService).receber(event);

        verify(vagaService).registrarAnalise(event);
    }

    @Test
    void publisherMandaVagaCriadaProExchange() {
        VagaCriadaEvent event = new VagaCriadaEvent(1L, "Dev", "...");

        new VagaEventPublisher(rabbitTemplate).publicar(event);

        verify(rabbitTemplate).convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_VAGA_CRIADA, event);
    }

    @Test
    void rabbitForaDoArNaoQuebraOCadastro() {
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // a vaga já foi commitada; fica PENDENTE em vez de devolver erro pro cliente
        assertThatCode(() -> new VagaEventPublisher(rabbitTemplate).publicar(new VagaCriadaEvent(1L, "Dev", "...")))
                .doesNotThrowAnyException();
    }
}
