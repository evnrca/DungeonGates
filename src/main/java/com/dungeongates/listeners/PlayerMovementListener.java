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
        
        // Get previous region from cache
        String lastKey = lastKnownRegion.get(player.getUniqueId());
        String lastRegion = null;
        String lastWorld = null;
        if (lastKey != null && lastKey.contains(":")) {
            String[] parts = lastKey.split(":", 2);
            lastWorld = parts[0];
            lastRegion = parts[1];
        }
        
        // Update progress manager with current room
        progressManager.setCurrentRoom(player.getUniqueId(), currentKey);
        
        // CHECK: Player EXITING a dungeon region (was in one, now in different or none)
        if (lastKey != null && !lastKey.equals(currentKey)) {
            handleRegionExit(player, lastWorld, lastRegion, currentWorld, currentRegion, event.getFrom());
        }
        
        // CHECK: Player entering a dungeon region (was outside, now inside)
        // For entry, we only need to allow/deny based on sequence (first room always allowed, next room requires completion)
        else if (currentKey != null && lastKey == null) {
            handleRegionEntry(player, currentWorld, currentRegion, event.getFrom());
        }
        
        // Player moved between two different dungeon regions
        else if (currentKey != null && lastKey != null && !currentKey.equals(lastKey)) {
            // Exiting one room, entering another - check exit from previous first
            handleRegionExit(player, lastWorld, lastRegion, currentWorld, currentRegion, event.getFrom());
        }
        
        // Update cache
        if (currentKey != null) {
            lastKnownRegion.put(player.getUniqueId(), currentKey);
        } else {
            lastKnownRegion.remove(player.getUniqueId());
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(@NotNull PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        String fromWorld = event.getFrom().getWorld().getName();
        String fromRegion = worldGuardHook.getRegionAt(event.getFrom());
        String toWorld = event.getTo().getWorld().getName();
        String toRegion = worldGuardHook.getRegionAt(event.getTo());
        
        // If teleporting from a dungeon region to outside, check exit requirements
        if (fromRegion != null && roomManager.isRegisteredRegion(fromRegion, fromWorld)) {
            if (toRegion == null || !roomManager.isRegisteredRegion(toRegion, toWorld)) {
                handleTeleportExit(player, fromWorld, fromRegion, event);
            }
        }
    }
    
    /**
     * Handle player EXITING a dungeon region.
     * Prevents leaving a room before completing required kills.
     */
    private void handleRegionExit(@NotNull Player player, @Nullable String fromWorld, @Nullable String fromRegion, 
                                   @Nullable String toWorld, @Nullable String toRegion, 
                                   @NotNull Location fromLocation) {
        if (fromRegion == null || fromWorld == null) return;
        
        Room fromRoom = roomManager.getRoom(fromRegion, fromWorld);
        if (fromRoom == null) return; // Not a dungeon room
        
        debug(player, "Attempting to exit room: " + fromRoom.getUniqueKey() + " (order: " + fromRoom.getOrder() + ")");
        
        // First room (order 0) - always allow exit
        if (fromRoom.getOrder() == 0) {
            debug(player, "First room - allowing exit");
            return;
        }
        
        // Check if the room is completed
        PlayerProgress progress = progressManager.getProgress(player.getUniqueId());
        RoomProgress roomProgress = progress.getRoomProgress(fromRoom.getUniqueKey());
        
        if (roomProgress != null && roomProgress.isCompleted()) {
            debug(player, "Room completed - allowing exit");
            return;
        }
        
        // Room not completed - DENY EXIT
        debug(player, "Room NOT completed - denying exit (kills: " + (roomProgress != null ? roomProgress.getKills() : 0) + "/" + fromRoom.getRequiredKills() + ")");
        handleFailedExit(player, fromRoom, fromLocation);
    }
    
    /**
     * Handle teleport exit from dungeon region.
     */
    private void handleTeleportExit(@NotNull Player player, @NotNull String fromWorld, @NotNull String fromRegion, 
                                     @NotNull PlayerTeleportEvent event) {
        Room fromRoom = roomManager.getRoom(fromRegion, fromWorld);
        if (fromRoom == null) return;
        
        // First room - always allow
        if (fromRoom.getOrder() == 0) return;
        
        // Check completion
        PlayerProgress progress = progressManager.getProgress(player.getUniqueId());
        RoomProgress roomProgress = progress.getRoomProgress(fromRoom.getUniqueKey());
        
        if (roomProgress != null && roomProgress.isCompleted()) return;
        
        // Not completed - cancel teleport and knockback
        event.setCancelled(true);
        handleFailedExit(player, fromRoom, event.getFrom());
    }
    
    /**
     * Handle player ENTERING a dungeon region.
     * First room always allowed. Next room in sequence requires previous completion.
     */
    private void handleRegionEntry(@NotNull Player player, @NotNull String currentWorld, @NotNull String currentRegion, 
                                    @NotNull Location fromLocation) {
        Room newRoom = roomManager.getRoom(currentRegion, currentWorld);
        if (newRoom == null) return;
        
        // First room always allowed
        if (newRoom.getOrder() == 0) {
            sendProgressMessage(player, newRoom);
            return;
        }
        
        // For entry to next rooms, we rely on the exit check from the previous room
        // If they managed to enter, it means they either:
        // 1. Completed the previous room (exit was allowed)
        // 2. Are entering from outside (should be caught by exit check when they try to leave)
        // Just show progress
        sendProgressMessage(player, newRoom);
    }
    
    private void handleFailedExit(@NotNull Player player, @NotNull Room fromRoom, @NotNull Location fromLocation) {
        PlayerProgress progress = progressManager.getProgress(player.getUniqueId());
        RoomProgress roomProgress = progress.getRoomProgress(fromRoom.getUniqueKey());
        
        int currentKills = roomProgress != null ? roomProgress.getKills() : 0;
        int requiredKills = fromRoom.getRequiredKills();
        int remaining = Math.max(0, requiredKills - currentKills);
        
        // Send title message (screen title + subtitle)
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
        
        // Knockback player back into the room
        knockbackPlayer(player, fromLocation);
    }
    
    private void knockbackPlayer(@NotNull Player player, @NotNull Location fromLocation) {
        // Cancel movement and apply knockback back into the room
        Vector knockback = fromLocation.toVector().subtract(player.getLocation().toVector()).normalize();
        
        // If no direction (same location), push backward
        if (knockback.length() < 0.1) {
            knockback = player.getLocation().getDirection().multiply(-1);
        }
        
        // Apply knockback - horizontal + slight vertical
        knockback.setY(0.4);
        player.setVelocity(knockback.multiply(1.5));
        
        // Schedule a 1-tick check to re-apply if player still trying to leave
        final Vector finalKnockback = knockback;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.getWorld().equals(fromLocation.getWorld())) {
                String currentRegion = worldGuardHook.getRegionAt(player.getLocation());
                if (currentRegion != null && roomManager.isRegisteredRegion(currentRegion, player.getWorld().getName())) {
                    Room current = roomManager.getRoom(currentRegion, player.getWorld().getName());
                    if (current != null && current.getOrder() > 0) {
                        PlayerProgress progress = progressManager.getProgress(player.getUniqueId());
                        RoomProgress roomProgress = progress.getRoomProgress(current.getUniqueKey());
                        if (roomProgress == null || !roomProgress.isCompleted()) {
                            player.setVelocity(finalKnockback);
                        }
                    }
                }
            }
        }, 1L);
    }
    
    private void sendProgressMessage(@NotNull Player player, @NotNull Room room) {
        PlayerProgress progress = progressManager.getProgress(player.getUniqueId());
        RoomProgress roomProgress = progress.getRoomProgress(room.getUniqueKey());
        
        int current = roomProgress != null ? roomProgress.getKills() : 0;
        int required = room.getRequiredKills();
        
        // Only show progress if not completed
        if (roomProgress == null || !roomProgress.isCompleted()) {
            String msg = plugin.getConfigManager().getPrefixedMessage("progress");
            msg = msg.replace("{current}", String.valueOf(current))
                     .replace("{required}", String.valueOf(required));
            player.sendMessage(colorize(msg));
        }
    }
    
    private void debug(@NotNull Player player, @NotNull String message) {
        if (plugin.isDebugMode()) {
            player.sendMessage(colorize("&7[DEBUG] &f" + message));
        }
    }
    
    private String colorize(String msg) {
        return msg.replace("&", "§");
    }
}