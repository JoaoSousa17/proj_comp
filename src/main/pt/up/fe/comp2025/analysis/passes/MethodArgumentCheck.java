package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.List;

public class MethodArgumentCheck extends AnalysisVisitor {

    private TypeUtils typeUtils;
    private SymbolTable symbolTable;

    @Override
    public void buildVisitor() {
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.METHOD_CALL, this::visitMethodCall);
    }

    private Void visitClassDecl(JmmNode classNode, SymbolTable table) {
        this.symbolTable = table;
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        this.typeUtils = new TypeUtils(table);
        typeUtils.setCurrentMethod(method.get("methodName"));
        return null;
    }

    private Void visitMethodCall(JmmNode methodCall, SymbolTable table) {
        if (typeUtils == null) return null;

        String methodName = methodCall.get("value");
        List<JmmNode> arguments = methodCall.getChildren().stream()
                .filter(child -> child.getKind().equals("Argument"))
                .toList();

        // Skip calls to imported classes
        if (methodName.contains(".")) {
            return null;
        }

        // Find method in symbol table
        if (!symbolTable.getMethods().contains(methodName)) {
            return null; // Undeclared method is handled by another pass
        }

        List<Symbol> parameters = symbolTable.getParameters(methodName);

        // Handle varargs case
        boolean isVarargs = !parameters.isEmpty() &&
                parameters.get(parameters.size() - 1).getType().isArray() &&
                parameters.get(parameters.size() - 1).getType().getName().equals("int");

        // Check argument count (special handling for varargs)
        if (!isVarargs && arguments.size() != parameters.size()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    methodCall.getLine(),
                    methodCall.getColumn(),
                    "Method '" + methodName + "' expects " + parameters.size() +
                            " arguments but got " + arguments.size(),
                    null));
            return null;
        }

        // For varargs, need at least (params.size() - 1) arguments
        if (isVarargs && arguments.size() < parameters.size() - 1) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    methodCall.getLine(),
                    methodCall.getColumn(),
                    "Method '" + methodName + "' with varargs expects at least " +
                            (parameters.size() - 1) + " arguments but got " + arguments.size(),
                    null));
            return null;
        }

        // Check each argument type
        for (int i = 0; i < arguments.size(); i++) {
            Type expectedType;

            // For varargs, all extra arguments must match the vararg type
            if (isVarargs && i >= parameters.size() - 1) {
                expectedType = new Type("int", false); // Varargs are passed as individual ints
            } else {
                expectedType = parameters.get(i).getType();
            }

            Type actualType = typeUtils.getExprType(arguments.get(i));

            if (!areTypesCompatible(expectedType, actualType)) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        methodCall.getLine(),
                        methodCall.getColumn(),
                        "Incompatible argument type: expected '" + formatType(expectedType) +
                                "' but got '" + formatType(actualType) + "' for parameter " + (i + 1),
                        null));
            }
        }

        return null;
    }

    private boolean areTypesCompatible(Type expected, Type actual) {
        // Special case for varargs - accept individual ints or int[]
        if (expected.isArray() && expected.getName().equals("int")) {
            return actual.getName().equals("int") && !actual.isArray(); // Accept int for varargs
        }

        // Primitive types must match exactly
        if (isPrimitive(expected) || isPrimitive(actual)) {
            return expected.equals(actual);
        }

        // For objects, allow subclass to superclass assignment
        return expected.equals(actual) ||
                (actual.getName().equals(symbolTable.getClassName()) &&
                        expected.getName().equals(symbolTable.getSuper()));
    }

    private boolean isPrimitive(Type type) {
        return type.getName().equals("int") ||
                type.getName().equals("boolean") ||
                type.getName().equals("void");
    }

    private String formatType(Type type) {
        return type.getName() + (type.isArray() ? "[]" : "");
    }
}