# Review Tool Sample Applications

These small applications exercise every language target supported by the
review tool. Each sample contains one intentional review signal so integration
tests can prove that the correct agent scanned the project and produced the
expected finding.

| Directory | Agent | Intentional review signal |
| --- | --- | --- |
| `cpp-console/` | C++ | Unsafe string copy, raw ownership, and a C-style cast |
| `csharp-console/` | C# | `async void`, blocking `.Result`, and interpolated SQL |
| `angular-typescript/` | TypeScript | TypeScript strict mode disabled |
| `javascript-browser/` | React/JavaScript | Token written to `localStorage` |
| `java-console/` | Java | Manual `JSONObject` construction |
| `rust-cli/` | Rust | Excessive `unwrap()` usage |
| `react-ui/` | React | Image without alternative text |

The samples are deliberately educational fixtures, not recommended production
patterns. Run their review integration tests from the parent directory:

```bash
npm test
```

Run every review agent over all samples:

```bash
DEEPSEEK=false INTERNET=false LANGUAGE=all ./run-review.sh
```
