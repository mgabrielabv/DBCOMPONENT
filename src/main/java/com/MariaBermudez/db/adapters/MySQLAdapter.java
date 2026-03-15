package com.MariaBermudez.db.adapters;

import com.MariaBermudez.db.DBComponent;
import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.motores.MotorPool;
import com.MariaBermudez.motores.MotorRaw;
import java.sql.Connection;
import java.sql.SQLException;

public class MySQLAdapter extends DBComponent {
    private MotorPool poolConnection;
    private MotorRaw rawConnection;
    private final boolean usePool;

    public MySQLAdapter(Ajustes ajustes, boolean usePool, String nombre) {
        super(ajustes, "MySQL/MariaDB", nombre);
        this.usePool = usePool;

        if (usePool) {
            this.poolConnection = MotorPool.getInstance(ajustes);
        } else {
            this.rawConnection = new MotorRaw(ajustes);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (usePool) {
            return poolConnection.obtenerConexion();
        } else {
            return rawConnection.obtenerConexion();
        }
    }

    @Override
    public void shutdown() {
        if (usePool && poolConnection != null) {
            poolConnection.cerrar();
        }
    }

    @Override
    public boolean isHealthy() {
        try (Connection conn = getConnection()) {
            return conn != null && conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public String escapeIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    /**
     * Método específico de MySQL para formatear fechas
     */
    public String dateFormat(String column, String format) {
        return String.format("DATE_FORMAT(%s, '%s')", escapeIdentifier(column), format);
    }

    /**
     * Método específico de MySQL para paginación
     */
    public String paginar(String query, int limit, int offset) {
        return query + " LIMIT " + offset + ", " + limit;
    }

    /**
     * Método específico de MySQL para INSERT IGNORE
     */
    public String insertIgnore(String table, String columns, String values) {
        return "INSERT IGNORE INTO " + escapeIdentifier(table) +
                " (" + columns + ") VALUES (" + values + ")";
    }

    /**
     * Obtiene estadísticas del pool si está usando pool
     */
    public String getEstadisticasPool() {
        if (usePool && poolConnection != null) {
            return poolConnection.getEstadisticas();
        }
        return "No usando pool de conexiones";
    }
}