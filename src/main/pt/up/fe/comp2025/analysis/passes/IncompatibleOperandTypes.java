package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

public class IncompatibleOperandTypes extends AnalysisVisitor {

    private TypeUtils typeUtils;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.BINARY_OP, this::visitBinaryExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        this.typeUtils = new TypeUtils(table);
        typeUtils.setCurrentMethod(method.get("methodName"));
        return null;
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        if (typeUtils == null) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    binaryExpr.getLine(),
                    binaryExpr.getColumn(),
                    "Current method is not set. Cannot check binary expressions.",
                    null)
            );
            return null;
        }

        JmmNode leftChild = binaryExpr.getChildren().get(0);
        JmmNode rightChild = binaryExpr.getChildren().get(1);

        Type leftType = typeUtils.getExprType(leftChild);
        Type rightType = typeUtils.getExprType(rightChild);
        String operator = binaryExpr.get("op");

        switch (operator) {
            case "+":
            case "-":
            case "*":
            case "/":
                validateArithmeticOperands(binaryExpr, leftType, rightType, operator);
                break;
            case "&&":
                validateLogicalOperands(binaryExpr, leftType, rightType);
                break;
            case "<":
                validateComparisonOperands(binaryExpr, leftType, rightType);
                break;
            default:
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        binaryExpr.getLine(),
                        binaryExpr.getColumn(),
                        "Unsupported binary operator: " + operator,
                        null)
                );
        }

        return null;
    }

    private void validateArithmeticOperands(JmmNode node, Type left, Type right, String operator) {
        if (!isValidIntOperand(left) || !isValidIntOperand(right)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    node.getLine(),
                    node.getColumn(),
                    "Operator '" + operator + "' expects both operands to be non-array integers, but found '" +
                            TypeUtils.formatType(left) + "' and '" + TypeUtils.formatType(right) + "'.",
                    null)
            );
        }
    }

    private void validateLogicalOperands(JmmNode node, Type left, Type right) {
        if (!isValidBooleanOperand(left) || !isValidBooleanOperand(right)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    node.getLine(),
                    node.getColumn(),
                    "Logical operator '&&' expects both operands to be non-array boolean, but found '" +
                            TypeUtils.formatType(left) + "' and '" + TypeUtils.formatType(right) + "'.",
                    null)
            );
        }
    }

    private void validateComparisonOperands(JmmNode node, Type left, Type right) {
        if (!isValidIntOperand(left) || !isValidIntOperand(right)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    node.getLine(),
                    node.getColumn(),
                    "Comparison operator '<' expects both operands to be non-array integers, but found '" +
                            TypeUtils.formatType(left) + "' and '" + TypeUtils.formatType(right) + "'.",
                    null)
            );
        }
    }

    private boolean isValidIntOperand(Type type) {
        return type.getName().equals("int") && !type.isArray();
    }

    private boolean isValidBooleanOperand(Type type) {
        return type.getName().equals("boolean") && !type.isArray();
    }
}