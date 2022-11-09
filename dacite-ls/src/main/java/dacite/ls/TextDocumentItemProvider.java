package dacite.ls;

import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;

import java.util.HashMap;
import java.util.Map;

public class TextDocumentItemProvider {

  private static final Map<String, TextDocumentItem> openedDocuments = new HashMap<>();

  public static void add(TextDocumentItem textDocumentItem) {
    openedDocuments.put(textDocumentItem.getUri(), textDocumentItem);
  }

  public static TextDocumentItem get(TextDocumentIdentifier textDocumentIdentifier) {
    return openedDocuments.get(textDocumentIdentifier.getUri());
  }

  public static TextDocumentItem get(String uri) {
    return openedDocuments.get(uri);
  }

  public static void remove(TextDocumentIdentifier textDocumentIdentifier) {
    openedDocuments.remove(textDocumentIdentifier.getUri());
  }

}
