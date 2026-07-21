# Math2 C++ client

C++20 library exposing immutable `Number`, composable `Equation`, and a libcurl REST `Client`.

```cpp
auto equation = Equation::of(Number::from_integer(9)).sqrt();
auto result = Client().calculate(equation);
```

Build and test with `cmake -S . -B build && cmake --build build && ctest --test-dir build`.
