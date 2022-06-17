package xyz.acrylicstyle.multiVersion.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public record RegistryData(@Nullable String defaultValue, int protocolId, @NotNull List<RegistryDataEntry> entries) {
    @Nullable
    public RegistryDataEntry getById(int id) {
        for (var entry : entries) {
            if (entry.protocolId() == id) {
                return entry;
            }
        }
        return null;
    }

    @NotNull
    public RegistryDataEntry getByIdOrThrow(int id) {
        var entry = getById(id);
        if (entry == null) {
            throw new IllegalArgumentException("No entry found for id " + id);
        }
        return entry;
    }

    @Nullable
    public RegistryDataEntry get(@NotNull ResourceLocation location) {
        for (RegistryDataEntry entry : entries) {
            if (entry.location().equals(location)) {
                return entry;
            }
        }
        return null;
    }

    @NotNull
    public RegistryDataEntry getOrThrow(@NotNull ResourceLocation location) {
        var entry = get(location);
        if (entry == null) {
            throw new IllegalArgumentException("No data found for " + location);
        }
        return entry;
    }

    public static class Serializer implements JsonDeserializer<RegistryData> {
        @Override
        public RegistryData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            var object = json.getAsJsonObject();
            var defaultValue = object.get("default") != null ? object.get("default").getAsString() : null;
            var protocolId = object.get("protocol_id").getAsInt();
            List<RegistryDataEntry> entries = new ObjectArrayList<>();
            for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("entries").entrySet()) {
                var key = new ResourceLocation(entry.getKey());
                RegistryDataEntry value = context.deserialize(entry.getValue(), RegistryDataEntry.class);
                entries.add(value.location(key));
            }
            return new RegistryData(defaultValue, protocolId, entries);
        }
    }
}
