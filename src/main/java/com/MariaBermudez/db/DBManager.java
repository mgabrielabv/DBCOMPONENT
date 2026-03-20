package com.MariaBermudez.db;

import com.MariaBermudez.modelos.Ajustes;

public class DBManager {
    private static DBComponent<?> componente;

    public static void iniciar(Ajustes config) throws Exception {
        componente = DBFactory.crear(config);
    }

    public static DBComponent<?> getComponent() {
        return componente;
    }
}