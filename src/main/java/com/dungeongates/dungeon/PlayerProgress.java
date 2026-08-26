package com.dungeongates.dungeon;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerProgress {
    
    private final UUID playerId;
    private final Map<String, RoomProgress> roomProgress;
    private String currentRoom;
    private long lastUpdate;
    
    public PlayerProgress(@NotNull UUID playerId) {
        this.playerId = playerId;
        this.roomProgress = new HashMap<>();
        this.currentRoom = null;
        this.lastUpdate = System.currentTimeMillis();
    }
    
    public @NotNull UUID getPlayerId() {
        return playerId;
    }
    
    public @NotNull Map<String, RoomProgress> getRoomProgress() {
        return roomProgress;
    }
    
    public @NotNull RoomProgress getOrCreateProgress(@NotNull String roomName) {
        return roomProgress.computeIfAbsent(roomName, RoomProgress::new);
    }
    
    public @Nullable RoomProgress getProgress(@NotNull String roomName) {
        return roomProgress.get(roomName);
    }
    
    public @Nullable String getCurrentRoom() {
        return currentRoom;
    }
    
    public void setCurrentRoom(@Nullable String currentRoom) {
        this.currentRoom = currentRoom;
        this.lastUpdate = System.currentTimeMillis();
    }
    
    public long getLastUpdate() {
        return lastUpdate;
    }
    
    public void updateLastUpdate() {
        this.lastUpdate = System.currentTimeMillis();
    }
    
    public void resetProgress(@NotNull String roomName) {
        roomProgress.remove(roomName);
    }
    
    public void resetAllProgress() {
        roomProgress.clear();
        currentRoom = null;
    }
    
    public boolean hasProgress(@NotNull String roomName) {
        RoomProgress progress = roomProgress.get(roomName);
        return progress != null && progress.getKills() > 0;
    }
    
    @Override
    public String toString() {
        return "PlayerProgress{playerId=" + playerId + ", currentRoom='" + currentRoom + "', rooms=" + roomProgress.size() + "}";
    }
}