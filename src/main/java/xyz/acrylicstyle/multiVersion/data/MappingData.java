package xyz.acrylicstyle.multiVersion.data;

import org.jetbrains.annotations.NotNull;

public record MappingData(@NotNull RegistriesData registries, @NotNull BlocksData blocks) {
}
