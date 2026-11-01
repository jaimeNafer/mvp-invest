export function gerarTokenMock(claims: { sub: string; name: string; role: string }): string {
  const header = codificarBase64Url({ alg: "none", typ: "JWT" });
  const payload = codificarBase64Url({
    ...claims,
    iat: Math.floor(Date.now() / 1000),
  });
  return `${header}.${payload}.`;
}

function codificarBase64Url(valor: unknown): string {
  const bytes = new TextEncoder().encode(JSON.stringify(valor));
  return btoa(String.fromCharCode(...bytes))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}
