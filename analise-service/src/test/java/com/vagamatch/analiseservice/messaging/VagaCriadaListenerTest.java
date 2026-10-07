package com.vagamatch.analiseservice.messaging;

import com.vagamatch.analiseservice.config.RabbitConfig;
import com.vagamatch.analiseservice.dto.SkillExtraida;
import com.vagamatch.analiseservice.dto.VagaAnalisadaEvent;
import com.vagamatch.analiseservice.dto.VagaCriadaEvent;
import com.vagamatch.analiseservice.exception.AnaliseException;
import com.vagamatch.analiseservice.service.GeminiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VagaCriadaListenerTest {

    @Mock
    private GeminiService geminiService;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @InjectMocks
    private VagaCriadaListener listener;

    @Test
    void publicaAsSkillsExtraidas() {
        List<SkillExtraida> skills = List.of(new SkillExtraida("Java", "LINGUAGEM", true));
        when(geminiService.extrairSkills("Dev Java", "descrição")).thenReturn(skills);

        listener.receber(new VagaCriadaEvent(7L, "Dev Java", "descrição"));

        verify(rabbitTemplate).convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_VAGA_ANALISADA,
                VagaAnalisadaEvent.sucesso(7L, skills));
    }

    @Test
    void falhaNoGeminiSobeAExcecaoSemPublicar() {
        when(geminiService.extrairSkills(any(), any())).thenThrow(new AnaliseException("Gemini fora"));

        // a exceção precisa subir pro retry do listener / recoverer
        assertThatThrownBy(() -> listener.receber(new VagaCriadaEvent(7L, "Dev Java", "descrição")))
                .isInstanceOf(AnaliseException.class);
        verifyNoInteractions(rabbitTemplate);
    }
}
