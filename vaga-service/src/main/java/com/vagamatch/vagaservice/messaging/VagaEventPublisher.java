package com.vagamatch.vagaservice.messaging;

import com.vagamatch.vagaservice.config.RabbitConfig;
import com.vagamatch.vagaservice.dto.VagaCriadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica vaga.criada só depois do commit. Se publicasse dentro da transação,
 * o analise-service poderia responder antes da vaga existir no banco
 * (ou a transação dar rollback e a mensagem já ter saído).
 */
@Component
public class VagaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(VagaEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public VagaEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(VagaCriadaEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_VAGA_CRIADA, event);
            log.info("vaga.criada publicada: vagaId={}", event.vagaId());
        } catch (AmqpException e) {
            // a vaga já foi salva; ela fica PENDENTE em vez de quebrar o cadastro
            log.error("Falha ao publicar vaga.criada: vagaId={}", event.vagaId(), e);
        }
    }
}
