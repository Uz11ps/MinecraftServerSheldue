package com.Minecraft_server.Narkomanka.util;

/**
 * A utility class for setting up SLF4J and Hibernate logging configuration
 */
public class LoggerAdapter {

    public static void initializeLogging() {
        // Set system properties to redirect SLF4J logs to JUL (Java Util Logging)
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.showLogName", "false");
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");

        // Hibernate logging settings
        System.setProperty("hibernate.show_sql", "false");
        System.setProperty("hibernate.format_sql", "false");
        System.setProperty("hibernate.use_sql_comments", "false");

        // Hikari logging
        System.setProperty("com.zaxxer.hikari.pool.HikariPool", "warn");
    }
}