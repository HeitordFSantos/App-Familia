package com.appfamilia.feature.agenda;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.appfamilia.core.Alarmes;
import com.appfamilia.core.Formatos;
import com.appfamilia.core.rede.Modelos;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

/**
 * Lembretes dos compromissos como notificações locais. Cada vez que a agenda é carregada,
 * os alarmes são refeitos a partir da lista do servidor; a lista agendada fica guardada
 * no aparelho para ser refeita depois de reiniciar.
 */
public final class Lembretes {

    static final String EXTRA_TITULO = "titulo";
    static final String EXTRA_TEXTO = "texto";

    private static final String PREFS = "lembretes";
    private static final String CHAVE = "agendados";

    private Lembretes() {
    }

    public static void sincronizar(Context contexto, List<Modelos.Compromisso> compromissos) {
        for (Modelos.Compromisso antigo : agendados(contexto)) {
            Alarmes.cancelar(contexto, acao(contexto, antigo));
        }
        List<Modelos.Compromisso> futuros = new ArrayList<>();
        long agora = System.currentTimeMillis();
        for (Modelos.Compromisso compromisso : compromissos) {
            if (compromisso.lembreteMinutos == null || compromisso.id == null) {
                continue;
            }
            long quando = Formatos.millis(compromisso.inicio) - compromisso.lembreteMinutos * 60_000L;
            if (quando > agora) {
                Alarmes.agendar(contexto, quando, acao(contexto, compromisso));
                futuros.add(compromisso);
            }
        }
        prefs(contexto).edit().putString(CHAVE, new Gson().toJson(futuros)).apply();
    }

    /** Refaz os alarmes guardados (usado depois de reiniciar o aparelho). */
    public static void reagendar(Context contexto) {
        sincronizar(contexto, agendados(contexto));
    }

    private static List<Modelos.Compromisso> agendados(Context contexto) {
        String json = prefs(contexto).getString(CHAVE, null);
        if (json == null) {
            return new ArrayList<>();
        }
        try {
            List<Modelos.Compromisso> lista = new Gson().fromJson(json,
                    new TypeToken<List<Modelos.Compromisso>>() { }.getType());
            return lista == null ? new ArrayList<>() : lista;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static PendingIntent acao(Context contexto, Modelos.Compromisso compromisso) {
        Intent intent = new Intent(contexto, LembreteReceiver.class);
        // A ação identifica o alarme: o mesmo compromisso sempre gera o mesmo PendingIntent
        intent.setAction("lembrete:" + compromisso.id);
        intent.putExtra(EXTRA_TITULO, compromisso.titulo);
        intent.putExtra(EXTRA_TEXTO, Formatos.dataHoraBr(compromisso.inicio));
        return PendingIntent.getBroadcast(contexto, compromisso.id.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static SharedPreferences prefs(Context contexto) {
        return contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
