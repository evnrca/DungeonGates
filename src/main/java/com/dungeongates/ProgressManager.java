package com.dungeongates;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class ProgressManager {
    
    private final DungeonGatesPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, PlayerProgress> progressMap = new ConcurrentHashMap<>();
    
    public ProgressManager(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }
    
    public @NotNull DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public @NotNull PlayerProgress getProgress(@NotNull UUID playerId) {
        return progressMap.computeIfAbsent(playerId, id -> {
            PlayerProgress progress = new PlayerProgress(id);
            // Load from database asynchronously
            loadFromDatabase(id, progress);
            return progress;
        });
    }
    
    public @Nullable PlayerProgress getProgressIfExists(@NotNull UUID playerId) {
        return progressMap.get(playerId);
    }
    
    private void loadFromDatabase(@NotNull UUID playerId, @NotNull PlayerProgress progress) {
        databaseManager.loadProgress(playerId).thenAccept(dataMap -> {
            for (Map.Entry<String, DatabaseManager.RoomProgressData> entry : dataMap.entrySet()) {
                RoomProgress roomProgress = new RoomProgress();
                roomProgress.setKills(entry.getValue().kills);
                roomProgress.setCompleted(entry.getValue().completed);
                progress.getRoomProgressMap().put(entry.getKey(), roomProgress);
            }
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.WARNING, "Failed to load progress for " + playerId, ex);
            return null;
        });
    }
    
    public void addKill(@NotNull UUID playerId, @NotNull String regionKey) {
        PlayerProgress progress = getProgress(playerId);
        progress.addKill(regionKey);
        
        // Check for room completion
        Room room = plugin.getRoomManager().getRoomByUniqueKey(regionKey);
        if (room != null) {
            RoomProgress roomProgress = progress.getRoomProgress(regionKey);
            if (roomProgress != null && roomProgress.getKills() >= room.getRequiredKills() && !roomProgress.isCompleted()) {
                roomProgress.setCompleted(true);
                sendCompletionMessage(playerId, regionKey);
            }
            // Save to database
            databaseManager.saveRoomProgress(playerId, regionKey, roomProgress.getKills(), roomProgress.isCompleted());
        }
    }
    
    private void sendCompletionMessage(@NotNull UUID playerId, @NotNull String regionKey) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            Room room = plugin.getRoomManager().getRoomByUniqueKey(regionKey);
            String regionName = room != null ? room.getRegion() : regionKey;
            String msg = plugin.getConfigManager().getMessage("completed");
            msg = msg.replace("{region}", regionName);
            player.sendMessage(colorize(msg));
        }
    }
    
    public boolean canEnterRoom(@NotNull UUID playerId, @NotNull String regionKey) {
        Room target = plugin.getRoomManager().getRoomByUniqueKey(regionKey);
        if (target == null) return true; // Not a dungeon room
        
        // First room is always accessible
        if (target.getOrder() == 0) return true;
        
        // Check previous room completion
        Room previous = plugin.getRoomManager().getPreviousRoom(target.getRegion(), target.getWorld());
        if (previous == null) return true;
        
        PlayerProgress progress = getProgressIfExists(playerId);
        if (progress == null) {
            // Not in cache, check database synchronously for this specific room
            DatabaseManager.RoomProgressData data = databaseManager.loadRoomProgressSync(playerId, previous.getUniqueKey());
            return data != null && data.completed;
        }
        
        RoomProgress prevProgress = progress.getRoomProgress(previous.getUniqueKey());
        return prevProgress != null && prevProgress.isCompleted();
    }
    
    public void setCurrentRoom(@NotNull UUID playerId, @Nullable String regionKey) {
        PlayerProgress progress = getProgress(playerId);
        progress.setCurrentRoom(regionKey);
    }
    
    public @Nullable String getCurrentRoom(@NotNull UUID playerId) {
        PlayerProgress progress = getProgressIfExists(playerId);
        return progress != null ? progress.getCurrentRoom() : null;
    }
    
    public void resetProgress(@NotNull UUID playerId) {
        progressMap.remove(playerId);
        databaseManager.deleteProgress(playerId);
    }
    
    public void resetProgressSync(@NotNull UUID playerId) {
        progressMap.remove(playerId);
        databaseManager.deleteProgressSync(playerId);
    }
    
    public void resetProgress(@NotNull UUID playerId, @NotNull String regionKey) {
        PlayerProgress progress = progressMap.get(playerId);
        if (progress != null) {
            progress.removeRoomProgress(regionKey);
        }
        databaseManager.deleteRoomProgress(playerId, regionKey);
    }
    
    public void resetProgressSync(@NotNull UUID playerId, @NotNull String regionKey) {
        PlayerProgress progress = progressMap.get(playerId);
        if (progress != null) {
            progress.removeRoomProgress(regionKey);
        }
        databaseManager.deleteRoomProgressSync(playerId, regionKey);
    }
    
    public void clearAllProgress() {
        progressMap.clear();
        databaseManager.deleteAllProgress();
    }
    
    public void saveAllProgress() {
        // Save all cached progress to database (on shutdown)
        for (Map.Entry<UUID, PlayerProgress> entry : progressMap.entrySet()) {
            UUID playerId = entry.getKey();
            PlayerProgress progress = entry.getValue();
            for (Map.Entry<String, RoomProgress> roomEntry : progress.getRoomProgressMap().entrySet()) {
                databaseManager.saveRoomProgressSync(
                    playerId,
                    roomEntry.getKey(),
                    roomEntry.getValue().getKills(),
                    roomEntry.getValue().isCompleted()
                );
            }
        }
    }
    
    private String colorize(String msg) {
        return msg.replace("&", "§");
    }
}