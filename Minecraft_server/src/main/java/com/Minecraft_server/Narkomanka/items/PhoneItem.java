package com.Minecraft_server.Narkomanka.items;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.npc.PhoneBoothNPC;
import com.Minecraft_server.Narkomanka.ui.PhoneUI;
import com.Minecraft_server.Narkomanka.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Класс для мобильного телефона, который игроки могут использовать
 * для доступа к телефонным миссиям и другим функциям
 */
public class PhoneItem implements Listener {

    private final Narkomanka plugin;
    private final PhoneBoothNPC phoneBoothNPC;
    private final NamespacedKey phoneKey;
    private final PhoneUI phoneUI;
    
    // Кулдаун для предотвращения спама
    private final List<UUID> cooldown = new ArrayList<>();

    /**
     * Создает менеджер телефонных предметов
     */
    public PhoneItem(Narkomanka plugin, PhoneBoothNPC phoneBoothNPC) {
        this.plugin = plugin;
        this.phoneBoothNPC = phoneBoothNPC;
        this.phoneKey = new NamespacedKey(plugin, "mobile_phone");
        this.phoneUI = new PhoneUI(plugin, phoneBoothNPC);
        
        // Регистрируем слушатель событий
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("PhoneItem зарегистрирован");
        
        // Запускаем таймер для очистки кулдауна
        plugin.getServer().getScheduler().runTaskTimer(plugin, cooldown::clear, 20L, 20L);
    }
    
    /**
     * Создает предмет телефона
     */
    public ItemStack createPhone(int quality) {
        Material material = Material.COMPASS;
        String name;
        String[] lore;
        
        switch (quality) {
            case 1:
                name = ChatColor.WHITE + "Простой телефон";
                lore = new String[] {
                    ChatColor.GRAY + "Самая базовая модель",
                    ChatColor.GRAY + "Позволяет принимать звонки",
                    "",
                    ChatColor.YELLOW + "ПКМ: " + ChatColor.GRAY + "Использовать телефон"
                };
                break;
            case 2:
                name = ChatColor.GREEN + "Улучшенный телефон";
                lore = new String[] {
                    ChatColor.GRAY + "Надежная модель",
                    ChatColor.GRAY + "Уменьшает риск при миссиях на 5%",
                    "",
                    ChatColor.YELLOW + "ПКМ: " + ChatColor.GRAY + "Использовать телефон"
                };
                break;
            case 3:
                name = ChatColor.BLUE + "Продвинутый телефон";
                lore = new String[] {
                    ChatColor.GRAY + "Шифрованная модель",
                    ChatColor.GRAY + "Уменьшает риск при миссиях на 10%",
                    ChatColor.GRAY + "Получайте больше миссий",
                    "",
                    ChatColor.YELLOW + "ПКМ: " + ChatColor.GRAY + "Использовать телефон"
                };
                break;
            case 4:
                name = ChatColor.GOLD + "Премиальный телефон";
                lore = new String[] {
                    ChatColor.GRAY + "Продвинутая модель с криптографией",
                    ChatColor.GRAY + "Уменьшает риск при миссиях на 15%",
                    ChatColor.GRAY + "Доступ к эксклюзивным миссиям",
                    ChatColor.GRAY + "Повышенные награды",
                    "",
                    ChatColor.YELLOW + "ПКМ: " + ChatColor.GRAY + "Использовать телефон"
                };
                break;
            default:
                name = ChatColor.WHITE + "Телефон";
                lore = new String[] {
                    ChatColor.GRAY + "Обычный телефон",
                    "",
                    ChatColor.YELLOW + "ПКМ: " + ChatColor.GRAY + "Использовать телефон"
                };
        }
        
        return new ItemBuilder(material)
                .setName(name)
                .setLore(Arrays.asList(lore))
                .setPersistentData(phoneKey, PersistentDataType.INTEGER, quality)
                .build();
    }
    
    /**
     * Проверяет, является ли предмет телефоном
     */
    public boolean isPhone(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        return item.getItemMeta().getPersistentDataContainer().has(phoneKey, PersistentDataType.INTEGER);
    }
    
    /**
     * Получает качество телефона
     */
    public int getPhoneQuality(ItemStack item) {
        if (!isPhone(item)) {
            return 0;
        }
        
        return item.getItemMeta().getPersistentDataContainer().get(phoneKey, PersistentDataType.INTEGER);
    }
    
    /**
     * Обрабатывает использование телефона игроком
     */
    @EventHandler
    public void onPhoneUse(PlayerInteractEvent event) {
        // Проверяем, что это правая кнопка мыши
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        // Проверяем, что это основная рука
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // Проверяем, что это телефон
        if (!isPhone(item)) {
            return;
        }
        
        // Проверяем кулдаун
        if (cooldown.contains(player.getUniqueId())) {
            return;
        }
        
        // Добавляем игрока в кулдаун
        cooldown.add(player.getUniqueId());
        
        // Отменяем событие
        event.setCancelled(true);
        
        // Получаем качество телефона
        int quality = getPhoneQuality(item);
        
        // Воспроизводим звук телефона
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
        
        // Открываем меню телефона
        phoneUI.openPhone(player, quality);
    }
} 