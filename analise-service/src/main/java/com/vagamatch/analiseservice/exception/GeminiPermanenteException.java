package com.vagamatch.analiseservice.exception;

/**
 * Erro do Gemini que não melhora tentando de novo (chave ausente/inválida, request malformado,
 * modelo inexistente). O retry do listener ignora essa exceção e vai direto pro recoverer.
 */
public class GeminiPermanenteException extends AnaliseException {

    public GeminiPermanenteException(String mensagem) {
        super(mensagem);
    }

    public GeminiPermanenteException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
