use reqwest::blocking::Client as HttpClient;
use serde::Deserialize;
use std::error::Error;
use std::fmt::{Display, Formatter};
use std::time::Duration;

#[derive(Debug)]
pub enum Math2Error {
    Validation(String),
    Transport(String),
    Protocol(String),
    Service { status: u16, code: String, message: String, position: Option<usize> },
}

impl Display for Math2Error {
    fn fmt(&self, formatter: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::Validation(message) | Self::Transport(message) | Self::Protocol(message) => formatter.write_str(message),
            Self::Service { status, code, message, .. } => write!(formatter, "service error {status} {code}: {message}"),
        }
    }
}
impl Error for Math2Error {}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct Number(String);

impl Number {
    pub fn from_string(value: impl AsRef<str>) -> Result<Self, Math2Error> { Ok(Self(canonical(value.as_ref())?)) }
    pub fn from_integer(value: i64) -> Self { Self(value.to_string()) }
    pub fn from_float(value: f64) -> Result<Self, Math2Error> {
        if !value.is_finite() { return Err(Math2Error::Validation("number must be finite".into())); }
        Self::from_string(value.to_string())
    }
    pub fn zero() -> Self { Self("0".into()) }
    pub fn as_str(&self) -> &str { &self.0 }
}
impl Display for Number { fn fmt(&self, formatter: &mut Formatter<'_>) -> std::fmt::Result { formatter.write_str(&self.0) } }

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct Equation(String);

impl Equation {
    pub fn parse(value: impl Into<String>) -> Result<Self, Math2Error> {
        let value = value.into();
        if value.trim().is_empty() { return Err(Math2Error::Validation("expression must not be blank".into())); }
        Ok(Self(value.trim().into()))
    }
    pub fn of(number: &Number) -> Self { Self(number.to_string()) }
    pub fn add(&self, other: &Self) -> Self { self.binary("+", other) }
    pub fn subtract(&self, other: &Self) -> Self { self.binary("-", other) }
    pub fn multiply(&self, other: &Self) -> Self { self.binary("*", other) }
    pub fn divide(&self, other: &Self) -> Self { self.binary("/", other) }
    pub fn power(&self, other: &Self) -> Self { self.binary("^", other) }
    pub fn min(&self, other: &Self) -> Self { self.function("min", other) }
    pub fn max(&self, other: &Self) -> Self { self.function("max", other) }
    pub fn sqrt(&self) -> Self { self.unary("sqrt") }
    pub fn abs(&self) -> Self { self.unary("abs") }
    pub fn log(&self) -> Self { self.unary("log") }
    pub fn sin(&self) -> Self { self.unary("sin") }
    pub fn cos(&self) -> Self { self.unary("cos") }
    pub fn tan(&self) -> Self { self.unary("tan") }
    pub fn as_str(&self) -> &str { &self.0 }
    fn binary(&self, operator: &str, other: &Self) -> Self { Self(format!("({}{}{})", self.0, operator, other.0)) }
    fn function(&self, name: &str, other: &Self) -> Self { Self(format!("{}({},{})", name, self.0, other.0)) }
    fn unary(&self, name: &str) -> Self { Self(format!("{}({})", name, self.0)) }
}
impl Display for Equation { fn fmt(&self, formatter: &mut Formatter<'_>) -> std::fmt::Result { formatter.write_str(&self.0) } }

pub struct Client { base_url: String, http: HttpClient }

impl Client {
    pub fn new(base_url: impl Into<String>, timeout: Duration) -> Result<Self, Math2Error> {
        let http = HttpClient::builder().timeout(timeout).build().map_err(|error| Math2Error::Transport(error.to_string()))?;
        Ok(Self { base_url: base_url.into().trim_end_matches('/').into(), http })
    }
    pub fn localhost() -> Result<Self, Math2Error> { Self::new("http://127.0.0.1:8080", Duration::from_secs(10)) }
    pub fn calculate(&self, equation: &Equation, precision: Option<usize>) -> Result<Number, Math2Error> {
        let mut query = vec![("equation", equation.as_str().to_string())];
        if let Some(value) = precision { query.push(("precision", value.to_string())); }
        let body: SingleResponse = self.get("/api/v1/calculate", &query)?;
        Number::from_string(body.result)
    }
    pub fn calculate_batch(&self, equations: &[Equation], precision: Option<usize>) -> Result<Vec<Number>, Math2Error> {
        if equations.is_empty() { return Err(Math2Error::Validation("equations must not be empty".into())); }
        let mut query = vec![("equations", equations.iter().map(Equation::as_str).collect::<Vec<_>>().join(","))];
        if let Some(value) = precision { query.push(("precision", value.to_string())); }
        let body: BatchResponse = self.get("/api/v1/calculate/batch", &query)?;
        if body.results.len() != equations.len() { return Err(Math2Error::Protocol("result count does not match request".into())); }
        body.results.into_iter().map(Number::from_string).collect()
    }
    fn get<T: for<'de> Deserialize<'de>>(&self, path: &str, query: &[(&str, String)]) -> Result<T, Math2Error> {
        let response = self.http.get(format!("{}{}", self.base_url, path)).query(query).send()
            .map_err(|error| Math2Error::Transport(error.to_string()))?;
        let status = response.status();
        if !status.is_success() {
            let error: ErrorResponse = response.json().map_err(|cause| Math2Error::Protocol(cause.to_string()))?;
            return Err(Math2Error::Service { status: status.as_u16(), code: error.code, message: error.message, position: error.position });
        }
        response.json().map_err(|error| Math2Error::Protocol(error.to_string()))
    }
}

#[derive(Deserialize)] struct SingleResponse { result: String }
#[derive(Deserialize)] struct BatchResponse { results: Vec<String> }
#[derive(Deserialize)] struct ErrorResponse { code: String, message: String, position: Option<usize> }

fn canonical(input: &str) -> Result<String, Math2Error> {
    let input = input.trim();
    let (mantissa, exponent) = match input.find(['e', 'E']) {
        Some(index) => (&input[..index], input[index + 1..].parse::<isize>().map_err(|_| Math2Error::Validation("invalid exponent".into()))?),
        None => (input, 0),
    };
    let (negative, mantissa) = if let Some(value) = mantissa.strip_prefix('-') { (true, value) }
        else { (false, mantissa.strip_prefix('+').unwrap_or(mantissa)) };
    if mantissa.is_empty() || !mantissa.chars().any(|character| character.is_ascii_digit())
        || mantissa.chars().filter(|character| *character == '.').count() > 1
        || !mantissa.chars().all(|character| character.is_ascii_digit() || character == '.') {
        return Err(Math2Error::Validation(format!("invalid decimal number: {input}")));
    }
    let dot = mantissa.find('.').unwrap_or(mantissa.len());
    let all_digits: String = mantissa.chars().filter(|character| *character != '.').collect();
    let digits = all_digits.trim_start_matches('0');
    if digits.is_empty() { return Ok("0".into()); }
    let decimal = dot as isize + exponent - all_digits.len() as isize + digits.len() as isize;
    let mut result = if decimal <= 0 { format!("0.{}{}", "0".repeat((-decimal) as usize), digits) }
        else if decimal as usize >= digits.len() { format!("{}{}", digits, "0".repeat(decimal as usize - digits.len())) }
        else { format!("{}.{}", &digits[..decimal as usize], &digits[decimal as usize..]) };
    if result.contains('.') { while result.ends_with('0') { result.pop(); } if result.ends_with('.') { result.pop(); } }
    Ok(if negative { format!("-{result}") } else { result })
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn values_and_equations_are_canonical_and_immutable() {
        assert_eq!(Number::from_string("001.2300e2").unwrap().as_str(), "123");
        assert_eq!(Number::from_string("-0.0").unwrap(), Number::zero());
        let two = Equation::of(&Number::from_integer(2));
        assert_eq!(two.add(&Equation::of(&Number::from_integer(3))).sqrt().as_str(), "sqrt((2+3))");
        assert_eq!(two.as_str(), "2");
    }

    #[test]
    fn client_calculates_against_live_math2_service() {
        let Ok(base_url) = std::env::var("MATH2_TEST_BASE_URL") else { return };
        let client = Client::new(base_url, Duration::from_secs(30)).unwrap();
        let single = client.calculate(&Equation::parse("sqrt(9)+2^3").unwrap(), Some(1000)).unwrap();
        assert_eq!(single.as_str(), "11");

        let batch = client.calculate_batch(&[
            Equation::parse("1+1").unwrap(),
            Equation::parse("max(2,3)").unwrap(),
            Equation::parse("sqrt(16)").unwrap(),
        ], Some(1000)).unwrap();
        assert_eq!(batch.iter().map(Number::as_str).collect::<Vec<_>>(), vec!["2", "3", "4"]);
    }
}
