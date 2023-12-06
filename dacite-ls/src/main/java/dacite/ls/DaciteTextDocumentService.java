package dacite.ls;

import com.github.javaparser.ParseProblemException;
import com.google.gson.JsonObject;

import dacite.lsp.InlayHintDecorationParams;
import dacite.lsp.defUseData.*;
import dacite.lsp.defUseData.transformation.DefUseVariable;
import dacite.lsp.defUseData.transformation.DefUseVariableRole;
import dacite.lsp.tvp.*;

import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import dacite.lsp.DaciteExtendedTextDocumentService;
import dacite.lsp.InlayHintDecoration;

public class DaciteTextDocumentService
    implements TextDocumentService, DaciteExtendedTextDocumentService, DaciteTreeViewService {

  private static final Logger logger = LoggerFactory.getLogger(DaciteTextDocumentService.class);

  private HashMap<TextDocumentIdentifier, HashMap<Position, DefUse>> highlightedDefUseVariables = new HashMap<>();

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    //logger.info("didOpen {}", params);
    TextDocumentItemProvider.add(params.getTextDocument());
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    //logger.info("didChange {}", params);
    List<TextDocumentContentChangeEvent> contentChanges = params.getContentChanges();
    if (!contentChanges.isEmpty()) {
      TextDocumentItemProvider.get(params.getTextDocument()).setText(contentChanges.get(0).getText());
    }
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    //logger.info("didClose {}", params);
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
      CodeAnalyser codeAnalyser = new CodeAnalyser(javaCode);
      codeLenses = codeAnalyser.extractCodeLens(params.getTextDocument().getUri());

    } catch (ParseProblemException e) {
      logger.error("Document {} could not be parsed successfully: {}", params.getTextDocument().getUri(), e);
    }
    //logger.info("codeLens {}", codeLenses);
    return CompletableFuture.completedFuture(codeLenses);
  }

  @Override
  public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
    //logger.info("inlayHint {}", params);

    List<InlayHint> inlayHints = new ArrayList<>();
    highlightedDefUseVariables = new HashMap<>();

    String text = "";
    if(TextDocumentItemProvider.get(params.getTextDocument()) != null){
      text = TextDocumentItemProvider.get(params.getTextDocument()).getText();
    }
    var codeAnalyser = new CodeAnalyser(text);
    var className = codeAnalyser.extractClassName();
    var packageName = codeAnalyser.extractPackageName();

    Map<Integer,List<DefUse>> map = DefUseAnalysisProvider.getDefUseByLine(packageName, className, true);
    getInlayHints(map, inlayHints, params, codeAnalyser);
    Map<Integer,List<DefUse>> notCoveredMap = DefUseAnalysisProvider.getDefUseByLine(packageName, className, false);
    getInlayHints(notCoveredMap, inlayHints, params, codeAnalyser);


    logger.info("hints {}", inlayHints);

    return CompletableFuture.completedFuture(inlayHints);
  }

  @Override
  public CompletableFuture<InlayHintDecoration> inlayHintDecoration(InlayHintDecorationParams params) {
    //logger.info("inlayHintDecoration {}", params);

    var font = Font.SERIF;
    String color = Color.BLUE.toString();

    if (highlightedDefUseVariables.containsKey(params.getIdentifier())) {
      var defUseVars = highlightedDefUseVariables.get(params.getIdentifier());
      if (defUseVars.containsKey(params.getPosition())) {
        color = defUseVars.get(params.getPosition()).getColor();
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
      nodes = getTreeViewNodes(nodeUri, "defUseChains");
    } else if (params.getViewId().equals("notCoveredDUC")) {
      nodes = getTreeViewNodes(nodeUri, "notCoveredDUC");
      //logger.info(nodes.toString());
    }

    var result = new TreeViewChildrenResult(nodes.toArray(new TreeViewNode[0]));

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
            for(Def def: var.getDefs()){
              if(nodeUri.equals(cl.getName() + "." + m.getName() + " " + var.getName() + " " + def.getDefLocation())){
                return CompletableFuture.completedFuture(new TreeViewParentResult(cl.getName() + "." + m.getName() + " " + var.getName()));
              }
              for (Use data : def.getData()) {
                if (nodeUri.equals(cl.getName() + "." + m.getName() + " " + var.getName() + " " + def.getDefLocation() + " - "
                        + data.getUseLocation())) {
                  return CompletableFuture.completedFuture(new TreeViewParentResult(cl.getName() + "." + m.getName() + " " + var.getName()+" " + def.getDefLocation()));
                }
              }
            }
          }
        }
      }
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void treeViewDidChange(TreeViewDidChangeParams params) {

  }

  private ArrayList<TreeViewNode> getTreeViewNodes(String nodeUri, String id) {
    ArrayList<TreeViewNode> nodes = new ArrayList<TreeViewNode>();
    List<DefUseClass> classes;
    if (id.equals("defUseChains")) {
      classes = DefUseAnalysisProvider.getDefUseClasses();
    } else {
      classes = DefUseAnalysisProvider.getNotCoveredClasses();
    }
    if (nodeUri.equals("") && classes != null) {
      for (DefUseClass cl : classes) {
        TreeViewNode node = new TreeViewNode(id, cl.getName(),
                cl.getName() + " " + cl.getNumberChains() + " chains");
        node.setContextValue(DefUseAnalysisProvider.getTextDocumentUriTrigger());
        node.setCollapseState("collapsed");
        node.setIcon("class");

        var commandArg = new JsonObject();
        commandArg.addProperty("packageClass", cl.getName());
        if (id.equals("notCoveredDUC")) {
          commandArg.addProperty("notCovered", true);
        }
        node.setCommand(new TreeViewCommand("Highlight", "dacite.highlight", List.of(commandArg)));

        nodes.add(node);
      }
    } else if(classes != null) {
      for (DefUseClass cl : classes) {
        if (nodeUri.equals(cl.getName())) {
          for (DefUseMethod m : cl.getMethods()) {
            TreeViewNode node = new TreeViewNode(id, cl.getName() + "." + m.getName(),
                    m.getName() + " " + m.getNumberChains() + " chains");
            node.setCollapseState("collapsed");
            node.setIcon("method");

            var commandArg = new JsonObject();
            commandArg.addProperty("packageClass", cl.getName());
            commandArg.addProperty("method", m.getName());
            if (id.equals("notCoveredDUC")) {
              commandArg.addProperty("notCovered", true);
            }
            node.setCommand(new TreeViewCommand("Highlight", "dacite.highlight", List.of(commandArg)));

            nodes.add(node);
          }
          break;
        } else {
          for (DefUseMethod m : cl.getMethods()) {
            if (nodeUri.equals(cl.getName() + "." + m.getName())) {
              for (DefUseVar var : m.getVariables()) {
                TreeViewNode node = new TreeViewNode(id,
                        cl.getName() + "." + m.getName() + " " + var.getName(),
                        var.getName() + " " + var.getNumberChains() + " chains");
                node.setCollapseState("collapsed");
                node.setIcon("variable");

                var commandArg = new JsonObject();
                commandArg.addProperty("packageClass", cl.getName());
                commandArg.addProperty("method", m.getName());
                commandArg.addProperty("variable", var.getName());
                if (id.equals("notCoveredDUC")) {
                  commandArg.addProperty("notCovered", true);
                }
                node.setCommand(new TreeViewCommand("Highlight", "dacite.highlight", List.of(commandArg)));

                nodes.add(node);
              }
              break;
            } else {
              for (DefUseVar var : m.getVariables()) {
                if (nodeUri.equals(cl.getName() + "." + m.getName() + " " + var.getName())) {
                  for (Def def : var.getDefs()) {
                    TreeViewNode node = new TreeViewNode(id,
                            cl.getName() + "." + m.getName() + " " + var.getName() + " " + def.getDefLocation(),
                            "Def: " + def.getDefLocation() +" "+ def.getNumberChains() + " chains");

                    var commandArg = new JsonObject();
                    commandArg.addProperty("packageClass", cl.getName());
                    commandArg.addProperty("method", m.getName());
                    commandArg.addProperty("variable", var.getName());
                    commandArg.addProperty("defLocation", def.getDefLocation());
                    commandArg.addProperty("defInstruction", def.getDefInstruction());
                    if (id.equals("notCoveredDUC")) {
                      commandArg.addProperty("notCovered", true);
                    }
                    node.setCommand(new TreeViewCommand("Highlight", "dacite.highlight", List.of(commandArg)));
                    node.setIcon("definition");

                    nodes.add(node);
                  }
                  break;
                } else {
                  for(Def def: var.getDefs()){
                    if(nodeUri.equals(cl.getName() + "." + m.getName() + " " + var.getName() + " " + def.getDefLocation()))
                      for (Use data : def.getData()) {
                        TreeViewNode node = new TreeViewNode(id,
                                cl.getName() + "." + m.getName() + " " + var.getName() + " " + def.getDefLocation() + " - "
                                        + data.getUseLocation() + " " + data.getIndex() + " " + data.getUseInstruction(),
                                "Use: "  + data.getUseLocation() + " " + data.getName());

                        var commandArg = new JsonObject();
                        commandArg.addProperty("packageClass", cl.getName());
                        commandArg.addProperty("method", m.getName());
                        commandArg.addProperty("variable", var.getName());
                        commandArg.addProperty("defLocation", def.getDefLocation());
                        commandArg.addProperty("defInstruction", def.getDefInstruction());
                        commandArg.addProperty("useLocation", data.getUseLocation());
                        commandArg.addProperty("index", data.getIndex());
                        commandArg.addProperty("useInstruction", data.getUseInstruction());
                        if (id.equals("notCoveredDUC")) {
                          commandArg.addProperty("notCovered", true);
                        }
                        node.setCommand(new TreeViewCommand("Highlight", "dacite.highlight", List.of(commandArg)));

                        nodes.add(node);
                      }
                  }
                }
              }
            }
          }
        }
      }
    }
    return nodes;
  }

  private void getInlayHints(Map<Integer, List<DefUse>> map, List<InlayHint> inlayHints, InlayHintParams params, CodeAnalyser codeAnalyser){
    Set<com.github.javaparser.Position> globalDefPositions = new HashSet<>();
    map.forEach((lineNumber, defUseVariables) -> {
      //logger.info(DefUseAnalysisProvider.groupByVariableNamesAndSortDefUse(defUseVariables).toString());
      // ...then group by variable name and try to match with positions obtained from parsing
      DefUseAnalysisProvider.groupByVariableNamesAndSortDefUse(defUseVariables)
              .forEach((variableName, groupedDefUseVariables) -> {
                // TODO: move the following fix into DefUseVariable class?
                if(variableName.contains("[")){
                  variableName = variableName.substring(0, variableName.indexOf("["));
                }
                /*if(variableName.contains(".")){
                  variableName = variableName.substring(variableName.indexOf(".")+1);
                }*/
                List<Def> defs = new ArrayList<>();
                List<Use> uses = new ArrayList<>();
                for(int i = 0; i< groupedDefUseVariables.size(); i++){
                  DefUse var = groupedDefUseVariables.get(i);
                  if(var instanceof Def){
                  //if(var.getRole() == DefUseVariableRole.DEFINITION){
                    defs.add((Def)var);
                  } else {
                    uses.add((Use)var);
                  }
                }
                List<com.github.javaparser.Position> defPositions = new ArrayList<>();
                //List<Position> defPos = codeAnalyser.extractVariablePositionsAtLine(lineNumber, variableName);
                if(defs.size() != 0){
                  defPositions = codeAnalyser.extractVariablePositionsAtLine(lineNumber, variableName, true).get(0);
                  globalDefPositions.addAll(defPositions);
                  int i = 0;
                  while (i < defs.size() && i < defPositions.size()) {
                    if(defs.get(i).isEditorHighlight()) {
                      var defUseVariable = defs.get(i);
                      var parserPosition = defPositions.get(i);
                      var label = "Def";

                      var lspPos = new Position(parserPosition.line - 1, parserPosition.column - 1);
                      var hint = new InlayHint(lspPos, Either.forLeft(label));
                      hint.setPaddingLeft(true);
                      hint.setPaddingRight(true);
                      inlayHints.add(hint);

                      if (highlightedDefUseVariables.containsKey(params.getTextDocument())) {
                        highlightedDefUseVariables.get(params.getTextDocument()).put(lspPos, defUseVariable);
                      } else {
                        HashMap<Position, DefUse> newMap = new HashMap<>();
                        newMap.put(lspPos, defUseVariable);
                        highlightedDefUseVariables.put(params.getTextDocument(), newMap);
                      }
                    }
                    i++;
                  }
                }

                if(uses.size() != 0){
                  var pos = codeAnalyser.extractVariablePositionsAtLine(lineNumber, variableName, false);
                  var usePositionsDup = pos.get(0);
                  var unaryPosition = pos.get(1);
                  List<com.github.javaparser.Position> usePositions = new ArrayList<>();
                  for(com.github.javaparser.Position p:usePositionsDup){
                    if(!globalDefPositions.contains(p) || unaryPosition.contains(p)){
                      usePositions.add(p);
                    }
                  }
                  logger.info("final "+usePositions);
                  int i = 0;
                  while (i < uses.size() && i < usePositions.size()) {
                    logger.info(uses.toString());
                    if(uses.get(i).isEditorHighlight()) {
                      logger.info("is highlighted");
                      var defUseVariable = uses.get(i);
                      var parserPosition = usePositions.get(i);
                      var label = "Use";

                      var lspPos = new Position(parserPosition.line - 1, parserPosition.column - 1);
                      var hint = new InlayHint(lspPos, Either.forLeft(label));
                      hint.setPaddingLeft(true);
                      hint.setPaddingRight(true);
                      inlayHints.add(hint);

                      if (highlightedDefUseVariables.containsKey(params.getTextDocument())) {
                        highlightedDefUseVariables.get(params.getTextDocument()).put(lspPos, defUseVariable);
                      } else {
                        HashMap<Position, DefUse> newMap = new HashMap<>();
                        newMap.put(lspPos, defUseVariable);
                        highlightedDefUseVariables.put(params.getTextDocument(), newMap);
                      }
                    }
                    i++;
                  }
                }
              });
    });
  }
}
