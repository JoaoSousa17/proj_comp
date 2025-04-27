# Compiler Project - Checkpoint 1 - Group 1G

## For Checkpoint 1:

- All required features were implemented, according to the assignment description;
- All public tests are passing.

Also, we decided to create additional tests for grammar, symbol table and semantic analysis.

## Additional Grammar Tests

The additional tests created (24 tests) cover edge cases and complex scenarios to ensure the grammar's robustness and flexibility. These tests include:

- Complex expressions with nested operations, method calls, and array accesses to verify proper parsing and operator precedence;
- Edge cases such as empty scopes, empty array initializers, and chained operations are tested to ensure the grammar handles minimal and degenerate inputs correctly;
- The tests also include realistic programming constructs like multi-parameter methods and import statements to validate typical usage patterns. 

Additionally, some tests intentionally include semantically invalid but syntactically correct constructs, such as varargs in field declarations, to confirm the grammar remains permissive as required, leaving semantic validation for later analysis phases.

## Additional Symbol Table Tests

The additional symbol table tests (7 tests) expand the validation of the compiler's semantic analysis capabilities, focusing on more granular aspects of symbol table functionality. 

- New tests like *ReturnTypes* verify proper identification of method return types (including arrays);
- *ArrayField* specifically checks correct handling of integer array fields;
- The *ObjectTypes* test ensures object types are properly managed across fields, parameters, and return values;
- Additional coverage includes *EmptyMethod* for validating methods without parameters or local variables, *LocalVariables* for correct scope handling of local declarations, and *Shadowing* to confirm proper variable shadowing behavior between fields and locals.


## Additional Semantic Analysis Tests

The additional semantic analysis tests (5 tests) verify type safety and language constraints in Java-- programs. These tests include cases like:

- *NestedBinary* to validate correct handling of complex arithmetic expressions;
- *ArrayIndexNotBoolean*/*ArrayIndexString* to ensure array indices are strictly integers;
- The *BooleanInWhileCondition* test confirms boolean expressions are properly enforced in control structures;
- *UsingBooleanAsInteger* checks for invalid type conversions.

Some tests (like *ArrayIndexString*) are designed to fail, demonstrating the compiler correctly catches type mismatches, while others (like *NestedBinary*) must pass, proving valid operations are accepted.