package com.dungeongates.dungeon;

import com.dungeongates.DungeonGatesPlugin;
import com.dungeongates.config.ConfigManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class RoomManager {
    
    private final DungeonGatesPlugin plugin;
    private final ConfigManager configManager;
    private final Map<String, DungeonRoom> rooms;
    private final Map<String, DungeonRoom> regionToRoom;
    
    public RoomManager(@NotNull DungeonGatesPlugin plugin, @NotNull ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.rooms = new ConcurrentHashMap<>();
        this.regionToRoom = new ConcurrentHashMap<>();
    }
    
    public void loadRooms() {
        rooms.clear();
        regionToRoom.clear();
        
        Map<String, DungeonRoom> loadedRooms = configManager.loadRooms();
        for (DungeonRoom room : loadedRooms.values()) {
            rooms.put(room.getName(), room);
            regionToRoom.put(room.getRegionName(), room);
        }
        
        // Validate regions with WorldGuard
        plugin.getWorldGuardHook().validateRooms(rooms.values());
        
        plugin.getLogger().info("Loaded " + rooms.size() + " dungeon room(s).");
    }
    
    public boolean addRoom(@NotNull String roomName, @NotNull String regionName, int requiredKills) {
        if (rooms.containsKey(roomName)) {
            return false;
        }
        
        if (regionToRoom.containsKey(regionName)) {
            return false;
        }
        
        // Validate region exists
        if (!plugin.getWorldGuardHook().regionExists(regionName)) {
            return false;
        }
        
        if (requiredKills < 1) {
            return false;
        }
        
        DungeonRoom room = new DungeonRoom(roomName, regionName, requiredKills);
        room.setOrder(rooms.size());
        
        ProtectedRegion region = plugin.getWorldGuardHook().getRegion(regionName);
        if (region != null) {
            room.setRegion(region);
        }
        
        rooms.put(roomName, room);
        regionToRoom.put(regionName, room);
        
        configManager.saveRoom(room);
        return true;
    }
    
    public boolean removeRoom(@NotNull String roomName) {
        DungeonRoom room = rooms.remove(roomName);
        if (room == null) {
            return false;
        }
        
        regionToRoom.remove(room.getRegionName());
        configManager.removeRoom(roomName);
        
        // Reorder remaining rooms
        reorderRooms();
        return true;
    }
    
    public void reorderRooms() {
        List<DungeonRoom> sorted = new ArrayList<>(rooms.values());
        sorted.sort(Comparator.comparingInt(DungeonRoom::getOrder));
        
        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).setOrder(i);
        }
        
        configManager.reorderRooms(sorted);
    }
    
    public @Nullable DungeonRoom getRoom(@NotNull String roomName) {
        return rooms.get(roomName);
    }
    
    public @Nullable DungeonRoom getRoomByRegion(@NotNull String regionName) {
        return regionToRoom.get(regionName);
    }
    
    public @Nullable DungeonRoom getNextRoom(@NotNull String currentRoomName) {
        DungeonRoom current = rooms.get(currentRoomName);
        if (current == null) return null;
        
        int currentOrder = current.getOrder();
        return rooms.values().stream()
                .filter(r -> r.getOrder() == currentOrder + 1)
                .findFirst()
                .orElse(null);
    }
    
    public @Nullable DungeonRoom getPreviousRoom(@NotNull String currentRoomName) {
        DungeonRoom current = rooms.get(currentRoomName);
        if (current == null) return null;
        
        int currentOrder = current.getOrder();
        if (currentOrder == 0) return null;
        
        return rooms.values().stream()
                .filter(r -> r.getOrder() == currentOrder - 1)
                .findFirst()
                .orElse(null);
    }
    
    public @NotNull List<DungeonRoom> getAllRooms() {
        List<DungeonRoom> sorted = new ArrayList<>(rooms.values());
        sorted.sort(Comparator.comparingInt(DungeonRoom::getOrder));
        return sorted;
    }
    
    public int getRoomCount() {
        return rooms.size();
    }
    
    public boolean isRegisteredRoom(@NotNull String roomName) {
        return rooms.containsKey(roomName);
    }
    
    public boolean isRegisteredRegion(@NotNull String regionName) {
        return regionToRoom.containsKey(regionName);
    }
    
    public @NotNull Collection<String> getRoomNames() {
        return rooms.keySet();
    }
    
    public @NotNull Collection<String> getRegionNames() {
        return regionToRoom.keySet();
    }
}