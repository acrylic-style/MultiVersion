package xyz.acrylicstyle.multiVersion.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record BlockData(@NotNull List<BlockProperty> properties, @NotNull List<BlockState> states) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockData blockData)) return false;
        return properties.equals(blockData.properties) && states.equals(blockData.states);
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties, states);
    }

    public static class Serializer implements JsonDeserializer<BlockData> {
        @Override
        public BlockData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            var object = json.getAsJsonObject();
            List<BlockProperty> properties;
            if (object.has("properties")) {
                properties = new ObjectArrayList<>();
                for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("properties").entrySet()) {
                    var name = entry.getKey();
                    var possibleValues = new ObjectArrayList<String>();
                    for (JsonElement value : entry.getValue().getAsJsonArray()) {
                        possibleValues.add(value.getAsString());
                    }
                    properties.add(new BlockProperty(name, possibleValues));
                }
            } else {
                properties = Collections.emptyList();
            }
            List<BlockState> states;
            if (object.has("states")) {
                JsonArray array = object.getAsJsonArray("states");
                if (array.size() == 1) {
                    states = Collections.singletonList(context.deserialize(array.get(0), BlockState.class));
                } else {
                    states = new ObjectArrayList<>();
                    array.forEach(element -> {
                        BlockState state = context.deserialize(element, BlockState.class);
                        states.add(state);
                    });
                }
            } else {
                states = Collections.emptyList();
            }
            return new BlockData(properties, states);
        }
    }

    public record BlockProperty(@NotNull String name, @NotNull List<String> possibleValues) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockProperty that)) return false;
            return name.equals(that.name) && possibleValues.equals(that.possibleValues);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, possibleValues);
        }
    }

    public record BlockState(int id, boolean isDefault, @NotNull Map<String, String> properties) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockState that)) return false;
            return id == that.id && isDefault == that.isDefault && properties.equals(that.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, isDefault, properties);
        }

        @Contract(pure = true)
        public boolean equalsProperties(@NotNull BlockState that) {
            return properties.equals(that.properties);
        }

        public static class Serializer implements JsonDeserializer<BlockState> {
            @Override
            public BlockState deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                var object = json.getAsJsonObject();
                var isDefault = object.has("default") && object.get("default").getAsBoolean();
                var id = object.get("id").getAsInt();
                if (!object.has("properties")) {
                    return new BlockState(id, isDefault, Collections.emptyMap());
                }
                var properties = new Object2ObjectOpenHashMap<String, String>();
                for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("properties").entrySet()) {
                    var key = entry.getKey();
                    var value = entry.getValue().getAsString();
                    properties.put(key, value);
                }
                return new BlockState(id, isDefault, properties);
            }
        }
    }
}
