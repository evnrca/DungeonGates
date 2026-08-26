package com.dungeongates.dungeon;

import com.dungeongates.DungeonGatesPlugin;
import com.dungeongates.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class ProgressManager {
    
    private final DungeonGatesPlugin plugin;
    private final ConfigManager configManager;
    private final RoomManager roomManager;
    private final Map<UUID, PlayerProgress> progressMap;
    
    public ProgressManager(@NotNull DungeonGatesPlugin plugin, @NotNull ConfigManager configManager, @NotNull RoomManager roomManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.roomManager = roomManager;
        this.progressMap = new ConcurrentHashMap<>();
    }
    
    public @NotNull PlayerProgress getProgress(@NotNull UUID playerId) {
        return progressMap.computeIfAbsent(playerId, PlayerProgress::new);
    }
    
    public @Nullable PlayerProgress getProgressIfExists(@NotNull UUID playerId) {
        return progressMap.get(playerId);
    }
    
    public void addKill(@NotNull UUID playerId, @NotNull String roomName, @Nullable String mobType) {
        PlayerProgress progress = getProgress(playerId);
        RoomProgress roomProgress = progress.getOrCreateProgress(roomName);
        
        if (mobType != null) {
            roomProgress.addKill(mobType);
        } else {
            roomProgress.addKill();
        }
        
        checkCompletion(playerId, roomName);
    }
    
    private void checkCompletion(@NotNull UUID playerId, @NotNull String roomName) {
        PlayerProgress progress = getProgress(playerId);
        RoomProgress roomProgress = progress.getProgress(roomName);
        DungeonRoom room = roomManager.getRoom(roomName);
        
        if (roomProgress == null || room == null) return;
        
        if (!roomProgress.isCompleted() && roomProgress.getTotalKills() >= room.getRequiredKills()) {
            roomProgress.setCompleted(true);
            progress.updateLastUpdate();
            
            // Send completion message
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                sendCompletionMessage(player, roomName);
            }
        }
    }
    
    private void sendCompletionMessage(@NotNull Player player, @NotNull String roomName) {
        DungeonRoom room = roomManager.getRoom(roomName);
        DungeonRoom nextRoom = roomManager.getNextRoom(roomName);
        
        Map<String, String> placeholders = Map.of(
            "player", player.getName(),
            "room", roomName,
            "current_room", roomName,
            "next_room", nextRoom != null ? nextRoom.getName() : "None",
            "current_kills", String.valueOf(room.getRequiredKills()),
            "required_kills", String.valueOf(room.getRequiredKills()),
            "remaining_kills", "0",
            "progress_percent", "100.0"
        );
        
        List<String> messages = configManager.getMessage("room-completed");
        for (String msg : messages) {
            plugin.getMessageUtil().send(player, msg, placeholders);
        }
    }
    
    public boolean canEnterRoom(@NotNull UUID playerId, @NotNull String roomName) {
        // First room (order 0) is always accessible
        DungeonRoom targetRoom = roomManager.getRoom(roomName);
        if (targetRoom == null) return true; // Not a dungeon room
        
        if (targetRoom.getOrder() == 0) return true;
        
        // Check if previous room is completed
        DungeonRoom previousRoom = roomManager.getPreviousRoom(roomName);
        if (previousRoom == null) return true;
        
        PlayerProgress progress = getProgress(playerId);
        RoomProgress prevProgress = progress.getProgress(previousRoom.getName());
        
        if (prevProgress == null) return false;
        
        return prevProgress.isCompleted();
    }
    
    public void setCurrentRoom(@NotNull UUID playerId, @Nullable String roomName) {
        PlayerProgress progress = getProgress(playerId);
        progress.setCurrentRoom(roomName);
    }
    
    public @Nullable String getCurrentRoom(@NotNull UUID playerId) {
        PlayerProgress progress = getProgressIfExists(playerId);
        return progress != null ? progress.getCurrentRoom() : null;
    }
    
    public void resetProgress(@NotNull UUID playerId) {
        PlayerProgress progress = progressMap.get(playerId);
        if (progress != null) {
            progress.resetAllProgress();
        }
    }
    
    public void resetProgress(@NotNull UUID playerId, @NotNull String roomName) {
        PlayerProgress progress = progressMap.get(playerId);
        if (progress != null) {
            progress.resetProgress(roomName);
        }
    }
    
    public void removePlayer(@NotNull UUID playerId) {
        progressMap.remove(playerId);
    }
    
    public void handlePlayerQuit(@NotNull PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        
        // Clean up if configured
        if (configManager.getProgressReset().onDungeonExit) {
            resetProgress(playerId);
        }
    }
    
    public void handlePlayerDeath(@NotNull UUID playerId) {
        if (configManager.getProgressReset().onPlayerDeath) {
            resetProgress(playerId);
        }
    }
    
    public int getTotalPlayers() {
        return progressMap.size();
    }
    
    public void clearAllProgress() {
        progressMap.clear();
    }
}