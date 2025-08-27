package com.Minecraft_server.Narkomanka.listeners;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.npc.JunkieNPC;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Слушатель для обработки взаимодействий игроков с NPC
 */
public class NPCInteractionListener implements Listener {
    
    private final Narkomanka plugin;
    private final NamespacedKey npcTypeKey;
    private final NamespacedKey npcIdKey;
    
    public NPCInteractionListener(Narkomanka plugin) {
        this.plugin = plugin;
        this.npcTypeKey = new NamespacedKey(plugin, "npc_type");
        this.npcIdKey = new NamespacedKey(plugin, "npc_id");
        
        // Регистрируем слушатель
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        
        // Проверяем, является ли сущность жителем (villager)
        if (!(entity instanceof Villager)) {
            return;
        }
        
        // Проверяем, является ли житель NPC
        if (!entity.getPersistentDataContainer().has(npcTypeKey, PersistentDataType.STRING)) {
            return;
        }
        
        // Получаем тип NPC
        String npcType = entity.getPersistentDataContainer().get(npcTypeKey, PersistentDataType.STRING);
        
        if (npcType == null) {
            return;
        }
        
        // Получаем ID NPC
        String npcIdStr = entity.getPersistentDataContainer().get(npcIdKey, PersistentDataType.STRING);
        
        if (npcIdStr == null) {
            return;
        }
        
        UUID npcId;
        try {
            npcId = UUID.fromString(npcIdStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid NPC UUID: " + npcIdStr);
            return;
        }
        
        // Отменяем стандартное взаимодействие с жителем
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        
        // Обрабатываем взаимодействие в зависимости от типа NPC
        switch (npcType) {
            case "junkie":
                handleJunkieInteraction(player, npcId);
                break;
            case "police":
                handlePoliceInteraction(player, npcId);
                break;
            case "citizen":
                handleCitizenInteraction(player, npcId);
                break;
        }
    }
    
    /**
     * Обрабатывает взаимодействие с наркоманом
     */
    private void handleJunkieInteraction(Player player, UUID npcId) {
        if (plugin.getNPCManager() == null) {
            return;
        }
        
        JunkieNPC junkie = plugin.getNPCManager().getJunkieNPC(npcId);
        
        if (junkie == null) {
            return;
        }
        
        // Обрабатываем продажу наркотиков
        junkie.handleDrugSale(player);
    }
    
    /**
     * Обрабатывает взаимодействие с полицейским
     */
    private void handlePoliceInteraction(Player player, UUID npcId) {
        // В Schedule I с полицейскими не взаимодействуют напрямую
        // Полицейские сами арестовывают игроков
    }
    
    /**
     * Обрабатывает взаимодействие с мирным жителем
     */
    private void handleCitizenInteraction(Player player, UUID npcId) {
        // Мирные жители в Schedule I не имеют специального взаимодействия
        // Они только реагируют на действия игрока
    }
} 