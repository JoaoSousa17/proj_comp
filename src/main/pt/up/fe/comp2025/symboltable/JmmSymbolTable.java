package pt.up.fe.comp2025.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;

import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

public class JmmSymbolTable extends AJmmSymbolTable implements SymbolTable {

    private final List<String> imports; // List of imported classes
    private final String className; // Name of the declared class
    private final String superClass; // Superclass (if any)
    private final List<Symbol> fields; // Fields of the class
    private final List<String> methods; // Method names
    private final Map<String, Type> returnTypes; // Return types of methods
    private final Map<String, List<Symbol>> params; // Parameters for each method
    private final Map<String, List<Symbol>> locals; // Local variables for each method

    public JmmSymbolTable(List<String> imports,
                          String className,
                          String superClass,
                          List<Symbol> fields,
                          List<String> methods,
                          Map<String, Type> returnTypes,
                          Map<String, List<Symbol>> params,
                          Map<String, List<Symbol>> locals) {
        this.imports = imports;
        this.className = className;
        this.superClass = superClass;
        this.fields = fields;
        this.methods = methods;
        this.returnTypes = returnTypes;
        this.params = params;
        this.locals = locals;
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClass;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    @Override
    public List<String> getMethods() {
        return methods;
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return returnTypes.get(methodSignature);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return params.get(methodSignature);
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return locals.get(methodSignature);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Symbol Table:\n");
        sb.append("  Imports: ").append(imports).append("\n");
        sb.append("  Class: ").append(className).append("\n");
        sb.append("  Super: ").append(superClass).append("\n");
        sb.append("  Fields: ").append(fields).append("\n");
        sb.append("  Methods: ").append(methods).append("\n");
        sb.append("  Return Types: ").append(returnTypes).append("\n");
        sb.append("  Parameters: ").append(params).append("\n");
        sb.append("  Locals: ").append(locals).append("\n");
        return sb.toString();
    }
}