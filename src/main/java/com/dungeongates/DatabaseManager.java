package com.dungeongates;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class DatabaseManager {
    
    private final DungeonGatesPlugin plugin;
    private Connection connection;
    private final Map<String, PreparedStatement> preparedStatements = new ConcurrentHashMap<>();
    
    // SQL statements
    private static final String CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS dungeon_progress (
            uuid TEXT NOT NULL,
            room_key TEXT NOT NULL,
            kills INTEGER DEFAULT 0,
            completed INTEGER DEFAULT 0,
            last_updated INTEGER DEFAULT 0,
            PRIMARY KEY (uuid, room_key)
        )
        """;
    
    private static final String INSERT_OR_UPDATE = """
        INSERT INTO dungeon_progress (uuid, room_key, kills, completed, last_updated)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT(uuid, room_key) DO UPDATE SET
            kills = excluded.kills,
            completed = excluded.completed,
            last_updated = excluded.last_updated
        """;
    
    private static final String SELECT_PROGRESS = """
        SELECT room_key, kills, completed FROM dungeon_progress WHERE uuid = ?
        """;
    
    private static final String SELECT_ROOM_PROGRESS = """
        SELECT kills, completed FROM dungeon_progress WHERE uuid = ? AND room_key = ?
        """;
    
    private static final String DELETE_PROGRESS = """
        DELETE FROM dungeon_progress WHERE uuid = ?
        """;
    
    private static final String DELETE_ROOM_PROGRESS = """
        DELETE FROM dungeon_progress WHERE uuid = ? AND room_key = ?
        """;
    
    private static final String DELETE_ALL = """
        DELETE FROM dungeon_progress
        """;
    
    public DatabaseManager(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void initialize() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        File dbFile = new File(dataFolder, "progress.db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        
        try {
            connection = DriverManager.getConnection(url);
            connection.setAutoCommit(true);
            
            // Enable WAL mode for better concurrency
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute("PRAGMA synchronous=NORMAL;");
                stmt.execute("PRAGMA busy_timeout=5000;");
            }
            
            // Create table
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(CREATE_TABLE);
            }
            
            // Prepare statements
            preparedStatements.put("insertOrUpdate", connection.prepareStatement(INSERT_OR_UPDATE));
            preparedStatements.put("selectProgress", connection.prepareStatement(SELECT_PROGRESS));
            preparedStatements.put("selectRoomProgress", connection.prepareStatement(SELECT_ROOM_PROGRESS));
            preparedStatements.put("deleteProgress", connection.prepareStatement(DELETE_PROGRESS));
            preparedStatements.put("deleteRoomProgress", connection.prepareStatement(DELETE_ROOM_PROGRESS));
            preparedStatements.put("deleteAll", connection.prepareStatement(DELETE_ALL));
            
            plugin.getLogger().info("SQLite database initialized at " + dbFile.getAbsolutePath());
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite database", e);
        }
    }
    
    public void close() {
        // Close prepared statements
        for (PreparedStatement stmt : preparedStatements.values()) {
            try {
                stmt.close();
            } catch (SQLException ignored) {}
        }
        preparedStatements.clear();
        
        // Close connection
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error closing database", e);
            }
        }
    }
    
    // Async save player's room progress
    public CompletableFuture<Void> saveRoomProgress(@NotNull UUID uuid, @NotNull String roomKey, int kills, boolean completed) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement stmt = preparedStatements.get("insertOrUpdate");
                if (stmt == null) return;
                
                long timestamp = System.currentTimeMillis();
                stmt.setString(1, uuid.toString());
                stmt.setString(2, roomKey);
                stmt.setInt(3, kills);
                stmt.setInt(4, completed ? 1 : 0);
                stmt.setLong(5, timestamp);
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save progress for " + uuid + " room " + roomKey, e);
            }
        });
    }
    
    // Async load all progress for a player
    public CompletableFuture<Map<String, RoomProgressData>> loadProgress(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, RoomProgressData> result = new HashMap<>();
            try {
                PreparedStatement stmt = preparedStatements.get("selectProgress");
                if (stmt == null) return result;
                
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String roomKey = rs.getString("room_key");
                        int kills = rs.getInt("kills");
                        boolean completed = rs.getInt("completed") == 1;
                        result.put(roomKey, new RoomProgressData(kills, completed));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load progress for " + uuid, e);
            }
            return result;
        });
    }
    
    // Async load single room progress
    public CompletableFuture<RoomProgressData> loadRoomProgress(@NotNull UUID uuid, @NotNull String roomKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement stmt = preparedStatements.get("selectRoomProgress");
                if (stmt == null) return null;
                
                stmt.setString(1, uuid.toString());
                stmt.setString(2, roomKey);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int kills = rs.getInt("kills");
                        boolean completed = rs.getInt("completed") == 1;
                        return new RoomProgressData(kills, completed);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load room progress for " + uuid + " room " + roomKey, e);
            }
            return null;
        });
    }
    
    // Sync load single room progress (for critical checks)
    public @Nullable RoomProgressData loadRoomProgressSync(@NotNull UUID uuid, @NotNull String roomKey) {
        try {
            PreparedStatement stmt = preparedStatements.get("selectRoomProgress");
            if (stmt == null) return null;
            
            stmt.setString(1, uuid.toString());
            stmt.setString(2, roomKey);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int kills = rs.getInt("kills");
                    boolean completed = rs.getInt("completed") == 1;
                    return new RoomProgressData(kills, completed);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load room progress for " + uuid + " room " + roomKey, e);
        }
        return null;
    }
    
    // Async delete all progress for a player
    public CompletableFuture<Void> deleteProgress(@NotNull UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement stmt = preparedStatements.get("deleteProgress");
                if (stmt == null) return;
                
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete progress for " + uuid, e);
            }
        });
    }
    
    // Async delete single room progress
    public CompletableFuture<Void> deleteRoomProgress(@NotNull UUID uuid, @NotNull String roomKey) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement stmt = preparedStatements.get("deleteRoomProgress");
                if (stmt == null) return;
                
                stmt.setString(1, uuid.toString());
                stmt.setString(2, roomKey);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete room progress for " + uuid + " room " + roomKey, e);
            }
        });
    }
    
    // Async delete all progress (admin command)
    public CompletableFuture<Void> deleteAllProgress() {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement stmt = preparedStatements.get("deleteAll");
                if (stmt == null) return;
                
                stmt.executeUpdate();
                plugin.getLogger().info("All dungeon progress cleared from database");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete all progress", e);
            }
        });
    }
    
    // Synchronous version for shutdown
    public void saveRoomProgressSync(@NotNull UUID uuid, @NotNull String roomKey, int kills, boolean completed) {
        try {
            PreparedStatement stmt = preparedStatements.get("insertOrUpdate");
            if (stmt == null) return;
            
            long timestamp = System.currentTimeMillis();
            stmt.setString(1, uuid.toString());
            stmt.setString(2, roomKey);
            stmt.setInt(3, kills);
            stmt.setInt(4, completed ? 1 : 0);
            stmt.setLong(5, timestamp);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save progress for " + uuid + " room " + roomKey, e);
        }
    }
    
    public Map<String, RoomProgressData> loadProgressSync(@NotNull UUID uuid) {
        Map<String, RoomProgressData> result = new HashMap<>();
        try {
            PreparedStatement stmt = preparedStatements.get("selectProgress");
            if (stmt == null) return result;
            
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String roomKey = rs.getString("room_key");
                    int kills = rs.getInt("kills");
                    boolean completed = rs.getInt("completed") == 1;
                    result.put(roomKey, new RoomProgressData(kills, completed));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load progress for " + uuid, e);
        }
        return result;
    }
    
    public void deleteProgressSync(@NotNull UUID uuid) {
        try {
            PreparedStatement stmt = preparedStatements.get("deleteProgress");
            if (stmt == null) return;
            
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete progress for " + uuid, e);
        }
    }
    
    public void deleteRoomProgressSync(@NotNull UUID uuid, @NotNull String roomKey) {
        try {
            PreparedStatement stmt = preparedStatements.get("deleteRoomProgress");
            if (stmt == null) return;
            
            stmt.setString(1, uuid.toString());
            stmt.setString(2, roomKey);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete room progress for " + uuid + " room " + roomKey, e);
        }
    }
    
    public static final class RoomProgressData {
        public final int kills;
        public final boolean completed;
        
        public RoomProgressData(int kills, boolean completed) {
            this.kills = kills;
            this.completed = completed;
        }
    }
}