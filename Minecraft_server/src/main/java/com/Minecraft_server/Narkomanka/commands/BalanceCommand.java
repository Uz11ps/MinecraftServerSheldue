package com.Minecraft_server.Narkomanka.commands;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.database.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BalanceCommand implements CommandExecutor, TabCompleter {
    private final Narkomanka plugin;
    private final DecimalFormat moneyFormat = new DecimalFormat("$#,##0.00");

    public BalanceCommand(Narkomanka plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        // Debug output
        plugin.getLogger().info("Balance command called by: " + player.getName());

        if (args.length == 0) {
            // Show balance information
            showBalance(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "transfer":
                // Transfer from cash to card
                if (args.length < 2) {
                    player.sendMessage(Component.text("Использование: /balance transfer <сумма>").color(NamedTextColor.RED));
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[1]);
                    transferToCard(player, amount);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Неверная сумма. Используйте числовое значение.").color(NamedTextColor.RED));
                }
                break;

            case "withdraw":
                // Withdraw from card to cash
                if (args.length < 2) {
                    player.sendMessage(Component.text("Использование: /balance withdraw <сумма>").color(NamedTextColor.RED));
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[1]);
                    withdrawFromCard(player, amount);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Неверная сумма. Используйте числовое значение.").color(NamedTextColor.RED));
                }
                break;

            default:
                player.sendMessage(Component.text("Неизвестная команда. Используйте /balance, /balance transfer <сумма> или /balance withdraw <сумма>").color(NamedTextColor.RED));
                break;
        }

        return true;
    }

    private void showBalance(Player player) {
        PlayerData playerData = plugin.getPlayerService().getOrCreatePlayerData(player);

        player.sendMessage(Component.text("=== Ваш Баланс ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Наличные: ")
                .color(NamedTextColor.GREEN)
                .append(Component.text(moneyFormat.format(playerData.getCashBalance()))
                        .color(NamedTextColor.GOLD)));
        player.sendMessage(Component.text("Карта: ")
                .color(NamedTextColor.AQUA)
                .append(Component.text(moneyFormat.format(playerData.getCardBalance()))
                        .color(NamedTextColor.GOLD)));
        player.sendMessage(Component.text("=================").color(NamedTextColor.GOLD));
    }

    private void transferToCard(Player player, double amount) {
        if (amount <= 0) {
            player.sendMessage(Component.text("Сумма должна быть положительной.").color(NamedTextColor.RED));
            return;
        }

        boolean success = plugin.getPlayerService().depositToCard(player.getUniqueId(), amount);

        if (success) {
            player.sendMessage(Component.text("Вы успешно перевели ")
                    .color(NamedTextColor.GREEN)
                    .append(Component.text(moneyFormat.format(amount))
                            .color(NamedTextColor.GOLD))
                    .append(Component.text(" на карту.")
                            .color(NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("У вас недостаточно наличных для перевода.").color(NamedTextColor.RED));
        }
    }

    private void withdrawFromCard(Player player, double amount) {
        if (amount <= 0) {
            player.sendMessage(Component.text("Сумма должна быть положительной.").color(NamedTextColor.RED));
            return;
        }

        boolean success = plugin.getPlayerService().withdrawFromCard(player.getUniqueId(), amount);

        if (success) {
            player.sendMessage(Component.text("Вы успешно сняли ")
                    .color(NamedTextColor.GREEN)
                    .append(Component.text(moneyFormat.format(amount))
                            .color(NamedTextColor.GOLD))
                    .append(Component.text(" с карты.")
                            .color(NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("У вас недостаточно средств на карте для снятия.").color(NamedTextColor.RED));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return filterStartingWith(args[0], Arrays.asList("transfer", "withdraw"));
        }
        return new ArrayList<>();
    }

    private List<String> filterStartingWith(String prefix, List<String> options) {
        if (prefix.isEmpty()) {
            return options;
        }

        List<String> filtered = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(prefix.toLowerCase())) {
                filtered.add(option);
            }
        }
        return filtered;
    }
}