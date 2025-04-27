package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.List;
import java.util.Optional;

public class InvalidTypeAssignment extends AnalysisVisitor {

    private String currentMethod;
    private TypeUtils typeUtils;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("methodName");
        typeUtils = new TypeUtils(table);
        typeUtils.setCurrentMethod(currentMethod);
        return null;
    }

    private Void visitAssignStmt(JmmNode assign, SymbolTable table) {
        if (currentMethod == null) {
            return null;
        }

        if (assign.getChildren().size() == 1) {
            checkDirectAssignment(assign, table);
        } else if (assign.getChildren().size() >= 2) {
            checkSeparateAssignment(assign, table);
        }
        return null;
    }

    private void checkDirectAssignment(JmmNode assign, SymbolTable table) {
        JmmNode rhs = assign.getChildren().get(0);
        String varName = findAssignedVarName(assign, table);

        if (varName == null) {
            return;
        }

        Type varType = resolveVariableType(varName, table);
        Type rhsType = inferExpressionType(rhs, table);

        if (varType != null && rhsType != null && !isTypeCompatible(varType, rhsType, table)) {
            addTypeMismatchError(assign, varType, rhsType);
        }
    }

    private void checkSeparateAssignment(JmmNode assign, SymbolTable table) {
        JmmNode lhs = assign.getChildren().get(0);
        JmmNode rhs = assign.getChildren().get(1);

        String varName = lhs.get("value");
        if (varName == null) {
            return;
        }

        Type varType = resolveVariableType(varName, table);
        Type rhsType = inferExpressionType(rhs, table);

        if (varType != null && rhsType != null && !isTypeCompatible(varType, rhsType, table)) {
            addTypeMismatchError(assign, varType, rhsType);
        }
    }

    private String findAssignedVarName(JmmNode assign, SymbolTable table) {
        if (assign.hasAttribute("varName")) {
            return assign.get("varName");
        }

        JmmNode method = assign.getParent();
        while (method != null && !method.getKind().equals("MethodDecl")) {
            method = method.getParent();
        }

        if (method != null) {
            int assignIndex = method.getChildren().indexOf(assign);
            for (int i = assignIndex - 1; i >= 0; i--) {
                JmmNode sibling = method.getChildren().get(i);
                if (sibling.getKind().equals("VarDecl") && sibling.hasAttribute("varName")) {
                    return sibling.get("varName");
                }
            }
        }
        return null;
    }

    private Type resolveVariableType(String varName, SymbolTable table) {
        // Check parameters
        Optional<Type> paramType = table.getParameters(currentMethod).stream()
                .filter(p -> p.getName().equals(varName))
                .map(p -> p.getType())
                .findFirst();
        if (paramType.isPresent()) return paramType.get();

        // Check local variables
        Optional<Type> localType = table.getLocalVariables(currentMethod).stream()
                .filter(v -> v.getName().equals(varName))
                .map(v -> v.getType())
                .findFirst();
        if (localType.isPresent()) return localType.get();

        // Check fields
        Optional<Type> fieldType = table.getFields().stream()
                .filter(f -> f.getName().equals(varName))
                .map(f -> f.getType())
                .findFirst();

        return fieldType.orElse(null);
    }

    private Type inferExpressionType(JmmNode expr, SymbolTable table) {
        switch (expr.getKind()) {
            case "Integer":
                return new Type("int", false);
            case "Boolean":
                return new Type("boolean", false);
            case "Identifier":
                return resolveVariableType(expr.get("value"), table);
            case "NewObject":
                String className = expr.get("value");
                if (className.equals(table.getClassName())) {
                return new Type(className, false);
            }
            if (isImportedClass(className, table)) {
                return new Type(className, false);
            }
            if (table.getSuper() != null && table.getSuper().equals(className)) {
                return new Type(className, false);
            }
            return null;
            default:
                return null;
        }
    }

    private boolean isTypeCompatible(Type targetType, Type sourceType, SymbolTable table) {
        // Direct type match
        if (targetType.getName().equals(sourceType.getName())) {
            return true;
        }

        // Check if both types are imported classes
        if (isImportedClass(targetType.getName(), table) && isImportedClass(sourceType.getName(), table)) {
            return true;
        }

        // Check for inheritance
        if (sourceType.getName().equals(table.getSuper()) &&
                targetType.getName().equals(table.getClassName())) {
            return true;
        }

        // Check if target is superclass of source
        if (targetType.getName().equals(table.getSuper()) &&
                sourceType.getName().equals(table.getClassName())) {
            return true;
        }

        return false;
    }

    private boolean isImportedClass(String className, SymbolTable table) {
        // Check if it's a direct import (import A;)
        if (table.getImports().contains(className)) {
            return true;
        }

        // Check if it's a qualified import (import some.package.A;)
        return table.getImports().stream()
                .anyMatch(imp -> imp.endsWith("." + className));
    }

    private void addTypeMismatchError(JmmNode node, Type expected, Type actual) {
        addReport(Report.newError(
                Stage.SEMANTIC,
                node.getLine(),
                node.getColumn(),
                String.format("Type mismatch: cannot assign %s to variable of type %s",
                        TypeUtils.formatType(actual),
                        TypeUtils.formatType(expected)),
                null
        ));
    }
}