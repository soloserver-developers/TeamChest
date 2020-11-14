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

package page.nafuchoco.teamchest.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import page.nafuchoco.soloservercore.database.DatabaseConnector;
import page.nafuchoco.soloservercore.database.DatabaseTable;
import page.nafuchoco.soloservercore.team.PlayersTeam;
import page.nafuchoco.teamchest.ChestItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ChestsTable extends DatabaseTable {
    private final ObjectMapper mapper = new ObjectMapper();

    public ChestsTable(DatabaseConnector connector) {
        super("chests", connector);
    }

    public void createTable() throws SQLException {
        super.createTable("id VARCHAR(36) PRIMARY KEY, items LONGTEXT NOT NULL");
    }

    public List<ChestItem> getTeamChest(PlayersTeam team) throws SQLException, InvalidConfigurationException {
        try (Connection connection = getConnector().getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT * FROM " + getTablename() + " WHERE id = ?"
             )) {
            ps.setString(1, team.getId().toString());
            List<ChestItem> items = new ArrayList<>();
            try (ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    String itemString = resultSet.getString("items");
                    YamlConfiguration yamlConfiguration = new YamlConfiguration();
                    yamlConfiguration.loadFromString(itemString);
                    items = (List<ChestItem>) yamlConfiguration.getList("items");
                }
            }
            return items;
        }
    }

    public void saveTeamChest(PlayersTeam team, List<ChestItem> items) throws SQLException {
        try (Connection connection = getConnector().getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "INSERT INTO " + getTablename() + " (id, items) VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE items = VALUES (items)"
             )) {
            YamlConfiguration yamlConfiguration = new YamlConfiguration();
            yamlConfiguration.set("items", items);
            ps.setString(1, team.getId().toString());
            ps.setString(2, yamlConfiguration.saveToString());
            ps.execute();
        }
    }
}
