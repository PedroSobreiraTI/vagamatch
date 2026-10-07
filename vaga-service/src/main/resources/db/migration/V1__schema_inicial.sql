CREATE TABLE vaga (
    id           BIGSERIAL PRIMARY KEY,
    titulo       VARCHAR(200) NOT NULL,
    empresa      VARCHAR(150),
    localizacao  VARCHAR(150),
    descricao    TEXT         NOT NULL,
    link         VARCHAR(500),
    criada_em    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE skill (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(100) NOT NULL UNIQUE,
    categoria  VARCHAR(50)
);

-- N:N vaga x skill (obrigatoria = requisito; false = diferencial)
CREATE TABLE vaga_skill (
    vaga_id      BIGINT  NOT NULL REFERENCES vaga(id) ON DELETE CASCADE,
    skill_id     BIGINT  NOT NULL REFERENCES skill(id),
    obrigatoria  BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (vaga_id, skill_id)
);

CREATE TABLE candidato (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(150) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    criado_em  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- N:N candidato x skill com nível de proficiência
CREATE TABLE candidato_skill (
    candidato_id  BIGINT   NOT NULL REFERENCES candidato(id) ON DELETE CASCADE,
    skill_id      BIGINT   NOT NULL REFERENCES skill(id),
    nivel         SMALLINT NOT NULL CHECK (nivel BETWEEN 1 AND 5),
    PRIMARY KEY (candidato_id, skill_id)
);

CREATE INDEX idx_vaga_skill_skill ON vaga_skill(skill_id);
CREATE INDEX idx_candidato_skill_skill ON candidato_skill(skill_id);
