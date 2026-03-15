package com.MariaBermudez.db.adapters;

import com.MariaBermudez.db.DBComponent;
import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.motores.MotorPool;
import com.MariaBermudez.motores.MotorRaw;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Adaptador específico para PostgreSQL
 */
public class PostgreSQLAdapter extends DBComponent {
    private MotorPool poolConnection;
    private MotorRaw rawConnection;
    private final boolean usePool;

    public PostgreSQLAdapter(Ajustes ajustes, boolean usePool, String nombre) {
        super(ajustes, "PostgreSQL", nombre);
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
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    /**
     * Método específico de PostgreSQL para consultas JSON
     */
    public String jsonExtract(String jsonColumn, String path) {
        return String.format("%s->>'%s'", escapeIdentifier(jsonColumn), path);
    }

    /**
     * Método específico de PostgreSQL para paginación
     */
    public String paginar(String query, int limit, int offset) {
        return query + " LIMIT " + limit + " OFFSET " + offset;
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