package com.example;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public record Manifest(@NotNull List<Element.Project> projects) {}
