package com.vagamatch.analiseservice.exception;

/** Falha ao extrair as skills de uma vaga (Gemini fora, resposta inválida, chave ausente...). */
public class AnaliseException extends RuntimeException {

    public AnaliseException(String mensagem) {
        super(mensagem);
    }

    public AnaliseException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
