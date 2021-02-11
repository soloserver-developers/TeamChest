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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import page.nafuchoco.soloservercore.team.PlayersTeam;
import page.nafuchoco.teamchest.database.ChestsTable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class InventoryCloseEventListener implements Listener {
    private final ChestsTable chestsTable;
    private final Map<InventoryView, PlayersTeam> openedInventory;
    private final ObjectMapper mapper = new ObjectMapper();

    public InventoryCloseEventListener(Map<InventoryView, PlayersTeam> openedInventory, ChestsTable chestsTable) {
        this.openedInventory = openedInventory;
        this.chestsTable = chestsTable;
    }

    @SneakyThrows
    @EventHandler
    public void onInventoryCloseEvent(InventoryCloseEvent event) {
        if (openedInventory.containsKey(event.getView())) {
            List<ChestItem> items = new ArrayList<>();
            for (int i = 0; i < event.getView().getTopInventory().getSize(); i++) {
                ItemStack itemStack = event.getView().getItem(i);
                if (itemStack != null) {
                    net.minecraft.server.v1_16_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
                    String nbtString = null;
                    if (nmsStack.getTag() != null)
                        nbtString = nmsStack.getTag().toString();
                    items.add(new ChestItem(itemStack, nbtString));
                } else {
                    items.add(null);
                }
            }
            try {
                chestsTable.saveTeamChest(openedInventory.remove(event.getView()), items);
            } catch (SQLException e) {
                event.getPlayer().sendMessage(ChatColor.RED + "[TeamChest] インベントリの保存中にエラーが発生しました。");
                YamlConfiguration yamlConfiguration = new YamlConfiguration();
                yamlConfiguration.set("items", items);
                TeamChest.getInstance().getLogger().log(Level.WARNING, "An error occurred while saving the inventory data.\n" +
                        "Destroyed inventory data:\n" +
                        yamlConfiguration.saveToString(), e);
            }
        }
    }
}
