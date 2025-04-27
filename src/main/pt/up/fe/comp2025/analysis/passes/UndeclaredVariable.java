package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;

/**
 * Checks if a variable reference is declared (as a parameter, local variable, or field).
 */
public class UndeclaredVariable extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IDENTIFIER, this::visitIdentifier); // Handle Identifier nodes
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        // Set the current method being analyzed
        currentMethod = method.get("methodName"); // Use "methodName" as defined in the grammar
        return null;
    }

    private Void visitIdentifier(JmmNode identifier, SymbolTable table) {
        // Ensure the current method is set
        if (currentMethod == null) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    identifier.getLine(),
                    identifier.getColumn(),
                    "Current method is not set. Cannot check variable references.",
                    null)
            );
            return null;
        }

        // Extract the variable name from the node
        String varRefName = identifier.get("value"); // Use "value" as defined in the grammar for ID nodes
        if (varRefName == null || varRefName.isEmpty()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    identifier.getLine(),
                    identifier.getColumn(),
                    "Variable reference node is missing the 'value' attribute.",
                    null)
            );
            return null;
        }

        // Debug: Print the variable name and current method
        System.out.println("Checking variable '" + varRefName + "' in method '" + currentMethod + "'");

        // Check if the variable is a parameter of the current method
        boolean isParameter = table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varRefName));

        // Check if the variable is a local variable of the current method
        boolean isLocalVariable = table.getLocalVariables(currentMethod).stream()
                .anyMatch(varDecl -> varDecl.getName().equals(varRefName));

        // Check if the variable is a field of the class
        boolean isField = table.getFields().stream()
                .anyMatch(field -> field.getName().equals(varRefName));

        // If the variable is not a parameter, local variable, or field, report an error
        if (!isParameter && !isLocalVariable && !isField) {
            String message = String.format("Variable '%s' is not declared in method '%s'.", varRefName, currentMethod);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    identifier.getLine(),
                    identifier.getColumn(),
                    message,
                    null)
            );
        }

        return null;
    }
}