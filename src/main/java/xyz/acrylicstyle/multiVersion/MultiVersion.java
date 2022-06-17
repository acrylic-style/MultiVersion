package xyz.acrylicstyle.multiVersion;

import io.netty.channel.Channel;
import net.blueberrymc.common.Blueberry;
import net.blueberrymc.common.bml.BlueberryMod;
import net.blueberrymc.common.bml.config.CompoundVisualConfig;
import net.blueberrymc.common.bml.config.VisualConfigManager;
import net.blueberrymc.common.bml.event.EventHandler;
import net.blueberrymc.common.bml.event.Listener;
import net.blueberrymc.common.event.network.ConnectionInitEvent;
import net.blueberrymc.common.event.network.ConnectionSetupCompressionEvent;
import net.minecraft.SharedConstants;
import xyz.acrylicstyle.multiVersion.transformer.InboundPacketTransformer;
import xyz.acrylicstyle.multiVersion.transformer.OutboundPacketTransformer;
import xyz.acrylicstyle.multiVersion.transformer.PacketRewriterManager;
import xyz.acrylicstyle.multiVersion.transformer.TransformableProtocolVersions;

import java.io.IOException;

public class MultiVersion extends BlueberryMod implements Listener {
    @Override
    public void onLoad() {
        if (SharedConstants.getProtocolVersion() > TransformableProtocolVersions.values()[0].getProtocolVersion()) {
            throw new RuntimeException("Incompatible version! Please use " + TransformableProtocolVersions.values()[0].getName() + "!");
        }
    }

    @Override
    public void onPreInit() {
        Blueberry.getEventManager().registerEvents(this, this);
        onReload();
        setVisualConfig(VisualConfigManager.createFromClass(MultiVersionConfig.class));
        this.getVisualConfig().onSave(this::saveConfig);
    }

    private void saveConfig(CompoundVisualConfig config) {
        VisualConfigManager.save(getConfig(), config);
        try {
            getConfig().saveConfig();
            this.getLogger().info("Saved configuration");
        } catch (IOException ex) {
            this.getLogger().error("Could not save configuration", ex);
        }
        onReload();
    }

    @Override
    public boolean onReload() {
        try {
            getConfig().reloadConfig();
        } catch (IOException e) {
            getLogger().warn("Failed to reload MultiVersion config", e);
        }
        VisualConfigManager.load(getConfig(), MultiVersionConfig.class);
        return false;
    }

    @EventHandler
    public static void onConnectionInit(ConnectionInitEvent e) {
        if (e.isLocalServer()) return;
        setupTransformer(e.getChannel());
        PacketRewriterManager.register(); // debug
    }

    @EventHandler
    public static void onConnectionSetupCompression(ConnectionSetupCompressionEvent e) {
        if (e.isLocalServer()) return;
        setupTransformer(e.getChannel());
    }

    private static void setupTransformer(Channel channel) {
        if (channel.pipeline().get("multiversion_inbound_transformer") instanceof InboundPacketTransformer) {
            channel.pipeline().remove("multiversion_inbound_transformer");
        }
        if (channel.pipeline().get("multiversion_outbound_transformer") instanceof OutboundPacketTransformer) {
            channel.pipeline().remove("multiversion_outbound_transformer");
        }
        channel.pipeline()
                // splitter -> multiversion_inbound_transformer -> decoder -> packet_handler
                .addBefore("decoder", "multiversion_inbound_transformer", new InboundPacketTransformer())
                // packet_handler -> encoder -> multiversion_outbound_transformer -> prepender
                .addBefore("encoder", "multiversion_outbound_transformer", new OutboundPacketTransformer());
    }
}
