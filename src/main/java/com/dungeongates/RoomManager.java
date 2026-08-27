package com.dungeongates;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class RoomManager {
    
    private final DungeonGatesPlugin plugin;
    private final Map<String, Room> rooms = new ConcurrentHashMap<>(); // key = world:region
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
            String key = entry.getKey(); // format: world:region or just region (legacy)
            Object value = entry.getValue();
            
            String world, region;
            if (key.contains(":")) {
                String[] parts = key.split(":", 2);
                world = parts[0];
                region = parts[1];
            } else {
                // Legacy format - assume main world
                world = "world";
                region = key;
            }
            
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
            
            Room room = new Room(region, world, requiredKills);
            room.setOrder(order++);
            rooms.put(room.getUniqueKey(), room);
            orderedRooms.add(room);
        }
        
        // Validate regions exist
        for (Room room : rooms.values()) {
            if (!plugin.getWorldGuardHook().regionExists(room.getRegion(), room.getWorld())) {
                plugin.getLogger().warning("WorldGuard region '" + room.getRegion() + "' in world '" + room.getWorld() + "' does not exist!");
            }
        }
        
        plugin.getLogger().info("Loaded " + rooms.size() + " dungeon room(s).");
    }
    
    public boolean addRoom(@NotNull String region, @NotNull String world, int requiredKills) {
        String key = world + ":" + region;
        if (rooms.containsKey(key)) return false;
        if (!plugin.getWorldGuardHook().regionExists(region, world)) return false;
        if (requiredKills < 1) return false;
        
        Room room = new Room(region, world, requiredKills);
        room.setOrder(rooms.size());
        rooms.put(key, room);
        orderedRooms.add(room);
        
        plugin.getConfigManager().saveRoom(world, region, requiredKills, room.getOrder());
        return true;
    }
    
    // Legacy method for backwards compatibility
    public boolean addRoom(@NotNull String region, int requiredKills) {
        return addRoom(region, "world", requiredKills);
    }
    
    public boolean removeRoom(@NotNull String region, @NotNull String world) {
        String key = world + ":" + region;
        Room room = rooms.remove(key);
        if (room == null) return false;
        
        orderedRooms.remove(room);
        plugin.getConfigManager().removeRoom(world, region);
        reorder();
        return true;
    }
    
    // Legacy method
    public boolean removeRoom(@NotNull String region) {
        return removeRoom(region, "world");
    }
    
    private void reorder() {
        for (int i = 0; i < orderedRooms.size(); i++) {
            orderedRooms.get(i).setOrder(i);
        }
        plugin.getConfigManager().saveRooms(orderedRooms);
    }
    
    public @Nullable Room getRoom(@NotNull String region, @NotNull String world) {
        return rooms.get(world + ":" + region);
    }
    
    public @Nullable Room getRoomByUniqueKey(@NotNull String uniqueKey) {
        return rooms.get(uniqueKey);
    }
    
    // Legacy method
    public @Nullable Room getRoom(@NotNull String region) {
        // Try exact match first, then fallback to any world
        for (Room room : rooms.values()) {
            if (room.getRegion().equals(region)) return room;
        }
        return null;
    }
    
    public @Nullable Room getNextRoom(@NotNull String currentRegion, @NotNull String world) {
        Room current = getRoom(currentRegion, world);
        if (current == null) return null;
        
        int currentOrder = current.getOrder();
        if (currentOrder + 1 >= orderedRooms.size()) return null;
        
        // Find next room in same world
        for (Room room : orderedRooms) {
            if (room.getOrder() == currentOrder + 1 && room.getWorld().equals(world)) {
                return room;
            }
        }
        return null;
    }
    
    public @Nullable Room getPreviousRoom(@NotNull String currentRegion, @NotNull String world) {
        Room current = getRoom(currentRegion, world);
        if (current == null) return null;
        
        int currentOrder = current.getOrder();
        if (currentOrder == 0) return null;
        
        // Find previous room in same world
        for (Room room : orderedRooms) {
            if (room.getOrder() == currentOrder - 1 && room.getWorld().equals(world)) {
                return room;
            }
        }
        return null;
    }
    
    public @NotNull List<Room> getAllRooms() {
        return new ArrayList<>(orderedRooms);
    }
    
    public @NotNull List<Room> getRoomsInWorld(@NotNull String world) {
        List<Room> result = new ArrayList<>();
        for (Room room : orderedRooms) {
            if (room.getWorld().equals(world)) {
                result.add(room);
            }
        }
        return result;
    }
    
    public int getRoomCount() {
        return rooms.size();
    }
    
    public boolean isRegisteredRegion(@NotNull String region, @NotNull String world) {
        return rooms.containsKey(world + ":" + region);
    }
    
    // Legacy method
    public boolean isRegisteredRegion(@NotNull String region) {
        for (Room room : rooms.values()) {
            if (room.getRegion().equals(region)) return true;
        }
        return false;
    }
    
    public @NotNull Collection<String> getRegionNames() {
        Set<String> names = new HashSet<>();
        for (Room room : rooms.values()) {
            names.add(room.getRegion());
        }
        return names;
    }
    
    public @NotNull Collection<String> getRegionNamesInWorld(@NotNull String world) {
        Set<String> names = new HashSet<>();
        for (Room room : rooms.values()) {
            if (room.getWorld().equals(world)) {
                names.add(room.getRegion());
            }
        }
        return names;
    }
}