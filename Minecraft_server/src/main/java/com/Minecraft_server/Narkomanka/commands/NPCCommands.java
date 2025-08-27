package com.Minecraft_server.Narkomanka.commands;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.npc.DrugDealerNPC;
import com.Minecraft_server.Narkomanka.npc.SuppliesVendorNPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NPCCommands implements CommandExecutor, TabCompleter {

    private final Narkomanka plugin;
    private final DrugDealerNPC drugDealerNPC;
    private final SuppliesVendorNPC suppliesVendorNPC;

    // List of available drug types
    private final List<String> drugTypes = Arrays.asList("marijuana", "cocaine", "meth", "heroin");

    // List of available supply categories (for tab completion)
    private final List<String> supplyCategories = Arrays.asList(
            "growbox", "soil", "water", "fertilizer", "seeds", "baggies", "scales", 
            "phone1", "phone2", "phone3", "phone4"
    );

    public NPCCommands(Narkomanka plugin, DrugDealerNPC drugDealerNPC, SuppliesVendorNPC suppliesVendorNPC) {
        this.plugin = plugin;
        this.drugDealerNPC = drugDealerNPC;
        this.suppliesVendorNPC = suppliesVendorNPC;
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
            case "spawn_dealer":
                return handleSpawnDealer(player, args);

            case "spawn_vendor":
                return handleSpawnVendor(player, args);

            case "buy_drug":
                return handleBuyDrug(player, args);

            case "buy_supply":
                return handleBuySupply(player, args);

            default:
                return false;
        }
    }

    private boolean handleSpawnDealer(Player player, String[] args) {
        // Check permission
        if (!player.hasPermission("narkomanka.admin")) {
            player.sendMessage(Component.text("У вас нет прав для выполнения этой команды.").color(NamedTextColor.RED));
            return true;
        }

        // Spawn dealer at player's location
        drugDealerNPC.spawnDrugDealer(player.getLocation());
        player.sendMessage(Component.text("Дилер наркотиков успешно создан!").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSpawnVendor(Player player, String[] args) {
        // Check permission
        if (!player.hasPermission("narkomanka.admin")) {
            player.sendMessage(Component.text("У вас нет прав для выполнения этой команды.").color(NamedTextColor.RED));
            return true;
        }

        // Spawn vendor at player's location
        suppliesVendorNPC.spawnSuppliesVendor(player.getLocation());
        player.sendMessage(Component.text("Поставщик оборудования успешно создан!").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleBuyDrug(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("Использование: /buy_drug <тип> [количество]").color(NamedTextColor.RED));
            return true;
        }

        String drugType = args[0].toLowerCase();
        int quantity = 1;

        // Parse quantity if provided
        if (args.length > 1) {
            try {
                quantity = Integer.parseInt(args[1]);
                if (quantity <= 0) {
                    player.sendMessage(Component.text("Количество должно быть положительным числом.").color(NamedTextColor.RED));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Неверный формат количества.").color(NamedTextColor.RED));
                return true;
            }
        }

        // Check if drug type is valid
        if (!drugTypes.contains(drugType)) {
            player.sendMessage(Component.text("Неизвестный тип наркотика. Доступные типы: " + String.join(", ", drugTypes))
                    .color(NamedTextColor.RED));
            return true;
        }

        // Process purchase
        drugDealerNPC.purchaseDrug(player, drugType, quantity);
        return true;
    }

    private boolean handleBuySupply(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("Использование: /buy_supply <тип> [количество]").color(NamedTextColor.RED));
            return true;
        }

        String supplyType = args[0].toLowerCase();
        int quantity = 1;

        // Parse quantity if provided
        if (args.length > 1) {
            try {
                quantity = Integer.parseInt(args[1]);
                if (quantity <= 0) {
                    player.sendMessage(Component.text("Количество должно быть положительным числом.").color(NamedTextColor.RED));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Неверный формат количества.").color(NamedTextColor.RED));
                return true;
            }
        }

        // Process purchase
        suppliesVendorNPC.purchaseSupply(player, supplyType, quantity);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("buy_drug")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                completions.addAll(drugTypes.stream()
                        .filter(type -> type.startsWith(partial))
                        .collect(Collectors.toList()));
            } else if (args.length == 2) {
                // Suggest quantities
                completions.addAll(Arrays.asList("1", "5", "10", "50"));
            }
        } else if (command.getName().equalsIgnoreCase("buy_supply")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                completions.addAll(supplyCategories.stream()
                        .filter(type -> type.startsWith(partial))
                        .collect(Collectors.toList()));
            } else if (args.length == 2) {
                // Suggest quantities
                completions.addAll(Arrays.asList("1", "5", "10"));
            }
        }

        return completions;
    }
}