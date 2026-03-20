package com.MariaBermudez.configuracion;

import com.MariaBermudez.modelos.Ajustes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.toml.TomlFactory; // <--- Importante
import java.io.*;
import java.util.Properties;
import java.util.HashMap;

public class CargadorConfig {

    public static Ajustes leer(String ruta) throws Exception {
        File archivo = new File(ruta);
        if (!archivo.exists()) {
            throw new FileNotFoundException("No se encontró el archivo: " + ruta);
        }

        String rutaMinuscula = ruta.toLowerCase();

        if (rutaMinuscula.endsWith(".json")) {
            return cargarDesdeJSON(archivo);
        }
        else if (rutaMinuscula.endsWith(".yaml") || rutaMinuscula.endsWith(".yml")) {
            return cargarDesdeYAML(archivo);
        }
        else if (rutaMinuscula.endsWith(".toml")) {
            return cargarDesdeTOML(archivo);
        }
        else if (rutaMinuscula.endsWith(".properties")) {
            return cargarDesdeProperties(archivo);
        }
        else {
            throw new IllegalArgumentException("Formato no soportado: " + ruta);
        }
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