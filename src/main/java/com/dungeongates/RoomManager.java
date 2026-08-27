package com.dungeongates;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class RoomManager {
    
    private final DungeonGatesPlugin plugin;
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final List<Room> orderedRooms = new ArrayList<>();
    
    public RoomManager(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void load() {
        rooms.clear();
        orderedRooms.clear();
        
        Map<String, Object> roomsConfig = plugin.getConfigManager().getRooms();
        if (roomsConfig == null || roomsConfig.isEmpty()) return;
        
        int order = 0;
        for (Map.Entry<String, Object> entry : roomsConfig.entrySet()) {
            String region = entry.getKey();
            Object value = entry.getValue();
            
            int requiredKills = 10;
            if (value instanceof Number) {
                requiredKills = ((Number) value).intValue();
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> roomData = (Map<String, Object>) value;
                Object kills = roomData.get("kills");
                if (kills instanceof Number) requiredKills = ((Number) kills).intValue();
            }
            
            if (requiredKills < 1) requiredKills = 1;
            
            Room room = new Room(region, requiredKills);
            room.setOrder(order++);
            rooms.put(region, room);
            orderedRooms.add(room);
        }
        
        // Validate regions exist
        for (Room room : rooms.values()) {
            if (!plugin.getWorldGuardHook().regionExists(room.getRegion())) {
                plugin.getLogger().warning("WorldGuard region '" + room.getRegion() + "' does not exist!");
            }
        }
        
        plugin.getLogger().info("Loaded " + rooms.size() + " dungeon room(s).");
    }
    
    public boolean addRoom(@NotNull String region, int requiredKills) {
        if (rooms.containsKey(region)) return false;
        if (!plugin.getWorldGuardHook().regionExists(region)) return false;
        if (requiredKills < 1) return false;
        
        Room room = new Room(region, requiredKills);
        room.setOrder(rooms.size());
        rooms.put(region, room);
        orderedRooms.add(room);
        
        plugin.getConfigManager().saveRoom(region, requiredKills, room.getOrder());
        return true;
    }
    
    public boolean removeRoom(@NotNull String region) {
        Room room = rooms.remove(region);
        if (room == null) return false;
        
        orderedRooms.remove(room);
        plugin.getConfigManager().removeRoom(region);
        reorder();
        return true;
    }
    
    private void reorder() {
        for (int i = 0; i < orderedRooms.size(); i++) {
            orderedRooms.get(i).setOrder(i);
        }
        plugin.getConfigManager().saveRooms(orderedRooms);
    }
    
    public @Nullable Room getRoom(@NotNull String region) {
        return rooms.get(region);
    }
    
    public @Nullable Room getNextRoom(@NotNull String currentRegion) {
        Room current = rooms.get(currentRegion);
        if (current == null) return null;
        
        int currentOrder = current.getOrder();
        if (currentOrder + 1 >= orderedRooms.size()) return null;
        
        return orderedRooms.get(currentOrder + 1);
    }
    
    public @Nullable Room getPreviousRoom(@NotNull String currentRegion) {
        Room current = rooms.get(currentRegion);
        if (current == null) return null;
        
        int currentOrder = current.getOrder();
        if (currentOrder == 0) return null;
        
        return orderedRooms.get(currentOrder - 1);
    }
    
    public @NotNull List<Room> getAllRooms() {
        return new ArrayList<>(orderedRooms);
    }
    
    public int getRoomCount() {
        return rooms.size();
    }
    
    public boolean isRegisteredRegion(@NotNull String region) {
        return rooms.containsKey(region);
    }
    
    public @NotNull Collection<String> getRegionNames() {
        return rooms.keySet();
    }
}