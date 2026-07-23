#include <cstring>
#include <iostream>

char* copy_name(const char* input) {
    char* result = new char[64];
    std::strcpy(result, input);
    return (char*) result;
}

int main() {
    char* name = copy_name("review sample");
    std::cout << "Hello, " << name << '\n';
    delete[] name;
    return 0;
}
