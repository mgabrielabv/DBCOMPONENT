package com.MariaBermudez.utilidades;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.toml.TomlFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class QueryLoader {
    public static Map<String, String> cargar(String ruta) throws Exception {
        try (InputStream is = QueryLoader.class.getResourceAsStream("/" + ruta)) {
            if (is == null) return new HashMap<>();
            ObjectMapper mapper = ruta.endsWith(".json") ? new ObjectMapper() :
                    (ruta.endsWith(".yaml") || ruta.endsWith(".yml")) ? new ObjectMapper(new YAMLFactory()) :
                            ruta.endsWith(".toml") ? new ObjectMapper(new TomlFactory()) : null;
            if (mapper == null) return new HashMap<>();
            return mapper.readValue(is, mapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
        }
    }
}