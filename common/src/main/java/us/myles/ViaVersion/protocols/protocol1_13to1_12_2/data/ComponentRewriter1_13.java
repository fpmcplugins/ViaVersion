package us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ShortTag;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.nbt.BinaryTagIO;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.rewriters.ComponentRewriter;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.packets.InventoryPackets;

import java.io.IOException;

public class ComponentRewriter1_13 extends ComponentRewriter {

    public ComponentRewriter1_13(Protocol protocol) {
        super(protocol);
    }

    public ComponentRewriter1_13() {
    }

    @Override
    protected void handleHoverEvent(JsonObject hoverEvent) {
        super.handleHoverEvent(hoverEvent);
        String action = hoverEvent.getAsJsonPrimitive("action").getAsString();
        if (!action.equals("show_item")) return;

        JsonElement value = hoverEvent.get("value");
        if (value == null) return;

        String text = findItemNBT(value);
        if (text == null) return;

        CompoundTag tag;
        try {
            tag = BinaryTagIO.readString(text);
        } catch (IOException e) {
            Via.getPlatform().getLogger().warning("Error reading NBT in show_item:" + text);
            e.printStackTrace();
            return;
        }

        CompoundTag itemTag = tag.get("tag");
        ShortTag damageTag = tag.get("Damage");

        // Call item converter
        short damage = damageTag != null ? damageTag.getValue() : 0;
        Item item = new Item();
        item.setData(damage);
        item.setTag(itemTag);
        handleItem(item);

        // Serialize again
        if (damage != item.getData()) {
            tag.put(new ShortTag("Damage", item.getData()));
        }
        if (itemTag != null) {
            tag.put(itemTag);
        }

        JsonArray array = new JsonArray();
        JsonObject object = new JsonObject();
        array.add(object);
        String serializedNBT;
        try {
            serializedNBT = BinaryTagIO.writeString(tag);
            object.addProperty("text", serializedNBT);
            hoverEvent.add("value", array);
        } catch (IOException e) {
            Via.getPlatform().getLogger().warning("Error writing NBT in show_item:" + text);
            e.printStackTrace();
        }
    }

    protected void handleItem(Item item) {
        InventoryPackets.toClient(item);
    }

    protected String findItemNBT(JsonElement element) {
        if (element.isJsonArray()) {
            for (JsonElement jsonElement : element.getAsJsonArray()) {
                String value = findItemNBT(jsonElement);
                if (value != null) {
                    return value;
                }
            }
        } else if (element.isJsonObject()) {
            JsonPrimitive text = element.getAsJsonObject().getAsJsonPrimitive("text");
            if (text != null) {
                return text.getAsString();
            }
        } else if (element.isJsonPrimitive()) {
            return element.getAsJsonPrimitive().getAsString();
        }
        return null;
    }

    @Override
    protected void handleTranslate(JsonObject object, String translate) {
        super.handleTranslate(object, translate);
        String newTranslate;
        newTranslate = MappingData.translateMapping.get(translate);
        if (newTranslate == null) {
            newTranslate = MappingData.mojangTranslation.get(translate);
        }
        if (newTranslate != null) {
            object.addProperty("translate", newTranslate);
        }
    }
}
