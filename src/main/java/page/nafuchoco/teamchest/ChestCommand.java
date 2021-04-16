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

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.v1_16_R3.MojangsonParser;
import net.minecraft.server.v1_16_R3.NBTBase;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import page.nafuchoco.soloservercore.team.PlayersTeam;
import page.nafuchoco.teamchest.database.ChestsTable;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ChestCommand implements CommandExecutor {
    private final ChestsTable chestsTable;
    private final Map<InventoryView, PlayersTeam> openedInventory;

    public ChestCommand(ChestsTable chestsTable) {
        this.chestsTable = chestsTable;
        openedInventory = new LinkedHashMap<>();
        Bukkit.getPluginManager().registerEvents(new InventoryCloseEventListener(openedInventory, chestsTable), TeamChest.getInstance());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            PlayersTeam joinedTeam = TeamChest.getInstance().getSoloServerApi().getPlayerJoinedTeam(player.getUniqueId());
            if (joinedTeam != null) {
                Inventory inventory = Bukkit.createInventory(null, 54, "TeamChest");
                try {
                    List<ChestItem> items = chestsTable.getTeamChest(joinedTeam);
                    for (int i = 0; i < items.size(); i++) {
                        ChestItem item = items.get(i);
                        // Debug Code.
                        // TeamChest.getInstance().getLogger().info("ChestItem[" + i + "]: " + (item != null ? item.getItemStack().toString() + " (NBT: " + item.getNbtTag() + ")" : "Empty"));
                        if (item != null) {
                            ItemStack itemStack = item.getItemStack();
                            net.minecraft.server.v1_16_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
                            if (item.getNbtTag() != null) {
                                NBTBase nbtBase = MojangsonParser.parse(item.getNbtTag());
                                nmsStack.setTag((NBTTagCompound) nbtBase);
                            }
                            inventory.setItem(i, CraftItemStack.asBukkitCopy(nmsStack));
                        }
                    }
                    InventoryView inventoryView = player.openInventory(inventory);
                    openedInventory.put(inventoryView, joinedTeam);
                } catch (SQLException e) {
                    sender.sendMessage(ChatColor.RED + "[TeamChest] インベントリの取得中にエラーが発生しました。");
                    TeamChest.getInstance().getLogger().log(Level.WARNING, "An error occurred while fetching inventory data.", e);
                } catch (InvalidConfigurationException | CommandSyntaxException e) {
                    sender.sendMessage(ChatColor.RED + "[TeamChest] インベントリの取得中にエラーが発生しました。");
                    TeamChest.getInstance().getLogger().log(Level.WARNING, "There seems to be some kind of syntax error in the stored data.", e);
                }
            } else {
                player.sendMessage(ChatColor.YELLOW + "[TeamChest] チームチェストを使用するにはチームに所属している必要があります。");
            }
        }
        return true;
    }
}
