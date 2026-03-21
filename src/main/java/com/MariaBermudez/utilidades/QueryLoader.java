package com.MariaBermudez.utilidades;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.toml.TomlFactory;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

public class QueryLoader {
    public static Map<String, String> cargar(String nombreArchivo) throws Exception {
        if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de archivo vacío");
        }
        String nombre = nombreArchivo.trim();
        String lower = nombre.toLowerCase();

        InputStream is = null;
        List<String> intentos = new ArrayList<>();

        // 1) Intentar classloader del tipo QueryLoader
        ClassLoader cl = QueryLoader.class.getClassLoader();
        try {
            if (cl != null) {
                intentos.add("classpath: " + nombre);
                is = cl.getResourceAsStream(nombre);
            }
        } catch (Throwable t) { /* ignorar */ }

        // 2) Intentar classloader del hilo (otra estrategia)
        if (is == null) {
            try {
                ClassLoader tcl = Thread.currentThread().getContextClassLoader();
                if (tcl != null) {
                    intentos.add("thread context classloader: " + nombre);
                    is = tcl.getResourceAsStream(nombre);
                }
            } catch (Throwable t) { /* ignorar */ }
        }

        // 3) Intentar con prefijo '/'
        if (is == null) {
            try {
                if (cl != null) {
                    intentos.add("classpath: /" + nombre);
                    is = cl.getResourceAsStream('/' + nombre);
                }
                if (is == null) {
                    ClassLoader tcl = Thread.currentThread().getContextClassLoader();
                    if (tcl != null) {
                        intentos.add("thread context classloader: /" + nombre);
                        is = tcl.getResourceAsStream('/' + nombre);
                    }
                }
            } catch (Throwable t) { /* ignorar */ }
        }

        // 4) Intentar en target/classes y en src/main/resources (modo desarrollo)
        if (is == null) {
            String userDir = System.getProperty("user.dir");
            File fTarget = new File(userDir, "target/classes/" + nombre);
            intentos.add("file: " + fTarget.getAbsolutePath());
            if (fTarget.exists()) {
                is = new FileInputStream(fTarget);
            } else {
                File fSrc = new File(userDir, "src/main/resources/" + nombre);
                intentos.add("file: " + fSrc.getAbsolutePath());
                if (fSrc.exists()) {
                    is = new FileInputStream(fSrc);
                }
            }
        }

        // 5) Finalmente intentar como ruta absoluta tal cual la puso el usuario
        if (is == null) {
            File f = new File(nombre);
            intentos.add("file: " + f.getAbsolutePath());
            if (f.exists()) {
                is = new FileInputStream(f);
            }
        }

        if (is == null) {
            // Registrar intentos en la consola para debugging y lanzar excepción con detalle
            StringBuilder sb = new StringBuilder();
            sb.append("Intentos para localizar '").append(nombre).append("':\n");
            for (String s : intentos) sb.append(" - ").append(s).append('\n');
            System.err.println(sb.toString());
            throw new RuntimeException("No se encontró el archivo: " + nombre + ". Rutas intentadas:\n" + sb.toString());
        }

        // Determinar formato por extensión
        if (lower.endsWith(".properties")) {
            Properties p = new Properties();
            try (InputStream pis = is) {
                p.load(pis);
            }
            Map<String, String> map = new HashMap<>();
            for (String k : p.stringPropertyNames()) {
                map.put(k, p.getProperty(k));
            }
            return map;
        }

        ObjectMapper mapper;
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) {
            mapper = new ObjectMapper(new YAMLFactory());
        } else if (lower.endsWith(".toml")) {
            mapper = new ObjectMapper(new TomlFactory());
        } else {
            // Por defecto usar JSON
            mapper = new ObjectMapper();
        }

        try (InputStream jis = is) {
            // Usar TypeReference con operador diamante para evitar la advertencia del compilador
            return mapper.readValue(jis, new TypeReference<>() {});
        }
    }
}