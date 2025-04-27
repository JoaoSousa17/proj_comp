package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;

import java.util.List;

public class VarargCheck extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        String methodName = method.get("methodName");
        List<Symbol> params = table.getParameters(methodName);
        boolean foundVararg = false;

        for (int i = 0; i < params.size(); i++) {
            Symbol param = params.get(i);
            Type paramType = param.getType();

            // Check if parameter is a vararg (int[])
            if (isVararg(paramType)) {
                if (i != params.size() - 1) { // Vararg must be last
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            method.getLine(),
                            method.getColumn(),
                            "Vararg parameter '" + param.getName() + "' must be the last parameter.",
                            null));
                }
                if (foundVararg) { // Only one vararg allowed
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            method.getLine(),
                            method.getColumn(),
                            "Multiple vararg parameters detected. Only one is allowed.",
                            null));
                }
                foundVararg = true;
            }
        }

        return null;
    }

    // Check if type is int[] (vararg)
    private boolean isVararg(Type type) {
        return type.isArray() && type.getName().equals("int");
    }
}