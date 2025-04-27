package pt.up.fe.comp2025.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.*;
import java.util.stream.Collectors;

public class JmmSymbolTableBuilder {

    private List<Report> reports; // List to store semantic error reports

    public List<Report> getReports() {
        return reports;
    }

    private static Report newError(JmmNode node, String message) {
        return Report.newError(
                Stage.SEMANTIC,
                node.getLine(),
                node.getColumn(),
                message,
                null);
    }

    // Main method to build the symbol table from the AST root node
    public JmmSymbolTable build(JmmNode root) {
        reports = new ArrayList<>(); // Initialize the reports list

        // Find the class declaration node in the AST
        JmmNode classDecl = findClassDeclaration(root);
        if (classDecl == null) {
            throw new IllegalArgumentException("Error: No class declaration found in AST!");
        }

        // Ensure the class declaration node has a "className" attribute
        if (!classDecl.hasAttribute("className")) {
            throw new IllegalArgumentException("ClassDecl does not contain attribute 'className'");
        }

        // Extract the class name and superclass (if any)
        String className = classDecl.get("className");
        String superClass = classDecl.hasAttribute("superName") ? classDecl.get("superName") : null;

        // Build the list of imports, fields, methods, return types, parameters, and local variables
        List<String> imports = buildImports(root);
        List<Symbol> fields = buildFields(classDecl);
        List<String> methods = buildMethods(classDecl);
        Map<String, Type> returnTypes = buildReturnTypes(classDecl);
        Map<String, List<Symbol>> params = buildParams(classDecl);
        Map<String, List<Symbol>> locals = buildLocals(classDecl);

        // Create and return the symbol table with the extracted information
        return new JmmSymbolTable(imports, className, superClass, fields, methods, returnTypes, params, locals);
    }

    // Finds and returns the class declaration node in the AST
    private JmmNode findClassDeclaration(JmmNode root) {
        return root.getChildren().stream()
                .filter(node -> node.getKind().equals("ClassDecl"))
                .findFirst()
                .orElse(null);
    }

    // Builds and returns a list of imported classes from the AST
    private List<String> buildImports(JmmNode root) {
        return root.getChildren("ImportDecl").stream()
                .map(importNode -> importNode.getChildren().stream()
                        .map(child -> child.get("name"))
                        .collect(Collectors.joining(".")))
                .toList();
    }

    // Builds and returns a list of fields (class variables) from the class declaration node
    private List<Symbol> buildFields(JmmNode classDecl) {
        return classDecl.getChildren("VarDecl").stream()
                .map(varDecl -> {
                    if (!varDecl.hasAttribute("varName")) {
                        throw new IllegalArgumentException("[ERROR] VarDecl missing 'varName' attribute.");
                    }
                    return new Symbol(TypeUtils.convertType(varDecl.getChild(0)), varDecl.get("varName"));
                })
                .toList();
    }

    // Builds and returns a list of method names from the class declaration node
    private List<String> buildMethods(JmmNode classDecl) {
        return classDecl.getChildren("MethodDecl").stream()
                .map(method -> {
                    if (!method.hasAttribute("methodName")) {
                        throw new IllegalArgumentException("MethodDecl does not contain attribute 'methodName'");
                    }
                    return method.get("methodName");
                })
                .toList();
    }

    // Builds and returns a map of method names to their return types
    private Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        return classDecl.getChildren("MethodDecl").stream()
                .collect(Collectors.toMap(
                        method -> {
                            if (!method.hasAttribute("methodName")) {
                                throw new IllegalArgumentException("MethodDecl does not contain attribute 'methodName'");
                            }
                            return method.get("methodName");
                        },
                        method -> TypeUtils.convertType(method.getChild(0))
                ));
    }

    // Builds and returns a map of method names to their parameters
    private Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        return classDecl.getChildren("MethodDecl").stream()
                .collect(Collectors.toMap(
                        method -> {
                            if (!method.hasAttribute("methodName")) {
                                throw new IllegalArgumentException("MethodDecl does not contain attribute 'methodName'");
                            }
                            return method.get("methodName");
                        },
                        method -> {
                            // Find the ParamList node for the method
                            JmmNode paramList = method.getChildren().stream()
                                    .filter(child -> child.getKind().equals("ParamList"))
                                    .findFirst()
                                    .orElse(null);

                            if (paramList == null) {
                                return Collections.emptyList(); // No parameters for this method
                            }

                            // Extract parameters from the ParamList
                            return paramList.getChildren("Param").stream()
                                    .map(param -> {
                                        if (!param.hasAttribute("paramName")) {
                                            throw new IllegalArgumentException("[ERROR] Param node is missing 'paramName' attribute.");
                                        }

                                        if (param.getNumChildren() == 0) {
                                            throw new IllegalArgumentException("[ERROR] Param node is missing a type node.");
                                        }

                                        // Extract the type and name of the parameter
                                        JmmNode typeNode = param.getChild(0);
                                        String paramName = param.get("paramName");
                                        Type paramType = TypeUtils.convertType(typeNode);

                                        return new Symbol(paramType, paramName);
                                    })
                                    .toList();
                        }
                ));
    }

    // Builds and returns a map of method names to their local variables
    private Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        return classDecl.getChildren("MethodDecl").stream()
                .collect(Collectors.toMap(
                        method -> method.get("methodName"),
                        method -> method.getChildren("VarDecl").stream()
                                .map(varDecl -> {
                                    if (!varDecl.hasAttribute("varName")) {
                                        throw new IllegalArgumentException("VarDecl does not contain attribute 'varName'");
                                    }
                                    return new Symbol(TypeUtils.convertType(varDecl.getChild(0)), varDecl.get("varName"));
                                })
                                .toList()
                ));
    }
}