package com.accordi.chitarra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Motore degli accordi: calcola le diteggiature reali sulla chitarra
 * (accordatura standard Mi La Re Sol Si Mi) per ogni nota e tipo di accordo.
 * Non usa nessuna libreria Android.
 */
public class Accordi {

    // Accordatura standard, dalla 6a corda (Mi basso) alla 1a (Mi cantino), in MIDI
    public static final int[] ACCORDATURA = {40, 45, 50, 55, 59, 64};

    public static final String[] TONICHE = {"C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"};
    public static final String[] TONICHE_IT = {"Do", "Do#", "Re", "Mib", "Mi", "Fa", "Fa#", "Sol", "Lab", "La", "Sib", "Si"};

    private static final String[] DIESIS = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final String[] BEMOLLI = {"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"};
    private static final String[] DIESIS_IT = {"Do", "Do#", "Re", "Re#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si"};
    private static final String[] BEMOLLI_IT = {"Do", "Reb", "Re", "Mib", "Mi", "Fa", "Solb", "Sol", "Lab", "La", "Sib", "Si"};

    /** Un tipo di accordo: sigla, nome italiano, intervalli e quali si possono omettere. */
    public static class Tipo {
        public final String sigla, nome;
        public final int[] intervalli;   // semitoni dalla tonica
        public final int[] omettibili;   // intervalli che si possono non suonare
        public final boolean minore;
        Tipo(String sigla, String nome, boolean minore, int[] intervalli, int[] omettibili) {
            this.sigla = sigla; this.nome = nome; this.minore = minore;
            this.intervalli = intervalli; this.omettibili = omettibili;
        }
    }

    private static final int[] NIENTE = {};
    private static final int[] QUINTA = {7};

    public static final Tipo[] TIPI = {
        new Tipo("",      "maggiore",                false, new int[]{0, 4, 7}, NIENTE),
        new Tipo("m",     "minore",                  true,  new int[]{0, 3, 7}, NIENTE),
        new Tipo("7",     "settima (di dominante)",  false, new int[]{0, 4, 7, 10}, QUINTA),
        new Tipo("maj7",  "settima maggiore",        false, new int[]{0, 4, 7, 11}, QUINTA),
        new Tipo("m7",    "minore settima",          true,  new int[]{0, 3, 7, 10}, QUINTA),
        new Tipo("m(maj7)", "minore settima maggiore", true, new int[]{0, 3, 7, 11}, QUINTA),
        new Tipo("6",     "sesta",                   false, new int[]{0, 4, 7, 9}, QUINTA),
        new Tipo("m6",    "minore sesta",            true,  new int[]{0, 3, 7, 9}, QUINTA),
        new Tipo("sus2",  "seconda sospesa",         false, new int[]{0, 2, 7}, NIENTE),
        new Tipo("sus4",  "quarta sospesa",          false, new int[]{0, 5, 7}, NIENTE),
        new Tipo("7sus4", "settima quarta sospesa",  false, new int[]{0, 5, 7, 10}, QUINTA),
        new Tipo("add9",  "maggiore nona aggiunta",  false, new int[]{0, 4, 7, 2}, NIENTE),
        new Tipo("madd9", "minore nona aggiunta",    true,  new int[]{0, 3, 7, 2}, NIENTE),
        new Tipo("9",     "nona",                    false, new int[]{0, 4, 7, 10, 2}, QUINTA),
        new Tipo("m9",    "minore nona",             true,  new int[]{0, 3, 7, 10, 2}, QUINTA),
        new Tipo("maj9",  "nona maggiore",           false, new int[]{0, 4, 7, 11, 2}, QUINTA),
        new Tipo("6/9",   "sesta e nona",            false, new int[]{0, 4, 7, 9, 2}, QUINTA),
        new Tipo("7b9",   "settima nona bemolle",    false, new int[]{0, 4, 7, 10, 1}, QUINTA),
        new Tipo("7#9",   "settima nona aumentata",  false, new int[]{0, 4, 7, 10, 3}, QUINTA),
        new Tipo("11",    "undicesima",              false, new int[]{0, 7, 10, 2, 5}, new int[]{7, 2}),
        new Tipo("13",    "tredicesima",             false, new int[]{0, 4, 10, 9, 7, 2}, new int[]{7, 2}),
        new Tipo("dim",   "diminuito",               true,  new int[]{0, 3, 6}, NIENTE),
        new Tipo("dim7",  "settima diminuita",       true,  new int[]{0, 3, 6, 9}, NIENTE),
        new Tipo("m7b5",  "semidiminuito",           true,  new int[]{0, 3, 6, 10}, NIENTE),
        new Tipo("aug",   "aumentato",               false, new int[]{0, 4, 8}, NIENTE),
        new Tipo("7#5",   "settima quinta aumentata", false, new int[]{0, 4, 8, 10}, NIENTE),
        new Tipo("5",     "bicordo (power chord)",   false, new int[]{0, 7}, NIENTE),
    };

    /** Una diteggiatura: tasto per ogni corda (-1 = non suonare, 0 = a vuoto). */
    public static class Posizione {
        public final int[] tasti = new int[6];
        public final int[] dita = new int[6];   // 0 = nessun dito
        public int barre = 0;                   // tasto del barrè, 0 = nessuno
        public int barreDa = -1, barreA = -1;   // corde coperte dal barrè (indici 0..5)
        public int tastoMin = 0, tastoMax = 0;  // tra le corde premute
        public int punteggio;

        public int[] noteMidi() {
            int n = 0;
            for (int t : tasti) if (t >= 0) n++;
            int[] r = new int[n];
            int k = 0;
            for (int i = 0; i < 6; i++) if (tasti[i] >= 0) r[k++] = ACCORDATURA[i] + tasti[i];
            return r;
        }

        public String chiave() {
            StringBuilder sb = new StringBuilder();
            for (int t : tasti) sb.append(t).append(',');
            return sb.toString();
        }

        /** Es. "x32010" */
        public String testo() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                if (tasti[i] < 0) sb.append('x');
                else if (tasti[i] > 9) sb.append('(').append(tasti[i]).append(')');
                else sb.append(tasti[i]);
            }
            return sb.toString();
        }
    }

    public static boolean usaBemolli(int tonica, Tipo tipo) {
        String n = TONICHE[tonica];
        if (n.contains("b") || n.equals("F")) return true;
        if (tipo.minore && (n.equals("C") || n.equals("D") || n.equals("G"))) return true;
        return false;
    }

    public static String nomeNota(int classe, boolean bemolli, boolean italiano) {
        classe = ((classe % 12) + 12) % 12;
        if (italiano) return bemolli ? BEMOLLI_IT[classe] : DIESIS_IT[classe];
        return bemolli ? BEMOLLI[classe] : DIESIS[classe];
    }

    public static String sigla(int tonica, Tipo tipo) {
        return TONICHE[tonica] + tipo.sigla;
    }

    /** Note dell'accordo, es. "C E G  (Do Mi Sol)" */
    public static String note(int tonica, Tipo tipo) {
        boolean b = usaBemolli(tonica, tipo);
        StringBuilder en = new StringBuilder(), it = new StringBuilder();
        for (int i = 0; i < tipo.intervalli.length; i++) {
            int c = tonica + tipo.intervalli[i];
            if (i > 0) { en.append(" - "); it.append(" - "); }
            en.append(nomeNota(c, b, false));
            it.append(nomeNota(c, b, true));
        }
        return en + "   (" + it + ")";
    }

    private static final Map<String, List<Posizione>> cache = new HashMap<>();

    /** Tutte le diteggiature utili per l'accordo, ordinate dalla più bassa sul manico. */
    public static synchronized List<Posizione> posizioni(int tonica, Tipo tipo) {
        String k = tonica + "|" + tipo.sigla;
        List<Posizione> c = cache.get(k);
        if (c != null) return c;
        c = calcola(tonica, tipo);
        cache.put(k, c);
        return c;
    }

    private static boolean contiene(int[] a, int v) {
        for (int x : a) if (x == v) return true;
        return false;
    }

    private static List<Posizione> calcola(int tonica, Tipo tipo) {
        boolean[] nellAccordo = new boolean[12];
        for (int iv : tipo.intervalli) nellAccordo[(tonica + iv) % 12] = true;

        int minCorde = tipo.intervalli.length <= 2 ? 2 : (tipo.intervalli.length >= 5 ? 4 : 4);
        if (tipo.intervalli.length == 2) minCorde = 2;

        Map<String, Posizione> trovate = new HashMap<>();

        // Finestre di 4 tasti: base 1 (con corde a vuoto), poi 2..13
        for (int base = 1; base <= 13; base++) {
            List<int[]> opzioni = new ArrayList<>();
            for (int s = 0; s < 6; s++) {
                List<Integer> o = new ArrayList<>();
                o.add(-1);
                if (base == 1 && nellAccordo[ACCORDATURA[s] % 12]) o.add(0);
                for (int f = base; f <= base + 3; f++) {
                    if (nellAccordo[(ACCORDATURA[s] + f) % 12]) o.add(f);
                }
                int[] arr = new int[o.size()];
                for (int i = 0; i < arr.length; i++) arr[i] = o.get(i);
                opzioni.add(arr);
            }
            int[] cur = new int[6];
            esplora(0, opzioni, cur, tonica, tipo, minCorde, trovate);
        }

        // Ordina per qualità, poi scegli posizioni distanti tra loro sul manico
        List<Posizione> tutte = new ArrayList<>(trovate.values());
        Collections.sort(tutte, new Comparator<Posizione>() {
            @Override public int compare(Posizione a, Posizione b) {
                if (a.punteggio != b.punteggio) return b.punteggio - a.punteggio;
                return zona(a) - zona(b);
            }
        });
        List<Posizione> scelte = new ArrayList<>();
        for (Posizione p : tutte) {
            if (scelte.size() >= 7) break;
            int z = zona(p);
            boolean vicina = false;
            for (Posizione q : scelte) {
                int zq = zona(q);
                if (z == 0 && zq == 0) vicina = true;
                else if (z != 0 && zq != 0 && Math.abs(z - zq) < 2) vicina = true;
            }
            if (!vicina) scelte.add(p);
        }
        Collections.sort(scelte, new Comparator<Posizione>() {
            @Override public int compare(Posizione a, Posizione b) { return zona(a) - zona(b); }
        });
        return scelte;
    }

    private static int zona(Posizione p) {
        for (int t : p.tasti) if (t == 0) return 0;
        return p.tastoMin;
    }

    private static void esplora(int s, List<int[]> opzioni, int[] cur, int tonica, Tipo tipo,
                                int minCorde, Map<String, Posizione> trovate) {
        if (s == 6) {
            Posizione p = valuta(cur, tonica, tipo, minCorde);
            if (p != null) {
                Posizione vecchia = trovate.get(p.chiave());
                if (vecchia == null) trovate.put(p.chiave(), p);
            }
            return;
        }
        for (int f : opzioni.get(s)) {
            cur[s] = f;
            esplora(s + 1, opzioni, cur, tonica, tipo, minCorde, trovate);
        }
    }

    private static Posizione valuta(int[] t, int tonica, Tipo tipo, int minCorde) {
        // Le corde mute sono ammesse solo dal basso (6a, 5a...) e al massimo il Mi cantino in alto
        int primo = 0;
        while (primo < 6 && t[primo] < 0) primo++;
        if (primo >= 6) return null;
        int ultimo = 5;
        while (ultimo > primo && t[ultimo] < 0) ultimo--;
        if (ultimo < 4 && tipo.intervalli.length > 2) return null;                 // il Mi cantino può essere muto, il Si no
        for (int i = primo; i <= ultimo; i++) if (t[i] < 0) return null;   // niente corde mute in mezzo

        int corde = ultimo - primo + 1;
        if (corde < minCorde) return null;
        if (tipo.intervalli.length == 2 && (corde > 3 || primo > 2)) return null;  // bicordo: 2-3 corde basse

        // Il basso deve essere la tonica
        if ((ACCORDATURA[primo] + t[primo]) % 12 != tonica) return null;

        // Tutte le note obbligatorie devono esserci
        boolean[] presente = new boolean[12];
        for (int i = primo; i <= ultimo; i++) presente[(ACCORDATURA[i] + t[i]) % 12] = true;
        boolean completa = true;
        for (int iv : tipo.intervalli) {
            if (!presente[(tonica + iv) % 12]) {
                if (contiene(tipo.omettibili, iv)) completa = false;
                else return null;
            }
        }

        // Estensione della mano
        int min = 99, max = 0, premute = 0, vuote = 0;
        for (int i = primo; i <= ultimo; i++) {
            if (t[i] > 0) { premute++; min = Math.min(min, t[i]); max = Math.max(max, t[i]); }
            else if (t[i] == 0) vuote++;
        }
        if (premute == 0) { min = 0; max = 0; }
        if (max - min > 3) return null;
        if (vuote > 0 && max > 4) return null;

        Posizione p = new Posizione();
        System.arraycopy(t, 0, p.tasti, 0, 6);
        p.tastoMin = min;
        p.tastoMax = max;

        // Dita: prova senza barrè, poi con barrè sul tasto più basso
        int dita;
        int contaMin = 0;
        for (int i = primo; i <= ultimo; i++) if (t[i] == min && min > 0) contaMin++;
        boolean barre = false;
        int bDa = -1, bA = -1;
        if (premute > 4 || (contaMin >= 2 && premute > 3) || (contaMin >= 3)) {
            // barrè: dalla prima corda con il tasto minimo fino all'ultima corda suonata
            if (contaMin >= 2) {
                bDa = -1;
                for (int i = primo; i <= ultimo; i++) if (t[i] == min) { bDa = i; break; }
                bA = -1;
                for (int i = ultimo; i >= primo; i--) if (t[i] == min) { bA = i; break; }
                boolean ok = true;
                for (int i = bDa; i <= bA; i++) if (t[i] < min) ok = false;
                if (ok) {
                    barre = true;
                    // Il barrè di solito copre fino al Mi cantino se suonato
                    if (t[ultimo] >= min) {
                        boolean fino = true;
                        for (int i = bA; i <= ultimo; i++) if (t[i] < min) fino = false;
                        if (fino) bA = ultimo;
                    }
                }
            }
        }
        if (barre) {
            int altre = 0;
            for (int i = primo; i <= ultimo; i++) if (t[i] > min) altre++;
            dita = 1 + altre;
        } else {
            dita = premute;
        }
        if (dita > 4) return null;

        // Assegna i numeri delle dita
        assegnaDita(p, barre, primo, ultimo);
        if (barre) { p.barre = min; p.barreDa = bDa; p.barreA = bA; }

        // Punteggio: più corde, meno dita, accordo completo, posizione bassa
        int punti = corde * 4 - dita * 2;
        if (completa) punti += 4;
        if (barre) punti -= 1;
        punti += vuote;
        if (primo <= 1) punti += 2;
        if (min > 0) punti -= min / 4;
        if (vuote > 0) punti -= max * 2;   // in prima posizione preferisci le forme classiche
        // la quinta presente nei triadi
        p.punteggio = punti;
        return p;
    }

    private static void assegnaDita(Posizione p, boolean barre, int primo, int ultimo) {
        int[] t = p.tasti;
        int min = p.tastoMin;
        // Ordina le note premute per tasto, poi per corda (dal basso)
        List<Integer> idx = new ArrayList<>();
        for (int i = primo; i <= ultimo; i++) {
            if (t[i] > 0 && !(barre && t[i] == min)) idx.add(i);
        }
        Collections.sort(idx, new Comparator<Integer>() {
            @Override public int compare(Integer a, Integer b) {
                if (p.tasti[a] != p.tasti[b]) return p.tasti[a] - p.tasti[b];
                return a - b;
            }
        });
        int dito = barre ? 2 : 1;
        // Se le note distano molto, salta un dito (es. tasto 1 e 3 -> dita 1 e 3)
        int ultimoTasto = barre ? min : -1;
        int liberi = 4 - (barre ? 1 : 0) - idx.size();
        for (int i : idx) {
            if (ultimoTasto > 0 && t[i] - ultimoTasto >= 2 && liberi > 0) { dito++; liberi--; }
            p.dita[i] = Math.min(dito, 4);
            dito++;
            ultimoTasto = t[i];
        }
        if (barre) for (int i = primo; i <= ultimo; i++) if (t[i] == min) p.dita[i] = 1;
    }
}
