package pt.up.fe.comp2025.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.symboltable.JmmSymbolTable;

/**
 * Utility methods regarding types.
 */
public class TypeUtils {
    private final JmmSymbolTable table;
    private String currentMethod;

    public TypeUtils(SymbolTable table) {
        this.table = (JmmSymbolTable) table;
    }

    public void setCurrentMethod(String currentMethod) {
        this.currentMethod = currentMethod;
    }

    public static Type newIntType() {
        return new Type("int", false);
    }

    public static Type newBooleanType() {
        return new Type("boolean", false);
    }

    public static Type newIntArrayType() {
        return new Type("int", true);
    }

    public static String formatType(Type type) {
        return type.getName() + (type.isArray() ? "[]" : "");
    }

    public static Type convertType(JmmNode typeNode) {
        String name;
        boolean isArray = false;

        switch (typeNode.getKind()) {
            case "IntType":
                name = "int";
                break;
            case "BooleanType":
                name = "boolean";
                break;
            case "StringType":
                name = "String";
                break;
            case "ClassType":
                if (!typeNode.hasAttribute("value")) {
                    throw new IllegalArgumentException("[ERROR] ClassType node is missing the 'value' attribute.");
                }
                name = typeNode.get("value");
                break;
            case "ArrayType":
                name = "int";
                isArray = true;
                break;
            case "StringArrayType":
                name = "String";
                isArray = true;
                break;
            case "VarArgsType":
                name = "int";
                isArray = true;
                break;
            case "VoidType":
                name = "void";
                break;
            default:
                throw new IllegalArgumentException("Unsupported type node: " + typeNode.getKind());
        }

        return new Type(name, isArray);
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     */
    public Type getExprType(JmmNode expr) {
        String kind = expr.getKind();
        switch (kind) {
            case "Integer":
                return newIntType();
            case "Boolean":
                return newBooleanType();
            case "Identifier":
                return resolveIdentifierType(expr);
            case "BinaryOp":
                return resolveBinaryOpType(expr);
            case "UnaryOp":
                return resolveUnaryOpType(expr);
            case "This":
                return new Type(table.getClassName(), false);
            case "NewArray":
                return newIntArrayType();
            case "ArrayAccess":
                return resolveArrayAccessType(expr);
            case "MethodCall":
                return newBooleanType(); // Default assumption for method calls in conditions
            case "Parenthesis":
                return getExprType(expr.getChildren().get(0));
            case "ArrayInitializer":
                return newIntArrayType();
            default:
                Report.newError(
                        Stage.SEMANTIC,
                        expr.getLine(),
                        expr.getColumn(),
                        "Unsupported expression type: " + kind,
                        null
                );
                return newIntType(); // Default return to continue analysis
        }
    }

    private Type resolveIdentifierType(JmmNode identifier) {
        String varName = identifier.get("value");

        // Check parameters
        for (var param : table.getParameters(currentMethod)) {
            if (param.getName().equals(varName)) {
                return param.getType();
            }
        }

        // Check local variables
        for (var local : table.getLocalVariables(currentMethod)) {
            if (local.getName().equals(varName)) {
                return local.getType();
            }
        }

        // Check class fields
        for (var field : table.getFields()) {
            if (field.getName().equals(varName)) {
                return field.getType();
            }
        }

        Report.newError(
                Stage.SEMANTIC,
                identifier.getLine(),
                identifier.getColumn(),
                "Variable '" + varName + "' is not declared",
                null
        );
        return newIntType(); // Default return to continue analysis
    }

    private Type resolveBinaryOpType(JmmNode binaryOp) {
        String op = binaryOp.get("op");
        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("<")) {
            return newIntType();
        }
        if (op.equals("&&") || op.equals("||") || op.equals("==") || op.equals("!=")) {
            return newBooleanType();
        }

        Report.newError(
                Stage.SEMANTIC,
                binaryOp.getLine(),
                binaryOp.getColumn(),
                "Unsupported binary operator: " + op,
                null
        );
        return newIntType(); // Default return
    }

    private Type resolveUnaryOpType(JmmNode unaryOp) {
        String op = unaryOp.get("op");
        if (op.equals("!")) {
            return newBooleanType();
        }
        if (op.equals("-")) {
            return newIntType();
        }

        Report.newError(
                Stage.SEMANTIC,
                unaryOp.getLine(),
                unaryOp.getColumn(),
                "Unsupported unary operator: " + op,
                null
        );
        return newIntType(); // Default return
    }

    private Type resolveArrayAccessType(JmmNode arrayAccess) {
        Type arrayType = getExprType(arrayAccess.getChildren().get(0));
        return new Type(arrayType.getName(), false); // Array access returns element type
    }
}