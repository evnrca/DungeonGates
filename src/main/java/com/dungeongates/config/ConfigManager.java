package com.dungeongates.config;

import com.dungeongates.DungeonGatesPlugin;
import com.dungeongates.dungeon.DungeonRoom;
import com.dungeongates.utils.ColorUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public final class ConfigManager {
    
    private final DungeonGatesPlugin plugin;
    private FileConfiguration config;
    private File configFile;
    
    // Settings
    private String progressMode;
    private FailedProgressionConfig failedProgression;
    private Map<String, List<String>> messages;
    private ProgressResetConfig progressReset;
    
    public ConfigManager(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        this.failedProgression = new FailedProgressionConfig();
        this.progressReset = new ProgressResetConfig();
    }
    
    public void load() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Load defaults from jar
        try (InputStream defaultStream = plugin.getResource("config.yml")) {
            if (defaultStream != null) {
                FileConfiguration defaults = YamlConfiguration.loadConfiguration(defaultStream);
                config.setDefaults(defaults);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not load default config", e);
        }
        
        parseConfig();
        validateConfig();
    }
    
    private void parseConfig() {
        // Settings
        progressMode = config.getString("settings.progress-mode", "INDIVIDUAL");
        
        // Failed progression
        ConfigurationSection fpSection = config.getConfigurationSection("failed-progression");
        if (fpSection != null) {
            failedProgression.action = fpSection.getString("action", "VELOCITY").toUpperCase();
            
            ConfigurationSection velSection = fpSection.getConfigurationSection("velocity");
            if (velSection != null) {
                failedProgression.velocityHorizontal = velSection.getDouble("horizontal", 1.5);
                failedProgression.velocityVertical = velSection.getDouble("vertical", 0.4);
            }
            
            ConfigurationSection tpSection = fpSection.getConfigurationSection("teleport");
            if (tpSection != null) {
                failedProgression.teleportUseLastLocation = tpSection.getBoolean("use-last-location", true);
            }
            
            ConfigurationSection kbSection = fpSection.getConfigurationSection("knockback");
            if (kbSection != null) {
                failedProgression.knockbackHorizontal = kbSection.getDouble("horizontal", 1.0);
                failedProgression.knockbackVertical = kbSection.getDouble("vertical", 0.3);
            }
        }
        
        // Messages
        ConfigurationSection msgSection = config.getConfigurationSection("messages");
        if (msgSection != null) {
            messages.clear();
            for (String key : msgSection.getKeys(false)) {
                Object value = msgSection.get(key);
                if (value instanceof List<?> list) {
                    messages.put(key, list.stream().map(Object::toString).toList());
                } else if (value instanceof String str) {
                    messages.put(key, List.of(str));
                }
            }
        }
        
        // Progress reset
        ConfigurationSection prSection = config.getConfigurationSection("progress-reset");
        if (prSection != null) {
            progressReset.onPlayerDeath = prSection.getBoolean("on-player-death", false);
            progressReset.onServerRestart = prSection.getBoolean("on-server-restart", false);
            progressReset.onDungeonExit = prSection.getBoolean("on-dungeon-exit", false);
            progressReset.onCompletion = prSection.getBoolean("on-completion", true);
        }
    }
    
    private void validateConfig() {
        // Validate progress mode
        if (!progressMode.equalsIgnoreCase("INDIVIDUAL") && !progressMode.equalsIgnoreCase("SHARED")) {
            plugin.getLogger().warning("Invalid progress-mode: " + progressMode + ". Defaulting to INDIVIDUAL.");
            progressMode = "INDIVIDUAL";
        }
        
        // Validate failed progression action
        Set<String> validActions = Set.of("VELOCITY", "TELEPORT", "CANCEL", "KNOCKBACK");
        if (!validActions.contains(failedProgression.action)) {
            plugin.getLogger().warning("Invalid failed-progression action: " + failedProgression.action + ". Defaulting to VELOCITY.");
            failedProgression.action = "VELOCITY";
        }
        
        // Validate velocity values
        if (failedProgression.velocityHorizontal < 0 || failedProgression.velocityHorizontal > 10) {
            plugin.getLogger().warning("Invalid velocity.horizontal value. Clamping to 1.5.");
            failedProgression.velocityHorizontal = 1.5;
        }
        if (failedProgression.velocityVertical < 0 || failedProgression.velocityVertical > 5) {
            plugin.getLogger().warning("Invalid velocity.vertical value. Clamping to 0.4.");
            failedProgression.velocityVertical = 0.4;
        }
    }
    
    public void save() {
        if (config == null || configFile == null) return;
        
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config.yml", e);
        }
    }
    
    public void reload() {
        load();
    }
    
    // Room management
    public @NotNull Map<String, DungeonRoom> loadRooms() {
        Map<String, DungeonRoom> rooms = new LinkedHashMap<>();
        ConfigurationSection roomsSection = config.getConfigurationSection("rooms");
        
        if (roomsSection != null) {
            int order = 0;
            for (String key : roomsSection.getKeys(false)) {
                ConfigurationSection roomSection = roomsSection.getConfigurationSection(key);
                if (roomSection == null) continue;
                
                String region = roomSection.getString("region", key);
                int requiredKills = roomSection.getInt("required-mythicmob-kills", 10);
                
                if (requiredKills < 1) {
                    plugin.getLogger().warning("Room " + key + " has invalid required-mythicmob-kills: " + requiredKills + ". Setting to 1.");
                    requiredKills = 1;
                }
                
                DungeonRoom room = new DungeonRoom(key, region, requiredKills);
                room.setOrder(order++);
                rooms.put(key, room);
            }
        }
        
        return rooms;
    }
    
    public void saveRoom(@NotNull DungeonRoom room) {
        String path = "rooms." + room.getName();
        config.set(path + ".region", room.getRegionName());
        config.set(path + ".required-mythicmob-kills", room.getRequiredKills());
        save();
    }
    
    public void removeRoom(@NotNull String roomName) {
        config.set("rooms." + roomName, null);
        save();
    }
    
    public void reorderRooms(@NotNull List<DungeonRoom> rooms) {
        config.set("rooms", null);
        for (int i = 0; i < rooms.size(); i++) {
            DungeonRoom room = rooms.get(i);
            room.setOrder(i);
            String path = "rooms." + room.getName();
            config.set(path + ".region", room.getRegionName());
            config.set(path + ".required-mythicmob-kills", room.getRequiredKills());
        }
        save();
    }
    
    // Getters
    public @NotNull String getProgressMode() {
        return progressMode;
    }
    
    public @NotNull FailedProgressionConfig getFailedProgression() {
        return failedProgression;
    }
    
    public @NotNull List<String> getMessage(@NotNull String key) {
        return messages.getOrDefault(key, List.of("Missing message: " + key));
    }
    
    public @NotNull String getMessageSingle(@NotNull String key) {
        List<String> list = getMessage(key);
        return list.isEmpty() ? "Missing message: " + key : list.get(0);
    }
    
    public @NotNull ProgressResetConfig getProgressReset() {
        return progressReset;
    }
    
    public @NotNull FileConfiguration getConfig() {
        return config;
    }
    
    public static final class FailedProgressionConfig {
        public String action = "VELOCITY";
        public double velocityHorizontal = 1.5;
        public double velocityVertical = 0.4;
        public boolean teleportUseLastLocation = true;
        public double knockbackHorizontal = 1.0;
        public double knockbackVertical = 0.3;
    }
    
    public static final class ProgressResetConfig {
        public boolean onPlayerDeath = false;
        public boolean onServerRestart = false;
        public boolean onDungeonExit = false;
        public boolean onCompletion = true;
    }
}