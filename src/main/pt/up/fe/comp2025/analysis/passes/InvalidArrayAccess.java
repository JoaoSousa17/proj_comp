package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

public class InvalidArrayAccess extends AnalysisVisitor {

    private TypeUtils typeUtils;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit("ArrayAccess", this::visitArrayAccess);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        this.typeUtils = new TypeUtils(table);
        typeUtils.setCurrentMethod(method.get("methodName"));
        return null;
    }

    private Void visitArrayAccess(JmmNode arrayAccess, SymbolTable table) {
        if (typeUtils == null) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    arrayAccess.getLine(),
                    arrayAccess.getColumn(),
                    "Current method is not set. Cannot check array access expressions.",
                    null)
            );
            return null;
        }

        if (arrayAccess.getChildren().size() != 2) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    arrayAccess.getLine(),
                    arrayAccess.getColumn(),
                    "Malformed array access operation - expected array and index expressions.",
                    null)
            );
            return null;
        }

        JmmNode arrayExpr = arrayAccess.getChildren().get(0);
        JmmNode indexExpr = arrayAccess.getChildren().get(1);

        validateArrayExpression(arrayAccess, arrayExpr);
        validateIndexExpression(arrayAccess, indexExpr);

        return null;
    }

    private void validateArrayExpression(JmmNode arrayAccess, JmmNode arrayExpr) {
        Type arrayType = typeUtils.getExprType(arrayExpr);

        if (!arrayType.isArray()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    arrayAccess.getLine(),
                    arrayAccess.getColumn(),
                    "Array access operation requires an array type, but found '" +
                            TypeUtils.formatType(arrayType) + "'.",
                    null)
            );
        }
    }

    private void validateIndexExpression(JmmNode arrayAccess, JmmNode indexExpr) {
        Type indexType = typeUtils.getExprType(indexExpr);

        if (!isValidArrayIndex(indexType)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    arrayAccess.getLine(),
                    arrayAccess.getColumn(),
                    "Array index must be a non-array integer, but found '" +
                            TypeUtils.formatType(indexType) + "'.",
                    null)
            );
        }
    }

    private boolean isValidArrayIndex(Type type) {
        return type.getName().equals("int") && !type.isArray();
    }
}