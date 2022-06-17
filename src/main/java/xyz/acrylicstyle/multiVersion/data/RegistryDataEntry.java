package xyz.acrylicstyle.multiVersion.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public record RegistryDataEntry(@NotNull ResourceLocation location, int protocolId) {
    @Contract(value = "_ -> new", pure = true)
    @NotNull
    public RegistryDataEntry location(@NotNull ResourceLocation location) {
        return new RegistryDataEntry(location, protocolId);
    }

    public static class Serializer implements JsonDeserializer<RegistryDataEntry> {
        private static final ResourceLocation NULL = new ResourceLocation("multiversion:null");

        @Override
        public RegistryDataEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            var object = json.getAsJsonObject();
            var protocolId = object.get("protocol_id").getAsInt();
            return new RegistryDataEntry(NULL, protocolId);
        }
    }
}
