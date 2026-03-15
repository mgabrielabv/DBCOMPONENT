package com.MariaBermudez;

import com.MariaBermudez.GUI.VentanaGrafica;
import com.MariaBermudez.configuracion.CargadorConfig;
import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.db.DBManager;
import com.MariaBermudez.utilidades.RegistradorLog;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.io.File;

public class Main {

    // Configuracion de la aplicacion
    private static final String TITULO = "DB COMPONENT - Pool vs Raw";
    private static final String VERSION = "2.0.0";
    private static final String ARCHIVO_LOG = "simulador.log";

    // Modos de look and feel
    private enum Tema {
        OSCURO,
        CLARO,
        SISTEMA
    }

    private static Tema temaActual = Tema.OSCURO;

    public static void main(String[] args) {

        // Procesar argumentos de linea de comandos
        procesarArgumentos(args);

        // Configurar logging
        configurarLogging();

        // Mostrar informacion de inicio
        mostrarBanner();

        // Verificar dependencias
        verificarDependencias();

        // Cargar configuracion
        cargarConfiguracionInicial();

        // Configurar look and feel
        configurarLookAndFeel();

        // Registrar shutdown hook para limpieza
        Runtime.getRuntime().addShutdownHook(new Thread(Main::limpiarRecursos));

        // Iniciar la aplicacion en el EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            try {
                // Crear y mostrar la ventana principal
                VentanaGrafica ventana = new VentanaGrafica();
                ventana.setVisible(true);

                // Log de inicio exitoso
                RegistradorLog.escribir(
                        -1,
                        "APLICACION INICIADA - Version: " + VERSION,
                        0,
                        0,
                        "MAIN",
                        ""
                );

            } catch (Exception e) {
                manejarErrorFatal("Error al iniciar la ventana principal", e);
            }
        });
    }

    /**
     * Procesa los argumentos de linea de comandos
     */
    private static void procesarArgumentos(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "-tema":
                case "-theme":
                    if (i + 1 < args.length) {
                        String tema = args[i + 1].toLowerCase();
                        switch (tema) {
                            case "oscuro":
                            case "dark":
                                temaActual = Tema.OSCURO;
                                break;
                            case "claro":
                            case "light":
                                temaActual = Tema.CLARO;
                                break;
                            case "sistema":
                            case "system":
                                temaActual = Tema.SISTEMA;
                                break;
                        }
                    }
                    break;

                case "-h":
                case "-help":
                case "--help":
                    mostrarAyuda();
                    System.exit(0);
                    break;

                case "-v":
                case "-version":
                case "--version":
                    System.out.println("Version: " + VERSION);
                    System.exit(0);
                    break;
            }
        }
    }

    /**
     * Muestra la ayuda de linea de comandos
     */
    private static void mostrarAyuda() {
        System.out.println("=== DB COMPONENT - Pool vs Raw ===\n");
        System.out.println("Uso: java -jar DBCOMPONENT.jar [opciones]\n");
        System.out.println("Opciones:");
        System.out.println("  -tema <oscuro|claro|sistema>  Selecciona el tema de la interfaz");
        System.out.println("  -v, -version                  Muestra la version");
        System.out.println("  -h, -help                     Muestra esta ayuda\n");
        System.out.println("Ejemplos:");
        System.out.println("  java -jar DBCOMPONENT.jar -tema claro");
        System.out.println("  java -jar DBCOMPONENT.jar -version\n");
    }

    /**
     * Configura el sistema de logging
     */
    private static void configurarLogging() {
        // Limpiar archivo de log anterior si es muy grande (>10MB)
        File logFile = new File(ARCHIVO_LOG);
        if (logFile.exists() && logFile.length() > 10 * 1024 * 1024) {
            logFile.delete();
        }
    }

    /**
     * Muestra el banner de inicio
     */
    private static void mostrarBanner() {
        System.out.println("================================================");
        System.out.println("   DB COMPONENT - Pool vs Raw " + VERSION);
        System.out.println("   PostgreSQL / MySQL Benchmark");
        System.out.println("================================================");
        System.out.println("Iniciando aplicacion...");
        System.out.println("Tema seleccionado: " + temaActual);
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println();
    }

    /**
     * Verifica que las dependencias necesarias esten disponibles
     */
    private static void verificarDependencias() {
        System.out.println("Verificando dependencias...");

        // Verificar drivers JDBC
        verificarDriver("org.postgresql.Driver", "PostgreSQL");
        verificarDriver("com.mysql.cj.jdbc.Driver", "MySQL");

        // Verificar FlatLaf
        try {
            Class.forName("com.formdev.flatlaf.FlatDarkLaf");
            System.out.println("OK - FlatLaf disponible");
        } catch (ClassNotFoundException e) {
            System.out.println("ERROR - FlatLaf no encontrado - Usando look and feel por defecto");
        }

        // Verificar Jackson
        try {
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            System.out.println("OK - Jackson disponible (JSON)");
        } catch (ClassNotFoundException e) {
            System.out.println("ERROR - Jackson no encontrado - No se podra leer config.json");
        }

        System.out.println();
    }

    /**
     * Verifica si un driver especifico esta disponible
     */
    private static void verificarDriver(String driverClass, String nombre) {
        try {
            Class.forName(driverClass);
            System.out.println("OK - Driver " + nombre + " disponible");
        } catch (ClassNotFoundException e) {
            System.out.println("ERROR - Driver " + nombre + " no encontrado");
        }
    }

    /**
     * Carga la configuracion inicial y muestra un resumen
     */
    private static void cargarConfiguracionInicial() {
        try {
            Ajustes config = CargadorConfig.cargarYValidar();
            System.out.println("OK - Configuracion cargada exitosamente");
            System.out.println("   Tipo DB: " + CargadorConfig.detectarTipoDB(config.url()));
            System.out.println("   URL: " + config.url());
            System.out.println("   Usuario: " + config.usuario());
            System.out.println("   Muestras: " + config.muestras());
            System.out.println("   Pool size: " + config.limitePool());
            System.out.println();

        } catch (Exception e) {
            System.out.println("ATENCION - No se pudo cargar la configuracion: " + e.getMessage());
            System.out.println("   Usando valores por defecto");
            System.out.println();
        }
    }

    /**
     * Configura el look and feel de la aplicacion
     */
    private static void configurarLookAndFeel() {
        try {
            switch (temaActual) {
                case OSCURO:
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                    System.out.println("Look and feel oscuro configurado");
                    break;

                case CLARO:
                    UIManager.setLookAndFeel(new FlatLightLaf());
                    System.out.println("Look and feel claro configurado");
                    break;

                case SISTEMA:
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    System.out.println("Look and feel del sistema configurado");
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error configurando look and feel: " + e.getMessage());

            // Fallback al look and feel por defecto
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ex) {
                System.err.println("Error fatal configurando look and feel por defecto");
            }
        }
    }

    /**
     * Limpia los recursos antes de cerrar la aplicacion
     */
    private static void limpiarRecursos() {
        System.out.println("Cerrando aplicacion y liberando recursos...");

        try {
            // Cerrar DBManager
            DBManager.getInstance().shutdownAll();

            // Log de cierre
            RegistradorLog.escribir(
                    -1,
                    "APLICACION FINALIZADA",
                    0,
                    0,
                    "MAIN",
                    ""
            );

        } catch (Exception e) {
            System.err.println("Error liberando recursos: " + e.getMessage());
        }

        System.out.println("Aplicacion cerrada correctamente");
    }

    /**
     * Maneja errores fatales de la aplicacion
     */
    private static void manejarErrorFatal(String mensaje, Exception e) {
        System.err.println("ERROR FATAL: " + mensaje);
        System.err.println("Detalles: " + e.getMessage());
        e.printStackTrace();

        // Intentar registrar el error
        try {
            RegistradorLog.escribir(
                    -1,
                    "ERROR FATAL: " + mensaje + " - " + e.getMessage(),
                    0,
                    0,
                    "MAIN",
                    ""
            );
        } catch (Exception ex) {
            // No se puede hacer nada mas
        }

        // Mostrar dialogo de error
        JOptionPane.showMessageDialog(
                null,
                "Error fatal: " + mensaje + "\n" + e.getMessage(),
                "Error de aplicacion",
                JOptionPane.ERROR_MESSAGE
        );

        System.exit(1);
    }
}
