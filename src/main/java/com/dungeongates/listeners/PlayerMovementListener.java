package com.dungeongates.listeners;

import com.dungeongates.DungeonGatesPlugin;
import com.dungeongates.ProgressManager;
import com.dungeongates.Room;
import com.dungeongates.RoomManager;
import com.dungeongates.RoomProgress;
import com.dungeongates.PlayerProgress;
import com.dungeongates.hooks.WorldGuardHook;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public final class PlayerMovementListener implements Listener {
    
    private final DungeonGatesPlugin plugin;
    private final ProgressManager progressManager;
    private final RoomManager roomManager;
    private final WorldGuardHook worldGuardHook;
    
    // Cache player's last known region
    private final Map<UUID, String> lastKnownRegion = new java.util.concurrent.ConcurrentHashMap<>();
    
    public PlayerMovementListener(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
        this.progressManager = plugin.getProgressManager();
        this.roomManager = plugin.getRoomManager();
        this.worldGuardHook = plugin.getWorldGuardHook();
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Only check if player changed blocks
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        // Get current region
        String currentRegion = worldGuardHook.getRegionAt(player.getLocation());
        String lastRegion = lastKnownRegion.get(player.getUniqueId());
        
        // Update progress manager
        progressManager.setCurrentRoom(player.getUniqueId(), currentRegion);
        
        // Player entered a new dungeon region
        if (currentRegion != null && !currentRegion.equals(lastRegion)) {
            handleRegionEntry(player, currentRegion, lastRegion, event.getFrom());
        } else if (currentRegion == null && lastRegion != null) {
            // Player left all dungeon regions
        }
        
        lastKnownRegion.put(player.getUniqueId(), currentRegion);
    }
    
    private void handleRegionEntry(@NotNull Player player, @NotNull String newRegion, 
                                    @Nullable String previousRegion, @NotNull Location fromLocation) {
        Room newRoom = roomManager.getRoom(newRegion);
        if (newRoom == null) return; // Not a dungeon room
        
        // First room is always accessible
        if (newRoom.getOrder() == 0) {
            sendProgressMessage(player, newRoom);
            return;
        }
        
        // Check if coming from previous room in sequence
        if (previousRegion != null) {
            Room previousRoom = roomManager.getRoom(previousRegion);
            if (previousRoom != null && roomManager.getNextRoom(previousRegion) == newRoom) {
                // Player trying to progress to next room
                if (!progressManager.canEnterRoom(player.getUniqueId(), newRegion)) {
                    // Requirements not met - deny entry
                    handleFailedEntry(player, previousRoom, newRoom, fromLocation);
                    return;
                }
            }
        }
        
        // Allow entry (not progressing sequentially, or requirements met)
        sendProgressMessage(player, newRoom);
    }
    
    private void handleFailedEntry(@NotNull Player player, @NotNull Room fromRoom, 
                                    @NotNull Room toRoom, @NotNull Location fromLocation) {
        PlayerProgress progress = progressManager.getProgress(player.getUniqueId());
        RoomProgress fromProgress = progress.getRoomProgress(fromRoom.getRegion());
        
        int currentKills = fromProgress != null ? fromProgress.getKills() : 0;
        int requiredKills = fromRoom.getRequiredKills();
        int remaining = Math.max(0, requiredKills - currentKills);
        
        // Send denial message
        String msg = plugin.getConfigManager().getPrefixedMessage("requirement-not-met");
        msg = msg.replace("{remaining}", String.valueOf(remaining))
                 .replace("{region}", fromRoom.getRegion());
        player.sendMessage(colorize(msg));
        
        // Apply configured action
        String action = plugin.getConfigManager().getFailedEntryAction();
        
        switch (action) {
            case "VELOCITY" -> applyVelocity(player, fromRoom);
            case "TELEPORT" -> applyTeleport(player, fromRoom);
            case "CANCEL" -> player.teleport(fromLocation);
            case "KNOCKBACK" -> applyKnockback(player, fromRoom);
        }
    }
    
    private void applyVelocity(@NotNull Player player, @NotNull Room room) {
        Location center = plugin.getWorldGuardHook().getRegionCenter(room.getRegion());
        if (center == null) return;
        
        Vector direction = player.getLocation().toVector().subtract(center.toVector()).normalize();
        
        Vector velocity = new Vector(
            direction.getX() * plugin.getConfigManager().getVelocityHorizontal(),
            plugin.getConfigManager().getVelocityVertical(),
            direction.getZ() * plugin.getConfigManager().getVelocityHorizontal()
        );
        
        player.setVelocity(velocity);
    }
    
    private void applyTeleport(@NotNull Player player, @NotNull Room room) {
        Location center = plugin.getWorldGuardHook().getRegionCenter(room.getRegion());
        if (center != null) {
            player.teleport(center);
        }
    }
    
    private void applyKnockback(@NotNull Player player, @NotNull Room room) {
        Location center = plugin.getWorldGuardHook().getRegionCenter(room.getRegion());
        if (center == null) return;
        
        Vector direction = player.getLocation().toVector().subtract(center.toVector()).normalize();
        
        Vector velocity = new Vector(
            direction.getX() * 1.0,
            0.3,
            direction.getZ() * 1.0
        );
        
        player.setVelocity(velocity);
    }
    
    private void sendProgressMessage(@NotNull Player player, @NotNull Room room) {
        PlayerProgress progress = progressManager.getProgress(player.getUniqueId());
        RoomProgress roomProgress = progress.getRoomProgress(room.getRegion());
        
        int current = roomProgress != null ? roomProgress.getKills() : 0;
        int required = room.getRequiredKills();
        
        String msg = plugin.getConfigManager().getPrefixedMessage("progress");
        msg = msg.replace("{current}", String.valueOf(current))
                 .replace("{required}", String.valueOf(required));
        player.sendMessage(colorize(msg));
    }
    
    private String colorize(String msg) {
        return msg.replace("&", "§");
    }
}