package com.MariaBermudez.configuracion;

import com.MariaBermudez.modelos.Ajustes;
import com.MariaBermudez.utilidades.QueryLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.toml.TomlFactory; // <--- Importante
import java.io.*;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

public class CargadorConfig {

    public static Ajustes leer(String ruta) throws Exception {
        File archivo = new File(ruta);
        if (!archivo.exists()) {
            throw new FileNotFoundException("No se encontró el archivo: " + ruta);
        }

        String rutaMinuscula = ruta.toLowerCase();

        Ajustes ajustes;

        if (rutaMinuscula.endsWith(".json")) {
            ajustes = cargarDesdeJSON(archivo);
        }
        else if (rutaMinuscula.endsWith(".yaml") || rutaMinuscula.endsWith(".yml")) {
            ajustes = cargarDesdeYAML(archivo);
        }
        else if (rutaMinuscula.endsWith(".toml")) {
            ajustes = cargarDesdeTOML(archivo);
        }
        else if (rutaMinuscula.endsWith(".properties")) {
            ajustes = cargarDesdeProperties(archivo);
        }
        else {
            throw new IllegalArgumentException("Formato no soportado: " + ruta);
        }

        // Si en los ajustes viene indicado un archivo de queries, intentar cargarlo desde resources
        String qfile = ajustes.getQueriesFile();
        if (qfile != null && !qfile.trim().isEmpty()) {
            try {
                Map<String, String> externas = QueryLoader.cargar(qfile.trim());
                if (externas != null) {
                    // Las externas sobrescriben o se añaden a las queries ya definidas
                    ajustes.getQueries().putAll(externas);
                }
            } catch (Exception ex) {
                // No hacer fallar la carga del config por un archivo de queries faltante,
                // pero informar en la traza para que el desarrollador lo note.
                System.err.println("Advertencia: no se pudo cargar el archivo de queries '" + qfile + "': " + ex.getMessage());
            }
        }

        return ajustes;
    }

    private static Ajustes cargarDesdeJSON(File archivo) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return validar(mapper.readValue(archivo, Ajustes.class));
    }

    private static Ajustes cargarDesdeYAML(File archivo) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return validar(mapper.readValue(archivo, Ajustes.class));
    }

    private static Ajustes cargarDesdeTOML(File archivo) throws Exception {
        // Usamos la fábrica de TOML que añadimos al pom.xml
        ObjectMapper mapper = new ObjectMapper(new TomlFactory());
        return validar(mapper.readValue(archivo, Ajustes.class));
    }

    private static Ajustes cargarDesdeProperties(File archivo) throws Exception {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(archivo)) {
            p.load(fis);
        }

        Ajustes a = new Ajustes();
        a.setQueries(new HashMap<>());

        a.setUrl(p.getProperty("db.url"));
        a.setUsuario(p.getProperty("db.user"));
        a.setClave(p.getProperty("db.password"));
        a.setDriver(p.getProperty("db.driver"));

        // Leer queriesFile si está presente
        String qfile = p.getProperty("queriesFile");
        if (qfile != null && !qfile.trim().isEmpty()) {
            a.setQueriesFile(qfile.trim());
        }

        String poolSizeStr = p.getProperty("db.poolSize", "10");
        a.setPoolSize(Integer.parseInt(poolSizeStr));

        p.forEach((k, v) -> {
            String key = k.toString();
            if (key.startsWith("sql.")) {
                a.getQueries().put(key.replace("sql.", ""), v.toString());
            }
        });
        return a;
    }

    private static Ajustes validar(Ajustes a) {
        if (a.getQueries() == null) a.setQueries(new HashMap<>());
        return a;
    }
}