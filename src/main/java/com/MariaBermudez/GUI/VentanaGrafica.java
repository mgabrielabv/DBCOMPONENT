package com.MariaBermudez.GUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class VentanaGrafica extends JFrame {
    private JTextField fldConfig, fldQueries, fldId, fldMuestras;
    private JTextArea consola;
    private JProgressBar barra;
    private JButton btnConectar, btnIniciar;
    private PanelGrafico panelGrafico;
    private final DBController controller = new DBController();

    public VentanaGrafica() {
        Estilos.aplicar(this);
        setTitle("DBComponent - Stress Test Tool");
        setSize(1100, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // ROOT PANEL: Ahora con fondo sólido de Estilos
        JPanel root = new JPanel(new BorderLayout(25, 25));
        root.setOpaque(true);
        root.setBackground(Estilos.FONDO_OBSCURO);
        root.setBorder(new EmptyBorder(30, 30, 30, 30));
        setContentPane(root);

        // TOP PANEL
        JPanel pnlTop = new JPanel(new GridLayout(1, 4, 25, 0));
        pnlTop.setOpaque(false);

        fldConfig = new JTextField("configPostgreSQL.json");
        fldQueries = new JTextField("queries.json");
        fldId = new JTextField("test_conexion");
        fldMuestras = new JTextField("100");

        pnlTop.add(crearCaja("Configuración", fldConfig));
        pnlTop.add(crearCaja("Archivo Queries", fldQueries));
        pnlTop.add(crearCaja("ID Query", fldId));
        pnlTop.add(crearCaja("Muestras", fldMuestras));

        // CENTER PANEL
        JPanel pnlCenter = new JPanel(new GridLayout(1, 2, 25, 0));
        pnlCenter.setOpaque(false);

        consola = new JTextArea();
        consola.setEditable(false);
        consola.setBackground(Estilos.PANEL_INTERNO);
        consola.setForeground(Estilos.CIAN_CONTRASTE);
        consola.setFont(Estilos.FUENTE_MONO);

        JScrollPane scroll = new JScrollPane(consola);
        scroll.setBorder(Estilos.crearBordeNeon("LOG DE EVENTOS"));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        panelGrafico = new PanelGrafico();
        JPanel wrapperGrafico = new JPanel(new BorderLayout());
        wrapperGrafico.setOpaque(false);
        wrapperGrafico.setBorder(Estilos.crearBordeNeon("RENDIMIENTO"));
        wrapperGrafico.add(panelGrafico);

        pnlCenter.add(scroll);
        pnlCenter.add(wrapperGrafico);

        // BOTTOM PANEL
        JPanel pnlBottom = new JPanel(new BorderLayout(15, 15));
        pnlBottom.setOpaque(false);

        barra = new JProgressBar();
        barra.setStringPainted(true);
        barra.setBackground(Estilos.PANEL_INTERNO);
        barra.setForeground(Estilos.MORADO_ELECTRICO);
        barra.setBorder(BorderFactory.createLineBorder(Estilos.MORADO_ELECTRICO));

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        pnlBtns.setOpaque(false);
        btnConectar = Estilos.crearBoton("CONECTAR", false);
        btnIniciar = Estilos.crearBoton("INICIAR TEST", true);
        btnIniciar.setEnabled(false);

        pnlBtns.add(btnConectar);
        pnlBtns.add(btnIniciar);
        pnlBottom.add(barra, BorderLayout.NORTH);
        pnlBottom.add(pnlBtns, BorderLayout.SOUTH);

        root.add(pnlTop, BorderLayout.NORTH);
        root.add(pnlCenter, BorderLayout.CENTER);
        root.add(pnlBottom, BorderLayout.SOUTH);

        btnConectar.addActionListener(e ->
                controller.conectar(fldConfig.getText(), fldQueries.getText(), this::log, () -> btnIniciar.setEnabled(true))
        );
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
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.add(Estilos.crearLabel(t), BorderLayout.NORTH);
        Estilos.estilizarCampo(f);
        p.add(f, BorderLayout.CENTER);
        return p;
    }
}