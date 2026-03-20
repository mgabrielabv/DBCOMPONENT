package com.MariaBermudez.modelos;

import java.util.Map;
import java.util.HashMap;

public class Ajustes {
    private String url;
    private String usuario;
    private String clave;
    private String driver;
    private int poolSize;
    // Aquí vive la lista interna de queries predefinidas
    private Map<String, String> queries = new HashMap<>();

    // Getters y Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getDriver() { return driver; }
    public void setDriver(String driver) { this.driver = driver; }
    public int getPoolSize() { return poolSize; }
    public void setPoolSize(int poolSize) { this.poolSize = poolSize; }
    public Map<String, String> getQueries() { return queries; }
    public void setQueries(Map<String, String> queries) { this.queries = queries; }
}