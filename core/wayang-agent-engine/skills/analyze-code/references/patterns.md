# Common Code Anti-Patterns Reference

## Java
- **God class**: Single class with >500 lines and >10 responsibilities
- **Checked exception swallowing**: `catch (Exception e) { /* ignored */ }`
- **String concatenation in loops**: Use `StringBuilder` instead
- **Mutable static state**: Global mutable state breaks thread safety
- **Resource leaks**: Streams/connections not closed in try-with-resources
- **NullPointerException traps**: Unchecked `null` dereferences

## General
- **Magic numbers**: `if (status == 3)` → use named constants
- **Long parameter lists**: >4 params → introduce a parameter object
- **Deep nesting**: >3 levels → extract methods or use early returns
- **Premature optimization**: Profile first, then optimize hotspots
- **Copy-paste code**: DRY — extract common logic

## Security
- **SQL injection**: Never concatenate user input into SQL; use prepared statements
- **Path traversal**: Validate/sanitize file paths from user input
- **Hardcoded secrets**: Never embed credentials in source code
- **Unvalidated input**: Always validate and sanitize external data
- **Insecure deserialization**: Avoid deserializing untrusted data

## Concurrency
- **Race conditions**: Shared mutable state without synchronization
- **Deadlocks**: Lock ordering inconsistencies
- **Thread starvation**: Unbounded thread pools
- **Double-checked locking**: Use `volatile` or `AtomicReference`
