package dacite.ls;

import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CodeAnalyser {

  CompilationUnit compilationUnit;

  private static final Logger logger = LoggerFactory.getLogger(CodeAnalyser.class);

  public CodeAnalyser(String code) {
    this.compilationUnit = StaticJavaParser.parse(code);
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

  public List<Position> extractVariablePositionsAtLine(int lineNumber, String variableName, boolean def) {
    List<Position> positions = new ArrayList<>();
    List<Node> nodes = extractNodesAtLine(lineNumber);
    //logger.info("ln: "+lineNumber+" name: "+variableName);
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
          nameExpr.getName().getRange().ifPresent(range -> positions.add(range.begin));
        }
      } else if (node instanceof VariableDeclarationExpr) {
        // Example: int n = fibonacci(5)
        ((VariableDeclarationExpr) node).getVariables().stream().filter(v -> Objects.equals(v.getName().asString(),
                variableName)).forEach(v -> v.getRange().ifPresent(range -> positions.add(range.begin)));
      } else if (node instanceof Parameter) {
        // Example: method(int n)
        var parameter = (Parameter) node;
        if (parameter.getName().asString().equals(variableName)) {
          parameter.getName().getRange().ifPresent(range -> positions.add(range.begin));
        }
      } else if (node instanceof NameExpr) {
        // Example: if(n == 0)
        var nameExpr = ((NameExpr) node);
        if (nameExpr.getName().asString().equals(variableName)) {
          nameExpr.getName().getRange().ifPresent(range -> positions.add(range.begin));
        }
      } else if (node instanceof FieldAccessExpr){
        FieldAccessExpr name = (FieldAccessExpr) node;
        if(name.toString().equals(variableName)){
          name.getRange().ifPresent(range -> positions.add(range.begin));
        }
      }
    }
    return positions;
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

  /*public List<Position> extractVariablePositionsAtLine(int lineNumber, String variableName) {
    List<Position> positions = new ArrayList<>();
    extractNodesAtLine(lineNumber).forEach(node -> {
      logger.info(node.toString() +" "+node.getClass().getSimpleName());

      if (node instanceof VariableDeclarationExpr) {
        // Example: int n = fibonacci(5)
        ((VariableDeclarationExpr) node).getVariables().stream().filter(v -> Objects.equals(v.getName().asString(),
            variableName)).forEach(v -> v.getRange().ifPresent(range -> positions.add(range.begin)));
      } else if (node instanceof Parameter) {
        // Example: method(int n)
        var parameter = (Parameter) node;
        if (parameter.getName().asString().equals(variableName)) {
          parameter.getName().getRange().ifPresent(range -> positions.add(range.begin));
        }
      } else if (node instanceof NameExpr) {
        // Example: if(n == 0)
        var nameExpr = ((NameExpr) node);
        if (nameExpr.getName().asString().equals(variableName)) {
          nameExpr.getName().getRange().ifPresent(range -> positions.add(range.begin));
        }
      } else if (node instanceof FieldAccessExpr){
        FieldAccessExpr name = (FieldAccessExpr) node;
        if(name.toString().equals(variableName)){
            name.getRange().ifPresent(range -> positions.add(range.begin));
        }
      } else if(node instanceof AssignExpr){

      }
    });
    if(variableName.equals("a") && lineNumber == 41) {
      //logger.info(positions.toString());
    }
    // Sort
    Comparator<Position> byColumn = Comparator.comparingInt(it -> it.column);
    positions.sort(byColumn);

    return positions;
  }*/

  public List<Node> extractNodesAtLine(int lineNumber) {
    return compilationUnit.findAll(Node.class).stream().filter(it -> {
      var range = it.getRange().orElse(null);
      return range != null && range.begin.line == lineNumber;
    }).collect(Collectors.toList());
  }

}
