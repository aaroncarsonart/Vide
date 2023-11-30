package com.atonementcrystals.dnr.vikari.ide.parsing;

/**
 * Variable-length identifiers for tokens that don't have one exact definition. As well as general categories that
 * can be specified so that each individual identifier doesn't need to be explicitly set in the list of rules.<b/>
 * <b/>
 * The declaration order of this enum specifies the order in which later rules will override prior rules. So
 * CONSTANT overrides the previous setting for VARIABLE for variables that are constants.
 */
public enum IdentifierType {
    VARIABLE,           // Reference.
    TYPE,               // TypeReference.
    TYPE_KEYWORD,       // Type, AbstractType, /Interface, TestSuite, Library.
    PUNCTUATION,        // The characters .,;:?!&~ as well as .. ... and ::.
    CONTROL_FLOW,       // The keyword operators ??, <>, --, ++.
    OPERATORS,          // Any unary or binary operator not specified in PUNCTUATION or CONTROL_FLOW.
    SEPARATORS,         // The characters |()[]{}.
    GROUPING,           // The characters [] as used in a grouping expression.
    CONSTANT,           // Reference enclosed in { }.
    CONSTANT_BRACKETS,  // { } enclosing a constant Reference.
    ANNOTATION,         // TypeReference prefixed by $: as well as the optional {|} characters used afterwards.
    COLLECTION_LITERAL, // $:(1|2|3)
    LITERALS,           // Any string, character, number, or boolean value.
    SWORDS,              // Identifiers consisting only of underscores _. Also \\, ||, and //.
    NUMBERS,             // Any numeric literal: 1, 3.14B, etc.
    BOOLEANS,            // A boolean literal of true or false.
    NULLS,               // The null keyword.
    STRINGS,             // ``Any string enclosed in capture quotations.``
    CHARACTERS,          // A character literal token. (`a`)
    QUOTED_IDENTIFIER,  // `Any string enclosed by single backticks.`
    COMMENT,            // ~:Any type of comment token.:~
    FIELD_ACCESS,       // Field member accesses prefixed by # and @.
    FUNCTION_DECLARATION,       // A function reference within its declaration statement.
    FUNCTION_CALL,              // A function reference when called with !.
    FUNCTION_PARAMETER_LIST,    // The characters (|) as used in a function parameter list's declaration.
    FUNCTION_ARGUMENT_LIST,     // The characters (|) as used in a function call's list of arguments.
    CONSTRUCTOR_DECLARATION,    // A function declaration for the constructor of a type.
    CONSTRUCTOR_CALL,           // A function call for the constructor of a type.
}