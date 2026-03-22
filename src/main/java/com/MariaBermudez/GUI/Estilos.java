package com.MariaBermudez.GUI;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Estilos {
    // Paleta: Deep Amethyst & Electric Violet
    public static final Color FONDO_OBSCURO = new Color(15, 10, 25);
    public static final Color PANEL_INTERNO = new Color(30, 20, 45);
    public static final Color MORADO_ELECTRICO = new Color(180, 70, 255);
    public static final Color CIAN_CONTRASTE = new Color(0, 255, 200);
    public static final Color TEXTO_OPACO = new Color(180, 160, 200);

    public static final Font FUENTE_TITULO = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FUENTE_NORMAL = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FUENTE_MONO = new Font("Consolas", Font.BOLD, 13);

    public static void aplicar(JFrame frame) {
        frame.getContentPane().setBackground(FONDO_OBSCURO);
    }

    public static JLabel crearLabel(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(TEXTO_OPACO);
        label.setFont(FUENTE_TITULO);
        return label;
    }

    public static void estilizarCampo(JTextField campo) {
        campo.setBackground(PANEL_INTERNO);
        campo.setForeground(Color.WHITE);
        campo.setCaretColor(MORADO_ELECTRICO);
        campo.setFont(FUENTE_NORMAL);
        campo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(MORADO_ELECTRICO, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
    }

    public static Border crearBordeNeon(String titulo) {
        return BorderFactory.createTitledBorder(
                new LineBorder(MORADO_ELECTRICO, 1),
                " " + titulo + " ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                FUENTE_TITULO,
                MORADO_ELECTRICO
        );
    }

    public static JButton crearBoton(String texto, boolean principal) {
        GradientButton btn = new GradientButton(texto, principal);
        btn.setFont(FUENTE_TITULO);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(140, 36));
        return btn;
    }

    private static class GradientButton extends JButton {
        private final boolean principal;

        GradientButton(String text, boolean principal) {
            super(text);
            this.principal = principal;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setBorder(principal ? BorderFactory.createEmptyBorder(6, 14, 6, 14)
                    : new LineBorder(MORADO_ELECTRICO, 1, true));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = Math.max(12, h / 2);

            Color c1, c2, borderColor;
            if (isEnabled()) {
                if (principal) {
                    c1 = MORADO_ELECTRICO.brighter();
                    c2 = MORADO_ELECTRICO.darker();
                    borderColor = MORADO_ELECTRICO.darker();
                } else {
                    c1 = PANEL_INTERNO.brighter();
                    c2 = PANEL_INTERNO;
                    borderColor = MORADO_ELECTRICO;
                }
            } else {
                c1 = PANEL_INTERNO;
                c2 = PANEL_INTERNO.darker();
                borderColor = PANEL_INTERNO.darker();
            }

            Shape clip = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, arc, arc);

            g2.setColor(new Color(0, 0, 0, 60));
            g2.fill(new RoundRectangle2D.Float(2, 2, w - 5, h - 4, arc, arc));

            GradientPaint gp = new GradientPaint(0, 0, c1, 0, h, c2);
            g2.setPaint(gp);
            g2.fill(clip);

            g2.setColor(new Color(255, 255, 255, 18));
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h / 2f, arc, arc));

            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(principal ? 0.8f : 1.2f));
            g2.draw(clip);

            FontMetrics fm = g2.getFontMetrics(getFont());
            String text = getText();
            int tx = (w - fm.stringWidth(text)) / 2;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;

            Color textColor = isEnabled() ? Color.WHITE : new Color(160, 150, 170);

            g2.setFont(getFont());
            g2.setColor(new Color(0, 0, 0, 120));
            g2.drawString(text, tx + 1, ty + 1);
            g2.setColor(textColor);
            g2.drawString(text, tx, ty);

            g2.dispose();
        }


        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.width = Math.max(d.width + 20, 110);
            d.height = Math.max(d.height + 8, 36);
            return d;
        }
    }
}