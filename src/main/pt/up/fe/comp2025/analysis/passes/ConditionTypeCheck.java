package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

public class ConditionTypeCheck extends AnalysisVisitor {

    private TypeUtils typeUtils;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IF_STMT, this::visitConditional);
        addVisit(Kind.WHILE_STMT, this::visitConditional);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        this.typeUtils = new TypeUtils(table);
        typeUtils.setCurrentMethod(method.get("methodName"));
        return null;
    }

    private Void visitConditional(JmmNode node, SymbolTable table) {
        JmmNode condition = node.getChildren().get(0);
        Type conditionType = typeUtils.getExprType(condition);

        if (!conditionType.getName().equals("boolean") || conditionType.isArray()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    condition.getLine(),
                    condition.getColumn(),
                    "Condition must be a non-array boolean, but found '" +
                            TypeUtils.formatType(conditionType) + "'",
                    null)
            );
        }
        return null;
    }
}