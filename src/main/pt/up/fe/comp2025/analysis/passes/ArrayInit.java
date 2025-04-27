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

/**
 * Analyzes array initializations, ensuring type consistency.
 */
public class ArrayInit extends AnalysisVisitor {

    private TypeUtils typeUtils;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.ARRAY_INITIALIZER, this::visitArrayInit);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        // Initialize TypeUtils and set the current method.
        this.typeUtils = new TypeUtils(table);
        typeUtils.setCurrentMethod(method.get("methodName"));
        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        if (typeUtils == null) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    assignStmt.getLine(),
                    assignStmt.getColumn(),
                    "Current method is not set. Cannot check assignments.",
                    null)
            );
            return null;
        }

        if (assignStmt.getChildren().size() < 2) return null; // Ignore invalid assignment

        JmmNode leftSide = assignStmt.getChildren().get(0);
        JmmNode rightSide = assignStmt.getChildren().get(1);

        Type leftType = typeUtils.getExprType(leftSide);
        Type rightType = typeUtils.getExprType(rightSide);

        if (!leftType.equals(rightType)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    assignStmt.getLine(),
                    assignStmt.getColumn(),
                    "Type mismatch: cannot assign '" + TypeUtils.formatType(rightType) +
                            "' to '" + TypeUtils.formatType(leftType) + "'.",
                    null)
            );
        }
        return null;
    }

    private Void visitArrayInit(JmmNode arrayInit, SymbolTable table) {
        if (typeUtils == null) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    arrayInit.getLine(),
                    arrayInit.getColumn(),
                    "Current method is not set. Cannot check array initializations.",
                    null)
            );
            return null;
        }

        // We expect the ArrayInitializer to be on the right-hand side of an assignment.
        JmmNode parent = arrayInit.getParent();
        if (parent == null || !Kind.ASSIGN_STMT.check(parent)) return null;
        // Not an assignment, ignore

        // Get the left-hand side variable node.
        JmmNode variableNode = parent.getChildren().get(0);
        Type expectedType = typeUtils.getExprType(variableNode);

        // Only proceed if the expected type is an array.
        if (expectedType.isArray()) {
            // Check that every element in the initializer matches the expected element type.
            List<JmmNode> elements = arrayInit.getChildren();
            String expectedElementType = expectedType.getName();

            for (JmmNode element : elements) {
                Type elementType = typeUtils.getExprType(element);
                if (!elementType.getName().equals(expectedElementType)) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            element.getLine(),
                            element.getColumn(),
                            "Array initializer contains mixed types. Expected '" +
                                    expectedElementType + "' but found '" + elementType.getName() + "'.",
                            null)
                    );
                }
            }
            return null; // No errors: valid array initialization.
        }

        // If the left-hand side is not an array, report an error.
        addReport(Report.newError(
                Stage.SEMANTIC,
                arrayInit.getLine(),
                arrayInit.getColumn(),
                "Cannot assign an array initializer to a non-array variable.",
                null)
        );
        return null;
    }
}

