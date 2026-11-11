package com.accordi.chitarra;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {

    // ---------- Temi, colori e caratteri disponibili ----------

    static class Tema {
        final String nome;
        final int sfondo, carta, testo, grigio, corde, tasti;
        Tema(String nome, String sfondo, String carta, String testo, String grigio, String corde, String tasti) {
            this.nome = nome;
            this.sfondo = Color.parseColor(sfondo);
            this.carta = Color.parseColor(carta);
            this.testo = Color.parseColor(testo);
            this.grigio = Color.parseColor(grigio);
            this.corde = Color.parseColor(corde);
            this.tasti = Color.parseColor(tasti);
        }
    }

    static final Tema[] TEMI = {
        new Tema("Notte",     "#14161B", "#1F232B", "#F2F2F2", "#9AA0AA", "#C9CDD3", "#6B7280"),
        new Tema("Chiaro",    "#F2F2EF", "#FFFFFF", "#1C1C1C", "#6B6B6B", "#4A4A4A", "#A3A3A3"),
        new Tema("Legno",     "#2B1D14", "#3A281C", "#F5E6D3", "#C2A88D", "#E8D9C0", "#8C6E52"),
        new Tema("Oceano",    "#0B1E2D", "#12304A", "#E6F4FF", "#8FB3CC", "#CFE6F5", "#4E7391"),
        new Tema("Seppia",    "#F3E9D2", "#FBF5E6", "#3B2F1E", "#7A6A50", "#5A4A33", "#B8A585"),
        new Tema("Bosco",     "#102018", "#1A3326", "#E8F5EC", "#93B5A0", "#D2E8DA", "#557A63"),
        new Tema("Contrasto", "#000000", "#141414", "#FFFFFF", "#D0D0D0", "#FFFFFF", "#9A9A9A"),
    };

    static final String[] NOMI_ACCENTI = {"Arancio", "Rosso", "Verde", "Azzurro", "Blu", "Viola", "Rosa", "Giallo"};
    static final int[] ACCENTI = {
        Color.parseColor("#FF9F1C"), Color.parseColor("#EF4444"), Color.parseColor("#22C55E"),
        Color.parseColor("#38BDF8"), Color.parseColor("#2563EB"), Color.parseColor("#A78BFA"),
        Color.parseColor("#F472B6"), Color.parseColor("#FACC15"),
    };

    static final String[] NOMI_FONT = {"Normale", "Serif", "Monospace", "Condensato", "Sottile", "Casual", "Corsivo"};
    static final String[] FAMIGLIE = {"sans-serif", "serif", "monospace", "sans-serif-condensed", "sans-serif-light", "casual", "cursive"};

    // ---------- Impostazioni correnti (lette anche dal disegno degli schemi) ----------

    static int SFONDO, CARTA, TESTO, GRIGIO, CORDE, TASTI, ACCENTO, SU_ACCENTO;
    static final int ROSSO_X = Color.parseColor("#EF4444");
    static Typeface FONT = Typeface.DEFAULT, FONT_B = Typeface.DEFAULT_BOLD;
    static float scalaTesto = 1f, scalaImm = 1f;
    static boolean orizzontale = false, mancino = false, italiano = false;
    static int colonne = 2;

    int temaI, accentoI, fontI, testoPerc, immPerc;
    SharedPreferences pref;

    int tonica = 0;
    int tipo = 0;
    boolean inImpostazioni = false;

    final List<TextView> bottoniNote = new ArrayList<>();
    final List<TextView> bottoniTipi = new ArrayList<>();
    TextView titolo, nome, note;
    LinearLayout listaSchemi;
    ScrollView scrollImpostazioni;

    static AudioTrack tracciaAttuale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = getSharedPreferences("impostazioni", MODE_PRIVATE);
        carica();
        mostraPrincipale();
    }

    @Override
    public void onBackPressed() {
        if (inImpostazioni) mostraPrincipale();
        else super.onBackPressed();
    }

    // ---------- Salvataggio impostazioni ----------

    void carica() {
        temaI = limita(pref.getInt("tema", 0), 0, TEMI.length - 1);
        accentoI = limita(pref.getInt("accento", 0), 0, ACCENTI.length - 1);
        fontI = limita(pref.getInt("font", 0), 0, FAMIGLIE.length - 1);
        testoPerc = limita(pref.getInt("testo", 100), 70, 160);
        immPerc = limita(pref.getInt("immagini", 100), 60, 180);
        orizzontale = pref.getBoolean("orizzontale", false);
        mancino = pref.getBoolean("mancino", false);
        italiano = pref.getBoolean("italiano", false);
        colonne = limita(pref.getInt("colonne", 2), 1, 3);
        tonica = limita(pref.getInt("tonica", 0), 0, 11);
        tipo = limita(pref.getInt("tipo", 0), 0, Accordi.TIPI.length - 1);
        applica();
    }

    void salva() {
        pref.edit()
            .putInt("tema", temaI).putInt("accento", accentoI).putInt("font", fontI)
            .putInt("testo", testoPerc).putInt("immagini", immPerc)
            .putBoolean("orizzontale", orizzontale).putBoolean("mancino", mancino)
            .putBoolean("italiano", italiano).putInt("colonne", colonne)
            .putInt("tonica", tonica).putInt("tipo", tipo)
            .apply();
        applica();
    }

    void applica() {
        Tema t = TEMI[temaI];
        SFONDO = t.sfondo; CARTA = t.carta; TESTO = t.testo; GRIGIO = t.grigio;
        CORDE = t.corde; TASTI = t.tasti;
        ACCENTO = ACCENTI[accentoI];
        SU_ACCENTO = luminosita(ACCENTO) > 0.55 ? Color.parseColor("#1A1A1A") : Color.WHITE;
        FONT = Typeface.create(FAMIGLIE[fontI], Typeface.NORMAL);
        FONT_B = Typeface.create(FAMIGLIE[fontI], Typeface.BOLD);
        scalaTesto = testoPerc / 100f;
        scalaImm = immPerc / 100f;
        try { getWindow().setStatusBarColor(SFONDO); } catch (Exception ignored) { }
    }

    static double luminosita(int c) {
        return (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 255.0;
    }

    static int limita(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    // ---------- Schermata principale ----------

    void mostraPrincipale() {
        inImpostazioni = false;
        bottoniNote.clear();
        bottoniTipi.clear();

        LinearLayout radice = new LinearLayout(this);
        radice.setOrientation(LinearLayout.VERTICAL);
        radice.setBackgroundColor(SFONDO);
        radice.setPadding(dp(12), dp(12), dp(12), 0);

        LinearLayout testata = new LinearLayout(this);
        testata.setGravity(Gravity.CENTER_VERTICAL);
        TextView app = testo("Accordi per Chitarra", 22, TESTO, true);
        testata.addView(app, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView stile = bottone("⚙  Stile");
        stile.setOnClickListener(v -> mostraImpostazioni());
        testata.addView(stile);
        radice.addView(testata);

        radice.addView(etichetta("NOTA"));
        LinearLayout rigaNote = new LinearLayout(this);
        for (int i = 0; i < 12; i++) {
            final int n = i;
            TextView b = bottone(Accordi.TONICHE[i] + "\n" + Accordi.TONICHE_IT[i]);
            b.setOnClickListener(v -> { tonica = n; salva(); aggiorna(); });
            bottoniNote.add(b);
            rigaNote.addView(b);
        }
        radice.addView(scorrevole(rigaNote));

        radice.addView(etichetta("TIPO DI ACCORDO"));
        LinearLayout rigaTipi = new LinearLayout(this);
        for (int i = 0; i < Accordi.TIPI.length; i++) {
            final int t = i;
            String s = Accordi.TIPI[i].sigla.isEmpty() ? "Magg." : Accordi.TIPI[i].sigla;
            TextView b = bottone(s);
            b.setOnClickListener(v -> { tipo = t; salva(); aggiorna(); });
            bottoniTipi.add(b);
            rigaTipi.addView(b);
        }
        radice.addView(scorrevole(rigaTipi));

        titolo = testo("", 34, ACCENTO, true);
        titolo.setPadding(0, dp(10), 0, 0);
        radice.addView(titolo);
        nome = testo("", 16, TESTO, false);
        radice.addView(nome);
        note = testo("", 14, GRIGIO, false);
        note.setPadding(0, dp(2), 0, dp(8));
        radice.addView(note);

        ScrollView scroll = new ScrollView(this);
        listaSchemi = new LinearLayout(this);
        listaSchemi.setOrientation(LinearLayout.VERTICAL);
        listaSchemi.setPadding(0, 0, 0, dp(24));
        scroll.addView(listaSchemi);
        radice.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(radice);
        aggiorna();
    }

    void aggiorna() {
        for (int i = 0; i < bottoniNote.size(); i++) evidenzia(bottoniNote.get(i), i == tonica);
        for (int i = 0; i < bottoniTipi.size(); i++) evidenzia(bottoniTipi.get(i), i == tipo);

        Accordi.Tipo t = Accordi.TIPI[tipo];
        titolo.setText(Accordi.sigla(tonica, t));
        nome.setText(Accordi.TONICHE_IT[tonica] + " " + t.nome);
        note.setText("Note: " + Accordi.note(tonica, t));

        listaSchemi.removeAllViews();
        List<Accordi.Posizione> pos = Accordi.posizioni(tonica, t);
        LinearLayout riga = null;
        for (int i = 0; i < pos.size(); i++) {
            if (i % colonne == 0) {
                riga = new LinearLayout(this);
                listaSchemi.addView(riga);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            riga.addView(scheda(pos.get(i), i, t, true), lp);
        }
        // Riempie la riga incompleta per tenere le schede della stessa larghezza
        int resto = pos.size() % colonne;
        if (resto != 0 && riga != null) {
            for (int i = resto; i < colonne; i++) {
                View vuoto = new View(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 1, 1f);
                lp.setMargins(dp(4), dp(4), dp(4), dp(4));
                riga.addView(vuoto, lp);
            }
        }
    }

    int altezzaSchema() {
        return dp((orizzontale ? 125 : 200) * scalaImm);
    }

    View scheda(final Accordi.Posizione p, int indice, Accordi.Tipo t, boolean conSuona) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(8), dp(8), dp(8), dp(8));
        c.setBackground(sfondoArrotondato(CARTA, 14));

        boolean vuote = false;
        for (int x : p.tasti) if (x == 0) vuote = true;
        String dove = (vuote || p.tastoMin <= 1) ? "Prima posizione" : "Dal " + p.tastoMin + "° tasto";
        TextView intest = testo((indice + 1) + ". " + dove, 13, TESTO, true);
        intest.setGravity(Gravity.CENTER);
        c.addView(intest);

        SchemaView sv = new SchemaView(this, p, tonica, Accordi.usaBemolli(tonica, t));
        c.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, altezzaSchema()));

        TextView tab = testo(p.testo() + (p.barre > 0 ? "  •  barrè" : ""), 12, GRIGIO, false);
        tab.setGravity(Gravity.CENTER);
        c.addView(tab);

        View.OnClickListener play = v -> suona(p.noteMidi());
        sv.setOnClickListener(play);
        if (conSuona) {
            TextView suona = bottone("▶  Suona");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(6), 0, 0);
            suona.setLayoutParams(lp);
            suona.setOnClickListener(play);
            c.addView(suona);
        }
        return c;
    }

    // ---------- Schermata "Personalizza" ----------

    void mostraImpostazioni() {
        inImpostazioni = true;

        LinearLayout radice = new LinearLayout(this);
        radice.setOrientation(LinearLayout.VERTICAL);
        radice.setBackgroundColor(SFONDO);
        radice.setPadding(dp(12), dp(12), dp(12), 0);

        LinearLayout testata = new LinearLayout(this);
        testata.setGravity(Gravity.CENTER_VERTICAL);
        TextView indietro = bottone("←  Indietro");
        indietro.setOnClickListener(v -> mostraPrincipale());
        testata.addView(indietro);
        TextView tit = testo("  Personalizza", 22, TESTO, true);
        testata.addView(tit);
        radice.addView(testata);

        // Anteprima dal vivo, sempre visibile in alto
        Accordi.Tipo t = Accordi.TIPI[tipo];
        List<Accordi.Posizione> pos = Accordi.posizioni(tonica, t);
        if (!pos.isEmpty()) {
            radice.addView(etichetta("ANTEPRIMA: " + Accordi.sigla(tonica, t)));
            LinearLayout rigaAnteprima = new LinearLayout(this);
            int quante = Math.min(colonne, pos.size());
            for (int i = 0; i < colonne; i++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(dp(4), 0, dp(4), 0);
                View v = i < quante ? scheda(pos.get(i), i, t, false) : new View(this);
                rigaAnteprima.addView(v, lp);
            }
            radice.addView(rigaAnteprima);
        }

        scrollImpostazioni = new ScrollView(this);
        LinearLayout lista = new LinearLayout(this);
        lista.setOrientation(LinearLayout.VERTICAL);
        lista.setPadding(0, dp(4), 0, dp(32));
        scrollImpostazioni.addView(lista);
        radice.addView(scrollImpostazioni, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Tema colori
        lista.addView(etichetta("TEMA COLORI"));
        LinearLayout rigaTemi = new LinearLayout(this);
        for (int i = 0; i < TEMI.length; i++) {
            final int n = i;
            TextView b = bottone(TEMI[i].nome);
            GradientDrawable g = sfondoArrotondato(TEMI[i].sfondo, 12);
            g.setStroke(dp(i == temaI ? 3 : 1), i == temaI ? ACCENTO : TEMI[i].tasti);
            b.setBackground(g);
            b.setTextColor(TEMI[i].testo);
            b.setOnClickListener(v -> { temaI = n; cambia(); });
            rigaTemi.addView(b);
        }
        lista.addView(scorrevole(rigaTemi));

        // Colore principale
        lista.addView(etichetta("COLORE PRINCIPALE: " + NOMI_ACCENTI[accentoI].toUpperCase()));
        LinearLayout rigaColori = new LinearLayout(this);
        for (int i = 0; i < ACCENTI.length; i++) {
            final int n = i;
            TextView b = new TextView(this);
            b.setGravity(Gravity.CENTER);
            b.setText(i == accentoI ? "✓" : "");
            b.setTextSize(18);
            b.setTypeface(Typeface.DEFAULT_BOLD);
            b.setTextColor(luminosita(ACCENTI[i]) > 0.55 ? Color.parseColor("#1A1A1A") : Color.WHITE);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(ACCENTI[i]);
            if (i == accentoI) g.setStroke(dp(3), TESTO);
            b.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(44));
            lp.setMargins(0, 0, dp(10), 0);
            b.setOnClickListener(v -> { accentoI = n; cambia(); });
            rigaColori.addView(b, lp);
        }
        lista.addView(scorrevole(rigaColori));

        // Carattere
        lista.addView(etichetta("CARATTERE"));
        LinearLayout rigaFont = new LinearLayout(this);
        for (int i = 0; i < NOMI_FONT.length; i++) {
            final int n = i;
            TextView b = bottone(NOMI_FONT[i]);
            b.setTypeface(Typeface.create(FAMIGLIE[i], Typeface.NORMAL));
            evidenzia(b, i == fontI);
            b.setOnClickListener(v -> { fontI = n; cambia(); });
            rigaFont.addView(b);
        }
        lista.addView(scorrevole(rigaFont));

        // Dimensioni
        lista.addView(cursore("DIMENSIONE TESTO", testoPerc, 70, 160, v -> { testoPerc = v; cambia(); }));
        lista.addView(cursore("DIMENSIONE SCHEMI", immPerc, 60, 180, v -> { immPerc = v; cambia(); }));

        // Orientamento
        lista.addView(etichetta("ORIENTAMENTO SCHEMI"));
        lista.addView(scelta(new String[]{"▯  Verticale", "▭  Orizzontale"}, orizzontale ? 1 : 0, n -> {
            boolean nuovo = n == 1;
            if (nuovo != orizzontale) {
                orizzontale = nuovo;
                colonne = orizzontale ? 1 : 2;   // proposta comoda, poi si può cambiare
            }
            cambia();
        }));

        lista.addView(etichetta("SCHEMI PER RIGA"));
        lista.addView(scelta(new String[]{"1", "2", "3"}, colonne - 1, n -> { colonne = n + 1; cambia(); }));

        lista.addView(etichetta("MANO"));
        lista.addView(scelta(new String[]{"Destrorso", "Mancino"}, mancino ? 1 : 0, n -> { mancino = n == 1; cambia(); }));

        lista.addView(etichetta("NOMI DELLE NOTE SOTTO GLI SCHEMI"));
        lista.addView(scelta(new String[]{"C  D  E", "Do  Re  Mi"}, italiano ? 1 : 0, n -> { italiano = n == 1; cambia(); }));

        // Ripristina
        TextView reset = bottone("↺  Ripristina impostazioni originali");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(24), 0, 0);
        reset.setLayoutParams(lp);
        reset.setOnClickListener(v -> {
            temaI = 0; accentoI = 0; fontI = 0; testoPerc = 100; immPerc = 100;
            orizzontale = false; mancino = false; italiano = false; colonne = 2;
            cambia();
        });
        lista.addView(reset);

        setContentView(radice);
    }

    /** Salva e ridisegna la schermata Personalizza, mantenendo la posizione di scorrimento. */
    void cambia() {
        final int y = scrollImpostazioni != null ? scrollImpostazioni.getScrollY() : 0;
        salva();
        mostraImpostazioni();
        scrollImpostazioni.post(() -> scrollImpostazioni.scrollTo(0, y));
    }

    interface AlCambio { void valore(int v); }

    View scelta(String[] voci, int attiva, AlCambio azione) {
        LinearLayout riga = new LinearLayout(this);
        for (int i = 0; i < voci.length; i++) {
            final int n = i;
            TextView b = bottone(voci[i]);
            evidenzia(b, i == attiva);
            b.setOnClickListener(v -> azione.valore(n));
            riga.addView(b);
        }
        return scorrevole(riga);
    }

    View cursore(final String nomeCursore, int valore, final int min, int max, final AlCambio azione) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final TextView et = etichetta(nomeCursore + ": " + valore + "%");
        box.addView(et);
        SeekBar sb = new SeekBar(this);
        sb.setMax((max - min) / 5);
        sb.setProgress((valore - min) / 5);
        sb.setProgressTintList(ColorStateList.valueOf(ACCENTO));
        sb.setThumbTintList(ColorStateList.valueOf(ACCENTO));
        sb.setProgressBackgroundTintList(ColorStateList.valueOf(GRIGIO));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean daUtente) {
                et.setText(nomeCursore + ": " + (min + p * 5) + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) {
                azione.valore(min + s.getProgress() * 5);
            }
        });
        box.addView(sb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(36)));
        return box;
    }

    // ---------- Suono: corda pizzicata (Karplus-Strong) ----------

    static void suona(final int[] midi) {
        new Thread(() -> {
            int sr = 22050;
            int n = (int) (sr * 2.6);
            float[] mix = new float[n];
            Random rnd = new Random();
            int ritardo = (int) (sr * 0.035);
            for (int k = 0; k < midi.length; k++) {
                double f = 440.0 * Math.pow(2, (midi[k] - 69) / 12.0);
                int periodo = Math.max(2, (int) Math.round(sr / f));
                float[] buf = new float[periodo];
                float prec = 0;
                for (int i = 0; i < periodo; i++) {
                    float r = rnd.nextFloat() * 2f - 1f;
                    prec = 0.5f * prec + 0.5f * r;
                    buf[i] = prec;
                }
                int pos = 0;
                for (int i = k * ritardo; i < n; i++) {
                    int a = pos % periodo, b = (pos + 1) % periodo;
                    float v = buf[a];
                    buf[a] = 0.4985f * (buf[a] + buf[b]);
                    mix[i] += v;
                    pos++;
                }
            }
            float max = 0.0001f;
            for (float v : mix) max = Math.max(max, Math.abs(v));
            short[] pcm = new short[n];
            int fade = sr / 4;
            for (int i = 0; i < n; i++) {
                float v = mix[i] / max * 0.85f;
                if (i > n - fade) v *= (n - i) / (float) fade;
                pcm[i] = (short) (v * 32767);
            }
            synchronized (MainActivity.class) {
                if (tracciaAttuale != null) {
                    try { tracciaAttuale.stop(); } catch (Exception ignored) { }
                    tracciaAttuale.release();
                    tracciaAttuale = null;
                }
                try {
                    AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, sr,
                            AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                            n * 2, AudioTrack.MODE_STATIC);
                    at.write(pcm, 0, n);
                    at.play();
                    tracciaAttuale = at;
                } catch (Exception ignored) { }
            }
        }).start();
    }

    // ---------- Aiutanti per l'interfaccia ----------

    int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    GradientDrawable sfondoArrotondato(int colore, int raggio) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(colore);
        g.setCornerRadius(dp(raggio));
        return g;
    }

    TextView testo(String s, float sp, int colore, boolean grassetto) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp * scalaTesto);
        t.setTextColor(colore);
        t.setTypeface(grassetto ? FONT_B : FONT);
        return t;
    }

    TextView etichetta(String s) {
        TextView t = testo(s, 12, GRIGIO, true);
        t.setPadding(dp(2), dp(12), 0, dp(4));
        return t;
    }

    TextView bottone(String s) {
        TextView b = testo(s, 15, TESTO, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(14), dp(8), dp(14), dp(8));
        b.setMinWidth(dp(52));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(6), 0);
        b.setLayoutParams(lp);
        evidenzia(b, false);
        return b;
    }

    void evidenzia(TextView b, boolean attivo) {
        GradientDrawable g = sfondoArrotondato(attivo ? ACCENTO : CARTA, 12);
        if (!attivo) g.setStroke(dp(1), TASTI);
        b.setBackground(g);
        b.setTextColor(attivo ? SU_ACCENTO : TESTO);
    }

    HorizontalScrollView scorrevole(View contenuto) {
        HorizontalScrollView h = new HorizontalScrollView(this);
        h.setHorizontalScrollBarEnabled(false);
        h.addView(contenuto);
        return h;
    }

    // ---------- Disegno dello schema dell'accordo (verticale o orizzontale) ----------

    static class SchemaView extends View {
        static final int TASTI_VISTI = 5;

        final Accordi.Posizione p;
        final int tonica;
        final boolean bemolli;
        final Paint linee = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint pieno = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint scritta = new Paint(Paint.ANTI_ALIAS_FLAG);
        final float d;

        // Geometria calcolata a ogni disegno
        float ox, oy, passoC, passoT, lungo, k;

        SchemaView(Context c, Accordi.Posizione p, int tonica, boolean bemolli) {
            super(c);
            this.p = p;
            this.tonica = tonica;
            this.bemolli = bemolli;
            d = c.getResources().getDisplayMetrics().density;
        }

        /** Posizione orizzontale di una corda (0 = Mi basso) a una certa distanza dal capotasto (in tasti). */
        float px(int corda, float u) {
            if (!orizzontale) {
                int col = mancino ? 5 - corda : corda;
                return ox + col * passoC;
            }
            return mancino ? ox + lungo - u * passoT : ox + u * passoT;
        }

        float py(int corda, float u) {
            if (!orizzontale) return oy + u * passoT;
            return oy + (5 - corda) * passoC;   // Mi cantino in alto, come nelle tablature
        }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight();
            k = d * scalaImm;
            int inizio = (p.tastoMax <= 5) ? 1 : p.tastoMin;

            if (!orizzontale) {
                float sopra = 26 * k, sotto = 22 * k, lato = 28 * k;
                float alt = h - sopra - sotto;
                float larg = Math.min(w - 2 * lato, alt * 1.05f);
                passoC = larg / 5f;
                passoT = alt / TASTI_VISTI;
                lungo = alt;
                ox = (w - larg) / 2f;
                oy = sopra;
            } else {
                float vicinoCapotasto = 26 * k, latoNomi = 34 * k, su = 24 * k, giu = 8 * k;
                float alt = h - su - giu;
                passoC = alt / 5f;
                float larg = Math.min(w - vicinoCapotasto - latoNomi, alt * 2.4f);
                passoT = larg / TASTI_VISTI;
                lungo = larg;
                float totale = vicinoCapotasto + larg + latoNomi;
                float inizioX = (w - totale) / 2f;
                ox = inizioX + (mancino ? latoNomi : vicinoCapotasto);
                oy = su;
            }

            // Tasti
            linee.setStyle(Paint.Style.STROKE);
            linee.setColor(TASTI);
            linee.setStrokeWidth(1.5f * k);
            for (int i = 0; i <= TASTI_VISTI; i++) {
                cv.drawLine(px(0, i), py(0, i), px(5, i), py(5, i), linee);
            }
            // Capotasto o numero del tasto di partenza
            scritta.setTypeface(FONT_B);
            if (inizio == 1) {
                linee.setColor(TESTO);
                linee.setStrokeWidth(5 * k);
                cv.drawLine(px(0, 0), py(0, 0), px(5, 0), py(5, 0), linee);
            } else {
                scritta.setColor(GRIGIO);
                scritta.setTextSize(12 * k);
                String n = inizio + "ª";
                if (!orizzontale) {
                    scritta.setTextAlign(mancino ? Paint.Align.LEFT : Paint.Align.RIGHT);
                    float x = mancino ? ox + 5 * passoC + 8 * k : ox - 8 * k;
                    cv.drawText(n, x, py(0, 0.5f) + 4 * k, scritta);
                } else {
                    scritta.setTextAlign(Paint.Align.CENTER);
                    cv.drawText(n, px(0, 0.5f), oy - 9 * k, scritta);
                }
            }
            // Corde (più spesse quelle basse)
            linee.setColor(CORDE);
            for (int s = 0; s < 6; s++) {
                linee.setStrokeWidth((2.2f - s * 0.25f) * k);
                cv.drawLine(px(s, 0), py(s, 0), px(s, TASTI_VISTI), py(s, TASTI_VISTI), linee);
            }

            // Barrè
            float r = Math.min(passoC, passoT) * 0.36f;
            pieno.setColor(ACCENTO);
            if (p.barre > 0) {
                float u = p.barre - inizio + 0.5f;
                float x1 = px(p.barreDa, u), y1 = py(p.barreDa, u);
                float x2 = px(p.barreA, u), y2 = py(p.barreA, u);
                RectF rect = new RectF(Math.min(x1, x2) - r, Math.min(y1, y2) - r,
                        Math.max(x1, x2) + r, Math.max(y1, y2) + r);
                cv.drawRoundRect(rect, r, r, pieno);
            }

            for (int s = 0; s < 6; s++) {
                int f = p.tasti[s];

                // Corda muta (×) o a vuoto (○), dalla parte del capotasto
                float mx, my;
                if (!orizzontale) { mx = px(s, 0); my = oy - 13 * k; }
                else { mx = mancino ? ox + lungo + 13 * k : ox - 13 * k; my = py(s, 0); }
                if (f < 0) {
                    scritta.setColor(ROSSO_X);
                    scritta.setTypeface(FONT_B);
                    scritta.setTextSize(15 * k);
                    scritta.setTextAlign(Paint.Align.CENTER);
                    cv.drawText("×", mx, my + 5 * k, scritta);
                } else if (f == 0) {
                    linee.setColor(TESTO);
                    linee.setStrokeWidth(1.6f * k);
                    linee.setStyle(Paint.Style.STROKE);
                    cv.drawCircle(mx, my, 5 * k, linee);
                }

                // Pallini con il numero del dito
                if (f > 0) {
                    float u = f - inizio + 0.5f;
                    float x = px(s, u), y = py(s, u);
                    boolean nelBarre = p.barre > 0 && f == p.barre && s >= p.barreDa && s <= p.barreA;
                    if (!nelBarre) cv.drawCircle(x, y, r, pieno);
                    if (p.dita[s] > 0 && (!nelBarre || s == p.barreDa)) {
                        scritta.setColor(SU_ACCENTO);
                        scritta.setTypeface(FONT_B);
                        scritta.setTextSize(r * 1.3f);
                        scritta.setTextAlign(Paint.Align.CENTER);
                        cv.drawText(String.valueOf(p.dita[s]), x, y + r * 0.45f, scritta);
                    }
                }

                // Nome della nota suonata, dalla parte opposta al capotasto
                if (f >= 0) {
                    int classe = (Accordi.ACCORDATURA[s] + f) % 12;
                    scritta.setTypeface(classe == tonica ? FONT_B : FONT);
                    scritta.setTextSize(10.5f * k);
                    scritta.setColor(classe == tonica ? ACCENTO : GRIGIO);
                    String nomeN = Accordi.nomeNota(classe, bemolli, italiano);
                    if (!orizzontale) {
                        scritta.setTextAlign(Paint.Align.CENTER);
                        cv.drawText(nomeN, px(s, 0), oy + lungo + 16 * k, scritta);
                    } else {
                        scritta.setTextAlign(mancino ? Paint.Align.RIGHT : Paint.Align.LEFT);
                        float x = mancino ? ox - 7 * k : ox + lungo + 7 * k;
                        cv.drawText(nomeN, x, py(s, 0) + 4 * k, scritta);
                    }
                }
            }
        }
    }
}
