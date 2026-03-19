package com.MariaBermudez.motores;

import com.MariaBermudez.modelos.Ajustes;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class MotorPool implements EstrategiaConexion {
    // Cambiado a mapa de instancias para soportar multiples pools (por URL)
    private static final ConcurrentHashMap<String, MotorPool> INSTANCIAS = new ConcurrentHashMap<>();

    private final BlockingQueue<ConnectionHolder> pool;
    private final List<ConnectionHolder> holders = new ArrayList<>();
    private final Ajustes ajustes;
    private final int maxPoolSize;
    private volatile boolean cerrado = false;

    // Métricas adicionales para monitoreo
    private final AtomicLong totalConexionesCreadas = new AtomicLong(0);
    private final AtomicLong totalConexionesCerradas = new AtomicLong(0);
    private final AtomicLong tiempoEsperaTotal = new AtomicLong(0);
    private final AtomicLong peticionesTotales = new AtomicLong(0);

    private static class ConnectionHolder {
        volatile Connection real;
        final AtomicBoolean inUse = new AtomicBoolean(false);
        final long tiempoCreacion;
        volatile long tiempoAdquisicion;
        volatile long tiempoLiberacion;

        ConnectionHolder(Connection real) {
            this.real = real;
            this.tiempoCreacion = System.currentTimeMillis();
        }
    }

    // Constructor privado
    private MotorPool(Ajustes ajustes) {
        this.ajustes = Objects.requireNonNull(ajustes);
        this.maxPoolSize = Math.max(1, ajustes.limitePool());
        this.pool = new ArrayBlockingQueue<>(this.maxPoolSize);

        // Intentar prellenar el pool con holders; no fallar completamente si la BD no está disponible.
        // Para evitar generar muchos errores si la BD no existe, prellenamos solo 1 conexión de prueba y hacemos creación bajo demanda.
        int prefill = Math.min(1, this.maxPoolSize);
        int exitos = 0;
        for (int i = 0; i < prefill; i++) {
            try {
                Connection real = crearConexionReal();
                ConnectionHolder holder = new ConnectionHolder(real);
                holders.add(holder);
                pool.offer(holder);
                totalConexionesCreadas.incrementAndGet();
                exitos++;
            } catch (SQLException e) {
                // Registrar el error y continuar intentando crear las demás conexiones.
                System.err.println("ERROR: no se pudo crear conexion del pool (intento " + (i+1) + "): " + e.getMessage());
                // No lanzar RuntimeException aquí para permitir que la aplicación siga en ejecución.
            }
        }

        if (exitos == 0) {
            System.err.println("ADVERTENCIA: No se pudieron crear conexiones del pool al inicializar (0/" + this.maxPoolSize + ").\n" +
                    "  - Revise que la base de datos exista y las credenciales sean correctas.\n" +
                    "  - Mensaje original de la URL: " + ajustes.url());
        } else {
            System.out.println("MotorPool inicializado: " + exitos + " conexiones creadas de " + this.maxPoolSize);
        }
    }

    /**
     * Obtiene/crea una instancia de MotorPool para la URL del ajuste (pool por URL)
     */
    public static MotorPool getInstance(Ajustes ajustes) {
        String key = ajustes.url();
        return INSTANCIAS.computeIfAbsent(key, k -> new MotorPool(ajustes));
    }

    private Connection crearConexionReal() throws SQLException {
        try {
            // Intentar cargar el driver automáticamente según la URL
            String url = ajustes.url();
            if (url.startsWith("jdbc:postgresql:")) {
                try {
                    Class.forName("org.postgresql.Driver");
                } catch (ClassNotFoundException e) {
                    System.err.println("Driver PostgreSQL no encontrado, usando DriverManager");
                }
            } else if (url.startsWith("jdbc:mysql:") || url.startsWith("jdbc:mariadb:")) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException e) {
                    try {
                        Class.forName("com.mysql.jdbc.Driver");
                    } catch (ClassNotFoundException ex) {
                        System.err.println("Driver MySQL no encontrado, usando DriverManager");
                    }
                }
            }
        } catch (Exception ignored) {
            // El driver puede ser cargado automáticamente por DriverManager
        }

        Connection conn = DriverManager.getConnection(
                ajustes.url(),
                ajustes.usuario(),
                ajustes.clave()
        );

        // Configuración inicial de la conexión
        conn.setAutoCommit(true);

        return conn;
    }

    private Connection crearProxyForHolder(final ConnectionHolder holder) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();

                if ("close".equals(name)) {
                    // Intentar devolver el holder al pool solo si estaba en uso
                    if (holder.inUse.compareAndSet(true, false)) {
                        holder.tiempoLiberacion = System.currentTimeMillis();

                        if (!cerrado) {
                            // Resetear la conexión antes de devolverla al pool
                            try {
                                if (holder.real != null && !holder.real.isClosed()) {
                                    if (!holder.real.getAutoCommit()) {
                                        holder.real.setAutoCommit(true);
                                    }
                                    holder.real.clearWarnings();
                                }
                            } catch (SQLException e) {
                                // Si falla el reset, la conexión está corrupta, crear una nueva
                                try {
                                    Connection nueva = crearConexionReal();
                                    Connection vieja = holder.real;
                                    holder.real = nueva;
                                    if (vieja != null && !vieja.isClosed()) {
                                        vieja.close();
                                        totalConexionesCerradas.incrementAndGet();
                                    }
                                    totalConexionesCreadas.incrementAndGet();
                                } catch (SQLException ex) {
                                    // Ignorar, intentaremos devolver la vieja igual
                                }
                            }

                            // intentar devolver al pool, si falla cerrar la conexión real
                            boolean offered = pool.offer(holder);
                            if (!offered) {
                                try {
                                    if (holder.real != null && !holder.real.isClosed()) {
                                        holder.real.close();
                                        totalConexionesCerradas.incrementAndGet();
                                    }
                                } catch (Exception ignored) {}
                            }
                        } else {
                            try {
                                if (holder.real != null && !holder.real.isClosed()) {
                                    holder.real.close();
                                    totalConexionesCerradas.incrementAndGet();
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    return null;
                }

                if ("isValid".equals(name) && args != null && args.length > 0) {
                    // Delegar el método isValid a la conexión real
                    try {
                        Connection r = holder.real;
                        if (r == null || r.isClosed()) {
                            return false;
                        }
                        return method.invoke(r, args);
                    } catch (Exception e) {
                        return false;
                    }
                }

                // Delegate el resto de métodos al real actual
                try {
                    Connection r = holder.real;
                    if (r == null) {
                        throw new SQLException("Conexión no disponible");
                    }
                    return method.invoke(r, args);
                } catch (Throwable t) {
                    throw t.getCause() == null ? t : t.getCause();
                }
            }
        };

        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                handler
        );
    }

    @Override
    public Connection obtenerConexion() throws SQLException {
        if (cerrado) {
            throw new SQLException("Pool de conexiones cerrado");
        }

        long tiempoInicioEspera = System.currentTimeMillis();
        peticionesTotales.incrementAndGet();

        try {
            ConnectionHolder holder = pool.poll(10, TimeUnit.SECONDS);

            long tiempoEspera = System.currentTimeMillis() - tiempoInicioEspera;
            tiempoEsperaTotal.addAndGet(tiempoEspera);

            if (holder == null) {
                // Intentar crear una nueva conexión bajo demanda si el pool está vacío
                synchronized (this) {
                    if (holders.size() < maxPoolSize && !cerrado) {
                        try {
                            Connection real = crearConexionReal();
                            ConnectionHolder h = new ConnectionHolder(real);
                            holders.add(h);
                            totalConexionesCreadas.incrementAndGet();
                            // devolver proxy directo sin meter al pool (simula adquisición)
                            h.inUse.set(true);
                            h.tiempoAdquisicion = System.currentTimeMillis();
                            return crearProxyForHolder(h);
                        } catch (SQLException e) {
                            throw new SQLException("Timeout y no se pudo crear conexion bajo demanda: " + e.getMessage(), e);
                        }
                    }
                }
                throw new SQLException("Timeout: no hay conexiones disponibles en el pool (espera: " + tiempoEspera + "ms)");
            }

            // Marcar como en uso y registrar tiempo
            holder.inUse.set(true);
            holder.tiempoAdquisicion = System.currentTimeMillis();

            // Validar la conexión real y reemplazar si está cerrada o inválida
            try {
                Connection real = holder.real;
                if (real == null || real.isClosed() || !real.isValid(2)) {
                    synchronized (holder) {
                        // verificar otra vez dentro del lock
                        Connection current = holder.real;
                        if (current == null || current.isClosed() || !current.isValid(2)) {
                            // Cerrar la conexión vieja si existe
                            try {
                                if (current != null && !current.isClosed()) {
                                    current.close();
                                    totalConexionesCerradas.incrementAndGet();
                                }
                            } catch (Exception ignored) {}

                            // Crear nueva conexión
                            Connection nueva = crearConexionReal();
                            holder.real = nueva;
                            totalConexionesCreadas.incrementAndGet();
                        }
                    }
                }
            } catch (SQLException sqle) {
                // Si la validación falla, intentar crear una nueva conexión
                synchronized (holder) {
                    try {
                        if (holder.real != null && !holder.real.isClosed()) {
                            holder.real.close();
                            totalConexionesCerradas.incrementAndGet();
                        }
                    } catch (Exception ignored) {}

                    holder.real = crearConexionReal();
                    totalConexionesCreadas.incrementAndGet();
                }
            }

            // Crear y devolver un proxy que delega al holder
            return crearProxyForHolder(holder);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrumpido al obtener conexión del pool", e);
        }
    }

    @Override
    public void cerrar() {
        cerrado = true;

        // Vaciar pool y cerrar las conexiones reales
        for (ConnectionHolder h : holders) {
            try {
                if (h.real != null && !h.real.isClosed()) {
                    h.real.close();
                    totalConexionesCerradas.incrementAndGet();
                }
            } catch (Exception ignored) {}
        }

        pool.clear();
        holders.clear();

        // Eliminar esta instancia del mapa de instancias
        INSTANCIAS.remove(ajustes.url());
    }

    // Métodos para monitoreo del pool

    /**
     * Obtiene el número de conexiones activas (en uso)
     */
    public int getActiveConnections() {
        int active = 0;
        for (ConnectionHolder holder : holders) {
            if (holder.inUse.get()) {
                active++;
            }
        }
        return active;
    }

    /**
     * Obtiene el número de conexiones inactivas (disponibles en el pool)
     */
    public int getIdleConnections() {
        return pool.size();
    }

    /**
     * Obtiene el tamaño total del pool
     */
    public int getTotalConnections() {
        return holders.size();
    }

    /**
     * Obtiene el número total de conexiones creadas desde el inicio
     */
    public long getTotalConexionesCreadas() {
        return totalConexionesCreadas.get();
    }

    /**
     * Obtiene el número total de conexiones cerradas
     */
    public long getTotalConexionesCerradas() {
        return totalConexionesCerradas.get();
    }

    /**
     * Obtiene el tiempo promedio de espera para obtener una conexión
     */
    public double getTiempoPromedioEspera() {
        long peticiones = peticionesTotales.get();
        if (peticiones == 0) return 0;
        return (double) tiempoEsperaTotal.get() / peticiones;
    }

    /**
     * Verifica si el pool está cerrado
     */
    public boolean isCerrado() {
        return cerrado;
    }

    /**
     * Obtiene estadísticas completas del pool
     */
    public String getEstadisticas() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ESTADÍSTICAS DEL POOL ===\n");
        sb.append(String.format("Conexiones totales: %d\n", getTotalConnections()));
        sb.append(String.format("Conexiones activas: %d\n", getActiveConnections()));
        sb.append(String.format("Conexiones inactivas: %d\n", getIdleConnections()));
        sb.append(String.format("Conexiones creadas totales: %d\n", totalConexionesCreadas.get()));
        sb.append(String.format("Conexiones cerradas totales: %d\n", totalConexionesCerradas.get()));
        sb.append(String.format("Peticiones totales: %d\n", peticionesTotales.get()));
        sb.append(String.format("Tiempo promedio espera: %.2f ms\n", getTiempoPromedioEspera()));
        sb.append(String.format("Pool cerrado: %s\n", cerrado ? "Sí" : "No"));
        return sb.toString();
    }

    /**
     * Refresca todas las conexiones inactivas del pool
     */
    public void refrescarConexionesInactivas() {
        for (ConnectionHolder holder : holders) {
            if (!holder.inUse.get()) {
                synchronized (holder) {
                    try {
                        if (holder.real != null && !holder.real.isClosed()) {
                            holder.real.close();
                            totalConexionesCerradas.incrementAndGet();
                        }
                        holder.real = crearConexionReal();
                        totalConexionesCreadas.incrementAndGet();
                    } catch (SQLException e) {
                        System.err.println("Error refrescando conexión inactiva: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Ajusta el tamaño del pool (solo funciona si no hay conexiones activas)
     */
    public boolean redimensionar(int nuevoTamanio) {
        if (nuevoTamanio < 1 || getActiveConnections() > 0) {
            return false;
        }

        // No podemos redimensionar fácilmente un BlockingQueue, así que
        // esta implementación es simplificada
        return false;
    }
}