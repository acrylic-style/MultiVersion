package xyz.acrylicstyle.multiVersion.transformer.rewriters;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.blueberrymc.common.Blueberry;
import net.blueberrymc.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import xyz.acrylicstyle.multiVersion.data.MappingDataKey;
import xyz.acrylicstyle.multiVersion.data.MappingDataPair;
import xyz.acrylicstyle.multiVersion.data.MappingVersion;
import xyz.acrylicstyle.multiVersion.transformer.PacketRewriter;
import xyz.acrylicstyle.multiVersion.transformer.PacketWrapper;
import xyz.acrylicstyle.multiVersion.transformer.TransformableProtocolVersions;
import xyz.acrylicstyle.multiVersion.util.CompactArrayUtil;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class v1_18_2_To_v1_19 extends PacketRewriter {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CHAT_REGISTRY_SNBT = """
            {
              "minecraft:chat_type": {
                "type": "minecraft:chat_type",
                "value": [
                  {
                    "name": "minecraft:system",
                    "id": 1,
                    "element": {
                      "chat": {},
                      "narration": {
                        "priority": "system"
                      }
                    }
                  },
                  {
                    "name": "minecraft:game_info",
                    "id": 2,
                    "element": {
                      "overlay": {}
                    }
                  }
                ]
              }
            }""";
    private static final CompoundTag CHAT_REGISTRY = Util.required(() -> TagParser.parseTag(CHAT_REGISTRY_SNBT)).getCompound("minecraft:chat_type");
    private static final MappingDataPair MAPPING = MappingDataPair.of(MappingVersion.v1_18_2, MappingVersion.v1_19);

    public v1_18_2_To_v1_19() {
        this(TransformableProtocolVersions.v1_19, TransformableProtocolVersions.v1_18_2);
    }

    protected v1_18_2_To_v1_19(@NotNull TransformableProtocolVersions sourcePV, @NotNull TransformableProtocolVersions targetPV) {
        super(sourcePV, targetPV);
    }

    @Override
    protected void preRegister() {
        registerItemRewriter();
        registerEntityRewriter();
        registerSoundRewriter();
        registerParticleRewriter();
    }

    @Override
    public void registerInbound() {
        //----- LOGIN -----//
        rewriteInbound(ConnectionProtocol.LOGIN, 0x02, wrapper -> {
            wrapper.passthroughUUID();
            wrapper.passthroughUtf(16);
            wrapper.writeVarInt(0);
        });
        //----- PLAY -----//
        //   0x00 Spawn Entity
        //   0x01 Spawn Experience Orb
        // - 0x02 Spawn Living Entity
        remapInbound(ConnectionProtocol.PLAY, 0x02, 0xFE);
        // - 0x03 Spawn Painting
        remapInbound(ConnectionProtocol.PLAY, 0x03, 0xFD);
        remapInbound(ConnectionProtocol.PLAY, 0x04, 0x02); // Spawn Player
        // - 0x05 Sculk Vibration Signal
        remapInbound(ConnectionProtocol.PLAY, 0x05, 0xFF);
        remapInbound(ConnectionProtocol.PLAY, 0x06, 0x03); // Entity Animation
        remapInbound(ConnectionProtocol.PLAY, 0x07, 0x04); // Statistics
        //   0x08 Acknowledge Player Digging -> 0x05 Acknowledge Block Change
        remapInbound(ConnectionProtocol.PLAY, 0x08, 0xFF);
        for (int oldId = 0x09; oldId <= 0x0E; oldId++) {
            remapInbound(ConnectionProtocol.PLAY, oldId, oldId - 3);
        }
        // + 0x0C Chat Preview
        remapInbound(ConnectionProtocol.PLAY, 0x0F, 0x5F); // Chat Message -> System Chat Message
        for (int oldId = 0x10; oldId <= 0x32; oldId++) {
            remapInbound(ConnectionProtocol.PLAY, oldId, oldId - 3);
        }
        // + 0x30 Player Chat Message
        for (int oldId = 0x33; oldId < 0x40; oldId++) {
            remapInbound(ConnectionProtocol.PLAY, oldId, oldId - 2);
        }
        // + 0x3F Server Data
        for (int oldId = 0x41; oldId <= 0x4B; oldId++) {
            remapInbound(ConnectionProtocol.PLAY, oldId, oldId - 1);
        }
        // + 0x4B Display Chat Preview
        // + 0x5F System Chat
        for (int oldId = 0x5F; oldId <= 0x67; oldId++) {
            remapInbound(ConnectionProtocol.PLAY, oldId, oldId + 1);
        }

        // Discard packets
        // - 0x05 Sculk Vibration Signal
        //   0x08 Acknowledge Player Digging
        rewriteInbound(ConnectionProtocol.PLAY, 0xFF, PacketWrapper::cancel);

        // Spawn Entity
        rewriteInbound(ConnectionProtocol.PLAY, 0x00, wrapper -> {
            wrapper.passthroughVarInt(); // Entity ID
            wrapper.passthroughUUID(); // Object UUID
            wrapper.passthroughVarInt(); // Type
            wrapper.passthroughDouble(); // X
            wrapper.passthroughDouble(); // Y
            wrapper.passthroughDouble(); // Z
            wrapper.passthroughByte(); // Pitch
            wrapper.passthroughByte(); // Yaw
            wrapper.writeByte(0); // Head Yaw
            wrapper.writeVarInt(wrapper.readInt()); // Data (Int -> VarInt)
            wrapper.passthroughShort(); // Velocity X
            wrapper.passthroughShort(); // Velocity Y
            wrapper.passthroughShort(); // Velocity Z
            wrapper.writerIndex();
        });

        // Spawn Living Entity (removed)
        rewriteInbound(ConnectionProtocol.PLAY, 0xFE, wrapper -> {
            wrapper.cancel(); // cancel the packet, because we don't want to send it to the client
            int id = wrapper.readVarInt();
            UUID uuid = wrapper.readUUID();
            int type = wrapper.readVarInt();
            double x = wrapper.readDouble();
            double y = wrapper.readDouble();
            double z = wrapper.readDouble();
            byte yRot = wrapper.readByte();
            byte xRot = wrapper.readByte();
            byte yHeadRot = wrapper.readByte();
            short xd = wrapper.readShort();
            short yd = wrapper.readShort();
            short zd = wrapper.readShort();
            writeInboundPacket(ConnectionProtocol.PLAY, 0x00, buf -> {
                buf.writeVarInt(id); // Entity ID
                buf.writeUUID(uuid); // Entity UUID
                buf.writeVarInt(type + remapEntityType(type)); // Entity Type
                buf.writeDouble(x);
                buf.writeDouble(y);
                buf.writeDouble(z);
                buf.writeByte(yRot);
                buf.writeByte(xRot);
                buf.writeByte(yHeadRot);
                buf.writeVarInt(0); // Data
                buf.writeShort(xd);
                buf.writeShort(yd);
                buf.writeShort(zd);
            });
        });

        // Spawn Painting (removed)
        rewriteInbound(ConnectionProtocol.PLAY, 0xFD, wrapper -> {
            wrapper.cancel();
            int entityId = wrapper.readVarInt(); // Entity ID
            UUID uuid = wrapper.readUUID(); // Entity UUID
            int motive = wrapper.readVarInt(); // Motive
            BlockPos loc = wrapper.readBlockPos(); // Position
            byte direction = wrapper.readByte(); // Direction

            // Spawn Entity
            writeInboundPacket(ConnectionProtocol.PLAY, 0x00, buf -> {
                buf.writeVarInt(entityId); // Entity ID
                buf.writeUUID(uuid); // Entity UUID
                buf.writeVarInt(63); // Entity Type (of Painting)
                buf.writeDouble(loc.getX() + 0.5d); // X
                buf.writeDouble(loc.getY() + 0.5d); // Y
                buf.writeDouble(loc.getZ() + 0.5d); // Z
                buf.writeByte(0); // Pitch
                buf.writeByte(0); // Yaw
                buf.writeByte(0); // Head Yaw
                buf.writeVarInt(to3dId(direction)); // Data
                buf.writeShort(0); // Velocity X
                buf.writeShort(0); // Velocity Y
                buf.writeShort(0); // Velocity Z
            });

            // Entity Metadata
            writeInboundPacket(ConnectionProtocol.PLAY, 0x4D, buf -> {
                buf.writeVarInt(entityId); // Entity ID
                buf.writeByte(8); // Metadata Index (Painting)
                buf.writeByte(21); // Metadata Type (Painting Variant)
                buf.writeByte(motive); // Metadata Value (Motive)
                buf.writeByte(0xFF); // Metadata Index (End of metadata)
            });
        });

        // Declare Commands
        rewriteInbound(ConnectionProtocol.PLAY, 0x0F, wrapper -> {
            int size = wrapper.passthroughVarInt();
            for (int i = 0; i < size; i++) {
                byte flags = wrapper.passthroughByte();
                wrapper.passthroughVarIntArray();
                if ((flags & 0x08) != 0) {
                    wrapper.passthroughVarInt();
                }

                int nodeType = flags & 0x03;
                if (nodeType == 1 || nodeType == 2) {
                    wrapper.passthroughUtf();
                }

                if (nodeType == 2) {
                    String argumentType = wrapper.readUtf();
                    int argumentTypeId = getArgumentTypeId(argumentType);
                    if (argumentTypeId == -1) {
                        LOGGER.warn("Unknown argument type: {}", argumentType);
                    }

                    wrapper.writeVarInt(argumentTypeId);

                    switch (argumentType) {
                        case "brigadier:double" -> {
                            int propertyFlags = wrapper.passthroughByte();
                            if ((propertyFlags & 0x01) != 0) {
                                wrapper.passthroughDouble(); // Min Value
                            }
                            if ((propertyFlags & 0x02) != 0) {
                                wrapper.passthroughDouble(); // Max Value
                            }
                        }
                        case "brigadier:float" -> {
                            int propertyFlags = wrapper.passthroughByte();
                            if ((propertyFlags & 0x01) != 0) {
                                wrapper.passthroughFloat(); // Min Value
                            }
                            if ((propertyFlags & 0x02) != 0) {
                                wrapper.passthroughFloat(); // Max Value
                            }
                        }
                        case "brigadier:integer" -> {
                            int propertyFlags = wrapper.passthroughByte();
                            if ((propertyFlags & 0x01) != 0) {
                                wrapper.passthroughInt(); // Min Value
                            }
                            if ((propertyFlags & 0x02) != 0) {
                                wrapper.passthroughInt(); // Max Value
                            }
                        }
                        case "brigadier:long" -> {
                            int propertyFlags = wrapper.passthroughByte();
                            if ((propertyFlags & 0x01) != 0) {
                                wrapper.passthroughLong(); // Min Value
                            }
                            if ((propertyFlags & 0x02) != 0) {
                                wrapper.passthroughLong(); // Max Value
                            }
                        }
                        case "brigadier:string" -> wrapper.passthroughVarInt();
                        case "minecraft:entity", "minecraft:score_holder" -> wrapper.passthroughByte();
                        case "minecraft:resource", "minecraft:resource_or_tag" -> wrapper.passthroughUtf();
                    }

                    if ((flags & 0x10) != 0) {
                        wrapper.passthroughUtf(); // Suggestion type
                    }
                }
            }
            wrapper.passthroughVarInt(); // Root node index
        });

        // Named Sound Effect
        rewriteInbound(ConnectionProtocol.PLAY, 0x16, wrapper -> {
            wrapper.passthroughResourceLocation(); // Identifier
            wrapper.passthroughVarInt(); // Sound Category
            wrapper.passthroughInt(); // Effect Position X
            wrapper.passthroughInt(); // Effect Position Y
            wrapper.passthroughInt(); // Effect Position Z
            wrapper.passthroughFloat(); // Volume
            wrapper.passthroughFloat(); // Pitch
            wrapper.writeLong(randomLong()); // Seed
        });

        // Explosion
        rewriteInbound(ConnectionProtocol.PLAY, 0x19, wrapper -> {
            wrapper.writeDouble(wrapper.readFloat()); // X (Float -> Double)
            wrapper.writeDouble(wrapper.readFloat()); // Y (Float -> Double)
            wrapper.writeDouble(wrapper.readFloat()); // Z (Float -> Double)
            wrapper.passthroughAll(); // Strength, Record Count, Records, Player Motion X/Y/Z
        });

        // Particle
        rewriteInbound(ConnectionProtocol.PLAY, 0x21, wrapper -> {
            wrapper.writeVarInt(wrapper.readInt()); // Particle ID (Int -> VarInt)
            wrapper.passthroughAll();
        });

        // Join Game
        rewriteInbound(ConnectionProtocol.PLAY, 0x23, wrapper -> {
            wrapper.passthroughInt(); // Entity ID
            wrapper.passthroughBoolean(); // Is hardcore
            wrapper.passthroughUnsignedByte(); // Gamemode
            wrapper.passthroughByte(); // Previous Gamemode
            wrapper.passthroughCollection(PacketWrapper.Type.RESOURCE_LOCATION); // World count / World names
            var codec = Objects.requireNonNull(wrapper.readNbt()); // Dimension Codec
            codec.put("minecraft:chat_type", CHAT_REGISTRY);
            var dimensions = Objects.requireNonNull(codec).getCompound("minecraft:dimension_type").getList("value", 10);
            var dimensionsMap = new HashMap<CompoundTag, String>(dimensions.size());
            for (Tag dimension : dimensions) {
                var dimensionCompound = (CompoundTag) dimension;
                var element = dimensionCompound.getCompound("element");
                var name = dimensionCompound.getString("name");
                dimensionsMap.put(element.copy(), name);

                // Add required tags
                element.putInt("monster_spawn_block_light_limit", 0);
                element.putInt("monster_spawn_light_level", 11);
            }
            wrapper.writeNbt(codec); // Registry Codec
            var nbt = Objects.requireNonNull(wrapper.readNbt()); // Dimension (1.18.2)
            String dimensionKey = dimensionsMap.get(nbt);
            if (dimensionKey == null) {
                LOGGER.error("Unknown dimension type: " + nbt);
                LOGGER.error("Known dimensions:");
                dimensionsMap.forEach((tag, name) -> LOGGER.error("  " + tag + " -> " + name));
                throw new IllegalArgumentException("Unknown dimension type: " + nbt);
            }
            wrapper.writeUtf(dimensionKey); // Dimension Type (1.19)
            wrapper.passthroughUtf(); // Dimension Name
            wrapper.passthroughLong(); // Hashed seed
            wrapper.passthroughVarInt(); // Max Players
            wrapper.passthroughVarInt(); // View Distance
            wrapper.passthroughVarInt(); // Simulation Distance
            wrapper.passthroughBoolean(); // Reduced Debug Info
            wrapper.passthroughBoolean(); // Enable respawn screen
            wrapper.passthroughBoolean(); // Is Debug
            wrapper.passthroughBoolean(); // Is Flat
            wrapper.writeBoolean(false); // Has Last Death Location

            // Disable chat preview
            writeInboundPacket(ConnectionProtocol.PLAY, 0x4B, buf -> buf.writeBoolean(true));
        });

        // Player Info
        rewriteInbound(ConnectionProtocol.PLAY, 0x34, wrapper -> {
            int action = wrapper.passthroughVarInt(); // Action
            int entries = wrapper.passthroughVarInt(); // Number Of Players
            for (int i = 0; i < entries; i++) {
                wrapper.passthroughUUID(); // UUID
                if (action == 0) { // Add player
                    wrapper.passthroughUtf(); // Player Name

                    int properties = wrapper.passthroughVarInt();
                    for (int j = 0; j < properties; j++) {
                        wrapper.passthroughUtf(); // Name
                        wrapper.passthroughUtf(); // Value
                        if (wrapper.passthroughBoolean()) { // Signature Present
                            wrapper.passthroughUtf(); // Signature
                        }
                    }

                    wrapper.passthroughVarInt(); // Gamemode
                    wrapper.passthroughVarInt(); // Ping
                    if (wrapper.passthroughBoolean()) { // Has Display Name
                        wrapper.passthroughComponent(); // Display Name
                    }

                    wrapper.writeBoolean(false); // Has Sig Data
                } else if (action == 1 || action == 2) { // Update gamemode or Update latency
                    wrapper.passthroughVarInt();
                } else if (action == 3) { // Update Display Name
                    // Optional Component
                    if (wrapper.passthroughBoolean()) {
                        wrapper.passthroughComponent();
                    }
                }
            }
        });

        // Respawn
        rewriteInbound(ConnectionProtocol.PLAY, 0x3B, wrapper -> {
            wrapper.passthroughUtf(); // Dimension Type
            wrapper.passthroughUtf(); // Dimension
            wrapper.passthroughLong(); // Hashed seed
            wrapper.passthroughUnsignedByte(); // Gamemode
            wrapper.passthroughByte(); // Previous Gamemode
            wrapper.passthroughBoolean(); // Is Debug
            wrapper.passthroughBoolean(); // Is Flat
            wrapper.passthroughBoolean(); // Keep player data
            wrapper.writeBoolean(false); // Has Last Death Location
        });

        // Entity Sound Effect
        rewriteInbound(ConnectionProtocol.PLAY, 0x5C, wrapper -> {
            wrapper.passthroughVarInt(); // Sound ID
            wrapper.passthroughVarInt(); // Sound Category
            wrapper.passthroughVarInt(); // Entity ID
            wrapper.passthroughFloat(); // Volume
            wrapper.passthroughFloat(); // Pitch
            wrapper.writeLong(randomLong()); // Seed
        });

        // Sound Effect
        rewriteInbound(ConnectionProtocol.PLAY, 0x5D, wrapper -> {
            wrapper.passthroughVarInt(); // Sound ID
            wrapper.passthroughVarInt(); // Sound Category
            wrapper.passthroughInt(); // Effect Position X
            wrapper.passthroughInt(); // Effect Position Y
            wrapper.passthroughInt(); // Effect Position Z
            wrapper.passthroughFloat(); // Volume
            wrapper.passthroughFloat(); // Pitch
            wrapper.writeLong(randomLong()); // Seed
        });

        // Chat Message -> System Chat Message
        rewriteInbound(ConnectionProtocol.PLAY, 0x5F, wrapper -> {
            wrapper.passthroughComponent(); // Message
            byte type = wrapper.readByte();
            if (type == 0) {
                wrapper.writeVarInt(1);
            } else {
                wrapper.writeVarInt(type);
            }
            wrapper.readUUID(); // Sender
        });
    }

    private static int to3dId(final int id) {
        switch (id) {
            case -1: return 1;
            case  2: return 2;
            case  0: return 3;
            case  1: return 4;
            case  3: return 5;
        }
        throw new IllegalArgumentException("Unknown 2d id: " + id);
    }

    @Override
    public void registerOutbound() {
        //----- LOGIN -----//
        // Login Start
        rewriteOutbound(ConnectionProtocol.LOGIN, 0x00, wrapper -> {
            wrapper.passthroughUtf(16); // Name
            if (wrapper.readBoolean()) { // Has Sig Data
                wrapper.readLong(); // Timestamp
                wrapper.readByteArray(); // Public Key Length, Public Key
                wrapper.readByteArray(); // Signature Length, Signature
            }
        });

        //----- PLAY -----//
        // + 0x03 Chat Command
        remapOutbound(ConnectionProtocol.PLAY, 0x03, 0xFE);
        remapOutbound(ConnectionProtocol.PLAY, 0x04, 0x03);
        // + 0x05 Chat Preview
        remapOutbound(ConnectionProtocol.PLAY, 0x05, 0xFF); // -> discard
        for (int oldId = 0x06; oldId <= 0x31; oldId++) {
            remapOutbound(ConnectionProtocol.PLAY, oldId, oldId - 2);
        }

        // Discard packets
        // + 0x05 Chat Preview
        rewriteOutbound(ConnectionProtocol.PLAY, 0xFF, PacketWrapper::cancel);

        // Chat Command
        rewriteOutbound(ConnectionProtocol.PLAY, 0xFE, wrapper -> {
            wrapper.cancel();
            writeOutboundPacket(ConnectionProtocol.PLAY, 0x03, buf -> {
                buf.writeUtf("/" + wrapper.readUtf()); // Command
                int signatures = wrapper.readVarInt();
                for (int i = 0; i < signatures; i++) {
                    wrapper.readUtf(); // Argument name
                    wrapper.readByteArray(); // Signature
                }
            });
        });

        // Chat Message
        rewriteOutbound(ConnectionProtocol.PLAY, 0x03, wrapper -> {
            wrapper.passthroughUtf(256); // Message
            wrapper.readLong(); // Timestamp
            wrapper.readLong(); // Salt
            wrapper.readByteArray(); // Signature
            wrapper.readBoolean(); // Signed Preview
        });

        // Player Digging
        rewriteOutbound(ConnectionProtocol.PLAY, 0x1A, wrapper -> {
            wrapper.passthroughVarInt(); // Status
            wrapper.passthroughBlockPos(); // Location
            wrapper.passthroughByte(); // Face
            sequenceHandler(wrapper); // Sequence
        });

        // Set Beacon Effect
        rewriteOutbound(ConnectionProtocol.PLAY, 0x26, wrapper -> {
            if (wrapper.readBoolean()) { // Primary Effect Present
                wrapper.passthroughVarInt(); // Primary Effect ID
            } else {
                wrapper.writeVarInt(-1); // Primary Effect ID (not present)
            }
            if (wrapper.readBoolean()) { // Secondary Effect Present
                wrapper.passthroughVarInt(); // Secondary Effect ID
            } else {
                wrapper.writeVarInt(-1); // Secondary Effect ID (not present)
            }
        });

        // Player Block Placement
        rewriteOutbound(ConnectionProtocol.PLAY, 0x2E, wrapper -> {
            wrapper.passthroughVarInt(); // Hand
            wrapper.passthroughBlockPos(); // Location
            wrapper.passthroughVarInt(); // Face
            wrapper.passthroughFloat(); // Cursor Position X
            wrapper.passthroughFloat(); // Cursor Position Y
            wrapper.passthroughFloat(); // Cursor Position Z
            wrapper.passthroughBoolean(); // Inside block
            sequenceHandler(wrapper); // Sequence
        });

        // Use Item
        rewriteOutbound(ConnectionProtocol.PLAY, 0x2F, wrapper -> {
            wrapper.passthroughVarInt(); // Hand
            sequenceHandler(wrapper); // Sequence
        });
    }

    @Override
    protected int remapEntityType(int entityType) {
        // returns offset
        int remappedId = MAPPING.remapIdFirstToSecond(MappingDataKey.ENTITY_TYPE, entityType);
        if (remappedId < 0) {
            throw new IllegalArgumentException("Failed to remap entity type: " + entityType);
        }
        return remappedId - entityType;
    }

    @Override
    protected int remapSoundId(int soundId) {
        return MAPPING.remapIdFirstToSecondOrThrow(MappingDataKey.SOUND_EVENT, soundId);
    }

    @Override
    protected int remapParticleId(int particleId) {
        return MAPPING.remapIdFirstToSecondOrThrow(MappingDataKey.PARTICLE_TYPE, particleId);
    }

    @Override
    protected @NotNull ItemStack rewriteInboundItemData(@NotNull PacketWrapper wrapper) {
        if (wrapper.passthroughBoolean()) { // present
            int itemId = wrapper.getRead().readVarInt();
            /*
            if (itemId >= 19) itemId++;
            if (itemId >= 28) itemId++;
            if (itemId >= 36) itemId++;
            if (itemId >= 107) itemId += 3;
            if (itemId >= 115) itemId++;
            if (itemId >= 123) itemId++;
            if (itemId >= 131) itemId++;
            if (itemId >= 139) itemId++;
            if (itemId >= 210) itemId++;
            if (itemId >= 220) itemId++;
            if (itemId == 244) itemId = 338; // OAK_STAIRS
            if (itemId >= 245) itemId--;
            if (itemId >= 263) itemId++;
            if (itemId >= 287) itemId += 2;
            if (itemId >= 292) itemId++;
            if (itemId >= 303) itemId++;
            if (itemId >= 310) itemId += 4;
            if (itemId >= 318) itemId++;
            if (itemId >= 321) itemId += 3;
            if (itemId >= 333) itemId++;
            if (itemId == 391) itemId = 342; // ACACIA_STAIRS
            if (itemId == 392) itemId = 343; // DARK_OAK_STAIRS
            if (itemId >= 393) itemId += 2;
            if (itemId >= 617) itemId++;
            if (itemId >= 629) itemId++;
            if (itemId >= 638) itemId++;
            if (itemId >= 647) itemId++;
            if (itemId >= 655) itemId++;
            if (itemId == 671) itemId = 700; // SPRUCE_BOAT
            if (itemId == 672) itemId = 702; // BIRCH_BOAT
            if (itemId == 673) itemId = 704; // JUNGLE_BOAT
            if (itemId == 674) itemId = 706; // ACACIA_BOAT
            if (itemId == 675) itemId = 708; // DARK_OAK_BOAT
            if (itemId >= 676) itemId += 8;
            if (itemId >= 774) itemId++;
            if (itemId >= 788) itemId++;
            if (itemId >= 796) itemId++;
            if (itemId >= 872) itemId++;
            if (itemId >= 891) itemId++;
            if (itemId >= 925) itemId++;
            if (itemId >= 932) itemId++;
            if (itemId >= 1028) itemId++;
            if (itemId >= 1029) itemId++;
            if (itemId >= 1042) itemId++;
            */
            itemId = MAPPING.remapIdFirstToSecond(MappingDataKey.ITEM, itemId);
            wrapper.writeVarInt(itemId);
            var count = wrapper.passthroughByte();
            var tag = wrapper.passthroughNbt();
            ItemStack item = new ItemStack(Item.byId(itemId), count);
            item.setTag(tag);
            return item;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    protected @NotNull ItemStack rewriteOutboundItemData(@NotNull PacketWrapper wrapper) {
        if (wrapper.passthroughBoolean()) { // present
            int itemId = wrapper.getRead().readVarInt();
            itemId = MAPPING.remapIdSecondToFirst(MappingDataKey.ITEM, itemId);
            if (itemId < 0) itemId = 1; // set to stone
            wrapper.writeVarInt(itemId);
            var count = wrapper.passthroughByte();
            var tag = wrapper.passthroughNbt();
            ItemStack item = new ItemStack(Item.byId(itemId), count);
            item.setTag(tag);
            return item;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    protected void registerItemRewriter() {
        registerItemRewriter(0x08, 0x28, 0x14, 0x16, 0x28, 0x4D, 0x50, 0x63, 0x66);
    }

    private void sequenceHandler(PacketWrapper wrapper) {
        int sequence = wrapper.readVarInt();
        writeBlockChange(sequence);
    }

    private void writeBlockChange(int sequence) {
        /*
        writeInboundPacket(ConnectionProtocol.PLAY, 0x05, buf -> {
            buf.writeVarInt(sequence); // Sequence ID
        });
        */
        // Schedule after 75ms
        Blueberry.getUtil().getClientScheduler().runTaskLaterAsynchronously(Objects.requireNonNull(Blueberry.getModLoader().getModById("multiversion")), 75, () -> {
            Minecraft mc = Minecraft.getInstance();
            //noinspection ConstantConditions
            if (mc != null) {
                mc.execute(() -> {
                    //noinspection ConstantConditions
                    if (mc == null || mc.player == null || mc.player.connection == null) return;
                    ClientPacketListener clientPacketListener = mc.player.connection;
                    clientPacketListener.handleBlockChangedAck(new ClientboundBlockChangedAckPacket(sequence));
                });
            }
        });
    }

    private static int getArgumentTypeId(@NotNull String argumentType) {
        return switch (argumentType) {
            case "brigadier:bool" -> 0;
            case "brigadier:float" -> 1;
            case "brigadier:double" -> 2;
            case "brigadier:integer" -> 3;
            case "brigadier:long" -> 4;
            case "brigadier:string" -> 5;
            case "minecraft:entity" -> 6;
            case "minecraft:game_profile" -> 7;
            case "minecraft:block_pos" -> 8;
            case "minecraft:column_pos" -> 9;
            case "minecraft:vec3" -> 10;
            case "minecraft:vec2" -> 11;
            case "minecraft:block_state" -> 12;
            case "minecraft:block_predicate" -> 13;
            case "minecraft:item_stack" -> 14;
            case "minecraft:item_predicate" -> 15;
            case "minecraft:color" -> 16;
            case "minecraft:component" -> 17;
            case "minecraft:message" -> 18;
            case "minecraft:nbt_compound_tag" -> 19;
            case "minecraft:nbt_tag" -> 20;
            case "minecraft:nbt_path" -> 21;
            case "minecraft:objective" -> 22;
            case "minecraft:objective_criteria" -> 23;
            case "minecraft:operation" -> 24;
            case "minecraft:particle" -> 25;
            case "minecraft:angle" -> 26;
            case "minecraft:rotation" -> 27;
            case "minecraft:scoreboard_slot" -> 28;
            case "minecraft:score_holder" -> 29;
            case "minecraft:swizzle" -> 30;
            case "minecraft:team" -> 31;
            case "minecraft:item_slot" -> 32;
            case "minecraft:resource_location" -> 33;
            case "minecraft:mob_effect" -> 34;
            case "minecraft:function" -> 35;
            case "minecraft:entity_anchor" -> 36;
            case "minecraft:int_range" -> 37;
            case "minecraft:float_range" -> 38;
            case "minecraft:item_enchantment" -> 39;
            case "minecraft:entity_summon" -> 40;
            case "minecraft:dimension" -> 41;
            case "minecraft:time" -> 42;
            case "minecraft:resource_or_tag" -> 43;
            case "minecraft:resource" -> 44;
            case "minecraft:template_mirror" -> 45;
            case "minecraft:template_rotation" -> 46;
            case "minecraft:uuid" -> 47;
            default -> -1; // unknown argument type
        };
    }

    private static long randomLong() {
        return ThreadLocalRandom.current().nextLong();
    }

    private static class ChunkSection {
        public static ChunkSection emptySection;
        public static final int SIZE = 16 * 16 * 16;
        public final short nonEmptyBlockCount;
        public DataPalettes statesType;
        public DataPalette states = null;
        public DataPalettes biomesType;
        public DataPalette biomes = null;

        private ChunkSection(short nonEmptyBlockCount, int statesGlobalPaletteBits, int biomesGlobalPaletteBits) {
            this.nonEmptyBlockCount = nonEmptyBlockCount;
            this.statesType = new DataPalettes(DataPaletteType.STATES, statesGlobalPaletteBits);
            this.biomesType = new DataPalettes(DataPaletteType.BIOMES, biomesGlobalPaletteBits);
        }

        @NotNull
        public static ChunkSection readChunkSection(@NotNull PacketWrapper wrapper, int statesGlobalPaletteBits, int biomesGlobalPaletteBits) {
            short nonEmptyBlockCount = wrapper.readShort();
            return new ChunkSection(nonEmptyBlockCount, statesGlobalPaletteBits, biomesGlobalPaletteBits);
        }
    }

    public enum DataPaletteType {
        STATES(ChunkSection.SIZE, 8),
        BIOMES(4 * 4 * 4, 2),
        ;

        private final int maxSize;
        private final int highestBitsPerValue;

        DataPaletteType(int maxSize, int highestBitsPerValue) {
            this.maxSize = maxSize;
            this.highestBitsPerValue = highestBitsPerValue;
        }
    }

    public record DataPalettes(@NotNull DataPaletteType type, int globalPaletteBits) {
        @NotNull
        public DataPalette readPalette(@NotNull PacketWrapper wrapper) {
            int bitsPerValue = wrapper.readByte();
            int originalBitsPerValue = bitsPerValue;
            if (bitsPerValue > type.highestBitsPerValue) {
                bitsPerValue = globalPaletteBits;
            }
            DataPalette palette;
            if (bitsPerValue == 0) {
                palette = new DataPalette(1);
                palette.addId(wrapper.readVarInt());
                wrapper.readVarInt();
                return palette;
            }
            if (bitsPerValue != globalPaletteBits) {
                int paletteLength = wrapper.readVarInt();
                palette = new DataPalette(paletteLength);
                for (int i = 0; i < paletteLength; i++) {
                    palette.addId(wrapper.readVarInt());
                }
            } else {
                palette = new DataPalette();
            }
            long[] values = new long[wrapper.readVarInt()];
            if (values.length > 0) {
                char valuesPerLong = (char) (64 / bitsPerValue);
                int expectedLength = (type.maxSize + valuesPerLong - 1) / valuesPerLong;
                if (values.length != expectedLength) {
                    throw new IllegalStateException("Palette data length (" + values.length + ") does not match expected length (" + expectedLength + ")! bitsPerValue=" + bitsPerValue + ", originalBitsPerValue=" + originalBitsPerValue);
                }
                for (int i = 0; i < values.length; i++) {
                    values[i] = wrapper.readLong();
                }
                CompactArrayUtil.iterateCompactArrayWithPadding(
                        bitsPerValue,
                        type.maxSize,
                        values,
                        bitsPerValue == globalPaletteBits ? palette::setIdAt : palette::setPaletteIndexAt
                );
            }
            return palette;
        }

        public void writePalette(@NotNull PacketWrapper wrapper, @NotNull DataPalette palette) {
            int bitsPerValue;
            if (palette.size() > 1) {
                bitsPerValue = type == DataPaletteType.STATES ? 4 : 1;
                while (palette.size() > 1 << bitsPerValue) {
                    bitsPerValue++;
                }
                if (bitsPerValue > type.highestBitsPerValue) {
                    bitsPerValue = globalPaletteBits;
                }
            } else {
                bitsPerValue = 0;
            }
            wrapper.writeByte(bitsPerValue);
            if (bitsPerValue == 0) {
                wrapper.writeVarInt(palette.idByIndex(0));
                wrapper.writeVarInt(0);
                return;
            }
            if (bitsPerValue != globalPaletteBits) {
                wrapper.writeVarInt(palette.size());
                for (int i = 0; i < palette.size(); i++) {
                    wrapper.writeVarInt(palette.idByIndex(i));
                }
            }
            long[] data = CompactArrayUtil.createCompactArrayWithPadding(
                    bitsPerValue,
                    type.maxSize,
                    bitsPerValue == globalPaletteBits ? palette::idAt : palette::paletteIndexAt
            );
            wrapper.writeVarInt(data.length);
            for (long l : data) {
                wrapper.writeLong(l);
            }
        }
    }

    public static class DataPalette {
        private final int[] values;
        private final IntList palette;
        private final Int2IntMap inversePalette;

        public DataPalette() {
            this.values = new int[ChunkSection.SIZE];
            palette = new IntArrayList();
            inversePalette = new Int2IntOpenHashMap();
            inversePalette.defaultReturnValue(-1);
        }

        public DataPalette(int expectedSize) {
            this.values = new int[ChunkSection.SIZE];
            palette = new IntArrayList(expectedSize);
            inversePalette = new Int2IntOpenHashMap(expectedSize);
            inversePalette.defaultReturnValue(-1);
        }

        public int idAt(int sectionCoordinate) {
            int index = values[sectionCoordinate];
            return palette.getInt(index);
        }

        public int idAt(int secX, int secY, int secZ) {
            return idAt(index(secX, secY, secZ));
        }

        public void setIdAt(int sectionCoordinate, int id) {
            int index = inversePalette.get(id);
            if (index == -1) {
                index = palette.size();
                palette.add(id);
                inversePalette.put(id, index);
            }
            values[sectionCoordinate] = index;
        }

        public void setIdAt(int secX, int secY, int secZ, int id) {
            setIdAt(index(secX, secY, secZ), id);
        }

        public int idByIndex(int index) {
            return palette.getInt(index);
        }

        /*
        public void setIdByIndex(int index, int id) {
            int oldId = palette.set(index, id);
            if (oldId == id) return;
            inversePalette.put(id, index);
            if (inversePalette.get(oldId) == index) {
                inversePalette.remove(oldId);
                for (int i = 0; i < palette.size(); i++) {
                    if (palette.getInt(i) == oldId) {
                        inversePalette.put(oldId, i);
                        break;
                    }
                }
            }
        }
        */

        public int paletteIndexAt(int packedCoordinate) {
            return values[packedCoordinate];
        }

        public void setPaletteIndexAt(int sectionCoordinate, int index) {
            values[sectionCoordinate] = index;
        }

        public void addId(int id) {
            inversePalette.put(id, palette.size());
            palette.add(id);
        }

        /*
        public void replaceId(int oldId, int newId) {
            int index = inversePalette.remove(oldId);
            if (index == -1) return;
            inversePalette.put(newId, index);
            for (int i = 0; i < palette.size(); i++) {
                if (palette.getInt(i) == oldId) {
                    palette.set(i, newId);
                }
            }
        }
        */

        public int size() {
            return palette.size();
        }

        public void setEntry(int index, int id) {
            int oldId = palette.set(index, id);
            if (oldId == id) return;
            inversePalette.put(id, index);
            if (inversePalette.get(oldId) == index) {
                inversePalette.remove(oldId);
                for (int i = 0; i < palette.size(); i++) {
                    if (palette.getInt(i) == oldId) {
                        inversePalette.put(oldId, i);
                        break;
                    }
                }
            }
        }

        /*
        public void replaceEntry(int oldId, int newId) {
            final int index = inversePalette.remove(oldId);
            if (index == -1) return;
            inversePalette.put(newId, index);
            for (int i = 0; i < palette.size(); i++) {
                if (palette.getInt(i) == oldId) {
                    palette.set(i, newId);
                }
            }
        }
        */

        public void addEntry(int id) {
            inversePalette.put(id, palette.size());
            palette.add(id);
        }

        public void clear() {
            palette.clear();
            inversePalette.clear();
        }

        public static int index(int x, int y, int z) {
            return y << 8 | z << 4 | x;
        }
    }
}
