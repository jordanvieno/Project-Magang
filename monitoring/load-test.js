import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 },   // naik bertahap ke 10 virtual user dalam 30 detik
    { duration: '1m', target: 10 },    // tahan di 10 user selama 1 menit
    { duration: '30s', target: 50 },   // naik ke 50 user dalam 30 detik
    { duration: '1m', target: 50 },    // tahan di 50 user selama 1 menit
    { duration: '30s', target: 0 },    // turun kembali ke 0 (cooldown)
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'], // 95% request harus di bawah 1 detik
    http_req_failed: ['rate<0.05'],    // error rate harus di bawah 5%
  },
};

const BASE_URL = 'http://agen46-dev:8080';

export default function () {
  const rootRes = http.get(`${BASE_URL}/`);
  check(rootRes, { 'root status 200': (r) => r.status === 200 });

  const healthRes = http.get(`${BASE_URL}/api/v1/payments/health`);
  check(healthRes, { 'health status 200': (r) => r.status === 200 });

  sleep(1);
}