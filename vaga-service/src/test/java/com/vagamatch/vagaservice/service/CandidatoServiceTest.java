package com.vagamatch.vagaservice.service;

import com.vagamatch.vagaservice.domain.Candidato;
import com.vagamatch.vagaservice.domain.Skill;
import com.vagamatch.vagaservice.dto.CandidatoRequest;
import com.vagamatch.vagaservice.dto.CandidatoResponse;
import com.vagamatch.vagaservice.dto.SkillNivelRequest;
import com.vagamatch.vagaservice.exception.ConflitoException;
import com.vagamatch.vagaservice.exception.RecursoNaoEncontradoException;
import com.vagamatch.vagaservice.repository.CandidatoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.vagamatch.vagaservice.Fixtures.candidato;
import static com.vagamatch.vagaservice.Fixtures.skill;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatoServiceTest {

    @Mock
    private CandidatoRepository candidatoRepository;
    @Mock
    private SkillService skillService;
    @InjectMocks
    private CandidatoService candidatoService;

    @Test
    void emailJaCadastradoDa409SemSalvar() {
        when(candidatoRepository.existsByEmailIgnoreCase("ana@exemplo.com")).thenReturn(true);

        assertThatThrownBy(() -> candidatoService.criar(new CandidatoRequest("Ana", "ana@exemplo.com")))
                .isInstanceOf(ConflitoException.class);
        verify(candidatoRepository, never()).save(any());
    }

    @Test
    void atualizarComEmailDeOutroCandidatoDa409() {
        when(candidatoRepository.existsByEmailIgnoreCaseAndIdNot("bia@exemplo.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> candidatoService.atualizar(1L, new CandidatoRequest("Ana", "bia@exemplo.com")))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void candidatoInexistenteDa404() {
        when(candidatoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidatoService.buscar(1L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void definirSkillAtualizaONivelDaQueJaExisteEAdicionaANova() {
        Skill java = skill(1, "Java");
        Skill sql = skill(2, "SQL");
        Candidato candidato = candidato(1, java); // Java nível 3
        when(candidatoRepository.findById(1L)).thenReturn(Optional.of(candidato));
        when(skillService.buscarOuCriar("java", null)).thenReturn(java);
        when(skillService.buscarOuCriar("SQL", null)).thenReturn(sql);

        CandidatoResponse resposta = candidatoService.definirSkills(1L, List.of(
                new SkillNivelRequest("java", 5), new SkillNivelRequest("SQL", 2)));

        assertThat(resposta.skills()).containsExactly(
                new CandidatoResponse.SkillDoCandidatoResponse(1L, "Java", 5),
                new CandidatoResponse.SkillDoCandidatoResponse(2L, "SQL", 2));
    }

    @Test
    void removerSkillQueOCandidatoNaoTemDa404() {
        when(candidatoRepository.findById(1L)).thenReturn(Optional.of(candidato(1, skill(1, "Java"))));

        assertThatThrownBy(() -> candidatoService.removerSkill(1L, 99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void removerSkillTiraDoPerfil() {
        Candidato candidato = candidato(1, skill(1, "Java"), skill(2, "SQL"));
        when(candidatoRepository.findById(1L)).thenReturn(Optional.of(candidato));

        candidatoService.removerSkill(1L, 1L);

        assertThat(candidato.getSkills()).extracting(cs -> cs.getSkill().getNome()).containsExactly("SQL");
    }
}
