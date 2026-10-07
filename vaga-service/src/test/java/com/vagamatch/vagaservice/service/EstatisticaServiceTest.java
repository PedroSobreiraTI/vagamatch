package com.vagamatch.vagaservice.service;

import com.vagamatch.vagaservice.domain.StatusVaga;
import com.vagamatch.vagaservice.dto.EstatisticaSkillsResponse;
import com.vagamatch.vagaservice.dto.EstatisticaSkillsResponse.SkillPedida;
import com.vagamatch.vagaservice.repository.SkillEstatisticaProjection;
import com.vagamatch.vagaservice.repository.SkillRepository;
import com.vagamatch.vagaservice.repository.VagaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstatisticaServiceTest {

    @Mock
    private SkillRepository skillRepository;
    @Mock
    private VagaRepository vagaRepository;
    @InjectMocks
    private EstatisticaService estatisticaService;

    @Test
    void percentualEhSobreAsVagasAnalisadas() {
        SkillEstatisticaProjection java = mock(SkillEstatisticaProjection.class);
        when(java.getSkillId()).thenReturn(1L);
        when(java.getNome()).thenReturn("Java");
        when(java.getCategoria()).thenReturn("LINGUAGEM");
        when(java.getTotalVagas()).thenReturn(3L);
        when(java.getComoObrigatoria()).thenReturn(2L);
        when(java.getComoDiferencial()).thenReturn(1L);
        when(vagaRepository.countByStatus(StatusVaga.ANALISADA)).thenReturn(4L);
        when(skillRepository.maisPedidas(10)).thenReturn(List.of(java));

        EstatisticaSkillsResponse resposta = estatisticaService.skillsMaisPedidas(10);

        assertThat(resposta.vagasAnalisadas()).isEqualTo(4);
        assertThat(resposta.skills()).containsExactly(new SkillPedida(1L, "Java", "LINGUAGEM", 3, 2, 1, 75));
    }

    @Test
    void semVagasAnalisadasDevolveVazio() {
        when(vagaRepository.countByStatus(StatusVaga.ANALISADA)).thenReturn(0L);
        when(skillRepository.maisPedidas(5)).thenReturn(List.of());

        EstatisticaSkillsResponse resposta = estatisticaService.skillsMaisPedidas(5);

        assertThat(resposta.vagasAnalisadas()).isZero();
        assertThat(resposta.skills()).isEmpty();
    }
}
