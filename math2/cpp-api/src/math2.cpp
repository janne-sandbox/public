#include "math2/math2.hpp"
#include <curl/curl.h>
#include <cmath>
#include <iomanip>
#include <regex>
#include <sstream>

namespace nologo2::math2 {
namespace {
std::string canonical(std::string input) {
    static const std::regex pattern(R"(^([+-]?)(\d*)(?:\.(\d*))?(?:[eE]([+-]?\d+))?$)");
    std::smatch match;
    input.erase(0, input.find_first_not_of(" \t\r\n"));
    input.erase(input.find_last_not_of(" \t\r\n") + 1);
    if (!std::regex_match(input, match, pattern) || (match[2].str().empty() && match[3].str().empty()))
        throw ValidationError("Invalid decimal number: " + input);
    std::string digits = match[2].str() + match[3].str();
    auto first = digits.find_first_not_of('0');
    if (first == std::string::npos) return "0";
    digits.erase(0, first);
    int exponent = match[4].matched ? std::stoi(match[4].str()) : 0;
    long decimal = static_cast<long>(match[2].length()) + exponent
        - static_cast<long>(match[2].length() + match[3].length()) + static_cast<long>(digits.length());
    std::string result;
    if (decimal <= 0) result = "0." + std::string(-decimal, '0') + digits;
    else if (decimal >= static_cast<long>(digits.length())) result = digits + std::string(decimal - digits.length(), '0');
    else result = digits.substr(0, decimal) + "." + digits.substr(decimal);
    if (result.find('.') != std::string::npos) {
        while (result.back() == '0') result.pop_back();
        if (result.back() == '.') result.pop_back();
    }
    return match[1].str() == "-" ? "-" + result : result;
}

size_t write_callback(char* data, size_t size, size_t count, void* target) {
    static_cast<std::string*>(target)->append(data, size * count);
    return size * count;
}

std::string json_string(const std::string& json, const std::string& key) {
    std::regex field("\\\"" + key + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    std::smatch match;
    if (!std::regex_search(json, match, field)) throw ProtocolError("Missing JSON field: " + key);
    return match[1].str();
}
}

ServiceError::ServiceError(long status, std::string code, std::string message, int position)
    : Error(std::move(message)), status_(status), code_(std::move(code)), position_(position) {}
long ServiceError::status() const noexcept { return status_; }
const std::string& ServiceError::code() const noexcept { return code_; }
int ServiceError::position() const noexcept { return position_; }

Number::Number(std::string value) : value_(canonical(std::move(value))) {}
Number Number::from_string(const std::string& value) { return Number(value); }
Number Number::from_integer(long long value) { return Number(std::to_string(value)); }
Number Number::from_float(double value) {
    if (!std::isfinite(value)) throw ValidationError("Number must be finite");
    std::ostringstream output; output << std::setprecision(17) << value;
    return Number(output.str());
}
Number Number::zero() { return Number("0"); }
const std::string& Number::str() const noexcept { return value_; }

Equation::Equation(std::string expression) : expression_(std::move(expression)) {
    if (expression_.find_first_not_of(" \t\r\n") == std::string::npos) throw ValidationError("Expression must not be blank");
}
Equation Equation::parse(const std::string& expression) { return Equation(expression); }
Equation Equation::of(const Number& number) { return Equation(number.str()); }
Equation Equation::binary(const char* operation, const Equation& other) const { return Equation("(" + expression_ + operation + other.expression_ + ")"); }
Equation Equation::function(const char* name, const Equation& other) const { return Equation(std::string(name) + "(" + expression_ + "," + other.expression_ + ")"); }
Equation Equation::unary(const char* name) const { return Equation(std::string(name) + "(" + expression_ + ")"); }
Equation Equation::add(const Equation& value) const { return binary("+", value); }
Equation Equation::subtract(const Equation& value) const { return binary("-", value); }
Equation Equation::multiply(const Equation& value) const { return binary("*", value); }
Equation Equation::divide(const Equation& value) const { return binary("/", value); }
Equation Equation::power(const Equation& value) const { return binary("^", value); }
Equation Equation::min(const Equation& value) const { return function("min", value); }
Equation Equation::max(const Equation& value) const { return function("max", value); }
Equation Equation::sqrt() const { return unary("sqrt"); }
Equation Equation::abs() const { return unary("abs"); }
Equation Equation::log() const { return unary("log"); }
Equation Equation::sin() const { return unary("sin"); }
Equation Equation::cos() const { return unary("cos"); }
Equation Equation::tan() const { return unary("tan"); }
const std::string& Equation::str() const noexcept { return expression_; }

Client::Client(std::string base_url, long timeout_ms) : base_url_(std::move(base_url)), timeout_ms_(timeout_ms) {
    if (timeout_ms_ < 1) throw ValidationError("Timeout must be positive");
    while (!base_url_.empty() && base_url_.back() == '/') base_url_.pop_back();
}
Number Client::calculate(const Equation& equation, int precision) const {
    auto body = get("/api/v1/calculate", "equation=" + equation.str() + (precision ? "&precision=" + std::to_string(precision) : ""));
    return Number::from_string(json_string(body, "result"));
}
std::vector<Number> Client::calculate_batch(const std::vector<Equation>& equations, int precision) const {
    if (equations.empty()) throw ValidationError("Equations must not be empty");
    std::string joined;
    for (const auto& equation : equations) { if (!joined.empty()) joined += ','; joined += equation.str(); }
    auto body = get("/api/v1/calculate/batch", "equations=" + joined + (precision ? "&precision=" + std::to_string(precision) : ""));
    std::regex value(R"json("([^"\\]*)")json");
    auto start = body.find("\"results\"");
    if (start == std::string::npos) throw ProtocolError("Missing results field");
    std::vector<Number> results;
    for (std::sregex_iterator it(body.begin() + start + 9, body.end(), value), end; it != end; ++it)
        results.push_back(Number::from_string((*it)[1].str()));
    if (results.size() != equations.size()) throw ProtocolError("Result count does not match request");
    return results;
}
std::string Client::get(const std::string& path, const std::string& query) const {
    CURL* curl = curl_easy_init();
    if (!curl) throw TransportError("Could not initialize libcurl");
    std::string encoded;
    std::size_t start = 0;
    while (start <= query.size()) {
        auto separator = query.find('&', start);
        auto part = query.substr(start, separator == std::string::npos ? std::string::npos : separator - start);
        auto equals = part.find('=');
        if (!encoded.empty()) encoded += '&';
        encoded += part.substr(0, equals) + '=';
        auto raw_value = equals == std::string::npos ? std::string() : part.substr(equals + 1);
        char* escaped = curl_easy_escape(curl, raw_value.c_str(), static_cast<int>(raw_value.size()));
        if (!escaped) { curl_easy_cleanup(curl); throw TransportError("Could not encode URL"); }
        encoded += escaped;
        curl_free(escaped);
        if (separator == std::string::npos) break;
        start = separator + 1;
    }
    std::string url = base_url_ + path + "?" + encoded;
    std::string body;
    curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
    curl_easy_setopt(curl, CURLOPT_TIMEOUT_MS, timeout_ms_);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_callback);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &body);
    CURLcode result = curl_easy_perform(curl);
    long status = 0; curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &status);
    curl_easy_cleanup(curl);
    if (result != CURLE_OK) throw TransportError(curl_easy_strerror(result));
    if (status < 200 || status >= 300) throw ServiceError(status, json_string(body, "code"), json_string(body, "message"));
    return body;
}
}
