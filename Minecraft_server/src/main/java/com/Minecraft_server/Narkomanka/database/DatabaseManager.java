package com.Minecraft_server.Narkomanka.database;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

public class DatabaseManager {
    private final Narkomanka plugin;
    private SessionFactory sessionFactory;
    private HikariDataSource dataSource;
    private boolean usingPostgreSQL = false;
    private boolean usingH2 = false;

    public DatabaseManager(Narkomanka plugin) {
        this.plugin = plugin;
        initializeConnection();
    }

    private void initializeConnection() {
        String dbType = plugin.getConfig().getString("database.type", "auto").toLowerCase();

        if (dbType.equals("postgresql") || dbType.equals("auto")) {
            // Try PostgreSQL first
            if (connectToPostgreSQL()) {
                usingPostgreSQL = true;
                plugin.getLogger().info("Successfully connected to PostgreSQL database!");
                return;
            }

            if (dbType.equals("postgresql")) {
                // If PostgreSQL was explicitly requested but failed, log error
                plugin.getLogger().severe("Failed to connect to PostgreSQL database. Check your configuration and make sure PostgreSQL is running.");
                plugin.getLogger().severe("Trying H2 as a fallback...");
            } else {
                plugin.getLogger().warning("PostgreSQL connection failed, falling back to H2 embedded database.");
            }
        }

        // Try H2 embedded database as fallback
        if (connectToH2()) {
            usingH2 = true;
            plugin.getLogger().info("Successfully connected to H2 embedded database!");
        } else {
            plugin.getLogger().severe("Failed to connect to any database. Using in-memory operation mode.");
        }
    }

    private boolean connectToPostgreSQL() {
        try {
            // Get database settings from config
            String host = plugin.getConfig().getString("database.host", "localhost");
            int port = plugin.getConfig().getInt("database.port", 5433); // Default to 5433 as seen in your config
            String database = plugin.getConfig().getString("database.name", "Minecraft_server");
            String username = plugin.getConfig().getString("database.username", "postgres");
            String password = plugin.getConfig().getString("database.password", "123");

            plugin.getLogger().info("Connecting to PostgreSQL database: " +
                    String.format("jdbc:postgresql://%s:%d/%s", host, port, database));

            // Set up HikariCP data source
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setDriverClassName("org.postgresql.Driver");

            // Connection pool settings
            hikariConfig.setMaximumPoolSize(5);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setIdleTimeout(30000); // 30 seconds
            hikariConfig.setMaxLifetime(60000); // 1 minute
            hikariConfig.setConnectionTimeout(10000); // 10 seconds
            hikariConfig.setInitializationFailTimeout(10000); // 10 seconds

            // PostgreSQL specific settings
            hikariConfig.addDataSourceProperty("ApplicationName", "Narkomanka");
            hikariConfig.addDataSourceProperty("reWriteBatchedInserts", "true");

            dataSource = new HikariDataSource(hikariConfig);

            // Test connection
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(5)) {
                    // Set up Hibernate for PostgreSQL
                    setupHibernate("org.hibernate.dialect.PostgreSQLDialect");
                    return true;
                }
            }

            // Close dataSource if connection test failed
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                dataSource = null;
            }

            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to connect to PostgreSQL database: " + e.getMessage(), e);

            // Close dataSource if it was created but connection failed
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                dataSource = null;
            }

            return false;
        }
    }

    private boolean connectToH2() {
        try {
            // Set up H2 embedded database
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File dbFile = new File(dataFolder, "database");
            String jdbcUrl = "jdbc:h2:" + dbFile.getAbsolutePath();

            plugin.getLogger().info("Connecting to H2 embedded database: " + jdbcUrl);

            // Set up HikariCP data source
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setDriverClassName("org.h2.Driver");

            // Connection pool settings - simplified
            hikariConfig.setMaximumPoolSize(5);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setIdleTimeout(30000); // 30 seconds
            hikariConfig.setConnectionTimeout(10000); // 10 seconds

            // Only essential H2 settings
            hikariConfig.addDataSourceProperty("DB_CLOSE_ON_EXIT", "FALSE");

            dataSource = new HikariDataSource(hikariConfig);

            // Test connection
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(5)) {
                    // Set up Hibernate for H2
                    setupHibernate("org.hibernate.dialect.H2Dialect");
                    return true;
                }
            }

            // Close dataSource if connection test failed
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                dataSource = null;
            }

            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to H2 database: " + e.getMessage(), e);

            // Close dataSource if it was created but connection failed
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                dataSource = null;
            }

            return false;
        }
    }

    private void setupHibernate(String dialect) {
        try {
            plugin.getLogger().info("Setting up Hibernate SessionFactory with dialect: " + dialect);

            // Set Hibernate properties
            Properties hibernateProps = new Properties();
            hibernateProps.put("hibernate.connection.datasource", dataSource);
            hibernateProps.put("hibernate.dialect", dialect);
            hibernateProps.put("hibernate.hbm2ddl.auto", "update");
            hibernateProps.put("hibernate.show_sql", "false");
            hibernateProps.put("hibernate.format_sql", "false");
            hibernateProps.put("hibernate.use_sql_comments", "false");
            hibernateProps.put("hibernate.current_session_context_class", "thread");

            // Create configuration
            Configuration configuration = new Configuration();
            configuration.setProperties(hibernateProps);

            // Add entity classes
            configuration.addAnnotatedClass(PlayerData.class);
            configuration.addAnnotatedClass(QuestData.class);
            configuration.addAnnotatedClass(PlayerQuestProgress.class);
            configuration.addAnnotatedClass(QuestObjective.class);

            // Build session factory
            sessionFactory = configuration.buildSessionFactory();

            plugin.getLogger().info("Hibernate SessionFactory created successfully!");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to set up Hibernate: " + e.getMessage(), e);
        }
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public boolean isAvailable() {
        // Modified to allow plugin to work even if database is unavailable
        return true;
    }

    public boolean isUsingPostgreSQL() {
        return usingPostgreSQL;
    }
    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public boolean isUsingH2() {
        return usingH2;
    }

    public void shutdown() {
        plugin.getLogger().info("Shutting down database connections...");

        if (sessionFactory != null && !sessionFactory.isClosed()) {
            try {
                sessionFactory.close();
                plugin.getLogger().info("Hibernate SessionFactory closed successfully");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error closing Hibernate SessionFactory: " + e.getMessage());
            }
        }

        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
                plugin.getLogger().info("HikariCP DataSource closed successfully");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error closing HikariCP DataSource: " + e.getMessage());
            }
        }
    }
}