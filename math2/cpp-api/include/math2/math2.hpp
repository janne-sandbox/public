#pragma once

#include <stdexcept>
#include <string>
#include <vector>

namespace nologo2::math2 {

class Error : public std::runtime_error { using std::runtime_error::runtime_error; };
class ValidationError : public Error { using Error::Error; };
class TransportError : public Error { using Error::Error; };
class ProtocolError : public Error { using Error::Error; };
class ServiceError : public Error {
public:
    ServiceError(long status, std::string code, std::string message, int position = -1);
    long status() const noexcept;
    const std::string& code() const noexcept;
    int position() const noexcept;
private:
    long status_;
    std::string code_;
    int position_;
};

class Number {
public:
    static Number from_string(const std::string& value);
    static Number from_integer(long long value);
    static Number from_float(double value);
    static Number zero();
    const std::string& str() const noexcept;
    bool operator==(const Number&) const = default;
private:
    explicit Number(std::string value);
    std::string value_;
};

class Equation {
public:
    static Equation parse(const std::string& expression);
    static Equation of(const Number& number);
    Equation add(const Equation&) const;
    Equation subtract(const Equation&) const;
    Equation multiply(const Equation&) const;
    Equation divide(const Equation&) const;
    Equation power(const Equation&) const;
    Equation min(const Equation&) const;
    Equation max(const Equation&) const;
    Equation sqrt() const;
    Equation abs() const;
    Equation log() const;
    Equation sin() const;
    Equation cos() const;
    Equation tan() const;
    const std::string& str() const noexcept;
private:
    explicit Equation(std::string expression);
    Equation binary(const char* operation, const Equation&) const;
    Equation function(const char* name, const Equation&) const;
    Equation unary(const char* name) const;
    std::string expression_;
};

class Client {
public:
    explicit Client(std::string base_url = "http://127.0.0.1:8080", long timeout_ms = 10000);
    Number calculate(const Equation&, int precision = 0) const;
    std::vector<Number> calculate_batch(const std::vector<Equation>&, int precision = 0) const;
private:
    std::string get(const std::string& path, const std::string& query) const;
    std::string base_url_;
    long timeout_ms_;
};

}
