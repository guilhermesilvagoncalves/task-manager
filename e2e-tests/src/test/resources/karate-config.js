function fn() {
  return { baseUrl: karate.properties['gatewayUrl'] || 'http://localhost:8080' };
}
