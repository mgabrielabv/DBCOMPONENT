package com.MariaBermudez.db;

import com.MariaBermudez.db.adapters.IAdapter;
import java.sql.*;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class DBComponent<T extends IAdapter> {
    private final ArrayBlockingQueue<Connection> pool;
    private final Map<String, String> queries;
    private Connection conexionTransaccional = null;

    public DBComponent(IAdapter adapter, String url, String user, String pass, int poolSize, Map<String, String> queries) throws Exception {
        this.queries = queries;
        this.pool = new ArrayBlockingQueue<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            pool.add(adapter.conectar(url, user, pass));
        }
    }

    public void query(String id) throws Exception {
        String sql = queries.get(id);
        if (sql == null) throw new Exception("ID de query no encontrado: " + id);


        Connection con = (conexionTransaccional != null) ? conexionTransaccional : pool.take();

        try (Statement st = con.createStatement()) {
            st.execute(sql);
        } finally {
            if (conexionTransaccional == null) pool.offer(con);
        }
    }

    // Ejecutar SQL crudo (sin usar el mapa de queries)
    public void executeRaw(String sql) throws Exception {
        if (sql == null || sql.trim().isEmpty()) throw new Exception("SQL vacío");

        Connection con = (conexionTransaccional != null) ? conexionTransaccional : pool.take();
        try (Statement st = con.createStatement()) {
            st.execute(sql);
        } finally {
            if (conexionTransaccional == null) pool.offer(con);
        }
    }

    // Iniciar una transacción. Si ya existe, no hace nada.
    public void transaction() throws Exception {
        if (conexionTransaccional == null) {
            conexionTransaccional = pool.take();
            conexionTransaccional.setAutoCommit(false);
        }
    }

    // Ejecutar una lista de ids de queries dentro de una transacción (commit al final).
    // Si ocurre un error se hace rollback automático.
    public void executeBatchByIds(List<String> ids) throws Exception {
        if (ids == null || ids.isEmpty()) return;

        boolean startedHere = false;
        try {
            if (conexionTransaccional == null) {
                transaction();
                startedHere = true;
            }

            for (String id : ids) {
                String sql = queries.get(id);
                if (sql == null) throw new Exception("ID de query no encontrado en batch: " + id);
                try (Statement st = conexionTransaccional.createStatement()) {
                    st.execute(sql);
                }
            }

            if (startedHere) commit();
        } catch (Exception ex) {
            // Si la transacción fue iniciada aquí intentamos rollback
            if (startedHere) {
                try { rollback(); } catch (Exception r) { /* ignorar*/ }
            }
            throw ex;
        }
    }

    public void commit() throws Exception {
        if (conexionTransaccional != null) {
            conexionTransaccional.commit();
            conexionTransaccional.setAutoCommit(true);
            pool.offer(conexionTransaccional);
            conexionTransaccional = null;
        }
    }

    public void rollback() throws Exception {
        if (conexionTransaccional != null) {
            conexionTransaccional.rollback();
            conexionTransaccional.setAutoCommit(true);
            pool.offer(conexionTransaccional);
            conexionTransaccional = null;
        }
    }
}