package com.MariaBermudez.GUI;

import com.MariaBermudez.db.DBManager;
import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.configuracion.CargadorConfig;
import com.MariaBermudez.utilidades.QueryLoader;
import javax.swing.SwingUtilities;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DBController {

    public void conectar(String rutaConfig, String rutaQueries, Consumer<String> logger, Runnable onSuccess) {
        try {
            Ajustes a = CargadorConfig.leer(rutaConfig);

            if (rutaQueries != null && !rutaQueries.isBlank()) {
                Map<String, String> qMap = QueryLoader.cargar(rutaQueries.trim());
                if (!qMap.isEmpty()) {
                    a.getQueries().putAll(qMap);
                    logger.accept("SISTEMA: Queries inyectadas desde " + rutaQueries);
                }
            }

            DBManager.iniciar(a);
            logger.accept("SISTEMA: Conexión establecida con éxito.");
            SwingUtilities.invokeLater(onSuccess);
        } catch (Exception e) {
            logger.accept("ERROR: " + e.getMessage());
        }
    }

    public void ejecutarPrueba(String qId, int total, BiConsumer<Integer, Integer> progreso, Consumer<String> callback) {

        final int[] ok = {0};
        final int[] err = {0};

        new Thread(() -> {
            try {
                DBManager.get().begin();
                for (int i = 1; i <= total; i++) {
                    try {
                        DBManager.get().query(qId, rs -> null);
                        ok[0]++;
                    } catch (Exception ex) {
                        err[0]++;
                    }

                    int actualOk = ok[0];
                    int actualErr = err[0];
                    int paso = i;

                    if (paso % 10 == 0 || paso == total) {
                        SwingUtilities.invokeLater(() -> progreso.accept(actualOk, actualErr));
                    }
                }
                DBManager.get().commit();

                int finalOk = ok[0];
                int finalErr = err[0];
                SwingUtilities.invokeLater(() ->
                        callback.accept("TEST FINALIZADO. OK: " + finalOk + " | ERR: " + finalErr)
                );
            } catch (Exception e) {
                DBManager.get().rollback();
                SwingUtilities.invokeLater(() -> callback.accept("ERROR CRÍTICO: " + e.getMessage()));
            }
        }).start();
    }
}