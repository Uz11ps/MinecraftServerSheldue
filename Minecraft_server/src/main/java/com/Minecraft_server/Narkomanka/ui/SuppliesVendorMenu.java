package com.Minecraft_server.Narkomanka.ui;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.npc.SuppliesVendorNPC;
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

public class SuppliesVendorMenu implements Listener {

    private final Narkomanka plugin;
    private final SuppliesVendorNPC suppliesVendorNPC;
    private final Map<UUID, Inventory> openMenus = new HashMap<>();

    // Keys для хранения метаданных предметов
    private final NamespacedKey supplyTypeKey;
    private final NamespacedKey supplyPriceKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey categoryKey;

    // Константы для интерфейса
    private static final int MENU_SIZE = 54; // 6 рядов
    private static final String MENU_TITLE = "Поставщик оборудования";

    // Информация о категориях
    private final Map<String, Component> categoryTitles = new HashMap<>();
    private final Map<String, Integer> categorySlots = new HashMap<>();

    // Информация о товарах
    private final Map<String, List<SupplyInfo>> categorySupplies = new HashMap<>();

    public SuppliesVendorMenu(Narkomanka plugin, SuppliesVendorNPC suppliesVendorNPC) {
        this.plugin = plugin;
        this.suppliesVendorNPC = suppliesVendorNPC;

        this.supplyTypeKey = new NamespacedKey(plugin, "supply_type");
        this.supplyPriceKey = new NamespacedKey(plugin, "supply_price");
        this.actionKey = new NamespacedKey(plugin, "menu_action");
        this.categoryKey = new NamespacedKey(plugin, "category");

        // Регистрация слушателя событий
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Инициализация категорий
        initializeCategories();

        // Инициализация товаров
        initializeSupplies();
    }

    private void initializeCategories() {
        // Названия категорий с цветами
        categoryTitles.put("equipment", Component.text("Оборудование").color(NamedTextColor.AQUA));
        categoryTitles.put("seeds", Component.text("Семена").color(NamedTextColor.GREEN));
        categoryTitles.put("chemicals", Component.text("Химикаты").color(NamedTextColor.LIGHT_PURPLE));
        categoryTitles.put("supplies", Component.text("Расходники").color(NamedTextColor.GOLD));

        // Слоты для кнопок категорий
        categorySlots.put("equipment", 0);
        categorySlots.put("seeds", 1);
        categorySlots.put("chemicals", 2);
        categorySlots.put("supplies", 3);
    }

    private void initializeSupplies() {
        // Инициализируем списки для каждой категории
        for (String category : categoryTitles.keySet()) {
            categorySupplies.put(category, new ArrayList<>());
        }

        // Оборудование
        categorySupplies.get("equipment").add(new SupplyInfo(
                "growbox", "Гроубокс", 1200.0, Material.BARREL, "equipment",
                "§bОборудование для выращивания", "§7Базовая установка для выращивания", "§7Цена: §6$1200.0"
        ));

        categorySupplies.get("equipment").add(new SupplyInfo(
                "hydroponic", "Гидропоника", 2500.0, Material.COMPOSTER, "equipment",
                "§bОборудование для выращивания", "§7Продвинутая установка для выращивания", "§7Цена: §6$2500.0"
        ));

        categorySupplies.get("equipment").add(new SupplyInfo(
                "lights", "Лампы для выращивания", 300.0, Material.LANTERN, "equipment",
                "§bОборудование для освещения", "§7Ускоряют рост растений", "§7Цена: §6$300.0"
        ));

        categorySupplies.get("equipment").add(new SupplyInfo(
                "lab_equipment", "Лабораторное оборудование", 450.0, Material.BREWING_STAND, "equipment",
                "§bЛабораторное оборудование", "§7Для производства метамфетамина", "§7Цена: §6$450.0"
        ));

        categorySupplies.get("equipment").add(new SupplyInfo(
                "scales", "Весы", 60.0, Material.COMPARATOR, "equipment",
                "§bИзмерительное оборудование", "§7Для точного измерения доз", "§7Цена: §6$60.0"
        ));

        // Семена
        categorySupplies.get("seeds").add(new SupplyInfo(
                "marijuana_seeds", "Семена марихуаны", 50.0, Material.WHEAT_SEEDS, "seeds",
                "§aСемена растений", "§7Для выращивания марихуаны", "§7Цена: §6$50.0"
        ));

        categorySupplies.get("seeds").add(new SupplyInfo(
                "coca_seeds", "Семена коки", 120.0, Material.BEETROOT_SEEDS, "seeds",
                "§aСемена растений", "§7Для выращивания коки", "§7Цена: §6$120.0"
        ));

        categorySupplies.get("seeds").add(new SupplyInfo(
                "poppy_seeds", "Семена мака", 200.0, Material.POPPY, "seeds",
                "§aСемена растений", "§7Для выращивания мака (героин)", "§7Цена: §6$200.0"
        ));

        // Химикаты
        categorySupplies.get("chemicals").add(new SupplyInfo(
                "chemicals", "Химические реагенты", 80.0, Material.GLASS_BOTTLE, "chemicals",
                "§dХимические вещества", "§7Для обработки сырья", "§7Цена: §6$80.0"
        ));

        // Расходники
        categorySupplies.get("supplies").add(new SupplyInfo(
                "filters", "Фильтры", 25.0, Material.PAPER, "supplies",
                "§6Расходные материалы", "§7Для очистки продукта", "§7Цена: §6$25.0"
        ));

        categorySupplies.get("supplies").add(new SupplyInfo(
                "baggies", "Пакетики", 15.0, Material.MAP, "supplies",
                "§6Упаковочные материалы", "§7Для упаковки товара", "§7Цена: §6$15.0"
        ));
    }

    /**
     * Открывает меню поставщика для игрока
     */
    public void openMenu(Player player) {
        openMenu(player, "equipment"); // По умолчанию показываем категорию "Оборудование"
    }

    /**
     * Открывает меню поставщика для игрока с выбранной категорией
     */
    public void openMenu(Player player, String category) {
        if (!categorySupplies.containsKey(category)) {
            category = "equipment"; // Если категория неверна, показываем оборудование
        }

        // Создаем инвентарь
        Inventory menu = Bukkit.createInventory(null, MENU_SIZE, Component.text(MENU_TITLE).color(NamedTextColor.GREEN));

        // Заполняем фон серым стеклом
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < MENU_SIZE; i++) {
            menu.setItem(i, filler);
        }

        // Добавляем кнопки категорий
        for (Map.Entry<String, Integer> entry : categorySlots.entrySet()) {
            String cat = entry.getKey();
            int slot = entry.getValue();

            boolean isSelected = cat.equals(category);
            Material buttonMaterial = isSelected ? Material.LIME_CONCRETE : Material.WHITE_CONCRETE;

            ItemStack categoryButton = new ItemStack(buttonMaterial);
            ItemMeta meta = categoryButton.getItemMeta();

            meta.displayName(categoryTitles.get(cat).decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(isSelected ? "Выбрано" : "Нажмите, чтобы выбрать")
                    .color(isSelected ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, cat);

            categoryButton.setItemMeta(meta);
            menu.setItem(slot, categoryButton);
        }

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

        // Размещаем товары выбранной категории
        List<SupplyInfo> supplies = categorySupplies.get(category);

        int slot = 9; // Начинаем с первой ячейки второго ряда
        for (SupplyInfo supply : supplies) {
            ItemStack supplyItem = createSupplyItem(supply);
            menu.setItem(slot, supplyItem);
            slot++;
        }

        // Кнопки покупки разных количеств
        menu.setItem(45, createActionButton("buy_1", Material.LIME_CONCRETE, "Купить 1", "Нажмите, чтобы купить 1 единицу"));
        menu.setItem(47, createActionButton("buy_5", Material.LIME_CONCRETE, "Купить 5", "Нажмите, чтобы купить 5 единиц"));
        menu.setItem(49, createActionButton("buy_10", Material.LIME_CONCRETE, "Купить 10", "Нажмите, чтобы купить 10 единиц"));
        menu.setItem(51, createActionButton("buy_50", Material.LIME_CONCRETE, "Купить 50", "Нажмите, чтобы купить 50 единиц"));

        // Показываем меню игроку
        player.openInventory(menu);

        // Запоминаем, что этот игрок открыл меню
        openMenus.put(player.getUniqueId(), menu);
    }

    private ItemStack createSupplyItem(SupplyInfo supplyInfo) {
        ItemStack item = new ItemStack(supplyInfo.material);
        ItemMeta meta = item.getItemMeta();

        // Назначаем имя предмета
        NamedTextColor color;
        switch (supplyInfo.category) {
            case "equipment":
                color = NamedTextColor.AQUA;
                break;
            case "seeds":
                color = NamedTextColor.GREEN;
                break;
            case "chemicals":
                color = NamedTextColor.LIGHT_PURPLE;
                break;
            case "supplies":
                color = NamedTextColor.GOLD;
                break;
            default:
                color = NamedTextColor.WHITE;
        }

        meta.displayName(Component.text(supplyInfo.displayName)
                .color(color)
                .decoration(TextDecoration.ITALIC, false));

        // Устанавливаем описание
        List<Component> lore = new ArrayList<>();
        for (String desc : supplyInfo.description) {
            lore.add(Component.text(desc).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);

        // Сохраняем метаданные для идентификации при клике
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(supplyTypeKey, PersistentDataType.STRING, supplyInfo.id);
        container.set(supplyPriceKey, PersistentDataType.DOUBLE, supplyInfo.price);
        container.set(categoryKey, PersistentDataType.STRING, supplyInfo.category);

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
        if (!event.getView().title().equals(Component.text(MENU_TITLE).color(NamedTextColor.GREEN))) return;

        // Отменяем действие, чтобы игрок не мог забрать предметы из меню
        event.setCancelled(true);

        // Получаем кликнутый предмет
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();

        // Проверяем, кликнул ли игрок на кнопку категории
        if (container.has(categoryKey, PersistentDataType.STRING)) {
            String category = container.get(categoryKey, PersistentDataType.STRING);
            openMenu(player, category); // Открываем меню с выбранной категорией
            return;
        }

        // Проверяем, кликнул ли игрок на кнопку действия
        if (container.has(actionKey, PersistentDataType.STRING)) {
            String action = container.get(actionKey, PersistentDataType.STRING);
            handleActionButton(player, action);
            return;
        }

        // Проверяем, кликнул ли игрок на товар
        if (container.has(supplyTypeKey, PersistentDataType.STRING)) {
            String supplyType = container.get(supplyTypeKey, PersistentDataType.STRING);
            String category = container.get(categoryKey, PersistentDataType.STRING);
            selectSupply(player, supplyType, category);
        }
    }

    private void handleActionButton(Player player, String action) {
        // Проверка, выбрал ли игрок товар (сохранен в метаданных игрока)
        PersistentDataContainer playerContainer = player.getPersistentDataContainer();
        if (!playerContainer.has(supplyTypeKey, PersistentDataType.STRING)) {
            player.sendMessage(Component.text("Сначала выберите товар!").color(NamedTextColor.RED));
            return;
        }

        String supplyType = playerContainer.get(supplyTypeKey, PersistentDataType.STRING);

        // Получаем категорию из метаданных игрока
        String category = "equipment"; // По умолчанию
        if (playerContainer.has(categoryKey, PersistentDataType.STRING)) {
            category = playerContainer.get(categoryKey, PersistentDataType.STRING);
        }

        // Определяем количество товара для покупки
        int quantity = 1;
        if (action.equals("buy_5")) quantity = 5;
        else if (action.equals("buy_10")) quantity = 10;
        else if (action.equals("buy_50")) quantity = 50;

        // Закрываем меню, чтобы обновить его после покупки
        player.closeInventory();

        // Сохраняем финальные значения для использования в лямбда-выражении
        final String finalCategory = category;
        final int finalQuantity = quantity;

        // Совершаем покупку
        boolean success = suppliesVendorNPC.purchaseSupply(player, supplyType, finalQuantity);

        // Если покупка успешна, обновляем меню
        if (success) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> openMenu(player, finalCategory), 2L);
        }
    }

    private void selectSupply(Player player, String supplyType, String category) {
        // Сохраняем выбранный товар в метаданных игрока
        PersistentDataContainer container = player.getPersistentDataContainer();
        container.set(supplyTypeKey, PersistentDataType.STRING, supplyType);
        container.set(categoryKey, PersistentDataType.STRING, category);

        // Находим информацию о товаре
        SupplyInfo selectedSupply = null;
        for (SupplyInfo supply : categorySupplies.get(category)) {
            if (supply.id.equals(supplyType)) {
                selectedSupply = supply;
                break;
            }
        }

        if (selectedSupply != null) {
            player.sendMessage(Component.text("Выбран товар: ")
                    .color(NamedTextColor.GREEN)
                    .append(Component.text(selectedSupply.displayName)
                            .color(NamedTextColor.AQUA)));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Удаляем запись о меню, когда игрок закрывает его
        openMenus.remove(player.getUniqueId());
    }

    /**
     * Класс для хранения информации о товарах
     */
    private static class SupplyInfo {
        final String id;
        final String displayName;
        final double price;
        final Material material;
        final String category;
        final String[] description;

        SupplyInfo(String id, String displayName, double price, Material material, String category, String... description) {
            this.id = id;
            this.displayName = displayName;
            this.price = price;
            this.material = material;
            this.category = category;
            this.description = description;
        }
    }
}