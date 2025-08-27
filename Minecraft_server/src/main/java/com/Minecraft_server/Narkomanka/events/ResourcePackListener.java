package com.Minecraft_server.Narkomanka.events;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.Minecraft_server.Narkomanka.Narkomanka;

/**
 * Класс для автоматической установки ресурспака у игроков
 */
public class ResourcePackListener implements Listener {
    
    private final Narkomanka plugin;
    private final File resourcePackFile;
    
    /**
     * Создает новый обработчик ресурспаков
     */
    public ResourcePackListener(Narkomanka plugin, File resourcePackFile) {
        this.plugin = plugin;
        this.resourcePackFile = resourcePackFile;
        
        // Регистрируем обработчик событий
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Обработчик события входа игрока
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Отправляем ресурспак с небольшой задержкой
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            try {
                if (resourcePackFile != null && resourcePackFile.exists()) {
                    // Вычисляем SHA-1 хеш файла для проверки целостности
                    String hash = calculateSHA1(resourcePackFile);
                    
                    // Преобразуем локальный путь в URL формат
                    // На Windows пути требуют специальной обработки
                    String resourcePackUrl = "file:///" + resourcePackFile.getAbsolutePath().replace("\\", "/");
                    
                    plugin.getLogger().info("Отправка ресурспака игроку " + player.getName() + ": " + resourcePackUrl);
                    
                    // Отправляем запрос на установку ресурспака
                    player.setResourcePack(resourcePackUrl, hash, true, 
                            net.kyori.adventure.text.Component.text("Ресурспак сервера - необходим для отображения текстур")
                                .color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
                    
                    plugin.getLogger().info("Запрос на установку ресурспака отправлен игроку: " + player.getName());
                } else {
                    plugin.getLogger().warning("Файл ресурспака не найден или не был создан: " + 
                            (resourcePackFile != null ? resourcePackFile.getAbsolutePath() : "null"));
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Ошибка при отправке ресурспака игроку " + player.getName() + ": " + e.getMessage(), e);
            }
        }, 40L); // Задержка 2 секунды для стабильности
    }
    
    /**
     * Обработчик состояния установки ресурспака
     */
    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        PlayerResourcePackStatusEvent.Status status = event.getStatus();
        
        switch (status) {
            case SUCCESSFULLY_LOADED:
                plugin.getLogger().info("Игрок " + player.getName() + " успешно установил ресурспак");
                player.sendMessage(net.kyori.adventure.text.Component.text("Ресурспак успешно установлен!")
                        .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
                break;
            case DECLINED:
                plugin.getLogger().warning("Игрок " + player.getName() + " отклонил установку ресурспака");
                player.sendMessage(net.kyori.adventure.text.Component.text("Вы отклонили установку ресурспака. Некоторые элементы могут отображаться некорректно.")
                        .color(net.kyori.adventure.text.format.NamedTextColor.RED));
                break;
            case FAILED_DOWNLOAD:
                plugin.getLogger().warning("Не удалось загрузить ресурспак для игрока " + player.getName());
                player.sendMessage(net.kyori.adventure.text.Component.text("Ошибка загрузки ресурспака. Попробуйте перезайти на сервер.")
                        .color(net.kyori.adventure.text.format.NamedTextColor.RED));
                break;
            default:
                break;
        }
    }
    
    /**
     * Вычисляет SHA-1 хеш файла
     */
    private String calculateSHA1(File file) throws Exception {
        try (java.io.InputStream fis = new java.io.FileInputStream(file)) {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
    }
} 