package com.MariaBermudez.configuracion;

import com.MariaBermudez.modelos.Ajustes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class CargadorConfig {

    private static final String CONFIG_FILE = "config.json";
    private static final String CONFIG_BACKUP = "config.backup.json";
    private static final String CONFIG_PROPERTIES = "database.properties";

    private static final String CONFIG_POSTGRES = "config-postgres.json";
    private static final String CONFIG_MYSQL = "config-mysql.json";

    private static ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Carga la configuracion desde el archivo config.json en resources
     */
    public static Ajustes cargar() {
        return cargar(CONFIG_FILE);
    }

    /**
     * Carga configuracion especifica para PostgreSQL
     */
    public static Ajustes cargarPostgreSQL() {
        Ajustes config = cargar(CONFIG_POSTGRES);
        if (config == null) {
            config = new Ajustes(
                    "jdbc:postgresql://localhost:5432/mi_basedatos",
                    "postgres",
                    "password",
                    "SELECT * FROM usuarios LIMIT 100",
                    5000,
                    3,
                    100,
                    20
            );
        }
        return config;
    }

    /**
     * Carga configuracion especifica para MySQL
     */
    public static Ajustes cargarMySQL() {
        Ajustes config = cargar(CONFIG_MYSQL);
        if (config == null) {
            config = new Ajustes(
                    "jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                    "root",
                    "123456",
                    "SELECT * FROM usuarios LIMIT 100",
                    5000,
                    3,
                    100,
                    20
            );
        }
        return config;
    }

    /**
     * Carga la configuracion desde una ruta especifica
     */
    public static Ajustes cargar(String ruta) {
        // Primero intentar cargar desde classpath (resources)
        try (InputStream is = CargadorConfig.class.getClassLoader().getResourceAsStream(ruta)) {
            if (is != null) {
                try {
                    Ajustes ajustes = mapper.readValue(is, Ajustes.class);
                    System.out.println("Configuracion cargada desde resources: " + ruta);
                    return ajustes;
                } catch (Exception e) {
                    System.err.println("Error parseando " + ruta + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error leyendo " + ruta + " desde resources: " + e.getMessage());
        }

        // Si no esta en resources, intentar desde el sistema de archivos
        try {
            return cargarDesdeArchivo(ruta);
        } catch (Exception e) {
            System.err.println("No se encontro " + ruta + " en el sistema de archivos");
        }

        // Si todo falla, crear configuracion por defecto
        System.err.println("Usando configuracion por defecto");
        return crearConfiguracionDefault();
    }

    /**
     * Carga configuracion desde un archivo del sistema de archivos
     */
    private static Ajustes cargarDesdeArchivo(String ruta) throws IOException {
        Path path = Paths.get(ruta);
        if (Files.exists(path)) {
            System.out.println("Configuracion cargada desde archivo: " + path.toAbsolutePath());
            return mapper.readValue(Files.newBufferedReader(path), Ajustes.class);
        }

        // Buscar en el directorio actual
        path = Paths.get("./" + ruta);
        if (Files.exists(path)) {
            System.out.println("Configuracion cargada desde: " + path.toAbsolutePath());
            return mapper.readValue(Files.newBufferedReader(path), Ajustes.class);
        }

        throw new FileNotFoundException("No se encontro el archivo: " + ruta);
    }

    /**
     * Guarda la configuracion en un archivo
     */
    public static void guardarConfiguracion(Ajustes ajustes, String nombreArchivo) {
        try {
            mapper.writeValue(new File(nombreArchivo), ajustes);
            System.out.println("Configuracion guardada en: " + nombreArchivo);
        } catch (Exception e) {
            System.err.println("Error guardando configuracion: " + e.getMessage());
        }
    }

    /**
     * Crea una configuracion por defecto
     */
    public static Ajustes crearConfiguracionDefault() {
        return new Ajustes(
                "jdbc:postgresql://localhost:5432/mi_basedatos",
                "postgres",
                "password",
                "SELECT * FROM usuarios LIMIT 100",
                5000,
                3,
                100,
                20
        );
    }

    /**
     * Crea configuracion por defecto para MySQL
     */
    public static Ajustes crearConfiguracionMySQLDefault() {
        return new Ajustes(
                "jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                "root",
                "123456",
                "SELECT * FROM usuarios LIMIT 100",
                5000,
                3,
                100,
                20
        );
    }

    /**
     * Carga configuracion desde propiedades
     */
    public static Ajustes cargarDesdeProperties() {
        return cargarDesdeProperties(CONFIG_PROPERTIES);
    }

    /**
     * Carga configuracion desde un archivo properties especifico
     */
    public static Ajustes cargarDesdeProperties(String archivo) {
        Properties props = new Properties();

        // Intentar desde classpath
        try (InputStream is = CargadorConfig.class.getClassLoader().getResourceAsStream(archivo)) {
            if (is != null) {
                props.load(is);
                System.out.println("Properties cargado desde resources: " + archivo);
                return convertirPropertiesAAjustes(props);
            }
        } catch (Exception e) {
            System.err.println("Error cargando properties desde resources: " + e.getMessage());
        }

        // Intentar desde sistema de archivos
        try (FileInputStream fis = new FileInputStream(archivo)) {
            props.load(fis);
            System.out.println("Properties cargado desde archivo: " + archivo);
            return convertirPropertiesAAjustes(props);
        } catch (Exception e) {
            System.err.println("No se encontro " + archivo + ", usando valores por defecto");
        }

        return crearConfiguracionDefault();
    }

    /**
     * Convierte Properties a Ajustes
     */
    private static Ajustes convertirPropertiesAAjustes(Properties props) {
        return new Ajustes(
                props.getProperty("db.url", "jdbc:postgresql://localhost:5432/mi_db"),
                props.getProperty("db.usuario", "postgres"),
                props.getProperty("db.clave", "password"),
                props.getProperty("db.query", "SELECT * FROM usuarios LIMIT 100"),
                Integer.parseInt(props.getProperty("db.muestras", "5000")),
                Integer.parseInt(props.getProperty("db.reintentos", "3")),
                Integer.parseInt(props.getProperty("db.salto", "100")),
                Integer.parseInt(props.getProperty("db.pool.size", "20"))
        );
    }

    /**
     * Guarda configuracion como properties
     */
    public static void guardarComoProperties(Ajustes ajustes, String archivo) {
        Properties props = new Properties();

        props.setProperty("db.url", ajustes.url());
        props.setProperty("db.usuario", ajustes.usuario());
        props.setProperty("db.clave", ajustes.clave());
        props.setProperty("db.query", ajustes.query());
        props.setProperty("db.muestras", String.valueOf(ajustes.muestras()));
        props.setProperty("db.reintentos", String.valueOf(ajustes.reintentos()));
        props.setProperty("db.salto", String.valueOf(ajustes.salto()));
        props.setProperty("db.pool.size", String.valueOf(ajustes.limitePool()));

        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            props.store(fos, "Configuracion de base de datos");
            System.out.println("Properties guardado en: " + archivo);
        } catch (Exception e) {
            System.err.println("Error guardando properties: " + e.getMessage());
        }
    }

    /**
     * Valida que la configuracion sea correcta
     */
    public static boolean validarConfiguracion(Ajustes ajustes) {
        if (ajustes == null) {
            System.err.println("Error: Configuracion nula");
            return false;
        }

        boolean valido = true;

        if (ajustes.url() == null || ajustes.url().trim().isEmpty()) {
            System.err.println("Error: URL no puede estar vacia");
            valido = false;
        }

        if (ajustes.usuario() == null || ajustes.usuario().trim().isEmpty()) {
            System.err.println("Error: Usuario no puede estar vacio");
            valido = false;
        }

        if (ajustes.muestras() <= 0) {
            System.err.println("Error: Muestras debe ser > 0");
            valido = false;
        }

        if (ajustes.reintentos() < 0) {
            System.err.println("Error: Reintentos no puede ser negativo");
            valido = false;
        }

        if (ajustes.limitePool() <= 0) {
            System.err.println("Error: Limite del pool debe ser > 0");
            valido = false;
        }

        // Validar formato de URL
        if (!ajustes.url().startsWith("jdbc:")) {
            System.err.println("Error: URL debe comenzar con 'jdbc:'");
            valido = false;
        }

        return valido;
    }

    /**
     * Obtiene el tipo de base de datos desde la URL
     */
    public static String detectarTipoDB(String url) {
        if (url == null) return "desconocido";

        String lowerUrl = url.toLowerCase();

        if (lowerUrl.contains("postgresql")) {
            return "PostgreSQL";
        } else if (lowerUrl.contains("mysql")) {
            return "MySQL";
        } else if (lowerUrl.contains("mariadb")) {
            return "MariaDB";
        } else if (lowerUrl.contains("oracle")) {
            return "Oracle";
        } else if (lowerUrl.contains("sqlserver") || lowerUrl.contains("sql server")) {
            return "SQL Server";
        } else if (lowerUrl.contains("h2")) {
            return "H2";
        } else if (lowerUrl.contains("sqlite")) {
            return "SQLite";
        } else {
            return "desconocido";
        }
    }

    /**
     * Carga y valida la configuracion
     */
    public static Ajustes cargarYValidar() {
        Ajustes ajustes = cargar();

        if (!validarConfiguracion(ajustes)) {
            System.err.println("Configuracion invalida, usando valores por defecto");
            ajustes = crearConfiguracionDefault();
        }

        System.out.println("Configuracion cargada - Tipo DB: " + detectarTipoDB(ajustes.url()));
        System.out.println("   URL: " + ajustes.url());
        System.out.println("   Usuario: " + ajustes.usuario());
        System.out.println("   Muestras: " + ajustes.muestras());
        System.out.println("   Pool size: " + ajustes.limitePool());

        return ajustes;
    }

    /**
     * Carga configuracion para un tipo especifico de base de datos
     */
    public static Ajustes cargarParaTipo(String tipo) {
        if ("mysql".equalsIgnoreCase(tipo)) {
            return cargarMySQL();
        } else if ("postgresql".equalsIgnoreCase(tipo)) {
            return cargarPostgreSQL();
        } else {
            return cargarYValidar();
        }
    }

    /**
     * Actualiza un campo especifico en la configuracion
     */
    public static Ajustes actualizarCampo(Ajustes original, String campo, String valor) {
        try {
            JsonNode node = mapper.valueToTree(original);
            ObjectMapper patchMapper = new ObjectMapper();

            // Crear un patch JSON para actualizar el campo
            String patchJson = String.format(
                    "[{\"op\":\"replace\",\"path\":\"/%s\",\"value\":%s}]",
                    campo,
                    mapper.writeValueAsString(valor)
            );

            JsonNode patch = patchMapper.readTree(patchJson);
            JsonNode resultado = mapper.readerForUpdating(original)
                    .readValue(patch);

            return mapper.treeToValue(resultado, Ajustes.class);

        } catch (Exception e) {
            System.err.println("Error actualizando campo '" + campo + "': " + e.getMessage());
            return original;
        }
    }

    /**
     * Compara dos configuraciones
     */
    public static String compararConfiguraciones(Ajustes a1, Ajustes a2) {
        if (a1 == null || a2 == null) {
            return "No se pueden comparar configuraciones nulas";
        }

        StringBuilder diff = new StringBuilder("Diferencias encontradas:\n");

        if (!a1.url().equals(a2.url()))
            diff.append("  - URL: ").append(a1.url()).append(" vs ").append(a2.url()).append("\n");

        if (!a1.usuario().equals(a2.usuario()))
            diff.append("  - Usuario: ").append(a1.usuario()).append(" vs ").append(a2.usuario()).append("\n");

        if (!a1.query().equals(a2.query()))
            diff.append("  - Query: difiere\n");

        if (a1.muestras() != a2.muestras())
            diff.append("  - Muestras: ").append(a1.muestras()).append(" vs ").append(a2.muestras()).append("\n");

        if (a1.reintentos() != a2.reintentos())
            diff.append("  - Reintentos: ").append(a1.reintentos()).append(" vs ").append(a2.reintentos()).append("\n");

        if (a1.limitePool() != a2.limitePool())
            diff.append("  - Pool size: ").append(a1.limitePool()).append(" vs ").append(a2.limitePool()).append("\n");

        if (diff.length() == 0) {
            return "Las configuraciones son identicas";
        }

        return diff.toString();
    }

    /**
     * Crea un archivo de configuracion de ejemplo
     */
    public static void crearConfiguracionEjemplo() {
        Ajustes postgres = new Ajustes(
                "jdbc:postgresql://localhost:5432/mi_basedatos",
                "postgres",
                "password",
                "SELECT * FROM usuarios LIMIT 100",
                5000,
                3,
                100,
                20
        );

        Ajustes mysql = new Ajustes(
                "jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                "root",
                "123456",
                "SELECT * FROM usuarios LIMIT 100",
                5000,
                3,
                100,
                20
        );

        guardarConfiguracion(postgres, "config-postgres.json");
        guardarConfiguracion(mysql, "config-mysql.json");
        guardarComoProperties(postgres, "database.properties");

        System.out.println("Archivos de ejemplo creados:");
        System.out.println("   - config-postgres.json");
        System.out.println("   - config-mysql.json");
        System.out.println("   - database.properties");
    }
}