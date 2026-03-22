package com.MariaBermudez.db;

import com.MariaBermudez.modelos.Ajustes;

public class DBManager {
    private static DBComponent<?> instancia;

    public static void iniciar(Ajustes config) throws Exception {
        if (instancia == null) instancia = DBFactory.crear(config);
    }

    public static DBComponent<?> get() { return instancia; }
}