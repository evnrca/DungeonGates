package com.dungeongates.commands;

import com.dungeongates.DungeonGatesPlugin;
import com.dungeongates.ProgressManager;
import com.dungeongates.Room;
import com.dungeongates.RoomManager;
import com.dungeongates.PlayerProgress;
import com.dungeongates.RoomProgress;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public final class DungeonGatesCommand implements CommandExecutor, TabCompleter {
    
    private final DungeonGatesPlugin plugin;
    private final RoomManager roomManager;
    private final ProgressManager progressManager;
    
    public DungeonGatesCommand(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
        this.roomManager = plugin.getRoomManager();
        this.progressManager = plugin.getProgressManager();
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        return switch (subCommand) {
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "status" -> handleStatus(sender, args);
            case "reset" -> handleReset(sender, args);
            case "reload" -> handleReload(sender);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }
    
    private void sendHelp(@NotNull CommandSender sender) {
        String prefix = plugin.getConfigManager().getMessage("prefix");
        sender.sendMessage(colorize(prefix + "&6Dungeon Gates Commands:"));
        sender.sendMessage(colorize("&e/dg add <region> <kills> &7- Register a dungeon room"));
        sender.sendMessage(colorize("&e/dg remove <region> &7- Remove a room"));
        sender.sendMessage(colorize("&e/dg list &7- List all rooms"));
        sender.sendMessage(colorize("&e/dg status [player] &7- Check progress"));
        sender.sendMessage(colorize("&e/dg reset <player> [region] &7- Reset progress"));
        sender.sendMessage(colorize("&e/dg reload &7- Reload config"));
    }
    
    private boolean handleAdd(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("dungeongates.admin")) {
            sender.sendMessage(colorize(plugin.getConfigManager().getPrefixedMessage("prefix") + "&cNo permission."));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(colorize("&cUsage: /dg add <region> <kills>"));
            return true;
        }
        
        String region = args[1];
        int requiredKills;
        
        try {
            requiredKills = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(colorize("&cKills must be a positive number."));
            return true;
        }
        
        if (requiredKills < 1) {
            sender.sendMessage(colorize("&cKills must be at least 1."));
            return true;
        }
        
        if (roomManager.isRegisteredRegion(region)) {
            sender.sendMessage(colorize("&cRegion '" + region + "' is already registered."));
            return true;
        }
        
        if (!plugin.getWorldGuardHook().regionExists(region)) {
            sender.sendMessage(colorize("&cWorldGuard region '" + region + "' does not exist."));
            return true;
        }
        
        if (roomManager.addRoom(region, requiredKills)) {
            sender.sendMessage(colorize("&aRegistered room: &e" + region + " &awith &e" + requiredKills + " &aMythicMob kills."));
        } else {
            sender.sendMessage(colorize("&cFailed to register room."));
        }
        return true;
    }
    
    private boolean handleRemove(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("dungeongates.admin")) {
            sender.sendMessage(colorize("&cNo permission."));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(colorize("&cUsage: /dg remove <region>"));
            return true;
        }
        
        String region = args[1];
        
        if (roomManager.removeRoom(region)) {
            sender.sendMessage(colorize("&aRemoved room: &e" + region));
        } else {
            sender.sendMessage(colorize("&cRoom '" + region + "' not found."));
        }
        return true;
    }
    
    private boolean handleList(@NotNull CommandSender sender) {
        List<Room> rooms = roomManager.getAllRooms();
        
        String prefix = plugin.getConfigManager().getMessage("prefix");
        sender.sendMessage(colorize(prefix + "&6=== &eDungeon Gates Rooms &6==="));
        
        if (rooms.isEmpty()) {
            sender.sendMessage(colorize(prefix + "&eNo rooms registered."));
        } else {
            for (Room room : rooms) {
                sender.sendMessage(colorize(prefix + "&e" + (room.getOrder() + 1) + ". &f" + room.getRegion() + " &7- &e" + room.getRequiredKills() + " &7kills"));
            }
        }
        
        sender.sendMessage(colorize(prefix + "&6=============================="));
        return true;
    }
    
    private boolean handleStatus(@NotNull CommandSender sender, @NotNull String[] args) {
        Player target;
        
        if (args.length >= 2) {
            if (!sender.hasPermission("dungeongates.status.others")) {
                sender.sendMessage(colorize("&cNo permission to check other players."));
                return true;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(colorize("&cPlayer not found."));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(colorize("&cConsole must specify a player."));
                return true;
            }
            target = (Player) sender;
        }
        
        PlayerProgress progress = progressManager.getProgressIfExists(target.getUniqueId());
        String prefix = plugin.getConfigManager().getMessage("prefix");
        
        if (progress == null) {
            sender.sendMessage(colorize(prefix + "&eNo progress data for " + target.getName()));
            return true;
        }
        
        String currentRegion = progress.getCurrentRoom();
        
        if (currentRegion == null) {
            sender.sendMessage(colorize(prefix + "&eNot in a dungeon room."));
        } else {
            Room room = roomManager.getRoom(currentRegion);
            RoomProgress roomProgress = progress.getRoomProgress(currentRegion);
            
            int current = roomProgress != null ? roomProgress.getKills() : 0;
            int required = room != null ? room.getRequiredKills() : 0;
            int remaining = Math.max(0, required - current);
            
            sender.sendMessage(colorize(prefix + "&eCurrent Room: &f" + currentRegion));
            sender.sendMessage(colorize(prefix + "&eProgress: &f" + current + "&7/&f" + required + " &7(&e" + remaining + " &7remaining)"));
            
            if (roomProgress != null && roomProgress.isCompleted()) {
                sender.sendMessage(colorize(prefix + "&aRoom completed!"));
            }
        }
        
        return true;
    }
    
    private boolean handleReset(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("dungeongates.reset")) {
            sender.sendMessage(colorize("&cNo permission."));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(colorize("&cUsage: /dg reset <player> [region]"));
            return true;
        }
        
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(colorize("&cPlayer not found."));
            return true;
        }
        
        UUID uuid = target.getUniqueId();
        
        if (args.length >= 3) {
            String region = args[2];
            progressManager.resetProgress(uuid, region);
            sender.sendMessage(colorize("&aReset progress for &e" + target.getName() + " &ain &e" + region));
        } else {
            progressManager.resetProgress(uuid);
            sender.sendMessage(colorize("&aReset all progress for &e" + target.getName()));
        }
        
        return true;
    }
    
    private boolean handleReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission("dungeongates.admin")) {
            sender.sendMessage(colorize("&cNo permission."));
            return true;
        }
        
        plugin.reload();
        sender.sendMessage(colorize("&aConfiguration reloaded."));
        return true;
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subCommands = List.of("add", "remove", "list", "status", "reset", "reload");
            return subCommands.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        
        String subCommand = args[0].toLowerCase();
        
        return switch (subCommand) {
            case "add" -> {
                if (args.length == 2) yield plugin.getWorldGuardHook().getAllRegionNames();
                if (args.length == 3) yield List.of("<kills>");
                yield List.of();
            }
            case "remove" -> {
                if (args.length == 2) yield new ArrayList<>(roomManager.getRegionNames());
                yield List.of();
            }
            case "status" -> {
                if (args.length == 2) yield Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                yield List.of();
            }
            case "reset" -> {
                if (args.length == 2) yield Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                if (args.length == 3) yield new ArrayList<>(roomManager.getRegionNames());
                yield List.of();
            }
            default -> List.of();
        };
    }
    
    private String colorize(String msg) {
        return msg.replace("&", "§");
    }
}