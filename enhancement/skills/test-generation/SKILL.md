---
name: test-generation
description: Generate unit tests, integration tests, and test data. Activate when the user asks to write tests, add test coverage, generate test cases, or create test fixtures.
license: Apache-2.0
metadata:
  author: gamelan-team
  version: "1.0"
allowed-tools: read_file write_file run_command glob search_files
---

# Test Generation Skill

## When to activate
- "write tests for X", "add unit tests", "generate test cases"
- "what needs test coverage?", "increase coverage to X%"
- "write integration tests", "add property-based tests"
- "this test is failing, help me fix it"

## Test generation approach

### Step 1: Understand what needs testing
Read the file to understand:
- Public methods and their contracts
- Edge cases: null, empty, negative, boundary values
- Error conditions: what exceptions can be thrown
- State changes: what side effects occur

### Step 2: Choose the right test types
| Test type | When to use |
|-----------|-------------|
| Unit       | Pure logic, single class, no I/O |
| Integration | Multiple classes, database, network |
| Property   | Algorithms that hold for all inputs |
| Snapshot   | UI components, serialized output |
| Parameterized | Same logic, many input variants |

### Step 3: Test structure (AAA pattern)
```
// Arrange: set up inputs and mocks
// Act: call the method under test
// Assert: verify the result
```

### Step 4: Coverage targets
Aim for:
- 100% of public methods
- Happy path + at least 2 edge cases per method
- All exception paths
- Boundary values (0, 1, max, max-1, null)

### Step 5: Write the tests
Use the framework already in the project (check for existing test files).
Write the test file using `write_file`.
Run the tests with `run_command` to verify they pass.

## Framework-specific notes

### JUnit 5 / Quarkus
```java
@Test void methodName_condition_expectedResult() {
    // descriptive test name: what, when, then
}
```

### pytest
```python
def test_method_name_when_condition_then_expected():
    # use pytest.raises() for exceptions
    # use @pytest.mark.parametrize for data-driven tests
```

### Jest / Vitest
```javascript
it('should return X when Y', () => {
    // arrange
    expect(fn()).toBe(expected);
});
```

## After generating tests
Always run them. Fix failures before reporting success.
If tests reveal bugs in production code, report them separately.
