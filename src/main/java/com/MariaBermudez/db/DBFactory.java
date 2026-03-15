package com.MariaBermudez.db;

import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.db.adapters.PostgreSQLAdapter;
import com.MariaBermudez.db.adapters.MySQLAdapter;

/**
 * Fábrica para crear adaptadores de base de datos
 */
public class DBFactory {

    public enum TipoDB {
        POSTGRESQL,
        MYSQL,
        MARIADB,
        AUTO_DETECT
    }

    /**
     * Crea un adaptador detectando automáticamente el tipo de BD
     */
    public static DBComponent crearAdapter(Ajustes ajustes, boolean usePool, String nombre) {
        TipoDB tipo = detectarTipo(ajustes.url());
        return crearAdapter(ajustes, usePool, nombre, tipo);
    }

    /**
     * Crea un adaptador con tipo específico
     */
    public static DBComponent crearAdapter(Ajustes ajustes, boolean usePool, String nombre, TipoDB tipo) {
        switch (tipo) {
            case POSTGRESQL:
                return new PostgreSQLAdapter(ajustes, usePool, nombre);
            case MYSQL:
            case MARIADB:
                return new MySQLAdapter(ajustes, usePool, nombre);
            case AUTO_DETECT:
                return detectarYCrear(ajustes, usePool, nombre);
            default:
                throw new IllegalArgumentException("Tipo no soportado: " + tipo);
        }
    }

    private static DBComponent detectarYCrear(Ajustes ajustes, boolean usePool, String nombre) {
        String url = ajustes.url().toLowerCase();

        if (url.contains("postgresql")) {
            return new PostgreSQLAdapter(ajustes, usePool, nombre);
        } else if (url.contains("mysql") || url.contains("mariadb")) {
            return new MySQLAdapter(ajustes, usePool, nombre);
        } else {
            throw new IllegalArgumentException(
                    "No se pudo detectar el tipo de BD en la URL: " + ajustes.url()
            );
        }
    }

    private static TipoDB detectarTipo(String url) {
        String lowerUrl = url.toLowerCase();

        if (lowerUrl.contains("postgresql")) {
            return TipoDB.POSTGRESQL;
        } else if (lowerUrl.contains("mysql")) {
            return TipoDB.MYSQL;
        } else if (lowerUrl.contains("mariadb")) {
            return TipoDB.MARIADB;
        } else {
            return TipoDB.AUTO_DETECT;
        }
    }

    /**
     * Verifica si un driver específico está disponible
     */
    public static boolean isDriverDisponible(String driverClass) {
        try {
            Class.forName(driverClass);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}