-- Status da análise da vaga pelo analise-service (Gemini).
-- Vagas que já existiam ficam PENDENTE.
ALTER TABLE vaga
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
        CONSTRAINT ck_vaga_status CHECK (status IN ('PENDENTE', 'ANALISADA', 'ERRO'));

CREATE INDEX idx_vaga_status ON vaga(status);
