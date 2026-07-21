#include "math2/math2.hpp"
#include <cassert>
#include <cstdlib>
#include <iostream>

using namespace nologo2::math2;

int main() {
    assert(Number::from_string("001.2300e2").str() == "123");
    assert(Number::from_string("-0.0") == Number::zero());
    auto two = Equation::of(Number::from_integer(2));
    assert(two.add(Equation::of(Number::from_integer(3))).sqrt().str() == "sqrt((2+3))");
    assert(two.str() == "2");
    bool rejected = false;
    try { Number::from_string("invalid"); } catch (const ValidationError&) { rejected = true; }
    assert(rejected);
    if (const char* base_url = std::getenv("MATH2_TEST_BASE_URL")) {
        Client client(base_url, 30000);
        assert(client.calculate(Equation::parse("sqrt(9)+2^3"), 1000).str() == "11");
        auto batch = client.calculate_batch({
            Equation::parse("1+1"), Equation::parse("max(2,3)"), Equation::parse("sqrt(16)")
        }, 1000);
        assert(batch.size() == 3);
        assert(batch[0].str() == "2" && batch[1].str() == "3" && batch[2].str() == "4");
    }
    std::cout << "Math2 C++ tests passed\n";
}
