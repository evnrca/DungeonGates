package com.dungeongates;

import com.dungeongates.commands.DungeonGatesCommand;
import com.dungeongates.config.ConfigManager;
import com.dungeongates.dungeon.ProgressManager;
import com.dungeongates.dungeon.RoomManager;
import com.dungeongates.integrations.MythicMobsHook;
import com.dungeongates.integrations.PlaceholderAPIHook;
import com.dungeongates.integrations.WorldGuardHook;
import com.dungeongates.listeners.MythicMobKillListener;
import com.dungeongates.listeners.PlayerMovementListener;
import com.dungeongates.listeners.PlayerQuitListener;
import com.dungeongates.utils.ColorUtil;
import com.dungeongates.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public final class DungeonGatesPlugin extends JavaPlugin {
    
    private static DungeonGatesPlugin instance;
    
    private ConfigManager configManager;
    private WorldGuardHook worldGuardHook;
    private MythicMobsHook mythicMobsHook;
    private PlaceholderAPIHook placeholderAPIHook;
    private RoomManager roomManager;
    private ProgressManager progressManager;
    private MessageUtil messageUtil;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize utilities
        this.messageUtil = new MessageUtil();
        
        // Load configuration
        this.configManager = new ConfigManager(this);
        configManager.load();
        
        // Initialize integrations
        this.worldGuardHook = new WorldGuardHook(this);
        if (!worldGuardHook.initialize()) {
            getLogger().severe("Failed to initialize WorldGuard integration. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        this.mythicMobsHook = new MythicMobsHook(this);
        if (!mythicMobsHook.initialize()) {
            getLogger().severe("Failed to initialize MythicMobs integration. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize managers
        this.roomManager = new RoomManager(this, configManager);
        this.progressManager = new ProgressManager(this, configManager, roomManager);
        
        // Load rooms
        roomManager.loadRooms();
        
        // Register commands
        DungeonGatesCommand command = new DungeonGatesCommand(this, roomManager, progressManager);
        getCommand("dg").setExecutor(command);
        getCommand("dg").setTabCompleter(command);
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerMovementListener(this, progressManager, roomManager, worldGuardHook), this);
        getServer().getPluginManager().registerEvents(new MythicMobKillListener(this, progressManager, mythicMobsHook, worldGuardHook), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this, progressManager), this);
        
        // Initialize PlaceholderAPI if available
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.placeholderAPIHook = new PlaceholderAPIHook(this);
            this.placeholderAPIHook.register();
            getLogger().info("PlaceholderAPI integration enabled.");
        }
        
        // Validate configuration
        validateConfiguration();
        
        getLogger().info("DungeonGates v" + getPluginMeta().getVersion() + " enabled successfully!");
        getLogger().info("Loaded " + roomManager.getRoomCount() + " dungeon room(s).");
    }
    
    @Override
    public void onDisable() {
        if (placeholderAPIHook != null) {
            placeholderAPIHook.unregister();
        }
        
        // Save any pending data
        configManager.save();
        
        getLogger().info("DungeonGates disabled.");
    }
    
    public void reloadPlugin() {
        configManager.reload();
        roomManager.loadRooms();
        validateConfiguration();
        getLogger().info("Configuration reloaded.");
    }
    
    private void validateConfiguration() {
        // Check for invalid rooms
        for (DungeonRoom room : roomManager.getAllRooms()) {
            if (!room.hasRegion()) {
                getLogger().warning("Room '" + room.getName() + "' references non-existent WorldGuard region: " + room.getRegionName());
            }
            
            if (room.getRequiredKills() < 1) {
                getLogger().warning("Room '" + room.getName() + "' has invalid required kills: " + room.getRequiredKills());
            }
        }
        
        // Check for duplicate regions
        Set<String> seenRegions = new java.util.HashSet<>();
        for (DungeonRoom room : roomManager.getAllRooms()) {
            if (!seenRegions.add(room.getRegionName())) {
                getLogger().warning("Duplicate WorldGuard region detected: " + room.getRegionName() + " (used by room: " + room.getName() + ")");
            }
        }
    }
    
    public static @NotNull DungeonGatesPlugin getInstance() {
        return instance;
    }
    
    public @NotNull ConfigManager getConfigManager() {
        return configManager;
    }
    
    public @NotNull WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }
    
    public @NotNull MythicMobsHook getMythicMobsHook() {
        return mythicMobsHook;
    }
    
    public @NotNull RoomManager getRoomManager() {
        return roomManager;
    }
    
    public @NotNull ProgressManager getProgressManager() {
        return progressManager;
    }
    
    public @NotNull MessageUtil getMessageUtil() {
        return messageUtil;
    }
}