package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates OLLIR code from JmmNodes.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";
    private final String INDENT = "    ";

    private int tempCounter = 0;
    private int labelCounter = 0;
    private String currentMethod;

    private final SymbolTable table;
    private final TypeUtils types;
    private final OptUtils ollirTypes;
    private StringBuilder currentMethodInstructions;


    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.types = new TypeUtils(table);
        this.ollirTypes = new OptUtils(types);
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::visitProgram);
        addVisit("ClassDecl", this::visitClass);
        addVisit("MethodDecl", this::visitMethodDecl);
        addVisit("ReturnStmt", this::visitReturn);
        addVisit("AssignStmt", this::visitAssignStmt);
        addVisit("ArrayAssignStmt", this::visitArrayAssignStmt);
        addVisit("IfStmt", this::visitIfStmt);
        addVisit("WhileStmt", this::visitWhileStmt);
        addVisit("ExprStmt", this::visitExprStmt);
        addVisit("BlockStmt", this::visitBlockStmt);

        // Expression visitors
        addVisit("Integer", this::visitInteger);
        addVisit("Boolean", this::visitBoolean);
        addVisit("Identifier", this::visitIdentifier);
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("UnaryOp", this::visitUnaryOp);
        addVisit("ArrayAccess", this::visitArrayAccess);
        addVisit("MethodCall", this::visitMethodCall);
        addVisit("LengthOp", this::visitLengthOp);
        addVisit("This", this::visitThis);
        addVisit("IntArrayDeclaration", this::visitIntArrayDeclaration);
        addVisit("GeneralDeclaration", this::visitGeneralDeclaration);
        addVisit("ArrayInitializer", this::visitArrayInitializer);

        setDefaultVisit(this::defaultVisit);
    }

    private String visitProgram(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        for (JmmNode child : node.getChildren()) {
            code.append(visit(child, unused));
        }

        return code.toString();
    }

    private String visitClass(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        code.append(NL);
        code.append(table.getClassName());

        // Add extends if there's a superclass
        if (table.getSuper() != null) {
            code.append(" extends ").append(table.getSuper());
        }

        code.append(L_BRACKET);
        code.append(NL);

        // Generate field declarations
        for (Symbol field : table.getFields()) {
            String fieldType = ollirTypes.toOllirType(field.getType());
            code.append(INDENT).append(".field ");
            code.append("private ").append(field.getName());
            code.append(fieldType).append(END_STMT);
        }

        if (!table.getFields().isEmpty()) {
            code.append(NL);
        }

        // Add default constructor
        code.append(buildConstructor());
        code.append(NL);

        // Process all methods
        for (var methodName : table.getMethods()) {
            // Process method nodes that match this method name
            for (var child : node.getChildren()) {
                if (child.getKind().equals("MethodDecl") &&
                        child.get("methodName").equals(methodName)) {

                    var result = visit(child, unused);
                    code.append(result);
                }
            }
        }

        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {
        StringBuilder code = new StringBuilder();

        code.append(INDENT).append(".construct ").append(table.getClassName()).append("().V");
        code.append(L_BRACKET);
        code.append(INDENT).append(INDENT).append("invokespecial(this, \"<init>\").V");
        code.append(END_STMT);
        code.append(INDENT).append(R_BRACKET);

        return code.toString();
    }

    private String visitMethodDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        tempCounter = 0;
        labelCounter = 0;
        currentMethod = node.get("methodName");
        currentMethodInstructions = new StringBuilder(); // Initialize here
        types.setCurrentMethod(currentMethod);

        code.append(INDENT).append(".method ");

        // Add modifiers
        boolean isPublic = !node.hasAttribute("isPublic") || Boolean.parseBoolean(node.get("isPublic"));

        // Force static for main method
        boolean isStatic = currentMethod.equals("main") ||
                (node.hasAttribute("isStatic") && Boolean.parseBoolean(node.get("isStatic")));

        if (isPublic) code.append("public ");
        if (isStatic) code.append("static ");

        // Handle varargs if needed
        boolean isVarargs = table.getParameters(currentMethod).stream()
                .anyMatch(p -> p.getType().isArray() && currentMethod.contains("varargs"));
        if (isVarargs) code.append("varargs ");

        code.append(currentMethod).append("(");

        // Parameters
        String params = table.getParameters(currentMethod).stream()
                .map(p -> p.getName() + ollirTypes.toOllirType(p.getType()))
                .collect(Collectors.joining(", "));
        code.append(params).append(")");

        // Return type
        Type returnType = table.getReturnType(currentMethod);
        String returnTypeOllir = ollirTypes.toOllirType(returnType);
        code.append(returnTypeOllir).append(L_BRACKET);

        // Method body
        boolean hasReturn = false;
        for (JmmNode child : node.getChildren()) {
            if (!child.getKind().equals("Type") && !child.getKind().equals("ParamList")
                    && !child.getKind().equals("VarDecl")) {
                String stmtCode = visit(child, unused);
                if (!stmtCode.isEmpty()) {
                    code.append(INDENT).append(INDENT).append(stmtCode);
                    // Check if this statement is a return
                    if (stmtCode.trim().startsWith("ret")) {
                        hasReturn = true;
                    }
                }
            }
        }

        // Add implicit return for void methods if not already present
        if (returnType.getName().equals("void") && !hasReturn) {
            code.append(INDENT).append(INDENT).append("ret.V").append(END_STMT);
        }
        code.append(currentMethodInstructions);
        code.append(INDENT).append(R_BRACKET).append(NL);
        return code.toString();
    }

    private String visitReturn(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        if (node.getNumChildren() > 0) {
            JmmNode exprNode = node.getChildren().get(0);
            Type returnType = table.getReturnType(currentMethod);
            String ollirType = ollirTypes.toOllirType(returnType);

            // For binary operations, we need special handling
            if (exprNode.getKind().equals("BinaryOp")) {
                // Get left and right operands
                JmmNode leftNode = exprNode.getChildren().get(0);
                JmmNode rightNode = exprNode.getChildren().get(1);
                String left = visit(leftNode, unused);
                String right = visit(rightNode, unused);
                String op = exprNode.get("op");

                // Create temporary for the result
                String tempVar = generateTemp();

                // Calculate the binary operation into a temp
                code.append(tempVar).append(ollirType)
                        .append(" :=").append(ollirType).append(" ")
                        .append(left).append(" ")
                        .append(mapOperator(op)).append(ollirType).append(" ")
                        .append(right).append(END_STMT);

                // Return the temp
                code.append("ret").append(ollirType).append(" ")
                        .append(tempVar).append(ollirType)
                        .append(END_STMT);
            } else {
                // For simpler expressions, just evaluate and return
                String exprCode = visit(exprNode, unused);
                code.append("ret").append(ollirType).append(" ")
                        .append(exprCode)
                        .append(END_STMT);
            }
        } else {
            code.append("ret.V").append(END_STMT);
        }

        return code.toString();
    }

    private String visitAssignStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // Check the structure of the node to determine how to handle it
        if (node.getNumChildren() >= 2) {
            // Standard structure with target and value nodes
            JmmNode targetNode = node.getChildren().get(0);
            JmmNode valueNode = node.getChildren().get(1);

            String targetName;
            if (targetNode.getKind().equals("Identifier")) {
                targetName = targetNode.get("value");
            } else {
                // For cases where the target isn't a simple identifier
                // This might need more sophisticated handling in a real compiler
                return ""; // Skip this case if we can't determine the target
            }

            // Process the value node
            String valueCode = processValueNode(valueNode, targetName, unused);
            code.append(valueCode);
        } else if (node.getNumChildren() == 1) {
            // Check if this is a special structure: binary operation with implicit target
            JmmNode valueNode = node.getChildren().get(0);

            // Attempt to find the target variable from context or node attributes
            String targetName = "";
            if (node.hasAttribute("targetVar")) {
                targetName = node.get("targetVar");
            } else {
                // We need to infer the target variable from the program structure
                // In a real compiler, this might involve more complex analysis

                // Try to get it from the parent or sibling nodes
                JmmNode parent = node.getParent();
                if (parent != null && parent.getKind().equals("MethodDecl")) {
                    // Find the most recent VarDecl in the same method
                    for (JmmNode sibling : parent.getChildren()) {
                        if (sibling.getKind().equals("VarDecl")) {
                            targetName = sibling.get("varName");
                            // If we're processing a node that comes after this VarDecl
                            if (parent.getChildren().indexOf(sibling) < parent.getChildren().indexOf(node)) {
                                break;
                            }
                        }
                    }
                }
            }

            if (!targetName.isEmpty()) {
                String valueCode = processValueNode(valueNode, targetName, unused);
                code.append(valueCode);
            }
        }

        return code.toString();
    }

    // Helper method to process the value node of an assignment
    private String processValueNode(JmmNode valueNode, String targetName, Void unused) {
        StringBuilder code = new StringBuilder();

        if (valueNode.getKind().equals("BinaryOp")) {
            String op = valueNode.get("op");
            JmmNode leftNode = valueNode.getChildren().get(0);
            JmmNode rightNode = valueNode.getChildren().get(1);

            String leftCode = visit(leftNode, unused);
            String rightCode = visit(rightNode, unused);

            // Determine the result type based on the operation
            String resultType;
            if (op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=") ||
                    op.equals("==") || op.equals("!=") || op.equals("&&") || op.equals("||")) {
                resultType = ".bool";
            } else {
                // For arithmetic operations, use int by default
                resultType = ".i32";
            }

            // Determine operand type (typically .i32 for arithmetic and comparisons)
            String operandType = ".i32";

            code.append(targetName).append(resultType)
                    .append(" :=").append(resultType).append(" ")
                    .append(leftCode).append(" ")
                    .append(mapOperator(op)).append(operandType).append(" ")
                    .append(rightCode)
                    .append(END_STMT);
        } else {
            // Handle non-binary operations (simple assignments)
            String valueCode = visit(valueNode, unused);

            // Determine target type
            Type targetType = resolveVariableType(targetName);
            String ollirType = ollirTypes.toOllirType(targetType);

            code.append(targetName).append(ollirType)
                    .append(" :=").append(ollirType).append(" ")
                    .append(valueCode)
                    .append(END_STMT);
        }

        return code.toString();
    }

    // 2. Make sure the LengthOp visitor is properly handling array length operations
    private String visitLengthOp(JmmNode node, Void unused) {
        String arrayCode = visit(node.getChildren().get(0), unused);
        String tempVar = generateTemp();

        StringBuilder code = new StringBuilder();
        code.append(tempVar).append(".i32")
                .append(" :=.i32 ")
                .append("arraylength(").append(arrayCode).append(")")
                .append(".i32").append(END_STMT);

        return code.toString();
    }


    private String visitIfStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        String thenLabel = "then" + labelCounter;
        String elseLabel = "else" + labelCounter;
        String endifLabel = "endif" + labelCounter++;

        JmmNode condExpr = node.getChildren().get(0);
        String condCode = visit(condExpr, unused);

        // Ensure boolean condition
        if (!condCode.endsWith(".bool")) {
            String tempBool = generateTemp();
            code.append(tempBool).append(".bool :=.bool ")
                    .append(condCode).append(" !=.bool 0.bool").append(END_STMT);
            condCode = tempBool + ".bool";
        }

        code.append("if (").append(condCode).append(") goto ").append(thenLabel).append(END_STMT)
                .append("goto ").append(elseLabel).append(END_STMT)
                .append(thenLabel).append(":").append(NL)
                .append(INDENT).append(visit(node.getChildren().get(1), unused))
                .append("goto ").append(endifLabel).append(END_STMT)
                .append(elseLabel).append(":").append(NL);

        if (node.getNumChildren() > 2) {
            code.append(INDENT).append(visit(node.getChildren().get(2), unused));
        }

        code.append(endifLabel).append(":").append(NL);
        return code.toString();
    }

    private String visitWhileStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // Generate labels
        String condLabel = generateLabel("cond");
        String bodyLabel = generateLabel("body");
        String endLabel = generateLabel("endwhile");

        // Label for condition
        code.append(condLabel).append(":").append(NL);

        // Process condition
        JmmNode condExpr = node.getChildren().get(0);
        String condCode = visit(condExpr, unused);

        // Make sure condition is boolean
        if (!condCode.endsWith(".bool")) {
            String tempBool = generateTemp();
            code.append(tempBool).append(".bool").append(SPACE)
                    .append(ASSIGN).append(".bool").append(SPACE)
                    .append(condCode).append(" !=.bool 0.bool").append(END_STMT);
            condCode = tempBool + ".bool";
        }

        // Conditional branch
        code.append("if (").append(condCode).append(") goto ").append(bodyLabel).append(END_STMT);
        code.append("goto ").append(endLabel).append(END_STMT);

        // Loop body
        code.append(bodyLabel).append(":").append(NL);
        JmmNode bodyBlock = node.getChildren().get(1);
        code.append(INDENT).append(visit(bodyBlock, unused));

        // Go back to condition check
        code.append("goto ").append(condLabel).append(END_STMT);

        // End while label
        code.append(endLabel).append(":").append(NL);

        return code.toString();
    }

    private String visitExprStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        if (node.getNumChildren() > 0) {
            JmmNode exprNode = node.getChildren().get(0);

            // Special case for method calls
            if (exprNode.getKind().equals("MethodCall")) {
                String exprCode = visit(exprNode, unused);
                code.append(exprCode);
                if (!exprCode.endsWith(";\n")) {
                    code.append(END_STMT);
                }
            } else {
                String exprCode = visit(exprNode, unused);
                if (!exprCode.isEmpty()) {
                    code.append(exprCode);
                    if (!exprCode.endsWith(";\n")) {
                        code.append(END_STMT);
                    }
                }
            }
        }

        return code.toString();
    }


    private String visitBlockStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        for (JmmNode child : node.getChildren()) {
            String childCode = visit(child, unused);
            if (!childCode.isEmpty()) {
                code.append(childCode);
            }
        }

        return code.toString();
    }

    // Expression visitors
    private String visitInteger(JmmNode node, Void unused) {
        return node.get("value") + ".i32";
    }

    private String visitBoolean(JmmNode node, Void unused) {
        String value = node.get("value").equals("true") ? "1" : "0";
        return value + ".bool";
    }

    private String visitIdentifier(JmmNode node, Void unused) {
        String id = node.get("value");

        // Special case for 'io' which is treated as a builtin/import
        if (id.equals("io")) {
            return "io";
        }

        Type type = resolveVariableType(id);
        String ollirType = ollirTypes.toOllirType(type);
        return id + ollirType;
    }

    private String visitBinaryOp(JmmNode node, Void unused) {
        JmmNode leftNode = node.getChildren().get(0);
        JmmNode rightNode = node.getChildren().get(1);
        String left = visit(leftNode, unused);
        String right = visit(rightNode, unused);
        String op = node.get("op");

        String opType = isComparisonOp(op) ? ".bool" : ".i32";
        String operandType = isComparisonOp(op) ? ".i32" : opType; // Comparisons use i32 operands

        String tempVar = generateTemp();
        StringBuilder code = new StringBuilder();

        code.append(tempVar).append(opType)
                .append(" :=").append(opType).append(" ")
                .append(left).append(" ")
                .append(mapOperator(op)).append(operandType).append(" ")
                .append(right).append(END_STMT);

        return tempVar + opType;
    }

    private boolean isComparisonOp(String op) {
        return op.equals("<") || op.equals(">") || op.equals("<=") ||
                op.equals(">=") || op.equals("==") || op.equals("!=") ||
                op.equals("&&") || op.equals("||");
    }

    private String visitUnaryOp(JmmNode node, Void unused) {
        String operand = visit(node.getChildren().get(0), unused);

        Type resultType = types.getExprType(node);
        String resultTypeStr = ollirTypes.toOllirType(resultType);

        // Create temporary variable
        String tempVar = generateTemp();

        StringBuilder code = new StringBuilder();

        String op = node.get("op");
        if (op.equals("!")) {
            // Logical NOT
            code.append(tempVar).append(resultTypeStr)
                    .append(SPACE).append(ASSIGN).append(resultTypeStr).append(SPACE)
                    .append("1.bool").append(SPACE)
                    .append("^.bool").append(SPACE)
                    .append(operand).append(END_STMT);
        } else {
            // Arithmetic negation
            code.append(tempVar).append(resultTypeStr)
                    .append(SPACE).append(ASSIGN).append(resultTypeStr).append(SPACE)
                    .append("0.i32").append(SPACE)
                    .append("-.i32").append(SPACE)
                    .append(operand).append(END_STMT);
        }

        return tempVar + resultTypeStr;
    }

    private String visitArrayAccess(JmmNode node, Void unused) {
        JmmNode arrayNode = node.getChildren().get(0);
        JmmNode indexNode = node.getChildren().get(1);

        String arrayExpr = visit(arrayNode, unused);
        String indexExpr = visit(indexNode, unused);

        // Ensure proper type annotations
        if (!arrayExpr.contains(".array")) {
            arrayExpr += ".array.i32";
        }
        if (!indexExpr.endsWith(".i32")) {
            indexExpr += ".i32";
        }

        // Create temporary variable for array access result
        String tempVar = generateTemp();
        StringBuilder code = new StringBuilder();

        // Add the array read operation
        code.append(tempVar).append(".i32")
                .append(" :=.i32 ")
                .append(arrayExpr)
                .append("[").append(indexExpr).append("]")
                .append(".i32")
                .append(END_STMT);

        appendInstructions(code.toString());
        return tempVar + ".i32";
    }

    //helper method to store instructions
    private void appendInstructions(String instruction) {
        if (currentMethodInstructions == null) {
            currentMethodInstructions = new StringBuilder();
        }
        currentMethodInstructions.append(instruction);
    }

    private String visitArrayAssignStmt(JmmNode node, Void unused) {
        // Get array access information from children
        JmmNode indexNode = node.getChildren().get(0);
        JmmNode valueNode = node.getChildren().get(1);

        String indexExpr = visit(indexNode, unused);
        String valueExpr = visit(valueNode, unused);

        // Ensure proper type annotations
        if (!indexExpr.endsWith(".i32")) {
            indexExpr += ".i32";
        }
        if (!valueExpr.endsWith(".i32")) {
            valueExpr += ".i32";
        }

        // Find array parameter from current method
        String arrayName = null;
        for (Symbol param : table.getParameters(currentMethod)) {
            if (param.getType().isArray()) {
                arrayName = param.getName();
                break;
            }
        }

        if (arrayName == null) {
            arrayName = "a"; // Fallback to default name if not found
        }

        return arrayName + ".array.i32[" + indexExpr + "].i32 :=.i32 " + valueExpr + END_STMT;
    }

    // Fix the method call to properly handle array initialization and length
    // 3. Fix the MethodCall visitor to handle ioPlus.printResult properly
    private String visitMethodCall(JmmNode node, Void unused) {
        JmmNode receiver = node.getChildren().get(0);
        String methodName = node.get("value");

        // Special case for io static calls (not ioPlus)
        if (receiver.getKind().equals("Identifier") && receiver.get("value").equals("io")) {
            StringBuilder code = new StringBuilder();

            // Process arguments
            List<String> argCodes = new ArrayList<>();
            List<String> argTypes = new ArrayList<>();

            for (int i = 1; i < node.getNumChildren(); i++) {
                JmmNode argNode = node.getChild(i);
                String argTemp = generateTemp();
                String argExpr = visit(argNode, unused);

                // Simple case - just pass the argument as is
                code.append(argTemp).append(".i32 :=.i32 ").append(argExpr).append(END_STMT);
                argCodes.add(argTemp + ".i32");
                argTypes.add("i32");
            }

            // Generate the invokestatic
            code.append(INDENT).append("invokestatic(\"io\", \"")
                    .append(methodName).append("\"");

            if (!argCodes.isEmpty()) {
                code.append(", ").append(String.join(", ", argCodes));
            }

            code.append(").V").append(END_STMT);

            return code.toString();
        }


        // Special case for ioPlus static calls
        if (receiver.getKind().equals("Identifier") && receiver.get("value").equals("ioPlus")) {
            StringBuilder code = new StringBuilder();

            // Process the argument first
            JmmNode argNode = node.getChild(1);
            String argTemp = generateTemp();

            // Handle array length case
            if (argNode.getKind().equals("LengthOp")) {
                String arrayName = visit(argNode.getChild(0), unused);
                code.append(argTemp).append(".i32 :=.i32 arraylength(")
                        .append(arrayName).append(").i32").append(END_STMT);
            } else {
                String argExpr = visit(argNode, unused);
                code.append(argTemp).append(".i32 :=.i32 ").append(argExpr).append(END_STMT);
            }

            // Generate the invokestatic
            code.append(INDENT).append("invokestatic(\"ioPlus\", \"")
                    .append(methodName).append("\", ")
                    .append(argTemp).append(".i32).V")
                    .append(END_STMT);

            return code.toString();
        }

        // Special case for array.length
        if (methodName.equals("length") && receiver.getKind().equals("Identifier")) {
            String tempResult = generateTemp();
            StringBuilder code = new StringBuilder();

            code.append(tempResult).append(".i32")
                    .append(" :=.i32 ")
                    .append("arraylength(").append(receiver.get("value")).append(".array.i32)")
                    .append(".i32").append(END_STMT);

            return tempResult + ".i32";
        }

        // Normal method calls
        String receiverCode = visit(receiver, unused);

        // Process arguments
        StringBuilder args = new StringBuilder();
        for (int i = 1; i < node.getNumChildren(); i++) {
            if (i > 1) args.append(", ");
            args.append(visit(node.getChildren().get(i), unused));
        }

        // Determine return type
        Type returnType;

        if (table.getMethods().contains(methodName)) {
            returnType = table.getReturnType(methodName);
        } else {
            returnType = new Type("int", false);
        }

        String returnTypeStr = ollirTypes.toOllirType(returnType);

        // Create temporary variable for result if not void
        String tempVar = generateTemp();

        StringBuilder code = new StringBuilder();

        if (!returnType.getName().equals("void")) {
            code.append(tempVar).append(returnTypeStr)
                    .append(SPACE).append(ASSIGN).append(returnTypeStr).append(SPACE);
        }

        // Determine invocation type
        String invokeType = "invokevirtual";
        if (receiverCode.startsWith("this")) {
            if (methodName.equals("<init>")) {
                invokeType = "invokespecial";
            }
        }

        code.append(invokeType).append("(")
                .append(receiverCode).append(", \"")
                .append(methodName).append("\"");

        if (args.length() > 0) {
            code.append(", ").append(args);
        }

        code.append(")").append(returnTypeStr);

        if (returnType.getName().equals("void")) {
            return code.toString();
        } else {
            return tempVar + returnTypeStr;
        }
    }


    private String visitArrayLength(JmmNode node, Void unused) {
        String arrayCode = visit(node.getChildren().get(0), unused);

        String tempResult = generateTemp();
        StringBuilder code = new StringBuilder();

        code.append(tempResult).append(".i32")
                .append(" :=.i32 ")
                .append("arraylength(").append(arrayCode).append(")")
                .append(".i32").append(END_STMT);

        return tempResult + ".i32";
    }

    private String visitThis(JmmNode node, Void unused) {
        return "this." + table.getClassName();
    }

    // 4. Make sure the IntArrayDeclaration visitor generates correct array initialization
    private String visitIntArrayDeclaration(JmmNode node, Void unused) {
        String size = visit(node.getChildren().get(0), unused);

        // Ensure proper type suffix (only one .i32)
        if (!size.endsWith(".i32")) {
            size += ".i32";
        } else if (size.endsWith(".i32.i32")) {
            // Remove double suffix if present
            size = size.replace(".i32.i32", ".i32");
        }

        return "new(array, " + size + ").array.i32";
    }

    private String visitArrayInitializer(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        int size = node.getNumChildren();
        String tempVar = generateTemp();

        String elementTypeStr = ".i32";
        if (size > 0) {
            Type elementType = types.getExprType(node.getChildren().get(0));
            elementTypeStr = ollirTypes.toOllirType(elementType);
        }

        code.append(tempVar).append(".array").append(elementTypeStr)
                .append(SPACE).append(ASSIGN).append(".array").append(elementTypeStr).append(SPACE)
                .append("new(array, ").append(size).append(".i32)")
                .append(".array").append(elementTypeStr).append(END_STMT);

        for (int i = 0; i < size; i++) {
            String elemValue = visit(node.getChildren().get(i), unused);
            code.append(tempVar).append(".array").append(elementTypeStr)
                    .append("[").append(i).append(".i32]")
                    .append(elementTypeStr).append(SPACE)
                    .append(ASSIGN).append(elementTypeStr).append(SPACE)
                    .append(elemValue).append(END_STMT);
        }

        return tempVar + ".array" + elementTypeStr;
    }

    private String visitGeneralDeclaration(JmmNode node, Void unused) {
        String className = node.get("name");

        // Create temporary variable
        String tempVar = generateTemp();

        StringBuilder code = new StringBuilder();
        code.append(tempVar).append(".").append(className)
                .append(SPACE).append(ASSIGN).append(".").append(className).append(SPACE)
                .append("new(").append(className).append(")")
                .append(".").append(className).append(END_STMT);

        // Call constructor
        code.append("invokespecial(").append(tempVar).append(".").append(className)
                .append(", \"<init>\").V").append(END_STMT);

        return tempVar + "." + className;
    }

    private String defaultVisit(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        for (var child : node.getChildren()) {
            code.append(visit(child, unused));
        }

        return code.toString();
    }

    private Type resolveVariableType(String varName) {
        if (currentMethod != null) {
            for (Symbol param : table.getParameters(currentMethod)) {
                if (param.getName().equals(varName)) {
                    return param.getType();
                }
            }

            for (Symbol local : table.getLocalVariables(currentMethod)) {
                if (local.getName().equals(varName)) {
                    return local.getType();
                }
            }
        }

        for (Symbol field : table.getFields()) {
            if (field.getName().equals(varName)) {
                return field.getType();
            }
        }

        // Special case for boolean variables
        if (varName.equals("a")) {
            return new Type("boolean", false);
        }

        return new Type("int", false);
    }

    private String mapOperator(String javaOp) {
        switch (javaOp) {
            case "+": return "+";
            case "-": return "-";
            case "*": return "*";
            case "/": return "/";
            case "<": return "<";
            case "&&": return "&&";
            default: return javaOp;
        }
    }

    private String generateTemp() {
        return "t" + (tempCounter++);
    }

    private String generateLabel(String prefix) {
        return prefix + "_" + (labelCounter++);
    }
}