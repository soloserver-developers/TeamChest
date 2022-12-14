package dev.nafusoft.teamchest;

import dev.nafusoft.soloservercore.SoloServerCore;
import dev.nafusoft.teamchest.database.DatabaseConnector;
import dev.nafusoft.teamchest.database.DatabaseType;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import page.nafuchoco.teamchest.ChestItem;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class TeamChest extends JavaPlugin {


    @Override
    public void onLoad() {
        // なにより最初にロードする。
        SoloServerCore ssc = (SoloServerCore) Bukkit.getPluginManager().getPlugin("SoloServerCore");
        val configFile = new File(ssc.getDataFolder(), "config.yml");
        if (!configFile.exists())
            return;

        val config = ssc.getConfig();

        val databaseType = DatabaseType.valueOf(config.getString("initialization.database.type"));
        val address = config.getString("initialization.database.address");
        val port = config.getInt("initialization.database.port", 3306);
        val database = config.getString("initialization.database.database");
        val username = config.getString("initialization.database.username");
        val password = config.getString("initialization.database.password");
        val tablePrefix = config.getString("initialization.database.tablePrefix");

        DatabaseConnector connector = new DatabaseConnector(databaseType, address + ":" + port, database, username, password);
        try (Connection connection = connector.getConnection()) {
            boolean converted = true;
            // Check table column type.
            getLogger().info("Checking table column type...");
            try (PreparedStatement statement = connection.prepareStatement(
                    "SHOW COLUMNS FROM " + tablePrefix + "chests WHERE Field = ?"
            )) {
                statement.setString(1, "items");
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next())
                    if (resultSet.getString("Type").equalsIgnoreCase("longtext"))
                        converted = false;
            }

            if (!converted) {
                getLogger().info("We found an old version of the database structure.\n" +
                        "Migrate to the new version...");

                Map<String, Inventory> inventoryMap = new HashMap<>();
                // Fetch all chests.
                getLogger().info("Fetching all chests...");
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT * FROM " + tablePrefix + "chests"
                )) {
                    ResultSet resultSet = ps.executeQuery();
                    while (resultSet.next()) {
                        String id = resultSet.getString("id");
                        String itemString = resultSet.getString("items");

                        Inventory inventory = convertInventory(itemString);
                        inventoryMap.put(id, inventory);
                    }
                }

                // Drop old table.
                getLogger().info("Drop old table...");
                try (PreparedStatement ps = connection.prepareStatement(
                        "DROP TABLE " + tablePrefix + "chests"
                )) {
                    ps.execute();
                }

                // Create new table.
                getLogger().info("Create new table...");
                try (PreparedStatement ps = connection.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS " + tablePrefix + "chests" + " (id VARCHAR(36) PRIMARY KEY, items VARBINARY(1024) NOT NULL)"
                )) {
                    ps.execute();
                }

                // Insert new data.
                getLogger().info("Insert new data...");
                for (Map.Entry<String, Inventory> entry : inventoryMap.entrySet()) {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO " + tablePrefix + "chests" + " (id, items) VALUES (?, ?) ON DUPLICATE KEY UPDATE items = VALUES (items)"
                    )) {
                        ps.setString(1, entry.getKey());
                        ps.setBytes(2, InventoryEncoder.encodeInventory(entry.getValue(), true));
                        ps.executeUpdate();
                    } catch (ItemProcessingException e) {
                        getLogger().log(Level.WARNING, "Failed to convert chest data.\nRaw Data:\n" + entry.getKey(), e);
                    }
                }

                getLogger().info("Migration completed!");
            }
        } catch (SQLException e) {
            getLogger().log(Level.WARNING, "Failed to convert chest data.", e);
        }

        connector.close();
    }

    @Override
    public void onEnable() {
        // This plugin will not be enabled.
        Bukkit.getServer().getPluginManager().disablePlugin(this);
    }

    private Inventory convertInventory(String itemList) {
        List<ChestItem> items;

        try {
            ConfigurationSerialization.registerClass(ChestItem.class);
            val yamlConfiguration = new YamlConfiguration();
            yamlConfiguration.loadFromString(itemList);
            items = (List<ChestItem>) yamlConfiguration.getList("items");

            ItemStack[] itemStacks = new ItemStack[54];
            for (int i = 0; i < items.size(); i++) {
                ChestItem item = items.get(i);
                if (item == null) continue;
                itemStacks[i] = item.getItemStack();
            }

            val inventory = Bukkit.getServer().createInventory(null, 54, "TeamChest");
            inventory.setContents(itemStacks);

            return inventory;
        } catch (InvalidConfigurationException e) {
            getLogger().log(Level.WARNING, "Failed to load the list of items in the team chest.\nRawData:\n" + itemList, e);
            return null;
        }
    }
}
