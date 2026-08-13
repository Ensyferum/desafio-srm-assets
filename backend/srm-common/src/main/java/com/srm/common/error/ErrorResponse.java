package com.srm.common.error;

import java.time.Instant;
import java.util.Map;

/**
 * Corpo padrão de resposta de erro da API.
 *
 * @param timestamp momento do erro
 * @param status código HTTP
 * @param error frase do status HTTP
 * @param message mensagem amigável/descritiva
 * @param path rota da requisição que falhou
 * @param correlationId rastreabilidade da solicitação
 * @param errorId identificador único para rastreio em logs (erros 500)
 * @param fieldErrors erros de validação por campo (quando aplicável)
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        String errorId,
        Map<String, String> fieldErrors) {}
