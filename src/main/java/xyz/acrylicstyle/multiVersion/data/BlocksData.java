package xyz.acrylicstyle.multiVersion.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.stream.Collectors;

public record BlocksData(@NotNull Map<ResourceLocation, BlockData> blocks) {
    @Contract(pure = true)
    @Nullable
    public IdentifiedBlockState findBlockStateById(int id) {
        for (Map.Entry<ResourceLocation, BlockData> entry : blocks.entrySet()) {
            for (BlockData.BlockState state : entry.getValue().states()) {
                if (state.id() == id) {
                    return new IdentifiedBlockState(entry.getKey(), state);
                }
            }
        }
        return null;
    }

    @Contract(pure = true)
    @Nullable
    public IdentifiedBlockState findBlockStateWithoutId(@NotNull IdentifiedBlockState another) {
        for (Map.Entry<ResourceLocation, BlockData> entry : blocks.entrySet()) {
            if (!entry.getKey().equals(another.location)) {
                continue;
            }
            for (BlockData.BlockState state : entry.getValue().states()) {
                if (state.equalsProperties(another.blockState)) {
                    return new IdentifiedBlockState(entry.getKey(), state);
                }
            }
        }
        return null;
    }

    public static class Serializer implements JsonDeserializer<BlocksData> {
        @Override
        public BlocksData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            var object = json.getAsJsonObject();
            var map = new Object2ObjectArrayMap<ResourceLocation, BlockData>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                var key = new ResourceLocation(entry.getKey());
                BlockData value = context.deserialize(entry.getValue(), BlockData.class);
                map.put(key, value);
            }
            return new BlocksData(map);
        }
    }

    public static class IdentifiedBlockState {
        private final ResourceLocation location;
        private final BlockData.BlockState blockState;
        private final String toString;

        public IdentifiedBlockState(@NotNull ResourceLocation location, @NotNull BlockData.BlockState blockState) {
            this.location = location;
            this.blockState = blockState;
            this.toString = asString();
        }

        @NotNull
        public ResourceLocation location() {
            return location;
        }

        @NotNull
        public BlockData.BlockState blockState() {
            return blockState;
        }

        @Override
        public String toString() {
            return toString;
        }

        private @NotNull String asString() {
            if (blockState.properties().size() == 0) {
                return location.toString();
            }
            if (blockState.properties().size() == 1) {
                String key = "";
                String value = "";
                for (Map.Entry<String, String> entry : blockState.properties().entrySet()) {
                    key = entry.getKey();
                    value = entry.getValue();
                }
                return location + "[" + key + "=" + value + "]";
            }
            var props = blockState.properties()
                    .entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(","));
            return location + "[" + props + "]";
        }
    }
}
