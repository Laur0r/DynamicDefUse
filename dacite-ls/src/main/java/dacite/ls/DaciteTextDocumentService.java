package dacite.ls;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DaciteTextDocumentService implements org.eclipse.lsp4j.services.TextDocumentService {

  private static final Logger logger = LoggerFactory.getLogger(DaciteTextDocumentService.class);

  private final Map<String, TextDocumentItem> openedDocuments = new HashMap<>();

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    logger.info("didOpen {}", params);
    TextDocumentItem textDocument = params.getTextDocument();
    openedDocuments.put(textDocument.getUri(), textDocument);
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    logger.info("didChange {}", params);
    List<TextDocumentContentChangeEvent> contentChanges = params.getContentChanges();
    TextDocumentItem textDocumentItem = openedDocuments.get(params.getTextDocument().getUri());
    if (!contentChanges.isEmpty()) {
      textDocumentItem.setText(contentChanges.get(0).getText());
    }
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    logger.info("didClose {}", params);
    String uri = params.getTextDocument().getUri();
    openedDocuments.remove(uri);
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {
  }

  @Override
  public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
    logger.info("codeAction {}", params);

    List<Either<Command, CodeAction>> codeActions = new ArrayList<>();

    String javaCode = openedDocuments.get(params.getTextDocument().getUri()).getText();
    CompilationUnit compilationUnit = StaticJavaParser.parse(javaCode);
    compilationUnit.findAll(MethodDeclaration.class).forEach(methodDeclaration -> {
      methodDeclaration.getName().getRange().ifPresent(range -> {
        codeActions.add(Either.forLeft(new Command("Run Analysis", "dacite.analyze")));
      });
    });

    return CompletableFuture.completedFuture(codeActions);
  }

  @Override
  public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
    logger.info("inlayHint {}", params);

    List<InlayHint> inlayHints = new ArrayList<>();

    String javaCode = openedDocuments.get(params.getTextDocument().getUri()).getText();
    CompilationUnit compilationUnit = StaticJavaParser.parse(javaCode);
    compilationUnit.findAll(MethodDeclaration.class).forEach(methodDeclaration -> {
      methodDeclaration.getName().getRange().ifPresent(range -> {
        var hint = new InlayHint(
            new Position(range.begin.line - 1, range.begin.column - 1),
            Either.forLeft("Usage")
        );
        hint.setPaddingLeft(true);
        hint.setPaddingRight(true);
        hint.setTooltip(new MarkupContent(
            MarkupKind.MARKDOWN,
            "# This is a test heading\n"
                + "This *is* **formatted** text."
        ));
        inlayHints.add(hint);
      });
    });

    logger.info("hints {}", inlayHints);

    return CompletableFuture.completedFuture(inlayHints);
  }

}
