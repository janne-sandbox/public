export function greeting(name) {
  return `Hello, ${name}`;
}

export function rememberToken(token) {
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('token', token);
  }
}

if (typeof document !== 'undefined') {
  document.querySelector('#message').textContent = greeting('review sample');
}
