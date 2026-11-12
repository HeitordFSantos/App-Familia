package com.appfamilia.shared.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Modelos costumam cercar o JSON com texto ("Aqui está:", cercas de código). Esta classe
 * pega o que está entre a primeira chave aberta e a última fechada e tenta ler.
 */
public final class JsonDaLlm {

    private JsonDaLlm() {
    }

    /** Devolve o objeto JSON encontrado na resposta, ou null se não houver um válido. */
    public static JsonNode extrair(ObjectMapper json, String resposta) {
        if (resposta == null) {
            return null;
        }
        int inicio = resposta.indexOf('{');
        int fim = resposta.lastIndexOf('}');
        if (inicio < 0 || fim <= inicio) {
            return null;
        }
        try {
            JsonNode lido = json.readTree(resposta.substring(inicio, fim + 1));
            return lido.isObject() ? lido : null;
        } catch (Exception invalido) {
            return null;
        }
    }
}
