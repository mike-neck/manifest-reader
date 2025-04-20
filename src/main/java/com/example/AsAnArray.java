package com.example;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;

public class AsAnArray {
    public static void main(String[] args) throws IOException, XMLStreamException {
        if (args.length < 1) {
            System.err.println("Usage: java -jar <jar-file> <input-file>");
            System.exit(1);
        }
        String filename = args[0];
        Path filePath = Path.of(filename);
        var elements = new ArrayList<JacksonElement>();
        XmlMapper mapper = new XmlMapper();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            JsonParser parser = mapper.getFactory().createParser(reader);
            FromXmlParser xmlParser = (FromXmlParser) parser;
            XMLStreamReader stax = xmlParser.getStaxReader();
            while (stax.hasNext()) {
                int event = stax.next();
                if (event == XMLStreamReader.START_ELEMENT) {
                    String name = stax.getLocalName();
          System.out.println("[debug] " + name);
                    JsonNode node = mapper.readTree(parser);
                    elements.add(new JacksonElement(name, node));
                    JsonParser p = mapper.getFactory().createParser(reader);
                    stax = ((FromXmlParser) p).getStaxReader();
                    for (int i = 0; i < elements.size(); i++) {
                        stax.nextTag();
                    }
                }
            }
        }
        for (JacksonElement element : elements) {
            System.out.println("---");
            System.out.println(element.name());
            System.out.println(element.contents());
        }
    }
}

record JacksonElement(@NotNull String name, @NotNull JsonNode contents) {}

class CachedReader extends Reader {


    final Reader reader;

    CachedReader(Reader reader) {
        this.reader = reader;
    }

    @Override
    public int read(@NotNull char[] cbuf, int off, int len) throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {

    }
}
