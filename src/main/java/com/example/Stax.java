package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Stax {
  public static void main(String[] args) throws IOException, XMLStreamException {
    if (args.length < 1) {
      System.err.println("Usage: java -jar <jar-file> <input-file>");
      System.exit(1);
    }
    String filename = args[0];
    Path filePath = Path.of(filename);
    var elements = new ArrayList<Element>();
    try (BufferedReader reader = Files.newBufferedReader(filePath)) {
      XMLInputFactory factory = xmlInputFactory();
      XMLEventReader sax = factory.createXMLEventReader(reader);
      Element.Builder elementBuilder = null;
      while (sax.hasNext()) {
        XMLEvent event = sax.nextEvent();
        if (event.isStartElement()) {
          StartElement startElement = event.asStartElement();
          QName qname = startElement.getName();
          String name = qname.getLocalPart();
          if ("manifest".equals(name)) {
            continue;
          }
          if (elementBuilder instanceof Element.ProjectBuilder parent) {
            System.out.printf("[DEBUG] nested: %s[%s]%n", name,  parent.getClass().getSimpleName());
            elementBuilder = Element.builder(name, startElement, parent);
          } else {
            elementBuilder = Element.builder(name, startElement);
          }
        }
        if (event.isEndElement()) {
          EndElement end = event.asEndElement();
          QName qname = end.getName();
          String name = qname.getLocalPart();
          if ("manifest".equals(name)) {
            continue;
          }
          if (elementBuilder instanceof Element.NestedBuilder nb
              && nb.parent() instanceof Element.ProjectBuilder pb) {
            Element.ChildOfProject element = nb.build();
            pb.children().add(element);
            elementBuilder = pb;
          } else {
            assert elementBuilder != null;
            Element element = elementBuilder.build();
            elements.add(element);
            elementBuilder = null;
          }
        }
      }
    }
    for (Element element : elements) {
      System.out.println(element);
    }
  }

  private static XMLInputFactory xmlInputFactory() {
    System.setProperty("com.ctc.wstx.stax.WstxInputFactory", "com.ctc.wstx.stax.WstxInputFactory");
    return XMLInputFactory.newFactory(
        "com.ctc.wstx.stax.WstxInputFactory", ClassLoader.getSystemClassLoader());
  }
}

sealed interface Element {

  @SuppressWarnings("ClassEscapesDefinedScope")
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

  @SuppressWarnings("ClassEscapesDefinedScope")
  record RemoveProjectBuilder(@NotNull @Override StartElement element) implements Element.Builder {
    @Override
    public @NotNull Element build() {
      return new RemoveProject(mustAttr("name"), attr("path"));
    }
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  record RemoteBuilder(@NotNull @Override StartElement element) implements Element.Builder {
    @Override
    public @NotNull Element build() {
      return new Remote(mustAttr("name"), mustAttr("fetch"));
    }
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  record DefaultBuilder(@NotNull @Override StartElement element) implements Element.Builder {
    @Override
    public @NotNull Element build() {
      return new Default(mustAttr("name"), mustAttr("value"));
    }
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  record IncludeBuilder(@NotNull @Override StartElement element) implements Element.Builder {
    @Override
    public @NotNull Element build() {
      return new Include(mustAttr("name"));
    }
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  record SubManifestBuilder(@NotNull @Override StartElement element) implements Element.Builder {
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

  record Default(@NotNull String name, @NotNull String value) implements Element {}

  record Include(@NotNull String name) implements Element {}

  record SubManifest(@NotNull String path) implements Element {}

  record Linkfile(@NotNull String src, @NotNull String dest) implements ChildOfProject {}

  record Copyfile(@NotNull String src, @NotNull String dest) implements ChildOfProject {}
}
