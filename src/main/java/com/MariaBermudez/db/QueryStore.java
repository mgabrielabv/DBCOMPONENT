package com.MariaBermudez.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacén singleton de queries predefinidas cargadas desde resources/queries.json
 */
public class QueryStore {
    private static volatile QueryStore instance;
    private final Map<String, String> queries = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String DEFAULT_RESOURCE = "queries.json";

    private QueryStore() {
        cargarDesdeResources(DEFAULT_RESOURCE);
    }

    public static QueryStore getInstance() {
        if (instance == null) {
            synchronized (QueryStore.class) {
                if (instance == null) {
                    instance = new QueryStore();
                }
            }
        }
        return instance;
    }

    private void cargarDesdeResources(String resource) {
        try (InputStream is = QueryStore.class.getClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                System.err.println("QueryStore: recurso no encontrado: " + resource);
                return;
            }
            Map<String, String> loaded = mapper.readValue(is, new TypeReference<Map<String, String>>() {});
            if (loaded != null) {
                queries.putAll(loaded);
                System.out.println("QueryStore: cargadas " + loaded.size() + " queries desde " + resource);
            }
        } catch (Exception e) {
            System.err.println("Error cargando queries desde resources: " + e.getMessage());
        }
    }

    public String getQuery(String nombre) {
        return queries.get(nombre);
    }

    public Set<String> getQueryNames() {
        return Collections.unmodifiableSet(queries.keySet());
    }

    public void reload() {
        queries.clear();
        cargarDesdeResources(DEFAULT_RESOURCE);
    }
}

