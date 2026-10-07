package com.vagamatch.vagaservice.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "candidato")
public class Candidato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "candidato", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CandidatoSkill> skills = new HashSet<>();

    protected Candidato() {
    }

    public Candidato(String nome, String email) {
        this.nome = nome;
        this.email = email;
    }

    @PrePersist
    void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }

    /** Adiciona a skill ou atualiza o nível se o candidato já tiver ela. */
    public void definirSkill(Skill skill, int nivel) {
        skills.stream()
                .filter(cs -> cs.getSkill().getId().equals(skill.getId()))
                .findFirst()
                .ifPresentOrElse(
                        cs -> cs.setNivel(nivel),
                        () -> skills.add(new CandidatoSkill(this, skill, nivel)));
    }

    public boolean removerSkill(Long skillId) {
        return skills.removeIf(cs -> cs.getSkill().getId().equals(skillId));
    }

    public void atualizar(String nome, String email) {
        this.nome = nome;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public Set<CandidatoSkill> getSkills() { return skills; }
}
