package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

/**
 * Utility methods for working with types in the AST.
 */
public class TypeUtils {

    private final SymbolTable symbolTable;
    private String currentMethod;

    public TypeUtils(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.currentMethod = null;
    }

    public void setCurrentMethod(String methodName) {
        this.currentMethod = methodName;
    }

    /**
     * Creates a new int type
     */
    public static Type newIntType() {
        return new Type("int", false);
    }

    /**
     * Creates a new boolean type
     */
    public static Type newBooleanType() {
        return new Type("boolean", false);
    }

    /**
     * Creates a new int array type
     */
    public static Type newIntArrayType() {
        return new Type("int", true);
    }

    /**
     * Converts a JmmNode type node to a Type
     */
    public Type convertType(JmmNode typeNode) {
        // Check if it's a primitive or array
        String typeName = typeNode.get("name");
        boolean isArray = false;

        // Try to get isArray attribute if it exists
        try {
            if (typeNode.hasAttribute("isArray")) {
                isArray = Boolean.parseBoolean(typeNode.get("isArray"));
            }
        } catch (Exception e) {
            // Ignore if attribute doesn't exist
        }

        return new Type(typeName, isArray);
    }

    /**
     * Determines the type of an expression
     */
    public Type getExprType(JmmNode expr) {
        String kind = expr.getKind();

        switch(kind) {
            case "Integer":
                return newIntType();
            case "Boolean":
                return newBooleanType();
            case "Identifier":
                return resolveIdentifierType(expr.get("value"));
            case "This":
                return new Type(symbolTable.getClassName(), false);
            case "BinaryOp":
                return getBinaryOpType(expr);
            case "ArrayAccess":
                // Array access gives element type
                Type arrayType = getExprType(expr.getChildren().get(0));
                if (arrayType.isArray()) {
                    return new Type(arrayType.getName(), false);
                }
                return newIntType(); // Default to int
            case "IntArrayDeclaration":
                return newIntArrayType();
            case "LengthOp":
                return newIntType();
            case "MethodCall":
                return getMethodCallType(expr);
            case "GeneralDeclaration":
                return new Type(expr.get("name"), false);
            case "ArrayInitializer":
                return getArrayInitializerType(expr);
            case "UnaryOp":
                String op = expr.get("op");
                if (op.equals("!")) {
                    return newBooleanType();
                } else {
                    return getExprType(expr.getChildren().get(0));
                }
            default:
                // Default to int if we can't determine
                return newIntType();
        }
    }

    private Type getArrayInitializerType(JmmNode node) {
        if (node.getNumChildren() == 0) {
            return newIntArrayType(); // Default to int array if empty
        }

        // Get type of first element and make it an array
        Type elemType = getExprType(node.getChildren().get(0));
        return new Type(elemType.getName(), true);
    }

    private Type getBinaryOpType(JmmNode node) {
        String op = node.get("op");

        // Relational operations return boolean
        if (op.equals("<") || op.equals(">") || op.equals("==") || op.equals("!=") ||
                op.equals("<=") || op.equals(">=") || op.equals("&&") || op.equals("||")) {
            return newBooleanType();
        }

        // Arithmetic operations return int
        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%")) {
            return newIntType();
        }

        // For any other operation, get the type of the left operand as default
        return getExprType(node.getChildren().get(0));
    }

    private Type getMethodCallType(JmmNode node) {
        String methodName = node.get("value");

        // Check if it's a method from the current class
        if (symbolTable.getMethods().contains(methodName)) {
            return symbolTable.getReturnType(methodName);
        }

        // If not a class method, assume it returns int (external method)
        return newIntType();
    }

    private Type resolveIdentifierType(String id) {
        // Check method parameters
        if (currentMethod != null) {
            for (Symbol param : symbolTable.getParameters(currentMethod)) {
                if (param.getName().equals(id)) {
                    return param.getType();
                }
            }

            // Check local variables
            for (Symbol local : symbolTable.getLocalVariables(currentMethod)) {
                if (local.getName().equals(id)) {
                    return local.getType();
                }
            }
        }

        // Check fields
        for (Symbol field : symbolTable.getFields()) {
            if (field.getName().equals(id)) {
                return field.getType();
            }
        }

        // Default to int if not found
        return newIntType();
    }
}