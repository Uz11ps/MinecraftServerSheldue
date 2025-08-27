package com.Minecraft_server.Narkomanka.commands;

import com.Minecraft_server.Narkomanka.Narkomanka;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HelloCommand implements CommandExecutor {

    private final Narkomanka plugin;

    public HelloCommand(Narkomanka plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Добавим отладочный вывод
        plugin.getLogger().info("Команда /hello вызвана пользователем: " + sender.getName());

        if (sender instanceof Player player) {
            // В Paper/MC 1.20 используется Adventure API для текста
            player.sendMessage(
                    Component.text("Привет, ")
                            .color(NamedTextColor.GOLD)
                            .append(Component.text(player.getName())
                                    .color(NamedTextColor.AQUA))
                            .append(Component.text("!")
                                    .color(NamedTextColor.GOLD))
            );

            // Если указан аргумент, отправляем сообщение другому игроку
            if (args.length > 0) {
                Player target = plugin.getServer().getPlayer(args[0]);
                if (target != null) {
                    target.sendMessage(
                            Component.text("Игрок ")
                                    .color(NamedTextColor.GOLD)
                                    .append(Component.text(player.getName())
                                            .color(NamedTextColor.AQUA))
                                    .append(Component.text(" передает вам привет!")
                                            .color(NamedTextColor.GOLD))
                    );

                    player.sendMessage(
                            Component.text("Вы передали привет игроку ")
                                    .color(NamedTextColor.GOLD)
                                    .append(Component.text(target.getName())
                                            .color(NamedTextColor.AQUA))
                                    .append(Component.text("!")
                                            .color(NamedTextColor.GOLD))
                    );
                } else {
                    player.sendMessage(
                            Component.text("Игрок " + args[0] + " не найден или не в сети.")
                                    .color(NamedTextColor.RED)
                    );
                }
            }
        } else {
            sender.sendMessage("Эта команда может быть выполнена только игроком.");
        }
        return true;
    }
}