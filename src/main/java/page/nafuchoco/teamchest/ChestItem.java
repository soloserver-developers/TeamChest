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

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

@AllArgsConstructor
@Data
public class ChestItem implements ConfigurationSerializable {
    private final int index;
    private final ItemStack itemStack;
    private final String nbtTag;

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serializedMap = new LinkedHashMap<>();
        serializedMap.put("index", index);
        serializedMap.put("itemStack", itemStack);
        serializedMap.put("nbtTag", nbtTag);
        return serializedMap;
    }

    public static ChestItem deserialize(Map<String, Object> args) {
        int index = args.get("index") != null ? (int) args.get("index") : -1;
        return new ChestItem(index, (ItemStack) args.get("itemStack"), (String) args.get("nbtTag"));
    }
}
