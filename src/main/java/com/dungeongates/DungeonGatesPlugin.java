package com.dungeongates;

import com.dungeongates.commands.DungeonGatesCommand;
import com.dungeongates.hooks.WorldGuardHook;
import com.dungeongates.hooks.MythicMobsHook;
import com.dungeongates.listeners.PlayerMovementListener;
import com.dungeongates.listeners.MythicMobKillListener;
import com.dungeongates.listeners.PlayerDeathListener;
import com.dungeongates.listeners.PlayerQuitListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.logging.Level;

public final class DungeonGatesPlugin extends JavaPlugin {
    
    private static DungeonGatesPlugin instance;
    
    private WorldGuardHook worldGuardHook;
    private MythicMobsHook mythicMobsHook;
    private RoomManager roomManager;
    private ProgressManager progressManager;
    private ConfigManager configManager;
    private boolean debugMode = false;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Load configuration
        configManager = new ConfigManager(this);
        configManager.load();
        
        // Initialize hooks
        worldGuardHook = new WorldGuardHook(this);
        if (!worldGuardHook.initialize()) {
            getLogger().severe("WorldGuard hook failed! Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        mythicMobsHook = new MythicMobsHook(this);
        if (!mythicMobsHook.initialize()) {
            getLogger().severe("MythicMobs hook failed! Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize managers
        roomManager = new RoomManager(this);
        progressManager = new ProgressManager(this);
        
        // Load rooms
        roomManager.load();
        
        // Register commands
        DungeonGatesCommand command = new DungeonGatesCommand(this);
        getCommand("dg").setExecutor(command);
        getCommand("dg").setTabCompleter(command);
        
        // Register listeners
        Bukkit.getPluginManager().registerEvents(new PlayerMovementListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MythicMobKillListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        
        getLogger().info("DungeonGates v" + getPluginMeta().getVersion() + " enabled!");
        getLogger().info("Loaded " + roomManager.getRoomCount() + " dungeon room(s).");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("DungeonGates disabled.");
    }
    
    public void reload() {
        configManager.load();
        roomManager.load();
        getLogger().info("Configuration reloaded.");
    }
    
    public static @NotNull DungeonGatesPlugin getInstance() {
        return instance;
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
    
    public @NotNull ConfigManager getConfigManager() {
        return configManager;
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
}