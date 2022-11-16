package dacite.ls;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.google.gson.JsonObject;

import dacite.lsp.InlayHintDecorationParams;
import dacite.lsp.defUseData.DefUseClass;
import dacite.lsp.defUseData.DefUseData;
import dacite.lsp.defUseData.DefUseMethod;
import dacite.lsp.defUseData.DefUseVar;
import dacite.lsp.defUseData.transformation.DefUseVariable;
import dacite.lsp.defUseData.transformation.DefUseVariableRole;
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
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import dacite.lsp.DaciteExtendedTextDocumentService;
import dacite.lsp.InlayHintDecoration;

public class DaciteTextDocumentService
    implements TextDocumentService, DaciteExtendedTextDocumentService, DaciteTreeViewService {

  private static final Logger logger = LoggerFactory.getLogger(DaciteTextDocumentService.class);

  private HashMap<TextDocumentIdentifier, HashMap<Position, DefUseVariable>> highlightedDefUseVariables = new HashMap<>();

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
    //logger.info("codeLens {}", params);

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
    //logger.info("inlayHint {}", params);

    List<InlayHint> inlayHints = new ArrayList<>();
    highlightedDefUseVariables = new HashMap<>();

    var codeAnalyser = new CodeAnalyser(TextDocumentItemProvider.get(params.getTextDocument()).getText());
    var className = codeAnalyser.extractClassName();
    var packageName = codeAnalyser.extractPackageName();

    var defUseVariableMap = DefUseAnalysisProvider.getUniqueDefUseVariablesByLine(packageName, className);
    // First use grouping by line number...
    defUseVariableMap.forEach((lineNumber, defUseVariables) -> {
      // ...then group by variable name and try to match with positions obtained from parsing
      DefUseAnalysisProvider.groupByVariableNamesAndSort(defUseVariables)
          .forEach((variableName, groupedDefUseVariables) -> {
            // TODO: move the following fix into DefUseVariable class?
            if(variableName.contains(".")){
              variableName = variableName.substring(variableName.indexOf(".")+1);
            }
            var positions = codeAnalyser.extractVariablePositionsAtLine(lineNumber, variableName);
            int i = 0;

            while (i < groupedDefUseVariables.size() && i < positions.size()) {
              if(groupedDefUseVariables.get(i).isEditorHighlight()) {
                var defUseVariable = groupedDefUseVariables.get(i);
                var parserPosition = positions.get(i);

                var label = defUseVariable.getRole() == DefUseVariableRole.DEFINITION ? "Def" : "Use";

                var lspPos = new Position(parserPosition.line - 1, parserPosition.column - 1);
                var hint = new InlayHint(lspPos, Either.forLeft(label));
                hint.setPaddingLeft(true);
                hint.setPaddingRight(true);
                inlayHints.add(hint);

                if (highlightedDefUseVariables.containsKey(params.getTextDocument())) {
                  highlightedDefUseVariables.get(params.getTextDocument()).put(lspPos, defUseVariable);
                } else {
                  HashMap<Position, DefUseVariable> newMap = new HashMap<>();
                  newMap.put(lspPos, defUseVariable);
                  highlightedDefUseVariables.put(params.getTextDocument(), newMap);
                }
              }
              i++;
            }
          });
    });

    //logger.info("hints {}", inlayHints);

    return CompletableFuture.completedFuture(inlayHints);
  }

  @Override
  public CompletableFuture<InlayHintDecoration> inlayHintDecoration(InlayHintDecorationParams params) {
    //logger.info("inlayHintDecoration {}", params);

    var font = Font.SERIF;
    var color = new int[] { 255, 0, 0, 255 };

    if (highlightedDefUseVariables.containsKey(params.getIdentifier())) {
      var defUseVars = highlightedDefUseVariables.get(params.getIdentifier());
      if (defUseVars.containsKey(params.getPosition())) {
        var awtColor = defUseVars.get(params.getPosition()).getColor();
        color = new int[] { awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue(), awtColor.getAlpha() };
      }
    }

    return CompletableFuture.completedFuture(new InlayHintDecoration(color, Font.SERIF));
  }

  @Override
  public CompletableFuture<TreeViewChildrenResult> treeViewChildren(TreeViewChildrenParams params) {
    //logger.info("experimental/treeViewChildren: {}", params);

    var nodeUri = params.getNodeUri();
    nodeUri = nodeUri == null ? "" : nodeUri;

    var nodes = new ArrayList<TreeViewNode>();
    List<DefUseClass> classes = DefUseAnalysisProvider.getDefUseClasses();

    if (params.getViewId().equals("defUseChains")) {
      if (nodeUri.equals("")) {
        for (DefUseClass cl : classes) {
          TreeViewNode node = new TreeViewNode("defUseChains", cl.getName(),
              cl.getName() + " " + cl.getNumberChains() + " chains");
          node.setCollapseState("collapsed");
          node.setIcon("class");

          var commandArg = new JsonObject();
          commandArg.addProperty("packageClass", cl.getName());
          node.setCommand(new TreeViewCommand("Highlight", "dacite.highlight", List.of(commandArg)));

          nodes.add(node);
        }
      } else {
        for (DefUseClass cl : classes) {
          if (nodeUri.equals(cl.getName())) {
            for (DefUseMethod m : cl.getMethods()) {
              TreeViewNode node = new TreeViewNode("defUseChains", cl.getName() + "." + m.getName(),
                  m.getName() + " " + m.getNumberChains() + " chains");
              node.setCollapseState("collapsed");
              node.setIcon("method");

              var commandArg = new JsonObject();
              commandArg.addProperty("packageClass", cl.getName());
              commandArg.addProperty("method", m.getName());
              node.setCommand(new TreeViewCommand("Highlight", "dacite.highlight", List.of(commandArg)));

              nodes.add(node);
            }
            break;
          } else {
            for (DefUseMethod m : cl.getMethods()) {
              if (nodeUri.equals(cl.getName() + "." + m.getName())) {
                for (DefUseVar var : m.getVariables()) {
                  TreeViewNode node = new TreeViewNode("defUseChains",
                      cl.getName() + "." + m.getName() + " " + var.getName(),
                      var.getName() + " " + var.getNumberChains() + " chains");
                  node.setCollapseState("collapsed");
                  node.setIcon("variable");

                  var commandArg = new JsonObject();
                  commandArg.addProperty("packageClass", cl.getName());
                  commandArg.addProperty("method", m.getName());
                  commandArg.addProperty("variable", var.getName());
                  node.setCommand(new TreeViewCommand("Highlight", "dacite.highlight", List.of(commandArg)));

                  nodes.add(node);
                }
                break;
              } else {
                for (DefUseVar var : m.getVariables()) {
                  if (nodeUri.equals(cl.getName() + "." + m.getName() + " " + var.getName())) {
                    for (DefUseData data : var.getData()) {
                      TreeViewNode node = new TreeViewNode("defUseChains",
                          cl.getName() + "." + m.getName() + " " + var.getName() + " " + data.getDefLocation() + " - "
                              + data.getUseLocation() + " " + data.getIndex() + " " + data.getInstruction(),
                          data.getName() + " " + data.getDefLocation() + " - " + data.getUseLocation());

                      var commandArg = new JsonObject();
                      commandArg.addProperty("packageClass", cl.getName());
                      commandArg.addProperty("method", m.getName());
                      commandArg.addProperty("variable", var.getName());
                      commandArg.addProperty("defLocation", data.getDefLocation());
                      commandArg.addProperty("useLocation", data.getUseLocation());
                      commandArg.addProperty("index", data.getIndex());
                      commandArg.addProperty("instruction", data.getInstruction());
                      node.setCommand(new TreeViewCommand("Highlight", "dacite.highlight", List.of(commandArg)));

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
    }

    var result = new TreeViewChildrenResult(nodes.toArray(new TreeViewNode[0]));
    //logger.info("children: {}", result);

    return CompletableFuture.completedFuture(result);
  }

  @Override
  public CompletableFuture<TreeViewParentResult> treeViewParent(TreeViewParentParams params) {
    //logger.info("experimental/treeViewParent: {}", params);

    var nodeUri = params.getNodeUri();
    nodeUri = nodeUri == null ? "" : nodeUri;

    var nodes = new ArrayList<TreeViewNode>();
    List<DefUseClass> classes = DefUseAnalysisProvider.getDefUseClasses();
    if(!params.getViewId().equals("defUseChains")){
      return CompletableFuture.completedFuture(null);
    }
    if(params.getNodeUri().equals("")){
      return CompletableFuture.completedFuture(new TreeViewParentResult(null));
    } else {
      for (DefUseClass cl : classes) {
        if (nodeUri.equals(cl.getName())) {
          return CompletableFuture.completedFuture(new TreeViewParentResult(null));
        }
        for(DefUseMethod m: cl.getMethods()){
          if (nodeUri.equals(cl.getName() + "." + m.getName())) {
            return CompletableFuture.completedFuture(new TreeViewParentResult(cl.getName()));
          }
          for (DefUseVar var : m.getVariables()) {
            if (nodeUri.equals(cl.getName() + "." + m.getName() + " " + var.getName())) {
              return CompletableFuture.completedFuture(new TreeViewParentResult(cl.getName() + "." + m.getName()));
            }
            for (DefUseData data : var.getData()) {
              if (nodeUri.equals(cl.getName() + "." + m.getName() + " " + var.getName() + " " + data.getDefLocation() + " - "
                      + data.getUseLocation())) {
                return CompletableFuture.completedFuture(new TreeViewParentResult(cl.getName() + "." + m.getName() + " " + var.getName()));
              }
            }
          }
        }
      }
    }
    return CompletableFuture.completedFuture(null);
  }
}
