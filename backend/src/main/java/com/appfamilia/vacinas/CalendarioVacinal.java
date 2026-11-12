package com.appfamilia.vacinas;

import java.util.List;

/**
 * Doses de rotina da criança no Calendário Nacional de Vacinação (PNI / Ministério da Saúde).
 *
 * Base: calendário técnico da criança atualizado em 30/01/2026, mais o 2º reforço da VIP
 * aos 4 anos (em vigor desde 03/08/2026). O calendário muda de tempos em tempos: ao
 * atualizar, mantenha os códigos existentes, porque os registros de aplicação apontam para eles.
 *
 * É uma referência para lembrar a família. Quem define o que aplicar é a unidade de saúde,
 * com base na caderneta da criança.
 */
public final class CalendarioVacinal {

    public static final String FONTE =
            "Calendário Nacional de Vacinação da criança (Ministério da Saúde), versão de 30/01/2026 "
                    + "com o 2º reforço da VIP aos 4 anos";

    public record Dose(String codigo, String vacina, String dose, int idadeMeses, String observacao) {
    }

    public static final List<Dose> DOSES = List.of(
            new Dose("BCG_UNICA", "BCG", "Dose única", 0, null),
            new Dose("HEPB_1", "Hepatite B", "1ª dose", 0, null),

            new Dose("PENTA_1", "Penta (DTP-Hib-HB)", "1ª dose", 2, null),
            new Dose("VIP_1", "VIP (poliomielite inativada)", "1ª dose", 2, null),
            new Dose("ROTA_1", "Rotavírus", "1ª dose", 2, "Tem idade máxima para aplicação"),
            new Dose("PNEUMO_1", "Pneumocócica conjugada", "1ª dose", 2, null),

            new Dose("MENC_1", "Meningocócica C", "1ª dose", 3, null),

            new Dose("PENTA_2", "Penta (DTP-Hib-HB)", "2ª dose", 4, null),
            new Dose("VIP_2", "VIP (poliomielite inativada)", "2ª dose", 4, null),
            new Dose("ROTA_2", "Rotavírus", "2ª dose", 4, "Tem idade máxima para aplicação"),
            new Dose("PNEUMO_2", "Pneumocócica conjugada", "2ª dose", 4, null),

            new Dose("MENC_2", "Meningocócica C", "2ª dose", 5, null),

            new Dose("PENTA_3", "Penta (DTP-Hib-HB)", "3ª dose", 6, null),
            new Dose("VIP_3", "VIP (poliomielite inativada)", "3ª dose", 6, null),
            new Dose("COVID_1", "Covid-19", "1ª dose", 6, "O número de doses depende da vacina disponível"),
            new Dose("INFLUENZA_1", "Influenza (gripe)", "1ª dose", 6,
                    "Na primeira vez são 2 doses com 30 dias de intervalo; depois, 1 dose por ano"),

            new Dose("COVID_2", "Covid-19", "2ª dose", 7, "O número de doses depende da vacina disponível"),

            new Dose("FA_1", "Febre amarela", "1ª dose", 9, null),
            new Dose("COVID_3", "Covid-19", "3ª dose", 9, "Só em esquemas de 3 doses"),

            new Dose("PNEUMO_REF", "Pneumocócica conjugada", "Reforço", 12, null),
            new Dose("MENACWY_REF", "Meningocócica ACWY", "Reforço", 12, null),
            new Dose("SCR_1", "Tríplice viral (sarampo, caxumba, rubéola)", "1ª dose", 12, null),

            new Dose("VIP_REF1", "VIP (poliomielite inativada)", "1º reforço", 15, null),
            new Dose("DTP_REF1", "DTP (tríplice bacteriana)", "1º reforço", 15, null),
            new Dose("SCR_2", "Tríplice viral (sarampo, caxumba, rubéola)", "2ª dose", 15, null),
            new Dose("VARICELA_1", "Varicela", "1ª dose", 15, null),
            new Dose("HEPA_UNICA", "Hepatite A", "Dose única", 15, null),

            new Dose("DTP_REF2", "DTP (tríplice bacteriana)", "2º reforço", 48, null),
            new Dose("VIP_REF2", "VIP (poliomielite inativada)", "2º reforço", 48, null),
            new Dose("FA_REF", "Febre amarela", "Reforço", 48, null),
            new Dose("VARICELA_2", "Varicela", "2ª dose", 48, null),

            new Dose("HPV_UNICA", "HPV", "Dose única", 108, "Indicada dos 9 aos 14 anos"));

    private CalendarioVacinal() {
    }

    public static boolean existe(String codigo) {
        return DOSES.stream().anyMatch(d -> d.codigo().equals(codigo));
    }
}
