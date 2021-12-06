package xyz.acrylicstyle.multiVersion.mixin;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import xyz.acrylicstyle.multiVersion.MultiVersionConfig;

import java.util.List;

@Mixin(value = DebugScreenOverlay.class, remap = false)
public class MixinDebugScreenOverlay {
    @Inject(method = "getGameInformation", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILSOFT)
    public void getGameInformation(CallbackInfoReturnable<List<String>> cir) {
        addGameInformation(cir.getReturnValue());
    }

    private static void addGameInformation(@NotNull List<String> list) {
        if (!Minecraft.getInstance().isLocalServer()) {
            list.add("");
            var serverVersion = MultiVersionConfig.version;
            list.add("Server Version: " + serverVersion.getName());
            if (serverVersion.getProtocolVersion() >= 0x40000000) {
                String hex = Integer.toHexString(serverVersion.getProtocolVersion());
                list.add("Server Protocol Version: " + serverVersion.getProtocolVersion() + " (0x" + hex + ")");
            } else {
                list.add("Server Protocol Version: " + serverVersion.getProtocolVersion());
            }
            if (SharedConstants.getProtocolVersion() >= 0x40000000) {
                String hex = Integer.toHexString(SharedConstants.getProtocolVersion());
                list.add("Client Protocol Version: " + SharedConstants.getProtocolVersion() + " (0x" + hex + ")");
            } else {
                list.add("Client Protocol Version: " + SharedConstants.getProtocolVersion());
            }
        }
    }
}
