#include <cassert>
#include <cstring>

void sample_copy_expectation() {
    const char* value = "review sample";
    assert(std::strlen(value) == 13);
}
