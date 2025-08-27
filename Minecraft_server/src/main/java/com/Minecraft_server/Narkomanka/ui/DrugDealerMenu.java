package com.Minecraft_server.Narkomanka.ui;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.npc.DrugDealerNPC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class DrugDealerMenu implements Listener {

    private final Narkomanka plugin;
    private final DrugDealerNPC drugDealerNPC;
    private final Map<UUID, Inventory> openMenus = new HashMap<>();

    // Keys для хранения метаданных предметов
    private final NamespacedKey drugTypeKey;
    private final NamespacedKey drugPriceKey;
    private final NamespacedKey actionKey;

    // Константы для интерфейса
    private static final int MENU_SIZE = 36; // 4 ряда
    private static final String MENU_TITLE = "Дилер наркотиков";

    // Информация о наркотиках
    private final Map<Integer, DrugInfo> drugSlots = new HashMap<>();

    public DrugDealerMenu(Narkomanka plugin, DrugDealerNPC drugDealerNPC) {
        this.plugin = plugin;
        this.drugDealerNPC = drugDealerNPC;

        this.drugTypeKey = new NamespacedKey(plugin, "drug_type");
        this.drugPriceKey = new NamespacedKey(plugin, "drug_price");
        this.actionKey = new NamespacedKey(plugin, "menu_action");

        // Регистрация слушателя событий
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Инициализация слотов для наркотиков
        initializeDrugSlots();
    }

    private void initializeDrugSlots() {
        // Марихуана
        drugSlots.put(10, new DrugInfo("marijuana", "Марихуана", 150.0, Material.DRIED_KELP,
                "§aЛёгкий наркотик", "§7Слабо влияет на ментальное здоровье", "§7Цена: §6$150.0"));

        // Кокаин
        drugSlots.put(12, new DrugInfo("cocaine", "Кокаин", 400.0, Material.SUGAR,
                "§eСредний наркотик", "§7Умеренно влияет на ментальное здоровье", "§7Цена: §6$400.0"));

        // Метамфетамин
        drugSlots.put(14, new DrugInfo("meth", "Метамфетамин", 600.0, Material.GLOWSTONE_DUST,
                "§cТяжёлый наркотик", "§7Сильно влияет на ментальное здоровье", "§7Цена: §6$600.0"));

        // Героин
        drugSlots.put(16, new DrugInfo("heroin", "Героин", 900.0, Material.GUNPOWDER,
                "§4Очень тяжёлый наркотик", "§7Критически влияет на ментальное здоровье", "§7Цена: §6$900.0"));
    }

    /**
     * Открывает меню дилера для игрока
     */
    public void openMenu(Player player) {
        // Создаем инвентарь
        Inventory menu = Bukkit.createInventory(null, MENU_SIZE, Component.text(MENU_TITLE).color(NamedTextColor.DARK_RED));

        // Заполняем фон серым стеклом
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < MENU_SIZE; i++) {
            menu.setItem(i, filler);
        }

        // Размещаем товары
        for (Map.Entry<Integer, DrugInfo> entry : drugSlots.entrySet()) {
            DrugInfo drugInfo = entry.getValue();
            ItemStack drugItem = createDrugItem(drugInfo);
            menu.setItem(entry.getKey(), drugItem);
        }

        // Кнопки покупки разных количеств
        menu.setItem(28, createActionButton("buy_1", Material.LIME_CONCRETE, "Купить 1", "Нажмите, чтобы купить 1 единицу"));
        menu.setItem(30, createActionButton("buy_5", Material.LIME_CONCRETE, "Купить 5", "Нажмите, чтобы купить 5 единиц"));
        menu.setItem(32, createActionButton("buy_10", Material.LIME_CONCRETE, "Купить 10", "Нажмите, чтобы купить 10 единиц"));
        menu.setItem(34, createActionButton("buy_50", Material.LIME_CONCRETE, "Купить 50", "Нажмите, чтобы купить 50 единиц"));

        // Информация о балансе
        double cash = plugin.getPlayerService().getOrCreatePlayerData(player).getCashBalance();
        ItemStack balanceItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta balanceMeta = balanceItem.getItemMeta();
        balanceMeta.displayName(Component.text("Ваш баланс").color(NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Наличные: $" + cash).color(NamedTextColor.YELLOW));
        balanceMeta.lore(lore);
        balanceItem.setItemMeta(balanceMeta);
        menu.setItem(4, balanceItem);

        // Показываем меню игроку
        player.openInventory(menu);

        // Запоминаем, что этот игрок открыл меню
        openMenus.put(player.getUniqueId(), menu);
    }

    private ItemStack createDrugItem(DrugInfo drugInfo) {
        ItemStack item = new ItemStack(drugInfo.material);
        ItemMeta meta = item.getItemMeta();

        // Назначаем имя предмета
        meta.displayName(Component.text(drugInfo.displayName).color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));

        // Устанавливаем описание
        List<Component> lore = new ArrayList<>();
        for (String desc : drugInfo.description) {
            lore.add(Component.text(desc).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("§4Нелегальный товар").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        // Сохраняем метаданные для идентификации при клике
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(drugTypeKey, PersistentDataType.STRING, drugInfo.id);
        container.set(drugPriceKey, PersistentDataType.DOUBLE, drugInfo.price);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActionButton(String action, Material material, String displayName, String description) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();

        meta.displayName(Component.text(displayName).color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(description).color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        // Сохраняем действие в метаданных
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);

        button.setItemMeta(meta);
        return button;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Проверяем, что это наше меню
        if (!openMenus.containsKey(player.getUniqueId())) return;
        if (event.getClickedInventory() == null) return;
        if (!event.getView().title().equals(Component.text(MENU_TITLE).color(NamedTextColor.DARK_RED))) return;

        // Отменяем действие, чтобы игрок не мог забрать предметы из меню
        event.setCancelled(true);

        // Получаем кликнутый предмет
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();

        // Проверяем, кликнул ли игрок на кнопку действия
        if (container.has(actionKey, PersistentDataType.STRING)) {
            String action = container.get(actionKey, PersistentDataType.STRING);
            handleActionButton(player, action);
            return;
        }

        // Проверяем, кликнул ли игрок на наркотик
        if (container.has(drugTypeKey, PersistentDataType.STRING)) {
            String drugType = container.get(drugTypeKey, PersistentDataType.STRING);
            selectDrug(player, drugType);
        }
    }

    private void handleActionButton(Player player, String action) {
        // Проверка, выбрал ли игрок наркотик (сохранен в метаданных игрока)
        PersistentDataContainer playerContainer = player.getPersistentDataContainer();
        if (!playerContainer.has(drugTypeKey, PersistentDataType.STRING)) {
            player.sendMessage(Component.text("Сначала выберите товар!").color(NamedTextColor.RED));
            return;
        }

        String drugType = playerContainer.get(drugTypeKey, PersistentDataType.STRING);

        int quantity = 1;
        if (action.equals("buy_5")) quantity = 5;
        else if (action.equals("buy_10")) quantity = 10;
        else if (action.equals("buy_50")) quantity = 50;

        // Закрываем меню, чтобы обновить его после покупки
        player.closeInventory();

        // Совершаем покупку
        boolean success = drugDealerNPC.purchaseDrug(player, drugType, quantity);

        // Если покупка успешна, обновляем меню
        if (success) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> openMenu(player), 2L);
        }
    }

    private void selectDrug(Player player, String drugType) {
        // Сохраняем выбранный наркотик в метаданных игрока
        PersistentDataContainer container = player.getPersistentDataContainer();
        container.set(drugTypeKey, PersistentDataType.STRING, drugType);

        // Находим информацию о наркотике
        DrugInfo selectedDrug = null;
        for (DrugInfo drug : drugSlots.values()) {
            if (drug.id.equals(drugType)) {
                selectedDrug = drug;
                break;
            }
        }

        if (selectedDrug != null) {
            player.sendMessage(Component.text("Выбран товар: ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text(selectedDrug.displayName)
                            .color(NamedTextColor.RED)));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Удаляем запись о меню, когда игрок закрывает его
        openMenus.remove(player.getUniqueId());
    }

    /**
     * Класс для хранения информации о наркотиках
     */
    private static class DrugInfo {
        final String id;
        final String displayName;
        final double price;
        final Material material;
        final String[] description;

        DrugInfo(String id, String displayName, double price, Material material, String... description) {
            this.id = id;
            this.displayName = displayName;
            this.price = price;
            this.material = material;
            this.description = description;
        }
    }
}