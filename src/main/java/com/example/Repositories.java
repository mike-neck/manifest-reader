package com.example;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Repositories(
    @NotNull Map<String, Element.Remote> remotes, @Nullable Element.Remote defaultRemote) {

  static class Builder {
    final Map<String, Element.Remote> remotes;

    Builder() {
      this.remotes = new LinkedHashMap<>();
    }

    @NotNull
    Repositories buildWithoutDefault() {
      return new Repositories(remotes, null);
    }

    @NotNull
    Repositories buildWithDefault(@NotNull Element.Remote defaultRemote) {
      return new Repositories(remotes, defaultRemote);
    }

    void add(Element.Remote remote) {
      remotes.put(remote.name(), remote);
    }
  }
}
