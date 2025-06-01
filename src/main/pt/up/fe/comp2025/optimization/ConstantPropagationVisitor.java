package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.*;

public class ConstantPropagationVisitor extends AJmmVisitor<Map<String, String>, Boolean> {
    private final List<Report> reports = new ArrayList<>();
    boolean changed = false;
    private int debugIndent = 0;

    @Override
    protected void buildVisitor() {
        addVisit("MethodDecl", this::visitMethodDecl);
        addVisit("VarDecl", this::visitVarDecl);
        addVisit("AssignStmt", this::visitAssignStmt);
        addVisit("WhileStmt", this::visitWhileStmt);
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("Identifier", this::visitIdentifier);
        setDefaultVisit(this::defaultVisit);
    }

    private void debug(String message) {
        System.out.println("  ".repeat(debugIndent) + "[DEBUG] " + message);
    }

    public List<Report> getReports() {
        return reports;
    }

    private Boolean visitMethodDecl(JmmNode node, Map<String, String> constants) {


        constants.clear();
        Boolean result = defaultVisit(node, constants);


        return result;
    }

    private Boolean visitVarDecl(JmmNode node, Map<String, String> constants) {
        // Just visit children, don't try to handle initialization here
        // Initialization is handled by AssignStmt


        Boolean result = defaultVisit(node, constants); // Visit type node if present

        return result; // VarDecl itself doesn't change constant status directly
    }

    private Boolean visitAssignStmt(JmmNode node, Map<String, String> constants) {



        String varName = null;
        JmmNode rhs = null;

        // Standard case: AssignStmt has two children (lhs: Identifier, rhs: expression)
        if (node.getNumChildren() >= 2) {
            JmmNode lhs = node.getChild(0);
            rhs = node.getChild(1);
            if (lhs.getKind().equals("Identifier")) {
                varName = lhs.get("value");

            }
        }

        // Single-child case: AssignStmt has one child (rhs), infer varName from context
        if (varName == null && node.getNumChildren() == 1) {
            rhs = node.getChild(0);


            // Check if the rhs is a BinaryOp (e.g., i = i + 1)
            if (rhs.getKind().equals("BinaryOp") && rhs.getNumChildren() >= 1) {
                JmmNode leftChild = rhs.getChild(0);
                if (leftChild.getKind().equals("Identifier")) {
                    varName = leftChild.get("value");

                }
            }

            // Fallback: Infer from VarDecl order in MethodDecl
            if (varName == null) {
                JmmNode methodDecl = node;
                while (methodDecl != null && !methodDecl.getKind().equals("MethodDecl")) {
                    methodDecl = methodDecl.getParent();
                }

                if (methodDecl != null) {
                    List<JmmNode> varDecls = new ArrayList<>();
                    List<JmmNode> assignStmts = new ArrayList<>();

                    // Collect VarDecl and AssignStmt nodes at MethodDecl level
                    for (JmmNode child : methodDecl.getChildren()) {
                        if (child.getKind().equals("VarDecl")) {
                            varDecls.add(child);
                        } else if (child.getKind().equals("AssignStmt")) {
                            assignStmts.add(child);
                        }
                    }

                    // Find the index of the current AssignStmt
                    int assignIndex = assignStmts.indexOf(node);
                    if (assignIndex >= 0 && assignIndex < varDecls.size()) {
                        JmmNode varDecl = varDecls.get(assignIndex);
                        varName = varDecl.get("varName");

                    }
                }
            }
        }

        // Fallback: Check node attributes (var or name)
        if (varName == null) {
            varName = node.getOptional("var").orElse(node.getOptional("name").orElse(null));
            if (varName != null) {

                if (node.getNumChildren() == 1) {
                    rhs = node.getChild(0);
                }
            }
        }

        if (varName == null || rhs == null) {

            return false;
        }

        // Set var attribute for OLLIR code generation
        node.put("var", varName);


        boolean rhsChanged = visit(rhs, constants);

        // If the RHS is not constant or involves the variable itself, remove from constants
        if (!isConstant(rhs) || (rhs.getKind().equals("BinaryOp") && containsIdentifier(rhs, varName))) {
            if (constants.containsKey(varName)) {

                constants.remove(varName);
                changed = true;
            }
        } else if (isConstant(rhs)) {
            String currentValue = constants.get(varName);
            String newValue = rhs.get("value");


            if (!constants.containsKey(varName) || !newValue.equals(currentValue)) {

                constants.put(varName, newValue);
                changed = true;

                return true;
            }
        }


        return rhsChanged;
    }

    // Helper method to check if a node contains an Identifier with a specific value
    private boolean containsIdentifier(JmmNode node, String varName) {
        if (node.getKind().equals("Identifier") && node.get("value").equals(varName)) {
            return true;
        }
        for (JmmNode child : node.getChildren()) {
            if (containsIdentifier(child, varName)) {
                return true;
            }
        }
        return false;
    }

    private Boolean visitWhileStmt(JmmNode node, Map<String, String> constants) {


        JmmNode condition = node.getChild(0);

        boolean conditionChanged = visit(condition, constants);

        Set<String> modifiedVars = new HashSet<>();
        JmmNode body = node.getChild(1);
        collectModifiedVars(body, modifiedVars);

        // Analyze the loop to propagate constants after it
        if (condition.getKind().equals("BinaryOp") && condition.get("op").equals("<")) {
            JmmNode left = condition.getChild(0);
            JmmNode right = condition.getChild(1);
            if (left.getKind().equals("Integer") && right.getKind().equals("Integer")) {
                String loopVar = null;
                for (String var : modifiedVars) {
                    // Check if the loop body increments the variable
                    for (JmmNode stmt : body.getChildren()) {
                        if (stmt.getKind().equals("AssignStmt") && stmt.getChild(0).getKind().equals("BinaryOp")) {
                            JmmNode binOp = stmt.getChild(0);
                            if (binOp.get("op").equals("+") && binOp.getChild(0).getKind().equals("Identifier") &&
                                    binOp.getChild(0).get("value").equals(var) && binOp.getChild(1).getKind().equals("Integer") &&
                                    binOp.getChild(1).get("value").equals("1")) {
                                loopVar = var;
                                break;
                            }
                        }
                    }
                }
                if (loopVar != null && right.getKind().equals("Integer")) {
                    // After the loop, loopVar equals the right operand
                    constants.put(loopVar, right.get("value"));

                    changed = true;
                }
            }
        }

        for (String var : modifiedVars) {
            if (constants.containsKey(var)) {

                constants.remove(var);
                changed = true;
            }
        }


        boolean bodyChanged = visit(body, constants);


        return conditionChanged || bodyChanged;
    }

    // Helper method to collect variables modified in a node (e.g., BlockStmt)
    private void collectModifiedVars(JmmNode node, Set<String> modifiedVars) {
        if (node.getKind().equals("AssignStmt")) {
            if (node.getNumChildren() >= 2 && node.getChild(0).getKind().equals("Identifier")) {
                modifiedVars.add(node.getChild(0).get("value"));
            } else if (node.getNumChildren() == 1 && node.getChild(0).getKind().equals("BinaryOp")) {
                JmmNode leftChild = node.getChild(0).getChild(0);
                if (leftChild.getKind().equals("Identifier")) {
                    modifiedVars.add(leftChild.get("value"));
                }
            }
        }
        for (JmmNode child : node.getChildren()) {
            collectModifiedVars(child, modifiedVars);
        }
    }

    private Boolean visitBinaryOp(JmmNode node, Map<String, String> constants) {


        boolean leftChanged = visit(node.getChild(0), constants);
        boolean rightChanged = visit(node.getChild(1), constants);

        return leftChanged || rightChanged;
    }

    private Boolean visitIdentifier(JmmNode node, Map<String, String> constants) {
        String varName = node.get("value");


        if (constants.containsKey(varName)) {
            String value = constants.get(varName);

            // Create a new Integer node instead of modifying the existing node
            JmmNode newNode = new pt.up.fe.comp.jmm.ast.JmmNodeImpl(Collections.singletonList("Integer"));
            newNode.put("value", value);
            node.replace(newNode); // Replace the Identifier with the new Integer node
            changed = true;

            return true;
        }

        return false;
    }

    private boolean isConstant(JmmNode node) {
        boolean isConst = node.getKind().equals("Integer") || node.getKind().equals("Boolean");

        return isConst;
    }

    private Boolean defaultVisit(JmmNode node, Map<String, String> constants) {


        boolean localChanged = false;
        for (JmmNode child : node.getChildren()) {
            Boolean childChanged = visit(child, constants);
            if (childChanged != null && childChanged) {
                localChanged = true;
            }
        }

        return localChanged;
    }

}