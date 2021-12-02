package xyz.acrylicstyle.multiVersion;

import net.blueberrymc.common.bml.config.VisualConfigManager.*;
import xyz.acrylicstyle.multiVersion.transformer.TransformableProtocolVersions;

@Config
@Name("MultiVersion")
public class MultiVersionConfig {
    @Order(10000)
    @Reverse
    @Name("Multiplayer Version")
    @Key("version")
    public static TransformableProtocolVersions version = TransformableProtocolVersions.values()[0];
}
