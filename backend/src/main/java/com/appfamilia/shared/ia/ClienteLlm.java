package com.appfamilia.shared.ia;

/**
 * Acesso ao modelo de linguagem. Os módulos dependem só desta interface; trocar de
 * modelo ou de provedor não muda o resto do sistema.
 */
public interface ClienteLlm {

    /** Falso quando o endereço do modelo não foi configurado. */
    boolean configurado();

    /** Pergunta de texto. Devolve a resposta do modelo. */
    String conversar(String instrucoes, String mensagem);

    /** Pergunta sobre uma imagem (precisa de um modelo com visão). */
    String lerImagem(String instrucoes, String imagemBase64, String tipoMime);
}
