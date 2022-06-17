package xyz.acrylicstyle.multiVersion.transformer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;
import it.unimi.dsi.fastutil.ints.IntList;
import net.blueberrymc.util.IntPair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.IdMap;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

@SuppressWarnings({ "unused", "UnusedReturnValue" })
public class PacketWrapper extends FriendlyByteBuf {
    private FriendlyByteBuf read;
    private FriendlyByteBuf write;
    private boolean readIsPassthrough = false;
    private boolean cancelled = false;

    public PacketWrapper(@NotNull ByteBuf read) {
        this(read, Unpooled.buffer());
    }

    public PacketWrapper(@NotNull ByteBuf read, @NotNull ByteBuf write) {
        super(read);
        this.read = new FriendlyByteBuf(read);
        this.write = new FriendlyByteBuf(write);
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isReadIsPassthrough() {
        return readIsPassthrough;
    }

    @Override
    public int indexOf(int i, int i2, byte b) {
        return getRead().indexOf(i, i2, b);
    }

    @Override
    public int bytesBefore(byte b) {
        return getRead().bytesBefore(b);
    }

    @Override
    public int bytesBefore(int i, byte b) {
        return getRead().bytesBefore(i, b);
    }

    @Override
    public int bytesBefore(int i, int i2, byte b) {
        return getRead().bytesBefore(i, i2, b);
    }

    @Override
    public int forEachByte(@NotNull ByteProcessor byteProcessor) {
        return getRead().forEachByte(byteProcessor);
    }

    @Override
    public int forEachByte(int i, int i2, @NotNull ByteProcessor byteProcessor) {
        return getRead().forEachByte(i, i2, byteProcessor);
    }

    @Override
    public int forEachByteDesc(@NotNull ByteProcessor byteProcessor) {
        return getRead().forEachByteDesc(byteProcessor);
    }

    @Override
    public int forEachByteDesc(int i, int i2, @NotNull ByteProcessor byteProcessor) {
        return getRead().forEachByteDesc(i, i2, byteProcessor);
    }

    @Override
    public @NotNull ByteBuf copy() {
        return getRead().copy();
    }

    @Override
    public @NotNull ByteBuf copy(int i, int i2) {
        return getRead().copy(i, i2);
    }

    @Override
    public @NotNull ByteBuf slice() {
        return getRead().slice();
    }

    @Override
    public @NotNull ByteBuf retainedSlice() {
        return getRead().retainedSlice();
    }

    @Override
    public @NotNull ByteBuf slice(int i, int i2) {
        return getRead().slice(i, i2);
    }

    @Override
    public @NotNull ByteBuf retainedSlice(int i, int i2) {
        return getRead().retainedSlice(i, i2);
    }

    @Override
    public @NotNull ByteBuf duplicate() {
        return getRead().duplicate();
    }

    @Override
    public @NotNull ByteBuf retainedDuplicate() {
        return getRead().retainedDuplicate();
    }

    @Override
    public int nioBufferCount() {
        return getRead().nioBufferCount();
    }

    @Override
    public @NotNull ByteBuffer nioBuffer() {
        return getRead().nioBuffer();
    }

    @Override
    public @NotNull ByteBuffer nioBuffer(int i, int i2) {
        return getRead().nioBuffer(i, i2);
    }

    @Override
    public @NotNull ByteBuffer internalNioBuffer(int i, int i2) {
        return getRead().internalNioBuffer(i, i2);
    }

    @Override
    public ByteBuffer @NotNull [] nioBuffers() {
        return getRead().nioBuffers();
    }

    @Override
    public ByteBuffer @NotNull [] nioBuffers(int i, int i2) {
        return getRead().nioBuffers(i, i2);
    }

    @Override
    public boolean hasArray() {
        return getRead().hasArray();
    }

    @Override
    public byte @NotNull [] array() {
        return getRead().array();
    }

    @Override
    public int arrayOffset() {
        return getRead().arrayOffset();
    }

    @Override
    public boolean hasMemoryAddress() {
        return getRead().hasMemoryAddress();
    }

    @Override
    public long memoryAddress() {
        return getRead().memoryAddress();
    }

    @Override
    public @NotNull String toString(@NotNull Charset charset) {
        return getRead().toString(charset);
    }

    @Override
    public @NotNull String toString(int i, int i2, @NotNull Charset charset) {
        return getRead().toString(i, i2, charset);
    }

    @Override
    public @NotNull ByteBuf retain(int i) {
        return getRead().retain(i);
    }

    @Override
    public @NotNull ByteBuf retain() {
        return getRead().retain();
    }

    @Override
    public @NotNull ByteBuf touch() {
        return getRead().touch();
    }

    @Override
    public @NotNull ByteBuf touch(@NotNull Object object) {
        return getRead().touch(object);
    }

    @Override
    public boolean release() {
        return getRead().release() && getWrite().release();
    }

    @Override
    public boolean release(int i) {
        return getRead().release(i) && getWrite().release(i);
    }

    @Override
    public @NotNull ByteBuf setBoolean(int i, boolean flag) {
        return getWrite().setBoolean(i, flag);
    }

    @Override
    public @NotNull ByteBuf setByte(int i, int i2) {
        return getWrite().setByte(i, i2);
    }

    @Override
    public @NotNull ByteBuf setShort(int i, int i2) {
        return getWrite().setShort(i, i2);
    }

    @Override
    public @NotNull ByteBuf setShortLE(int i, int i2) {
        return getWrite().setShortLE(i, i2);
    }

    @Override
    public @NotNull ByteBuf setMedium(int i, int i2) {
        return getWrite().setMedium(i, i2);
    }

    @Override
    public @NotNull ByteBuf setMediumLE(int i, int i2) {
        return getWrite().setMediumLE(i, i2);
    }

    @Override
    public @NotNull ByteBuf setInt(int i, int i2) {
        return getWrite().setInt(i, i2);
    }

    @Override
    public @NotNull ByteBuf setIntLE(int i, int i2) {
        return getWrite().setIntLE(i, i2);
    }

    @Override
    public @NotNull ByteBuf setLong(int i, long l) {
        return getWrite().setLong(i, l);
    }

    @Override
    public @NotNull ByteBuf setLongLE(int i, long l) {
        return getWrite().setLongLE(i, l);
    }

    @Override
    public @NotNull ByteBuf setChar(int i, int i2) {
        return getWrite().setChar(i, i2);
    }

    @Override
    public @NotNull ByteBuf setFloat(int i, float f) {
        return getWrite().setFloat(i, f);
    }

    @Override
    public @NotNull ByteBuf setDouble(int i, double d) {
        return getWrite().setDouble(i, d);
    }

    @Override
    public @NotNull ByteBuf setBytes(int i, @NotNull ByteBuf byteBuf) {
        return getWrite().setBytes(i, byteBuf);
    }

    @Override
    public @NotNull ByteBuf setBytes(int i, @NotNull ByteBuf byteBuf, int i2) {
        return getWrite().setBytes(i, byteBuf, i2);
    }

    @Override
    public @NotNull ByteBuf setBytes(int i, @NotNull ByteBuf byteBuf, int i2, int i3) {
        return getWrite().setBytes(i, byteBuf, i2, i3);
    }

    @Override
    public @NotNull ByteBuf setBytes(int i, byte @NotNull [] bytes) {
        return getWrite().setBytes(i, bytes);
    }

    @Override
    public @NotNull ByteBuf setBytes(int i, byte @NotNull [] bytes, int i2, int i3) {
        return getWrite().setBytes(i, bytes, i2, i3);
    }

    @Override
    public @NotNull ByteBuf setBytes(int i, @NotNull ByteBuffer byteBuffer) {
        return getWrite().setBytes(i, byteBuffer);
    }

    @Override
    public int setBytes(int i, @NotNull InputStream inputStream, int i2) throws IOException {
        return getWrite().setBytes(i, inputStream, i2);
    }

    @Override
    public int setBytes(int i, @NotNull ScatteringByteChannel scatteringByteChannel, int i2) throws IOException {
        return getWrite().setBytes(i, scatteringByteChannel, i2);
    }

    @Override
    public int setBytes(int i, @NotNull FileChannel fileChannel, long l, int i2) throws IOException {
        return getWrite().setBytes(i, fileChannel, l, i2);
    }

    @Override
    public @NotNull ByteBuf setZero(int i, int i2) {
        return getWrite().setZero(i, i2);
    }

    @Override
    public int setCharSequence(int i, @NotNull CharSequence charSequence, @NotNull Charset charset) {
        return getWrite().setCharSequence(i, charSequence, charset);
    }

    @Override
    public int capacity() {
        return getRead().capacity();
    }

    @NotNull
    @Override
    public ByteBuf capacity(int i) {
        return getRead().capacity(i);
    }

    @Override
    public int maxCapacity() {
        return getRead().maxCapacity();
    }

    @NotNull
    @Override
    public ByteBufAllocator alloc() {
        return getRead().alloc();
    }

    @NotNull
    @Override
    public ByteOrder order() {
        return getRead().order();
    }

    @NotNull
    @Override
    public ByteBuf order(@NotNull ByteOrder byteOrder) {
        return getRead().order(byteOrder);
    }

    @NotNull
    @Override
    public ByteBuf unwrap() {
        return getRead().unwrap();
    }

    @Override
    public boolean isDirect() {
        return getRead().isDirect();
    }

    @Override
    public boolean isReadOnly() {
        return getRead().isReadOnly();
    }

    @NotNull
    @Override
    public ByteBuf asReadOnly() {
        return getRead().asReadOnly();
    }

    @Override
    public boolean getBoolean(int i) {
        return getRead().getBoolean(i);
    }

    @Override
    public byte getByte(int i) {
        return getRead().getByte(i);
    }

    @Override
    public short getUnsignedByte(int i) {
        return getRead().getUnsignedByte(i);
    }

    @Override
    public short getShort(int i) {
        return getRead().getShort(i);
    }

    @Override
    public short getShortLE(int i) {
        return getRead().getShortLE(i);
    }

    @Override
    public int getUnsignedShort(int i) {
        return getRead().getUnsignedShort(i);
    }

    @Override
    public int getUnsignedShortLE(int i) {
        return getRead().getUnsignedShortLE(i);
    }

    @Override
    public int getMedium(int i) {
        return getRead().getMedium(i);
    }

    @Override
    public int getMediumLE(int i) {
        return getRead().getMediumLE(i);
    }

    @Override
    public int getUnsignedMedium(int i) {
        return getRead().getUnsignedMedium(i);
    }

    @Override
    public int getUnsignedMediumLE(int i) {
        return getRead().getUnsignedMediumLE(i);
    }

    @Override
    public int getInt(int i) {
        return getRead().getInt(i);
    }

    @Override
    public int getIntLE(int i) {
        return getRead().getIntLE(i);
    }

    @Override
    public long getUnsignedInt(int i) {
        return getRead().getUnsignedInt(i);
    }

    @Override
    public long getUnsignedIntLE(int i) {
        return getRead().getUnsignedIntLE(i);
    }

    @Override
    public long getLong(int i) {
        return getRead().getLong(i);
    }

    @Override
    public long getLongLE(int i) {
        return getRead().getLongLE(i);
    }

    @Override
    public char getChar(int i) {
        return getRead().getChar(i);
    }

    @Override
    public float getFloat(int i) {
        return getRead().getFloat(i);
    }

    @Override
    public double getDouble(int i) {
        return getRead().getDouble(i);
    }

    @NotNull
    @Override
    public ByteBuf getBytes(int i, @NotNull ByteBuf byteBuf) {
        return getRead().getBytes(i, byteBuf);
    }

    @NotNull
    @Override
    public ByteBuf getBytes(int i, @NotNull ByteBuf byteBuf, int i2) {
        return getRead().getBytes(i, byteBuf, i2);
    }

    @NotNull
    @Override
    public ByteBuf getBytes(int i, @NotNull ByteBuf byteBuf, int i2, int i3) {
        return getRead().getBytes(i, byteBuf, i2, i3);
    }

    @NotNull
    @Override
    public ByteBuf getBytes(int i, byte@NotNull[] bytes) {
        return getRead().getBytes(i, bytes);
    }

    @NotNull
    @Override
    public ByteBuf getBytes(int i, byte@NotNull[] bytes, int i2, int i3) {
        return getRead().getBytes(i, bytes, i2, i3);
    }

    @NotNull
    @Override
    public ByteBuf getBytes(int i, @NotNull ByteBuffer byteBuffer) {
        return getRead().getBytes(i, byteBuffer);
    }

    @NotNull
    @Override
    public ByteBuf getBytes(int i, @NotNull OutputStream outputStream, int i2) throws IOException {
        return getRead().getBytes(i, outputStream, i2);
    }

    @Override
    public int getBytes(int i, @NotNull GatheringByteChannel gatheringByteChannel, int i2) throws IOException {
        return getRead().getBytes(i, gatheringByteChannel, i2);
    }

    @Override
    public int getBytes(int i, @NotNull FileChannel fileChannel, long l, int i2) throws IOException {
        return getRead().getBytes(i, fileChannel, l, i2);
    }

    @NotNull
    @Override
    public CharSequence getCharSequence(int i, int i2, @NotNull Charset charset) {
        return getRead().getCharSequence(i, i2, charset);
    }

    @NotNull
    @Override
    public ByteBuf setIndex(int i, int i2) {
        return getRead().setIndex(i, i2);
    }

    @NotNull
    @Override
    public ByteBuf clear() {
        return getRead().clear();
    }

    @NotNull
    @Override
    public ByteBuf discardReadBytes() {
        return getRead().discardReadBytes();
    }

    @NotNull
    @Override
    public ByteBuf discardSomeReadBytes() {
        return getRead().discardSomeReadBytes();
    }

    @NotNull
    @Override
    public ByteBuf ensureWritable(int i) {
        return getWrite().ensureWritable(i);
    }

    @Override
    public int ensureWritable(int i, boolean flag) {
        return getWrite().ensureWritable(i, flag);
    }

    @Override
    public int refCnt() {
        return getRead().refCnt();
    }

    public void setReadIsPassthrough(boolean readIsPassthrough) {
        this.readIsPassthrough = readIsPassthrough;
    }

    public void readIsPassthrough(@NotNull Runnable runnable) {
        setReadIsPassthrough(true);
        try {
            runnable.run();
        } finally {
            setReadIsPassthrough(false);
        }
    }

    @NotNull
    public FriendlyByteBuf getRead() {
        return read;
    }

    public void setRead(@NotNull FriendlyByteBuf read) {
        this.read = read;
    }

    @NotNull
    public FriendlyByteBuf getWrite() {
        return write;
    }

    public void setWrite(@NotNull FriendlyByteBuf write) {
        this.write = write;
    }

    @NotNull
    public PacketWrapper swap() {
        FriendlyByteBuf temp = getRead();
        setRead(getWrite());
        setWrite(temp);
        return this;
    }

    @NotNull
    public PacketWrapper writeBoolean(boolean flag) {
        getWrite().writeBoolean(flag);
        return this;
    }

    @NotNull
    public PacketWrapper writeByte(int i) {
        getWrite().writeByte(i);
        return this;
    }

    @NotNull
    public PacketWrapper writeBytes(byte@NotNull[] bytes) {
        getWrite().writeBytes(bytes);
        return this;
    }

    @NotNull
    public PacketWrapper writeBytes(@NotNull ByteBuf byteBuf) {
        getWrite().writeBytes(byteBuf);
        return this;
    }

    @NotNull
    public PacketWrapper writeBytes(@NotNull ByteBuffer byteBuffer) {
        getWrite().writeBytes(byteBuffer);
        return this;
    }

    @NotNull
    public PacketWrapper writeBytes(@NotNull ByteBuf byteBuf, int i) {
        getWrite().writeBytes(byteBuf, i);
        return this;
    }

    @NotNull
    public PacketWrapper writeBytes(byte@NotNull[] bytes, int i, int i2) {
        getWrite().writeBytes(bytes, i, i2);
        return this;
    }

    @NotNull
    public PacketWrapper writeBytes(@NotNull ByteBuf byteBuf, int i, int i2) {
        getWrite().writeBytes(byteBuf, i, i2);
        return this;
    }

    @NotNull
    public PacketWrapper writeChar(int i) {
        getWrite().writeChar(i);
        return this;
    }

    @NotNull
    public PacketWrapper writeDouble(double d) {
        getWrite().writeDouble(d);
        return this;
    }

    @NotNull
    public PacketWrapper writeDoubleLE(double value) {
        getWrite().writeDoubleLE(value);
        return this;
    }

    @NotNull
    public PacketWrapper writeFloat(float f) {
        getWrite().writeFloat(f);
        return this;
    }

    @NotNull
    public PacketWrapper writeFloatLE(float value) {
        getWrite().writeFloatLE(value);
        return this;
    }

    @NotNull
    public PacketWrapper writeInt(int i) {
        getWrite().writeInt(i);
        return this;
    }

    @NotNull
    public PacketWrapper writeIntLE(int i) {
        getWrite().writeIntLE(i);
        return this;
    }

    @NotNull
    public PacketWrapper writeLong(long l) {
        getWrite().writeLong(l);
        return this;
    }

    @NotNull
    public PacketWrapper writeLongLE(long l) {
        getWrite().writeLongLE(l);
        return this;
    }

    @NotNull
    public PacketWrapper writeMedium(int i) {
        getWrite().writeMedium(i);
        return this;
    }

    @NotNull
    public PacketWrapper writeMediumLE(int i) {
        getWrite().writeMediumLE(i);
        return this;
    }

    @NotNull
    public PacketWrapper writerIndex(int i) {
        getWrite().writerIndex(i);
        return this;
    }

    @NotNull
    public PacketWrapper writeShort(int i) {
        getWrite().writeShort(i);
        return this;
    }

    @NotNull
    public PacketWrapper writeShortLE(int i) {
        getWrite().writeShortLE(i);
        return this;
    }

    @NotNull
    public PacketWrapper writeZero(int i) {
        getWrite().writeZero(i);
        return this;
    }

    @NotNull
    public PacketWrapper writeBlockPos(@NotNull BlockPos blockPos) {
        getWrite().writeBlockPos(blockPos);
        return this;
    }

    @NotNull
    public PacketWrapper writeByteArray(byte@NotNull[] bytes) {
        getWrite().writeByteArray(bytes);
        return this;
    }

    @NotNull
    public PacketWrapper writeChunkPos(@NotNull ChunkPos chunkPos) {
        getWrite().writeChunkPos(chunkPos);
        return this;
    }

    @NotNull
    public PacketWrapper writeComponent(@NotNull Component component) {
        getWrite().writeComponent(component);
        return this;
    }

    @NotNull
    public PacketWrapper writeDate(@NotNull Date date) {
        getWrite().writeDate(date);
        return this;
    }

    @NotNull
    public PacketWrapper writeEnum(@NotNull Enum<?> enum_) {
        getWrite().writeEnum(enum_);
        return this;
    }

    @NotNull
    public PacketWrapper writeItem(@NotNull ItemStack itemStack) {
        getWrite().writeItem(itemStack);
        return this;
    }

    @NotNull
    public PacketWrapper writeLongArray(long@NotNull[] longs) {
        getWrite().writeLongArray(longs);
        return this;
    }

    @NotNull
    public PacketWrapper writeNbt(@Nullable CompoundTag compoundTag) {
        getWrite().writeNbt(compoundTag);
        return this;
    }

    @NotNull
    public PacketWrapper writeResourceLocation(@NotNull ResourceLocation resourceLocation) {
        getWrite().writeResourceLocation(resourceLocation);
        return this;
    }

    @NotNull
    public PacketWrapper writeSectionPos(@NotNull SectionPos sectionPos) {
        getWrite().writeSectionPos(sectionPos);
        return this;
    }

    @NotNull
    public PacketWrapper writeUtf(@NotNull String s) {
        getWrite().writeUtf(s);
        return this;
    }

    @NotNull
    public PacketWrapper writeUtf(@NotNull String s, int i) {
        getWrite().writeUtf(s, i);
        return this;
    }

    @NotNull
    public PacketWrapper writeUUID(@NotNull UUID uuid) {
        getWrite().writeUUID(uuid);
        return this;
    }

    @NotNull
    public PacketWrapper writeVarInt(int i) {
        getWrite().writeVarInt(i);
        return this;
    }

    @NotNull
    public PacketWrapper writeVarIntArray(int@NotNull[] intArray) {
        getWrite().writeVarIntArray(intArray);
        return this;
    }

    @NotNull
    public PacketWrapper writeVarLong(long l) {
        getWrite().writeVarLong(l);
        return this;
    }

    public boolean readBoolean() {
        if (readIsPassthrough) return passthroughBoolean();
        return getRead().readBoolean();
    }

    public byte readByte() {
        if (readIsPassthrough) return passthroughByte();
        return getRead().readByte();
    }

    public short readUnsignedByte() {
        if (readIsPassthrough) return passthroughUnsignedByte();
        return getRead().readUnsignedByte();
    }

    public short readShort() {
        if (readIsPassthrough) return passthroughShort();
        return getRead().readShort();
    }

    public short readShortLE() {
        if (readIsPassthrough) return passthroughShortLE();
        return getRead().readShortLE();
    }

    public int readUnsignedShort() {
        if (readIsPassthrough) return passthroughUnsignedShort();
        return getRead().readUnsignedShort();
    }

    public int readUnsignedShortLE() {
        if (readIsPassthrough) return passthroughUnsignedShortLE();
        return getRead().readUnsignedShortLE();
    }

    public int readMedium() {
        if (readIsPassthrough) return passthroughMedium();
        return getRead().readMedium();
    }

    public int readMediumLE() {
        if (readIsPassthrough) return passthroughMediumLE();
        return getRead().readMediumLE();
    }

    public int readUnsignedMedium() {
        if (readIsPassthrough) return passthroughUnsignedMedium();
        return getRead().readUnsignedMedium();
    }

    public int readUnsignedMediumLE() {
        if (readIsPassthrough) return passthroughUnsignedMediumLE();
        return getRead().readUnsignedMediumLE();
    }

    public int readInt() {
        if (readIsPassthrough) return passthroughInt();
        return getRead().readInt();
    }

    public int readIntLE() {
        if (readIsPassthrough) return passthroughIntLE();
        return getRead().readIntLE();
    }

    public long readUnsignedInt() {
        if (readIsPassthrough) return passthroughUnsignedInt();
        return getRead().readUnsignedInt();
    }

    public long readUnsignedIntLE() {
        if (readIsPassthrough) return passthroughUnsignedIntLE();
        return getRead().readUnsignedIntLE();
    }

    public long readLong() {
        if (readIsPassthrough) return passthroughLong();
        return getRead().readLong();
    }

    public long readLongLE() {
        if (readIsPassthrough) return passthroughLongLE();
        return getRead().readLongLE();
    }

    public char readChar() {
        if (readIsPassthrough) return passthroughChar();
        return getRead().readChar();
    }

    public float readFloat() {
        if (readIsPassthrough) return passthroughFloat();
        return getRead().readFloat();
    }

    public double readDouble() {
        if (readIsPassthrough) return passthroughDouble();
        return getRead().readDouble();
    }

    @NotNull
    public ByteBuf readBytes(int i) {
        if (readIsPassthrough) return passthroughBytes(i);
        return getRead().readBytes(i);
    }

    @NotNull
    public ByteBuf readSlice(int i) {
        if (readIsPassthrough) return passthroughSlice(i);
        return getRead().readSlice(i);
    }

    @NotNull
    public ByteBuf readRetainedSlice(int i) {
        if (readIsPassthrough) return passthroughRetainedSlice(i);
        return getRead().readRetainedSlice(i);
    }

    @NotNull
    public PacketWrapper readBytes(@NotNull ByteBuf byteBuf) {
        if (readIsPassthrough) {
            passthroughBytes(byteBuf);
            return this;
        }
        getRead().readBytes(byteBuf);
        return this;
    }

    @NotNull
    public PacketWrapper readBytes(@NotNull ByteBuf byteBuf, int length) {
        if (readIsPassthrough) {
            passthroughBytes(byteBuf, length);
            return this;
        }
        getRead().readBytes(byteBuf, length);
        return this;
    }

    @NotNull
    public PacketWrapper readBytes(@NotNull ByteBuf byteBuf, int dstIndex, int length) {
        if (readIsPassthrough) {
            passthroughBytes(byteBuf, dstIndex, length);
            return this;
        }
        getRead().readBytes(byteBuf, dstIndex, length);
        return this;
    }

    @NotNull
    public PacketWrapper readBytes(byte@NotNull[] bytes) {
        if (readIsPassthrough) {
            passthroughBytes(bytes);
            return this;
        }
        getRead().readBytes(bytes);
        return this;
    }

    @NotNull
    public PacketWrapper readBytes(byte@NotNull[] bytes, int dstIndex, int length) {
        if (readIsPassthrough) {
            passthroughBytes(bytes, dstIndex, length);
            return this;
        }
        getRead().readBytes(bytes, dstIndex, length);
        return this;
    }

    @NotNull
    public PacketWrapper readBytes(@NotNull ByteBuffer dst) {
        if (readIsPassthrough) {
            int pos = dst.position();
            getRead().readBytes(dst);
            int pos2 = dst.position();
            dst.position(pos);
            getWrite().writeBytes(dst);
            dst.position(pos2);
            return this;
        }
        getRead().readBytes(dst);
        return this;
    }

    // no readIsPassthrough
    @NotNull
    public PacketWrapper readBytes(@NotNull OutputStream outputStream, int i) throws IOException {
        getRead().readBytes(outputStream, i);
        return this;
    }

    // no readIsPassthrough
    public int readBytes(@NotNull GatheringByteChannel gatheringByteChannel, int i) throws IOException {
        return getRead().readBytes(gatheringByteChannel, i);
    }

    @NotNull
    public CharSequence readCharSequence(int i, @NotNull Charset charset) {
        if (readIsPassthrough) return passthroughCharSequence(i, charset);
        return getRead().readCharSequence(i, charset);
    }

    // no readIsPassthrough
    public int readBytes(@NotNull FileChannel fileChannel, long l, int i) throws IOException {
        return getRead().readBytes(fileChannel, l, i);
    }

    @NotNull
    public PacketWrapper skipBytes(int i) {
        getRead().skipBytes(i);
        getWrite().skipBytes(i);
        return this;
    }

    public int writeBytes(@NotNull InputStream inputStream, int i) throws IOException {
        return getWrite().writeBytes(inputStream, i);
    }

    public int writeBytes(@NotNull ScatteringByteChannel scatteringByteChannel, int i) throws IOException {
        return getWrite().writeBytes(scatteringByteChannel, i);
    }

    public int writeBytes(@NotNull FileChannel fileChannel, long l, int i) throws IOException {
        return getWrite().writeBytes(fileChannel, l, i);
    }

    public int writeCharSequence(@NotNull CharSequence charSequence, @NotNull Charset charset) {
        return getWrite().writeCharSequence(charSequence, charset);
    }

    @NotNull
    public <T> T readWithCodec(@NotNull Codec<T> codec) {
        if (readIsPassthrough) return passthroughWithCodec(codec);
        return getRead().readWithCodec(codec);
    }

    public <T> void writeWithCodec(@NotNull Codec<T> codec, @NotNull T object) {
        getWrite().writeWithCodec(codec, object);
    }

    @NotNull
    public <T, C extends Collection<T>> C readCollection(@NotNull IntFunction<C> toCollectionFunction, @NotNull Reader<T> valueFunction) {
        if (readIsPassthrough) return passthroughCollection(toCollectionFunction, valueFunction);
        return getRead().readCollection(toCollectionFunction, valueFunction);
    }

    public <T> void writeCollection(@NotNull Collection<T> collection, @NotNull Writer<T> biConsumer) {
        getWrite().writeCollection(collection, biConsumer);
    }

    @NotNull
    public <T> List<T> readList(@NotNull Reader<T> valueFunction) {
        return this.readCollection(Lists::newArrayListWithCapacity, valueFunction);
    }

    @NotNull
    public IntList readIntIdList() {
        if (readIsPassthrough) return passthroughIntIdList();
        return getRead().readIntIdList();
    }

    public void writeIntIdList(@NotNull IntList intList) {
        getWrite().writeIntIdList(intList);
    }

    @NotNull
    public <K, V, M extends Map<K, V>> M readMap(@NotNull IntFunction<M> toMapFunction, @NotNull Reader<K> keyFunction, @NotNull Reader<V> valueFunction) {
        if (readIsPassthrough) return passthroughMap(toMapFunction, keyFunction, valueFunction);
        return getRead().readMap(toMapFunction, keyFunction, valueFunction);
    }

    @NotNull
    public <K, V> Map<K, V> readMap(@NotNull Reader<K> keyFunction, @NotNull Reader<V> valueFunction) {
        return this.readMap(Maps::newHashMapWithExpectedSize, keyFunction, valueFunction);
    }

    public <K, V> void writeMap(@NotNull Map<K, V> map, @NotNull Writer<K> kWriter, @NotNull Writer<V> vWriter) {
        getWrite().writeMap(map, kWriter, vWriter);
    }

    public void readWithCount(@NotNull Consumer<FriendlyByteBuf> consumer) {
        if (readIsPassthrough) {
            passthroughWithCount(consumer);
            return;
        }
        getRead().readWithCount(consumer);
    }

    public <T> void writeOptional(@NotNull Optional<T> optional, @NotNull Writer<T> writer) {
        getWrite().writeOptional(optional, writer);
    }

    @NotNull
    public <T> Optional<T> readOptional(@NotNull Function<FriendlyByteBuf, T> function) {
        return this.readBoolean() ? Optional.of(function.apply(this)) : Optional.empty();
    }

    public byte@NotNull[] readByteArray() {
        if (readIsPassthrough) return passthroughByteArray();
        return getRead().readByteArray();
    }

    public byte@NotNull[] readByteArray(int i) {
        if (readIsPassthrough) return passthroughByteArray(i);
        return getRead().readByteArray(i);
    }

    public int@NotNull[] readVarIntArray() {
        if (readIsPassthrough) return passthroughVarIntArray();
        return getRead().readVarIntArray();
    }

    public int@NotNull[] readVarIntArray(int i) {
        if (readIsPassthrough) return passthroughVarIntArray(i);
        return getRead().readVarIntArray(i);
    }

    public long@NotNull[] readLongArray() {
        if (readIsPassthrough) return passthroughLongArray();
        return getRead().readLongArray();
    }

    public long@NotNull[] readLongArray(long[] longs) {
        if (readIsPassthrough) return passthroughLongArray(longs);
        return getRead().readLongArray(longs);
    }

    public long@NotNull[] readLongArray(long[] longs, int i) {
        if (readIsPassthrough) return passthroughLongArray(longs, i);
        return getRead().readLongArray(longs, i);
    }

    public byte@NotNull[] accessByteBufWithCorrectSize() {
        return getRead().accessByteBufWithCorrectSize();
    }

    @NotNull
    public BlockPos readBlockPos() {
        if (readIsPassthrough) return passthroughBlockPos();
        return getRead().readBlockPos();
    }

    @NotNull
    public ChunkPos readChunkPos() {
        if (readIsPassthrough) return passthroughChunkPos();
        return getRead().readChunkPos();
    }

    @NotNull
    public SectionPos readSectionPos() {
        if (readIsPassthrough) return passthroughSectionPos();
        return getRead().readSectionPos();
    }

    @NotNull
    public Component readComponent() {
        if (readIsPassthrough) return passthroughComponent();
        return getRead().readComponent();
    }

    @NotNull
    public <T extends Enum<T>> T readEnum(@NotNull Class<T> clazz) {
        return clazz.getEnumConstants()[this.readVarInt()];
    }

    public int readVarInt() {
        if (readIsPassthrough) return passthroughVarInt();
        return getRead().readVarInt();
    }

    public long readVarLong() {
        if (readIsPassthrough) return passthroughVarLong();
        return getRead().readVarLong();
    }

    @NotNull
    public UUID readUUID() {
        if (readIsPassthrough) return passthroughUUID();
        return getRead().readUUID();
    }

    @Nullable
    public CompoundTag readNbt() {
        if (readIsPassthrough) return passthroughNbt();
        return getRead().readNbt();
    }

    @Nullable
    public CompoundTag readAnySizeNbt() {
        if (readIsPassthrough) return passthroughAnySizeNbt();
        return getRead().readAnySizeNbt();
    }

    @Nullable
    public CompoundTag readNbt(@NotNull NbtAccounter nbtAccounter) {
        if (readIsPassthrough) return passthroughNbt(nbtAccounter);
        return getRead().readNbt(nbtAccounter);
    }

    @NotNull
    public ItemStack readItem() {
        if (readIsPassthrough) return passthroughItem();
        return getRead().readItem();
    }

    @NotNull
    public String readUtf() {
        if (readIsPassthrough) return passthroughUtf();
        return getRead().readUtf();
    }

    @NotNull
    public String readUtf(int i) {
        if (readIsPassthrough) return passthroughUtf(i);
        return getRead().readUtf(i);
    }

    @NotNull
    public ResourceLocation readResourceLocation() {
        if (readIsPassthrough) return passthroughResourceLocation();
        return getRead().readResourceLocation();
    }

    @NotNull
    public Date readDate() {
        if (readIsPassthrough) return passthroughDate();
        return getRead().readDate();
    }

    @NotNull
    public BlockHitResult readBlockHitResult() {
        if (readIsPassthrough) return passthroughBlockHitResult();
        return getRead().readBlockHitResult();
    }

    public void writeBlockHitResult(@NotNull BlockHitResult blockHitResult) {
        getWrite().writeBlockHitResult(blockHitResult);
    }

    @NotNull
    public PacketWrapper writeBlockHitResultAndReturnSelf(@NotNull BlockHitResult blockHitResult) {
        getWrite().writeBlockHitResult(blockHitResult);
        return this;
    }

    @NotNull
    public BitSet readBitSet() {
        if (readIsPassthrough) return passthroughBitSet();
        return getWrite().readBitSet();
    }

    public void writeBitSet(@NotNull BitSet bitSet) {
        getWrite().writeBitSet(bitSet);
    }

    @NotNull
    public PacketWrapper writeBitSetAndReturnSelf(@NotNull BitSet bitSet) {
        getWrite().writeBitSet(bitSet);
        return this;
    }

    public int readerCapacity() {
        return getRead().capacity();
    }

    @NotNull
    public PacketWrapper readerCapacity(int i) {
        getRead().capacity(i);
        return this;
    }

    public int readerMaxCapacity() {
        return getRead().maxCapacity();
    }

    public int writerCapacity() {
        return getWrite().capacity();
    }

    @NotNull
    public PacketWrapper writerCapacity(int i) {
        getWrite().capacity(i);
        return this;
    }

    public int writerMaxCapacity() {
        return getWrite().maxCapacity();
    }

    @NotNull
    public ByteOrder readerOrder() {
        return getRead().order();
    }

    @NotNull
    public ByteOrder writerOrder() {
        return getWrite().order();
    }

    @NotNull
    public ByteBuf readerOrder(@NotNull ByteOrder byteOrder) {
        return getRead().order(byteOrder);
    }

    @NotNull
    public ByteBuf writerOrder(@NotNull ByteOrder byteOrder) {
        return getWrite().order(byteOrder);
    }

    @Nullable
    public ByteBuf readerUnwrap() {
        return getRead().unwrap();
    }

    @Nullable
    public ByteBuf writerUnwrap() {
        return getWrite().unwrap();
    }

    public boolean readerIsDirect() {
        return getRead().isDirect();
    }

    public boolean writerIsDirect() {
        return getWrite().isDirect();
    }

    public boolean readerIsReadOnly() {
        return getRead().isReadOnly();
    }

    public boolean writerIsReadOnly() {
        return getWrite().isReadOnly();
    }

    @NotNull
    public ByteBuf readerAsReadOnly() {
        return getRead().asReadOnly();
    }

    @NotNull
    public ByteBuf writerAsReadOnly() {
        return getWrite().asReadOnly();
    }

    public int readerIndex() {
        return getRead().readerIndex();
    }

    @NotNull
    public PacketWrapper readerIndex(int i) {
        getRead().readerIndex(i);
        return this;
    }

    public int writerIndex() {
        return getWrite().writerIndex();
    }

    @NotNull
    public PacketWrapper readerSetIndex(int i, int i2) {
        getRead().setIndex(i, i2);
        return this;
    }

    @NotNull
    public PacketWrapper writerSetIndex(int i, int i2) {
        getWrite().setIndex(i, i2);
        return this;
    }

    public int readableBytes() {
        return getRead().readableBytes();
    }

    public int writableBytes() {
        return getWrite().writableBytes();
    }

    public int maxWritableBytes() {
        return getWrite().maxWritableBytes();
    }

    public boolean isReadable() {
        return getRead().isReadable();
    }

    public boolean isReadable(int i) {
        return getRead().isReadable(i);
    }

    public boolean isWritable() {
        return getWrite().isWritable();
    }

    public boolean isWritable(int i) {
        return getWrite().isWritable(i);
    }

    @NotNull
    public PacketWrapper readerClear() {
        getRead().clear();
        return this;
    }

    @NotNull
    public PacketWrapper writerClear() {
        getWrite().clear();
        return this;
    }

    @NotNull
    public PacketWrapper markReaderIndex() {
        getRead().markReaderIndex();
        return this;
    }

    @NotNull
    public PacketWrapper resetReaderIndex() {
        getRead().resetReaderIndex();
        return this;
    }

    @NotNull
    public PacketWrapper markWriterIndex() {
        getWrite().markWriterIndex();
        return this;
    }

    @NotNull
    public PacketWrapper resetWriterIndex() {
        getWrite().resetWriterIndex();
        return this;
    }

    @NotNull
    public String passthroughUtf() {
        String s = getRead().readUtf();
        getWrite().writeUtf(s);
        return s;
    }

    @NotNull
    public String passthroughUtf(int maxLength) {
        String s = getRead().readUtf(maxLength);
        getWrite().writeUtf(s);
        return s;
    }

    public byte passthroughByte() {
        byte b = getRead().readByte();
        getWrite().writeByte(b);
        return b;
    }

    public short passthroughShort() {
        short s = getRead().readShort();
        getWrite().writeShort(s);
        return s;
    }

    public short passthroughShortLE() {
        short s = getRead().readShortLE();
        getWrite().writeShortLE(s);
        return s;
    }

    public int passthroughUnsignedShort() {
        int i = getRead().readUnsignedShort();
        getWrite().writeShort(i);
        return i;
    }

    public int passthroughUnsignedShortLE() {
        int i = getRead().readUnsignedShortLE();
        getWrite().writeShortLE(i);
        return i;
    }

    public int passthroughMedium() {
        int i = getRead().readMedium();
        getWrite().writeMedium(i);
        return i;
    }

    public int passthroughMediumLE() {
        int i = getRead().readMediumLE();
        getWrite().writeMediumLE(i);
        return i;
    }

    public int passthroughUnsignedMedium() {
        int i = getRead().readUnsignedMedium();
        getWrite().writeMedium(i);
        return i;
    }

    public int passthroughUnsignedMediumLE() {
        int i = getRead().readUnsignedMediumLE();
        getWrite().writeMediumLE(i);
        return i;
    }

    public short passthroughUnsignedByte() {
        short s = getRead().readUnsignedByte();
        getWrite().writeByte(s);
        return s;
    }

    public int passthroughVarInt() {
        int i = getRead().readVarInt();
        getWrite().writeVarInt(i);
        return i;
    }

    public long passthroughVarLong() {
        long l = getRead().readVarLong();
        getWrite().writeVarLong(l);
        return l;
    }

    public int passthroughInt() {
        int i = getRead().readInt();
        getWrite().writeInt(i);
        return i;
    }

    public int passthroughIntLE() {
        int i = getRead().readIntLE();
        getWrite().writeIntLE(i);
        return i;
    }

    public long passthroughUnsignedInt() {
        long l = getRead().readUnsignedInt();
        getWrite().writeInt((int) l);
        return l;
    }

    public long passthroughUnsignedIntLE() {
        long l = getRead().readUnsignedIntLE();
        getWrite().writeIntLE((int) l);
        return l;
    }

    public boolean passthroughBoolean() {
        boolean b = getRead().readBoolean();
        getWrite().writeBoolean(b);
        return b;
    }

    public long passthroughLong() {
        long l = getRead().readLong();
        getWrite().writeLong(l);
        return l;
    }

    public long passthroughLongLE() {
        long l = getRead().readLongLE();
        getWrite().writeLongLE(l);
        return l;
    }

    public char passthroughChar() {
        char c = getRead().readChar();
        getWrite().writeChar(c);
        return c;
    }

    public float passthroughFloat() {
        float f = getRead().readFloat();
        getWrite().writeFloat(f);
        return f;
    }

    public float passthroughFloatLE() {
        float f = getRead().readFloatLE();
        getWrite().writeFloatLE(f);
        return f;
    }

    public double passthroughDouble() {
        double d = getRead().readDouble();
        getWrite().writeDouble(d);
        return d;
    }

    public double passthroughDoubleLE() {
        double d = getRead().readDoubleLE();
        getWrite().writeDoubleLE(d);
        return d;
    }

    @NotNull
    public ByteBuf passthroughBytes(int i) {
        ByteBuf buf = getRead().readBytes(i);
        getWrite().writeBytes(buf);
        return buf;
    }

    @NotNull
    public ByteBuf passthroughSlice(int i) {
        ByteBuf buf = getRead().readSlice(i);
        getWrite().writeBytes(buf);
        return buf;
    }

    @NotNull
    public ByteBuf passthroughRetainedSlice(int i) {
        ByteBuf buf = getRead().readRetainedSlice(i);
        getWrite().writeBytes(buf);
        return buf;
    }

    @NotNull
    public ByteBuf passthroughBytes(@NotNull ByteBuf buf) {
        getRead().readBytes(buf);
        getWrite().writeBytes(buf);
        return buf;
    }

    @NotNull
    public ByteBuf passthroughBytes(@NotNull ByteBuf buf, int length) {
        getRead().readBytes(buf, length);
        getWrite().writeBytes(buf, length);
        return buf;
    }

    @NotNull
    public ByteBuf passthroughBytes(@NotNull ByteBuf buf, int dstIndex, int length) {
        getRead().readBytes(buf, dstIndex, length);
        getWrite().writeBytes(buf, dstIndex, length);
        return buf;
    }

    public byte@NotNull[] passthroughBytes(byte@NotNull[] bytes) {
        getRead().readBytes(bytes);
        getWrite().writeBytes(bytes);
        return bytes;
    }

    public byte@NotNull[] passthroughBytes(byte@NotNull[] bytes, int dstIndex, int length) {
        if (bytes.length == length) {
            return passthroughBytes(bytes);
        }
        getRead().readBytes(bytes, dstIndex, length);
        getWrite().writeBytes(bytes, dstIndex, length);
        return bytes;
    }

    @NotNull
    public CharSequence passthroughCharSequence(int i, @NotNull Charset charset) {
        CharSequence cs = getRead().readCharSequence(i, charset);
        getWrite().writeCharSequence(cs, charset);
        return cs;
    }

    @NotNull
    public <T> T passthroughWithCodec(@NotNull Codec<T> codec) {
        T object = getRead().readWithCodec(codec);
        getWrite().writeWithCodec(codec, object);
        return object;
    }

    @NotNull
    public IntList passthroughIntIdList() {
        IntList intList = getRead().readIntIdList();
        getWrite().writeIntIdList(intList);
        return intList;
    }

    @NotNull
    public <T, C extends Collection<T>> C passthroughCollection(@NotNull IntFunction<C> toCollectionFunction, @NotNull Function<FriendlyByteBuf, T> valueFunction) {
        int length = this.passthroughVarInt();
        C collection = toCollectionFunction.apply(length);
        for (int i = 0; i < length; ++i) {
            collection.add(valueFunction.apply(this));
        }
        return collection;
    }

    @NotNull
    public <K, V, M extends Map<K, V>> M passthroughMap(@NotNull IntFunction<M> toMapFunction, @NotNull Function<FriendlyByteBuf, K> keyFunction, @NotNull Function<FriendlyByteBuf, V> valueFunction) {
        int length = passthroughVarInt();
        M map = toMapFunction.apply(length);
        for (int i = 0; i < length; ++i) {
            K key = keyFunction.apply(this);
            V value = valueFunction.apply(this);
            map.put(key, value);
        }
        return map;
    }

    public void passthroughWithCount(@NotNull Consumer<FriendlyByteBuf> consumer) {
        int count = passthroughVarInt();
        for (int i = 0; i < count; i++) {
            consumer.accept(this);
        }
    }

    public byte@NotNull[] passthroughByteArray() {
        byte[] bytes = getRead().readByteArray();
        getWrite().writeByteArray(bytes);
        return bytes;
    }

    public byte@NotNull[] passthroughByteArray(int i) {
        byte[] bytes = getRead().readByteArray();
        getWrite().writeByteArray(bytes);
        return bytes;
    }

    public int@NotNull[] passthroughVarIntArray() {
        int[] ints = getRead().readVarIntArray();
        getWrite().writeVarIntArray(ints);
        return ints;
    }

    public int@NotNull[] passthroughVarIntArray(int i) {
        int[] ints = getRead().readVarIntArray(i);
        getWrite().writeVarIntArray(ints);
        return ints;
    }

    public long@NotNull[] passthroughLongArray() {
        long[] longs = getRead().readLongArray();
        getWrite().writeLongArray(longs);
        return longs;
    }

    public long@NotNull[] passthroughLongArray(long[] arr) {
        long[] longs = getRead().readLongArray(arr);
        getWrite().writeLongArray(longs);
        return longs;
    }

    public long@NotNull[] passthroughLongArray(long[] arr, int i) {
        long[] longs = getRead().readLongArray(arr, i);
        getWrite().writeLongArray(longs);
        return longs;
    }

    @NotNull
    public BlockPos passthroughBlockPos() {
        BlockPos blockPos = getRead().readBlockPos();
        getWrite().writeBlockPos(blockPos);
        return blockPos;
    }

    @NotNull
    public ChunkPos passthroughChunkPos() {
        ChunkPos chunkPos = getRead().readChunkPos();
        getWrite().writeChunkPos(chunkPos);
        return chunkPos;
    }

    @NotNull
    public SectionPos passthroughSectionPos() {
        SectionPos sectionPos = getRead().readSectionPos();
        getWrite().writeSectionPos(sectionPos);
        return sectionPos;
    }

    @NotNull
    public Component passthroughComponent() {
        Component component = getRead().readComponent();
        getWrite().writeComponent(component);
        return component;
    }

    @NotNull
    public UUID passthroughUUID() {
        UUID uuid = getRead().readUUID();
        getWrite().writeUUID(uuid);
        return uuid;
    }

    @Nullable
    public CompoundTag passthroughNbt() {
        return passthroughNbt(new NbtAccounter(2097152L));
    }

    @Nullable
    public CompoundTag passthroughAnySizeNbt() {
        CompoundTag tag = getRead().readAnySizeNbt();
        getWrite().writeNbt(tag);
        return tag;
    }

    @Nullable
    public CompoundTag passthroughNbt(@NotNull NbtAccounter nbtAccounter) {
        int readerIndex = this.readerIndex();
        int writerIndex = this.writerIndex();
        byte type = passthroughByte();
        if (type == 0) {
            return null;
        } else {
            this.readerIndex(readerIndex);
            this.writerIndex(writerIndex);
            try {
                CompoundTag tag = NbtIo.read(new ByteBufInputStream(this), nbtAccounter);
                if (!readIsPassthrough) getWrite().writeNbt(tag);
                return tag;
            } catch (IOException var5) {
                throw new EncoderException(var5);
            }
        }
    }

    @NotNull
    public ItemStack passthroughItem() {
        if (passthroughBoolean()) {
            int id = passthroughVarInt();
            int count = passthroughByte();
            ItemStack itemStack = new ItemStack(Item.byId(id), count);
            itemStack.setTag(passthroughNbt());
            return itemStack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    // unused
    @NotNull
    public PacketWrapper passthroughEntityMetadataValue(int type) {
        if (type == 0) passthrough(Type.BYTE); // Byte
        if (type == 1) passthrough(Type.VAR_INT); // VarInt
        if (type == 2) passthrough(Type.FLOAT); // Float
        if (type == 3) passthrough(Type.STRING); // String
        if (type == 4) passthrough(Type.COMPONENT); // Chat
        if (type == 5 && passthroughBoolean()) passthrough(Type.COMPONENT); // Optional Chat
        if (type == 6) passthrough(Type.ITEM); // Slot (Item)
        if (type == 7) passthrough(Type.BOOLEAN); // Boolean
        if (type == 8) passthrough(Type.FLOAT).passthrough(Type.FLOAT).passthrough(Type.FLOAT); // Rotation (3 floats)
        if (type == 9) passthrough(Type.BLOCK_POS); // Position
        if (type == 10 && passthroughBoolean()) passthrough(Type.BLOCK_POS); // Position
        if (type == 11) passthrough(Type.VAR_INT); // Direction (VarInt)
        if (type == 12 && passthroughBoolean()) passthrough(Type.UUID); // Optional UUID
        if (type == 13) passthrough(Type.VAR_INT); // Optional Block ID (VarInt) (0 for absent)
        if (type == 14) passthrough(Type.NBT); // NBT
        if (type == 15) passthroughParticle(readVarInt()); // Particle
        if (type == 16) passthrough(Type.VAR_INT).passthrough(Type.VAR_INT).passthrough(Type.VAR_INT); // Villager Data (type, profession, level)
        if (type == 17) passthrough(Type.VAR_INT); // Optional VarInt (0 for absent); used for entity IDs
        if (type == 18) passthrough(Type.VAR_INT); // Pose (VarInt)
        return this;
    }

    // unused
    @NotNull
    public PacketWrapper passthroughParticle(int id) {
        if (id == 4) { // minecraft:block
            passthrough(Type.VAR_INT); // BlockState
        }
        if (id == 15) { // minecraft:dust
            passthrough(Type.FLOAT); // Red, 0-1
            passthrough(Type.FLOAT); // Green, 0-1
            passthrough(Type.FLOAT); // Blue, 0-1
            passthrough(Type.FLOAT); // Scale, will be clamped between 0.01 and 4
        }
        if (id == 16) {
            passthrough(Type.FLOAT); // FromRed, 0-1
            passthrough(Type.FLOAT); // FromGreen, 0-1
            passthrough(Type.FLOAT); // FromBlue, 0-1
            passthrough(Type.FLOAT); // Scale, will be clamped between 0.01 and 4
            passthrough(Type.FLOAT); // ToRed, 0-1
            passthrough(Type.FLOAT); // ToGreen, 0-1
            passthrough(Type.FLOAT); // ToBlue, 0-1
        }
        if (id == 25) { // minecraft:falling_dust
            passthrough(Type.VAR_INT); // BlockState
        }
        if (id == 36) { // minecraft:item
            passthrough(Type.ITEM); // Item
        }
        if (id == 37) { // minecraft:vibration
            passthrough(Type.DOUBLE); // Origin X
            passthrough(Type.DOUBLE); // Origin Y
            passthrough(Type.DOUBLE); // Origin Z
            passthrough(Type.DOUBLE); // Dest X
            passthrough(Type.DOUBLE); // Dest Y
            passthrough(Type.DOUBLE); // Dest Z
            passthrough(Type.INT); // Ticks
        }
        return this;
    }

    @NotNull
    public PacketWrapper passthroughCollection(@NotNull Type type) {
        int length = passthroughVarInt();
        for (int i = 0; i < length; i++) {
            passthrough(type);
        }
        return this;
    }

    @NotNull
    public PacketWrapper passthroughCollection(@NotNull Runnable runnable) {
        int length = passthroughVarInt();
        for (int i = 0; i < length; i++) {
           runnable.run();
        }
        return this;
    }

    @NotNull
    public PacketWrapper passthroughMap(@NotNull Runnable key, @NotNull Runnable value) {
        int size = passthroughVarInt();
        for (int i = 0; i < size; i++) {
            key.run();
            value.run();
        }
        return this;
    }

    @NotNull
    public PacketWrapper passthroughMap(@NotNull Type key, @NotNull Type value) {
        int size = passthroughVarInt();
        for (int i = 0; i < size; i++) {
            passthrough(key);
            passthrough(value);
        }
        return this;
    }

    @NotNull
    public PacketWrapper passthroughAdvancement() {
        if (passthroughBoolean()) { // Has parent
            passthrough(Type.RESOURCE_LOCATION); // Parent id
        }
        if (passthroughBoolean()) { // Has display
            passthroughAdvancementDisplay(); // Display data
        }
        passthroughMap(Type.STRING, Type.VOID); // Criteria
        int length = passthroughVarInt(); // Array length
        for (int i = 0; i < length; i++) {
            int length2 = passthroughVarInt(); // Array length 2
            for (int j = 0; j < length2; j++) {
                passthrough(Type.STRING); // Requirement
            }
        }
        return this;
    }

    @NotNull
    public PacketWrapper passthroughAdvancementDisplay() {
        passthrough(Type.COMPONENT); // Title
        passthrough(Type.COMPONENT); // Description
        passthrough(Type.ITEM); // Icon
        passthrough(Type.VAR_INT); // Frame Type (0 = task, 1 = challenge, 2 = goal)
        int flags = passthroughInt(); // Flags (0x01 = has background texture, 0x02 = show_toast, 0x04 = hidden)
        if ((flags & 1) != 0) passthrough(Type.RESOURCE_LOCATION); // Background texture
        passthrough(Type.FLOAT); // X coord
        passthrough(Type.FLOAT); // Y coord
        return this;
    }

    @NotNull
    public PacketWrapper passthroughAdvancementProgress() {
        passthroughMap(() -> passthrough(Type.STRING), this::passthroughCriterionProgress); // criteria progress
        return this;
    }

    @NotNull
    public PacketWrapper passthroughCriterionProgress() {
        if (passthroughBoolean()) { // achieved
            passthrough(Type.LONG); // date of achieving
        }
        return this;
    }

    @NotNull
    public Date passthroughDate() {
        Date date = getRead().readDate();
        getWrite().writeDate(date);
        return date;
    }

    @NotNull
    public ResourceLocation passthroughResourceLocation() {
        ResourceLocation location = getRead().readResourceLocation();
        getWrite().writeResourceLocation(location);
        return location;
    }

    @NotNull
    public BlockHitResult passthroughBlockHitResult() {
        BlockHitResult blockHitResult = getRead().readBlockHitResult();
        getWrite().writeBlockHitResult(blockHitResult);
        return blockHitResult;
    }

    @NotNull
    public BitSet passthroughBitSet() {
        BitSet bitSet = getRead().readBitSet();
        getWrite().writeBitSet(bitSet);
        return bitSet;
    }

    @NotNull
    public PacketWrapper passthroughAll() {
        getWrite().writeBytes(read, getRead().readerIndex(), getRead().readableBytes());
        //getWrite().writeBytes(getRead().readBytes(getRead().readableBytes()));
        getRead().readerIndex(getRead().writerIndex());
        return this;
    }

    @NotNull
    public PacketWrapper passthrough(@NotNull Type type) {
        return switch (type) {
            case VAR_INT_LIST -> writeVarIntArray(readVarIntArray());
            case BYTE_ARRAY -> writeByteArray(readByteArray());
            case ENUM, VAR_INT -> writeVarInt(readVarInt());
            case BOOLEAN -> writeBoolean(readBoolean());
            case INT -> writeInt(readInt());
            case FLOAT -> writeFloat(readFloat());
            case LONG -> writeLong(readLong());
            case DOUBLE -> writeDouble(readDouble());
            case BIT_SET -> writeBitSetAndReturnSelf(readBitSet());
            case BLOCK_POS -> writeBlockPos(readBlockPos());
            case BLOCK_HIT_RESULT -> writeBlockHitResultAndReturnSelf(readBlockHitResult());
            case CHAR -> writeChar(readChar());
            case STRING -> writeUtf(readUtf());
            case DATE -> writeDate(readDate());
            case COMPONENT -> writeComponent(readComponent());
            case ITEM -> writeItem(readItem());
            case NBT -> writeNbt(readNbt());
            case NBT_UNLIMITED -> writeNbt(readAnySizeNbt());
            case UUID -> writeUUID(readUUID());
            case VAR_LONG -> writeVarLong(readVarLong());
            case SHORT -> writeShort(readShort());
            case BYTE -> writeByte(readByte());
            case UNSIGNED_BYTE -> writeByte(readUnsignedByte());
            case RESOURCE_LOCATION -> writeResourceLocation(readResourceLocation());
            case VOID -> this; // do nothing
        };
    }

    public int peekVarInt() {
        int readerIndex = getRead().readerIndex();
        int i = getRead().readVarInt();
        getRead().readerIndex(readerIndex);
        return i;
    }

    @NotNull
    public IntPair index() {
        return IntPair.of(readerIndex(), writerIndex());
    }

    public void index(@NotNull IntPair pair) {
        readerIndex(pair.first());
        writerIndex(pair.second());
    }

    @Nullable
    @Override
    public <T> T readById(@NotNull IdMap<T> idMap) {
        return getRead().readById(idMap);
    }

    @Override
    public <T> void writeId(@NotNull IdMap<T> idMap, @NotNull T object) {
        getWrite().writeId(idMap, object);
    }

    public static int readVarInt(@NotNull ByteBuf buf) {
        int i = 0;
        int i2 = 0;

        byte b;
        do {
            b = buf.readByte();
            i |= (b & 127) << i2++ * 7;
            if (i2 > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while((b & 128) == 128);

        return i;
    }

    public enum Type {
        VAR_INT_LIST, // IntList
        BYTE_ARRAY, // byte[]
        BYTE, // byte
        VAR_INT, // int, Enum
        ENUM, // same as VAR_INT
        BOOLEAN, // boolean
        INT, // int
        FLOAT, // float
        LONG, // long
        DOUBLE, // double
        BIT_SET, // BitSet
        BLOCK_POS, // BlockPos
        BLOCK_HIT_RESULT, // BlockHitResult
        CHAR, // char
        STRING, // String
        DATE, // Date
        COMPONENT, // Component
        ITEM, // ItemStack
        NBT, // CompoundTag
        NBT_UNLIMITED, // CompoundTag but unlimited size
        UUID, // UUID
        VAR_LONG, // long
        SHORT, // short
        UNSIGNED_BYTE, // short -> byte
        RESOURCE_LOCATION, // ResourceLocation
        VOID, // void
    }
}
