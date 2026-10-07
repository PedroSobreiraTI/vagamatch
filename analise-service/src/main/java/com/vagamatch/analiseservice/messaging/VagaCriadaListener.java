package com.vagamatch.analiseservice.messaging;

import com.vagamatch.analiseservice.config.RabbitConfig;
import com.vagamatch.analiseservice.dto.SkillExtraida;
import com.vagamatch.analiseservice.dto.VagaAnalisadaEvent;
import com.vagamatch.analiseservice.dto.VagaCriadaEvent;
import com.vagamatch.analiseservice.service.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Consome vaga.criada, extrai as skills com o Gemini e publica vaga.analisada.
 * Se der exceção, o retry do listener tenta de novo; esgotou, entra o FalhaAnaliseRecoverer.
 */
@Component
public class VagaCriadaListener {

    private static final Logger log = LoggerFactory.getLogger(VagaCriadaListener.class);

    private final GeminiService geminiService;
    private final RabbitTemplate rabbitTemplate;

    public VagaCriadaListener(GeminiService geminiService, RabbitTemplate rabbitTemplate) {
        this.geminiService = geminiService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_VAGA_CRIADA)
    public void receber(VagaCriadaEvent event) {
        log.info("Analisando vaga: vagaId={}", event.vagaId());
        List<SkillExtraida> skills = geminiService.extrairSkills(event.titulo(), event.descricao());

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_VAGA_ANALISADA,
                VagaAnalisadaEvent.sucesso(event.vagaId(), skills));
        log.info("Vaga analisada: vagaId={}, skills={}", event.vagaId(), skills.size());
    }
}
