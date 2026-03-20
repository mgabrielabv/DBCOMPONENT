package com.MariaBermudez.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;

public class PanelGrafico extends JPanel {
    private int exitos = 0;
    private int fallos = 0;

    public PanelGrafico() {
        setOpaque(false);
        setPreferredSize(new Dimension(220, 220));
    }

    public synchronized void actualizar(int e, int f) {
        this.exitos = e;
        this.fallos = f;
        repaint();
    }

    public void limpiar() {
        this.exitos = 0;
        this.fallos = 0;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int ancho = getWidth();
        int alto = getHeight();
        int size = Math.min(ancho, alto);
        int padding = Math.max(18, size / 8);
        int diametro = size - padding * 2;
        int x = (ancho - diametro) / 2;
        int y = (alto - diametro) / 2;

        float stroke = Math.max(8f, diametro * 0.08f);
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        Color bgRing = Estilos.PANEL_INTERNO;
        Color bgRingSemi = new Color(bgRing.getRed(), bgRing.getGreen(), bgRing.getBlue(), 200);
        g2.setColor(bgRingSemi);
        g2.drawOval(x, y, diametro, diametro);

        int total = exitos + fallos;
        int startAngle = 90;
        if (total > 0) {
            int anguloExito = (int) Math.round(((double) exitos / total) * 360);
            g2.setColor(Estilos.CIAN_CONTRASTE);
            g2.drawArc(x, y, diametro, diametro, startAngle, -anguloExito);
            if (fallos > 0) {
                g2.setColor(new Color(220, 60, 120));
                g2.drawArc(x, y, diametro, diametro, startAngle - anguloExito, -(360 - anguloExito));
            }
        }

        int innerDiam = (int) (diametro - stroke * 1.6);
        int ix = x + (diametro - innerDiam) / 2;
        int iy = y + (diametro - innerDiam) / 2;

        float radius = innerDiam / 2f;
        float[] dist = {0f, 1f};
        Color c1 = Estilos.PANEL_INTERNO.brighter();
        Color c2 = new Color(18, 6, 36);
        RadialGradientPaint rg = new RadialGradientPaint(new Point2D.Float(ix + radius, iy + radius), radius, dist, new Color[]{c1, c2});
        g2.setPaint(rg);
        g2.fillOval(ix, iy, innerDiam, innerDiam);

        GradientPaint gloss = new GradientPaint(0, iy, new Color(255, 255, 255, 50), 0, iy + innerDiam / 2f, new Color(255, 255, 255, 6));
        g2.setPaint(gloss);
        g2.fillOval(ix, iy, innerDiam, innerDiam / 2);

        g2.setColor(Color.WHITE);
        Font titulo = Estilos.FUENTE_TITULO.deriveFont(Font.BOLD, Math.max(18f, innerDiam * 0.22f));
        g2.setFont(titulo);
        String pct = (total == 0) ? "0%" : (exitos * 100 / total) + "%";
        FontMetrics fm = g2.getFontMetrics();
        int px = ix + (innerDiam - fm.stringWidth(pct)) / 2;
        int py = iy + (innerDiam / 2) + (fm.getAscent() / 3);
        g2.drawString(pct, px, py);

        Font etiqueta = Estilos.FUENTE_TITULO.deriveFont(Font.PLAIN, Math.max(10f, innerDiam * 0.09f));
        g2.setFont(etiqueta);
        String label = "Éxito";
        FontMetrics fm2 = g2.getFontMetrics();
        int lx = ix + (innerDiam - fm2.stringWidth(label)) / 2;
        int ly = py + fm2.getHeight();
        g2.setColor(new Color(255, 255, 255, 200));
        g2.drawString(label, lx, ly);

        Font conteos = Estilos.FUENTE_TITULO.deriveFont(Font.PLAIN, Math.max(10f, innerDiam * 0.08f));
        g2.setFont(conteos);
        String counts = exitos + " / " + total;
        FontMetrics fm3 = g2.getFontMetrics();
        int cx = ix + (innerDiam - fm3.stringWidth(counts)) / 2;
        int cy = iy + innerDiam - fm3.getHeight() / 2;
        g2.setColor(new Color(255, 255, 255, 120));
        g2.drawString(counts, cx, cy);

        g2.dispose();
    }
}