import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import encoding from 'k6/encoding';
import crypto from 'k6/crypto';

/**
 * [작품 등록 부하 테스트]
 *  1. 로그인
 *  2. 썸네일 등록
 *     - 포스터형(POSTER):   presigned URL 요청 → S3 업로드 → READY 마킹
 *     - 가로형(HORIZONTAL): presigned URL 요청 → S3 업로드(응답이 오면 가로형은 READY로 변경)
 *                          → k6가 Lambda 콜백(/api/files/resize-complete) 직접 호출
 *                          → 서버가 파생 6종 READY 처리
 *                          → GET /api/files/resize-status 폴링으로 READY 확인
 *  3. 작품 등록
 */

// ── 커스텀 메트릭 ──────────────────────────────────────────────
const errorRate              = new Rate('error_rate');
const loginDuration          = new Trend('login_duration', true);
const presignedDuration      = new Trend('presigned_duration', true);
const s3UploadDuration       = new Trend('s3_upload_duration', true);
const markReadyDuration      = new Trend('mark_ready_duration', true);
const lambdaCallbackDuration = new Trend('lambda_callback_duration', true);
const ssePollDuration        = new Trend('sse_poll_duration', true);
const createDuration         = new Trend('creation_create_duration', true);
const totalFlowDuration      = new Trend('total_flow_duration', true);
const resizeTimeoutCount     = new Counter('resize_timeout_count');

// ── 환경변수 ───────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8000';

// 테스트용 CREATOR 계정 (미리 DB에 seeding 필요)
const TEST_EMAIL    = __ENV.TEST_EMAIL    || 'test2@test.com';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'test1234!';

// Lambda 콜백 HMAC 시크릿 (application-local-secret.yml: callback.secret)
const CALLBACK_SECRET = __ENV.CALLBACK_SECRET || 'creatorhub-resize-callback-7f3b9c4e2a8d6e1f9a0c5b4d8e2f7a6c';

// S3 버킷명
const S3_BUCKET = __ENV.S3_BUCKET || 'creatorhub-dev';

// 리사이징 폴링 설정
const RESIZE_TIMEOUT_MS        = 30000; // READY 대기 최대 시간 (ms)
const RESIZE_POLL_INTERVAL_SEC = 0.5;   // 폴링 간격 (초)

// ── 부하 시나리오 ───────────────────────────────────────────────
export const options = {
    // 기본적으로 모든 응답 바디를 버림 → 메모리/IO 절약
    // body가 필요한 요청은 개별적으로 responseType: 'text' 옵션을 지정
    discardResponseBodies: true,
    scenarios: {
        ramp_up: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 5  },  // 워밍업
                { duration: '1m',  target: 20 },  // 부하 증가
                { duration: '2m',  target: 30 },  // 최대 부하 유지
                { duration: '30s', target: 0  },  // 부하 감소
            ],
        },
    },
    thresholds: {
        creation_create_duration: ['p(95)<3000', 'p(99)<5000'],
        total_flow_duration:      ['p(95)<60000'],
        s3_upload_duration:       ['p(95)<5000'],
        lambda_callback_duration: ['p(95)<1000'],
        error_rate:               ['rate<0.05'],
    },
};

// ── 더미 JPEG (1x1 흰색 픽셀) ─────────────────────────────────
// k6는 Canvas/파일시스템 접근 불가 → 최소 유효 JPEG 바이너리를 base64로 하드코딩
const TINY_JPEG_B64 =
    '/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8U' +
    'HRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgN' +
    'DRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIy' +
    'MjL/wAARCAABAAEDASIAAhEBAxEB/8QAFAABAAAAAAAAAAAAAAAAAAAACf/EABQQAQAAAAAA' +
    'AAAAAAAAAAAAAAD/xAAUAQEAAAAAAAAAAAAAAAAAAAAA/8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/' +
    'aAAwDAQACEQMRAD8AJQAB/9k=';

function makeDummyJpeg(thumbnailType) {
    const bytes = encoding.b64decode(TINY_JPEG_B64, 'std', 'b');
    return {
        body:     bytes,
        filename: `test_${thumbnailType.toLowerCase()}_${Date.now()}.jpg`,
        size:     bytes.byteLength,
    };
}

// ── HTTP 래퍼 (res.timings.duration으로 측정 → 음수 없음) ──────
function timedPost(trend, url, body, params) {
    const res = http.post(url, body, params);
    trend.add(res.timings.duration);
    return res;
}

function timedGet(trend, url, params) {
    const res = http.get(url, params);
    trend.add(res.timings.duration);
    return res;
}

function timedPut(trend, url, body, params) {
    const res = http.put(url, body, params);
    trend.add(res.timings.duration);
    return res;
}

// ── 헬퍼 ──────────────────────────────────────────────────────
// "upload/uuid_434x330.jpg" → "upload/uuid"
function extractBaseKey(storageKey) {
    const i = storageKey.lastIndexOf('_');
    return i !== -1 ? storageKey.substring(0, i) : storageKey;
}

function randomItem(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

function randomSubset(arr, minCount, maxCount) {
    const shuffled = arr.slice().sort(() => Math.random() - 0.5);
    const count = minCount + Math.floor(Math.random() * (maxCount - minCount + 1));
    return shuffled.slice(0, Math.min(count, shuffled.length));
}

function authHeaders(token) {
    return { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` };
}

// ── 테스트 데이터 ──────────────────────────────────────────────
const FORMATS      = ['EPISODE', 'OMNIBUS', 'STORY'];
const GENRES       = ['ROMANCE', 'FANTASY', 'ACTION', 'DAILY_LIFE', 'THRILLER',
                      'COMEDY', 'MARTIAL_ARTS', 'DRAMA', 'EMOTIONAL', 'SPORTS'];
const PUBLISH_DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
const HASHTAG_IDS  = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]; // V2__seed_hashtag.sql 기준

// ──────────────────────────────────────────────────────────────
// 로그인  POST /api/auth/login
// ──────────────────────────────────────────────────────────────
function login() {
    const res = timedPost(
        loginDuration,
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({ email: TEST_EMAIL, password: TEST_PASSWORD, keepLogin: false }),
        { headers: { 'Content-Type': 'application/json' }, responseType: 'text' },
    );

    const ok = check(res, {
        '[login] status 200':       (r) => r.status === 200,
        '[login] accessToken 존재': (r) => { try { return !!JSON.parse(r.body).accessToken; } catch { return false; } },
    });
    errorRate.add(!ok);
    if (!ok) fail(`로그인 실패: status=${res.status} body=${res.body}`);

    return JSON.parse(res.body).accessToken;
}

// ──────────────────────────────────────────────────────────────
// Presigned URL 요청  POST /api/files/creation-thumbnails/presigned
// ──────────────────────────────────────────────────────────────
function requestPresignedUrl(token, thumbnailType, jpeg) {
    const res = timedPost(
        presignedDuration,
        `${BASE_URL}/api/files/creation-thumbnails/presigned`,
        JSON.stringify({
            contentType:      'image/jpeg',
            thumbnailType:    thumbnailType,
            originalFilename: jpeg.filename,
            sizeBytes:        jpeg.size,
        }),
        { headers: authHeaders(token), responseType: 'text' },
    );

    const ok = check(res, {
        [`[presigned:${thumbnailType}] status 200`]:        (r) => r.status === 200,
        [`[presigned:${thumbnailType}] uploadUrl 존재`]:    (r) => { try { return !!JSON.parse(r.body).uploadUrl; } catch { return false; } },
        [`[presigned:${thumbnailType}] fileObjectId 존재`]: (r) => { try { return !!JSON.parse(r.body).fileObjectId; } catch { return false; } },
    });
    errorRate.add(!ok);
    if (!ok) fail(`Presigned URL 실패 [${thumbnailType}]: status=${res.status} body=${res.body}`);

    const body = JSON.parse(res.body);
    return { uploadUrl: body.uploadUrl, fileObjectId: body.fileObjectId, storageKey: body.storageKey };
}

// ──────────────────────────────────────────────────────────────
// S3 PUT 업로드 (presigned URL 직접 호출)
// ──────────────────────────────────────────────────────────────
function uploadToS3(uploadUrl, jpeg) {
    const res = timedPut(
        s3UploadDuration,
        uploadUrl,
        jpeg.body,
        { headers: { 'Content-Type': 'image/jpeg' } },
    );

    const ok = check(res, { '[s3 upload] status 200': (r) => r.status === 200 });
    errorRate.add(!ok);
    if (!ok) fail(`S3 업로드 실패: status=${res.status}`);
}

// ──────────────────────────────────────────────────────────────
// POSTER READY 마킹  POST /api/files/{fileObjectId}/thumbnails/ready
// (HORIZONTAL은 Lambda 콜백에서 READY 처리되므로 호출하지 않음)
// ──────────────────────────────────────────────────────────────
function markThumbnailReady(token, fileObjectId) {
    const res = timedPost(
        markReadyDuration,
        `${BASE_URL}/api/files/${fileObjectId}/thumbnails/ready`,
        null,
        { headers: authHeaders(token) },
    );

    const ok = check(res, { '[ready:POSTER] status 200': (r) => r.status === 200 });
    errorRate.add(!ok);
    if (!ok) fail(`READY 마킹 실패: status=${res.status}`);
}

// ──────────────────────────────────────────────────────────────
// Lambda 콜백 흉내  POST /api/files/resize-complete
//
// 실제 환경: S3 → SQS → Lambda → 리사이징 → 백엔드 콜백
// k6 환경:  실제 Lambda가 없으므로 k6가 직접 콜백을 호출해
//           파생 FileObject 6개를 DB에 생성 (6개 파생 모두 READY 처리됨)
//
// 인증 방식 (LambdaCallbackAuthFilter):
//   X-Timestamp : 현재 시각 (ms, 문자열)
//   X-Signature : HMAC-SHA256( secret, "{timestamp}.{rawBody}" ) hex
// ──────────────────────────────────────────────────────────────
function simulateLambdaCallback(baseKey) {
    const timestamp = String(Date.now());

    const body = JSON.stringify({
        bucket:      S3_BUCKET,
        triggerKey:  baseKey + '_434x330.jpg',
        baseKey:     baseKey,
        derivedFiles: [
            { key: baseKey + '_83x90.jpg',   width: 83,  height: 90,  sizeBytes: 1024 },
            { key: baseKey + '_98x79.jpg',   width: 98,  height: 79,  sizeBytes: 1024 },
            { key: baseKey + '_125x101.jpg', width: 125, height: 101, sizeBytes: 1024 },
            { key: baseKey + '_202x164.jpg', width: 202, height: 164, sizeBytes: 1024 },
            { key: baseKey + '_217x165.jpg', width: 217, height: 165, sizeBytes: 1024 },
            { key: baseKey + '_218x120.jpg', width: 218, height: 120, sizeBytes: 1024 },
        ],
        resizedAt: new Date().toISOString(),
    });

    const signature = crypto.hmac('sha256', CALLBACK_SECRET, timestamp + '.' + body, 'hex');

    const res = timedPost(
        lambdaCallbackDuration,
        `${BASE_URL}/api/files/resize-complete`,
        body,
        {
            headers: {
                'Content-Type': 'application/json',
                'X-Timestamp':  timestamp,
                'X-Signature':  signature,
            },
        },
    );

    const ok = check(res, { '[lambda-callback] status 200': (r) => r.status === 200 });
    errorRate.add(!ok);
    if (!ok) fail(`Lambda 콜백 실패: status=${res.status}`);
}

// ──────────────────────────────────────────────────────────────
// 리사이징 완료 폴링  GET /api/files/resize-status?baseKey=...
//
// SSE는 k6 단일 스레드 특성상 재현 불가(콜백 전에 구독 불가).
// 대신 파생 6종 FileObject가 모두 READY인지 DB 기반으로 폴링.
// 즉시 응답(200 JSON)이므로 timeout 없음 → http_req_failed 오염 없음.
// ──────────────────────────────────────────────────────────────
function waitForResize(token, baseKey) {
    const deadline = Date.now() + RESIZE_TIMEOUT_MS;

    let found   = false;
    let attempt = 0;

    while (Date.now() < deadline) {
        attempt++;

        const res = timedGet(
            ssePollDuration,
            `${BASE_URL}/api/files/resize-status?baseKey=${encodeURIComponent(baseKey)}`,
            {
                headers: { ...authHeaders(token), 'Accept': 'application/json' },
                responseType: 'text',
                tags: { name: 'resize_status_poll' },
            },
        );

        if (res.status === 200) {
            try {
                if (JSON.parse(res.body).ready === true) {
                    found = true;
                    break;
                }
            } catch (_) { /* 파싱 실패 시 다음 폴링 */ }
        }

        sleep(RESIZE_POLL_INTERVAL_SEC);
    }

    if (!found) {
        resizeTimeoutCount.add(1);
        console.warn(`[resize-status] 폴링 타임아웃 baseKey=${baseKey} attempts=${attempt}`);
    }
}

// ──────────────────────────────────────────────────────────────
// 작품 등록  POST /api/creations/create
// ──────────────────────────────────────────────────────────────
function createCreation(token, horizontalFileObjectId, posterFileObjectId) {
    const res = timedPost(
        createDuration,
        `${BASE_URL}/api/creations/create`,
        JSON.stringify({
            format:                         randomItem(FORMATS),
            genre:                          randomItem(GENRES),
            title:                          `테스트 작품 ${Date.now()}`,
            plot:                           `k6 부하 테스트용 줄거리입니다. VU=${__VU} ITER=${__ITER}`,
            isPublic:                       true,
            publishDays:                    randomSubset(PUBLISH_DAYS, 1, 3),
            hashtagIds:                     randomSubset(HASHTAG_IDS, 1, 3),
            horizontalOriginalFileObjectId: horizontalFileObjectId,
            posterOriginalFileObjectId:     posterFileObjectId,
        }),
        { headers: authHeaders(token), responseType: 'text' },
    );

    const ok = check(res, {
        '[create] status 200':      (r) => r.status === 200,
        '[create] creationId 존재': (r) => { try { return !!JSON.parse(r.body).creationId; } catch { return false; } },
    });
    errorRate.add(!ok);
    if (!ok) fail(`작품 등록 실패: status=${res.status} body=${res.body}`);

    return JSON.parse(res.body).creationId;
}

// ── VU별 토큰 캐시 ────────────────────────────────────────────
const tokenCache = {};

function getToken() {
    if (!tokenCache[__VU]) {
        tokenCache[__VU] = login();
    }
    return tokenCache[__VU];
}

function invalidateToken() {
    delete tokenCache[__VU];
}

/**
 * 401 응답 시 토큰을 갱신하고 fn을 재시도
 * fn: (token) => response
 */
function withAuth(fn) {
    let token = getToken();
    let res = fn(token);
    if (res && res.status === 401) {
        console.warn(`[VU ${__VU}] 401 감지 → 재로그인 후 재시도`);
        invalidateToken();
        token = getToken();
        res = fn(token);
    }
    return res;
}

// ── 메인 시나리오 ──────────────────────────────────────────────
export default function () {
    const flowStart = Date.now();

    // 1) 토큰 확보 (캐시 hit 시 로그인 생략)
    getToken();
    sleep(0.3);

    // 2) POSTER: presigned URL → S3 업로드 → READY 마킹
    const posterJpeg = makeDummyJpeg('POSTER');
    const posterPresigned = withAuth((token) => requestPresignedUrl(token, 'POSTER', posterJpeg));
    sleep(0.1);

    uploadToS3(posterPresigned.uploadUrl, posterJpeg);
    sleep(0.1);

    withAuth((token) => markThumbnailReady(token, posterPresigned.fileObjectId));
    sleep(0.1);

    // 3) HORIZONTAL: presigned URL → S3 업로드(응답이 오면 가로형은 READY로 변경) → Lambda 콜백 → resize-status 폴링
    const horizontalJpeg = makeDummyJpeg('HORIZONTAL');
    const horizontalPresigned = withAuth((token) => requestPresignedUrl(token, 'HORIZONTAL', horizontalJpeg));
    const baseKey = extractBaseKey(horizontalPresigned.storageKey);
    sleep(0.1);

    uploadToS3(horizontalPresigned.uploadUrl, horizontalJpeg);
    sleep(0.1);

    withAuth((token) => markThumbnailReady(token, horizontalPresigned.fileObjectId));
    sleep(0.1);

    simulateLambdaCallback(baseKey);
    sleep(0.1);

    withAuth((token) => { waitForResize(token, baseKey); return { status: 200 }; });
    sleep(0.1);

    // 4) 작품 등록
    const creationId = withAuth((token) => createCreation(
        token,
        horizontalPresigned.fileObjectId,
        posterPresigned.fileObjectId,
    ));

    totalFlowDuration.add(Date.now() - flowStart);
    console.log(`[VU ${__VU}] ITER=${__ITER} 작품 등록 완료 creationId=${creationId} (토큰 캐시: ${tokenCache[__VU] ? 'HIT' : 'MISS'})`);

    sleep(Math.random() * 2 + 1);
}
