package pt.up.fe.comp2025.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.type.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.HashMap;
import java.util.Map;

public class JasminUtils {

    private final OllirResult ollirResult;

    public JasminUtils(OllirResult ollirResult) {
        // Can be useful to have if you expand this class with more methods
        this.ollirResult = ollirResult;
    }


    public String getModifier(AccessModifier accessModifier) {
        return accessModifier != AccessModifier.DEFAULT ?
                accessModifier.name().toLowerCase() + " " :
                "";
    }

    public String getJasminType(Type type) {
    // Handle array types first
    if (type instanceof ArrayType) {
        ArrayType arrayType = (ArrayType) type;
        Type elementType = arrayType.getElementType();
        return "[" + getJasminType(elementType);
    }
    
    // Handle class types
    if (type instanceof ClassType) {
        ClassType classType = (ClassType) type;
        return "L" + classType.getName() + ";";
    }
    
    // Handle primitive types by checking the type's string representation
    String typeStr = type.toString();
    return switch (typeStr) {
        case "INT32" -> "I";
        case "BOOLEAN" -> "I";  // Boolean is stored as int in JVM
        case "VOID" -> "V";
        case "STRING" -> "Ljava/lang/String;";
        default -> {
            // Try to handle by class name as a fallback
            String className = type.getClass().getSimpleName();
            yield switch (className) {
                case "INT32Type" -> "I";
                case "BOOLEANType" -> "I";  // Boolean is stored as int in JVM
                case "VoidType" -> "V";
                case "StringType" -> "Ljava/lang/String;";
                default -> throw new NotImplementedException("Type not implemented: " + typeStr + " (class: " + className + ")");
            };
        }
    };
}
}
