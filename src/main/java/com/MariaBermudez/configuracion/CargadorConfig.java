package com.MariaBermudez.configuracion;

import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.utilidades.QueryLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.toml.TomlFactory;
import java.io.*;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

public class CargadorConfig {

    public static Ajustes leer(String ruta) throws Exception {
        File archivo = new File(ruta);
        if (!archivo.exists()) throw new FileNotFoundException("Archivo no encontrado: " + ruta);

        String ext = ruta.toLowerCase();
        Ajustes ajustes;

        if (ext.endsWith(".json")) ajustes = new ObjectMapper().readValue(archivo, Ajustes.class);
        else if (ext.endsWith(".yaml") || ext.endsWith(".yml")) ajustes = new ObjectMapper(new YAMLFactory()).readValue(archivo, Ajustes.class);
        else if (ext.endsWith(".toml")) ajustes = new ObjectMapper(new TomlFactory()).readValue(archivo, Ajustes.class);
        else if (ext.endsWith(".properties")) ajustes = cargarDesdeProperties(archivo);
        else throw new IllegalArgumentException("Formato no soportado: " + ruta);

        if (ajustes.getQueries() == null) ajustes.setQueries(new HashMap<>());

        String qfile = ajustes.getQueriesFile();
        if (qfile != null && !qfile.isBlank()) {
            try {
                Map<String, String> externas = QueryLoader.cargar(qfile.trim());
                ajustes.getQueries().putAll(externas);
            } catch (Exception ex) {
                System.err.println("Advertencia: No se cargó el archivo de queries: " + ex.getMessage());
            }
        }
        return ajustes;
    }

    private static Ajustes cargarDesdeProperties(File archivo) throws Exception {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(archivo)) { p.load(fis); }
        Ajustes a = new Ajustes();
        a.setUrl(p.getProperty("db.url"));
        a.setUsuario(p.getProperty("db.user"));
        a.setClave(p.getProperty("db.password"));
        a.setDriver(p.getProperty("db.driver"));
        a.setPoolSize(Integer.parseInt(p.getProperty("db.poolSize", "10")));
        a.setQueriesFile(p.getProperty("queriesFile"));
        p.forEach((k, v) -> {
            if (k.toString().startsWith("sql.")) a.getQueries().put(k.toString().replace("sql.", ""), v.toString());
        });
        return a;
    }
}