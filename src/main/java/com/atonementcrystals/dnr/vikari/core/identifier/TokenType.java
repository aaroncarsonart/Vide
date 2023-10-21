package com.atonementcrystals.dnr.vikari.core.identifier;

public enum TokenType {

    // comments
    COMMENT_PREFIX_CRYSTAL("~:"),
    COMMENT_SUFFIX_CRYSTAL(":~"),

    // Keywords
    CONDITIONAL_BRANCH("??"),
    LOOP("<>"),
    THROW("--"),
    CATCH("++"),
    PUBLIC_ACCESS_MODIFIER("public"),
    PRIVATE_ACCESS_MODIFIER("private"),
    IMPORT("import"),
    PACKAGE("package"),

    // Literals
    TRUE("true"),
    FALSE("false"),
    SWORD("_"),

    // Separators
    BACKTICK("`"),
    CAPTURE_QUOTATION("``"),
    LEFT_SQUARE_BRACKET("["),
    RIGHT_SQUARE_BRACKET("]"),
    ESCAPED_LEFT_CAPTURE_QUOTATION("[``"),
    ESCAPED_RIGHT_CAPTURE_QUOTATION("``]"),

    STATEMENT_SEPARATOR(","),
    QUATERNITY_OPERATOR("::"),

    // List constructor literals
    LEFT_PARENTHESIS("("),
    RIGHT_PARENTHESIS(")"),
    LIST_ELEMENT_SEPARATOR("|"),

    // Atonement Field projection enclosures
    LEFT_CURLY_BRACKET("{"),
    RIGHT_CURLY_BRACKET("}"),

    // Operators
    DOT_OPERATOR("."),
    ECALYIPPE_NIPYONNE_DYUMDYENNAII("..."),
    KNOWLEDGE_OPERATOR(":"),
    FUNCTION_OPERATOR("!"),

    FIELD_MEMBER_ACCESS("@"),
    HARMONIZED_FIELD_MEMBER_ACCESS("#"),
    INDEX_OPERATOR("$"),
    COPY_CONSTRUCTOR("&"),
    MODULUS("%"),
    PERCENT("%"),
    MULTIPLY("*"),
    CONSTRUCTOR("*"),
    SUBTRACT("-"),
    NEGATE("-"),

    // assignment operators
    LEFT_ASSIGNMENT("<<"),
    LEFT_ADD_ASSIGNMENT("+<<"),
    LEFT_SUBTRACT_ASSIGNMENT("-<<"),
    LEFT_DIVIDE_ASSIGNMENT("/<<"),
    LEFT_MULTIPLY_ASSIGNMENT("*<<"),

    RIGHT_ASSIGNMENT(">>"),
    RIGHT_ADD_ASSIGNMENT("+>>"),
    RIGHT_SUBTRACT_ASSIGNMENT("->>"),
    RIGHT_DIVIDE_ASSIGNMENT("\\>>"),
    RIGHT_MULTIPLY_ASSIGNMENT("*>>"),

    LEFT_LOGICAL_AND_ASSIGNMENT("^<<"),
    LEFT_LOGICAL_OR_ASSIGNMENT("\"<<"),
    RIGHT_LOGICAL_AND_ASSIGNMENT("^>>"),
    RIGHT_LOGICAL_OR_ASSIGNMENT("\">>"),

    ADD("+"),
    CONCATENATE("+"),
    LEFT_DIVIDE("/"),
    RIGHT_DIVIDE("\\"),
    FEATHER("~"),
    DELETE("~"),
    LINE_CONTINUATION("~"),

    // logical and comparison operators
    // ^,",=,'=
    LOGICAL_AND("^"),
    LOGICAL_OR("\""),
    LOGICAL_NOT("'"),
    EQUALS("="),
    GREATER_THAN("<"),
    LESS_THAN(">"),
    GREATER_THAN_OR_EQUALS(">="),
    LESS_THAN_OR_EQUALS("<="),

    RETURN("^^"),
    CONTINUE(">>"),
    BREAK("vv"),
    KEY_VALUE_PAIR("=>"),

    // Angel guards
    CATCH_ALL("||"),
    LEFT_FEATHER_FALL_OPERATOR("\\\\"),
    RIGHT_FEATHER_FALL_OPERATOR("//"),

    // Fear crystals
    FEAR_CRYSTAL("_.|._"),
    LEFT_FEAR_CRYSTAL("_.|.-"),
    RIGHT_FEAR_CRYSTAL("-.|._"),
    LOVE_CRYSTAL("}*{"),

    // two curriculums
    TEACHER_OF_TEACHERS("*~."),
    STUDENT_OF_STUDENTS(".~*");

    private String identifier;

    /**
     * Create A new DefaultIdentifierMapping between its identifier and its concrete Crystal class type.
     * @param identifier The default string identifier to map.
     */
    TokenType(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
