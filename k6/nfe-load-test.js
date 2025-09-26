import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 10 },
    { duration: '30s', target: 20 },
    { duration: '1m', target: 20 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<800'],
    http_req_failed: ['rate<0.01'],
    errors: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const traceId = `k6-${__VU}-${__ITER}`;
  
  // Test 1: Health check
  let healthRes = http.get(`${BASE_URL}/actuator/health`, {
    headers: { 'X-Trace-Id': traceId },
  });
  
  check(healthRes, {
    'health OK': (r) => r.status === 200,
    'health < 100ms': (r) => r.timings.duration < 100,
  });
  
  // Test 2: List batches
  let listRes = http.get(`${BASE_URL}/api/v1/batches?page=0&size=10`, {
    headers: { 'X-Trace-Id': traceId },
  });
  
  check(listRes, {
    'list OK': (r) => r.status === 200,
    'list < 800ms': (r) => r.timings.duration < 800,
    'has traceId': (r) => r.headers['X-Trace-Id'] !== undefined,
  });
  
  // Test 3: Error handling
  let errorRes = http.get(`${BASE_URL}/api/v1/batches/999999`);
  check(errorRes, {
    'error is 404': (r) => r.status === 404,
    'error is problem+json': (r) => 
      r.headers['Content-Type'] && r.headers['Content-Type'].includes('problem+json'),
  });
  
  errorRate.add(listRes.status !== 200);
  
  sleep(1);
}

export function handleSummary(data) {
  return {
    '.buildtoflip/validations/k6-results.json': JSON.stringify(data),
  };
}
