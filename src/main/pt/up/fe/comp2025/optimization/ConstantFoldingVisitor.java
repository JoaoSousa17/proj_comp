package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;

import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ConstantFoldingVisitor extends AJmmVisitor<Void, Boolean> {

    // Using a Logger instead of System.out.println for debugging
    private static final Logger logger = Logger.getLogger(ConstantFoldingVisitor.class.getName());
    private int optimizationCounter = 0;
    private boolean debugMode = false;

    // Store operations in a map for better extensibility
    private final Map<String, BiFunction<Integer, Integer, Integer>> operationMap;

    public ConstantFoldingVisitor() {
        // Initialize operations map
        operationMap = new HashMap<>();
        operationMap.put("+", (a, b) -> a + b);
        operationMap.put("-", (a, b) -> a - b);
        operationMap.put("*", (a, b) -> a * b);
        operationMap.put("/", (a, b) -> b != 0 ? a / b : null);
        operationMap.put("%", (a, b) -> b != 0 ? a % b : null);
    }

    public void enableDebug(boolean enable) {
        this.debugMode = enable;
    }

    @Override
    protected void buildVisitor() {
        // Register node visit methods
        addVisit("BinaryOp", this::processBinaryOperation);
        addVisit("UnaryOp", this::processUnaryOperation);

        // Default visit for all other nodes
        setDefaultVisit(this::processDefaultNode);
    }

    private Boolean processDefaultNode(JmmNode node, Void unused) {


        // Process all children
        boolean modified = false;
        for (JmmNode child : node.getChildren()) {
            boolean childModified = visit(child);
            modified = modified || childModified;
        }

        return modified;
    }

    private Boolean processBinaryOperation(JmmNode node, Void unused) {


        // First process children recursively
        boolean modified = false;
        for (JmmNode child : node.getChildren()) {
            boolean childModified = visit(child);
            modified = modified || childModified;
        }

        // Check if we can fold this operation
        if (node.getNumChildren() != 2) {

            return modified;
        }

        JmmNode leftOperand = node.getChild(0);
        JmmNode rightOperand = node.getChild(1);
        String operator = node.get("op");

        if (canBeEvaluated(leftOperand) && canBeEvaluated(rightOperand)) {
            Integer leftValue = extractIntegerValue(leftOperand);
            Integer rightValue = extractIntegerValue(rightOperand);

            // Try to evaluate
            Integer result = evaluateOperation(leftValue, rightValue, operator);
            if (result != null) {
                // Create a new literal node with the folded result
                JmmNode foldedNode = createIntegerLiteralNode(result);
                node.replace(foldedNode);
                optimizationCounter++;

                return true;
            }
        }

        return modified;
    }

    private Boolean processUnaryOperation(JmmNode node, Void unused) {


        // Process the operand first
        boolean modified = false;
        if (node.getNumChildren() > 0) {
            boolean childModified = visit(node.getChild(0));
            modified = modified || childModified;
        } else {

            return false;
        }

        // Check if we can evaluate this unary operation
        String operator = node.get("op");
        JmmNode operand = node.getChild(0);

        if (operator.equals("-") && canBeEvaluated(operand)) {
            Integer value = extractIntegerValue(operand);
            if (value != null) {
                JmmNode foldedNode = createIntegerLiteralNode(-value);
                node.replace(foldedNode);
                optimizationCounter++;

                return true;
            }
        }

        return modified;
    }

    private boolean canBeEvaluated(JmmNode node) {
        return node != null && node.getKind().equals("Integer") && node.hasAttribute("value");
    }

    private Integer extractIntegerValue(JmmNode node) {
        if (!canBeEvaluated(node)) {
            return null;
        }

        try {
            return Integer.parseInt(node.get("value"));
        } catch (NumberFormatException e) {

            return null;
        }
    }

    private Integer evaluateOperation(Integer left, Integer right, String operator) {
        if (left == null || right == null) {
            return null;
        }

        BiFunction<Integer, Integer, Integer> operation = operationMap.get(operator);
        if (operation == null) {

            return null;
        }

        try {
            return operation.apply(left, right);
        } catch (ArithmeticException e) {

            return null;
        }
    }

    private JmmNode createIntegerLiteralNode(Integer value) {
        JmmNode literalNode = new pt.up.fe.comp.jmm.ast.JmmNodeImpl(Collections.singletonList("Integer"));
        literalNode.put("value", value.toString());
        return literalNode;
    }

    private void logDebug(String message) {
        if (debugMode) {
            logger.log(Level.INFO, message);
        }
    }

    /**
     * Returns the number of constant folding optimizations performed.
     * @return count of successful optimizations
     */
    public int getOptimizationCount() {
        return optimizationCounter;
    }
}