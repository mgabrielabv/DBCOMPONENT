package com.MariaBermudez.db;

import com.MariaBermudez.modelos.Ajustes;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Componente abstracto para adaptadores de base de datos
 */
public abstract class DBComponent {
    protected final Ajustes ajustes;
    protected final String tipoMotor;
    protected final String nombre;

    protected DBComponent(Ajustes ajustes, String tipoMotor, String nombre) {
        this.ajustes = ajustes;
        this.tipoMotor = tipoMotor;
        this.nombre = nombre;
    }

    /**
     * Obtiene una conexión a la base de datos
     */
    public abstract Connection getConnection() throws SQLException;

    /**
     * Cierra todos los recursos del componente
     */
    public abstract void shutdown();

    /**
     * Verifica si la conexión es válida
     */
    public abstract boolean isHealthy();

    /**
     * Obtiene el tipo de motor (PostgreSQL, MySQL, etc.)
     */
    public String getTipoMotor() {
        return tipoMotor;
    }

    /**
     * Obtiene el nombre identificador del componente
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Escapa un identificador según el motor específico
     */
    public abstract String escapeIdentifier(String identifier);

    @Override
    public String toString() {
        return String.format("DBComponent[nombre=%s, tipo=%s]", nombre, tipoMotor);
    }
}