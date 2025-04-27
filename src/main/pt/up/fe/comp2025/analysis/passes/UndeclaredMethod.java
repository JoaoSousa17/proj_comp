package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;

import java.util.List;

/**
 * Checks if a method call is to a declared method (either in the current class or its superclass).
 */
public class UndeclaredMethod extends AnalysisVisitor {

    private String currentMethod;
    private String className;

    @Override
    public void buildVisitor() {
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit("MethodCall", this::visitMethodCall); // Changed to use string literal
    }

    private Void visitClassDecl(JmmNode classNode, SymbolTable table) {
        className = table.getClassName();
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("methodName");
        return null;
    }

    private Void visitMethodCall(JmmNode methodCall, SymbolTable table) {
        // Skip if we're in a static context (like main method)
        if (currentMethod == null) {
            return null;
        }

        // Get the method name from the "value" attribute
        String methodName = methodCall.get("value");
        if (methodName == null) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    methodCall.getLine(),
                    methodCall.getColumn(),
                    "Method call is missing method name",
                    null)
            );
            return null;
        }

        // Check if this is a method call on an object (has an Identifier child)
        boolean isInstanceCall = false;
        String objectName = null;
        for (JmmNode child : methodCall.getChildren()) {
            if (child.getKind().equals("Identifier")) {
                isInstanceCall = true;
                objectName = child.get("value");
                break;
            }
        }

        if (isInstanceCall) {
            // It's a call to object.method()
            if (objectName.equals("this")) {
                // It's a call to this.method(), check if method exists in current class or superclass
                if (!table.getMethods().contains(methodName) &&
                        (table.getSuper() == null || !isMethodInSuperClass(methodName, table))) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            methodCall.getLine(),
                            methodCall.getColumn(),
                            "Method '" + methodName + "' is not declared in class '" + className +
                                    "' or its superclass",
                            null)
                    );
                }
            } else {
                // It's a call to someObject.method()
                // Check if someObject is a variable of this class type
                boolean isThisClassObject = false;

                // Check parameters
                for (var param : table.getParameters(currentMethod)) {
                    if (param.getName().equals(objectName) &&
                            param.getType().getName().equals(className)) {
                        isThisClassObject = true;
                        break;
                    }
                }

                // Check local variables
                if (!isThisClassObject) {
                    for (var local : table.getLocalVariables(currentMethod)) {
                        if (local.getName().equals(objectName) &&
                                local.getType().getName().equals(className)) {
                            isThisClassObject = true;
                            break;
                        }
                    }
                }

                // Check fields
                if (!isThisClassObject) {
                    for (var field : table.getFields()) {
                        if (field.getName().equals(objectName) &&
                                field.getType().getName().equals(className)) {
                            isThisClassObject = true;
                            break;
                        }
                    }
                }

                if (isThisClassObject) {
                    // It's a call to an object of this class type
                    if (!table.getMethods().contains(methodName) &&
                            (table.getSuper() == null || !isMethodInSuperClass(methodName, table))) {
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                methodCall.getLine(),
                                methodCall.getColumn(),
                                "Method '" + methodName + "' is not declared in class '" + className +
                                        "' or its superclass",
                                null)
                        );
                    }
                }
                // Else assume it's an imported class and skip verification
            }
        } else {
            // It's a direct method call (not on an object), check if it's a static method in this class
            if (!table.getMethods().contains(methodName)) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        methodCall.getLine(),
                        methodCall.getColumn(),
                        "Method '" + methodName + "' is not declared in class '" + className + "'",
                        null)
                );
            }
        }

        return null;
    }

    private boolean isMethodInSuperClass(String methodName, SymbolTable table) {
        //  just check if the superclass exists
        return table.getSuper() != null;
    }
}