package com.MariaBermudez.db;

import com.MariaBermudez.db.adapters.*;
import com.MariaBermudez.modelos.Ajustes;

public class DBFactory {
    public static DBComponent<?> crear(Ajustes config) throws Exception {
        IAdapter adapter;
        String driver = config.getDriver().toLowerCase();

        if (driver.contains("mysql")) {
            adapter = new MySQLAdapter();
        } else if (driver.contains("postgresql")) {
            adapter = new PostgreSQLAdapter();
        } else {
            throw new Exception("Adapter no implementado para: " + driver);
        }

        return new DBComponent<>(
                adapter,
                config.getUrl(),
                config.getUsuario(),
                config.getClave(),
                config.getPoolSize(),
                config.getQueries()
        );
    }
}