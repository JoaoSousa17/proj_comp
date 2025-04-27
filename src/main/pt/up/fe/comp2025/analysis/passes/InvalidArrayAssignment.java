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

public class InvalidArrayAssignment extends AnalysisVisitor {

    private String currentMethod;
    private JmmNode currentMethodNode;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("methodName");
        currentMethodNode = method;
        return null;
    }

    private Void visitAssignStmt(JmmNode assign, SymbolTable table) {
        String varName = null;
        JmmNode rhs = null;

        if (assign.hasAttribute("varName")) {
            varName = assign.get("varName");
            if (assign.getChildren().size() >= 1) {
                rhs = assign.getChildren().get(0);
            }
        } else if (assign.getChildren().size() >= 2) {
            JmmNode lhs = assign.getChildren().get(0);
            rhs = assign.getChildren().get(1);
            if (lhs.getKind().equals(Kind.IDENTIFIER.getNodeName())) {
                varName = lhs.get("value");
            }
        } else {
            List<JmmNode> siblings = currentMethodNode.getChildren();
            int index = siblings.indexOf(assign);
            for (int i = index - 1; i >= 0; i--) {
                JmmNode sibling = siblings.get(i);
                if (sibling.getKind().equals("VarDecl") && sibling.hasAttribute("varName")) {
                    varName = sibling.get("varName");
                    break;
                }
            }
            if (assign.getChildren().size() >= 1) {
                rhs = assign.getChildren().get(0);
            }
        }

        if (varName == null || rhs == null || !rhs.getKind().equals("ArrayInitializer")) {
            return null;
        }

        Type varType = resolveVariableType(varName, table);
        if (varType == null) {
            return null;
        }

        if (!varType.isArray()) {
            String message = String.format(
                    "Invalid array assignment: cannot assign array literal to variable '%s' of non-array type %s.",
                    varName,
                    TypeUtils.formatType(varType)
            );
            addReport(Report.newError(Stage.SEMANTIC, assign.getLine(), assign.getColumn(), message, null));
        }

        return null;
    }

    private Type resolveVariableType(String varName, SymbolTable table) {
        Optional<Type> typeOpt = table.getParameters(currentMethod).stream()
                .filter(param -> param.getName().equals(varName))
                .map(param -> param.getType())
                .findFirst();
        if (typeOpt.isPresent()) {
            return typeOpt.get();
        }

        typeOpt = table.getLocalVariables(currentMethod).stream()
                .filter(local -> local.getName().equals(varName))
                .map(local -> local.getType())
                .findFirst();
        if (typeOpt.isPresent()) {
            return typeOpt.get();
        }

        typeOpt = table.getFields().stream()
                .filter(field -> field.getName().equals(varName))
                .map(field -> field.getType())
                .findFirst();
        return typeOpt.orElse(null);
    }
}





