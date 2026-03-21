package com.MariaBermudez.GUI;

import com.MariaBermudez.db.DBManager;
import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.configuracion.CargadorConfig;
import com.MariaBermudez.utilidades.QueryLoader;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class VentanaGrafica extends JFrame {
    private JTextField fldConfig, fldQueriesFile, fldId, fldMuestras;
    private JTextArea consola;
    private JProgressBar barra;
    private JButton btnConectar, btnIniciar;
    private JButton btnStartTxn, btnAddToTxn, btnCommit, btnRollback;
    private JTextArea txnArea;
    private PanelGrafico panelGrafico;
    private List<String> txnList = new ArrayList<>();
    private boolean txnActive = false;

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
        JPanel pnlTop = new JPanel(new GridLayout(1, 4, 20, 0));
        pnlTop.setOpaque(false);

        fldConfig = new JTextField("configMySQL.json");
        fldQueriesFile = new JTextField("queries.json");
        fldId = new JTextField("default");
        fldMuestras = new JTextField("500");

        pnlTop.add(crearCaja("Configuración", fldConfig));
        pnlTop.add(crearCaja("Archivo Queries", fldQueriesFile));
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

        // Panel lateral para transacciones
        JPanel txnPanel = new JPanel(new BorderLayout(5,5));
        txnPanel.setOpaque(false);
        txnPanel.setBorder(Estilos.crearBordeNeon("TRANSACCIÓN"));

        txnArea = new JTextArea();
        txnArea.setEditable(false);
        txnArea.setBackground(Estilos.PANEL_INTERNO);
        txnArea.setForeground(Color.WHITE);
        txnArea.setFont(Estilos.FUENTE_MONO);
        JScrollPane txnScroll = new JScrollPane(txnArea);
        txnPanel.add(txnScroll, BorderLayout.CENTER);

        JPanel txnBtns = new JPanel(new GridLayout(2,2,5,5));
        txnBtns.setOpaque(false);
        btnStartTxn = Estilos.crearBoton("Iniciar Txn", false);
        btnAddToTxn = Estilos.crearBoton("Agregar ID", false);
        btnCommit = Estilos.crearBoton("Commit", false);
        btnRollback = Estilos.crearBoton("Rollback", false);
        txnBtns.add(btnStartTxn);
        txnBtns.add(btnAddToTxn);
        txnBtns.add(btnCommit);
        txnBtns.add(btnRollback);
        txnPanel.add(txnBtns, BorderLayout.SOUTH);

        JPanel rightWrap = new JPanel(new BorderLayout());
        rightWrap.setOpaque(false);
        rightWrap.add(chartWrap, BorderLayout.CENTER);
        rightWrap.add(txnPanel, BorderLayout.SOUTH);

        pnlCenter.add(scroll, BorderLayout.CENTER);
        pnlCenter.add(rightWrap, BorderLayout.EAST);

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

        // Listeners
        btnConectar.addActionListener(e -> conectar());
        btnIniciar.addActionListener(e -> ejecutar());
        btnStartTxn.addActionListener(e -> iniciarTransaccion());
        btnAddToTxn.addActionListener(e -> agregarATransaccion());
        btnCommit.addActionListener(e -> commitTransaccion());
        btnRollback.addActionListener(e -> rollbackTransaccion());
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

            // Si se especificó un archivo de queries en la UI, cargarlo y mezclar
            String qfile = fldQueriesFile.getText();
            if (qfile != null && !qfile.trim().isEmpty()) {
                try {
                    Map<String, String> externas = QueryLoader.cargar(qfile.trim());
                    if (externas != null) a.getQueries().putAll(externas);
                    log("SISTEMA: Cargado archivo de queries: " + qfile.trim());
                } catch (Exception ex) {
                    // Mostrar diagnóstico completo en la traza para facilitar debugging
                    log("WARN: No se pudo cargar archivo de queries: " + ex.getMessage());
                    java.io.StringWriter sw = new java.io.StringWriter();
                    java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                    ex.printStackTrace(pw);
                    pw.flush();
                    log(sw.getBuffer().toString());
                }
            }

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

    // Transacción: iniciar conexión transaccional
    private void iniciarTransaccion() {
        try {
            DBManager.getComponent().transaction();
            txnActive = true;
            log("TRANSACCIÓN: iniciada (modo manual). Agregue IDs y luego haga Commit o Rollback.");
        } catch (Exception e) { log("ERR: " + e.getMessage()); }
    }

    // Añadir el id actual al buffer de transacción
    private void agregarATransaccion() {
        String id = fldId.getText();
        if (id == null || id.trim().isEmpty()) { log("ERROR: ID vacío"); return; }
        txnList.add(id.trim());
        actualizarTxnArea();
        log("TRANSACCIÓN: agregado ID='" + id.trim() + "'");
    }

    private void actualizarTxnArea() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < txnList.size(); i++) {
            sb.append((i+1)).append(". ").append(txnList.get(i)).append('\n');
        }
        txnArea.setText(sb.toString());
    }

    // Ejecutar el batch de IDs en una transacción (commit automático si no estaba iniciada manualmente)
    private void commitTransaccion() {
        try {
            if (txnList.isEmpty()) { log("TRANSACCIÓN: no hay queries en la lista."); return; }

            if (txnActive) {
                // La transacción ya está abierta por el usuario: ejecutar cada query con query() (usa la conexión transaccional)
                for (String id : new ArrayList<>(txnList)) {
                    DBManager.getComponent().query(id);
                }
                DBManager.getComponent().commit();
                log("TRANSACCIÓN: commit realizado (modo manual).");
            } else {
                // No estaba iniciada manualmente: usar el método que hace start/commit automático
                DBManager.getComponent().executeBatchByIds(new ArrayList<>(txnList));
                log("TRANSACCIÓN: commit realizado (modo batch automático).");
            }

            txnList.clear();
            actualizarTxnArea();
            txnActive = false;
        } catch (Exception e) { log("ERR: " + e.getMessage()); }
    }

    private void rollbackTransaccion() {
        try {
            if (txnActive) {
                DBManager.getComponent().rollback();
                log("TRANSACCIÓN: rollback realizado (modo manual).");
            } else {
                // Si no está activa, limpiar la lista local
                log("TRANSACCIÓN: rollback (modo batch) - limpiando lista local.");
            }
            txnList.clear();
            actualizarTxnArea();
            txnActive = false;
        } catch (Exception e) { log("ERR: " + e.getMessage()); }
    }

    private void log(String m) { consola.append(" > " + m + "\n"); }
}