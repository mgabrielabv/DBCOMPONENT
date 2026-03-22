package com.MariaBermudez.GUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Point2D;

public class VentanaGrafica extends JFrame {
    private JTextField fldConfig, fldId, fldMuestras;
    private JTextArea consola;
    private JProgressBar barra;
    private JButton btnConectar, btnIniciar;
    private PanelGrafico panelGrafico;
    private final DBController controller = new DBController();

    public VentanaGrafica() {
        Estilos.aplicar(this);
        setTitle("DBComponent");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(20, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth(), h = getHeight();
                RadialGradientPaint rg = new RadialGradientPaint(
                        new Point2D.Float(w / 2, h / 2), Math.max(w, h),
                        new float[]{0f, 1f},
                        new Color[]{new Color(30, 30, 50), new Color(10, 10, 20)}
                );
                g2.setPaint(rg);
                g2.fillRect(0, 0, w, h);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(25, 25, 25, 25));
        setContentPane(root);

        JPanel pnlTop = new JPanel(new GridLayout(1, 3, 20, 0));
        pnlTop.setOpaque(false);
        fldConfig = new JTextField("config.json");
        fldId = new JTextField("test_conexion");
        fldMuestras = new JTextField("100");
        pnlTop.add(crearCaja("Archivo Config", fldConfig));
        pnlTop.add(crearCaja("ID de Consulta", fldId));
        pnlTop.add(crearCaja("Total Muestras", fldMuestras));

        JPanel pnlCenter = new JPanel(new GridLayout(1, 2, 20, 0));
        pnlCenter.setOpaque(false);

        consola = new JTextArea();
        consola.setEditable(false);
        consola.setBackground(new Color(15, 15, 25));
        consola.setForeground(Color.CYAN);
        JScrollPane scroll = new JScrollPane(consola);
        scroll.setBorder(Estilos.crearBordeNeon("Queries y conexiones"));

        panelGrafico = new PanelGrafico();
        panelGrafico.setBorder(Estilos.crearBordeNeon("RENDIMIENTO EN TIEMPO REAL"));

        pnlCenter.add(scroll);
        pnlCenter.add(panelGrafico);

        JPanel pnlBottom = new JPanel(new BorderLayout(10, 10));
        pnlBottom.setOpaque(false);
        barra = new JProgressBar();
        barra.setStringPainted(true);

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlBtns.setOpaque(false);
        btnConectar = Estilos.crearBoton("CONECTAR", false);
        btnIniciar = Estilos.crearBoton("INICIAR", true);
        btnIniciar.setEnabled(false);

        pnlBtns.add(btnConectar);
        pnlBtns.add(btnIniciar);
        pnlBottom.add(barra, BorderLayout.NORTH);
        pnlBottom.add(pnlBtns, BorderLayout.SOUTH);

        add(pnlTop, BorderLayout.NORTH);
        add(pnlCenter, BorderLayout.CENTER);
        add(pnlBottom, BorderLayout.SOUTH);

        btnConectar.addActionListener(e -> controller.conectar(fldConfig.getText(), this::log, () -> btnIniciar.setEnabled(true)));
        btnIniciar.addActionListener(e -> ejecutarPrueba());

        setLocationRelativeTo(null);
    }

    private void ejecutarPrueba() {
        try {
            int total = Integer.parseInt(fldMuestras.getText());
            barra.setMaximum(total);
            barra.setValue(0);
            btnIniciar.setEnabled(false);

            controller.ejecutarPrueba(fldId.getText(), total, (ok, err) -> {
                barra.setValue(ok + err);
                panelGrafico.actualizar(ok, err);
            }, msg -> {
                log(msg);
                btnIniciar.setEnabled(true);
            });
        } catch (Exception ex) { log("ERROR: Muestras invalidas."); }
    }

    private void log(String m) {
        SwingUtilities.invokeLater(() -> consola.append(" > " + m + "\n"));
    }

    private JPanel crearCaja(String t, JTextField f) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        Estilos.estilizarCampo(f);
        p.add(Estilos.crearLabel(t), BorderLayout.NORTH);
        p.add(f, BorderLayout.CENTER);
        return p;
    }
}