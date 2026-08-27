package com.dungeongates;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerProgress {
    
    private final UUID playerId;
    private final Map<String, RoomProgress> roomProgress = new HashMap<>();
    private @Nullable String currentRoom;
    
    public PlayerProgress(@NotNull UUID playerId) {
        this.playerId = playerId;
    }
    
    public @NotNull UUID getPlayerId() {
        return playerId;
    }
    
    public @NotNull Map<String, RoomProgress> getRoomProgressMap() {
        return roomProgress;
    }
    
    public @NotNull RoomProgress getOrCreateProgress(@NotNull String region) {
        return roomProgress.computeIfAbsent(region, k -> new RoomProgress());
    }
    
    public @Nullable RoomProgress getRoomProgress(@NotNull String region) {
        return roomProgress.get(region);
    }
    
    public void addKill(@NotNull String region) {
        RoomProgress progress = getOrCreateProgress(region);
        progress.addKill();
    }
    
    public void removeRoomProgress(@NotNull String region) {
        roomProgress.remove(region);
    }
    
    public void clearAllProgress() {
        roomProgress.clear();
        currentRoom = null;
    }
    
    public @Nullable String getCurrentRoom() {
        return currentRoom;
    }
    
    public void setCurrentRoom(@Nullable String currentRoom) {
        this.currentRoom = currentRoom;
    }
}