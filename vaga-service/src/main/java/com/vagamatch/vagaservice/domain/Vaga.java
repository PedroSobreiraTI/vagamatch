package com.vagamatch.vagaservice.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "vaga")
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 150)
    private String empresa;

    @Column(length = 150)
    private String localizacao;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(length = 500)
    private String link;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private LocalDateTime criadaEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusVaga status = StatusVaga.PENDENTE;

    @OneToMany(mappedBy = "vaga", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<VagaSkill> skills = new HashSet<>();

    protected Vaga() {
    }

    public Vaga(String titulo, String empresa, String localizacao, String descricao, String link) {
        atualizar(titulo, empresa, localizacao, descricao, link);
    }

    @PrePersist
    void prePersist() {
        this.criadaEm = LocalDateTime.now();
    }

    public void atualizar(String titulo, String empresa, String localizacao, String descricao, String link) {
        this.titulo = titulo;
        this.empresa = empresa;
        this.localizacao = localizacao;
        this.descricao = descricao;
        this.link = link;
    }

    public void adicionarSkill(Skill skill, boolean obrigatoria) {
        skills.add(new VagaSkill(this, skill, obrigatoria));
    }

    /** Recebe as skills extraídas pelo analise-service (chave = skill, valor = obrigatória). */
    public void registrarAnalise(Map<Skill, Boolean> skillsExtraidas) {
        skillsExtraidas.forEach(this::adicionarSkill);
        this.status = StatusVaga.ANALISADA;
    }

    public void marcarErroNaAnalise() {
        this.status = StatusVaga.ERRO;
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getEmpresa() { return empresa; }
    public String getLocalizacao() { return localizacao; }
    public String getDescricao() { return descricao; }
    public String getLink() { return link; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public StatusVaga getStatus() { return status; }
    public Set<VagaSkill> getSkills() { return skills; }
}
