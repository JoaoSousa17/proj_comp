package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;

public class ConstantFoldingVisitor extends AJmmVisitor<Boolean, Boolean> {

    public ConstantFoldingVisitor() {
        setDefaultVisit(this::visitDefault);
    }

    @Override
    protected void buildVisitor() {
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("UnaryOp", this::visitUnaryOp);
    }

    private Boolean visitBinaryOp(JmmNode node, Boolean dummy) {
        boolean childrenChanged = false;
        for (var child : node.getChildren()) {
            childrenChanged |= visit(child);
        }

        List<JmmNode> children = node.getChildren();
        if (children.size() < 2) return childrenChanged;

        JmmNode left = children.get(0);
        JmmNode right = children.get(1);
        String op = node.get("op");

        if (isLiteral(left) && isLiteral(right)) {
            return evaluateLiterals(node, left, right, op);
        }

        return childrenChanged;
    }

    private Boolean evaluateLiterals(JmmNode node, JmmNode left, JmmNode right, String op) {
        try {
            if (isIntLiteral(left) && isIntLiteral(right)) {
                int l = Integer.parseInt(left.get("value"));
                int r = Integer.parseInt(right.get("value"));

                return switch (op) {
                    case "+" -> { replaceWithInteger(node, l + r); yield true; }
                    case "-" -> { replaceWithInteger(node, l - r); yield true; }
                    case "*" -> { replaceWithInteger(node, l * r); yield true; }
                    case "/" -> {
                        if (r == 0) yield false;
                        replaceWithInteger(node, l / r); yield true;
                    }
                    case "<" -> { replaceWithBoolean(node, l < r); yield true; }
                    case ">" -> { replaceWithBoolean(node, l > r); yield true; }
                    case "<=" -> { replaceWithBoolean(node, l <= r); yield true; }
                    case ">=" -> { replaceWithBoolean(node, l >= r); yield true; }
                    case "==" -> { replaceWithBoolean(node, l == r); yield true; }
                    case "!=" -> { replaceWithBoolean(node, l != r); yield true; }
                    default -> false;
                };
            } else if (isBoolLiteral(left) && isBoolLiteral(right)) {
                boolean l = Boolean.parseBoolean(left.get("value"));
                boolean r = Boolean.parseBoolean(right.get("value"));

                return switch (op) {
                    case "&&" -> { replaceWithBoolean(node, l && r); yield true; }
                    case "||" -> { replaceWithBoolean(node, l || r); yield true; }
                    case "==" -> { replaceWithBoolean(node, l == r); yield true; }
                    case "!=" -> { replaceWithBoolean(node, l != r); yield true; }
                    default -> false;
                };
            }
        } catch (Exception e) {
            System.err.println("Erro ao avaliar literais: " + e.getMessage());
        }

        return false;
    }

    private Boolean visitUnaryOp(JmmNode node, Boolean dummy) {
        boolean childrenChanged = false;

        if (node.getChildren().isEmpty()) return false;

        JmmNode child = node.getChildren().get(0);
        childrenChanged = visit(child);

        String op = node.get("op");

        try {
            if (op.equals("-") && isIntLiteral(child)) {
                int val = Integer.parseInt(child.get("value"));
                replaceWithInteger(node, -val);
                return true;
            } else if (op.equals("!") && isBoolLiteral(child)) {
                boolean val = Boolean.parseBoolean(child.get("value"));
                replaceWithBoolean(node, !val);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Erro ao aplicar unary folding: " + e.getMessage());
        }

        return childrenChanged;
    }

    private Boolean visitDefault(JmmNode node, Boolean dummy) {
        boolean changed = false;
        for (var child : node.getChildren()) {
            changed |= visit(child);
        }
        return changed;
    }

    // --- Métodos auxiliares ---

    private boolean isLiteral(JmmNode node) {
        return isIntLiteral(node) || isBoolLiteral(node);
    }

    private boolean isIntLiteral(JmmNode node) {
        return node.getKind().equals("Integer");
    }

    private boolean isBoolLiteral(JmmNode node) {
        return node.getKind().equals("Boolean");
    }

    private void replaceWithInteger(JmmNode node, int value) {
        node.put("kind", "Integer");
        node.put("value", String.valueOf(value));
        node.getChildren().clear();
    }

    private void replaceWithBoolean(JmmNode node, boolean value) {
        node.put("kind", "Boolean");
        node.put("value", String.valueOf(value));
        node.getChildren().clear();
    }
}
