package dacite.ls;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import dacite.lsp.InlayHintDecorationParams;
import dacite.lsp.defUseData.DefUseClass;
import dacite.lsp.defUseData.DefUseData;
import dacite.lsp.defUseData.DefUseMethod;
import dacite.lsp.defUseData.DefUseVar;
import dacite.lsp.tvp.*;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
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
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import dacite.lsp.DaciteExtendedTextDocumentService;
import dacite.lsp.InlayHintDecoration;

public class DaciteTextDocumentService
    implements TextDocumentService, DaciteExtendedTextDocumentService, DaciteTreeViewService {

  private static final Logger logger = LoggerFactory.getLogger(DaciteTextDocumentService.class);

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    logger.info("didOpen {}", params);
    TextDocumentItemProvider.add(params.getTextDocument());
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    logger.info("didChange {}", params);
    List<TextDocumentContentChangeEvent> contentChanges = params.getContentChanges();
    if (!contentChanges.isEmpty()) {
      TextDocumentItemProvider.get(params.getTextDocument()).setText(contentChanges.get(0).getText());
    }
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    logger.info("didClose {}", params);
    TextDocumentItemProvider.remove(params.getTextDocument());
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {
  }

  @Override
  public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
    logger.info("codeLens {}", params);

    List<CodeLens> codeLenses = new ArrayList<>();

    try {
      String javaCode = TextDocumentItemProvider.get(params.getTextDocument()).getText();
      CompilationUnit compilationUnit = StaticJavaParser.parse(javaCode);

      compilationUnit.findAll(ClassOrInterfaceDeclaration.class).stream()
          .filter(coid -> coid.getMethods().stream().anyMatch(md -> md.getAnnotationByName("Test").isPresent()))
          .forEach(coid -> coid.getName().getRange().ifPresent(range -> {
            codeLenses.add(new CodeLens(new Range(new Position(range.begin.line - 1, range.begin.column - 1),
                new Position(range.end.line - 1, range.end.column)),
                new Command("Run Analysis", "dacite.analyze", List.of(params.getTextDocument().getUri())), null));
          }));
    } catch (ParseProblemException e) {
      logger.error("Document {} could not be parsed successfully: {}", params.getTextDocument().getUri(), e);
    }

    return CompletableFuture.completedFuture(codeLenses);
  }

  @Override
  public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
    logger.info("inlayHint {}", params);

    List<InlayHint> inlayHints = new ArrayList<>();

    String javaCode = TextDocumentItemProvider.get(params.getTextDocument()).getText();
    CompilationUnit compilationUnit = StaticJavaParser.parse(javaCode);
    compilationUnit.findAll(MethodDeclaration.class).forEach(methodDeclaration -> {
      methodDeclaration.getName().getRange().ifPresent(range -> {
        var hint = new InlayHint(new Position(range.begin.line - 1, range.begin.column - 1), Either.forLeft("Usage"));
        hint.setPaddingLeft(true);
        hint.setPaddingRight(true);
        hint.setTooltip(
            new MarkupContent(MarkupKind.MARKDOWN, "# This is a test heading\n" + "This *is* **formatted** text."));
        inlayHints.add(hint);
      });
    });

    logger.info("hints {}", inlayHints);

    return CompletableFuture.completedFuture(inlayHints);
  }

  @Override
  public CompletableFuture<InlayHintDecoration> inlayHintDecoration(InlayHintDecorationParams params) {
    logger.info("inlayHint {}", params);

    int[] color = new int[]{255,0,0,255};
    InlayHintDecoration inlayHints = new InlayHintDecoration( color, Font.SERIF);

    return CompletableFuture.completedFuture(inlayHints);
  }

  @Override
  public CompletableFuture<TreeViewChildrenResult> treeViewChildren(TreeViewChildrenParams params) {
    logger.info("experimental/treeViewChildren: {}", params);

    var nodeUri = params.getNodeUri();
    nodeUri = nodeUri == null ? "" : nodeUri;

    var nodes = new ArrayList<TreeViewNode>();
    ArrayList<DefUseClass> classes = AnalysisProvider.getDefUseClasses();

    if (params.getViewId().equals("defUseChains")) {
      if(nodeUri.equals("")){
        for(DefUseClass cl: classes){
          TreeViewNode node = new TreeViewNode("defUseChains", cl.getName(), cl.getName()+" "+cl.getNumberChains()+" chains");
          node.setCollapseState("collapsed");
          node.setIcon("class");
          nodes.add(node);
        }
      } else {
        for(DefUseClass cl: classes){
          if(nodeUri.equals(cl.getName())){
            for(DefUseMethod m: cl.getMethods()){
              TreeViewNode node = new TreeViewNode("defUseChains", m.getName(), m.getName()+" "+m.getNumberChains()+" chains");
              node.setCollapseState("collapsed");
              node.setIcon("method");
              nodes.add(node);
            }
            break;
          } else{
            for(DefUseMethod m: cl.getMethods()){
              if(nodeUri.equals(m.getName())){
                for(DefUseVar var: m.getVariables()){
                  TreeViewNode node = new TreeViewNode("defUseChains", var.getName(), var.getName()+" "+var.getNumberChains()+" chains");
                  node.setCollapseState("collapsed");
                  node.setIcon("variable");
                  nodes.add(node);
                }
                break;
              } else {
                for(DefUseVar var: m.getVariables()){
                  if(nodeUri.equals(var.getName())){
                    for(DefUseData data: var.getData()){
                      TreeViewNode node = new TreeViewNode("defUseChains", data.getName(), data.getName()+ " "+data.getDefLocation()+" - "+data.getUseLocation());
                      node.setCollapseState("collapsed");
                      nodes.add(node);
                    }
                    break;
                  }
                }
              }
            }
          }
        }
      }
      switch (nodeUri) {
        case "dacite/tryme/EuclidianGcd":
          var node1 = new TreeViewNode("defUseChains", "dacite/tryme/EuclidianGcd/egcd", "egcd 6 chains");
          node1.setCollapseState("expanded");
          node1.setIcon("method");
          nodes.add(node1);

          var node2 = new TreeViewNode("defUseChains", "dacite/tryme/EuclidianGcd/testGCD", "testGCD 5 chains");
          node2.setCollapseState("collapsed");
          node2.setIcon("method");
          nodes.add(node2);
          break;
        case "dacite/tryme/EuclidianGcd/egcd":
          var node3 = new TreeViewNode("defUseChains", "dacite/tryme/EuclidianGcd/egcd/a", "a 8 chains");
          node3.setCollapseState("expanded");
          node3.setIcon("variable");
          nodes.add(node3);

          var node4 = new TreeViewNode("defUseChains", "dacite/tryme/EuclidianGcd/egcd/b", "b 4 chains");
          node4.setCollapseState("collapsed");
          node4.setIcon("variable");
          nodes.add(node4);
          break;
        case "dacite/tryme/EuclidianGcd/egcd/a":
          var node5 = new TreeViewNode("defUseChains", "dacite/tryme/EuclidianGcd/egcd/a/1", "a L8 - L9");
          nodes.add(node5);

          var node6 = new TreeViewNode("defUseChains", "dacite/tryme/EuclidianGcd/egcd/a/2", "a/k L15 - testGcd L14");
          nodes.add(node6);
          break;
        case "":

      }
    }

    var result = new TreeViewChildrenResult(nodes.toArray(new TreeViewNode[0]));
    logger.info("children: {}", result);

    return CompletableFuture.completedFuture(result);
  }

  @Override
  public CompletableFuture<TreeViewParentResult> treeViewParent(TreeViewParentParams params) {
    logger.info("experimental/treeViewParent: {}", params);

    switch (params.getNodeUri()) {
      case "dacite/tryme/EuclidianGcd/egcd/a/1":
        return CompletableFuture.completedFuture(new TreeViewParentResult("dacite/tryme/EuclidianGcd/egcd/a"));
      case "dacite/tryme/EuclidianGcd/egcd/a":
        return CompletableFuture.completedFuture(new TreeViewParentResult("dacite/tryme/EuclidianGcd/egcd"));
      case "dacite/tryme/EuclidianGcd/egcd":
        return CompletableFuture.completedFuture(new TreeViewParentResult("dacite/tryme/EuclidianGcd"));
    }

    return CompletableFuture.completedFuture(new TreeViewParentResult(null));
  }

}
