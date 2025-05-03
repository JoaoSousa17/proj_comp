package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;
    private final TypeUtils types;
    private final OptUtils ollirTypes;

    private int tempCounter = 0;
    private String currentMethod;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.types = new TypeUtils(table);
        this.ollirTypes = new OptUtils(types);
        setDefaultValue(() -> OllirExprResult.EMPTY);
    }

    @Override
    protected void buildVisitor() {
        addVisit("Identifier", this::visitIdentifier);
        addVisit("Integer", this::visitInteger);
        addVisit("Boolean", this::visitBoolean);
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("UnaryOp", this::visitUnaryOp);
        addVisit("ArrayAccess", this::visitArrayAccess);
        addVisit("MethodCall", this::visitMethodCall);
        addVisit("LengthOp", this::visitLengthOp);
        addVisit("This", this::visitThis);
        addVisit("GeneralDeclaration", this::visitGeneralDeclaration);
        addVisit("IntArrayDeclaration", this::visitIntArrayDeclaration);
        addVisit("ArrayInitializer", this::visitArrayInitializer);
        addVisit("Parenthesis", this::visitParenthesis);

        setDefaultVisit(this::defaultVisit);
    }

    public void setCurrentMethod(String methodName) {
        this.currentMethod = methodName;
        this.types.setCurrentMethod(methodName);
    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        Type intType = TypeUtils.newIntType();
        String ollirIntType = ollirTypes.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBoolean(JmmNode node, Void unused) {
        Type boolType = TypeUtils.newBooleanType();
        String ollirBoolType = ollirTypes.toOllirType(boolType);

        // In OLLIR true is 1, false is 0
        String value = node.get("value").equals("true") ? "1" : "0";
        String code = value + ollirBoolType;

        return new OllirExprResult(code);
    }

    private OllirExprResult visitIdentifier(JmmNode node, Void unused) {
        String id = node.get("value");
        Type type = resolveVariableType(id);
        String ollirType = ollirTypes.toOllirType(type);

        String code = id + ollirType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBinaryOp(JmmNode node, Void unused) {
        OllirExprResult lhs = visit(node.getChildren().get(0));
        OllirExprResult rhs = visit(node.getChildren().get(1));

        StringBuilder computation = new StringBuilder();

        // Add code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // Determine the type of the result
        Type resType = types.getExprType(node);
        String resOllirType = ollirTypes.toOllirType(resType);

        // Generate a temporary variable for the result
        String tempVar = generateTemp();
        String resultVar = tempVar + resOllirType;

        // Build the binary operation
        computation.append(resultVar).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        // Map Java operator to OLLIR operator
        String ollirOp = mapOperator(node.get("op"));
        computation.append(ollirOp).append(resOllirType).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(resultVar, computation.toString());
    }

    private OllirExprResult visitUnaryOp(JmmNode node, Void unused) {
        OllirExprResult operand = visit(node.getChildren().get(0));

        StringBuilder computation = new StringBuilder();
        computation.append(operand.getComputation());

        // Determine result type
        Type resType = types.getExprType(node);
        String resOllirType = ollirTypes.toOllirType(resType);

        // Generate temporary variable
        String tempVar = generateTemp();
        String resultVar = tempVar + resOllirType;

        // Build unary operation based on operator
        String op = node.get("op");
        if (op.equals("!")) {
            // Logical NOT - use xor with 1 (true) for boolean negation
            computation.append(resultVar).append(SPACE)
                    .append(ASSIGN).append(resOllirType).append(SPACE)
                    .append("1").append(resOllirType).append(SPACE)
                    .append("^.bool").append(SPACE)  // XOR operator for boolean negation
                    .append(operand.getCode()).append(END_STMT);
        } else {
            // For arithmetic negation (-)
            computation.append(resultVar).append(SPACE)
                    .append(ASSIGN).append(resOllirType).append(SPACE)
                    .append("0").append(resOllirType).append(SPACE)
                    .append("-.i32").append(SPACE)  // Subtraction for negation
                    .append(operand.getCode()).append(END_STMT);
        }

        return new OllirExprResult(resultVar, computation.toString());
    }

    private OllirExprResult visitArrayAccess(JmmNode node, Void unused) {
        // Get array and index expressions
        OllirExprResult arrayExpr = visit(node.getChildren().get(0));
        OllirExprResult indexExpr = visit(node.getChildren().get(1));

        StringBuilder computation = new StringBuilder();
        computation.append(arrayExpr.getComputation());
        computation.append(indexExpr.getComputation());

        // Determine element type of the array
        Type arrayType = types.getExprType(node.getChildren().get(0));
        Type elementType = new Type(arrayType.getName(), false);
        String elementTypeStr = ollirTypes.toOllirType(elementType);

        // Generate temporary variable for the array access result
        String tempVar = generateTemp();
        String resultVar = tempVar + elementTypeStr;

        // Generate array access instruction
        computation.append(resultVar).append(SPACE)
                .append(ASSIGN).append(elementTypeStr).append(SPACE)
                .append(arrayExpr.getCode()).append("[")
                .append(indexExpr.getCode()).append("]")
                .append(elementTypeStr).append(END_STMT);

        return new OllirExprResult(resultVar, computation.toString());
    }

    private OllirExprResult visitMethodCall(JmmNode node, Void unused) {
        // Get the receiver and method name
        OllirExprResult receiver = visit(node.getChildren().get(0));
        String methodName = node.get("value");

        StringBuilder computation = new StringBuilder();
        computation.append(receiver.getComputation());

        // Process arguments
        List<String> argCodes = new ArrayList<>();
        List<OllirExprResult> argResults = new ArrayList<>();

        for (int i = 1; i < node.getNumChildren(); i++) {
            OllirExprResult arg = visit(node.getChildren().get(i));
            computation.append(arg.getComputation());
            argCodes.add(arg.getCode());
            argResults.add(arg);
        }

        // Determine return type - need to check if this is a method of the current class
        Type returnType;
        if (table.getMethods().contains(methodName) &&
                receiver.getCode().startsWith("this")) {
            // Local method call
            returnType = table.getReturnType(methodName);
        } else {
            // External method call - assume int for now, semantic analysis should have checked
            returnType = new Type("int", false);
        }

        String returnTypeStr = ollirTypes.toOllirType(returnType);

        // Generate temporary variable for the result if not void
        String tempVar = generateTemp();
        String resultVar = tempVar + returnTypeStr;

        // Determine invocation type (virtual, static, etc.)
        String invocationType = "invokevirtual";

        // Construct the method call
        StringBuilder callBuilder = new StringBuilder();

        // Add assignment part if not void
        if (!returnType.getName().equals("void")) {
            callBuilder.append(resultVar).append(SPACE)
                    .append(ASSIGN).append(returnTypeStr).append(SPACE);
        }

        // Build the invocation
        callBuilder.append(invocationType).append("(")
                .append(receiver.getCode()).append(", \"")
                .append(methodName).append("\"");

        // Add arguments if any
        if (!argCodes.isEmpty()) {
            callBuilder.append(", ");
            callBuilder.append(String.join(", ", argCodes));
        }

        callBuilder.append(")").append(returnTypeStr).append(END_STMT);

        computation.append(callBuilder);

        // If void return, just return the computation, otherwise return the temp var
        if (returnType.getName().equals("void")) {
            return new OllirExprResult("", computation.toString());
        } else {
            return new OllirExprResult(resultVar, computation.toString());
        }
    }

    private OllirExprResult visitLengthOp(JmmNode node, Void unused) {
        // Get the array
        OllirExprResult array = visit(node.getChildren().get(0));

        StringBuilder computation = new StringBuilder();
        computation.append(array.getComputation());

        // The length operation returns an int
        Type intType = TypeUtils.newIntType();
        String intTypeStr = ollirTypes.toOllirType(intType);

        // Generate temporary variable for the result
        String tempVar = generateTemp();
        String resultVar = tempVar + intTypeStr;

        // Generate array length instruction
        computation.append(resultVar).append(SPACE)
                .append(ASSIGN).append(intTypeStr).append(SPACE)
                .append("arraylength(").append(array.getCode()).append(")")
                .append(intTypeStr).append(END_STMT);

        return new OllirExprResult(resultVar, computation.toString());
    }

    private OllirExprResult visitThis(JmmNode node, Void unused) {
        // 'this' refers to current instance of the class
        String className = table.getClassName();
        String thisType = "." + className;

        return new OllirExprResult("this" + thisType);
    }

    private OllirExprResult visitGeneralDeclaration(JmmNode node, Void unused) {
        // Object instantiation (new ClassName())
        String className = node.get("name");
        String classType = "." + className;

        // Generate temporary variable
        String tempVar = generateTemp();
        String resultVar = tempVar + classType;

        StringBuilder computation = new StringBuilder();

        // Generate new object instruction
        computation.append(resultVar).append(SPACE)
                .append(ASSIGN).append(classType).append(SPACE)
                .append("new(").append(className).append(")").append(classType)
                .append(END_STMT);

        // Call the constructor
        computation.append("invokespecial(").append(resultVar).append(", \"<init>\").V")
                .append(END_STMT);

        return new OllirExprResult(resultVar, computation.toString());
    }

    private OllirExprResult visitIntArrayDeclaration(JmmNode node, Void unused) {
        // new int[expr]
        OllirExprResult sizeExpr = visit(node.getChildren().get(0));

        StringBuilder computation = new StringBuilder();
        computation.append(sizeExpr.getComputation());

        // Array type
        Type arrayType = TypeUtils.newIntArrayType();
        String arrayTypeStr = ollirTypes.toOllirType(arrayType);

        // Generate temporary variable
        String tempVar = generateTemp();
        String resultVar = tempVar + arrayTypeStr;

        // Generate new array instruction
        computation.append(resultVar).append(SPACE)
                .append(ASSIGN).append(arrayTypeStr).append(SPACE)
                .append("new(array, ").append(sizeExpr.getCode()).append(")")
                .append(arrayTypeStr).append(END_STMT);

        return new OllirExprResult(resultVar, computation.toString());
    }

    private OllirExprResult visitArrayInitializer(JmmNode node, Void unused) {
        // Array initializer: [elem1, elem2, ...]
        List<JmmNode> elements = node.getChildren();

        StringBuilder computation = new StringBuilder();
        List<String> elementCodes = new ArrayList<>();

        // Process each element
        for (JmmNode element : elements) {
            OllirExprResult elemResult = visit(element);
            computation.append(elemResult.getComputation());
            elementCodes.add(elemResult.getCode());
        }

        // Determine array type based on elements or context
        Type elementType = elements.isEmpty() ?
                TypeUtils.newIntType() :
                types.getExprType(elements.get(0));
        Type arrayType = new Type(elementType.getName(), true);
        String arrayTypeStr = ollirTypes.toOllirType(arrayType);

        // Generate temporary variable for the array
        String tempVar = generateTemp();
        String resultVar = tempVar + arrayTypeStr;

        // Create array with correct size
        computation.append(resultVar).append(SPACE)
                .append(ASSIGN).append(arrayTypeStr).append(SPACE)
                .append("new(array, ").append(elements.size()).append(".i32)")
                .append(arrayTypeStr).append(END_STMT);

        // Initialize array elements
        String elementTypeStr = ollirTypes.toOllirType(elementType);
        for (int i = 0; i < elements.size(); i++) {
            computation.append(resultVar).append("[").append(i).append(".i32]")
                    .append(elementTypeStr).append(SPACE)
                    .append(ASSIGN).append(elementTypeStr).append(SPACE)
                    .append(elementCodes.get(i)).append(END_STMT);
        }

        return new OllirExprResult(resultVar, computation.toString());
    }

    private OllirExprResult visitParenthesis(JmmNode node, Void unused) {
        // Simply visit the inner expression
        return visit(node.getChildren().get(0));
    }

    private String mapOperator(String javaOp) {
        switch (javaOp) {
            case "+": return "+";
            case "-": return "-";
            case "*": return "*";
            case "/": return "/";
            case "<": return "<";
            case "&&": return "&&";
            default: return javaOp; // Use as is for others
        }
    }

    private Type resolveVariableType(String varName) {
        // Check current method parameters
        if (currentMethod != null) {
            for (Symbol param : table.getParameters(currentMethod)) {
                if (param.getName().equals(varName)) {
                    return param.getType();
                }
            }

            // Check local variables
            for (Symbol local : table.getLocalVariables(currentMethod)) {
                if (local.getName().equals(varName)) {
                    return local.getType();
                }
            }
        }

        // Check fields
        for (Symbol field : table.getFields()) {
            if (field.getName().equals(varName)) {
                return field.getType();
            }
        }

        // Default to int if not found (should not happen after semantic analysis)
        return new Type("int", false);
    }

    private String generateTemp() {
        return "t" + (tempCounter++);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();

        for (var child : node.getChildren()) {
            OllirExprResult childResult = visit(child);
            computation.append(childResult.getComputation());
        }

        return new OllirExprResult("", computation.toString());
    }
}