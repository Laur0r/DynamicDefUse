package dacite.ls;

import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class CodeAnalyser {

  CompilationUnit compilationUnit;

  private static final Logger logger = LoggerFactory.getLogger(CodeAnalyser.class);

  public CodeAnalyser(String code) {
    this.compilationUnit = StaticJavaParser.parse(code);
    /*YamlPrinter printer = new YamlPrinter(true);
    logger.info(printer.output(compilationUnit));*/
  }

  public CompilationUnit getCompilationUnit() {
    return compilationUnit;
  }

  public String extractClassName() {
    var cls = this.compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).orElse(null);
    if (cls != null) {
      return cls.getName().asString();
    }
    return null;
  }

  public String extractPackageName() {
    var pckg = this.compilationUnit.getPackageDeclaration().orElse(null);
    if (pckg != null) {
      return pckg.getName().asString();
    }
    return null;
  }

  public List<List<Position>> extractVariablePositionsAtLine(int lineNumber, String variableName, boolean def) {
    List<Position> unaryPosition = new ArrayList();
    List<Position> positions = new ArrayList<>();
    List<List<Position>> output = new ArrayList<>();
    List<Node> nodes = extractNodesAtLine(lineNumber);
    for(int i=0; i<nodes.size();i++){
      Node node = nodes.get(i);
      if (node instanceof AssignExpr) {
        // Expression = Expression
        Expression e;
        if(def){
          e = ((AssignExpr) node).getTarget();
        } else {
          e = ((AssignExpr) node).getValue();
        }
        //logger.info(e.toString() +" "+e.getClass().getSimpleName());
        if(e instanceof NameExpr){
          NameExpr nameExpr = ((NameExpr) e);
          if (nameExpr.getName().asString().equals(variableName)) {
            nameExpr.getName().getRange().ifPresent(range -> positions.add(range.begin));
          }
        } else if(e instanceof ArrayAccessExpr){
          NameExpr nameExpr = (NameExpr)((ArrayAccessExpr) e).getName();
          if (nameExpr.getName().asString().equals(variableName)) {
            nameExpr.getName().getRange().ifPresent(range -> positions.add(range.begin));
          }
        } else if(e instanceof FieldAccessExpr) {
          FieldAccessExpr name = (FieldAccessExpr) e;
          String fieldName = name.toString();
          if(fieldName.contains("this.") && !variableName.contains(".")){
            fieldName = fieldName.substring(fieldName.indexOf(".")+1);
          }
          if (fieldName.equals(variableName)) {
            name.getRange().ifPresent(range -> positions.add(range.begin));
          }
        } else {
          List<Position> pos = extractVariablePositionFromNode(e.getChildNodes(), variableName);
          positions.addAll(pos);
        }
      } else if (node instanceof VariableDeclarator) {
        // Example: int n = fibonacci(5)
        if(def){
          SimpleName name = ((VariableDeclarator) node).getName();
          if(name.asString().equals(variableName)){
            name.getRange().ifPresent(range -> positions.add(range.begin));
          }
        } else {
          Optional<Expression> expr = ((VariableDeclarator) node).getInitializer();
          if(expr.isPresent()){
            List<Position> pos = extractVariablePositionFromNode(expr.get().getChildNodes(), variableName);
            positions.addAll(pos);
          }
        }
      } else if (node instanceof Parameter) {
        // Example: method(int n)
        var parameter = (Parameter) node;
        if (parameter.getName().asString().equals(variableName)) {
          parameter.getName().getRange().ifPresent(range -> positions.add(range.begin));
        }
      } else if(node instanceof UnaryExpr){
        // Example: i++
        Expression e = ((UnaryExpr)node).getExpression();
        NameExpr nameExpr;
        if(e instanceof NameExpr){
          nameExpr = ((NameExpr) e);

        } else if(e instanceof ArrayAccessExpr){
          nameExpr = (NameExpr)((ArrayAccessExpr) e).getName();
        } else {
          nameExpr = null;
          List<Position> pos = extractVariablePositionFromNode(e.getChildNodes(), variableName);
          positions.addAll(pos);
        }
        if (nameExpr != null && nameExpr.getName().asString().equals(variableName)) {
          nameExpr.getName().getRange().ifPresent(range -> {positions.add(range.begin); unaryPosition.add(range.begin);});
        }
      } else if (!def){
        if(node instanceof NameExpr) {
          var nameExpr = ((NameExpr) node);
          if (nameExpr.getName().asString().equals(variableName)) {
            nameExpr.getName().getRange().ifPresent(range -> positions.add(range.begin));
          }
        }
        else if(node instanceof FieldAccessExpr) {
          FieldAccessExpr name = (FieldAccessExpr) node;
          String fieldName = name.toString();
          if(fieldName.contains("this.") && !variableName.contains(".")){
            fieldName = fieldName.substring(fieldName.indexOf(".")+1);
          }
          if (fieldName.equals(variableName)) {
            name.getRange().ifPresent(range -> positions.add(range.begin));
          }
        }
      }
    }
    // Sort
    Comparator<Position> byColumn = Comparator.comparingInt(it -> it.column);
    positions.sort(byColumn);
    output.add(positions);
    output.add(unaryPosition);

    return output;
  }

  public List<Position> extractVariablePositionFromNode(List<Node> nodes, String variableName){
    List<Position> positions = new ArrayList<>();
    nodes.forEach(node -> {
      //logger.info(node.toString() + " " + node.getClass().getSimpleName());
      if (node instanceof NameExpr) {
        // Example: if(n == 0)
        var nameExpr = ((NameExpr) node);
        if (nameExpr.getName().asString().equals(variableName)) {
          nameExpr.getName().getRange().ifPresent(range -> positions.add(range.begin));
        }
      } else if (node instanceof FieldAccessExpr) {
        FieldAccessExpr name = (FieldAccessExpr) node;
        if (name.toString().equals(variableName)) {
          name.getRange().ifPresent(range -> positions.add(range.begin));
        }
      }
    });
    return positions;
  }

  public List<Node> extractNodesAtLine(int lineNumber) {
    return compilationUnit.findAll(Node.class).stream().filter(it -> {
      var range = it.getRange().orElse(null);
      return range != null && range.begin.line == lineNumber;
    }).collect(Collectors.toList());
  }

  public List<CodeLens> extractCodeLens(String uri){
    List<CodeLens> codeLenses = new ArrayList<>();
    compilationUnit.findAll(ClassOrInterfaceDeclaration.class).stream()
            .filter(coid -> coid.getMethods().stream().anyMatch(md -> md.getAnnotationByName("Test").isPresent()))
            .forEach(coid -> coid.getName().getRange().ifPresent(range -> {
              codeLenses.add(new CodeLens(new Range(new org.eclipse.lsp4j.Position(range.begin.line - 1, range.begin.column - 1),
                      new org.eclipse.lsp4j.Position(range.end.line - 1, range.end.column)),
                      new Command("Run Dacite Analysis", "dacite.analyze", List.of(uri)), null));
            }));

    compilationUnit.findAll(ClassOrInterfaceDeclaration.class).stream().filter(coid ->String.valueOf(coid.getName()).contains("DaciteSymbolicDriver"))
            .forEach(coid ->coid.getName().getRange().ifPresent(range -> {
              codeLenses.add(new CodeLens(new Range(new org.eclipse.lsp4j.Position(range.begin.line - 1, range.begin.column - 1),
                      new org.eclipse.lsp4j.Position(range.end.line - 1, range.end.column)),
                      new Command("Run Dacite Symbolic Analysis", "dacite.analyzeSymbolic", List.of(uri)), null));
            }));
    return codeLenses;
  }
}
