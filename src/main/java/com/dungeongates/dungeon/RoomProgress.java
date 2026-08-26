package com.dungeongates.dungeon;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class RoomProgress {
    
    private final String roomName;
    private int totalKills;
    private final Map<String, Integer> specificMobKills;
    private boolean completed;
    private long completedAt;
    
    public RoomProgress(@NotNull String roomName) {
        this.roomName = roomName;
        this.totalKills = 0;
        this.specificMobKills = new HashMap<>();
        this.completed = false;
        this.completedAt = 0;
    }
    
    public @NotNull String getRoomName() {
        return roomName;
    }
    
    public int getTotalKills() {
        return totalKills;
    }
    
    public void addKill() {
        this.totalKills++;
    }
    
    public void addKill(@NotNull String mobType) {
        this.totalKills++;
        this.specificMobKills.merge(mobType, 1, Integer::sum);
    }
    
    public int getSpecificKills(@NotNull String mobType) {
        return specificMobKills.getOrDefault(mobType, 0);
    }
    
    public @NotNull Map<String, Integer> getSpecificMobKills() {
        return specificMobKills;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed && completedAt == 0) {
            this.completedAt = System.currentTimeMillis();
        }
    }
    
    public long getCompletedAt() {
        return completedAt;
    }
    
    public void reset() {
        this.totalKills = 0;
        this.specificMobKills.clear();
        this.completed = false;
        this.completedAt = 0;
    }
    
    public int getRemainingKills(int required) {
        return Math.max(0, required - totalKills);
    }
    
    public double getProgressPercent(int required) {
        if (required <= 0) return 100.0;
        return Math.min(100.0, (totalKills * 100.0) / required);
    }
    
    @Override
    public String toString() {
        return "RoomProgress{room='" + roomName + "', kills=" + totalKills + ", completed=" + completed + "}";
    }
}