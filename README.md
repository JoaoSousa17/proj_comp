# Compiler Project - Checkpoint 2 - Group 1G

## For Checkpoint 2:

### Ollir
- All required features are implemented, according to the assignment description;
- All public tests are passing;
- Due to time constraints, we were unable to create extra Ollir test cases.

### Optimizations
- 5/7 public tests are passing;
- Tests **_regAllocSequence_** and **_constPropWithLoop_** are failing, most likely due to minor unresolved bugs that we could not fix before the deadline;
- Implemented Optimizations:
    - **Constant Propagation:** Replaces variables known to hold constant values with those values directly.
    - **Constant Folding:** Precomputes constant expressions and replaces them by their resulting constant value.
    - **Register Allocation:** Minimizes the number of registers used by reusing registers when variables are no longer needed.
- No extra optimization tests were created due to time limitations.


