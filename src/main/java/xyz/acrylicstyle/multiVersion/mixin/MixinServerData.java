package xyz.acrylicstyle.multiVersion.mixin;

import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.acrylicstyle.multiVersion.MultiVersionConfig;

@Mixin(value = ServerData.class, remap = false)
public class MixinServerData {
    @Shadow public int protocol;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(String s, String s2, boolean flag, CallbackInfo ci) {
        this.protocol = MultiVersionConfig.version.getProtocolVersion();
    }
}
