package com.vagamatch.vagaservice.messaging;

import com.vagamatch.vagaservice.config.RabbitConfig;
import com.vagamatch.vagaservice.dto.VagaAnalisadaEvent;
import com.vagamatch.vagaservice.service.VagaService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class VagaAnalisadaListener {

    private final VagaService vagaService;

    public VagaAnalisadaListener(VagaService vagaService) {
        this.vagaService = vagaService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_VAGA_ANALISADA)
    public void receber(VagaAnalisadaEvent event) {
        vagaService.registrarAnalise(event);
    }
}
