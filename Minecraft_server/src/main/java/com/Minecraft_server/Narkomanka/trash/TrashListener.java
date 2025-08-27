package com.Minecraft_server.Narkomanka.trash;

import com.Minecraft_server.Narkomanka.Narkomanka;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Обработчик событий для системы мусора
 */
public class TrashListener implements Listener {
    private final Narkomanka plugin;

    // Кэш для предотвращения дублирования событий
    private final Map<UUID, Long> lastInteractCooldown = new HashMap<>();
    private static final long INTERACT_COOLDOWN_MS = 500; // 500 мс

    public TrashListener(Narkomanka plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Обработка взаимодействия с мусором и станциями переработки
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Проверяем что событие происходит от основной руки
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        
        // Если взаимодействие с блоком и это правый клик
        if (clickedBlock != null && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Проверяем, является ли блок станцией переработки
            if (plugin.getTrashStation().isTrashStation(clickedBlock)) {
                // Обрабатываем сдачу мусора
                plugin.getTrashStation().processTrashTurn(player, clickedBlock);
                event.setCancelled(true);
                return;
            }
        }
        
        // Проверяем взаимодействие с мусоросборником
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem != null && plugin.getTrashCollector().isCollector(handItem)) {
            handleCollectorUse(event);
        }
    }
    
    /**
     * Обрабатывает использование мусоросборника
     */
    private void handleCollectorUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Ищем ближайший мусор в радиусе 3 блоков
        event.getPlayer().getNearbyEntities(3, 3, 3).forEach(entity -> {
            if (entity instanceof org.bukkit.entity.Item itemEntity) {
                ItemStack item = itemEntity.getItemStack();
                if (plugin.getTrashManager().isTrash(item)) {
                    // Подбираем мусор в инвентарь игрока
                    if (player.getInventory().firstEmpty() != -1) {
                        player.getInventory().addItem(item);
                        itemEntity.remove();
                        player.sendMessage("§aВы собрали мусор стоимостью §e$" + 
                            plugin.getTrashManager().getTrashValue(item) * item.getAmount());
                    } else {
                        player.sendMessage("§cВаш инвентарь полон! Освободите место для мусора.");
                    }
                }
            }
        });
        
        // Отменяем событие, чтобы не использовать мусоросборник как обычный предмет
        event.setCancelled(true);
    }

    /**
     * Обработка клика по предмету-мусору
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof Item itemEntity)) return;

        Player player = event.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();

        // Если игрок держит мусоросборник и кликает по предмету, который является мусором
        if (plugin.getTrashCollector().isCollector(handItem) &&
                plugin.getTrashManager().isTrash(itemEntity.getItemStack())) {

            event.setCancelled(true); // Отменяем стандартное взаимодействие

            ItemStack trashItem = itemEntity.getItemStack();
            int value = plugin.getTrashManager().getTrashValue(trashItem);
            String displayName = trashItem.getItemMeta().displayName().toString();

            // Проверяем, есть ли место в инвентаре
            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage(Component.text("Ваш инвентарь полон!").color(NamedTextColor.RED));
                return;
            }

            // Добавляем мусор в инвентарь и удаляем с земли
            player.getInventory().addItem(trashItem);
            itemEntity.remove();

            player.sendMessage(Component.text("Вы подобрали мусор: ").color(NamedTextColor.GREEN)
                    .append(Component.text(displayName).color(NamedTextColor.GOLD))
                    .append(Component.text(" (ценность: $" + value + ")").color(NamedTextColor.GREEN)));

            // Удаляем из системы отслеживания
            plugin.getTrashManager().removeTrashItem(itemEntity.getUniqueId());

            // Звуковой эффект
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
        }
    }

    /**
     * Предотвращаем обычный подбор мусора игроками без сборщика
     */
    @EventHandler
    public void onPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Item item = event.getItem();
        ItemStack itemStack = item.getItemStack();

        // Если предмет является мусором, и у игрока нет мусоросборника в руке, отменяем подбор
        if (plugin.getTrashManager().isTrash(itemStack)) {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (!plugin.getTrashCollector().isCollector(handItem)) {
                event.setCancelled(true);
                player.sendActionBar(Component.text("Используйте мусоросборник для сбора мусора!")
                        .color(NamedTextColor.RED));
            }
        }
    }

    /**
     * Обработка нажатий в инвентаре
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Ничего особенного здесь пока не требуется
    }

    /**
     * Обработка закрытия инвентаря
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Ничего особенного здесь пока не требуется
    }
}