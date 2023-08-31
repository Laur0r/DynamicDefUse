package dacite.ls;

import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.YamlPrinter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
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
          if(fieldName.contains("this.")){
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
          if (name.toString().equals(variableName)) {
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

  public static Map<String, List<String>> analyseJUnitTest(File project, String classname){
    String packageName = classname.substring(0, classname.lastIndexOf(".")).replace(".","/");
    ClassReader reader;
    try {
      //logger.info(classname);
      URL url = project.toURI().toURL();
      logger.info(String.valueOf(url));
      URLClassLoader classLoader = new URLClassLoader(new URL[]{url});
      InputStream input = classLoader.getResourceAsStream(classname.replace('.', '/') + ".class");
      //logger.info(input.toString());
      reader = new ClassReader(input);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    ClassNode classNode = new ClassNode();
    reader.accept(classNode, 0);
    Map<String, List<String>> invokedMethods = new HashMap<>();
    for(MethodNode mnode : classNode.methods) {
      if (mnode.visibleAnnotations != null) {
        for (AnnotationNode an : mnode.visibleAnnotations) {
          if (an.desc.equals("Lorg/junit/Test;")) {
            InsnList insns = mnode.instructions;
            Iterator<AbstractInsnNode> j = insns.iterator();
            while (j.hasNext()) {
              AbstractInsnNode in = j.next();
              if (in instanceof MethodInsnNode) {
                MethodInsnNode methodins = (MethodInsnNode) in;
                if(methodins.owner.contains(packageName) && !methodins.name.equals("<init>")){
                  String name = methodins.owner + "." + methodins.name;
                  if(methodins.owner.contains("/")){
                    name = methodins.owner.substring(methodins.owner.lastIndexOf("/")+1)+"." + methodins.name;
                  }
                  Type[] types = Type.getArgumentTypes(methodins.desc);
                  List<String> list = new ArrayList<>();
                  if(methodins.getOpcode() == Opcodes.INVOKESTATIC){
                    list.add("static");
                  } else if(methodins.getOpcode() == Opcodes.INVOKEINTERFACE){
                    list.add("interface");
                  } else {
                    list.add("object");
                  }
                  String returnType = Type.getReturnType(methodins.desc).getClassName();
                  list.add(returnType);
                  List<String> list2 = Arrays.stream(types).map(Type::getClassName).collect(Collectors.toList());
                  list.addAll(list2);
                  invokedMethods.put(name, list);
                }
              }
            }
          }
        }
      }
    }
    return invokedMethods;
  }
}
