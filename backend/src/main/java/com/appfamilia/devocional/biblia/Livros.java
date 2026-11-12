package com.appfamilia.devocional.biblia;

import java.util.List;
import java.util.Optional;

/** Os 66 livros do cânon protestante, com o código padrão (USFM) e o número de capítulos. */
public final class Livros {

    public record Livro(String codigo, String nome, int capitulos) {
    }

    public static final List<Livro> TODOS = List.of(
            new Livro("GEN", "Gênesis", 50), new Livro("EXO", "Êxodo", 40), new Livro("LEV", "Levítico", 27),
            new Livro("NUM", "Números", 36), new Livro("DEU", "Deuteronômio", 34), new Livro("JOS", "Josué", 24),
            new Livro("JDG", "Juízes", 21), new Livro("RUT", "Rute", 4), new Livro("1SA", "1 Samuel", 31),
            new Livro("2SA", "2 Samuel", 24), new Livro("1KI", "1 Reis", 22), new Livro("2KI", "2 Reis", 25),
            new Livro("1CH", "1 Crônicas", 29), new Livro("2CH", "2 Crônicas", 36), new Livro("EZR", "Esdras", 10),
            new Livro("NEH", "Neemias", 13), new Livro("EST", "Ester", 10), new Livro("JOB", "Jó", 42),
            new Livro("PSA", "Salmos", 150), new Livro("PRO", "Provérbios", 31), new Livro("ECC", "Eclesiastes", 12),
            new Livro("SNG", "Cânticos", 8), new Livro("ISA", "Isaías", 66), new Livro("JER", "Jeremias", 52),
            new Livro("LAM", "Lamentações", 5), new Livro("EZK", "Ezequiel", 48), new Livro("DAN", "Daniel", 12),
            new Livro("HOS", "Oseias", 14), new Livro("JOL", "Joel", 3), new Livro("AMO", "Amós", 9),
            new Livro("OBA", "Obadias", 1), new Livro("JON", "Jonas", 4), new Livro("MIC", "Miqueias", 7),
            new Livro("NAM", "Naum", 3), new Livro("HAB", "Habacuque", 3), new Livro("ZEP", "Sofonias", 3),
            new Livro("HAG", "Ageu", 2), new Livro("ZEC", "Zacarias", 14), new Livro("MAL", "Malaquias", 4),
            new Livro("MAT", "Mateus", 28), new Livro("MRK", "Marcos", 16), new Livro("LUK", "Lucas", 24),
            new Livro("JHN", "João", 21), new Livro("ACT", "Atos", 28), new Livro("ROM", "Romanos", 16),
            new Livro("1CO", "1 Coríntios", 16), new Livro("2CO", "2 Coríntios", 13), new Livro("GAL", "Gálatas", 6),
            new Livro("EPH", "Efésios", 6), new Livro("PHP", "Filipenses", 4), new Livro("COL", "Colossenses", 4),
            new Livro("1TH", "1 Tessalonicenses", 5), new Livro("2TH", "2 Tessalonicenses", 3),
            new Livro("1TI", "1 Timóteo", 6), new Livro("2TI", "2 Timóteo", 4), new Livro("TIT", "Tito", 3),
            new Livro("PHM", "Filemom", 1), new Livro("HEB", "Hebreus", 13), new Livro("JAS", "Tiago", 5),
            new Livro("1PE", "1 Pedro", 5), new Livro("2PE", "2 Pedro", 3), new Livro("1JN", "1 João", 5),
            new Livro("2JN", "2 João", 1), new Livro("3JN", "3 João", 1), new Livro("JUD", "Judas", 1),
            new Livro("REV", "Apocalipse", 22));

    private Livros() {
    }

    public static Optional<Livro> porCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        String procurado = codigo.trim().toUpperCase();
        return TODOS.stream().filter(l -> l.codigo().equals(procurado)).findFirst();
    }
}
