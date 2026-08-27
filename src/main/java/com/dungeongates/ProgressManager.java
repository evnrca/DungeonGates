package com.dungeongates;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ProgressManager {
    
    private final DungeonGatesPlugin plugin;
    private final Map<UUID, PlayerProgress> progressMap = new ConcurrentHashMap<>();
    
    public ProgressManager(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
    }
    
    public @NotNull PlayerProgress getProgress(@NotNull UUID playerId) {
        return progressMap.computeIfAbsent(playerId, PlayerProgress::new);
    }
    
    public @Nullable PlayerProgress getProgressIfExists(@NotNull UUID playerId) {
        return progressMap.get(playerId);
    }
    
    public void addKill(@NotNull UUID playerId, @NotNull String region) {
        PlayerProgress progress = getProgress(playerId);
        progress.addKill(region);
        
        // Check for room completion
        Room room = plugin.getRoomManager().getRoom(region);
        if (room != null) {
            RoomProgress roomProgress = progress.getRoomProgress(region);
            if (roomProgress != null && roomProgress.getKills() >= room.getRequiredKills() && !roomProgress.isCompleted()) {
                roomProgress.setCompleted(true);
                sendCompletionMessage(playerId, region);
            }
        }
    }
    
    private void sendCompletionMessage(@NotNull UUID playerId, @NotNull String region) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            String msg = plugin.getConfigManager().getMessage("completed");
            msg = msg.replace("{region}", region);
            player.sendMessage(colorize(msg));
        }
    }
    
    public boolean canEnterRoom(@NotNull UUID playerId, @NotNull String region) {
        Room target = plugin.getRoomManager().getRoom(region);
        if (target == null) return true; // Not a dungeon room
        
        // First room is always accessible
        if (target.getOrder() == 0) return true;
        
        // Check previous room completion
        Room previous = plugin.getRoomManager().getPreviousRoom(region);
        if (previous == null) return true;
        
        PlayerProgress progress = getProgressIfExists(playerId);
        if (progress == null) return false;
        
        RoomProgress prevProgress = progress.getRoomProgress(previous.getRegion());
        return prevProgress != null && prevProgress.isCompleted();
    }
    
    public void setCurrentRoom(@NotNull UUID playerId, @Nullable String region) {
        PlayerProgress progress = getProgress(playerId);
        progress.setCurrentRoom(region);
    }
    
    public @Nullable String getCurrentRoom(@NotNull UUID playerId) {
        PlayerProgress progress = getProgressIfExists(playerId);
        return progress != null ? progress.getCurrentRoom() : null;
    }
    
    public void resetProgress(@NotNull UUID playerId) {
        progressMap.remove(playerId);
    }
    
    public void resetProgress(@NotNull UUID playerId, @NotNull String region) {
        PlayerProgress progress = progressMap.get(playerId);
        if (progress != null) {
            progress.removeRoomProgress(region);
        }
    }
    
    public void clearAllProgress() {
        progressMap.clear();
    }
    
    private String colorize(String msg) {
        return msg.replace("&", "§");
    }
}