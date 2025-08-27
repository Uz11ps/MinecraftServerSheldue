package com.Minecraft_server.Narkomanka.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Утилитарный класс для облегчения создания предметов с метаданными
 */
public class ItemBuilder {
    private final ItemStack itemStack;
    private final ItemMeta itemMeta;
    private final List<String> lore = new ArrayList<>();

    /**
     * Создает новый ItemBuilder с указанным материалом
     */
    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }

    /**
     * Создает новый ItemBuilder на основе существующего ItemStack
     */
    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.itemMeta = this.itemStack.getItemMeta();
        
        if (itemMeta.hasLore()) {
            lore.addAll(itemMeta.getLore());
        }
    }

    /**
     * Устанавливает имя предмета
     */
    public ItemBuilder setName(String name) {
        itemMeta.setDisplayName(name);
        return this;
    }

    /**
     * Устанавливает описание предмета из списка строк
     */
    public ItemBuilder setLore(List<String> lore) {
        this.lore.clear();
        this.lore.addAll(lore);
        return this;
    }

    /**
     * Устанавливает описание предмета из массива строк
     */
    public ItemBuilder setLore(String... lore) {
        return setLore(Arrays.asList(lore));
    }

    /**
     * Добавляет строку к описанию предмета
     */
    public ItemBuilder addLoreLine(String line) {
        lore.add(line);
        return this;
    }

    /**
     * Устанавливает модель предмета (CustomModelData)
     */
    public ItemBuilder setCustomModelData(int modelData) {
        itemMeta.setCustomModelData(modelData);
        return this;
    }

    /**
     * Устанавливает значение в PersistentDataContainer для String
     */
    public ItemBuilder setPersistentData(NamespacedKey key, PersistentDataType<String, String> type, String value) {
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        container.set(key, type, value);
        return this;
    }

    /**
     * Устанавливает значение в PersistentDataContainer для Integer
     */
    public ItemBuilder setPersistentData(NamespacedKey key, PersistentDataType<Integer, Integer> type, int value) {
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        container.set(key, type, value);
        return this;
    }

    /**
     * Устанавливает значение в PersistentDataContainer для Byte
     */
    public ItemBuilder setPersistentData(NamespacedKey key, PersistentDataType<Byte, Byte> type, byte value) {
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        container.set(key, type, value);
        return this;
    }

    /**
     * Устанавливает значение в PersistentDataContainer для Double
     */
    public ItemBuilder setPersistentData(NamespacedKey key, PersistentDataType<Double, Double> type, double value) {
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        container.set(key, type, value);
        return this;
    }

    /**
     * Устанавливает предмет невозможным для уничтожения
     */
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        itemMeta.setUnbreakable(unbreakable);
        return this;
    }

    /**
     * Создает предмет с установленными параметрами
     */
    public ItemStack build() {
        if (!lore.isEmpty()) {
            itemMeta.setLore(lore);
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
} 