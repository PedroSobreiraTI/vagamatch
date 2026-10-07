package com.vagamatch.vagaservice.service;

import com.vagamatch.vagaservice.domain.Skill;
import com.vagamatch.vagaservice.domain.StatusVaga;
import com.vagamatch.vagaservice.domain.Vaga;
import com.vagamatch.vagaservice.dto.VagaAnalisadaEvent;
import com.vagamatch.vagaservice.dto.VagaAnalisadaEvent.SkillExtraida;
import com.vagamatch.vagaservice.dto.VagaCriadaEvent;
import com.vagamatch.vagaservice.dto.VagaRequest;
import com.vagamatch.vagaservice.dto.VagaResponse;
import com.vagamatch.vagaservice.exception.RecursoNaoEncontradoException;
import com.vagamatch.vagaservice.repository.VagaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class VagaService {

    private static final Logger log = LoggerFactory.getLogger(VagaService.class);

    private static final int TAMANHO_MAX_NOME_SKILL = 100;
    private static final int TAMANHO_MAX_CATEGORIA = 50;

    private final VagaRepository vagaRepository;
    private final SkillService skillService;
    private final ApplicationEventPublisher eventPublisher;

    public VagaService(VagaRepository vagaRepository, SkillService skillService,
                       ApplicationEventPublisher eventPublisher) {
        this.vagaRepository = vagaRepository;
        this.skillService = skillService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public VagaResponse criar(VagaRequest request) {
        Vaga vaga = vagaRepository.save(new Vaga(request.titulo(), request.empresa(), request.localizacao(),
                request.descricao(), request.link()));
        // só vai pro RabbitMQ depois do commit (ver VagaEventPublisher)
        eventPublisher.publishEvent(new VagaCriadaEvent(vaga.getId(), vaga.getTitulo(), vaga.getDescricao()));
        return VagaResponse.from(vaga);
    }

    @Transactional(readOnly = true)
    public Page<VagaResponse> listar(Pageable pageable) {
        return vagaRepository.findAll(pageable).map(VagaResponse::from);
    }

    @Transactional(readOnly = true)
    public VagaResponse buscar(Long id) {
        return VagaResponse.from(buscarEntidade(id));
    }

    @Transactional
    public VagaResponse atualizar(Long id, VagaRequest request) {
        Vaga vaga = buscarEntidade(id);
        vaga.atualizar(request.titulo(), request.empresa(), request.localizacao(),
                request.descricao(), request.link());
        return VagaResponse.from(vaga);
    }

    @Transactional
    public void remover(Long id) {
        vagaRepository.delete(buscarEntidade(id));
    }

    /**
     * Aplica o resultado do analise-service. Idempotente: o RabbitMQ entrega pelo menos
     * uma vez, então uma mensagem repetida (ou de vaga já removida) é ignorada.
     */
    @Transactional
    public void registrarAnalise(VagaAnalisadaEvent event) {
        Vaga vaga = vagaRepository.findById(event.vagaId()).orElse(null);
        if (vaga == null) {
            log.warn("vaga.analisada para vaga inexistente: vagaId={}", event.vagaId());
            return;
        }
        if (vaga.getStatus() != StatusVaga.PENDENTE) {
            log.info("vaga.analisada ignorada, vaga já está {}: vagaId={}", vaga.getStatus(), vaga.getId());
            return;
        }

        if (!event.sucesso()) {
            vaga.marcarErroNaAnalise();
            log.warn("Análise da vaga falhou: vagaId={}, erro={}", vaga.getId(), event.erro());
            return;
        }

        vaga.registrarAnalise(resolverSkills(event.skills()));
        log.info("Vaga analisada: vagaId={}, skills={}", vaga.getId(), vaga.getSkills().size());
    }

    /**
     * Converte as skills extraídas em entidades, sem repetir skill na mesma vaga
     * (o Gemini pode devolver "Java" e "java"). Se aparecer duas vezes, vale obrigatória.
     */
    private Map<Skill, Boolean> resolverSkills(List<SkillExtraida> extraidas) {
        Map<String, SkillExtraida> porNome = new LinkedHashMap<>();
        for (SkillExtraida skill : extraidas == null ? List.<SkillExtraida>of() : extraidas) {
            if (skill.nome() == null || skill.nome().isBlank()) {
                continue;
            }
            porNome.merge(skill.nome().trim().toLowerCase(Locale.ROOT), skill,
                    (atual, nova) -> atual.obrigatoria() ? atual : nova);
        }

        Map<Skill, Boolean> resultado = new LinkedHashMap<>();
        porNome.values().forEach(skill -> resultado.put(
                skillService.buscarOuCriar(limitar(skill.nome().trim(), TAMANHO_MAX_NOME_SKILL),
                        limitar(skill.categoria(), TAMANHO_MAX_CATEGORIA)),
                skill.obrigatoria()));
        return resultado;
    }

    private static String limitar(String valor, int tamanhoMax) {
        return valor == null || valor.length() <= tamanhoMax ? valor : valor.substring(0, tamanhoMax);
    }

    private Vaga buscarEntidade(Long id) {
        return vagaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga", id));
    }
}
