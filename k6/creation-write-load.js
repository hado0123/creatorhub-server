import http from 'k6/http';
import { check, fail } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import sse from 'k6/x/sse';


// init 컨텍스트에서 실제 JPEG 파일을 바이너리로 로드 (VU 초기화 시 1회만 실행)
const SAMPLE_JPEG = open('./sample-images/thumb.jpg', 'b');

/**ㄴ
 * [작품 등록 부하 테스트]
 *  1. 로그인
 *  2. 썸네일 등록
 *     - 포스터형(POSTER):   presigned URL 요청 → S3 업로드 → READY 마킹
 *     - 가로형(HORIZONTAL): presigned URL 요청 → S3 업로드 → 실제 Lambda가 리사이징
 *                          → Lambda가 ngrok 경유(개발시)로 /api/files/resize-complete 콜백
 *                          → SSE 구독(/api/sse/subscribe)으로 RESIZE_COMPLETE 이벤트 수신
 *  3. 작품 등록
 */

// ── 커스텀 메트릭 ──────────────────────────────────────────────
const errorRate              = new Rate('error_rate');
const loginDuration          = new Trend('login_duration', true);
const presignedDuration      = new Trend('presigned_duration', true);
const s3UploadDuration       = new Trend('s3_upload_duration', true);
const markReadyDuration      = new Trend('mark_ready_duration', true);
const sseConnectDuration     = new Trend('sse_connect_duration', true);     // 1. SSE 연결 확립
const uploadReadyDuration    = new Trend('upload_ready_duration', true);    // 2. S3 업로드 + READY 마킹
const lambdaCallbackDuration = new Trend('lambda_callback_duration', true); // 3. Lambda 처리 + 서버 콜백 수신 (서버가 callbackReceivedAt 제공 시)
const ssePublishDuration     = new Trend('sse_publish_duration', true);     // 4. SSE 이벤트 발행 → 클라이언트 수신 (서버가 callbackReceivedAt 제공 시)
const createDuration         = new Trend('creation_create_duration', true);
const totalFlowDuration      = new Trend('total_flow_duration', true);
const resizeTimeoutCount     = new Counter('resize_timeout_count');

// ── 환경변수 ───────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8000';

// 테스트용 CREATOR 계정 (미리 DB에 seeding 필요)
const TEST_EMAIL    = __ENV.TEST_EMAIL    || 'test@test.com';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'test1234!';

// SSE 리사이징 대기 설정 (xk6-sse timeout은 Go duration 문자열 형식)
const RESIZE_TIMEOUT = '120s';

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
        error_rate:               ['rate<0.05'],
    },
};

function makeSampleJpeg(thumbnailType) {
    return {
        body:     SAMPLE_JPEG,
        filename: `test_${thumbnailType.toLowerCase()}_${Date.now()}.jpg`,
        size:     SAMPLE_JPEG.byteLength,
    };
}

// ── HTTP 래퍼 (res.timings.duration으로 측정 → 음수 없음) ──────
function timedPost(trend, url, body, params) {
    const res = http.post(url, body, params);
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
// SSE 구독 + RESIZE_COMPLETE 대기
//   GET /api/sse/subscribe?baseKey=...
//
// 실행 순서:
//   1) SSE 구독 시작 (연결 확립)
//   2) onConnected() 콜백 실행 → S3 업로드 + READY 마킹 (Lambda 트리거)
//   3) Lambda 리사이징 → ngrok 경유로 서버에 콜백
//   4) 서버가 SSE로 RESIZE_COMPLETE 이벤트 전송 → 수신 시 종료
//
// SSE 연결을 먼저 열어 Lambda보다 구독이 앞서도록 보장.
//
// xk6-sse를 사용하므로 k6 빌드 시 아래 명령으로 커스텀 바이너리 필요:
//   xk6 build --with github.com/phymbert/xk6-sse
// ──────────────────────────────────────────────────────────────
function subscribeAndWaitForResize(token, baseKey, onConnected) {
    const t0  = Date.now();
    const url = `${BASE_URL}/api/sse/subscribe?baseKey=${encodeURIComponent(baseKey)}`;

    let resizeComplete = false;
    let t2 = 0; // onConnected 완료 시점 (READY 마킹 직후)

    const res = sse.open(url, {
        headers: { 'Authorization': `Bearer ${token}` },
        timeout: RESIZE_TIMEOUT,
        tags: { name: 'sse_resize_subscribe' },
    }, function(client) {
        const t1 = Date.now();
        sseConnectDuration.add(t1 - t0); // 1. SSE 연결 확립

        // CONNECTED 이벤트의 serverTime으로 서버-클라이언트 클럭 오프셋 계산
        // adjustedServerTime = serverTime - clockOffset → 클라이언트 기준 시간으로 변환
        let clockOffset = 0;

        client.on('event', function(event) {
            if (event.name === 'CONNECTED') {
                try {
                    const data = JSON.parse(event.data || '{}');
                    if (data.serverTime) {
                        clockOffset = data.serverTime - t1; // 서버 시간 - 클라이언트 시간
                    }
                } catch (_) {}
            }

            if (event.name === 'RESIZE_COMPLETE') {
                const t4 = Date.now();
                resizeComplete = true;

                // 3+4단계 분리: callbackReceivedAt을 clockOffset으로 보정해 클라이언트 기준 시간으로 변환
                try {
                    const data = JSON.parse(event.data || '{}');
                    if (data.callbackReceivedAt) {
                        const adjustedCallbackTime = data.callbackReceivedAt - clockOffset;
                        lambdaCallbackDuration.add(adjustedCallbackTime - t2); // 3. Lambda 처리 + 서버 콜백
                        ssePublishDuration.add(t4 - adjustedCallbackTime);     // 4. SSE 발행 → 클라이언트 수신
                    }
                } catch (_) { /* event.data가 JSON이 아닌 경우 무시 */ }

                client.close();
            }
        });

        client.on('error', function(e) {
            console.error(`[sse] 오류 baseKey=${baseKey}: ${e}`);
            client.close();
        });

        // SSE 연결 확립 후 S3 업로드 + READY 마킹 실행 (Lambda 트리거)
        if (onConnected) onConnected();
        t2 = Date.now();
        uploadReadyDuration.add(t2 - t1); // 2. S3 업로드 + READY 마킹
    });

    const ok = check(res, { '[sse] status 200': (r) => r.status === 200 });
    errorRate.add(!ok);

    if (!resizeComplete) {
        resizeTimeoutCount.add(1);
        console.warn(`[sse] RESIZE_COMPLETE 미수신 baseKey=${baseKey}`);
    }

    return res;
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

    // 2) POSTER: presigned URL → S3 업로드 → READY 마킹
    const posterJpeg = makeSampleJpeg('POSTER');
    const posterPresigned = withAuth((token) => requestPresignedUrl(token, 'POSTER', posterJpeg));

    uploadToS3(posterPresigned.uploadUrl, posterJpeg);

    withAuth((token) => markThumbnailReady(token, posterPresigned.fileObjectId));

    // 3) HORIZONTAL: presigned URL 요청(baseKey 획득)
    //               → SSE 구독 시작
    //               → S3 업로드(Lambda 트리거) → READY 마킹
    //               → RESIZE_COMPLETE 이벤트 수신
    const horizontalJpeg = makeSampleJpeg('HORIZONTAL');
    const horizontalPresigned = withAuth((token) => requestPresignedUrl(token, 'HORIZONTAL', horizontalJpeg));
    const baseKey = extractBaseKey(horizontalPresigned.storageKey);

    // SSE 구독 시작 → 연결 확립 후 S3 업로드 + READY 마킹 → RESIZE_COMPLETE 수신 대기
    withAuth((token) => subscribeAndWaitForResize(token, baseKey, () => {
        uploadToS3(horizontalPresigned.uploadUrl, horizontalJpeg);
        withAuth((t) => markThumbnailReady(t, horizontalPresigned.fileObjectId));
    }));

    // 4) 작품 등록
    const creationId = withAuth((token) => createCreation(
        token,
        horizontalPresigned.fileObjectId,
        posterPresigned.fileObjectId,
    ));

    totalFlowDuration.add(Date.now() - flowStart);
    console.log(`[VU ${__VU}] ITER=${__ITER} 작품 등록 완료 creationId=${creationId} (토큰 캐시: ${tokenCache[__VU] ? 'HIT' : 'MISS'})`);

}
