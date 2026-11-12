package com.appfamilia.feature.devocional;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuração do horário do devocional, guardada só no aparelho: cada pessoa
 * define o seu horário e os aplicativos que quer bloquear.
 */
public class DevocionalConfig {

    /** Aplicativos que o app sabe bloquear: nome mostrado e pacote. */
    public static final String[][] APPS_CONHECIDOS = {
            {"Instagram", "com.instagram.android"},
            {"Facebook", "com.facebook.katana"},
            {"Facebook Lite", "com.facebook.lite"},
            {"TikTok", "com.zhiliaoapp.musically"},
            {"TikTok Lite", "com.zhiliaoapp.musically.go"},
            {"X (Twitter)", "com.twitter.android"},
            {"YouTube", "com.google.android.youtube"},
            {"Threads", "com.instagram.barcelona"},
            {"Kwai", "com.kwai.video"},
            {"Snapchat", "com.snapchat.android"},
            {"Pinterest", "com.pinterest"},
            {"Reddit", "com.reddit.frontpage"},
            {"WhatsApp", "com.whatsapp"},
            {"Telegram", "org.telegram.messenger"},
            {"Messenger", "com.facebook.orca"},
    };

    /** Mensageiros começam desmarcados; o restante começa marcado. */
    private static final Set<String> PADRAO = new HashSet<>(Arrays.asList(
            "com.instagram.android", "com.facebook.katana", "com.facebook.lite",
            "com.zhiliaoapp.musically", "com.zhiliaoapp.musically.go", "com.twitter.android",
            "com.google.android.youtube", "com.instagram.barcelona", "com.kwai.video",
            "com.snapchat.android", "com.pinterest", "com.reddit.frontpage"));

    private static final long UM_DIA = 24 * 60 * 60 * 1000L;

    private final SharedPreferences prefs;

    public DevocionalConfig(Context contexto) {
        prefs = contexto.getSharedPreferences("devocional", Context.MODE_PRIVATE);
    }

    public boolean ativo() {
        return prefs.getBoolean("ativo", false);
    }

    public int hora() {
        return prefs.getInt("hora", 6);
    }

    public int minuto() {
        return prefs.getInt("minuto", 0);
    }

    public int duracaoMinutos() {
        return prefs.getInt("duracao", 30);
    }

    public Set<String> apps() {
        return new HashSet<>(prefs.getStringSet("apps", PADRAO));
    }

    public void salvar(boolean ativo, int hora, int minuto, int duracaoMinutos, Set<String> apps) {
        prefs.edit()
                .putBoolean("ativo", ativo)
                .putInt("hora", hora)
                .putInt("minuto", minuto)
                .putInt("duracao", duracaoMinutos)
                .putStringSet("apps", new HashSet<>(apps))
                .apply();
    }

    /** Fim do período em andamento, em millis; 0 se agora não é horário do devocional. */
    public long fimDoPeriodoAtual() {
        long agora = System.currentTimeMillis();
        long duracao = duracaoMinutos() * 60_000L;
        long inicioHoje = inicioHoje();
        if (agora >= inicioHoje && agora < inicioHoje + duracao) {
            return inicioHoje + duracao;
        }
        // Período que começou ontem e atravessa a meia-noite
        long inicioOntem = inicioHoje - UM_DIA;
        if (agora < inicioOntem + duracao) {
            return inicioOntem + duracao;
        }
        return 0;
    }

    /** Próximo início do período, em millis. */
    public long proximoInicio() {
        long inicioHoje = inicioHoje();
        return inicioHoje > System.currentTimeMillis() ? inicioHoje : inicioHoje + UM_DIA;
    }

    private long inicioHoje() {
        Calendar calendario = Calendar.getInstance();
        calendario.set(Calendar.HOUR_OF_DAY, hora());
        calendario.set(Calendar.MINUTE, minuto());
        calendario.set(Calendar.SECOND, 0);
        calendario.set(Calendar.MILLISECOND, 0);
        return calendario.getTimeInMillis();
    }
}
