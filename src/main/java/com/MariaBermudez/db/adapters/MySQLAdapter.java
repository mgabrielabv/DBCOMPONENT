package com.MariaBermudez.db.adapters;


public class MySQLAdapter implements IAdapter {
    @Override
    public java.sql.Connection conectar(String url, String user, String pass) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return java.sql.DriverManager.getConnection(url, user, pass);
    }
}