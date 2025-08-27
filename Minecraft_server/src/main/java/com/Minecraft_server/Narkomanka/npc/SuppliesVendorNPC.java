package com.Minecraft_server.Narkomanka.npc;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.database.PlayerData;
import com.Minecraft_server.Narkomanka.ui.SuppliesVendorMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SuppliesVendorNPC implements Listener {

    private final Narkomanka plugin;
    private final Map<String, SupplyItem> availableSupplies = new HashMap<>();
    private final NamespacedKey npcTypeKey;
    private boolean isDaytime = true;

    // Меню поставщика
    private SuppliesVendorMenu suppliesVendorMenu;

    public SuppliesVendorNPC(Narkomanka plugin) {
        this.plugin = plugin;
        this.npcTypeKey = new NamespacedKey(plugin, "npc_type");

        // Initialize supply items with different categories and prices

        // Equipment and tools
        availableSupplies.put("growbox", new SupplyItem("Гроубокс", 500.0, "Для выращивания растений", "BARREL", "equipment"));
        
        // Почва разных типов
        availableSupplies.put("soil_basic", new SupplyItem("Обычная почва", 30.0, "Базовая почва для растений", "DIRT", "soil"));
        availableSupplies.put("soil_enriched", new SupplyItem("Обогащенная почва", 150.0, "Улучшает скорость роста в 1.5 раза", "PODZOL", "soil"));
        availableSupplies.put("soil_premium", new SupplyItem("Премиум почва", 300.0, "Улучшает скорость роста в 2 раза и качество +1", "ROOTED_DIRT", "soil"));
        availableSupplies.put("soil_hydroponic", new SupplyItem("Гидропонная система", 800.0, "Улучшает скорость роста в 2.5 раза и качество +2", "CYAN_CONCRETE", "soil"));
        
        // Вода разных типов
        availableSupplies.put("water_regular", new SupplyItem("Обычная вода", 20.0, "Базовая вода для растений", "WATER_BUCKET", "water"));
        availableSupplies.put("water_filtered", new SupplyItem("Фильтрованная вода", 60.0, "Улучшает скорость роста в 1.2 раза", "LIGHT_BLUE_DYE", "water"));
        availableSupplies.put("water_mineral", new SupplyItem("Минеральная вода", 150.0, "Улучшает скорость роста в 1.4 раза и качество +1", "BLUE_DYE", "water"));
        availableSupplies.put("water_nutrient", new SupplyItem("Питательный раствор", 350.0, "Улучшает скорость роста в 1.8 раза и качество +2", "DIAMOND", "water"));
        
        // Удобрения разных типов
        availableSupplies.put("fertilizer_basic", new SupplyItem("Базовое удобрение", 50.0, "Улучшает скорость роста в 1.3 раза", "BONE_MEAL", "fertilizer"));
        availableSupplies.put("fertilizer_growth", new SupplyItem("Стимулятор роста", 180.0, "Улучшает скорость роста в 1.6 раза и качество +1", "GLOWSTONE_DUST", "fertilizer"));
        availableSupplies.put("fertilizer_quality", new SupplyItem("Улучшитель качества", 220.0, "Улучшает скорость роста в 1.3 раза и качество +2", "REDSTONE", "fertilizer"));
        availableSupplies.put("fertilizer_premium", new SupplyItem("Премиум удобрение", 450.0, "Улучшает скорость роста в 1.5 раза и качество +2", "BLAZE_POWDER", "fertilizer"));
        
        // Семена
        availableSupplies.put("seeds_marijuana", new SupplyItem("Семена марихуаны", 40.0, "Для выращивания марихуаны", "WHEAT_SEEDS", "seeds"));
        availableSupplies.put("seeds_coca", new SupplyItem("Семена коки", 80.0, "Для выращивания коки", "BEETROOT_SEEDS", "seeds"));
        availableSupplies.put("seeds_poppy", new SupplyItem("Семена мака", 120.0, "Для выращивания мака", "POPPY", "seeds"));
        
        // Расходные материалы
        availableSupplies.put("baggies", new SupplyItem("Пакетики", 15.0, "Для упаковки товара", "MAP", "supplies"));
        availableSupplies.put("scales", new SupplyItem("Весы", 60.0, "Для точного измерения доз", "COMPARATOR", "equipment"));
        
        // Телефоны
        availableSupplies.put("phone1", new SupplyItem("Простой телефон", 150.0, "Базовая модель для связи", "COMPASS", "phone"));
        availableSupplies.put("phone2", new SupplyItem("Улучшенный телефон", 450.0, "Снижает риск при миссиях на 5%", "COMPASS", "phone"));
        availableSupplies.put("phone3", new SupplyItem("Продвинутый телефон", 1000.0, "Снижает риск при миссиях на 10%", "COMPASS", "phone"));
        availableSupplies.put("phone4", new SupplyItem("Премиальный телефон", 2500.0, "Снижает риск при миссиях на 15%", "COMPASS", "phone"));

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Создаем меню
        this.suppliesVendorMenu = new SuppliesVendorMenu(plugin, this);
    }

    /**
     * Spawn supplies vendor NPC at a specific location
     */
    public void spawnSuppliesVendor(Location location) {
        Villager vendor = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);

        // Set custom name and make it visible
        vendor.customName(Component.text("Поставщик").color(NamedTextColor.GREEN));
        vendor.setCustomNameVisible(true);

        // Set profession and prevent entity AI if desired
        vendor.setProfession(Villager.Profession.FARMER);
        vendor.setAI(false);
        vendor.setInvulnerable(true);
        vendor.setRemoveWhenFarAway(false);

        // Set custom tag to identify this as a supplies vendor
        vendor.getPersistentDataContainer().set(npcTypeKey, PersistentDataType.STRING, "supplies_vendor");

        plugin.getLogger().info("Supplies vendor NPC spawned at " + location.getWorld().getName() +
                " " + location.getX() + " " + location.getY() + " " + location.getZ());
    }

    /**
     * Handle interaction with the supplies vendor
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // Check if interacted entity is our NPC
        if (!(event.getRightClicked() instanceof Villager)) return;

        Villager villager = (Villager) event.getRightClicked();
        if (!villager.getPersistentDataContainer().has(npcTypeKey, PersistentDataType.STRING)) return;

        String npcType = villager.getPersistentDataContainer().get(npcTypeKey, PersistentDataType.STRING);
        if (!"supplies_vendor".equals(npcType)) return;

        // Prevent further interaction
        event.setCancelled(true);

        // Check if it's night time
        if (!isDaytime) {
            event.getPlayer().sendMessage(Component.text("Поставщик не работает ночью. Приходи днём!").color(NamedTextColor.RED));
            return;
        }

        // Open supplies vendor UI
        suppliesVendorMenu.openMenu(event.getPlayer());
    }

    /**
     * Process supply purchase
     */
    public boolean purchaseSupply(Player player, String supplyType, int quantity) {
        if (!isDaytime) {
            player.sendMessage(Component.text("Поставщик не работает ночью. Приходи днём!").color(NamedTextColor.RED));
            return false;
        }

        // Validate supply type
        if (!availableSupplies.containsKey(supplyType)) {
            player.sendMessage(Component.text("Такого товара нет в продаже.").color(NamedTextColor.RED));
            return false;
        }

        // Get item info
        SupplyItem supply = availableSupplies.get(supplyType);
        double totalCost = supply.price * quantity;

        // Check if player has enough money
        PlayerData playerData = plugin.getPlayerService().getOrCreatePlayerData(player);
        if (playerData.getCashBalance() < totalCost) {
            player.sendMessage(Component.text("У вас недостаточно наличных! Нужно: $" + totalCost).color(NamedTextColor.RED));
            return false;
        }

        // Remove money and give supply item
        boolean success = plugin.getPlayerService().removeCash(player.getUniqueId(), totalCost);
        if (!success) {
            player.sendMessage(Component.text("Ошибка при оплате.").color(NamedTextColor.RED));
            return false;
        }

        // Give item to player
        ItemStack supplyItem;
        
        // Если это телефон, создаем его через PhoneItem
        if (supplyType.startsWith("phone") && "phone".equals(supply.category)) {
            try {
                int phoneQuality = Integer.parseInt(supplyType.substring(5));
                supplyItem = plugin.getPhoneItem().createPhone(phoneQuality);
                if (quantity > 1) {
                    supplyItem.setAmount(quantity);
                }
            } catch (NumberFormatException e) {
                // В случае ошибки создаем обычный предмет
                supplyItem = createSupplyItem(supply, quantity);
            }
        } else {
            // Для обычных предметов
            supplyItem = createSupplyItem(supply, quantity);
        }

        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(supplyItem);

        if (!leftover.isEmpty()) {
            // Return money for items that couldn't be added
            int leftoverAmount = 0;
            for (ItemStack stack : leftover.values()) {
                leftoverAmount += stack.getAmount();
            }

            double refundAmount = supply.price * leftoverAmount;
            plugin.getPlayerService().addCash(player.getUniqueId(), refundAmount);

            player.sendMessage(Component.text("Инвентарь полон! Куплено только " +
                    (quantity - leftoverAmount) + " шт. Деньги возвращены.").color(NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("Вы купили " + supply.name + " x" + quantity +
                    " за $" + totalCost).color(NamedTextColor.GREEN));
        }

        return true;
    }

    /**
     * Create a custom item to represent the supply
     */
    private ItemStack createSupplyItem(SupplyItem supply, int quantity) {
        ItemStack item = new ItemStack(org.bukkit.Material.getMaterial(supply.material), quantity);
        ItemMeta meta = item.getItemMeta();

        // Set appropriate color based on category
        NamedTextColor nameColor;
        switch (supply.category) {
            case "equipment":
                nameColor = NamedTextColor.AQUA;
                break;
            case "seeds":
                nameColor = NamedTextColor.GREEN;
                break;
            case "chemicals":
                nameColor = NamedTextColor.LIGHT_PURPLE;
                break;
            case "supplies":
                nameColor = NamedTextColor.GOLD;
                break;
            case "phone":
                nameColor = NamedTextColor.BLUE;
                break;
            default:
                nameColor = NamedTextColor.WHITE;
        }

        meta.displayName(Component.text(supply.name).color(nameColor));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Цена: $" + supply.price).color(NamedTextColor.GRAY));
        lore.add(Component.text(supply.description).color(NamedTextColor.GRAY));

        meta.lore(lore);

        // Add custom data to identify this as a supply item
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "supply_type"),
                PersistentDataType.STRING,
                supply.category + ":" + supply.name
        );

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Создает предмет расходников по ID
     */
    public ItemStack createSupplyItem(String supplyId) {
        SupplyItem supply = availableSupplies.get(supplyId);
        if (supply == null) {
            return null;
        }
        return createSupplyItem(supply, 1);
    }

    public void setDaytime(boolean isDaytime) {
        this.isDaytime = isDaytime;
    }

    /**
     * Inner class to represent a supply item
     */
    public static class SupplyItem {
        final String name;
        final double price;
        final String description;
        final String material;
        final String category;

        SupplyItem(String name, double price, String description, String material, String category) {
            this.name = name;
            this.price = price;
            this.description = description;
            this.material = material;
            this.category = category;
        }
    }
}