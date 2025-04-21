package com.example;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public record Submanifest(
    @NotNull String name,
    @Nullable String remote,
    @NotNull Path manifestName,
    @NotNull String path,
    @Nullable String revision) {}
