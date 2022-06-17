package xyz.acrylicstyle.multiVersion.data;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.BiFunction;

import static java.util.Map.entry;

public record MappingDataPair(@NotNull MappingData first, @NotNull MappingData second) {
    private static final Map<MappingDataKey, BiFunction<MappingData, MappingData, Int2IntFunction>> LENIENT_MAPPERS = Map.ofEntries(
            entry(MappingDataKey.BLOCK_STATE, (first, second) -> (id) -> {
                var oldState = first.blocks().findBlockStateById(id);
                if (oldState == null) {
                    return -1;
                }
                var newState = second.blocks().findBlockStateWithoutId(oldState);
                if (newState == null) {
                    return -2;
                }
                return newState.blockState().id();
            }),
            entry(MappingDataKey.ITEM, mapByKeyNoThrow(MappingDataKey.ITEM)),
            entry(MappingDataKey.BLOCK, mapByKeyNoThrow(MappingDataKey.BLOCK)),
            entry(MappingDataKey.ENTITY_TYPE, mapByKeyNoThrow(MappingDataKey.ENTITY_TYPE)),
            entry(MappingDataKey.SOUND_EVENT, mapByKeyNoThrow(MappingDataKey.SOUND_EVENT)),
            entry(MappingDataKey.PARTICLE_TYPE, mapByKeyNoThrow(MappingDataKey.PARTICLE_TYPE))
    );
    private static final Map<MappingDataKey, BiFunction<MappingData, MappingData, Int2IntFunction>> MAPPERS = Map.ofEntries(
            entry(MappingDataKey.BLOCK_STATE, (first, second) -> (id) -> {
                var oldState = first.blocks().findBlockStateById(id);
                if (oldState == null) {
                    throw new IllegalArgumentException("Block state with id " + id + " is not registered in first");
                }
                var newState = second.blocks().findBlockStateWithoutId(oldState);
                if (newState == null) {
                    throw new IllegalArgumentException("Block state " + id + " is not registered in second");
                }
                return newState.blockState().id();
            }),
            entry(MappingDataKey.ITEM, mapByKey(MappingDataKey.ITEM)),
            entry(MappingDataKey.BLOCK, mapByKey(MappingDataKey.BLOCK)),
            entry(MappingDataKey.ENTITY_TYPE, mapByKey(MappingDataKey.ENTITY_TYPE)),
            entry(MappingDataKey.SOUND_EVENT, mapByKey(MappingDataKey.SOUND_EVENT)),
            entry(MappingDataKey.PARTICLE_TYPE, mapByKey(MappingDataKey.PARTICLE_TYPE))
    );

    @Contract(pure = true)
    private static @NotNull BiFunction<MappingData, MappingData, Int2IntFunction> mapByKeyNoThrow(MappingDataKey key) {
        return (first, second) -> (id) -> {
            var entry = first.registries().getOrThrow(key.registryOrThrow()).getById(id);
            if (entry == null) {
                return -1;
            }
            var newEntry = second.registries().getOrThrow(key.registryOrThrow()).get(entry.location());
            if (newEntry == null) {
                return -2;
            }
            return newEntry.protocolId();
        };
    }

    @Contract(pure = true)
    private static @NotNull BiFunction<MappingData, MappingData, Int2IntFunction> mapByKey(MappingDataKey key) {
        return (first, second) -> (id) -> {
            var entry = first.registries().getOrThrow(key.registryOrThrow()).getByIdOrThrow(id);
            return second.registries().getOrThrow(key.registryOrThrow()).getOrThrow(entry.location()).protocolId();
        };
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull MappingDataPair of(@NotNull MappingVersion first, @NotNull MappingVersion second) {
        return new MappingDataPair(MappingLoader.getMappingData(first), MappingLoader.getMappingData(second));
    }

    /**
     * Remaps the given id with the provided key.
     * @param key the data to remap
     * @param id the id to remap
     * @return the remapped id, -1 if old id is missing from first registry, or -2 if new id is missing from second registry
     */
    public int remapIdFirstToSecond(@NotNull MappingDataKey key, int id) {
        return remapId(LENIENT_MAPPERS, key, id, first, second);
    }

    /**
     * Remaps the given id with the provided key.
     * @param key the data to remap
     * @param id the id to remap
     * @return the remapped id, -1 if old id is missing from first registry, or -2 if new id is missing from second registry
     */
    public int remapIdSecondToFirst(@NotNull MappingDataKey key, int id) {
        return remapId(LENIENT_MAPPERS, key, id, second, first);
    }

    /**
     * Remaps the given id with the provided key.
     * @param key the data to remap
     * @param id the id to remap
     * @return the remapped id
     */
    public int remapIdFirstToSecondOrThrow(@NotNull MappingDataKey key, int id) {
        return remapId(MAPPERS, key, id, first, second);
    }

    /**
     * Remaps the given id with the provided key.
     * @param key the data to remap
     * @param id the id to remap
     * @return the remapped id
     */
    public int remapIdSecondToFirstOrThrow(@NotNull MappingDataKey key, int id) {
        return remapId(MAPPERS, key, id, second, first);
    }

    private int remapId(
            @NotNull Map<MappingDataKey, BiFunction<MappingData, MappingData, Int2IntFunction>> mappers,
            @NotNull MappingDataKey key,
            int id,
            @NotNull MappingData from,
            @NotNull MappingData to) {
        var mapper = mappers.get(key);
        if (mapper == null) {
            throw new IllegalArgumentException("No mapper found for " + key);
        }
        return mapper.apply(from, to).apply(id);
    }
}
