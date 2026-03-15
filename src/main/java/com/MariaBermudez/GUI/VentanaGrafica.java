package com.MariaBermudez.GUI;

import com.MariaBermudez.configuracion.CargadorConfig;
import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.db.*;
import com.MariaBermudez.utilidades.RegistradorLog;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.border.*;

public class VentanaGrafica extends JFrame {
    private JProgressBar barra;
    private JLabel lblExitos, lblFallos, lblPorcExito, lblPorcFallo, lblReloj;
    private JLabel lblReintPool, lblReintRaw;
    private JLabel lblPoolStats, lblRawStats;
    private JLabel lblTiempoPool, lblTiempoRaw;
    private DefaultListModel<String> logModel;
    private JTextField txtQuery, txtMuestras, txtUrl, txtUsuario, txtClave, txtReintentos, txtPoolSize;
    private JComboBox<String> cmbMotorDB;
    private JCheckBox chkUsarPool, chkComparar;
    private JButton btnIn, btnSt;
    private PanelGrafico panelGrafica;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private DBManager dbManager;
    private long tiempoInicioGlobal;
    private Ajustes configActual; // Guardar la configuración actual

    public VentanaGrafica() {
        dbManager = DBManager.getInstance();
        setupUI();
        setLocationRelativeTo(null);
        cargarConfiguracionInicial();
    }

    private void setupUI() {
        setTitle("POOLED VS RAW - PostgreSQL/MySQL");
        setSize(1500, 950);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(Estilos.BG);
        setLayout(new BorderLayout(20, 20));
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));

        // ========== PANEL DE CONTROL IZQUIERDO ==========
        JPanel lateral = new JPanel(new BorderLayout(15, 15));
        lateral.setOpaque(false);
        lateral.setPreferredSize(new Dimension(450, 0));

        // Panel de configuración
        JPanel cardConfig = new JPanel(new GridBagLayout());
        cardConfig.setBackground(Estilos.CARD);
        cardConfig.setBorder(new CompoundBorder(
                new LineBorder(new Color(45,45,60)),
                new EmptyBorder(15,15,15,15)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        // Título de configuración
        JLabel lblTitulo = new JLabel("CONFIGURACION DE BASE DE DATOS");
        lblTitulo.setForeground(Estilos.ACCENT);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cardConfig.add(lblTitulo, gbc);

        // Fila 1: Motor DB
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        cardConfig.add(new JLabel("Motor DB:") {{ setForeground(Estilos.SUBTEXT); }}, gbc);

        gbc.gridx = 1;
        String[] motores = {"PostgreSQL", "MySQL/MariaDB", "Auto-detectar"};
        cmbMotorDB = new JComboBox<>(motores);
        cmbMotorDB.setSelectedIndex(2);
        cardConfig.add(cmbMotorDB, gbc);

        // Fila 2: URL
        gbc.gridx = 0;
        gbc.gridy = 2;
        cardConfig.add(new JLabel("URL JDBC:") {{ setForeground(Estilos.SUBTEXT); }}, gbc);

        gbc.gridx = 1;
        txtUrl = new JTextField("jdbc:postgresql://localhost:5432/mi_db");
        cardConfig.add(txtUrl, gbc);

        // Fila 3: Usuario
        gbc.gridx = 0;
        gbc.gridy = 3;
        cardConfig.add(new JLabel("Usuario:") {{ setForeground(Estilos.SUBTEXT); }}, gbc);

        gbc.gridx = 1;
        txtUsuario = new JTextField("postgres");
        cardConfig.add(txtUsuario, gbc);

        // Fila 4: Clave
        gbc.gridx = 0;
        gbc.gridy = 4;
        cardConfig.add(new JLabel("Clave:") {{ setForeground(Estilos.SUBTEXT); }}, gbc);

        gbc.gridx = 1;
        txtClave = new JPasswordField("password");
        cardConfig.add(txtClave, gbc);

        // Separador
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        cardConfig.add(new JSeparator(), gbc);

        // Fila 5: Query
        gbc.gridy = 6;
        cardConfig.add(new JLabel("Query:") {{ setForeground(Estilos.SUBTEXT); }}, gbc);

        gbc.gridy = 7;
        txtQuery = new JTextField("SELECT * FROM usuarios LIMIT 100");
        cardConfig.add(txtQuery, gbc);

        // Fila 6: Muestras
        gbc.gridy = 8;
        cardConfig.add(new JLabel("Muestras por motor:") {{ setForeground(Estilos.SUBTEXT); }}, gbc);

        gbc.gridy = 9;
        txtMuestras = new JTextField("5000");
        cardConfig.add(txtMuestras, gbc);

        // Fila 7: Reintentos
        gbc.gridy = 10;
        cardConfig.add(new JLabel("Reintentos por fallo:") {{ setForeground(Estilos.SUBTEXT); }}, gbc);

        gbc.gridy = 11;
        txtReintentos = new JTextField("3");
        cardConfig.add(txtReintentos, gbc);

        // Fila 8: Pool Size
        gbc.gridy = 12;
        cardConfig.add(new JLabel("Tamano del pool:") {{ setForeground(Estilos.SUBTEXT); }}, gbc);

        gbc.gridy = 13;
        txtPoolSize = new JTextField("20");
        cardConfig.add(txtPoolSize, gbc);

        // Fila 9: Opciones
        gbc.gridy = 14;
        gbc.gridwidth = 2;
        JPanel panelOpciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelOpciones.setOpaque(false);

        chkUsarPool = new JCheckBox("Usar Pool de conexiones", true);
        chkUsarPool.setForeground(Estilos.TEXT);
        chkUsarPool.setOpaque(false);
        panelOpciones.add(chkUsarPool);

        chkComparar = new JCheckBox("Comparar Pool vs Raw", true);
        chkComparar.setForeground(Estilos.TEXT);
        chkComparar.setOpaque(false);
        panelOpciones.add(chkComparar);

        cardConfig.add(panelOpciones, gbc);

        // Fila 10: Botones
        gbc.gridy = 15;
        gbc.gridwidth = 2;
        JPanel panelBotones = new JPanel(new GridLayout(1, 2, 10, 0));
        panelBotones.setOpaque(false);

        btnIn = new JButton("INICIAR PRUEBA");
        btnIn.setBackground(Estilos.ACCENT);
        btnIn.setForeground(Color.BLACK);
        btnIn.setFont(new Font("Segoe UI", Font.BOLD, 14));

        btnSt = new JButton("DETENER");
        btnSt.setBackground(Estilos.RED);
        btnSt.setForeground(Color.WHITE);
        btnSt.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSt.setEnabled(false);

        panelBotones.add(btnIn);
        panelBotones.add(btnSt);
        cardConfig.add(panelBotones, gbc);

        // Panel de log
        logModel = new DefaultListModel<>();
        JList<String> logList = new JList<>(logModel);
        logList.setBackground(Estilos.CARD);
        logList.setForeground(Estilos.TEXT);
        logList.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JScrollPane scroll = new JScrollPane(logList);
        scroll.setBorder(new TitledBorder(
                new LineBorder(new Color(45,45,60)),
                "REGISTRO DE EVENTOS",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                Estilos.ACCENT
        ));
        scroll.setPreferredSize(new Dimension(0, 250));

        lateral.add(cardConfig, BorderLayout.NORTH);
        lateral.add(scroll, BorderLayout.CENTER);

        // ========== PANEL CENTRAL ==========
        JPanel centro = new JPanel(new BorderLayout(20, 20));
        centro.setOpaque(false);

        // Panel de métricas superiores
        JPanel panelMetricas = new JPanel(new GridLayout(1, 4, 15, 15));
        panelMetricas.setOpaque(false);
        panelMetricas.setPreferredSize(new Dimension(0, 110));

        lblExitos = createMetricCard(panelMetricas, "EXITOSAS", Estilos.GREEN);
        lblFallos = createMetricCard(panelMetricas, "FALLIDAS", Estilos.RED);
        lblPorcExito = createMetricCard(panelMetricas, "% EXITO", Estilos.GREEN);
        lblPorcFallo = createMetricCard(panelMetricas, "% FALLO", Estilos.RED);

        // Panel de gráficas
        panelGrafica = new PanelGrafico();
        panelGrafica.setPreferredSize(new Dimension(0, 350));

        JPanel cardGr = new JPanel(new BorderLayout());
        cardGr.setBackground(Estilos.CARD);
        cardGr.setBorder(new LineBorder(new Color(45,45,60)));
        cardGr.add(panelGrafica);

        // Panel inferior de estadísticas detalladas
        JPanel inferior = new JPanel(new GridLayout(4, 2, 10, 5));
        inferior.setOpaque(false);
        inferior.setBorder(new EmptyBorder(10, 10, 10, 10));

        lblReloj = new JLabel("TIEMPO TOTAL: 0 ms");
        lblReloj.setForeground(Color.WHITE);
        lblReloj.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel lblTiempoPoolTitle = new JLabel("Tiempo Pool:");
        lblTiempoPoolTitle.setForeground(Estilos.SUBTEXT);
        lblTiempoPool = new JLabel("0 ms");
        lblTiempoPool.setForeground(Color.CYAN);

        JLabel lblTiempoRawTitle = new JLabel("Tiempo Raw:");
        lblTiempoRawTitle.setForeground(Estilos.SUBTEXT);
        lblTiempoRaw = new JLabel("0 ms");
        lblTiempoRaw.setForeground(Color.MAGENTA);

        lblReintPool = new JLabel("Reintentos Pool (promedio): 0.00");
        lblReintPool.setForeground(Color.CYAN);

        lblReintRaw = new JLabel("Reintentos Raw (promedio): 0.00");
        lblReintRaw.setForeground(Color.MAGENTA);

        lblPoolStats = new JLabel("POOL: E:0 F:0 (0.0%)");
        lblPoolStats.setForeground(Estilos.GREEN);
        lblPoolStats.setFont(new Font("Segoe UI", Font.BOLD, 13));

        lblRawStats = new JLabel("RAW: E:0 F:0 (0.0%)");
        lblRawStats.setForeground(Estilos.ACCENT);
        lblRawStats.setFont(new Font("Segoe UI", Font.BOLD, 13));

        inferior.add(lblReloj);
        inferior.add(new JLabel(""));
        inferior.add(lblTiempoPoolTitle);
        inferior.add(lblTiempoPool);
        inferior.add(lblTiempoRawTitle);
        inferior.add(lblTiempoRaw);
        inferior.add(lblReintPool);
        inferior.add(lblReintRaw);

        // Barra de progreso
        barra = new JProgressBar(0, 100);
        barra.setPreferredSize(new Dimension(0, 35));
        barra.setStringPainted(true);
        barra.setForeground(Estilos.ACCENT);
        barra.setBackground(Estilos.CARD);
        barra.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JPanel barraHolder = new JPanel(new BorderLayout());
        barraHolder.setOpaque(false);
        barraHolder.add(barra, BorderLayout.CENTER);
        barraHolder.setBorder(new EmptyBorder(10, 0, 0, 0));

        // Ensamblar centro
        JPanel centroSuperior = new JPanel(new BorderLayout());
        centroSuperior.setOpaque(false);
        centroSuperior.add(panelMetricas, BorderLayout.NORTH);
        centroSuperior.add(cardGr, BorderLayout.CENTER);

        centro.add(centroSuperior, BorderLayout.CENTER);

        JPanel centroInferior = new JPanel(new BorderLayout());
        centroInferior.setOpaque(false);
        centroInferior.add(inferior, BorderLayout.CENTER);
        centroInferior.add(barraHolder, BorderLayout.SOUTH);
        centroInferior.add(lblPoolStats, BorderLayout.NORTH);
        centroInferior.add(lblRawStats, BorderLayout.SOUTH);

        centro.add(centroInferior, BorderLayout.SOUTH);

        add(lateral, BorderLayout.WEST);
        add(centro, BorderLayout.CENTER);

        // Listeners
        btnIn.addActionListener(e -> ejecutarPrueba());
        btnSt.addActionListener(e -> {
            running.set(false);
            btnSt.setEnabled(false);
            logModel.addElement(">>> PRUEBA DETENIDA POR USUARIO");
        });
    }

    private JLabel createMetricCard(JPanel p, String titulo, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Estilos.CARD);
        card.setBorder(new LineBorder(new Color(45,45,60)));

        JLabel title = new JLabel(titulo, SwingConstants.CENTER);
        title.setForeground(Estilos.SUBTEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JLabel valor = new JLabel("0", SwingConstants.CENTER);
        valor.setForeground(color);
        valor.setFont(new Font("JetBrains Mono", Font.BOLD, 28));

        card.add(title, BorderLayout.NORTH);
        card.add(valor, BorderLayout.CENTER);
        p.add(card);

        return valor;
    }

    private void cargarConfiguracionInicial() {
        try {
            configActual = CargadorConfig.cargar();
            txtUrl.setText(configActual.url());
            txtUsuario.setText(configActual.usuario());
            txtClave.setText(configActual.clave());
            txtQuery.setText(configActual.query());
            txtMuestras.setText(String.valueOf(configActual.muestras()));
            txtReintentos.setText(String.valueOf(configActual.reintentos()));
            txtPoolSize.setText(String.valueOf(configActual.limitePool()));
        } catch (Exception e) {
            logModel.addElement("No se pudo cargar config.json, usando valores por defecto");
        }
    }

    private void ejecutarPrueba() {
        running.set(true);
        logModel.clear();
        panelGrafica.limpiar();
        btnIn.setEnabled(false);
        btnSt.setEnabled(true);

        int totalMuestras = Integer.parseInt(txtMuestras.getText());
        String queryPersonalizada = txtQuery.getText();
        int reintentosConfig = Integer.parseInt(txtReintentos.getText());
        int poolSize = Integer.parseInt(txtPoolSize.getText());
        boolean usarPool = chkUsarPool.isSelected();
        boolean comparar = chkComparar.isSelected();

        Thread.startVirtualThread(() -> {
            try {
                // Crear configuración con los valores de la UI
                configActual = new Ajustes(
                        txtUrl.getText(),
                        txtUsuario.getText(),
                        txtClave.getText(),
                        queryPersonalizada,
                        totalMuestras,
                        reintentosConfig,
                        100, // salto (no usado)
                        poolSize
                );

                // Determinar tipo de motor seleccionado
                DBFactory.TipoDB dbType = getSelectedDatabaseType();

                // Registrar adaptadores según opciones
                DBComponent dbPool = null;
                DBComponent dbRaw = null;

                if (usarPool || comparar) {
                    dbPool = dbManager.registrarAdapter(configActual, true, "POOL", dbType);
                    final DBComponent poolFinal = dbPool; // Variable final para usar dentro del lambda
                    SwingUtilities.invokeLater(() ->
                            logModel.addElement(">>> ADAPTER POOL REGISTRADO: " + poolFinal.getTipoMotor())
                    );
                }

                if (!usarPool || comparar) {
                    dbRaw = dbManager.registrarAdapter(configActual, false, "RAW", dbType);
                    final DBComponent rawFinal = dbRaw; // Variable final para usar dentro del lambda
                    SwingUtilities.invokeLater(() ->
                            logModel.addElement(">>> ADAPTER RAW REGISTRADO: " + rawFinal.getTipoMotor())
                    );
                }
                String mensajeInicio = ">>> INICIANDO PRUEBA - Muestras: " + totalMuestras;
                String mensajeQuery = ">>> Query: " + queryPersonalizada;

                SwingUtilities.invokeLater(() -> {
                    logModel.addElement(mensajeInicio);
                    logModel.addElement(mensajeQuery);
                });

                tiempoInicioGlobal = System.currentTimeMillis();

                if (comparar && dbPool != null && dbRaw != null) {
                    // Ejecutar prueba comparativa
                    ejecutarPruebaComparativa(dbPool, dbRaw, totalMuestras, reintentosConfig);
                } else if (usarPool && dbPool != null) {
                    // Ejecutar solo con pool
                    ejecutarPruebaSimple(dbPool, totalMuestras, reintentosConfig, "POOL");
                } else if (dbRaw != null) {
                    // Ejecutar solo con raw
                    ejecutarPruebaSimple(dbRaw, totalMuestras, reintentosConfig, "RAW");
                }

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    logModel.addElement("ERROR: " + e.getMessage());
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                            "Error: " + e.getMessage(),
                            "Error de conexión",
                            JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                // Limpiar al terminar
                dbManager.shutdownAll();
                SwingUtilities.invokeLater(() -> {
                    btnIn.setEnabled(true);
                    btnSt.setEnabled(false);
                    running.set(false);
                    logModel.addElement(">>> PRUEBA FINALIZADA");
                });
            }
        });
    }

    private DBFactory.TipoDB getSelectedDatabaseType() {
        switch (cmbMotorDB.getSelectedIndex()) {
            case 0: return DBFactory.TipoDB.POSTGRESQL;
            case 1: return DBFactory.TipoDB.MYSQL;
            default: return DBFactory.TipoDB.AUTO_DETECT;
        }
    }

    private void ejecutarPruebaSimple(DBComponent db, int total, int reintentosMax, String nombre) {
        Resultado res = new Resultado(nombre);
        CountDownLatch inicioLatch = new CountDownLatch(1);

        SwingUtilities.invokeLater(() ->
                logModel.addElement(">>> EJECUTANDO PRUEBA SIMPLE: " + nombre)
        );

        for (int i = 1; i <= total && running.get(); i++) {
            final int id = i;
            Thread.startVirtualThread(() -> {
                try {
                    inicioLatch.await();
                    ejecutarConsulta(db, id, res, reintentosMax, configActual.query());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        inicioLatch.countDown(); // Disparar todos los hilos

        // Esperar a que terminen
        while (res.procesados.get() < total && running.get()) {
            try { Thread.sleep(100); } catch (InterruptedException e) { break; }
        }

        long tiempoTotal = System.currentTimeMillis() - tiempoInicioGlobal;

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "PRUEBA " + nombre + " FINALIZADA\n" +
                            "Exitos: " + res.exitos.get() + "\n" +
                            "Fallos: " + res.fallos.get() + "\n" +
                            "Eficacia: " + String.format("%.2f%%",
                            (res.procesados.get() == 0 ? 0 : (res.exitos.get() * 100.0 / res.procesados.get()))) + "\n" +
                            "Tiempo total: " + tiempoTotal + " ms",
                    "Resultados",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void ejecutarPruebaComparativa(DBComponent dbPool, DBComponent dbRaw, int total, int reintentosMax) {
        Resultado resPool = new Resultado("POOL");
        Resultado resRaw = new Resultado("RAW");

        CountDownLatch inicioLatch = new CountDownLatch(1);
        CountDownLatch finLatch = new CountDownLatch(2);

        long inicioPool = 0, inicioRaw = 0, finPool = 0, finRaw = 0;
        final long[] tiempos = new long[4];

        SwingUtilities.invokeLater(() ->
                logModel.addElement(">>> EJECUTANDO PRUEBA COMPARATIVA")
        );

        // Worker para POOL
        Thread.startVirtualThread(() -> {
            try {
                inicioLatch.await();
                tiempos[0] = System.currentTimeMillis(); // inicioPool

                for (int i = 1; i <= total && running.get(); i++) {
                    final int id = i;
                    ejecutarConsulta(dbPool, id, resPool, reintentosMax, configActual.query());
                    actualizarUI(resPool, resRaw, total * 2);
                }

                tiempos[2] = System.currentTimeMillis(); // finPool
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        logModel.addElement("ERROR en POOL: " + e.getMessage())
                );
            } finally {
                finLatch.countDown();
            }
        });

        // Worker para RAW
        Thread.startVirtualThread(() -> {
            try {
                inicioLatch.await();
                tiempos[1] = System.currentTimeMillis(); // inicioRaw

                for (int i = 1; i <= total && running.get(); i++) {
                    final int id = i;
                    ejecutarConsulta(dbRaw, id, resRaw, reintentosMax, configActual.query());
                    actualizarUI(resPool, resRaw, total * 2);
                }

                tiempos[3] = System.currentTimeMillis(); // finRaw
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        logModel.addElement("ERROR en RAW: " + e.getMessage())
                );
            } finally {
                finLatch.countDown();
            }
        });

        // Iniciar ambos simultáneamente
        inicioLatch.countDown();

        try {
            finLatch.await(); // Esperar que terminen ambos
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long tiempoPool = tiempos[2] - tiempos[0];
        long tiempoRaw = tiempos[3] - tiempos[1];

        // Actualizar tiempos en UI
        SwingUtilities.invokeLater(() -> {
            lblTiempoPool.setText(tiempoPool + " ms");
            lblTiempoRaw.setText(tiempoRaw + " ms");
        });

        // Mostrar comparación final
        mostrarComparacion(resPool, resRaw, tiempoPool, tiempoRaw);
    }

    private void ejecutarConsulta(DBComponent db, int id, Resultado res, int reintentosMax, String query) {
        boolean ok = false;
        int reintentos = 0;
        long latencia = 0;

        while (!ok && reintentos < reintentosMax && running.get()) {
            try (Connection conn = db.getConnection()) {
                long t0 = System.currentTimeMillis();

                try (var stmt = conn.createStatement()) {
                    if (query.toUpperCase().trim().startsWith("SELECT")) {
                        try (var rs = stmt.executeQuery(query)) {
                            int count = 0;
                            while (rs.next()) count++;
                            latencia = System.currentTimeMillis() - t0;
                            ok = true;
                            res.exitos.incrementAndGet();

                            String queryResumida = query.length() > 50 ?
                                    query.substring(0, 47) + "..." : query;

                            RegistradorLog.escribir(
                                    id,
                                    "EXITO:" + count + " rows",
                                    latencia,
                                    reintentos,
                                    db.getTipoMotor() + "-" + res.nombre,
                                    queryResumida
                            );
                        }
                    } else {
                        int updated = stmt.executeUpdate(query);
                        latencia = System.currentTimeMillis() - t0;
                        ok = true;
                        res.exitos.incrementAndGet();

                        String queryResumida = query.length() > 50 ?
                                query.substring(0, 47) + "..." : query;

                        RegistradorLog.escribir(
                                id,
                                "EXITO:UPD " + updated,
                                latencia,
                                reintentos,
                                db.getTipoMotor() + "-" + res.nombre,
                                queryResumida
                        );
                    }
                }
            } catch (SQLException e) {
                reintentos++;
                res.reintentos.incrementAndGet();

                if (reintentos >= reintentosMax) {
                    res.fallos.incrementAndGet();
                    RegistradorLog.escribir(
                            id,
                            "FALLO:" + e.getMessage(),
                            0,
                            reintentos,
                            db.getTipoMotor() + "-" + res.nombre,
                            ""
                    );
                }
            } catch (Exception e) {
                reintentos++;
                res.reintentos.incrementAndGet();
            }
        }

        res.procesados.incrementAndGet();
    }

    private void actualizarUI(Resultado pool, Resultado raw, int total) {
        SwingUtilities.invokeLater(() -> {
            int totalProcesados = pool.procesados.get() + raw.procesados.get();
            int totalExitos = pool.exitos.get() + raw.exitos.get();
            int totalFallos = pool.fallos.get() + raw.fallos.get();

            if (totalProcesados > 0) {
                double pctExito = (totalExitos * 100.0) / totalProcesados;
                double pctFallo = (totalFallos * 100.0) / totalProcesados;

                lblExitos.setText(String.valueOf(totalExitos));
                lblFallos.setText(String.valueOf(totalFallos));
                lblPorcExito.setText(String.format("%.1f%%", pctExito));
                lblPorcFallo.setText(String.format("%.1f%%", pctFallo));

                int progreso = (totalProcesados * 100) / total;
                barra.setValue(Math.min(progreso, 100));
                barra.setString(progreso + "% | " + totalProcesados + " de " + total);

                long tiempoActual = System.currentTimeMillis() - tiempoInicioGlobal;
                lblReloj.setText(String.format("TIEMPO TOTAL: %d ms", tiempoActual));

                // Estadísticas por motor
                double avgPool = pool.procesados.get() == 0 ? 0 :
                        (double) pool.reintentos.get() / pool.procesados.get();
                double avgRaw = raw.procesados.get() == 0 ? 0 :
                        (double) raw.reintentos.get() / raw.procesados.get();

                lblReintPool.setText(String.format("Reintentos Pool: %.2f", avgPool));
                lblReintRaw.setText(String.format("Reintentos Raw: %.2f", avgRaw));

                lblPoolStats.setText(String.format(
                        "POOL: E:%d F:%d (%.1f%%)",
                        pool.exitos.get(),
                        pool.fallos.get(),
                        pool.procesados.get() == 0 ? 0 :
                                (pool.exitos.get() * 100.0 / pool.procesados.get())
                ));

                lblRawStats.setText(String.format(
                        "RAW: E:%d F:%d (%.1f%%)",
                        raw.exitos.get(),
                        raw.fallos.get(),
                        raw.procesados.get() == 0 ? 0 :
                                (raw.exitos.get() * 100.0 / raw.procesados.get())
                ));

                // Actualizar gráfica
                panelGrafica.actualizarContadores(
                        pool.exitos.get(), pool.fallos.get(),
                        raw.exitos.get(), raw.fallos.get()
                );
            }
        });
    }

    private void mostrarComparacion(Resultado pool, Resultado raw, long tiempoPool, long tiempoRaw) {
        double efPool = pool.procesados.get() == 0 ? 0 :
                (pool.exitos.get() * 100.0) / pool.procesados.get();
        double efRaw = raw.procesados.get() == 0 ? 0 :
                (raw.exitos.get() * 100.0) / raw.procesados.get();

        double avgPool = pool.procesados.get() == 0 ? 0 :
                (double) pool.reintentos.get() / pool.procesados.get();
        double avgRaw = raw.procesados.get() == 0 ? 0 :
                (double) raw.reintentos.get() / raw.procesados.get();

        String comparacion = "COMPARACION FINAL\n\n" +
                "POOLED (con pool de conexiones):\n" +
                "  Exitos: " + pool.exitos.get() + "\n" +
                "  Fallos: " + pool.fallos.get() + "\n" +
                "  Eficacia: " + String.format("%.2f%%", efPool) + "\n" +
                "  Reintentos promedio: " + String.format("%.2f", avgPool) + "\n" +
                "  Tiempo total: " + tiempoPool + " ms\n\n" +

                "RAW (sin pool):\n" +
                "  Exitos: " + raw.exitos.get() + "\n" +
                "  Fallos: " + raw.fallos.get() + "\n" +
                "  Eficacia: " + String.format("%.2f%%", efRaw) + "\n" +
                "  Reintentos promedio: " + String.format("%.2f", avgRaw) + "\n" +
                "  Tiempo total: " + tiempoRaw + " ms\n\n" +

                "CONCLUSION:\n" +
                (efPool > efRaw ? "El motor POOLED es mas eficiente" :
                        efRaw > efPool ? "El motor RAW es mas eficiente" :
                                "Empate tecnico");

        String finalComparacion = comparacion;

        SwingUtilities.invokeLater(() -> {
            logModel.addElement(">>> " + finalComparacion.replace("\n", " | "));
            JOptionPane.showMessageDialog(this, finalComparacion, "Resultados", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    // Clase interna para resultados
    private static class Resultado {
        final String nombre;
        final AtomicInteger exitos = new AtomicInteger(0);
        final AtomicInteger fallos = new AtomicInteger(0);
        final AtomicInteger procesados = new AtomicInteger(0);
        final AtomicInteger reintentos = new AtomicInteger(0);

        Resultado(String nombre) {
            this.nombre = nombre;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaGrafica().setVisible(true));
    }
}