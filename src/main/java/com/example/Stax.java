package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.jetbrains.annotations.NotNull;

public record Stax(@NotNull Path manifestPath) {

  public static void main(String[] args) throws IOException, XMLStreamException {
    if (args.length < 1) {
      System.err.println("Usage: java -jar <jar-file> <input-file>");
      System.exit(1);
    }
    String filename = args[0];
    Path manifestPath = Path.of(filename);
    Stax stax = new Stax(manifestPath);
    Manifest manifest = stax.readManifest();
    for (Element.Project element : manifest.projects()) {
      System.out.println(element);
    }
    for (Submanifest submanifest : manifest.submanifests()) {
      System.out.println(submanifest);
    }
  }

  @NotNull
  Manifest readManifest() throws IOException, XMLStreamException {
    Path manifestPath = manifestPath();
    List<Element> elements = readElements(manifestPath);
    Deque<Element> deque = new ArrayDeque<>(elements);
    List<Element.Project> projects = new ArrayList<>();
    List<Submanifest> subManifests = new ArrayList<>();
    while (!deque.isEmpty()) {
      Element element = deque.poll();
      if (element instanceof Element.Project project) {
        projects.add(project);
      } else if (element instanceof Element.Include(String name)) {
        Path inclusion = manifestPath.resolveSibling(name);
        List<Element> includeElements = readElements(inclusion);
        for (int i = includeElements.size() - 1; i >= 0; i--) {
          deque.addFirst(includeElements.get(i));
        }
      } else if (element instanceof Element.RemoveProject(String name, String path)) {
        projects.removeIf(
            project ->
                name.equals(project.name()) && (path == null || path.equals(project.path())));
      } else if (element instanceof Element.SubManifest subManifest) {
        String manifestName = subManifest.manifestName();
        Path path = manifestPath.resolveSibling(manifestName);
        subManifests.add(
            new Submanifest(
                subManifest.name(), null, path, subManifest.path(), subManifest.revision()));
      }
    }
    return new Manifest(List.copyOf(projects), List.copyOf(subManifests));
  }

  private static @NotNull List<Element> readElements(@NotNull Path manifestPath)
      throws IOException, XMLStreamException {
    var elements = new ArrayList<Element>();
    try (BufferedReader reader = Files.newBufferedReader(manifestPath)) {
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
    return elements;
  }

  private static XMLInputFactory xmlInputFactory() {
    System.setProperty("com.ctc.wstx.stax.WstxInputFactory", "com.ctc.wstx.stax.WstxInputFactory");
    return XMLInputFactory.newFactory(
        "com.ctc.wstx.stax.WstxInputFactory", ClassLoader.getSystemClassLoader());
  }
}
