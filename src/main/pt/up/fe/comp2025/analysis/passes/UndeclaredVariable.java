package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;

import java.util.Set;

/**
 * Checks if a variable reference is declared (as a parameter, local variable, or field).
 */
public class UndeclaredVariable extends AnalysisVisitor {

    private String currentMethod;

    // ✅ Add a set of known external utility/static classes
    private static final Set<String> EXTERNAL_CLASSES = Set.of("io","ioPlus");

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IDENTIFIER, this::visitIdentifier); // Handle Identifier nodes
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("methodName");
        return null;
    }

    private Void visitIdentifier(JmmNode identifier, SymbolTable table) {
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

        String varRefName = identifier.get("value");
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

        // ✅ Skip check if this is a known external class like "io"
        if (EXTERNAL_CLASSES.contains(varRefName)) {
            return null;
        }



        boolean isParameter = table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varRefName));
        boolean isLocalVariable = table.getLocalVariables(currentMethod).stream()
                .anyMatch(varDecl -> varDecl.getName().equals(varRefName));
        boolean isField = table.getFields().stream()
                .anyMatch(field -> field.getName().equals(varRefName));

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
