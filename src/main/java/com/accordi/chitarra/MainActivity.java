package com.accordi.chitarra;

import android.app.Activity;
import android.content.Context;
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
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {

    static final int SFONDO = Color.parseColor("#14161B");
    static final int CARTA = Color.parseColor("#1F232B");
    static final int TESTO = Color.parseColor("#F2F2F2");
    static final int GRIGIO = Color.parseColor("#9AA0AA");
    static final int ACCENTO = Color.parseColor("#FF9F1C");

    int tonica = 0;
    int tipo = 0;

    final List<TextView> bottoniNote = new ArrayList<>();
    final List<TextView> bottoniTipi = new ArrayList<>();
    TextView titolo, nome, note;
    LinearLayout listaSchemi;

    static AudioTrack tracciaAttuale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(SFONDO);

        LinearLayout radice = new LinearLayout(this);
        radice.setOrientation(LinearLayout.VERTICAL);
        radice.setBackgroundColor(SFONDO);
        radice.setPadding(dp(12), dp(12), dp(12), 0);

        TextView app = testo("Accordi per Chitarra", 22, TESTO, true);
        radice.addView(app);

        radice.addView(etichetta("NOTA"));
        LinearLayout rigaNote = new LinearLayout(this);
        for (int i = 0; i < 12; i++) {
            final int n = i;
            TextView b = bottone(Accordi.TONICHE[i] + "\n" + Accordi.TONICHE_IT[i]);
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { tonica = n; aggiorna(); }
            });
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
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { tipo = t; aggiorna(); }
            });
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
            if (i % 2 == 0) {
                riga = new LinearLayout(this);
                listaSchemi.addView(riga);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            riga.addView(scheda(pos.get(i), i, t), lp);
        }
        if (pos.size() % 2 == 1 && riga != null) {
            View vuoto = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 1, 1f);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            riga.addView(vuoto, lp);
        }
    }

    View scheda(final Accordi.Posizione p, int indice, Accordi.Tipo t) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable g = new GradientDrawable();
        g.setColor(CARTA);
        g.setCornerRadius(dp(14));
        c.setBackground(g);

        String dove;
        boolean vuote = false;
        for (int x : p.tasti) if (x == 0) vuote = true;
        if (vuote || p.tastoMin <= 1) dove = "Prima posizione";
        else dove = "Dal " + p.tastoMin + "° tasto";
        TextView intest = testo((indice + 1) + ". " + dove, 13, TESTO, true);
        intest.setGravity(Gravity.CENTER);
        c.addView(intest);

        SchemaView sv = new SchemaView(this, p, tonica, Accordi.usaBemolli(tonica, t));
        c.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(200)));

        TextView tab = testo(p.testo() + (p.barre > 0 ? "  •  barrè" : ""), 12, GRIGIO, false);
        tab.setGravity(Gravity.CENTER);
        c.addView(tab);

        TextView suona = bottone("▶  Suona");
        suona.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, 0);
        suona.setLayoutParams(lp);
        evidenzia(suona, false);
        View.OnClickListener play = new View.OnClickListener() {
            @Override public void onClick(View v) { suona(p.noteMidi()); }
        };
        suona.setOnClickListener(play);
        sv.setOnClickListener(play);
        c.addView(suona);
        return c;
    }

    // ---------- Suono: corda pizzicata (Karplus-Strong) ----------

    static void suona(final int[] midi) {
        new Thread(new Runnable() {
            @Override public void run() {
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
                    int inizio = k * ritardo;
                    int pos = 0;
                    for (int i = inizio; i < n; i++) {
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
            }
        }).start();
    }

    // ---------- Aiutanti per l'interfaccia ----------

    int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    TextView testo(String s, float sp, int colore, boolean grassetto) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(colore);
        if (grassetto) t.setTypeface(Typeface.DEFAULT_BOLD);
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
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(12));
        g.setColor(attivo ? ACCENTO : CARTA);
        b.setBackground(g);
        b.setTextColor(attivo ? Color.parseColor("#1A1A1A") : TESTO);
    }

    HorizontalScrollView scorrevole(View contenuto) {
        HorizontalScrollView h = new HorizontalScrollView(this);
        h.setHorizontalScrollBarEnabled(false);
        h.addView(contenuto);
        return h;
    }

    // ---------- Disegno dello schema dell'accordo ----------

    static class SchemaView extends View {
        final Accordi.Posizione p;
        final int tonica;
        final boolean bemolli;
        final Paint linee = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint pieno = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint scritta = new Paint(Paint.ANTI_ALIAS_FLAG);
        final float d;

        SchemaView(Context c, Accordi.Posizione p, int tonica, boolean bemolli) {
            super(c);
            this.p = p;
            this.tonica = tonica;
            this.bemolli = bemolli;
            d = c.getResources().getDisplayMetrics().density;
        }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight();
            float sinistra = 30 * d, destra = 14 * d, sopra = 26 * d, sotto = 24 * d;
            float larg = w - sinistra - destra;
            float alt = h - sopra - sotto;
            int tasti = 5;
            float passoC = larg / 5f;
            float passoT = alt / tasti;

            int inizio = (p.tastoMax <= 5) ? 1 : p.tastoMin;

            // Tasti
            linee.setColor(Color.parseColor("#6B7280"));
            linee.setStrokeWidth(1.5f * d);
            for (int i = 0; i <= tasti; i++) {
                float y = sopra + i * passoT;
                cv.drawLine(sinistra, y, sinistra + larg, y, linee);
            }
            // Capotasto
            if (inizio == 1) {
                linee.setColor(TESTO);
                linee.setStrokeWidth(5 * d);
                cv.drawLine(sinistra - 1 * d, sopra, sinistra + larg + 1 * d, sopra, linee);
            } else {
                scritta.setColor(GRIGIO);
                scritta.setTextSize(12 * d);
                scritta.setTextAlign(Paint.Align.RIGHT);
                scritta.setTypeface(Typeface.DEFAULT_BOLD);
                cv.drawText(inizio + "ª", sinistra - 6 * d, sopra + passoT * 0.5f + 4 * d, scritta);
            }
            // Corde (più spesse in basso)
            linee.setColor(Color.parseColor("#C9CDD3"));
            for (int s = 0; s < 6; s++) {
                linee.setStrokeWidth((2.2f - s * 0.25f) * d);
                float x = sinistra + s * passoC;
                cv.drawLine(x, sopra, x, sopra + alt, linee);
            }

            // Barrè
            float r = Math.min(passoC, passoT) * 0.36f;
            if (p.barre > 0) {
                float y = sopra + (p.barre - inizio + 0.5f) * passoT;
                float x1 = sinistra + p.barreDa * passoC, x2 = sinistra + p.barreA * passoC;
                pieno.setColor(ACCENTO);
                cv.drawRoundRect(new RectF(x1 - r, y - r, x2 + r, y + r), r, r, pieno);
            }

            scritta.setTextAlign(Paint.Align.CENTER);
            for (int s = 0; s < 6; s++) {
                float x = sinistra + s * passoC;
                int f = p.tasti[s];
                // Sopra: X (non suonare) o O (a vuoto)
                scritta.setTypeface(Typeface.DEFAULT_BOLD);
                scritta.setTextSize(14 * d);
                if (f < 0) {
                    scritta.setColor(Color.parseColor("#EF4444"));
                    cv.drawText("×", x, sopra - 8 * d, scritta);
                } else if (f == 0) {
                    linee.setColor(TESTO);
                    linee.setStrokeWidth(1.6f * d);
                    linee.setStyle(Paint.Style.STROKE);
                    cv.drawCircle(x, sopra - 13 * d, 5 * d, linee);
                    linee.setStyle(Paint.Style.FILL);
                }
                // Pallini con il numero del dito
                if (f > 0) {
                    float y = sopra + (f - inizio + 0.5f) * passoT;
                    boolean nelBarre = p.barre > 0 && f == p.barre && s >= p.barreDa && s <= p.barreA;
                    if (!nelBarre || s == p.barreDa) {
                        pieno.setColor(ACCENTO);
                        cv.drawCircle(x, y, r, pieno);
                    }
                    if (p.dita[s] > 0 && (!nelBarre || s == p.barreDa)) {
                        scritta.setColor(Color.parseColor("#1A1A1A"));
                        scritta.setTextSize(r * 1.3f);
                        cv.drawText(String.valueOf(p.dita[s]), x, y + r * 0.45f, scritta);
                    }
                }
                // Sotto: nome della nota suonata
                if (f >= 0) {
                    int classe = (Accordi.ACCORDATURA[s] + f) % 12;
                    scritta.setTypeface(Typeface.DEFAULT);
                    scritta.setTextSize(10.5f * d);
                    scritta.setColor(classe == tonica ? ACCENTO : GRIGIO);
                    cv.drawText(Accordi.nomeNota(classe, bemolli, false), x, h - 6 * d, scritta);
                }
            }
        }
    }
}
