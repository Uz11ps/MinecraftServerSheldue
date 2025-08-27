package com.Minecraft_server.Narkomanka.trash;

import com.Minecraft_server.Narkomanka.Narkomanka;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Класс для управления контейнерами для мусора
 */
public class TrashContainer implements Listener {
    private final Narkomanka plugin;
    private final NamespacedKey containerKey;
    private final NamespacedKey ownerKey;
    private final Material containerMaterial;
    private final String containerName;
    private final int customModelId;
    
    // Хранилище инвентарей контейнеров по их ID
    private final Map<UUID, Inventory> containerInventories = new HashMap<>();

    public TrashContainer(Narkomanka plugin) {
        this.plugin = plugin;
        this.containerKey = new NamespacedKey(plugin, "trash_container");
        this.ownerKey = new NamespacedKey(plugin, "container_owner");

        // Загружаем настройки из конфига или устанавливаем дефолтные
        this.containerMaterial = Material.valueOf(
                plugin.getConfig().getString("trash.container.material", "CHEST")
        );

        this.containerName = plugin.getConfig().getString("trash.container.name", "Сжигатель Коли");
        this.customModelId = plugin.getConfig().getInt("trash.container.custom_model_id", 1);
        
        // Регистрируем слушатель событий
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Создаёт новый контейнер для мусора с текстурой СжигательКоли
     */
    public ItemStack createContainer(UUID ownerUuid) {
        ItemStack container = new ItemStack(containerMaterial);
        ItemMeta meta = container.getItemMeta();

        // Устанавливаем название и описание
        meta.displayName(net.kyori.adventure.text.Component.text(containerName)
                .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text("Контейнер для мусора")
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        lore.add(net.kyori.adventure.text.Component.text("ПКМ - открыть контейнер")
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        lore.add(net.kyori.adventure.text.Component.text("Текстура: СжигательКоли")
                .color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
        meta.lore(lore);

        // Устанавливаем кастомную модель - важно для отображения текстуры
        meta.setCustomModelData(customModelId);
        plugin.getLogger().info("Установлен CustomModelData=" + customModelId + " для " + containerName);

        // Помечаем предмет как контейнер для мусора и сохраняем владельца
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        dataContainer.set(containerKey, PersistentDataType.BYTE, (byte) 1);
        
        // Сохраняем UUID владельца
        UUID containerUuid = UUID.randomUUID();
        dataContainer.set(ownerKey, PersistentDataType.STRING, containerUuid.toString());
        
        // Создаем инвентарь для этого контейнера
        Inventory containerInventory = Bukkit.createInventory(null, 27, 
                net.kyori.adventure.text.Component.text(containerName));
        containerInventories.put(containerUuid, containerInventory);

        // Устанавливаем метаданные и возвращаем предмет
        container.setItemMeta(meta);
        
        plugin.getLogger().info("Создан контейнер для мусора с CustomModelData=" + customModelId);
        
        return container;
    }

    /**
     * Создаёт контейнер через консольную команду give с гарантией текстуры
     */
    public void createContainerWithModel(Player player) {
        plugin.getLogger().info("Создаем контейнер мусора с текстурой через команду give...");
        try {
            // Команда для прямого создания предмета с нужными NBT-тегами
            String command = "give " + player.getName() + " chest{CustomModelData:1,display:{Name:'{\"text\":\"Сжигатель Коли\",\"color\":\"green\"}',Lore:['{\"text\":\"Контейнер для мусора\",\"color\":\"gray\"}','{\"text\":\"ПКМ - открыть контейнер\",\"color\":\"gray\"}','{\"text\":\"Текстура: СжигательКоли\",\"color\":\"gold\"}']},NarkoTrashContainer:1b,ContainerOwner:\"" + UUID.randomUUID() + "\"}";
            
            plugin.getLogger().info("Выполняем команду: " + command);
            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            
            if (success) {
                plugin.getLogger().info("Команда выполнена успешно");
                player.sendMessage(net.kyori.adventure.text.Component.text("Вы получили контейнер для мусора «Сжигатель Коли»!")
                        .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
            } else {
                plugin.getLogger().warning("Ошибка при выполнении команды give");
                player.sendMessage(net.kyori.adventure.text.Component.text("Ошибка при создании контейнера")
                        .color(net.kyori.adventure.text.format.NamedTextColor.RED));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при создании контейнера через команду: " + e.getMessage());
            player.sendMessage(net.kyori.adventure.text.Component.text("Ошибка: " + e.getMessage())
                    .color(net.kyori.adventure.text.format.NamedTextColor.RED));
        }
    }

    /**
     * Проверяет, является ли предмет контейнером для мусора
     */
    public boolean isContainer(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.has(containerKey, PersistentDataType.BYTE);
    }

    /**
     * Получает UUID контейнера
     */
    public UUID getContainerUuid(ItemStack item) {
        if (!isContainer(item)) return null;
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        
        String uuidStr = container.get(ownerKey, PersistentDataType.STRING);
        if (uuidStr == null) return null;
        
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Выдаёт контейнер для мусора игроку с гарантированной установкой текстуры
     */
    public void giveContainerToPlayer(Player player) {
        // Сначала пробуем выдать через команду give
        if (player.hasPermission("minecraft.command.give")) {
            createContainerWithModel(player);
            return;
        }
        
        // Если нет разрешения на команду give, создаем стандартным способом
        ItemStack container = createContainer(player.getUniqueId());

        // Проверяем, есть ли у игрока свободное место в инвентаре
        if (player.getInventory().firstEmpty() == -1) {
            // Если инвентарь полон, выбрасываем предмет перед игроком
            player.getWorld().dropItem(player.getLocation(), container);
            player.sendMessage("§cВаш инвентарь полон! Сжигатель мусора выброшен на землю.");
        } else {
            // Иначе добавляем предмет в инвентарь
            player.getInventory().addItem(container);
            player.sendMessage("§aВы получили сжигатель мусора \"Сжигатель Коли\"!");
        }
    }
    
    /**
     * Обработчик взаимодействия с контейнером
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Проверяем что это взаимодействие правой кнопкой и в основной руке
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        // Проверяем, что игрок держит контейнер
        ItemStack item = event.getItem();
        if (item == null || !isContainer(item)) return;
        
        // Отменяем стандартное действие
        event.setCancelled(true);
        
        // Получаем UUID контейнера
        UUID containerUuid = getContainerUuid(item);
        if (containerUuid == null) {
            // Если UUID нет, создаем новый инвентарь
            containerUuid = UUID.randomUUID();
            Inventory containerInventory = Bukkit.createInventory(null, 27, 
                    net.kyori.adventure.text.Component.text(containerName));
            containerInventories.put(containerUuid, containerInventory);
            
            // Обновляем UUID в предмете
            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(ownerKey, PersistentDataType.STRING, containerUuid.toString());
            item.setItemMeta(meta);
        }
        
        // Открываем инвентарь контейнера
        Inventory inventory = containerInventories.getOrDefault(containerUuid, 
                Bukkit.createInventory(null, 27, net.kyori.adventure.text.Component.text(containerName)));
        containerInventories.putIfAbsent(containerUuid, inventory);
        
        event.getPlayer().openInventory(inventory);
    }
    
    /**
     * Проверяет, содержит ли контейнер мусор и возвращает его количество
     */
    public int getTrashAmountInContainer(UUID containerUuid) {
        if (!containerInventories.containsKey(containerUuid)) {
            return 0;
        }
        
        Inventory inventory = containerInventories.get(containerUuid);
        int count = 0;
        
        for (ItemStack item : inventory.getContents()) {
            if (item != null && plugin.getTrashManager().isTrash(item)) {
                count += item.getAmount();
            }
        }
        
        return count;
    }
    
    /**
     * Опустошает контейнер от мусора и возвращает его общую стоимость
     */
    public int emptyContainerAndGetValue(UUID containerUuid) {
        if (!containerInventories.containsKey(containerUuid)) {
            return 0;
        }
        
        Inventory inventory = containerInventories.get(containerUuid);
        int totalValue = 0;
        
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && plugin.getTrashManager().isTrash(item)) {
                totalValue += plugin.getTrashManager().getTrashValue(item) * item.getAmount();
                inventory.clear(i);
            }
        }
        
        return totalValue;
    }
    
    /**
     * Получает инвентарь по UUID контейнера
     */
    public Inventory getContainerInventory(UUID containerUuid) {
        return containerInventories.get(containerUuid);
    }

    /**
     * Получает материал, используемый для контейнера мусора
     */
    public Material getContainerMaterial() {
        return containerMaterial;
    }

    /**
     * Получает название контейнера мусора
     */
    public String getContainerName() {
        return containerName;
    }

    /**
     * Получает ключ метаданных для контейнера мусора
     */
    public NamespacedKey getContainerKey() {
        return containerKey;
    }
} 