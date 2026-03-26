package com.MariaBermudez.GUI;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Estilos {
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
                BorderFactory.createLineBorder(MORADO_ELECTRICO, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
    }

    public static Border crearBordeNeon(String titulo) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(MORADO_ELECTRICO, 1),
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
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = 15;

            Color c1, c2;
            if (isEnabled()) {
                c1 = principal ? MORADO_ELECTRICO : PANEL_INTERNO.brighter();
                c2 = principal ? MORADO_ELECTRICO.darker() : PANEL_INTERNO;
            } else {
                c1 = Color.DARK_GRAY;
                c2 = Color.BLACK;
            }

            g2.setPaint(new GradientPaint(0, 0, c1, 0, h, c2));
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, arc, arc));

            g2.setColor(principal ? Color.WHITE : MORADO_ELECTRICO);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, arc, arc));

            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(getText())) / 2;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(Color.WHITE);
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(150, 40);
        }
    }
}