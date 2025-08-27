package com.Minecraft_server.Narkomanka.npc;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.database.PlayerData;
import com.Minecraft_server.Narkomanka.ui.DrugDealerMenu;
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
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class DrugDealerNPC implements Listener {

    private final Narkomanka plugin;
    private final Map<String, DrugItem> availableDrugs = new HashMap<>();
    private final NamespacedKey npcTypeKey;
    private boolean isDaytime = true;

    // Меню дилера
    private DrugDealerMenu drugDealerMenu;

    public DrugDealerNPC(Narkomanka plugin) {
        this.plugin = plugin;
        this.npcTypeKey = new NamespacedKey(plugin, "npc_type");

        // Initialize drug types with different effects and prices
        availableDrugs.put("marijuana", new DrugItem("Марихуана", 150.0, "Лёгкий наркотик. Слабо влияет на ментальное здоровье.", "DRIED_KELP"));
        availableDrugs.put("cocaine", new DrugItem("Кокаин", 400.0, "Средний наркотик. Умеренно влияет на ментальное здоровье.", "SUGAR"));
        availableDrugs.put("meth", new DrugItem("Метамфетамин", 600.0, "Тяжёлый наркотик. Сильно влияет на ментальное здоровье.", "GLOWSTONE_DUST"));
        availableDrugs.put("heroin", new DrugItem("Героин", 900.0, "Очень тяжёлый наркотик. Критически влияет на ментальное здоровье.", "GUNPOWDER"));

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Создаем меню
        this.drugDealerMenu = new DrugDealerMenu(plugin, this);

        // Start time check task
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkDaytime, 0L, 600L); // Check every 30 seconds
    }

    /**
     * Spawn drug dealer NPC at a specific location
     */
    public void spawnDrugDealer(Location location) {
        Villager dealer = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);

        // Set custom name and make it visible
        dealer.customName(Component.text("Дилер").color(NamedTextColor.RED));
        dealer.setCustomNameVisible(true);

        // Set profession and prevent entity AI if desired
        dealer.setProfession(Villager.Profession.NITWIT);
        dealer.setAI(false);
        dealer.setInvulnerable(true);
        dealer.setRemoveWhenFarAway(false);

        // Set custom tag to identify this as a drug dealer
        dealer.getPersistentDataContainer().set(npcTypeKey, PersistentDataType.STRING, "drug_dealer");

        plugin.getLogger().info("Drug dealer NPC spawned at " + location.getWorld().getName() +
                " " + location.getX() + " " + location.getY() + " " + location.getZ());
    }

    /**
     * Handle interaction with the drug dealer
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // Check if interacted entity is our NPC
        if (!(event.getRightClicked() instanceof Villager)) return;

        Villager villager = (Villager) event.getRightClicked();
        if (!villager.getPersistentDataContainer().has(npcTypeKey, PersistentDataType.STRING)) return;

        String npcType = villager.getPersistentDataContainer().get(npcTypeKey, PersistentDataType.STRING);
        if (!"drug_dealer".equals(npcType)) return;

        // Prevent further interaction
        event.setCancelled(true);

        // Check if it's night time
        if (!isDaytime) {
            event.getPlayer().sendMessage(Component.text("Дилер не работает ночью. Приходи днём!").color(NamedTextColor.RED));
            return;
        }

        // Open drug dealer menu
        drugDealerMenu.openMenu(event.getPlayer());
    }

    /**
     * Process drug purchase
     */
    public boolean purchaseDrug(Player player, String drugType, int quantity) {
        if (!isDaytime) {
            player.sendMessage(Component.text("Дилер не работает ночью. Приходи днём!").color(NamedTextColor.RED));
            return false;
        }

        // Validate drug type
        if (!availableDrugs.containsKey(drugType)) {
            player.sendMessage(Component.text("Такого наркотика нет в продаже.").color(NamedTextColor.RED));
            return false;
        }

        // Get drug info
        DrugItem drug = availableDrugs.get(drugType);
        double totalCost = drug.price * quantity;

        // Check if player has enough money
        PlayerData playerData = plugin.getPlayerService().getOrCreatePlayerData(player);
        if (playerData.getCashBalance() < totalCost) {
            player.sendMessage(Component.text("У вас недостаточно наличных! Нужно: $" + totalCost).color(NamedTextColor.RED));
            return false;
        }

        // Remove money and give drug item
        boolean success = plugin.getPlayerService().removeCash(player.getUniqueId(), totalCost);
        if (!success) {
            player.sendMessage(Component.text("Ошибка при оплате.").color(NamedTextColor.RED));
            return false;
        }

        // Give item to player
        ItemStack drugItem = createDrugItem(drug, quantity);
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drugItem);

        if (!leftover.isEmpty()) {
            // Return money for items that couldn't be added
            int leftoverAmount = 0;
            for (ItemStack stack : leftover.values()) {
                leftoverAmount += stack.getAmount();
            }

            double refundAmount = drug.price * leftoverAmount;
            plugin.getPlayerService().addCash(player.getUniqueId(), refundAmount);

            player.sendMessage(Component.text("Инвентарь полон! Куплено только " +
                    (quantity - leftoverAmount) + " шт. Деньги возвращены.").color(NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("Вы купили " + drug.name + " x" + quantity +
                    " за $" + totalCost).color(NamedTextColor.GREEN));
        }

        return true;
    }

    /**
     * Create a custom item to represent the drug
     */
    private ItemStack createDrugItem(DrugItem drug, int quantity) {
        // Here you would create a custom item using Bukkit ItemStack API
        // This is a simplified example
        ItemStack item = new ItemStack(org.bukkit.Material.getMaterial(drug.material), quantity);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(drug.name).color(NamedTextColor.RED));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Цена: $" + drug.price).color(NamedTextColor.GRAY));
        lore.add(Component.text(drug.description).color(NamedTextColor.GRAY));
        lore.add(Component.text("Нелегальный товар").color(NamedTextColor.DARK_RED));

        meta.lore(lore);

        // Add custom data to identify this as a drug
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "drug_type"),
                PersistentDataType.STRING,
                drug.name
        );

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Check if it's daytime on the server
     */
    private void checkDaytime() {
        // If world exists, check the time
        if (Bukkit.getWorlds().isEmpty()) return;

        long time = Bukkit.getWorlds().get(0).getTime();

        // In Minecraft, 0-12000 is day, 12001-24000 is night
        boolean newDaytime = (time < 12000);

        // Only notify on change
        if (isDaytime != newDaytime) {
            isDaytime = newDaytime;

            if (isDaytime) {
                Bukkit.broadcast(Component.text("Дилеры снова начали работу!").color(NamedTextColor.GREEN));
            } else {
                Bukkit.broadcast(Component.text("Дилеры прекратили работу до утра!").color(NamedTextColor.RED));
            }
        }
    }

    public boolean isDaytime() {
        return isDaytime;
    }

    public void setDaytime(boolean isDaytime) {
        this.isDaytime = isDaytime;
    }

    /**
     * Inner class to represent a drug item
     */
    public static class DrugItem {
        final String name;
        final double price;
        final String description;
        final String material;

        DrugItem(String name, double price, String description, String material) {
            this.name = name;
            this.price = price;
            this.description = description;
            this.material = material;
        }
    }
}