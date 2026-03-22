package com.MariaBermudez.GUI;

import com.MariaBermudez.db.DBManager;
import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.configuracion.CargadorConfig;
import javax.swing.SwingUtilities;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DBController {

    public void conectar(String ruta, Consumer<String> logger, Runnable onSuccess) {
        try {
            Ajustes a = CargadorConfig.leer(ruta);
            DBManager.iniciar(a);
            logger.accept("SISTEMA: Motor conectado y listo.");
            SwingUtilities.invokeLater(onSuccess);
        } catch (Exception e) {
            logger.accept("ERROR: No se pudo conectar -> " + e.getMessage());
        }
    }

    public void ejecutarPrueba(String qId, int total, BiConsumer<Integer, Integer> progreso, Consumer<String> callback) {
        new Thread(() -> {
            int ok = 0;
            int err = 0;
            try {
                DBManager.get().begin();

                for (int i = 1; i <= total; i++) {
                    try {
                        DBManager.get().query(qId, rs -> null);
                        ok++;
                    } catch (Exception ex) {
                        err++;
                    }

                    final int fOk = ok, fErr = err, fI = i;
                    if (fI % 10 == 0 || fI == total) {
                        SwingUtilities.invokeLater(() -> progreso.accept(fOk, fErr));
                    }
                }

                DBManager.get().commit();
                final int finalOk = ok;
                SwingUtilities.invokeLater(() -> callback.accept("PRUEBA EXITOSA: " + finalOk + " registros procesados."));

            } catch (Exception e) {
                DBManager.get().rollback();
                SwingUtilities.invokeLater(() -> callback.accept("TRANSACCION ABORTADA: " + e.getMessage()));
            }
        }).start();
    }
}