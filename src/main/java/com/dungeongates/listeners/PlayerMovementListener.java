package com.dungeongates.listeners;

import com.dungeongates.DungeonGatesPlugin;
import com.dungeongates.dungeon.DungeonRoom;
import com.dungeongates.dungeon.PlayerProgress;
import com.dungeongates.dungeon.ProgressManager;
import com.dungeongates.dungeon.RoomManager;
import com.dungeongates.dungeon.RoomProgress;
import com.dungeongates.integrations.WorldGuardHook;
import com.dungeongates.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerMovementListener implements Listener {
    
    private final DungeonGatesPlugin plugin;
    private final ProgressManager progressManager;
    private final RoomManager roomManager;
    private final WorldGuardHook worldGuardHook;
    private final ConfigManager.FailedProgressionConfig failedProgression;
    
    // Cache player's last known room to avoid redundant checks
    private final Map<UUID, String> lastKnownRoom = new java.util.concurrent.ConcurrentHashMap<>();
    
    public PlayerMovementListener(@NotNull DungeonGatesPlugin plugin, @NotNull ProgressManager progressManager, 
                                   @NotNull RoomManager roomManager, @NotNull WorldGuardHook worldGuardHook) {
        this.plugin = plugin;
        this.progressManager = progressManager;
        this.roomManager = roomManager;
        this.worldGuardHook = worldGuardHook;
        this.failedProgression = plugin.getConfigManager().getFailedProgression();
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Only check if player actually changed blocks
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        // Get current room at new location
        DungeonRoom currentRoom = worldGuardHook.getRoomAt(player.getLocation());
        String currentRoomName = currentRoom != null ? currentRoom.getName() : null;
        
        // Update progress manager with current room
        progressManager.setCurrentRoom(player.getUniqueId(), currentRoomName);
        
        // Check if player entered a new dungeon room
        String lastRoom = lastKnownRoom.get(player.getUniqueId());
        
        if (currentRoomName != null && !currentRoomName.equals(lastRoom)) {
            // Player entered a new room
            handleRoomEntry(player, currentRoom, lastRoom, event.getFrom());
        } else if (currentRoomName == null && lastRoom != null) {
            // Player left all dungeon rooms
            handleDungeonExit(player, lastRoom);
        }
        
        lastKnownRoom.put(player.getUniqueId(), currentRoomName);
    }
    
    private void handleRoomEntry(@NotNull Player player, @NotNull DungeonRoom newRoom, @Nullable String previousRoomName, @NotNull Location fromLocation) {
        // Check if this is the first room (order 0) - always allow
        if (newRoom.getOrder() == 0) {
            sendProgressMessage(player, newRoom);
            return;
        }
        
        // Check if coming from previous room in sequence
        if (previousRoomName != null) {
            DungeonRoom previousRoom = roomManager.getRoom(previousRoomName);
            if (previousRoom != null && roomManager.getNextRoom(previousRoomName) == newRoom) {
                // Player is trying to progress to next room
                if (!progressManager.canEnterRoom(player.getUniqueId(), newRoom.getName())) {
                    // Requirements not met - push back
                    handleFailedProgression(player, previousRoom, newRoom, fromLocation);
                    return;
                }
            }
        }
        
        // Allow entry (either not progressing sequentially, or requirements met)
        sendProgressMessage(player, newRoom);
    }
    
    private void handleDungeonExit(@NotNull Player player, @NotNull String previousRoomName) {
        // Player completely left the dungeon
        // Could trigger progress reset if configured
    }
    
    private void handleFailedProgression(@NotNull Player player, @NotNull DungeonRoom fromRoom, @NotNull DungeonRoom toRoom, @NotNull Location fromLocation) {
        PlayerProgress progress = progressManager.getProgress(player.getUniqueId());
        RoomProgress roomProgress = progress.getProgress(fromRoom.getName());
        
        int currentKills = roomProgress != null ? roomProgress.getTotalKills() : 0;
        int requiredKills = fromRoom.getRequiredKills();
        int remaining = Math.max(0, requiredKills - currentKills);
        
        Map<String, String> placeholders = Map.of(
            "player", player.getName(),
            "room", fromRoom.getName(),
            "current_room", fromRoom.getName(),
            "next_room", toRoom.getName(),
            "current_kills", String.valueOf(currentKills),
            "required_kills", String.valueOf(requiredKills),
            "remaining_kills", String.valueOf(remaining),
            "progress_percent", String.format("%.1f", requiredKills > 0 ? (currentKills * 100.0 / requiredKills) : 0)
        );
        
        // Send denial messages
        List<String> messages = plugin.getConfigManager().getMessage("requirements-not-met");
        for (String msg : messages) {
            MessageUtil.send(player, msg, placeholders);
        }
        
        // Apply configured action
        switch (failedProgression.action) {
            case "VELOCITY" -> applyVelocity(player, fromRoom);
            case "TELEPORT" -> applyTeleport(player, fromRoom);
            case "KNOCKBACK" -> applyKnockback(player, fromRoom);
            case "CANCEL" -> {
                // Just cancel movement - player stays at boundary
                player.teleport(fromLocation);
            }
        }
    }
    
    private void applyVelocity(@NotNull Player player, @NotNull DungeonRoom room) {
        // Calculate push direction back into the room
        Location center = plugin.getWorldGuardHook().getRegionCenter(room.getRegion());
        if (center == null) return;
        
        Vector direction = player.getLocation().toVector().subtract(center.toVector()).normalize();
        
        Vector velocity = new Vector(
            direction.getX() * failedProgression.velocityHorizontal,
            failedProgression.velocityVertical,
            direction.getZ() * failedProgression.velocityHorizontal
        );
        
        player.setVelocity(velocity);
    }
    
    private void applyTeleport(@NotNull Player player, @NotNull DungeonRoom room) {
        if (failedProgression.teleportUseLastLocation) {
            // Try to find a safe spot in the previous room
            Location center = plugin.getWorldGuardHook().getRegionCenter(room.getRegion());
            if (center != null) {
                player.teleport(center);
            }
        }
    }
    
    private void applyKnockback(@NotNull Player player, @NotNull DungeonRoom room) {
        Location center = plugin.getWorldGuardHook().getRegionCenter(room.getRegion());
        if (center == null) return;
        
        Vector direction = player.getLocation().toVector().subtract(center.toVector()).normalize();
        
        Vector velocity = new Vector(
            direction.getX() * failedProgression.knockbackHorizontal,
            failedProgression.knockbackVertical,
            direction.getZ() * failedProgression.knockbackHorizontal
        );
        
        player.setVelocity(velocity);
    }
    
    private void sendProgressMessage(@NotNull Player player, @NotNull DungeonRoom room) {
        PlayerProgress progress = progressManager.getProgress(player.getUniqueId());
        RoomProgress roomProgress = progress.getProgress(room.getName());
        
        int currentKills = roomProgress != null ? roomProgress.getTotalKills() : 0;
        int requiredKills = room.getRequiredKills();
        int remaining = Math.max(0, requiredKills - currentKills);
        double percent = requiredKills > 0 ? (currentKills * 100.0 / requiredKills) : 100.0;
        
        Map<String, String> placeholders = Map.of(
            "player", player.getName(),
            "room", room.getName(),
            "current_room", room.getName(),
            "current_kills", String.valueOf(currentKills),
            "required_kills", String.valueOf(requiredKills),
            "remaining_kills", String.valueOf(remaining),
            "progress_percent", String.format("%.1f", percent)
        );
        
        // Only send progress message occasionally or on room entry
        // For now, send on every room entry
        List<String> messages = plugin.getConfigManager().getMessage("room-progress");
        for (String msg : messages) {
            MessageUtil.send(player, msg, placeholders);
        }
    }
}