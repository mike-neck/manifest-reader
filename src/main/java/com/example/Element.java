package com.example;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed interface Element {

  sealed interface Builder {
    @NotNull
    Element build();

    @NotNull
    StartElement element();

    default @Nullable String attr(@NotNull String name) {
      Attribute attribute = element().getAttributeByName(QName.valueOf(name));
      return attribute == null ? null : attribute.getValue();
    }

    default @NotNull String mustAttr(@NotNull String name) {
      String attr = attr(name);
      if (attr == null) {
        StartElement element = element();
        Location location = element.getLocation();
        int line = location.getLineNumber();
        int col = location.getColumnNumber();
        int offset = location.getCharacterOffset();
        String publicId = location.getPublicId();
        String systemId = location.getSystemId();
        throw new IllegalArgumentException(
            "attribute %s is required @line=%d,col=%d(offset=%d)[publicId=%s,systemId=%s]"
                .formatted(name, line, col, offset, publicId, systemId));
      }
      return attr;
    }

    default @Nullable Integer intAttr(@NotNull String name) {
      String attr = attr(name);
      if (attr == null) {
        return null;
      }
      return Integer.parseInt(attr);
    }
  }

  sealed interface NestedBuilder extends Builder {
    @Override
    @NotNull
    ChildOfProject build();

    @Nullable
    ProjectBuilder parent();
  }

  record ProjectBuilder(
      @NotNull @Override StartElement element,
      @NotNull List<ChildOfProject> children,
      @Nullable @Override Element.ProjectBuilder parent)
      implements NestedBuilder {

    ProjectBuilder(@NotNull StartElement element, @Nullable Element.ProjectBuilder parent) {
      this(element, new ArrayList<>(), parent);
    }

    @Override
    public @NotNull ChildOfProject build() {
      return new Project(
          mustAttr("name"),
          mustAttr("path"),
          attr("remote"),
          mustAttr("revision"),
          attr("dest-branch"),
          attr("groups"),
          attr("upstream"),
          intAttr("clone-depth"),
          List.copyOf(children));
    }
  }

  record RemoveProjectBuilder(@NotNull @Override StartElement element) implements Builder {
    @Override
    public @NotNull Element build() {
      return new RemoveProject(mustAttr("name"), attr("path"));
    }
  }

  record RemoteBuilder(@NotNull @Override StartElement element) implements Builder {
    @Override
    public @NotNull Element build() {
      return new Remote(mustAttr("name"), mustAttr("fetch"));
    }
  }

  record DefaultBuilder(@NotNull @Override StartElement element) implements Builder {
    @Override
    public @NotNull Element build() {
      return new Default(mustAttr("remote"), mustAttr("revision"));
    }
  }

  record IncludeBuilder(@NotNull @Override StartElement element) implements Builder {
    @Override
    public @NotNull Element build() {
      return new Include(mustAttr("name"));
    }
  }

  record SubManifestBuilder(@NotNull @Override StartElement element) implements Builder {
    @Override
    public @NotNull Element build() {
      return new SubManifest(mustAttr("path"));
    }
  }

  record LinkfileBuilder(
      @NotNull @Override StartElement element, @Nullable @Override Element.ProjectBuilder parent)
      implements NestedBuilder {
    @Override
    public @NotNull ChildOfProject build() {
      return new Linkfile(mustAttr("src"), mustAttr("dest"));
    }
  }

  record CopyfileBuilder(
      @NotNull @Override StartElement element, @Nullable @Override Element.ProjectBuilder parent)
      implements NestedBuilder {
    @Override
    public @NotNull ChildOfProject build() {
      return new Copyfile(mustAttr("src"), mustAttr("dest"));
    }
  }

  static @NotNull Builder builder(@NotNull String name, @NotNull StartElement element) {
    return builder(name, element, null);
  }

  static @NotNull Builder builder(
      @NotNull String name,
      @NotNull StartElement element,
      @Nullable Element.ProjectBuilder parent) {
    return switch (name) {
      case "project" -> new ProjectBuilder(element, parent);
      case "remove-project" -> new RemoveProjectBuilder(element);
      case "remote" -> new RemoteBuilder(element);
      case "default" -> new DefaultBuilder(element);
      case "include" -> new IncludeBuilder(element);
      case "sub-manifest" -> new SubManifestBuilder(element);
      case "linkfile" -> new LinkfileBuilder(element, parent);
      case "copyfile" -> new CopyfileBuilder(element, parent);
      default -> throw new IllegalArgumentException("unknown element name: " + name);
    };
  }

  sealed interface ChildOfProject extends Element {}

  record Project(
      @NotNull String name,
      @NotNull String path,
      @Nullable String remote,
      @NotNull String revision,
      @Nullable String destBranch,
      @Nullable String groups,
      @Nullable String upstream,
      @Nullable Integer cloneDepth,
      @NotNull List<Element> children)
      implements ChildOfProject {}

  record RemoveProject(@NotNull String name, @Nullable String path) implements Element {}

  record Remote(@NotNull String name, @NotNull String fetch) implements Element {}

  record Default(@NotNull String remote, @NotNull String revision) implements Element {}

  record Include(@NotNull String name) implements Element {}

  record SubManifest(@NotNull String path) implements Element {}

  record Linkfile(@NotNull String src, @NotNull String dest) implements ChildOfProject {}

  record Copyfile(@NotNull String src, @NotNull String dest) implements ChildOfProject {}
}
