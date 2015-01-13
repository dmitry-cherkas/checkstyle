package com.puppycrawl.tools.checkstyle.checks.indentation;

import com.puppycrawl.tools.checkstyle.api.DetailAST;

/**
 * @author Dmitry_Cherkas
 */
public class LambdaHandler extends BlockParentHandler {

    /**
     * Construct an instance of this handler with the given indentation check,
     * name, abstract syntax tree, and parent handler.
     *
     * @param aIndentCheck the indentation check
     * @param aAst         the abstract syntax tree
     * @param aParent      the parent handler
     */
    public LambdaHandler(IndentationCheck aIndentCheck, DetailAST aAst, ExpressionHandler aParent) {
        super(aIndentCheck, "lambda", aAst, aParent);
    }

    @Override
    public void checkIndentation() {
        System.out.println("LAMBDA HERE!");
        super.checkIndentation();
    }
}
