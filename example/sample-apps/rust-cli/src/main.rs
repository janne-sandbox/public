use std::fs;

fn load_name(path: &str) -> Result<String, std::io::Error> {
    let content = fs::read_to_string(path).unwrap();
    Ok(content.trim().to_owned())
}

fn main() {
    let name = load_name("name.txt").unwrap();
    println!("Hello, {name}");
}
