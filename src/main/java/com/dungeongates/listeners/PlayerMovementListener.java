package com.dungeongates.listeners;

import com.dungeongates.DatabaseManager;
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
import org.bukkit.event.player.PlayerChangedWorldEvent;
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
        
        // Check if player changed worlds (e.g., portal)
        if (lastWorld != null && !lastWorld.equals(currentWorld)) {
            // Player left the dungeon world entirely - reset progress
            handleDungeonWorldExit(player, lastWorld, lastRegion);
        }
        
        // Update progress manager with current room
        progressManager.setCurrentRoom(player.getUniqueId(), currentKey);
        
        // CHECK: Player EXITING a dungeon region (was in one, now in different or none)
        if (lastKey != null && !lastKey.equals(currentKey)) {
            handleRegionExit(player, lastWorld, lastRegion, currentWorld, currentRegion, event.getFrom());
        }
        
        // CHECK: Player entering a dungeon region (was outside, now inside)
        else if (currentKey != null && lastKey == null) {
            handleRegionEntry(player, currentWorld, currentRegion, event.getFrom());
        }
        
        // Player moved between two different dungeon regions
        else if (currentKey != null && lastKey != null && !currentKey.equals(lastKey)) {
            // Exiting one room, entering another - check BOTH exit and entry
            handleRegionExit(player, lastWorld, lastRegion, currentWorld, currentRegion, event.getFrom());
            // Also check entry into the new room (allows returning to completed rooms)
            handleRegionEntry(player, currentWorld, currentRegion, event.getFrom());
        }
        
        // Update cache
        if (currentKey != null) {
            lastKnownRegion.put(player.getUniqueId(), currentKey);
        } else {
            lastKnownRegion.remove(player.getUniqueId());
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerChangedWorld(@NotNull PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String fromWorld = event.getFrom().getName();
        
        // Check if player was in a dungeon region in the old world
        String lastKey = lastKnownRegion.get(player.getUniqueId());
        if (lastKey != null && lastKey.contains(":")) {
            String[] parts = lastKey.split(":", 2);
            String lastWorld = parts[0];
            String lastRegion = parts[1];
            
            if (lastWorld.equals(fromWorld) && roomManager.isRegisteredRegion(lastRegion, lastWorld)) {
                // Player left the dungeon world - reset progress
                handleDungeonWorldExit(player, lastWorld, lastRegion);
            }
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
     * Handle player leaving the dungeon world entirely (portal, world change).
     * Resets all progress.
     */
    private void handleDungeonWorldExit(@NotNull Player player, @NotNull String lastWorld, @NotNull String lastRegion) {
        debug(player, "Left dungeon world (" + lastWorld + ") - resetting all progress");
        
        // Send message
        String msg = plugin.getConfigManager().getPrefixedMessage("progress-reset-world-exit");
        if (msg != null) {
            player.sendMessage(colorize(msg));
        }
        
        // Clear all progress
        progressManager.resetProgress(player.getUniqueId());
        lastKnownRegion.remove(player.getUniqueId());
    }
    
    /**
     * Handle player EXITING a dungeon region.
     * Prevents leaving a room before completing required kills.
     * First room (order 0) always allowed. Last room requires completion like any other room.
     */
    private void handleRegionExit(@NotNull Player player, @Nullable String fromWorld, @Nullable String fromRegion, 
                                    @Nullable String toWorld, @Nullable String toRegion, 
                                    @NotNull Location fromLocation) {
        if (fromRegion == null || fromWorld == null) return;
        
        // Admin bypass
        if (player.hasPermission("dungeongates.bypass")) {
            debug(player, "Admin bypass - allowing exit");
            return;
        }
        
        Room fromRoom = roomManager.getRoom(fromRegion, fromWorld);
        if (fromRoom == null) return; // Not a dungeon room
        
        debug(player, "Attempting to exit room: " + fromRoom.getUniqueKey() + " (order: " + fromRoom.getOrder() + ")");
        
        // First room (order 0) - always allow exit
        if (fromRoom.getOrder() == 0) {
            debug(player, "First room - allowing exit");
            return;
        }
        
        // Check if the room is completed - USE SYNC LOAD for critical check
        DatabaseManager.RoomProgressData data = progressManager.getDatabaseManager().loadRoomProgressSync(player.getUniqueId(), fromRoom.getUniqueKey());
        boolean completed = data != null && data.completed;
        
        if (completed) {
            debug(player, "Room completed - allowing exit");
            return;
        }
        
        // Room not completed - DENY EXIT (applies to ALL rooms including last room)
        debug(player, "Room NOT completed - denying exit (kills: " + (data != null ? data.kills : 0) + "/" + fromRoom.getRequiredKills() + ")");
        handleFailedExit(player, fromRoom, fromLocation);
    }
    
    /**
     * Handle teleport exit from dungeon region.
     */
    private void handleTeleportExit(@NotNull Player player, @NotNull String fromWorld, @NotNull String fromRegion, 
                                     @NotNull PlayerTeleportEvent event) {
        // Admin bypass
        if (player.hasPermission("dungeongates.bypass")) return;
        
        Room fromRoom = roomManager.getRoom(fromRegion, fromWorld);
        if (fromRoom == null) return;
        
        // First room - always allow
        if (fromRoom.getOrder() == 0) return;
        
        // Check completion - USE SYNC LOAD
        DatabaseManager.RoomProgressData data = progressManager.getDatabaseManager().loadRoomProgressSync(player.getUniqueId(), fromRoom.getUniqueKey());
        boolean completed = data != null && data.completed;
        
        if (completed) return;
        
        // Not completed - cancel teleport and knockback
        event.setCancelled(true);
        handleFailedExit(player, fromRoom, event.getFrom());
    }
    
    /**
     * Handle player ENTERING a dungeon region.
     * First room always allowed. Second+ rooms require previous room completion.
     * ALSO allows entry if target room itself is already completed (returning to completed rooms).
     */
    private void handleRegionEntry(@NotNull Player player, @NotNull String currentWorld, @NotNull String currentRegion, 
                                    @NotNull Location fromLocation) {
        // Admin bypass
        if (player.hasPermission("dungeongates.bypass")) {
            debug(player, "Admin bypass - allowing entry");
            return;
        }
        
        Room newRoom = roomManager.getRoom(currentRegion, currentWorld);
        if (newRoom == null) return;
        
        // First room always allowed
        if (newRoom.getOrder() == 0) {
            sendProgressMessage(player, newRoom);
            return;
        }
        
        // Check if TARGET room itself is already completed (allows returning to completed rooms)
        DatabaseManager.RoomProgressData targetData = progressManager.getDatabaseManager().loadRoomProgressSync(player.getUniqueId(), newRoom.getUniqueKey());
        if (targetData != null && targetData.completed) {
            debug(player, "Target room already completed - allowing entry");
            sendProgressMessage(player, newRoom);
            return;
        }
        
        // Check if previous room is completed (for progressing forward)
        Room previousRoom = roomManager.getPreviousRoom(currentRegion, currentWorld);
        if (previousRoom != null) {
            // Use sync load for previous room check too
            DatabaseManager.RoomProgressData prevData = progressManager.getDatabaseManager().loadRoomProgressSync(player.getUniqueId(), previousRoom.getUniqueKey());
            if (prevData == null || !prevData.completed) {
                // Previous room not completed - DENY ENTRY
                debug(player, "Previous room not completed - denying entry to " + newRoom.getUniqueKey());
                handleFailedEntry(player, previousRoom, newRoom, fromLocation);
                return;
            }
        }
        
        // Previous room completed - allow entry (progressing forward)
        sendProgressMessage(player, newRoom);
    }
    
    private void handleFailedEntry(@NotNull Player player, @NotNull Room fromRoom, 
                                    @NotNull Room toRoom, @NotNull Location fromLocation) {
        PlayerProgress progress = progressManager.getProgress(player.getUniqueId());
        RoomProgress fromProgress = progress.getRoomProgress(fromRoom.getUniqueKey());
        
        int currentKills = fromProgress != null ? fromProgress.getKills() : 0;
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
        
        // Knockback player back to previous room
        knockbackPlayer(player, fromLocation);
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
        // Calculate knockback vector toward the room center
        Vector toRoom = fromLocation.toVector().subtract(player.getLocation().toVector());
        
        // If no direction (same location or zero vector), push backward relative to player facing
        Vector knockback;
        if (toRoom.length() < 0.1) {
            knockback = player.getLocation().getDirection().multiply(-1);
        } else {
            knockback = toRoom.normalize();
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