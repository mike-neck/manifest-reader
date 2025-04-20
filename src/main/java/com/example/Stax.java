package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

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
