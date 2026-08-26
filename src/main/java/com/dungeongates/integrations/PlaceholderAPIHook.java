package com.dungeongates.integrations;

import com.dungeongates.DungeonGatesPlugin;
import com.dungeongates.dungeon.PlayerProgress;
import com.dungeongates.dungeon.RoomProgress;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    
    private final DungeonGatesPlugin plugin;
    
    public PlaceholderAPIHook(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "dungeongates";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "DungeonGates Team";
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public @Nullable String onPlaceholderRequest(@NotNull OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) return "";
        
        PlayerProgress progress = plugin.getProgressManager().getProgress(player.getUniqueId());
        if (progress == null) return "";
        
        return switch (params.toLowerCase()) {
            case "current_room" -> progress.getCurrentRoom() != null ? progress.getCurrentRoom() : "None";
            case "current_kills" -> {
                String room = progress.getCurrentRoom();
                if (room == null) yield "0";
                RoomProgress rp = progress.getProgress(room);
                yield rp != null ? String.valueOf(rp.getTotalKills()) : "0";
            }
            case "required_kills" -> {
                String room = progress.getCurrentRoom();
                if (room == null) yield "0";
                DungeonRoom dr = plugin.getRoomManager().getRoom(room);
                yield dr != null ? String.valueOf(dr.getRequiredKills()) : "0";
            }
            case "remaining_kills" -> {
                String room = progress.getCurrentRoom();
                if (room == null) yield "0";
                RoomProgress rp = progress.getProgress(room);
                DungeonRoom dr = plugin.getRoomManager().getRoom(room);
                if (rp == null || dr == null) yield "0";
                yield String.valueOf(rp.getRemainingKills(dr.getRequiredKills()));
            }
            case "progress_percent" -> {
                String room = progress.getCurrentRoom();
                if (room == null) yield "0";
                RoomProgress rp = progress.getProgress(room);
                DungeonRoom dr = plugin.getRoomManager().getRoom(room);
                if (rp == null || dr == null) yield "0";
                yield String.format("%.1f", rp.getProgressPercent(dr.getRequiredKills()));
            }
            case "rooms_completed" -> String.valueOf((int) progress.getRoomProgress().values().stream().filter(RoomProgress::isCompleted).count());
            case "total_rooms" -> String.valueOf(plugin.getRoomManager().getRoomCount());
            default -> null;
        };
    }
}