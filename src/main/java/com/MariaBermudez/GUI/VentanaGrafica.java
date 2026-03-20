package com.MariaBermudez.GUI;

import com.MariaBermudez.db.DBManager;
import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.configuracion.CargadorConfig;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;

public class VentanaGrafica extends JFrame {
    private JTextField fldConfig, fldId, fldMuestras;
    private JTextArea consola;
    private JProgressBar barra;
    private JButton btnConectar, btnIniciar;
    private PanelGrafico panelGrafico;

    public VentanaGrafica() {
        Estilos.aplicar(this);
        setTitle("DB Component");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(20, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                Point2D center = new Point2D.Float(w * 0.47f, h * 0.35f);
                float radius = Math.max(w, h) * 0.9f;
                float[] dist = {0f, 1f};
                Color cCenter = Estilos.PANEL_INTERNO.brighter();
                Color cEdge = Estilos.FONDO_OBSCURO.darker();
                RadialGradientPaint rg = new RadialGradientPaint(center, radius, dist, new Color[]{cCenter, cEdge});
                g2.setPaint(rg);
                g2.fillRect(0, 0, w, h);
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillOval(-w/4, -h/2, (int)(w * 1.5f), (int)(h * 1.5f));
                g2.dispose();
            }
        };
        root.setOpaque(true);
        root.setBorder(new EmptyBorder(25, 25, 25, 25));
        setContentPane(root);

        initUI();
        setLocationRelativeTo(null);
    }

    private void initUI() {
        JPanel pnlTop = new JPanel(new GridLayout(1, 3, 20, 0));
        pnlTop.setOpaque(false);

        fldConfig = new JTextField("configMySQL.json");
        fldId = new JTextField("default");
        fldMuestras = new JTextField("500");

        pnlTop.add(crearCaja("Configuración", fldConfig));
        pnlTop.add(crearCaja("Query ID", fldId));
        pnlTop.add(crearCaja("Nº Muestras", fldMuestras));

        JPanel pnlCenter = new JPanel(new BorderLayout(20, 0));
        pnlCenter.setOpaque(false);

        consola = new JTextArea();
        consola.setBackground(Estilos.PANEL_INTERNO);
        consola.setForeground(Color.WHITE);
        consola.setFont(Estilos.FUENTE_MONO);
        consola.setEditable(false);

        JScrollPane scroll = new JScrollPane(consola);
        scroll.setBorder(Estilos.crearBordeNeon("TRAZA DE OPERACIONES"));

        panelGrafico = new PanelGrafico();
        panelGrafico.setPreferredSize(new Dimension(400, 0));
        JPanel chartWrap = new JPanel(new BorderLayout());
        chartWrap.setOpaque(false);
        chartWrap.setBorder(Estilos.crearBordeNeon("RENDIMIENTO (%)"));
        chartWrap.add(panelGrafico);

        pnlCenter.add(scroll, BorderLayout.CENTER);
        pnlCenter.add(chartWrap, BorderLayout.EAST);

        JPanel pnlBottom = new JPanel(new BorderLayout(10, 15));
        pnlBottom.setOpaque(false);

        barra = new JProgressBar();
        barra.setForeground(Estilos.MORADO_ELECTRICO);
        barra.setBackground(Estilos.PANEL_INTERNO);
        barra.setBorder(null);

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlBtns.setOpaque(false);
        btnConectar = Estilos.crearBoton("Conectar", false);
        btnConectar.setToolTipText("Conectar a la de base de datos");
        btnConectar.setMnemonic('C');
        btnIniciar = Estilos.crearBoton("Ejecutar queries", true);
        btnIniciar.setToolTipText("Ejecutar el set de queries seleccionado");
        btnIniciar.setMnemonic('E');
        btnIniciar.setEnabled(false);

        pnlBtns.add(btnConectar);
        pnlBtns.add(btnIniciar);
        pnlBottom.add(barra, BorderLayout.NORTH);
        pnlBottom.add(pnlBtns, BorderLayout.SOUTH);

        add(pnlTop, BorderLayout.NORTH);
        add(pnlCenter, BorderLayout.CENTER);
        add(pnlBottom, BorderLayout.SOUTH);

        btnConectar.addActionListener(e -> conectar());
        btnIniciar.addActionListener(e -> ejecutar());
    }

    private JPanel crearCaja(String txt, JTextField f) {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setOpaque(false);
        Estilos.estilizarCampo(f);
        p.add(Estilos.crearLabel(txt), BorderLayout.NORTH);
        p.add(f, BorderLayout.CENTER);
        return p;
    }

    private void conectar() {
        try {
            Ajustes a = CargadorConfig.leer(fldConfig.getText());
            DBManager.iniciar(a);
            log("SISTEMA: Motor conectado.");
            btnIniciar.setEnabled(true);
        } catch (Exception ex) { log("ERR: " + ex.getMessage()); }
    }

    private void ejecutar() {
        try {
            int limite = Integer.parseInt(fldMuestras.getText());
            btnIniciar.setEnabled(false);
            barra.setMaximum(limite);
            barra.setValue(0);
            panelGrafico.limpiar();
            log("PROCESO: Iniciando test de " + limite + " peticiones...");

            Thread.startVirtualThread(() -> {
                int ok = 0;
                int err = 0;

                for (int i = 1; i <= limite; i++) {
                    try {
                        DBManager.getComponent().query(fldId.getText());
                        ok++;
                    } catch (Exception e) {
                        err++;
                    }

                    final int actual = i;
                    final int finalOk = ok;
                    final int finalErr = err;

                    if (actual % 10 == 0 || actual == limite) {
                        SwingUtilities.invokeLater(() -> {
                            barra.setValue(actual);
                            panelGrafico.actualizar(finalOk, finalErr);
                        });
                    }
                }

                final int totalOk = ok;
                final int totalErr = err;
                SwingUtilities.invokeLater(() -> {
                    log("FIN: EXITOSAS=" + totalOk + " | FALLIDAS=" + totalErr);
                    btnIniciar.setEnabled(true);
                });
            });
        } catch (NumberFormatException e) {
            log("ERROR: El número de muestras debe ser un entero.");
        }
    }

    private void log(String m) { consola.append(" > " + m + "\n"); }
}