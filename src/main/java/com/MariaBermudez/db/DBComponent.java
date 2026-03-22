package com.MariaBermudez.db;

import com.MariaBermudez.db.adapters.IAdapter;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DBComponent<T extends IAdapter> {
    private final ArrayBlockingQueue<Connection> pool;
    private final Map<String, String> queries;
    private Connection txConnection = null;

    public DBComponent(IAdapter adapter, String url, String user, String pass, int poolSize, Map<String, String> queries) throws Exception {
        this.queries = queries;
        this.pool = new ArrayBlockingQueue<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            pool.add(adapter.conectar(url, user, pass));
        }
    }

    public <R> DBQueryResult<List<R>> query(String id, RowMapper<R> mapper) throws DBException {
        String sql = getSql(id);
        Connection con = null;
        try {
            con = (txConnection != null) ? txConnection : pool.poll(5, TimeUnit.SECONDS);
            if (con == null) throw new DBException(DBException.Category.CONNECTION, "Timeout esperando conexion", null);
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                List<R> list = new ArrayList<>();
                while (rs.next()) list.add(mapper.mapRow(rs));
                return new DBQueryResult<>(list, 0);
            }
        } catch (Exception e) { throw handleException(e); }
        finally { if (txConnection == null && con != null) pool.offer(con); }
    }

    public DBQueryResult<Integer> update(String id) throws DBException {
        String sql = getSql(id);
        Connection con = null;
        try {
            con = (txConnection != null) ? txConnection : pool.poll(5, TimeUnit.SECONDS);
            try (Statement st = con.createStatement()) {
                int affected = st.executeUpdate(sql);
                return new DBQueryResult<>(affected, affected);
            }
        } catch (Exception e) { throw handleException(e); }
        finally { if (txConnection == null && con != null) pool.offer(con); }
    }

    public void begin() throws Exception {
        if (txConnection == null) { txConnection = pool.take(); txConnection.setAutoCommit(false); }
    }

    public void commit() throws Exception {
        if (txConnection != null) { try { txConnection.commit(); } finally { finalizeTx(); } }
    }

    public void rollback() {
        if (txConnection != null) { try { txConnection.rollback(); } catch (SQLException e) {} finally { finalizeTx(); } }
    }

    private void finalizeTx() {
        try { txConnection.setAutoCommit(true); } catch (Exception e) {}
        pool.offer(txConnection);
        txConnection = null;
    }

    private String getSql(String id) throws DBException {
        String sql = queries.get(id);
        if (sql == null) throw new DBException(DBException.Category.SYNTAX, "ID de query inexistente: " + id, null);
        return sql;
    }

    private DBException handleException(Exception e) {
        return (e instanceof SQLException se) ? DBException.fromSQLException(se) : new DBException(DBException.Category.UNKNOWN, e.getMessage(), e);
    }
}