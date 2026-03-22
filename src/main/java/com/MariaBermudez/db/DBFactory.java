package com.MariaBermudez.db;

import com.MariaBermudez.db.adapters.*;
import com.MariaBermudez.modelos.Ajustes;

public class DBFactory {
    public static DBComponent<?> crear(Ajustes config) throws Exception {
        String driver = config.getDriver().toLowerCase();
        IAdapter adapter = driver.contains("mysql") ? new MySQLAdapter() :
                driver.contains("postgresql") ? new PostgreSQLAdapter() : null;
        if (adapter == null) throw new Exception("Driver no soportado: " + driver);

        return new DBComponent<>(adapter, config.getUrl(), config.getUsuario(), config.getClave(), config.getPoolSize(), config.getQueries());
    }
}