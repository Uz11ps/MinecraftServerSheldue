package com.Minecraft_server.Narkomanka.commands;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.trash.TrashManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Команды для управления системой мусора
 */
public class TrashCommands implements CommandExecutor, TabCompleter {

    private final Narkomanka plugin;

    public TrashCommands(Narkomanka plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        // Debug output
        plugin.getLogger().info(command.getName() + " command called by: " + player.getName());

        switch (command.getName().toLowerCase()) {
            case "trash":
                return handleTrashCommand(player, args);

            case "trashstation":
                return handleTrashStationCommand(player, args);

            default:
                return false;
        }
    }

    /**
     * Обрабатывает команду /trash
     */
    private boolean handleTrashCommand(Player player, String[] args) {
        if (args.length == 0) {
            showTrashHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                showTrashHelp(player);
                break;

            case "collector":
                // Выдаем игроку мусоросборник
                if (player.hasPermission("narkomanka.trash.collector")) {
                    plugin.getTrashCollector().giveCollectorToPlayer(player);
                } else {
                    player.sendMessage(Component.text("У вас нет прав для получения мусоросборника!").color(NamedTextColor.RED));
                }
                break;

            case "container":
                // Выдаем игроку контейнер для мусора
                if (player.hasPermission("narkomanka.trash.container")) {
                    plugin.getTrashContainer().giveContainerToPlayer(player);
                    // Отправляем информацию о текстуре
                    player.sendMessage(Component.text("Для отображения текстуры Сжигателя Коли требуется ресурспак.")
                            .color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("Если текстура не отображается, выполните команду:")
                            .color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/trash container_model")
                            .color(NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("У вас нет прав для получения контейнера для мусора!").color(NamedTextColor.RED));
                }
                break;

            case "container_model":
                // Выдаем игроку контейнер напрямую с гарантированной текстурой
                if (player.hasPermission("narkomanka.trash.container")) {
                    plugin.getTrashContainer().createContainerWithModel(player);
                    player.sendMessage(Component.text("Если текстура всё еще не отображается, убедитесь что:")
                            .color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("1. Вы используете ресурспак из папки сервера")
                            .color(NamedTextColor.GRAY));
                    player.sendMessage(Component.text("2. Перезайдите в игру или перезагрузите ресурспаки (F3+T)")
                            .color(NamedTextColor.GRAY));
                } else {
                    player.sendMessage(Component.text("У вас нет прав для получения контейнера для мусора!").color(NamedTextColor.RED));
                }
                break;

            case "info":
                // Показываем информацию о мусоре
                showTrashInfo(player);
                break;

            case "reload":
                // Перезагружаем конфигурацию мусора
                if (player.hasPermission("narkomanka.admin")) {
                    plugin.getTrashManager().loadConfig();
                    player.sendMessage(Component.text("Конфигурация мусора перезагружена!").color(NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("У вас нет прав для этой команды!").color(NamedTextColor.RED));
                }
                break;

            case "clean":
                // Удаляем весь мусор с карты
                if (player.hasPermission("narkomanka.admin")) {
                    int removed = plugin.getTrashManager().removeAllTrash();
                    player.sendMessage(Component.text("Удалено " + removed + " единиц мусора!").color(NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("У вас нет прав для этой команды!").color(NamedTextColor.RED));
                }
                break;

            case "generate":
                // Принудительно генерируем мусор
                if (player.hasPermission("narkomanka.admin")) {
                    plugin.getTrashManager().generateTrash();
                    player.sendMessage(Component.text("Мусор сгенерирован в мире!").color(NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("У вас нет прав для этой команды!").color(NamedTextColor.RED));
                }
                break;

            default:
                player.sendMessage(Component.text("Неизвестная подкоманда. Используйте /trash help").color(NamedTextColor.RED));
                break;
        }

        return true;
    }

    /**
     * Обрабатывает команду /trashstation
     */
    private boolean handleTrashStationCommand(Player player, String[] args) {
        if (!player.hasPermission("narkomanka.admin")) {
            player.sendMessage(Component.text("У вас нет прав для этой команды!").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            showTrashStationHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                showTrashStationHelp(player);
                break;

            case "create":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Использование: /trashstation create <название>").color(NamedTextColor.RED));
                    return true;
                }

                String stationName = args[1];
                plugin.getTrashStation().createTrashStation(player.getLocation(), stationName);
                    player.sendMessage(Component.text("Станция переработки мусора '" + stationName + "' успешно создана!").color(NamedTextColor.GREEN));
                return true;

            case "remove":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Использование: /trashstation remove <название>").color(NamedTextColor.RED));
                    return true;
                }

                String stationToRemove = args[1];
                boolean removed = plugin.getTrashStation().removeStation(stationToRemove);

                if (removed) {
                    player.sendMessage(Component.text("Станция переработки мусора '" + stationToRemove + "' удалена!").color(NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Станция с названием '" + stationToRemove + "' не найдена!").color(NamedTextColor.RED));
                }
                break;

            case "list":
                // Показываем список всех станций
                Map<String, Location> stations = plugin.getTrashStation().getStations();

                if (stations.isEmpty()) {
                    player.sendMessage(Component.text("Станции переработки мусора не найдены!").color(NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text("==== Станции переработки мусора ====").color(NamedTextColor.GREEN));
                    for (Map.Entry<String, Location> entry : stations.entrySet()) {
                        Location loc = entry.getValue();
                        player.sendMessage(Component.text(entry.getKey() + ": ").color(NamedTextColor.AQUA)
                                .append(Component.text(
                                        loc.getWorld().getName() + " (" +
                                                (int)loc.getX() + ", " +
                                                (int)loc.getY() + ", " +
                                                (int)loc.getZ() + ")"
                                ).color(NamedTextColor.GRAY)));
                    }
                }
                break;

            default:
                player.sendMessage(Component.text("Неизвестная подкоманда. Используйте /trashstation help").color(NamedTextColor.RED));
                break;
        }

        return true;
    }

    /**
     * Показывает справку по команде /trash
     */
    private void showTrashHelp(Player player) {
        player.sendMessage(Component.text("==== Команды системы мусора ====").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("/trash collector").color(NamedTextColor.GOLD)
                .append(Component.text(" - Получить мусоросборник").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/trash container").color(NamedTextColor.GOLD)
                .append(Component.text(" - Получить контейнер для мусора").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/trash container_model").color(NamedTextColor.GOLD)
                .append(Component.text(" - Получить контейнер с текстурой").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/trash info").color(NamedTextColor.GOLD)
                .append(Component.text(" - Показать информацию о системе мусора").color(NamedTextColor.GRAY)));

        if (player.hasPermission("narkomanka.admin")) {
            player.sendMessage(Component.text("/trash reload").color(NamedTextColor.GOLD)
                    .append(Component.text(" - Перезагрузить конфигурацию мусора").color(NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/trash clean").color(NamedTextColor.GOLD)
                    .append(Component.text(" - Удалить весь мусор с карты").color(NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/trash generate").color(NamedTextColor.GOLD)
                    .append(Component.text(" - Принудительно сгенерировать мусор").color(NamedTextColor.GRAY)));
        }
    }

    /**
     * Показывает справку по команде /trashstation
     */
    private void showTrashStationHelp(Player player) {
        player.sendMessage(Component.text("==== Команды станций переработки мусора ====").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("/trashstation create <название>").color(NamedTextColor.GOLD)
                .append(Component.text(" - Создать станцию на вашей позиции").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/trashstation remove <название>").color(NamedTextColor.GOLD)
                .append(Component.text(" - Удалить станцию").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/trashstation list").color(NamedTextColor.GOLD)
                .append(Component.text(" - Показать список станций").color(NamedTextColor.GRAY)));
    }

    /**
     * Показывает информацию о системе мусора
     */
    private void showTrashInfo(Player player) {
        player.sendMessage(Component.text("==== Информация о системе мусора ====").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Активный мусор в мире: ").color(NamedTextColor.AQUA)
                .append(Component.text(plugin.getTrashManager().getActiveTrashCount()).color(NamedTextColor.GOLD)));

        player.sendMessage(Component.text("Как это работает:").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("1. Получите мусоросборник с помощью /trash collector").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("2. Используйте его (ПКМ) для сбора мусора").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("3. Отнесите собранный мусор на станцию переработки").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("4. Нажмите ПКМ по станции, чтобы сдать весь мусор").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("5. Получите деньги за сданный мусор!").color(NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("trash")) {
            if (args.length == 1) {
                List<String> options = new ArrayList<>(Arrays.asList("help", "collector", "container", "container_model", "info"));
                if (sender.hasPermission("narkomanka.admin")) {
                    options.addAll(Arrays.asList("reload", "clean", "generate"));
                }
                return filterStartingWith(args[0], options);
            }
        }
        else if (command.getName().equalsIgnoreCase("trashstation")) {
            if (!sender.hasPermission("narkomanka.admin")) {
                return completions;
            }

            if (args.length == 1) {
                return filterStartingWith(args[0], Arrays.asList("help", "create", "remove", "list"));
            }
            else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
                // Предлагаем названия существующих станций
                return filterStartingWith(args[1],
                        new ArrayList<>(plugin.getTrashStation().getStations().keySet()));
            }
        }

        return completions;
    }

    /**
     * Фильтрует список по началу строки
     */
    private List<String> filterStartingWith(String prefix, List<String> options) {
        if (prefix.isEmpty()) {
            return options;
        }

        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }}