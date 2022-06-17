package xyz.acrylicstyle.multiVersion.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum MappingVersion {
    v1_19("1.19"),
    v1_18_2("1.18.2"),
    ;

    public final String version;

    MappingVersion(@NotNull String version) {
        this.version = version;
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String toString() {
        return version;
    }
}
