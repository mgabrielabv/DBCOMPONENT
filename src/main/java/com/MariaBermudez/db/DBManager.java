package com.MariaBermudez.db;

import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.utilidades.RegistradorLog;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.MariaBermudez.db.QueryStore;

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
     * Ejecuta una consulta con callback (API legacy: pasa Connection directamente).
     * Nota: esta API permite ejecutar SQL crudo si el callback lo realiza, úsela solo internamente.
     */
    @Deprecated
    public <T> T ejecutarQuery(String nombreAdapter, QueryCallback<T> callback) throws SQLException {
        DBComponent adapter = getAdapter(nombreAdapter);

        try (Connection conn = adapter.getConnection()) {
            return callback.ejecutar(conn);
        }
    }

    /**
     * Ejecuta una consulta con el adapter por defecto (legacy)
     */
    @Deprecated
    public <T> T ejecutarQuery(QueryCallback<T> callback) throws SQLException {
        return ejecutarQuery(getDefaultAdapter(), callback);
    }

    private <T> T ejecutarQuery(DBComponent adapter, QueryCallback<T> callback) throws SQLException {
        try (Connection conn = adapter.getConnection()) {
            return callback.ejecutar(conn);
        }
    }

    /**
     * Ejecuta una transacción (legacy)
     */
    @Deprecated
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
     * Ejecuta una consulta predefinida (SELECT) identificada por nombre.
     * Solo permite ejecutar SQL predefinido cargado por QueryStore y evita exponer SQL crudo.
     */
    public interface QueryResultHandler<T> {
        T handle(ResultSet rs) throws SQLException;
    }

    public <T> T ejecutarQueryPorNombre(String nombreAdapter, String nombreQuery, Object[] params, QueryResultHandler<T> handler) throws SQLException {
        DBComponent adapter = getAdapter(nombreAdapter);
        String sql = QueryStore.getInstance().getQuery(nombreQuery);
        if (sql == null) {
            throw new IllegalArgumentException("No existe la query definida: " + nombreQuery);
        }

        try (Connection conn = adapter.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                return handler.handle(rs);
            }
        }
    }

    /**
     * Ejecuta una consulta predefinida de actualización (INSERT/UPDATE/DELETE) por nombre.
     */
    public int ejecutarUpdatePorNombre(String nombreAdapter, String nombreQuery, Object[] params) throws SQLException {
        DBComponent adapter = getAdapter(nombreAdapter);
        String sql = QueryStore.getInstance().getQuery(nombreQuery);
        if (sql == null) {
            throw new IllegalArgumentException("No existe la query definida: " + nombreQuery);
        }

        try (Connection conn = adapter.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }
            return ps.executeUpdate();
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
     * Callback para queries (legacy)
     */
    @FunctionalInterface
    public interface QueryCallback<T> {
        T ejecutar(Connection conn) throws SQLException;
    }

    /**
     * Callback para transacciones (legacy)
     */
    @FunctionalInterface
    public interface TransaccionCallback<T> {
        T ejecutar(Connection conn) throws SQLException;
    }
}
