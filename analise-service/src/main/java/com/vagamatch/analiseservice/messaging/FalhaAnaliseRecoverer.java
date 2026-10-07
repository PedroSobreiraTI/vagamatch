package com.vagamatch.analiseservice.messaging;

import com.vagamatch.analiseservice.config.RabbitConfig;
import com.vagamatch.analiseservice.dto.VagaAnalisadaEvent;
import com.vagamatch.analiseservice.dto.VagaCriadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Chamado quando o retry do listener se esgota.
 * 1. Republica a mensagem original na DLQ (com o stack trace nos headers, pra investigar).
 * 2. Avisa o vaga-service com vaga.analisada sucesso=false, pra vaga ir pra ERRO
 *    em vez de ficar PENDENTE pra sempre.
 * O Spring Boot liga este bean no retry do listener por ser um MessageRecoverer.
 */
@Component
public class FalhaAnaliseRecoverer extends RepublishMessageRecoverer {

    private static final Logger log = LoggerFactory.getLogger(FalhaAnaliseRecoverer.class);
    private static final int TAMANHO_MAX_ERRO = 500;

    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;

    public FalhaAnaliseRecoverer(RabbitTemplate rabbitTemplate, JsonMapper jsonMapper) {
        super(rabbitTemplate, RabbitConfig.DLX, RabbitConfig.DLQ_VAGA_CRIADA);
        this.rabbitTemplate = rabbitTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        super.recover(message, cause);

        Long vagaId;
        try {
            vagaId = jsonMapper.readValue(message.getBody(), VagaCriadaEvent.class).vagaId();
        } catch (JacksonException e) {
            log.error("Mensagem inválida enviada pra DLQ sem avisar o vaga-service", e);
            return;
        }

        String erro = mensagem(cause);
        log.error("Análise falhou após as tentativas, vaga enviada pra DLQ: vagaId={}, erro={}", vagaId, erro);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_VAGA_ANALISADA,
                VagaAnalisadaEvent.falha(vagaId, erro));
    }

    private static String mensagem(Throwable cause) {
        // a exceção chega embrulhada em ListenerExecutionFailedException; o que interessa é a original
        Throwable raiz = NestedExceptionUtils.getMostSpecificCause(cause);
        String erro = String.valueOf(raiz.getMessage());
        return erro.length() <= TAMANHO_MAX_ERRO ? erro : erro.substring(0, TAMANHO_MAX_ERRO);
    }
}
