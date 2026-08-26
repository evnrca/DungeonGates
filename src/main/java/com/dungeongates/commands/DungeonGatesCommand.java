package com.dungeongates.commands;

import com.dungeongates.DungeonGatesPlugin;
import com.dungeongates.dungeon.DungeonRoom;
import com.dungeongates.dungeon.PlayerProgress;
import com.dungeongates.dungeon.ProgressManager;
import com.dungeongates.dungeon.RoomManager;
import com.dungeongates.utils.ColorUtil;
import com.dungeongates.utils.MessageUtil;
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
    
    public DungeonGatesCommand(@NotNull DungeonGatesPlugin plugin, @NotNull RoomManager roomManager, @NotNull ProgressManager progressManager) {
        this.plugin = plugin;
        this.roomManager = roomManager;
        this.progressManager = progressManager;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
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
        String prefix = plugin.getConfigManager().getMessageSingle("prefix");
        ColorUtil.sendMessage(sender, prefix + "&6Dungeon Gates Commands:");
        ColorUtil.sendMessage(sender, "&e/dg add <room> <region> <kills> &7- Register a new dungeon room");
        ColorUtil.sendMessage(sender, "&e/dg remove <room> &7- Remove a registered room");
        ColorUtil.sendMessage(sender, "&e/dg list &7- List all registered rooms");
        ColorUtil.sendMessage(sender, "&e/dg status [player] &7- Check dungeon progress");
        ColorUtil.sendMessage(sender, "&e/dg reset <player> [room] &7- Reset player progress");
        ColorUtil.sendMessage(sender, "&e/dg reload &7- Reload configuration");
    }
    
    private boolean handleAdd(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("dungeongates.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + "&cNo permission.");
            return true;
        }
        
        if (args.length < 4) {
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + "&cUsage: /dg add <room> <region> <kills>");
            return true;
        }
        
        String roomName = args[1];
        String regionName = args[2];
        
        int requiredKills;
        try {
            requiredKills = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + 
                MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("invalid-kills"), Map.of()));
            return true;
        }
        
        if (requiredKills < 1) {
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + 
                MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("invalid-kills"), Map.of()));
            return true;
        }
        
        // Check if room already exists
        if (roomManager.isRegisteredRoom(roomName)) {
            Map<String, String> placeholders = Map.of("room", roomName);
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + 
                MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("room-already-exists"), placeholders));
            return true;
        }
        
        // Check if region already registered
        if (roomManager.isRegisteredRegion(regionName)) {
            Map<String, String> placeholders = Map.of("region", regionName);
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + 
                "&cWorldGuard region &e" + regionName + " &cis already registered to another room.");
            return true;
        }
        
        // Validate region exists
        if (!plugin.getWorldGuardHook().regionExists(regionName)) {
            Map<String, String> placeholders = Map.of("region", regionName);
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + 
                MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("region-not-found"), placeholders));
            return true;
        }
        
        // Add room
        if (roomManager.addRoom(roomName, regionName, requiredKills)) {
            Map<String, String> placeholders = Map.of("room", roomName, "kills", String.valueOf(requiredKills));
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + 
                MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("room-added"), placeholders));
            plugin.getLogger().info(sender.getName() + " added room: " + roomName + " (region: " + regionName + ", kills: " + requiredKills + ")");
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + "&cFailed to add room. Check console for details.");
        }
        
        return true;
    }
    
    private boolean handleRemove(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("dungeongates.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + "&cNo permission.");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + "&cUsage: /dg remove <room>");
            return true;
        }
        
        String roomName = args[1];
        
        if (roomManager.removeRoom(roomName)) {
            Map<String, String> placeholders = Map.of("room", roomName);
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + 
                MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("room-removed"), placeholders));
            plugin.getLogger().info(sender.getName() + " removed room: " + roomName);
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + "&cRoom &e" + roomName + " &cnot found.");
        }
        
        return true;
    }
    
    private boolean handleList(@NotNull CommandSender sender) {
        List<DungeonRoom> rooms = roomManager.getAllRooms();
        
        String prefix = plugin.getConfigManager().getMessageSingle("prefix");
        ColorUtil.sendMessage(sender, prefix + MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("rooms-list-header"), Map.of()));
        
        if (rooms.isEmpty()) {
            ColorUtil.sendMessage(sender, prefix + MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("no-rooms-registered"), Map.of()));
        } else {
            for (int i = 0; i < rooms.size(); i++) {
                DungeonRoom room = rooms.get(i);
                Map<String, String> placeholders = Map.of(
                    "index", String.valueOf(i + 1),
                    "room", room.getName(),
                    "region", room.getRegionName(),
                    "kills", String.valueOf(room.getRequiredKills())
                );
                ColorUtil.sendMessage(sender, prefix + 
                    MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("rooms-list-format"), placeholders));
            }
        }
        
        ColorUtil.sendMessage(sender, prefix + MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("rooms-list-footer"), Map.of()));
        return true;
    }
    
    private boolean handleStatus(@NotNull CommandSender sender, @NotNull String[] args) {
        Player targetPlayer;
        
        if (args.length >= 2) {
            if (!sender.hasPermission("dungeongates.status.others")) {
                sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + "&cNo permission to check other players.");
                return true;
            }
            
            targetPlayer = Bukkit.getPlayerExact(args[1]);
            if (targetPlayer == null) {
                Map<String, String> placeholders = Map.of("player", args[1]);
                sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + 
                    MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("error-player-not-found"), placeholders));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + "&cConsole must specify a player.");
                return true;
            }
            targetPlayer = (Player) sender;
        }
        
        PlayerProgress progress = progressManager.getProgressIfExists(targetPlayer.getUniqueId());
        
        if (progress == null) {
            Map<String, String> placeholders = Map.of("player", targetPlayer.getName());
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + 
                MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("error-no-progress"), placeholders));
            return true;
        }
        
        String prefix = plugin.getConfigManager().getMessageSingle("prefix");
        String currentRoom = progress.getCurrentRoom();
        
        if (currentRoom == null) {
            sender.sendMessage(prefix + MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("error-not-in-dungeon"), Map.of()));
        } else {
            DungeonRoom room = roomManager.getRoom(currentRoom);
            RoomProgress roomProgress = progress.getProgress(currentRoom);
            
            int currentKills = roomProgress != null ? roomProgress.getTotalKills() : 0;
            int requiredKills = room != null ? room.getRequiredKills() : 0;
            int remaining = Math.max(0, requiredKills - currentKills);
            double percent = requiredKills > 0 ? (currentKills * 100.0 / requiredKills) : 100.0;
            
            Map<String, String> placeholders = Map.of(
                "player", targetPlayer.getName(),
                "room", currentRoom,
                "current_room", currentRoom,
                "current_kills", String.valueOf(currentKills),
                "required_kills", String.valueOf(requiredKills),
                "remaining_kills", String.valueOf(remaining),
                "progress_percent", String.format("%.1f", percent)
            );
            
            ColorUtil.sendMessage(sender, prefix + MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("status-current-room"), placeholders));
            ColorUtil.sendMessage(sender, prefix + MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("status-progress"), placeholders));
            
            if (roomProgress != null && roomProgress.isCompleted()) {
                ColorUtil.sendMessage(sender, prefix + MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("status-completed"), placeholders));
            }
        }
        
        return true;
    }
    
    private boolean handleReset(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("dungeongates.reset")) {
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + "&cNo permission.");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + "&cUsage: /dg reset <player> [room]");
            return true;
        }
        
        Player targetPlayer = Bukkit.getPlayerExact(args[1]);
        if (targetPlayer == null) {
            Map<String, String> placeholders = Map.of("player", args[1]);
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + 
                MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("error-player-not-found"), placeholders));
            return true;
        }
        
        if (args.length >= 3) {
            String roomName = args[2];
            progressManager.resetProgress(targetPlayer.getUniqueId(), roomName);
            
            Map<String, String> placeholders = Map.of("player", targetPlayer.getName(), "room", roomName);
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + 
                MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("progress-reset-room"), placeholders));
        } else {
            progressManager.resetProgress(targetPlayer.getUniqueId());
            
            Map<String, String> placeholders = Map.of("player", targetPlayer.getName());
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + 
                MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("progress-reset"), placeholders));
        }
        
        return true;
    }
    
    private boolean handleReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission("dungeongates.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + "&cNo permission.");
            return true;
        }
        
        plugin.reloadPlugin();
        sender.sendMessage(plugin.getConfigManager().getMessageSingle("prefix") + 
            MessageUtil.replacePlaceholders(plugin.getConfigManager().getMessageSingle("config-reloaded"), Map.of()));
        return true;
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subCommands = List.of("add", "remove", "list", "status", "reset", "reload");
            return subCommands.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        
        String subCommand = args[0].toLowerCase();
        
        return switch (subCommand) {
            case "add" -> {
                if (args.length == 2) yield List.of("<room_name>");
                if (args.length == 3) yield plugin.getWorldGuardHook().getAllRegionNames();
                if (args.length == 4) yield List.of("<kills>");
                yield List.of();
            }
            case "remove", "status" -> {
                if (args.length == 2) yield new ArrayList<>(roomManager.getRoomNames());
                yield List.of();
            }
            case "reset" -> {
                if (args.length == 2) yield Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                if (args.length == 3) yield new ArrayList<>(roomManager.getRoomNames());
                yield List.of();
            }
            default -> List.of();
        };
    }
}