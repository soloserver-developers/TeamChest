/*
 * Copyright 2020 NAFU_at
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package page.nafuchoco.teamchest;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import page.nafuchoco.soloservercore.SoloServerApi;
import page.nafuchoco.soloservercore.database.DatabaseConnector;
import page.nafuchoco.teamchest.database.ChestsTable;

import java.sql.SQLException;
import java.util.logging.Level;

import static page.nafuchoco.soloservercore.SoloServerCore.getCoreConfig;

public final class TeamChest extends JavaPlugin {
    private static TeamChest instance;

    private SoloServerApi soloServerApi;
    private static DatabaseConnector connector;
    private ChestsTable chestsTable;

    public static TeamChest getInstance() {
        if (instance == null)
            instance = (TeamChest) Bukkit.getPluginManager().getPlugin("TeamChest");
        return instance;
    }

    public SoloServerApi getSoloServerApi() {
        return soloServerApi;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        soloServerApi = SoloServerApi.getSoloServerApi();
        connector = new DatabaseConnector(getCoreConfig().getInitConfig().getDatabaseType(),
                getCoreConfig().getInitConfig().getAddress() + ":" + getCoreConfig().getInitConfig().getPort(),
                getCoreConfig().getInitConfig().getDatabase(),
                getCoreConfig().getInitConfig().getUsername(),
                getCoreConfig().getInitConfig().getPassword());
        chestsTable = new ChestsTable(connector);
        try {
            chestsTable.createTable();
        } catch (SQLException e) {
            getLogger().log(Level.WARNING, "An error occurred while initializing the database table.", e);
        }
        ConfigurationSerialization.registerClass(ChestItem.class);
        getCommand("teamchest").setExecutor(new ChestCommand(chestsTable));
        Bukkit.getPluginManager().registerEvents(new PlayersTeamDisappearanceEventListener(chestsTable), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (connector != null)
            connector.close();
    }
}
