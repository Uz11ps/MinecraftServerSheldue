package com.Minecraft_server.Narkomanka;

import com.Minecraft_server.Narkomanka.commands.*;
import com.Minecraft_server.Narkomanka.database.DatabaseManager;
import com.Minecraft_server.Narkomanka.database.QuestData;
import com.Minecraft_server.Narkomanka.listeners.PlayerJoinListener;
import com.Minecraft_server.Narkomanka.listeners.QuestProgressListener;
import com.Minecraft_server.Narkomanka.listeners.TrashQuestListener;
import com.Minecraft_server.Narkomanka.listeners.GrowBoxListener;
import com.Minecraft_server.Narkomanka.listeners.GrowQuestListener;
import com.Minecraft_server.Narkomanka.listeners.NPCInteractionListener;
import com.Minecraft_server.Narkomanka.npc.DrugDealerNPC;
import com.Minecraft_server.Narkomanka.npc.NPCManager;
import com.Minecraft_server.Narkomanka.npc.SuppliesVendorNPC;
import com.Minecraft_server.Narkomanka.services.PlayerService;
import com.Minecraft_server.Narkomanka.services.QuestService;
import com.Minecraft_server.Narkomanka.trash.TrashCollector;
import com.Minecraft_server.Narkomanka.trash.TrashListener;
import com.Minecraft_server.Narkomanka.trash.TrashManager;
import com.Minecraft_server.Narkomanka.trash.TrashStation;
import com.Minecraft_server.Narkomanka.trash.TrashContainer;
import com.Minecraft_server.Narkomanka.ui.UIManager;
import com.Minecraft_server.Narkomanka.util.LoggerAdapter;
import com.Minecraft_server.Narkomanka.world.DayNightCycleManager;
import com.Minecraft_server.Narkomanka.world.GrowSystem;
import com.Minecraft_server.Narkomanka.npc.PhoneBoothNPC;
import com.Minecraft_server.Narkomanka.commands.PhoneCommand;
import com.Minecraft_server.Narkomanka.items.PhoneItem;
import com.Minecraft_server.Narkomanka.npc.QuestGiverNPC;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Level;

import javax.imageio.ImageIO;

/**
 * Главный класс плагина Narkomanka
 * 
 * @author Minecraft_server
 */
public class Narkomanka extends JavaPlugin implements Listener {
    private DatabaseManager databaseManager;
    private PlayerService playerService;
    private QuestService questService;
    private UIManager uiManager;
    private boolean databaseAvailable = false;

    // Schedule I features
    private DrugDealerNPC drugDealerNPC;
    private SuppliesVendorNPC suppliesVendorNPC;
    private DayNightCycleManager dayNightCycleManager;
    private NPCManager npcManager;
    private QuestGiverNPC questGiverNPC;

    // Trash system
    private TrashManager trashManager;
    private TrashCollector trashCollector;
    private TrashStation trashStation;
    private TrashQuestListener trashQuestListener;
    private TrashContainer trashContainer;

    // Phone system
    private PhoneBoothNPC phoneBoothNPC;
    private PhoneItem phoneItem;
    
    // Grow system
    private GrowSystem growSystem;
    private GrowQuestListener growQuestListener;

    // Resource pack file
    private File resourcePackFile;

    @Override
    public void onEnable() {
        try {
            // Initialize logging first
            LoggerAdapter.initializeLogging();

            // Save default configuration
            saveDefaultConfig();
            getLogger().info("Configuration loaded.");
            
            // Создаем ресурспак и регистрируем обработчик
            resourcePackFile = generateResourcePack();
            new com.Minecraft_server.Narkomanka.events.ResourcePackListener(this, resourcePackFile);

            // Initialize database connection
            getLogger().info("Initializing database connection...");
            databaseManager = new DatabaseManager(this);

            // Check if database is available - but continue even if it's not
            databaseAvailable = databaseManager.isAvailable();
            if (!databaseAvailable) {
                getLogger().warning("Database is not available. Some functionality will be limited.");
                // We'll continue without a database, but some features won't work
            } else {
                // Log which database we're using
                if (databaseManager.isUsingPostgreSQL()) {
                    getLogger().info("Using PostgreSQL database");
                } else if (databaseManager.isUsingH2()) {
                    getLogger().info("Using H2 embedded database");
                }
            }

            // Initialize services
            playerService = new PlayerService(this);
            questService = new QuestService(this);

            // Load default quests if database is available
            if (databaseAvailable) {
                getLogger().info("Проверка наличия квестов...");
                List<QuestData> quests = questService.getAllQuests();

                if (quests.isEmpty()) {
                    getLogger().info("Квесты не найдены, создаем базовые квесты...");
                    // Сначала пробуем через Hibernate
                    initializeDefaultQuests();

                    // Проверяем, созданы ли квесты
                    quests = questService.getAllQuests();

                    // Если не созданы, пробуем через SQL
                    if (quests.isEmpty()) {
                        getLogger().info("Квесты не созданы через Hibernate, пробуем SQL метод...");
                        questService.createDefaultQuestsWithSQL();

                        // Проверяем еще раз
                        quests = questService.getAllQuests();
                        getLogger().info("После SQL создания найдено квестов: " + quests.size());
                    }
                } else {
                    getLogger().info("Найдено существующих квестов: " + quests.size());
                    for (QuestData quest : quests) {
                        getLogger().info("  - Квест #" + quest.getId() + ": " + quest.getTitle());
                    }
                }
            }

            // Initialize UI manager
            uiManager = new UIManager(this);

            // Initialize Schedule I features
            initializeScheduleFeatures();

            // Initialize trash system
            initializeTrashSystem();
            
            // Initialize grow system
            initializeGrowSystem();

            // Register commands
            registerCommands();

            // Register event listeners
            registerEventListeners();

            // Register event handlers
            registerEventHandlers();

            // Log plugin start
            getLogger().info("Narkomanka successfully enabled!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin initialization", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Cleanup UI tasks
        if (uiManager != null) {
            getServer().getOnlinePlayers().forEach(player -> uiManager.removePlayerHUD(player));
        }

        // Останавливаем задачи менеджера мусора
        if (trashManager != null) {
            trashManager.stopTasks();
        }

        // Останавливаем задачи менеджера растений
        if (growSystem != null) {
            // Никаких специальных задач для остановки не требуется
        }

        // Close database connections
        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        // Log plugin shutdown
        getLogger().info("Narkomanka has been disabled!");
    }

    /**
     * Инициализирует систему мусора
     */
    private void initializeTrashSystem() {
        try {
            getLogger().info("Initializing trash management system...");

            // Создаем менеджер мусора
            trashManager = new TrashManager(this);

            // Создаем инструмент для сбора мусора
            trashCollector = new TrashCollector(this);

            // Создаем станции переработки мусора
            trashStation = new TrashStation(this);
            
            // Создаем контейнер для мусора
            trashContainer = new TrashContainer(this);
            
            // Регистрируем модель для контейнера мусора
            registerTrashContainerModel();

            // Создаем отслеживатель квестов на сбор мусора
            trashQuestListener = new TrashQuestListener(this);

            getLogger().info("Trash management system initialized successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error initializing trash system: " + e.getMessage(), e);
        }
    }

    /**
     * Регистрирует модель для контейнера мусора
     */
    private void registerTrashContainerModel() {
        try {
            // Логируем информацию
            getLogger().info("Регистрируем модель контейнера мусора...");
            
            // Создаем директории для ресурспака
            File resourcePackDir = new File(getDataFolder().getParentFile().getParentFile(), "resource_pack");
            File modelsDir = new File(resourcePackDir, "assets/minecraft/models/item");
            File texturesDir = new File(resourcePackDir, "assets/minecraft/textures/item");
            
            if (!modelsDir.exists()) modelsDir.mkdirs();
            if (!texturesDir.exists()) texturesDir.mkdirs();
            
            // Создаем содержимое файла модели предмета
            File modelFile = new File(modelsDir, "chest_сжигатель_коли.json");
            String modelContent = "{\n" +
                    "    \"parent\": \"minecraft:item/generated\",\n" +
                    "    \"textures\": {\n" +
                    "        \"layer0\": \"minecraft:item/сжигатель_коли\"\n" +
                    "    }\n" +
                    "}";
            
            Files.write(modelFile.toPath(), modelContent.getBytes(StandardCharsets.UTF_8));
            getLogger().info("Создан файл модели: " + modelFile.getAbsolutePath());
            
            // Копируем текстуру напрямую из ресурсов плагина
            InputStream textureInput = getClass().getResourceAsStream("/com/Minecraft_server/Narkomanka/textyre/СжигательКоли.png");
            if (textureInput != null) {
                File textureFile = new File(texturesDir, "сжигатель_коли.png");
                Files.copy(textureInput, textureFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                textureInput.close();
                getLogger().info("Текстура скопирована: " + textureFile.getAbsolutePath());
            } else {
                getLogger().warning("Не удалось найти текстуру в ресурсах плагина");
                // Создаем минимальную текстуру, если не найдена в ресурсах
                createFallbackTexture(new File(texturesDir, "сжигатель_коли.png"));
            }
            
            // Создаем или обновляем файл chest.json для переопределения модели
            File chestModelFile = new File(modelsDir, "chest.json");
            String chestModelContent = "{\n" +
                    "    \"parent\": \"minecraft:item/generated\",\n" +
                    "    \"textures\": {\n" +
                    "        \"layer0\": \"minecraft:item/chest\"\n" +
                    "    },\n" +
                    "    \"overrides\": [\n" +
                    "        {\"predicate\": {\"custom_model_data\": 1}, \"model\": \"minecraft:item/chest_сжигатель_коли\"}\n" +
                    "    ]\n" +
                    "}";
            
            Files.write(chestModelFile.toPath(), chestModelContent.getBytes(StandardCharsets.UTF_8));
            getLogger().info("Создан файл переопределения модели сундука: " + chestModelFile.getAbsolutePath());
            
            // Генерируем pack.mcmeta
            File packMcmeta = new File(resourcePackDir, "pack.mcmeta");
            String packMcmetaContent = "{\n" +
                    "  \"pack\": {\n" +
                    "    \"pack_format\": 12,\n" +
                    "    \"description\": \"Narkomanka Resource Pack\"\n" +
                    "  }\n" +
                    "}";
            
            Files.write(packMcmeta.toPath(), packMcmetaContent.getBytes(StandardCharsets.UTF_8));
            getLogger().info("Создан файл pack.mcmeta: " + packMcmeta.getAbsolutePath());
            
            // Уведомляем игроков о необходимости установки ресурспака
            final File finalResourcePackDir = resourcePackDir;
            getServer().getScheduler().runTask(this, () -> {
                getServer().getOnlinePlayers().forEach(player -> {
                    player.sendMessage(net.kyori.adventure.text.Component.text("Для отображения текстур добавлен ресурспак. ")
                            .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                            .append(net.kyori.adventure.text.Component.text("Путь: " + finalResourcePackDir.getAbsolutePath())
                            .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)));
                });
            });
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Ошибка при регистрации модели контейнера мусора: " + e.getMessage(), e);
        }
    }

    /**
     * Создает запасную текстуру, если не удалось найти оригинальную
     */
    private void createFallbackTexture(File textureFile) {
        try {
            // Создаем простое изображение 16x16 пикселей красного цвета
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            g2d.setColor(new Color(170, 0, 0)); // Темно-красный
            g2d.fillRect(0, 0, 16, 16);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(2, 2, 12, 12);
            g2d.drawString("СК", 4, 12); // Сжигатель Коли
            g2d.dispose();
            
            ImageIO.write(img, "PNG", textureFile);
            getLogger().info("Создана запасная текстура: " + textureFile.getAbsolutePath());
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Ошибка при создании запасной текстуры: " + e.getMessage(), e);
        }
    }

    /**
     * Initialize Schedule I features
     */
    private void initializeScheduleFeatures() {
        try {
            // Инициализируем менеджер NPC
            getLogger().info("Initializing NPC Manager...");
            npcManager = new NPCManager(this);
            
            // First create NPCs
            getLogger().info("Initializing drug dealer NPC...");
            drugDealerNPC = new DrugDealerNPC(this);

            getLogger().info("Initializing supplies vendor NPC...");
            suppliesVendorNPC = new SuppliesVendorNPC(this);

            getLogger().info("Initializing phone booth system...");
            phoneBoothNPC = new PhoneBoothNPC(this);
            
            getLogger().info("Initializing phone item system...");
            phoneItem = new PhoneItem(this, phoneBoothNPC);
            
            getLogger().info("Initializing quest giver NPCs...");
            questGiverNPC = new QuestGiverNPC(this);

            // Then create day/night cycle manager
            getLogger().info("Initializing day/night cycle manager...");
            dayNightCycleManager = new DayNightCycleManager(this);

            // Connect systems
            dayNightCycleManager.setDrugDealerNPC(drugDealerNPC);
            dayNightCycleManager.setSuppliesVendorNPC(suppliesVendorNPC);

            getLogger().info("Schedule I features initialized successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error initializing Schedule I features: " + e.getMessage(), e);
        }
    }

    /**
     * Инициализирует систему выращивания растений
     */
    private void initializeGrowSystem() {
        try {
            getLogger().info("Initializing grow system...");
            
            // Создаем систему выращивания
            growSystem = new GrowSystem(this);
            
            // Регистрируем слушатель для гроубоксов
            new GrowBoxListener(this);
            
            // Создаем слушатель для квестов на выращивание
            growQuestListener = new GrowQuestListener(this);
            
            getLogger().info("Grow system initialized successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error initializing grow system: " + e.getMessage(), e);
        }
    }

    private void registerCommands() {
        // Register existing commands
        PluginCommand helloCmd = getCommand("hello");
        if (helloCmd != null) {
            helloCmd.setExecutor(new HelloCommand(this));
            getLogger().info("Command /hello successfully registered!");
        } else {
            getLogger().warning("Failed to register command /hello!");
        }

        PluginCommand balanceCmd = getCommand("balance");
        if (balanceCmd != null) {
            balanceCmd.setExecutor(new BalanceCommand(this));
            getLogger().info("Command /balance successfully registered!");
        } else {
            getLogger().warning("Failed to register command /balance!");
        }

        PluginCommand questCmd = getCommand("quest");
        if (questCmd != null) {
            questCmd.setExecutor(new QuestCommand(this));
            getLogger().info("Command /quest successfully registered!");
        } else {
            getLogger().warning("Failed to register command /quest!");
        }

        PluginCommand phoneCmd = getCommand("phone");
        if (phoneCmd != null) {
            phoneCmd.setExecutor(new PhoneCommand(this, phoneBoothNPC));
            phoneCmd.setTabCompleter(new PhoneCommand(this, phoneBoothNPC));
            getLogger().info("Command /phone successfully registered!");
        } else {
            getLogger().warning("Failed to register command /phone!");
        }
        
        // Регистрируем команду для управления NPC
        PluginCommand npcCmd = getCommand("npc");
        if (npcCmd != null) {
            NPCCommand npcCommand = new NPCCommand(this);
            npcCmd.setExecutor(npcCommand);
            npcCmd.setTabCompleter(npcCommand);
            getLogger().info("Command /npc successfully registered!");
        } else {
            getLogger().warning("Failed to register command /npc!");
        }

        // Register NPC commands
        if (drugDealerNPC != null && suppliesVendorNPC != null) {
            NPCCommands npcCommands = new NPCCommands(this, drugDealerNPC, suppliesVendorNPC);

            PluginCommand spawnDealerCmd = getCommand("spawn_dealer");
            if (spawnDealerCmd != null) {
                spawnDealerCmd.setExecutor(npcCommands);
                getLogger().info("Command /spawn_dealer successfully registered!");
            } else {
                getLogger().warning("Failed to register command /spawn_dealer!");
            }

            PluginCommand spawnVendorCmd = getCommand("spawn_vendor");
            if (spawnVendorCmd != null) {
                spawnVendorCmd.setExecutor(npcCommands);
                getLogger().info("Command /spawn_vendor successfully registered!");
            } else {
                getLogger().warning("Failed to register command /spawn_vendor!");
            }

            PluginCommand buyDrugCmd = getCommand("buy_drug");
            if (buyDrugCmd != null) {
                buyDrugCmd.setExecutor(npcCommands);
                buyDrugCmd.setTabCompleter(npcCommands);
                getLogger().info("Command /buy_drug successfully registered!");
            } else {
                getLogger().warning("Failed to register command /buy_drug!");
            }

            PluginCommand buySupplyCmd = getCommand("buy_supply");
            if (buySupplyCmd != null) {
                buySupplyCmd.setExecutor(npcCommands);
                buySupplyCmd.setTabCompleter(npcCommands);
                getLogger().info("Command /buy_supply successfully registered!");
            } else {
                getLogger().warning("Failed to register command /buy_supply!");
            }
        }

        // Register trash commands
        TrashCommands trashCommands = new TrashCommands(this);

        PluginCommand trashCmd = getCommand("trash");
        if (trashCmd != null) {
            trashCmd.setExecutor(trashCommands);
            trashCmd.setTabCompleter(trashCommands);
            getLogger().info("Command /trash successfully registered!");
        } else {
            getLogger().warning("Failed to register command /trash!");
        }

        PluginCommand trashStationCmd = getCommand("trashstation");
        if (trashStationCmd != null) {
            trashStationCmd.setExecutor(trashCommands);
            trashStationCmd.setTabCompleter(trashCommands);
            getLogger().info("Command /trashstation successfully registered!");
        } else {
            getLogger().warning("Failed to register command /trashstation!");
        }

        // Register grow commands
        PluginCommand growCmd = getCommand("grow");
        if (growCmd != null) {
            growCmd.setExecutor(new GrowCommands(this, growSystem));
            growCmd.setTabCompleter(new GrowCommands(this, growSystem));
            getLogger().info("Command /grow successfully registered!");
        } else {
            getLogger().warning("Failed to register command /grow!");
        }
    }

    private void registerEventListeners() {
        // Register existing listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new QuestProgressListener(this), this);

        // NPCs and day/night manager are already self-registering in their constructors

        // Register trash listener
        getServer().getPluginManager().registerEvents(new TrashListener(this), this);

        // Register NPC interaction listener
        if (npcManager != null) {
            new NPCInteractionListener(this);
            getLogger().info("NPC interaction listener registered.");
        }

        // TrashQuestListener уже регистрируется в своем конструкторе
    }

    private void initializeDefaultQuests() {
        if (getConfig().getBoolean("quests.initialize-default-quests", true)) {
            try {
                // Принудительно создаём квесты, даже если уже есть в базе
                getLogger().info("Инициализация квестов...");

                // Создаем квесты
                if (getConfig().isConfigurationSection("quests.default-quests")) {
                    getLogger().info("Создание квестов из конфигурации...");
                    getConfig().getConfigurationSection("quests.default-quests").getKeys(false).forEach(key -> {
                        String path = "quests.default-quests." + key;
                        QuestData quest = new QuestData();

                        quest.setTitle(getConfig().getString(path + ".title", "Unnamed Quest"));
                        quest.setDescription(getConfig().getString(path + ".description", "No description"));
                        quest.setQuestType(getConfig().getString(path + ".quest-type", "BREAK_BLOCK"));
                        quest.setTargetItem(getConfig().getString(path + ".target-item", "STONE"));
                        quest.setTargetAmount(getConfig().getInt(path + ".target-amount", 1));
                        quest.setRewardCash(getConfig().getDouble(path + ".reward-cash", 0.0));
                        quest.setRewardCardMoney(getConfig().getDouble(path + ".reward-card-money", 0.0));
                        quest.setRepeatable(getConfig().getBoolean(path + ".repeatable", false));

                        questService.saveQuest(quest);
                        getLogger().info("Создан квест: " + quest.getTitle());
                    });
                } else {
                    // Create some basic quests if no config section exists
                    getLogger().info("Создание базовых квестов...");

                    QuestData stoneQuest = new QuestData();
                    stoneQuest.setTitle("Камнетёс");
                    stoneQuest.setDescription("Добудьте 10 камней");
                    stoneQuest.setQuestType("BREAK_BLOCK");
                    stoneQuest.setTargetItem("STONE");
                    stoneQuest.setTargetAmount(10);
                    stoneQuest.setRewardCash(50.0);
                    stoneQuest.setRepeatable(true);
                    questService.saveQuest(stoneQuest);
                    getLogger().info("Создан квест: Камнетёс");

                    QuestData logQuest = new QuestData();
                    logQuest.setTitle("Лесоруб");
                    logQuest.setDescription("Добудьте 20 древесины");
                    logQuest.setQuestType("BREAK_BLOCK");
                    logQuest.setTargetItem("OAK_LOG");
                    logQuest.setTargetAmount(20);
                    logQuest.setRewardCash(75.0);
                    logQuest.setRepeatable(true);
                    questService.saveQuest(logQuest);
                    getLogger().info("Создан квест: Лесоруб");

                    QuestData zombieQuest = new QuestData();
                    zombieQuest.setTitle("Охотник на зомби");
                    zombieQuest.setDescription("Убейте 5 зомби");
                    zombieQuest.setQuestType("KILL_ENTITY");
                    zombieQuest.setTargetItem("ZOMBIE");
                    zombieQuest.setTargetAmount(5);
                    zombieQuest.setRewardCash(100.0);
                    zombieQuest.setRewardCardMoney(50.0);
                    zombieQuest.setRepeatable(true);
                    questService.saveQuest(zombieQuest);
                    getLogger().info("Создан квест: Охотник на зомби");

                    // Новые квесты, связанные с наркотиками
                    QuestData drugQuest = new QuestData();
                    drugQuest.setTitle("Начинающий дилер");
                    drugQuest.setDescription("Купите товар у дилера");
                    drugQuest.setQuestType("BUY_DRUG");
                    drugQuest.setTargetItem("ANY");
                    drugQuest.setTargetAmount(1);
                    drugQuest.setRewardCash(100.0);
                    drugQuest.setRewardCardMoney(0.0);
                    drugQuest.setRepeatable(false);
                    questService.saveQuest(drugQuest);
                    getLogger().info("Создан квест: Начинающий дилер");

                    QuestData growQuest = new QuestData();
                    growQuest.setTitle("Фермер");
                    growQuest.setDescription("Приобретите оборудование для выращивания");
                    growQuest.setQuestType("BUY_SUPPLY");
                    growQuest.setTargetItem("growbox");
                    growQuest.setTargetAmount(1);
                    growQuest.setRewardCash(300.0);
                    growQuest.setRewardCardMoney(100.0);
                    growQuest.setRepeatable(false);
                    questService.saveQuest(growQuest);
                    getLogger().info("Создан квест: Фермер");
                    
                    // Квест на выращивание растений
                    QuestData plantingQuest = new QuestData();
                    plantingQuest.setTitle("Начинающий агроном");
                    plantingQuest.setDescription("Вырастите и соберите 10 растений марихуаны");
                    plantingQuest.setQuestType("GROW_PLANT");
                    plantingQuest.setTargetItem("marijuana");
                    plantingQuest.setTargetAmount(10);
                    plantingQuest.setRewardCash(500.0);
                    plantingQuest.setRewardCardMoney(200.0);
                    plantingQuest.setRepeatable(true);
                    questService.saveQuest(plantingQuest);
                    getLogger().info("Создан квест: Начинающий агроном");
                    
                    // Квест на выращивание растений высокого качества
                    QuestData qualityQuest = new QuestData();
                    qualityQuest.setTitle("Селекционер");
                    qualityQuest.setDescription("Вырастите 5 растений марихуаны высокого качества (3+)");
                    qualityQuest.setQuestType("GROW_PLANT_QUALITY");
                    qualityQuest.setTargetItem("marijuana");
                    qualityQuest.setTargetAmount(5);
                    qualityQuest.setRewardCash(800.0);
                    qualityQuest.setRewardCardMoney(300.0);
                    qualityQuest.setRepeatable(true);
                    questService.saveQuest(qualityQuest);
                    getLogger().info("Создан квест: Селекционер");
                    
                    // Квест на продажу наркотиков                    
                    QuestData sellQuest = new QuestData();
                    sellQuest.setTitle("Мелкий сбытчик");
                    sellQuest.setDescription("Продайте через телефонную будку 20 единиц любого наркотика");
                    sellQuest.setQuestType("SELL_DRUG");
                    sellQuest.setTargetItem("ANY");
                    sellQuest.setTargetAmount(20);
                    sellQuest.setRewardCash(600.0);
                    sellQuest.setRewardCardMoney(400.0);
                    sellQuest.setRepeatable(true);
                    questService.saveQuest(sellQuest);
                    getLogger().info("Создан квест: Мелкий сбытчик");
                    
                    // Добавляем квест на сбор мусора
                    QuestData trashQuest = new QuestData();
                    trashQuest.setTitle("Сборщик мусора");
                    trashQuest.setDescription("Соберите и сдайте 10 единиц мусора на станцию переработки");
                    trashQuest.setQuestType("TRASH_COLLECT");
                    trashQuest.setTargetItem("ANY");
                    trashQuest.setTargetAmount(10);
                    trashQuest.setRewardCash(150.0);
                    trashQuest.setRewardCardMoney(0.0);
                    trashQuest.setRepeatable(true);
                    questService.saveQuest(trashQuest);
                    getLogger().info("Создан квест: Сборщик мусора");

                    getLogger().info("Базовые квесты созданы успешно!");
                }

                // Проверяем, что квесты созданы
                List<QuestData> quests = questService.getAllQuests();
                getLogger().info("После инициализации найдено квестов: " + quests.size());
                for (QuestData quest : quests) {
                    getLogger().info("Квест #" + quest.getId() + ": " + quest.getTitle());
                }

            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Ошибка при инициализации квестов: " + e.getMessage(), e);
                e.printStackTrace();
            }
        }
    }

    // Getters for managers and services
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerService getPlayerService() {
        return playerService;
    }

    public QuestService getQuestService() {
        return questService;
    }

    public UIManager getUiManager() {
        return uiManager;
    }

    public DrugDealerNPC getDrugDealerNPC() {
        return drugDealerNPC;
    }

    public SuppliesVendorNPC getSuppliesVendorNPC() {
        return suppliesVendorNPC;
    }

    public DayNightCycleManager getDayNightCycleManager() {
        return dayNightCycleManager;
    }

    public TrashManager getTrashManager() {
        return trashManager;
    }

    public TrashCollector getTrashCollector() {
        return trashCollector;
    }

    public TrashStation getTrashStation() {
        return trashStation;
    }

    public TrashQuestListener getTrashQuestListener() {
        return trashQuestListener;
    }

    public boolean isDatabaseAvailable() {
        return databaseAvailable;
    }

    public boolean isUsingH2() {
        return databaseManager != null && databaseManager.isUsingH2();
    }

    public PhoneBoothNPC getPhoneBoothNPC() {
        return phoneBoothNPC;
    }

    public GrowSystem getGrowSystem() {
        return growSystem;
    }

    public GrowQuestListener getGrowQuestListener() {
        return growQuestListener;
    }

    public PhoneItem getPhoneItem() {
        return phoneItem;
    }

    /**
     * Получает менеджер NPC
     */
    public NPCManager getNPCManager() {
        return npcManager;
    }

    /**
     * Получает QuestGiverNPC
     */
    public QuestGiverNPC getQuestGiverNPC() {
        return questGiverNPC;
    }

    public TrashContainer getTrashContainer() {
        return trashContainer;
    }

    /**
     * Создает и возвращает файл ресурспака
     */
    private File generateResourcePack() {
        try {
            getLogger().info("Создание ресурспака для сервера...");
            
            // Создаем директории для ресурспака
            File resourcePackDir = new File(getDataFolder().getParentFile().getParentFile(), "resource_pack");
            File zipFile = new File(getDataFolder().getParentFile().getParentFile(), "narkomanka_resources.zip");
            
            // Создаем структуру ресурспака
            createResourcePackStructure(resourcePackDir);
            
            // Создаем ZIP файл из директории ресурспака
            zipDirectory(resourcePackDir, zipFile);
            
            getLogger().info("Ресурспак создан: " + zipFile.getAbsolutePath());
            // Сохраняем ссылку на файл ресурспака
            resourcePackFile = zipFile;
            return zipFile;
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Ошибка при создании ресурспака: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Создает структуру ресурспака в указанной директории
     */
    private void createResourcePackStructure(File resourcePackDir) throws IOException {
        // Создаем основные директории
        File modelsDir = new File(resourcePackDir, "assets/minecraft/models/item");
        File texturesDir = new File(resourcePackDir, "assets/minecraft/textures/item");
        
        if (!modelsDir.exists()) modelsDir.mkdirs();
        if (!texturesDir.exists()) texturesDir.mkdirs();
        
        // Создаем содержимое файла модели предмета
        File modelFile = new File(modelsDir, "chest_сжигатель_коли.json");
        String modelContent = "{\n" +
                "    \"parent\": \"minecraft:item/generated\",\n" +
                "    \"textures\": {\n" +
                "        \"layer0\": \"minecraft:item/сжигатель_коли\"\n" +
                "    }\n" +
                "}";
        
        Files.write(modelFile.toPath(), modelContent.getBytes(StandardCharsets.UTF_8));
        getLogger().info("Создан файл модели: " + modelFile.getAbsolutePath());
        
        // Копируем текстуру напрямую из ресурсов плагина
        InputStream textureInput = getClass().getResourceAsStream("/com/Minecraft_server/Narkomanka/textyre/СжигательКоли.png");
        if (textureInput != null) {
            File textureFile = new File(texturesDir, "сжигатель_коли.png");
            Files.copy(textureInput, textureFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            textureInput.close();
            getLogger().info("Текстура скопирована: " + textureFile.getAbsolutePath());
        } else {
            getLogger().warning("Не удалось найти текстуру в ресурсах плагина");
            // Создаем минимальную текстуру, если не найдена в ресурсах
            createFallbackTexture(new File(texturesDir, "сжигатель_коли.png"));
        }
        
        // Создаем или обновляем файл chest.json для переопределения модели
        File chestModelFile = new File(modelsDir, "chest.json");
        String chestModelContent = "{\n" +
                "    \"parent\": \"minecraft:item/generated\",\n" +
                "    \"textures\": {\n" +
                "        \"layer0\": \"minecraft:item/chest\"\n" +
                "    },\n" +
                "    \"overrides\": [\n" +
                "        {\"predicate\": {\"custom_model_data\": 1}, \"model\": \"minecraft:item/chest_сжигатель_коли\"}\n" +
                "    ]\n" +
                "}";
        
        Files.write(chestModelFile.toPath(), chestModelContent.getBytes(StandardCharsets.UTF_8));
        getLogger().info("Создан файл переопределения модели сундука: " + chestModelFile.getAbsolutePath());
        
        // Генерируем pack.mcmeta
        File packMcmeta = new File(resourcePackDir, "pack.mcmeta");
        String packMcmetaContent = "{\n" +
                "  \"pack\": {\n" +
                "    \"pack_format\": 12,\n" +
                "    \"description\": \"Narkomanka Resource Pack\"\n" +
                "  }\n" +
                "}";
        
        Files.write(packMcmeta.toPath(), packMcmetaContent.getBytes(StandardCharsets.UTF_8));
        getLogger().info("Создан файл pack.mcmeta: " + packMcmeta.getAbsolutePath());
    }

    /**
     * Упаковывает директорию в ZIP архив
     */
    private void zipDirectory(File directory, File zipFile) throws IOException {
        getLogger().info("Создание ZIP архива из директории: " + directory.getAbsolutePath());
        
        Path zipFilePath = zipFile.toPath();
        if (Files.exists(zipFilePath)) {
            Files.delete(zipFilePath);
        }
        
        java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipFilePath));
        Path sourcePath = directory.toPath();
        
        Files.walk(sourcePath)
            .filter(path -> !Files.isDirectory(path))
            .forEach(path -> {
                try {
                    // Создаем запись для файла в zip
                    java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(sourcePath.relativize(path).toString().replace("\\", "/"));
                    zos.putNextEntry(zipEntry);
                    
                    // Копируем содержимое файла в zip
                    Files.copy(path, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    getLogger().log(Level.WARNING, "Ошибка при добавлении файла в ZIP: " + path, e);
                }
            });
        
        zos.close();
        getLogger().info("ZIP архив создан: " + zipFile.getAbsolutePath());
    }

    /**
     * Регистрирует обработчики событий
     */
    private void registerEventHandlers() {
        // Создаем и регистрируем обработчик событий входа игрока
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                // Переемещено в ResourcePackListener
            }
        }, this);
        
        // Регистрируем остальные обработчики событий
        // ... existing code ...
    }

    /**
     * Обработчик состояния установки ресурспака
     */
    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        // Переемещено в ResourcePackListener
    }
}