import java.util.List;

public class Regex {

    // The internal expression, if this is a simple regular expression
    private Character exp;

    // The list of subexpressions, if this is a regular expression operation
    private List<RegularExp> internalExpressions;

    // The operation of the primary regular expression
    private Operator operation;
    private enum Operator {
        CHAR, EMPTY, NULL, UNION, CONCAT, STAR
    }

    public Regex(String expression) {
        // Check for CHAR, EMPTY, or NULL
        if (expression.charAt(0) != '(') {
            if (expression.matches("\\w")) {
                operation = Operator.CHAR;
                exp = expression.charAt(0);
            }
        }
    }
}
