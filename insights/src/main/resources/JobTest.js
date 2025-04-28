import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// ─── CUSTOM METRIC ──────────────────────────────────────────────────────────────
// A single “errors” rate to track *all* failures, tagged by operation
export let errorRate = new Rate('errors');

// ─── TEST CONFIGURATION ─────────────────────────────────────────────────────────
export const options = {
    stages: [
        { duration: '5s', target: 500 },  // ramp up
        { duration: '10s', target: 500 }, // stay at
        { duration: '5s', target: 0 },    // ramp down
    ],
    thresholds: {
        // overall response‐time SLO
        http_req_duration: ['p(95)<500'],

        // overall failure rate (across *all* requests)
        'errors': ['rate<0.01'],

        // per‐operation failure rates (uses the built‐in http_req_failed tagged by name)
        'http_req_failed{name:create_job}':      ['rate<0.01'],
        'http_req_failed{name:get_job}':         ['rate<0.01'],
        'http_req_failed{name:update_status}':   ['rate<0.01'],
        'http_req_failed{name:list_jobs}':       ['rate<0.01'],
        'http_req_failed{name:list_jobs_by_master}': ['rate<0.01'],
    },
};

// ─── CONSTANTS & HELPERS ────────────────────────────────────────────────────────
const BASE_URL = 'http://localhost:8081';
const LOB      = 'Retail';
const MASTER   = 'Master1';

function createJobRequest() {
    return {
        extendedAttrs: {
            source: 'k6-load-test',
        },
        publisherJobUri: 'http://publisher/job/123',
        consumerJobUri: 'http://consumer/job/456',
    };
}

// ─── TEST SCENARIO ───────────────────────────────────────────────────────────────
export default function () {
    group('Job Creation and Retrieval', () => {
        // ── CREATE ─────────────────
        let createRes = http.post(
            `${BASE_URL}/api/${LOB}/master/${MASTER}/job`,
            JSON.stringify(createJobRequest()),
            {
                headers: { 'Content-Type': 'application/json' },
                tags:    { name: 'create_job' },
            }
        );
        let ok = check(createRes, {
            'status is 201':        (r) => r.status === 201,
            'response has id':      (r) => !!r.json('id'),
        });
        if (!ok) {
            errorRate.add(1, { request: 'create_job', status: createRes.status });
            console.error(`❌ create_job failed → ${createRes.status}`);
        }

        let jobId = createRes.json('id');
        sleep(0.5);

        // ── GET ────────────────────
        let getRes = http.get(
            `${BASE_URL}/api/${LOB}/master/${MASTER}/job/${jobId}`,
            { tags: { name: 'get_job' } }
        );
        ok = check(getRes, {
            'status is 200':            (r) => r.status === 200,
            'returned id matches':      (r) => r.json('id') === jobId,
        });
        if (!ok) {
            errorRate.add(1, { request: 'get_job', status: getRes.status });
            console.error(`❌ get_job failed → ${getRes.status}`);
        }
    });

    group('Job Status Update', () => {
        // reuse creation
        let createRes = http.post(
            `${BASE_URL}/api/${LOB}/master/${MASTER}/job`,
            JSON.stringify(createJobRequest()),
            { headers: { 'Content-Type': 'application/json' }, tags: { name: 'create_job' } }
        );
        let jobId = createRes.json('id');
        sleep(0.5);

        let updateRes = http.put(
            `${BASE_URL}/api/${LOB}/master/${MASTER}/job/${jobId}/status/COMPLETED`,
            null,
            { tags: { name: 'update_status' } }
        );
        let ok = check(updateRes, {
            'status is 200':           (r) => r.status === 200,
            'status is COMPLETED':     (r) => r.json('status') === 'COMPLETED',
        });
        if (!ok) {
            errorRate.add(1, { request: 'update_status', status: updateRes.status });
            console.error(`❌ update_status failed → ${updateRes.status}`);
        }
    });

    group('Job Listing', () => {
        let listRes = http.get(
            `${BASE_URL}/api/${LOB}/master/jobs?page=0&size=10&sort=startTime,asc`,
            { tags: { name: 'list_jobs' } }
        );
        let ok = check(listRes, {
            'status is 200':       (r) => r.status === 200,
            'returns array':       (r) => Array.isArray(r.json()),
        });
        if (!ok) {
            errorRate.add(1, { request: 'list_jobs', status: listRes.status });
            console.error(`❌ list_jobs failed → ${listRes.status}`);
        }
    });

    sleep(0.5);

    group('Job Listing by Master', () => {
        let listByMasterRes = http.get(
            `${BASE_URL}/api/${LOB}/master/${MASTER}/jobs?page=0&size=10&sort=startTime,desc`,
            { tags: { name: 'list_jobs_by_master' } }
        );
        let ok = check(listByMasterRes, {
            'status is 200':       (r) => r.status === 200,
            'returns array':       (r) => Array.isArray(r.json()),
        });
        if (!ok) {
            errorRate.add(1, { request: 'list_jobs_by_master', status: listByMasterRes.status });
            console.error(`❌ list_jobs_by_master failed → ${listByMasterRes.status}`);
        }
    });

}