package com.Minecraft_server.Narkomanka.trash;

import com.Minecraft_server.Narkomanka.Narkomanka;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для управления инструментами сбора мусора
 */
public class TrashCollector {
    private final Narkomanka plugin;
    private final NamespacedKey collectorKey;
    private final Material collectorMaterial;
    private final String collectorName;

    public TrashCollector(Narkomanka plugin) {
        this.plugin = plugin;
        this.collectorKey = new NamespacedKey(plugin, "trash_collector");

        // Загружаем настройки из конфига
        collectorMaterial = Material.valueOf(
                plugin.getConfig().getString("trash.collector.material", "FISHING_ROD")
        );

        collectorName = plugin.getConfig().getString("trash.collector.name", "Мусоросборник");
    }

    /**
     * Создаёт новый инструмент для сбора мусора
     */
    public ItemStack createCollector() {
        ItemStack collector = new ItemStack(collectorMaterial);
        ItemMeta meta = collector.getItemMeta();

        // Устанавливаем название и описание
        meta.displayName(net.kyori.adventure.text.Component.text(collectorName)
                .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text("Используйте для сбора мусора")
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        lore.add(net.kyori.adventure.text.Component.text("ПКМ для подбора мусора")
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        meta.lore(lore);

        // Помечаем предмет как сборщик мусора
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(collectorKey, PersistentDataType.BYTE, (byte) 1);

        // Устанавливаем метаданные и возвращаем предмет
        collector.setItemMeta(meta);
        return collector;
    }

    /**
     * Проверяет, является ли предмет инструментом для сбора мусора
     */
    public boolean isCollector(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.has(collectorKey, PersistentDataType.BYTE);
    }

    /**
     * Выдаёт инструмент для сбора мусора игроку
     */
    public void giveCollectorToPlayer(Player player) {
        ItemStack collector = createCollector();

        // Проверяем, есть ли у игрока свободное место в инвентаре
        if (player.getInventory().firstEmpty() == -1) {
            // Если инвентарь полон, выбрасываем предмет перед игроком
            player.getWorld().dropItem(player.getLocation(), collector);
            player.sendMessage("§cВаш инвентарь полон! Мусоросборник выброшен на землю.");
        } else {
            // Иначе добавляем предмет в инвентарь
            player.getInventory().addItem(collector);
            player.sendMessage("§aВы получили мусоросборник!");
        }
    }

    /**
     * Получает материал, используемый для инструмента сбора мусора
     */
    public Material getCollectorMaterial() {
        return collectorMaterial;
    }

    /**
     * Получает название инструмента сбора мусора
     */
    public String getCollectorName() {
        return collectorName;
    }

    /**
     * Получает ключ метаданных для инструмента сбора мусора
     */
    public NamespacedKey getCollectorKey() {
        return collectorKey;
    }
}