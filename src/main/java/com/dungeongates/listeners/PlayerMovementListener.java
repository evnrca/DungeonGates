package com.dungeongates.listeners;

import com.dungeongates.DungeonGatesPlugin;
import com.dungeongates.ProgressManager;
import com.dungeongates.Room;
import com.dungeongates.RoomManager;
import com.dungeongates.RoomProgress;
import com.dungeongates.PlayerProgress;
import com.dungeongates.hooks.WorldGuardHook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerMovementListener implements Listener {
    
    private final DungeonGatesPlugin plugin;
    private final ProgressManager progressManager;
    private final RoomManager roomManager;
    private final WorldGuardHook worldGuardHook;
    
    // Cache player's last known dungeon region
    private final Map<UUID, String> lastKnownRegion = new ConcurrentHashMap<>();
    // Cache player's last valid location in a completed room
    private final Map<UUID, Location> lastValidLocation = new ConcurrentHashMap<>();
    
    public PlayerMovementListener(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
        this.progressManager = plugin.getProgressManager();
        this.roomManager = plugin.getRoomManager();
        this.worldGuardHook = plugin.getWorldGuardHook();
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Only check if player changed blocks
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        // Get current world and region
        String currentWorld = player.getWorld().getName();
        String currentRegion = worldGuardHook.getRegionAt(player.getLocation());
        String currentKey = (currentRegion != null) ? currentWorld + ":" + currentRegion : null;
        
        // Quick check: is player in any dungeon region?
        String lastKey = lastKnownRegion.get(player.getUniqueId());
        if (currentKey == null && lastKey == null) {
            // Player not in dungeon region and wasn't in one - skip
            return;
        }
        
        String lastRegion = null;
        String lastWorld = null;
        if (lastKey != null && lastKey.contains(":")) {
            String[] parts = lastKey.split(":", 2);
            lastWorld = parts[0];
            lastRegion = parts[1];
        }
        
        String currentKeyForProgress = currentKey;
        
        // Update progress manager
        progressManager.setCurrentRoom(player.getUniqueId(), currentKey);
        
        // Player entered a new dungeon region
        if (currentKey != null && !currentKey.equals(lastKey)) {
            handleRegionEntry(player, currentWorld, currentRegion, lastWorld, lastRegion, event.getFrom());
        } 
        // Player left all dungeon regions
        else if (currentKey == null && lastKey != null) {
            handleDungeonExit(player, lastWorld, lastRegion);
        }
        
        // Update cache (only non-null regions)
        if (currentKey != null) {
            lastKnownRegion.put(player.getUniqueId(), currentKey);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(@NotNull PlayerTeleportEvent event) {
        // Check if teleporting out of dungeon
        Player player = event.getPlayer();
        String fromWorld = event.getFrom().getWorld().getName();
        String fromRegion = worldGuardHook.getRegionAt(event.getFrom());
        String toWorld = event.getTo().getWorld().getName();
        String toRegion = worldGuardHook.getRegionAt(event.getTo());
        
        // If teleporting from a dungeon region to outside, clear progress
        if (fromRegion != null && roomManager.isRegisteredRegion(fromRegion, fromWorld)) {
            if (toRegion == null || !roomManager.isRegisteredRegion(toRegion, toWorld)) {
                handleDungeonExit(player, fromWorld, fromRegion);
            }
        }
    }
    
    private void handleRegionEntry(@NotNull Player player, @NotNull String newWorld, @NotNull String newRegion, 
                                    @Nullable String previousWorld, @Nullable String previousRegion, 
                                    @NotNull Location fromLocation) {
        Room newRoom = roomManager.getRoom(newRegion, newWorld);
        if (newRoom == null) return; // Not a dungeon room
        
        // Check if player is entering a completed room or first room - always allow
        if (newRoom.getOrder() == 0) {
            updateLastValidLocation(player, newRoom);
            sendProgressMessage(player, newRoom);
            return;
        }
        
        Room previousRoom = (previousRegion != null && previousWorld != null) 
            ? roomManager.getRoom(previousRegion, previousWorld) : null;
        
        // Check if entering the immediate next room in sequence
        if (previousRoom != null && roomManager.getNextRoom(previousRegion, previousWorld) == newRoom) {
            // Player trying to progress to next room - check requirements
            if (!progressManager.canEnterRoom(player.getUniqueId(), newRoom.getUniqueKey())) {
                // Requirements not met - deny entry
                handleFailedEntry(player, previousRoom, newRoom, fromLocation, event -> event.setCancelled(true));
                return;
            }
            
            // Requirements met - allow entry
            updateLastValidLocation(player, newRoom);
            sendProgressMessage(player, newRoom);
            return;
        }
        
        // Allow entry to any other room (e.g., returning to previous rooms, or non-sequential)
        // But only if the target room is already completed or is the first room
        if (newRoom.getOrder() == 0) {
            updateLastValidLocation(player, newRoom);
            sendProgressMessage(player, newRoom);
            return;
        }
        
        // Check if this room itself is completed
        PlayerProgress progress = progressManager.getProgress(player.getUniqueId());
        RoomProgress roomProgress = progress.getRoomProgress(newRoom.getUniqueKey());
        if (roomProgress != null && roomProgress.isCompleted()) {
            updateLastValidLocation(player, newRoom);
            sendProgressMessage(player, newRoom);
            return;
        }
        
        // Player is trying to enter a room they haven't completed and aren't progressing to
        // Deny entry
        if (previousRegion != null && previousWorld != null) {
            Room prev = roomManager.getRoom(previousRegion, previousWorld);
            if (prev != null) {
                handleFailedEntry(player, prev, newRoom, fromLocation, event -> event.setCancelled(true));
                return;
            }
        }
        
        // Fallback: deny entry and send back
        Location center = plugin.getWorldGuardHook().getRegionCenter(newRoom.getRegion(), newWorld);
        if (center != null) {
            player.teleport(center);
        }
    }
    
    private void handleFailedEntry(@NotNull Player player, @NotNull Room fromRoom, 
                                    @NotNull Room toRoom, @NotNull Location fromLocation,
                                    @NotNull java.util.function.Consumer<PlayerMoveEvent> cancelAction) {
        PlayerProgress progress = progressManager.getProgress(player.getUniqueId());
        RoomProgress fromProgress = progress.getRoomProgress(fromRoom.getRegion());
        
        int currentKills = fromProgress != null ? fromProgress.getKills() : 0;
        int requiredKills = fromRoom.getRequiredKills();
        int remaining = Math.max(0, requiredKills - currentKills);
        
        // Send title message
        String title = plugin.getConfigManager().getMessage("denied-title");
        String subtitle = plugin.getConfigManager().getMessage("denied-subtitle");
        title = title.replace("{remaining}", String.valueOf(remaining))
                     .replace("{region}", fromRoom.getRegion());
        subtitle = subtitle.replace("{remaining}", String.valueOf(remaining))
                           .replace("{region}", fromRoom.getRegion());
        
        player.sendTitle(colorize(title), colorize(subtitle), 10, 70, 20);
        
        // Play sound
        String soundName = plugin.getConfigManager().getMessage("denied-sound");
        float volume = plugin.getConfigManager().getDeniedSoundVolume();
        float pitch = plugin.getConfigManager().getDeniedSoundPitch();
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound in config: " + soundName);
        }
        
        // Send chat message
        String msg = plugin.getConfigManager().getPrefixedMessage("requirement-not-met");
        msg = msg.replace("{remaining}", String.valueOf(remaining))
                 .replace("{region}", fromRoom.getRegion());
        player.sendMessage(colorize(msg));
        
        // Cancel movement and teleport back
        cancelAction.accept(null); // We handle teleport manually
        
        // Teleport back to last valid location in previous room, or region center
        Location backLocation = lastValidLocation.get(player.getUniqueId());
        if (backLocation != null && backLocation.getWorld() == player.getWorld()) {
            player.teleport(backLocation);
        } else {
            Location center = plugin.getWorldGuardHook().getRegionCenter(fromRoom.getRegion(), fromRoom.getWorld());
            if (center != null) {
                player.teleport(center);
            } else {
                player.teleport(fromLocation);
            }
        }
    }
    
    private void handleDungeonExit(@NotNull Player player, @Nullable String lastWorld, @Nullable String lastRegion) {
        // Clear progress when leaving dungeon
        plugin.getProgressManager().resetProgress(player.getUniqueId());
        lastKnownRegion.remove(player.getUniqueId());
        lastValidLocation.remove(player.getUniqueId());
    }
    
    private void updateLastValidLocation(@NotNull Player player, @NotNull Room room) {
        // Store current location as last valid if in a completed room
        PlayerProgress progress = progressManager.getProgress(player.getUniqueId());
        RoomProgress roomProgress = progress.getRoomProgress(room.getRegion());
        if (roomProgress != null && roomProgress.isCompleted()) {
            lastValidLocation.put(player.getUniqueId(), player.getLocation().clone());
        }
    }
    
    private void sendProgressMessage(@NotNull Player player, @NotNull Room room) {
        PlayerProgress progress = progressManager.getProgress(player.getUniqueId());
        RoomProgress roomProgress = progress.getRoomProgress(room.getRegion());
        
        int current = roomProgress != null ? roomProgress.getKills() : 0;
        int required = room.getRequiredKills();
        
        // Only show progress if not completed or if in the room
        if (roomProgress == null || !roomProgress.isCompleted()) {
            String msg = plugin.getConfigManager().getPrefixedMessage("progress");
            msg = msg.replace("{current}", String.valueOf(current))
                     .replace("{required}", String.valueOf(required));
            player.sendMessage(colorize(msg));
        }
    }
    
    private String colorize(String msg) {
        return msg.replace("&", "§");
    }
}