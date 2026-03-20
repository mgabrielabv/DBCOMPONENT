package com.MariaBermudez.db.adapters;

public interface IAdapter {
    java.sql.Connection conectar(String url, String user, String pass) throws Exception;
}