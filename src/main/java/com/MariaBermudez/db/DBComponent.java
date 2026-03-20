package com.MariaBermudez.db;

import com.MariaBermudez.db.adapters.IAdapter;
import java.sql.*;
import java.util.Map;
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

    public void transaction() throws Exception {
        if (conexionTransaccional == null) {
            conexionTransaccional = pool.take();
            conexionTransaccional.setAutoCommit(false);
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
}