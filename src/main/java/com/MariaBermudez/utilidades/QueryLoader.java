package com.MariaBermudez.utilidades;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Map;

public class QueryLoader {
    public static Map<String, String> cargar(String nombreArchivo) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = QueryLoader.class.getClassLoader().getResourceAsStream(nombreArchivo)) {
            if (is == null) throw new RuntimeException("No se encontró el archivo: " + nombreArchivo);
            return mapper.readValue(is, new TypeReference<Map<String, String>>() {});
        }
    }
}