package com.MariaBermudez.db;

import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.utilidades.RegistradorLog;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor centralizado de adaptadores de base de datos
 */
public class DBManager {
    private static volatile DBManager instancia;
    private final Map<String, DBComponent> adaptadores = new ConcurrentHashMap<>();
    private DBComponent defaultAdapter;

    private DBManager() {}

    public static DBManager getInstance() {
        if (instancia == null) {
            synchronized (DBManager.class) {
                if (instancia == null) {
                    instancia = new DBManager();
                }
            }
        }
        return instancia;
    }

    /**
     * Registra un adaptador con detección automática
     */
    public DBComponent registrarAdapter(Ajustes ajustes, boolean usePool, String nombre) {
        DBComponent adapter = DBFactory.crearAdapter(ajustes, usePool, nombre);
        adaptadores.put(nombre, adapter);

        if (defaultAdapter == null) {
            defaultAdapter = adapter;
        }

        RegistradorLog.escribir(
                -1,
                "ADAPTER REGISTRADO: " + nombre + " - " + adapter.getTipoMotor(),
                0, 0, "DBManager", ""
        );

        return adapter;
    }

    /**
     * Registra un adaptador con tipo específico
     */
    public DBComponent registrarAdapter(Ajustes ajustes, boolean usePool, String nombre, DBFactory.TipoDB tipo) {
        DBComponent adapter = DBFactory.crearAdapter(ajustes, usePool, nombre, tipo);
        adaptadores.put(nombre, adapter);

        if (defaultAdapter == null) {
            defaultAdapter = adapter;
        }

        RegistradorLog.escribir(
                -1,
                "ADAPTER REGISTRADO: " + nombre + " - " + adapter.getTipoMotor(),
                0, 0, "DBManager", ""
        );

        return adapter;
    }

    /**
     * Obtiene un adaptador por nombre
     */
    public DBComponent getAdapter(String nombre) {
        DBComponent adapter = adaptadores.get(nombre);
        if (adapter == null) {
            throw new IllegalArgumentException("No existe adapter: " + nombre);
        }
        return adapter;
    }

    /**
     * Obtiene el adaptador por defecto
     */
    public DBComponent getDefaultAdapter() {
        if (defaultAdapter == null) {
            throw new IllegalStateException("No hay adapter por defecto");
        }
        return defaultAdapter;
    }

    /**
     * Ejecuta una consulta con callback
     */
    public <T> T ejecutarQuery(String nombreAdapter, QueryCallback<T> callback) throws SQLException {
        DBComponent adapter = getAdapter(nombreAdapter);

        try (Connection conn = adapter.getConnection()) {
            return callback.ejecutar(conn);
        }
    }

    /**
     * Ejecuta una consulta con el adapter por defecto
     */
    public <T> T ejecutarQuery(QueryCallback<T> callback) throws SQLException {
        return ejecutarQuery(getDefaultAdapter(), callback);
    }

    private <T> T ejecutarQuery(DBComponent adapter, QueryCallback<T> callback) throws SQLException {
        try (Connection conn = adapter.getConnection()) {
            return callback.ejecutar(conn);
        }
    }

    /**
     * Ejecuta una transacción
     */
    public <T> T ejecutarTransaccion(String nombreAdapter, TransaccionCallback<T> callback) throws SQLException {
        DBComponent adapter = getAdapter(nombreAdapter);
        Connection conn = null;

        try {
            conn = adapter.getConnection();
            conn.setAutoCommit(false);

            T resultado = callback.ejecutar(conn);

            conn.commit();
            return resultado;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    e.addSuppressed(ex);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    RegistradorLog.escribir(-1, "ERROR CLOSE: " + e.getMessage(), 0, 0, "DBManager", "");
                }
            }
        }
    }

    /**
     * Cierra todos los adaptadores
     */
    public void shutdownAll() {
        adaptadores.values().forEach(adapter -> {
            try {
                adapter.shutdown();
                RegistradorLog.escribir(
                        -1,
                        "ADAPTER CERRADO: " + adapter.getNombre(),
                        0, 0, "DBManager", ""
                );
            } catch (Exception e) {
                RegistradorLog.escribir(
                        -1,
                        "ERROR CLOSE: " + e.getMessage(),
                        0, 0, "DBManager", ""
                );
            }
        });
        adaptadores.clear();
        defaultAdapter = null;
    }

    /**
     * Obtiene el número de adaptadores registrados
     */
    public int getCantidadAdapters() {
        return adaptadores.size();
    }

    /**
     * Verifica si existe un adaptador
     */
    public boolean existeAdapter(String nombre) {
        return adaptadores.containsKey(nombre);
    }

    /**
     * Callback para queries
     */
    @FunctionalInterface
    public interface QueryCallback<T> {
        T ejecutar(Connection conn) throws SQLException;
    }

    /**
     * Callback para transacciones
     */
    @FunctionalInterface
    public interface TransaccionCallback<T> {
        T ejecutar(Connection conn) throws SQLException;
    }
}
