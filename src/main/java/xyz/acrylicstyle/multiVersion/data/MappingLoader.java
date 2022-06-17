package xyz.acrylicstyle.multiVersion.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

public class MappingLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .registerTypeHierarchyAdapter(RegistriesData.class, new RegistriesData.Serializer())
            .registerTypeHierarchyAdapter(RegistryData.class, new RegistryData.Serializer())
            .registerTypeHierarchyAdapter(RegistryDataEntry.class, new RegistryDataEntry.Serializer())
            .registerTypeHierarchyAdapter(BlocksData.class, new BlocksData.Serializer())
            .registerTypeHierarchyAdapter(BlockData.class, new BlockData.Serializer())
            .registerTypeHierarchyAdapter(BlockData.BlockState.class, new BlockData.BlockState.Serializer())
            .create();
    private static final Map<MappingVersion, MappingData> LOADED_DATA = new Object2ObjectOpenHashMap<>();

    @Contract(value = "_ -> new", pure = true)
    @NotNull
    private static MappingData load(@NotNull MappingVersion version) throws IOException {
        String registriesPath = "/data/" + version + ".json.gz";
        String blocksPath = "/data/" + version + "-blocks.json.gz";
        RegistriesData registriesData;
        try (var in = Objects.requireNonNull(MappingLoader.class.getResourceAsStream(registriesPath), registriesPath + " does not exist");
             var gzip = new GZIPInputStream(in);
             var reader = new InputStreamReader(gzip)) {
            registriesData = GSON.fromJson(reader, RegistriesData.class);
        }
        BlocksData blocksData;
        try (var in = Objects.requireNonNull(MappingLoader.class.getResourceAsStream(blocksPath), blocksPath + " does not exist");
             var gzip = new GZIPInputStream(in);
             var reader = new InputStreamReader(gzip)) {
            blocksData = GSON.fromJson(reader, BlocksData.class);
        }
        return new MappingData(registriesData, blocksData);
    }

    public static void loadAll() {
        for (MappingVersion version : MappingVersion.values()) {
            LOGGER.info("Loading mapping data for " + version + "...");
            try {
                LOADED_DATA.put(version, load(version));
            } catch (IOException e) {
                LOGGER.error("Failed to load mapping data for " + version, e);
            }
        }
    }

    @NotNull
    public static MappingData getMappingData(@NotNull MappingVersion version) {
        if (!LOADED_DATA.containsKey(version)) {
            throw new IllegalArgumentException("Mapping data for " + version + " is not loaded");
        }
        return LOADED_DATA.get(version);
    }
}
