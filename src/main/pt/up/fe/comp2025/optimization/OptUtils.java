package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.collections.AccumulatorMap;

/**
 * Utility methods related to the optimization middle-end.
 */
public class OptUtils {
    private final AccumulatorMap<String> temporaries;
    private final TypeUtils types;

    public OptUtils(TypeUtils types) {
        this.types = types;
        this.temporaries = new AccumulatorMap<>();
    }

    public String nextTemp() {
        return nextTemp("tmp");
    }

    public String nextTemp(String prefix) {
        // Subtract 1 because the base is 1
        var nextTempNum = temporaries.add(prefix) - 1;
        return prefix + nextTempNum;
    }

    public String toOllirType(JmmNode typeNode) {
        if (typeNode == null) {
            throw new IllegalArgumentException("Type node cannot be null");
        }

        return toOllirType(types.convertType(typeNode));
    }

    public String toOllirType(Type type) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }

        StringBuilder sb = new StringBuilder(".");

        // Handle array types
        if (type.isArray()) {
            sb.append("array.");
        }

        // Map Java type names to OLLIR type names
        sb.append(mapTypeName(type.getName()));

        return sb.toString();
    }

    private String mapTypeName(String typeName) {
        switch (typeName) {
            case "int":
                return "i32";
            case "boolean":
                return "bool";
            case "void":
                return "V";
            case "String":
                return "String";
            default:
                // Assume it's a class name
                return typeName;
        }
    }
}