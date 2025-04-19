package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public class Main {
  public static void main(String @NotNull [] args) throws IOException {
    if (args.length < 1) {
      System.err.println("Usage: java -jar <jar-file> <input-file>");
      System.exit(1);
    }
    String filename = args[0];
    Path filePath = Path.of(filename);
    XmlMapper mapper = new XmlMapper();
    JsonNode root = mapper.readValue(filePath.toFile(), JsonNode.class);
    Deque<Node> deque = new ArrayDeque<>(Node.root(root));
    var sb = new StringBuilder();
    while (!deque.isEmpty()) {
      Node node = deque.pop();
      JsonNodeType type = node.getNodeType();
      switch (type) {
        case OBJECT -> {
          sb.append(" ".repeat(node.depth() * 4))
              .append(node.name())
              .append(' ')
              .append("[object]")
              .append('\n');
          List<Node> nodes = node.objectChildren();
          for (int i = nodes.size() - 1; i >= 0; i--) {
            deque.push(nodes.get(i));
          }
        }
        case ARRAY -> {
          sb.append(" ".repeat(node.depth() * 4))
              .append(node.name())
              .append(' ')
              .append("[array]")
              .append('\n');
          List<Node> nodes = node.arrayChildren();
          for (int i = nodes.size() - 1; i >= 0; i--) {
            deque.push(nodes.get(i));
          }
        }
        default ->
            sb.append(" ".repeat(node.depth() * 4))
                .append(node.name())
                .append(' ')
                .append(type)
                .append(node.node().toPrettyString())
                .append('\n');
      }
    }
    System.out.println(sb);
    System.out.println("----");
    {
      Iterator<JsonNode> asAnArray = root.elements();
      while (asAnArray.hasNext()) {
        JsonNode next = asAnArray.next();
        System.out.print("- ");
        System.out.print(next.getNodeType());
        System.out.print(" : ");
        System.out.println(next.textValue());
      }
    }
    System.out.println("----");
    {
      Iterator<Map.Entry<String, JsonNode>> ite = root.fields();
      while (ite.hasNext()) {
        Map.Entry<String, JsonNode> next = ite.next();
        System.out.print("- ");
        System.out.print(next.getKey());
        System.out.print(" : ");
        System.out.println(next.getValue().getNodeType());
      }
    }
    System.out.println("----");
    ObjectNode object = (ObjectNode) root;
  }
}

record Node(int depth, @NotNull String name, @NotNull JsonNode node) {
  @NotNull
  @Unmodifiable
  List<Node> objectChildren() {
    if (!node.isObject()) {
      return List.of();
    }
    var list = new ArrayList<Node>();
    var ite = node.fieldNames();
    while (ite.hasNext()) {
      String fieldName = ite.next();
      JsonNode fieldValue = node.get(fieldName);
      list.add(new Node(depth + 1, fieldName, fieldValue));
    }
    return List.copyOf(list);
  }

  @NotNull
  @Unmodifiable
  List<Node> arrayChildren() {
    if (!node.isArray()) {
      return List.of();
    }
    Iterator<JsonNode> ite = node.elements();
    var list = new ArrayList<Node>();
    while (ite.hasNext()) {
      JsonNode child = ite.next();
      list.add(new Node(depth + 1, "", child));
    }
    return List.copyOf(list);
  }

  @Contract("_ -> new")
  static @NotNull @Unmodifiable List<Node> root(@NotNull JsonNode node) {
    return List.of(new Node(0, "", node));
  }

  public @NotNull JsonNodeType getNodeType() {
    return node.getNodeType();
  }
}
