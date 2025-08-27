package com.Minecraft_server.Narkomanka.npc;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.database.QuestData;
import com.Minecraft_server.Narkomanka.database.QuestProgress;
import com.Minecraft_server.Narkomanka.ui.QuestMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * NPC для выдачи квестов в стиле Schedule I
 */
public class QuestGiverNPC implements Listener {

    private final Narkomanka plugin;
    private final Map<UUID, Villager> npcEntities = new HashMap<>();
    private final Map<UUID, QuestGiverType> npcTypes = new HashMap<>();
    private final NamespacedKey npcTypeKey;
    private final NamespacedKey npcIdKey;
    
    // Меню квестов
    private final QuestMenu questMenu;
    
    // Диалоги для разных типов квестодателей
    private final Map<QuestGiverType, List<String>> greetingDialogs = new HashMap<>();
    private final Map<QuestGiverType, List<String>> questOfferDialogs = new HashMap<>();
    private final Map<QuestGiverType, List<String>> questAcceptedDialogs = new HashMap<>();
    private final Map<QuestGiverType, List<String>> questCompletedDialogs = new HashMap<>();
    
    /**
     * Создает менеджера NPC для выдачи квестов
     */
    public QuestGiverNPC(Narkomanka plugin) {
        this.plugin = plugin;
        this.npcTypeKey = new NamespacedKey(plugin, "npc_type");
        this.npcIdKey = new NamespacedKey(plugin, "npc_id");
        
        // Инициализируем меню квестов
        this.questMenu = new QuestMenu(plugin);
        
        // Инициализируем диалоги
        initializeDialogs();
        
        // Регистрируем обработчик событий
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Создаем NPC при запуске сервера
        Bukkit.getScheduler().runTaskLater(plugin, this::createDefaultQuestGivers, 100L);
        
        plugin.getLogger().info("QuestGiverNPC инициализирован");
    }
    
    /**
     * Инициализирует диалоги NPC
     */
    private void initializeDialogs() {
        // Диалоги для уличного торговца
        List<String> streetDealerGreetings = Arrays.asList(
            "Эй, приятель! Ищешь способ заработать?",
            "Тс-с-с... у меня есть дело для тебя.",
            "Новичок? У меня есть работа для таких, как ты.",
            "Привет, у меня есть предложение для тебя."
        );
        
        List<String> streetDealerOffers = Arrays.asList(
            "Мне нужен человек для небольшой доставки. Заинтересован?",
            "У меня есть клиент, которому нужен товар. Хочешь подзаработать?",
            "Нужно быстро доставить посылку. Сможешь?",
            "Мне нужен надежный курьер. Платят хорошо."
        );
        
        List<String> streetDealerAccepted = Arrays.asList(
            "Отлично! Не подведи меня.",
            "Правильный выбор, друг. Деньги получишь по завершении.",
            "Только без глупостей, понял? Доставишь и свободен.",
            "Молодец. Деталями поделюсь, когда выполнишь задание."
        );
        
        List<String> streetDealerCompleted = Arrays.asList(
            "Неплохо для новичка. Может, есть что-то еще для тебя.",
            "Ты не так плох, как я думал. Вот твоя оплата.",
            "Хорошая работа. Вот деньги, как договаривались.",
            "Быстро справился. Молодец, таких людей ценю."
        );
        
        greetingDialogs.put(QuestGiverType.STREET_DEALER, streetDealerGreetings);
        questOfferDialogs.put(QuestGiverType.STREET_DEALER, streetDealerOffers);
        questAcceptedDialogs.put(QuestGiverType.STREET_DEALER, streetDealerAccepted);
        questCompletedDialogs.put(QuestGiverType.STREET_DEALER, streetDealerCompleted);
        
        // Диалоги для бизнесмена
        List<String> businessmanGreetings = Arrays.asList(
            "Добрый день. Ищу компетентного человека для деликатного поручения.",
            "Здравствуйте. Вы выглядите как человек, который ценит конфиденциальность.",
            "Приветствую. У меня есть высокооплачиваемая работа для дискретного человека.",
            "Рад видеть новое лицо. Мне нужен человек для специального задания."
        );
        
        List<String> businessmanOffers = Arrays.asList(
            "Мне требуется доставить ценный груз важному клиенту. Оплата премиальная.",
            "У меня есть деловое предложение, требующее... особого подхода. Заинтересованы?",
            "Ищу исполнителя для задания повышенной сложности. Риск компенсируется щедрым вознаграждением.",
            "Мне нужен надежный человек для конфиденциальной миссии. Оплата соответствующая."
        );
        
        List<String> businessmanAccepted = Arrays.asList(
            "Прекрасно. Я ожидаю полной конфиденциальности и эффективности.",
            "Отлично. Помните, дискретность - наш основной приоритет.",
            "Разумное решение. Детали миссии у вас. Жду результата.",
            "Хороший выбор. Работайте чисто, и будет достойное вознаграждение."
        );
        
        List<String> businessmanCompleted = Arrays.asList(
            "Работа выполнена удовлетворительно. Вот ваш гонорар.",
            "Миссия завершена успешно. Ваша оплата, как договаривались.",
            "Вы оправдали мои ожидания. Вознаграждение переведено.",
            "Превосходно. Ваше вознаграждение готово. У меня будут и другие предложения."
        );
        
        greetingDialogs.put(QuestGiverType.BUSINESSMAN, businessmanGreetings);
        questOfferDialogs.put(QuestGiverType.BUSINESSMAN, businessmanOffers);
        questAcceptedDialogs.put(QuestGiverType.BUSINESSMAN, businessmanAccepted);
        questCompletedDialogs.put(QuestGiverType.BUSINESSMAN, businessmanCompleted);
        
        // Диалоги для наркомана
        List<String> junkieGreetings = Arrays.asList(
            "Э-э-эй, друг... у тебя не найдется что-нибудь...",
            "Привет, чувак... мне нужна... помощь...",
            "О, эй!.. Мне очень нужно... ты понимаешь...",
            "Слушай, мне плохо... ты можешь помочь?"
        );
        
        List<String> junkieOffers = Arrays.asList(
            "Мне очень нужна доза... достань для меня, а? Заплачу...",
            "Чувак, я в отчаянии... принеси мне что-нибудь, я в долгу не останусь...",
            "Я сделаю что угодно... просто достань мне дозу, окей?",
            "Ты же знаешь места... принеси мне товар, и я тебя отблагодарю..."
        );
        
        List<String> junkieAccepted = Arrays.asList(
            "О, спасибо, спасибо! Я буду ждать...",
            "Ты спасаешь меня, друг... жду не дождусь...",
            "Скорее бы... я прямо здесь буду...",
            "Ох, как я тебе благодарен... поторопись..."
        );
        
        List<String> junkieCompleted = Arrays.asList(
            "Да-а-а... вот это то, что надо... держи деньги...",
            "О-о-о... наконец-то... спасибо, вот, возьми...",
            "Вау... я уже чувствую это... ты лучший... вот твоя награда...",
            "Спаситель... мой спаситель... вот, это тебе..."
        );
        
        greetingDialogs.put(QuestGiverType.JUNKIE, junkieGreetings);
        questOfferDialogs.put(QuestGiverType.JUNKIE, junkieOffers);
        questAcceptedDialogs.put(QuestGiverType.JUNKIE, junkieAccepted);
        questCompletedDialogs.put(QuestGiverType.JUNKIE, junkieCompleted);
    }
    
    /**
     * Создает стандартных NPC для выдачи квестов
     */
    private void createDefaultQuestGivers() {
        // Проверяем, есть ли уже созданные NPC
        if (!npcEntities.isEmpty()) {
            return;
        }
        
        // Получаем мир по умолчанию
        if (Bukkit.getWorlds().isEmpty()) {
            plugin.getLogger().warning("Нет доступных миров для создания NPC");
            return;
        }
        
        org.bukkit.World defaultWorld = Bukkit.getWorlds().get(0);
        
        // Создаем уличного торговца возле спавна
        Location spawnLocation = defaultWorld.getSpawnLocation();
        spawnQuestGiver(spawnLocation.clone().add(5, 0, 5), QuestGiverType.STREET_DEALER);
        
        // Создаем бизнесмена в рандомной локации
        double x = ThreadLocalRandom.current().nextDouble(-50, 50);
        double z = ThreadLocalRandom.current().nextDouble(-50, 50);
        Location businessLocation = spawnLocation.clone().add(x, 0, z);
        businessLocation.setY(defaultWorld.getHighestBlockYAt((int)x, (int)z) + 1);
        spawnQuestGiver(businessLocation, QuestGiverType.BUSINESSMAN);
        
        // Создаем наркомана в "плохом" районе
        x = ThreadLocalRandom.current().nextDouble(-100, 100);
        z = ThreadLocalRandom.current().nextDouble(-100, 100);
        Location junkieLocation = spawnLocation.clone().add(x, 0, z);
        junkieLocation.setY(defaultWorld.getHighestBlockYAt((int)x, (int)z) + 1);
        spawnQuestGiver(junkieLocation, QuestGiverType.JUNKIE);
        
        plugin.getLogger().info("Созданы стандартные NPC для выдачи квестов");
    }
    
    /**
     * Создает NPC для выдачи квестов
     */
    public UUID spawnQuestGiver(Location location, QuestGiverType type) {
        Villager npc = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        UUID npcId = UUID.randomUUID();
        
        // Настраиваем внешний вид в зависимости от типа
        switch (type) {
            case STREET_DEALER:
                npc.setCustomName(ChatColor.YELLOW + "Уличный торговец");
                npc.setProfession(Villager.Profession.NITWIT);
                break;
            case BUSINESSMAN:
                npc.setCustomName(ChatColor.GOLD + "Бизнесмен");
                npc.setProfession(Villager.Profession.LIBRARIAN);
                break;
            case JUNKIE:
                npc.setCustomName(ChatColor.DARK_PURPLE + "Наркоман");
                npc.setProfession(Villager.Profession.NONE);
                break;
        }
        
        npc.setCustomNameVisible(true);
        npc.setAdult();
        npc.setInvulnerable(true);
        
        // Добавляем метаданные
        npc.getPersistentDataContainer().set(npcTypeKey, PersistentDataType.STRING, "quest_giver");
        npc.getPersistentDataContainer().set(npcIdKey, PersistentDataType.STRING, npcId.toString());
        
        // Сохраняем NPC
        npcEntities.put(npcId, npc);
        npcTypes.put(npcId, type);
        
        plugin.getLogger().info("Создан NPC для выдачи квестов: " + type + " на " + 
                location.getWorld().getName() + " " + location.getX() + " " + location.getY() + " " + location.getZ());
        
        return npcId;
    }
    
    /**
     * Обработчик взаимодействия с NPC
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // Проверяем, является ли сущность жителем
        if (!(event.getRightClicked() instanceof Villager)) {
            return;
        }
        
        Villager villager = (Villager) event.getRightClicked();
        
        // Проверяем, является ли житель NPC для выдачи квестов
        if (!villager.getPersistentDataContainer().has(npcTypeKey, PersistentDataType.STRING)) {
            return;
        }
        
        String npcType = villager.getPersistentDataContainer().get(npcTypeKey, PersistentDataType.STRING);
        if (!"quest_giver".equals(npcType)) {
            return;
        }
        
        // Отменяем стандартное взаимодействие
        event.setCancelled(true);
        
        // Получаем ID NPC
        String npcIdStr = villager.getPersistentDataContainer().get(npcIdKey, PersistentDataType.STRING);
        if (npcIdStr == null) {
            return;
        }
        
        try {
            UUID npcId = UUID.fromString(npcIdStr);
            // Обрабатываем взаимодействие
            handleQuestGiverInteraction(event.getPlayer(), npcId);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Некорректный UUID NPC: " + npcIdStr);
        }
    }
    
    /**
     * Обрабатывает взаимодействие с NPC для выдачи квестов
     */
    private void handleQuestGiverInteraction(Player player, UUID npcId) {
        QuestGiverType npcType = npcTypes.get(npcId);
        if (npcType == null) {
            player.sendMessage(ChatColor.RED + "Ошибка: NPC не найден");
            return;
        }
        
        // Проверяем активные квесты игрока
        UUID playerUuid = player.getUniqueId();
        List<QuestProgress> activeQuests = plugin.getQuestService().getActiveQuests(playerUuid);
        List<QuestProgress> completedQuests = plugin.getQuestService().getCompletedQuests(playerUuid);
        
        // Находим завершенные квесты, которые еще не были сданы
        List<QuestProgress> questsToComplete = new ArrayList<>();
        for (QuestProgress progress : activeQuests) {
            QuestData quest = plugin.getQuestService().getQuestById(progress.getQuestId().intValue());
            if (quest != null && progress.getCurrentAmount() >= quest.getTargetAmount()) {
                questsToComplete.add(progress);
            }
        }
        
        // Если есть квесты для завершения, предлагаем их завершить
        if (!questsToComplete.isEmpty()) {
            completeQuests(player, npcType, questsToComplete);
            return;
        }
        
        // Предлагаем диалог в зависимости от типа NPC
        String greeting = getRandomDialog(npcType, greetingDialogs);
        String offer = getRandomDialog(npcType, questOfferDialogs);
        
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "=== " + getNpcTypeName(npcType) + " ===");
        player.sendMessage(ChatColor.YELLOW + greeting);
        player.sendMessage(ChatColor.YELLOW + offer);
        player.sendMessage("");
        
        // Воспроизводим звук
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f);
        
        // Открываем меню квестов
        questMenu.openMenu(player, npcType);
    }
    
    /**
     * Завершает квесты игрока
     */
    private void completeQuests(Player player, QuestGiverType npcType, List<QuestProgress> questsToComplete) {
        String completionDialog = getRandomDialog(npcType, questCompletedDialogs);
        
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "=== " + getNpcTypeName(npcType) + " ===");
        player.sendMessage(ChatColor.YELLOW + completionDialog);
        player.sendMessage("");
        
        // Завершаем каждый квест
        for (QuestProgress progress : questsToComplete) {
            QuestData quest = plugin.getQuestService().getQuestById(progress.getQuestId().intValue());
            if (quest != null) {
                // Выдаем награду
                double cashReward = quest.getRewardCash();
                double cardReward = quest.getRewardCardMoney();
                
                plugin.getPlayerService().addCash(player.getUniqueId(), cashReward);
                plugin.getPlayerService().addCardBalance(player.getUniqueId(), cardReward);
                
                // Отмечаем квест как завершенный
                plugin.getQuestService().completeQuest(player.getUniqueId(), progress.getQuestId());
                
                // Уведомляем игрока
                player.sendMessage(ChatColor.GREEN + "Квест завершен: " + quest.getTitle());
                player.sendMessage(ChatColor.GREEN + "Награда: " + 
                        ChatColor.GOLD + "$" + cashReward + ChatColor.GREEN + " наличными, " + 
                        ChatColor.GOLD + "$" + cardReward + ChatColor.GREEN + " на карту");
            }
        }
        
        // Воспроизводим звук завершения
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }
    
    /**
     * Возвращает случайный диалог для типа NPC
     */
    private String getRandomDialog(QuestGiverType type, Map<QuestGiverType, List<String>> dialogMap) {
        List<String> dialogs = dialogMap.get(type);
        if (dialogs == null || dialogs.isEmpty()) {
            return "...";
        }
        
        return dialogs.get(ThreadLocalRandom.current().nextInt(dialogs.size()));
    }
    
    /**
     * Возвращает отображаемое имя типа NPC
     */
    private String getNpcTypeName(QuestGiverType type) {
        switch (type) {
            case STREET_DEALER:
                return "Уличный торговец";
            case BUSINESSMAN:
                return "Бизнесмен";
            case JUNKIE:
                return "Наркоман";
            default:
                return "Неизвестный";
        }
    }
    
    /**
     * Типы NPC для выдачи квестов
     */
    public enum QuestGiverType {
        STREET_DEALER,  // Уличный торговец
        BUSINESSMAN,    // Бизнесмен
        JUNKIE          // Наркоман
    }
} 