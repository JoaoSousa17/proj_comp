package pt.up.fe.comp2025.optimization;

/**
 * Class representing the result of generating OLLIR code from a JmmNode that is an expression.
 * It contains the OLLIR code for the expression and the computation steps needed to get that code.
 */
public class OllirExprResult {

    public static final OllirExprResult EMPTY = new OllirExprResult("", "");

    private final String computation;
    private final String code;

    public OllirExprResult(String code, String computation) {
        this.code = code;
        this.computation = computation;
    }

    public OllirExprResult(String code) {
        this(code, "");
    }

    public OllirExprResult(String code, StringBuilder computation) {
        this(code, computation.toString());
    }

    public String getComputation() {
        return computation;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "OllirExprResult{" +
                "computation='" + computation + '\'' +
                ", code='" + code + '\'' +
                '}';
    }
}