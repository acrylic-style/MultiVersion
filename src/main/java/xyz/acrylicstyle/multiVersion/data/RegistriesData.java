package xyz.acrylicstyle.multiVersion.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Map;

public record RegistriesData(@NotNull Map<ResourceLocation, RegistryData> registries) {
    @Nullable
    public RegistryData get(@NotNull ResourceLocation location) {
        return registries.get(location);
    }

    @NotNull
    public RegistryData getOrThrow(@NotNull ResourceLocation location) {
        var registry = registries.get(location);
        if (registry == null) {
            throw new IllegalArgumentException("No registry found for " + location);
        }
        return registry;
    }

    public static class Serializer implements JsonDeserializer<RegistriesData> {
        @Override
        public RegistriesData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            var object = json.getAsJsonObject();
            var map = new Object2ObjectArrayMap<ResourceLocation, RegistryData>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                var key = new ResourceLocation(entry.getKey());
                RegistryData value = context.deserialize(entry.getValue(), RegistryData.class);
                map.put(key, value);
            }
            return new RegistriesData(map);
        }
    }
}
