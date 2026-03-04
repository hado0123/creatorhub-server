import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import encoding from 'k6/encoding';

/**
 * [회차 등록 부하 테스트]
 *  1. 로그인
 *  2. 에피소드 썸네일(EPISODE) presigned → S3 업로드 → READY 마킹
 *  3. SNS 썸네일(SNS)         presigned → S3 업로드 → READY 마킹
 *  4. 원고 이미지 presigned 일괄 요청 → S3 병렬 업로드 → READY 일괄 마킹
 *  5. 회차 등록 (POST /api/episodes/create)
 */

// ── 커스텀 메트릭 ──────────────────────────────────────────────
const errorRate              = new Rate('error_rate');
const loginDuration          = new Trend('login_duration', true);
const presignedDuration      = new Trend('presigned_duration', true);
const s3UploadDuration       = new Trend('s3_upload_duration', true);
const markReadyDuration      = new Trend('mark_ready_duration', true);
const manuscriptPresignedDuration = new Trend('manuscript_presigned_duration', true);
const manuscriptS3Duration   = new Trend('manuscript_s3_duration', true);
const manuscriptMarkDuration = new Trend('manuscript_mark_ready_duration', true);
const createDuration         = new Trend('episode_create_duration', true);
const totalFlowDuration      = new Trend('total_flow_duration', true);
const uploadFailCount        = new Counter('upload_fail_count');

// ── 환경변수 ───────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8000';

// 테스트용 CREATOR 계정 (미리 DB에 seeding 필요)
const TEST_EMAIL    = __ENV.TEST_EMAIL    || 'test@test.com';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'test1234!';

// 회차를 등록할 작품 ID
const CREATION_ID = parseInt(__ENV.CREATION_ID || '1');

// 회차 번호 충돌 방지: VU별로 범위를 분리 (VU 1 → 1000번대, VU 2 → 2000번대 ...)
const EPISODE_NUM_START = parseInt(__ENV.EPISODE_NUM_START || '1000');

// 회차당 업로드할 원고 수 (1 ~ 5장, 실제는 최대 50장)
const MANUSCRIPT_COUNT = parseInt(__ENV.MANUSCRIPT_COUNT || '3');

// ── 부하 시나리오 ───────────────────────────────────────────────
export const options = {
    discardResponseBodies: true,
    scenarios: {
        ramp_up: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 5  },
                { duration: '1m',  target: 20 },
                { duration: '2m',  target: 30 },
                { duration: '30s', target: 0  },
            ],
        },
    },
    thresholds: {
        episode_create_duration:          ['p(95)<3000', 'p(99)<5000'],
        total_flow_duration:              ['p(95)<60000'],
        s3_upload_duration:               ['p(95)<5000'],
        manuscript_s3_duration:           ['p(95)<5000'],
        error_rate:                       ['rate<0.05'],
    },
};

// ── 더미 JPEG (1x1 흰색 픽셀) ─────────────────────────────────
const TINY_JPEG_B64 =
    '/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8U' +
    'HRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgN' +
    'DRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIy' +
    'MjL/wAARCAABAAEDASIAAhEBAxEB/8QAFAABAAAAAAAAAAAAAAAAAAAACf/EABQQAQAAAAAA' +
    'AAAAAAAAAAAAAAD/xAAUAQEAAAAAAAAAAAAAAAAAAAAA/8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/' +
    'aAAwDAQACEQMRAD8AJQAB/9k=';

function makeDummyJpeg(name) {
    const bytes = encoding.b64decode(TINY_JPEG_B64, 'std', 'b');
    return { body: bytes, filename: `${name}_${Date.now()}.jpg`, size: bytes.byteLength };
}

// ── HTTP 래퍼 ──────────────────────────────────────────────────
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

function authHeaders(token) {
    return { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` };
}

// ── VU별 토큰 캐시 ─────────────────────────────────────────────
const tokenCache = {};

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
    if (!ok) fail(`로그인 실패: status=${res.status}`);
    return JSON.parse(res.body).accessToken;
}

function getToken() {
    if (!tokenCache[__VU]) tokenCache[__VU] = login();
    return tokenCache[__VU];
}

function withAuth(fn) {
    let token = getToken();
    let res = fn(token);
    if (res && res.status === 401) {
        console.warn(`[VU ${__VU}] 401 → 재로그인`);
        delete tokenCache[__VU];
        token = getToken();
        res = fn(token);
    }
    return res;
}

// ── 썸네일 업로드 (presigned → S3 → READY) ─────────────────────
// POST /api/files/episode-thumbnails/presigned
function requestEpisodeThumbnailPresigned(token, thumbnailType, jpeg) {
    const res = timedPost(
        presignedDuration,
        `${BASE_URL}/api/files/episode-thumbnails/presigned`,
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
    if (!ok) fail(`썸네일 presigned 실패 [${thumbnailType}]: status=${res.status}`);
    const body = JSON.parse(res.body);
    return { uploadUrl: body.uploadUrl, fileObjectId: body.fileObjectId };
}

function uploadToS3(uploadUrl, jpeg) {
    const res = timedPut(
        s3UploadDuration,
        uploadUrl,
        jpeg.body,
        { headers: { 'Content-Type': 'image/jpeg' } },
    );
    const ok = check(res, { '[s3 thumbnail] status 200': (r) => r.status === 200 });
    errorRate.add(!ok);
    if (!ok) fail(`S3 썸네일 업로드 실패: status=${res.status}`);
}

// POST /api/files/{fileObjectId}/thumbnails/ready
function markThumbnailReady(token, fileObjectId, label) {
    const res = timedPost(
        markReadyDuration,
        `${BASE_URL}/api/files/${fileObjectId}/thumbnails/ready`,
        null,
        { headers: authHeaders(token) },
    );
    const ok = check(res, { [`[ready:${label}] status 200`]: (r) => r.status === 200 });
    errorRate.add(!ok);
    if (!ok) fail(`READY 마킹 실패 [${label}]: status=${res.status}`);
}

// ── 원고 업로드 (presigned 일괄 → S3 병렬 → READY 일괄) ────────
// POST /api/files/manuscripts/presigned
function requestManuscriptPresigned(token, manuscripts) {
    const res = timedPost(
        manuscriptPresignedDuration,
        `${BASE_URL}/api/files/manuscripts/presigned`,
        JSON.stringify({
            creationId: CREATION_ID,
            files: manuscripts.map((m, i) => ({
                displayOrder:     i + 1,
                contentType:      'image/jpeg',
                originalFilename: m.filename,
                sizeBytes:        m.size,
            })),
        }),
        { headers: authHeaders(token), responseType: 'text' },
    );
    const ok = check(res, {
        '[manuscript presigned] status 200':  (r) => r.status === 200,
        '[manuscript presigned] items 존재':  (r) => { try { return Array.isArray(JSON.parse(r.body).items); } catch { return false; } },
    });
    errorRate.add(!ok);
    if (!ok) fail(`원고 presigned 실패: status=${res.status}`);
    return JSON.parse(res.body).items; // [{ fileObjectId, presignedUrl }, ...]
}

function uploadManuscriptsToS3(presignedItems, manuscripts) {
    const requests = presignedItems.map((item, i) => [
        'PUT',
        item.presignedUrl,
        manuscripts[i].body,
        { headers: { 'Content-Type': 'image/jpeg' } },
    ]);
    const responses = http.batch(requests);
    responses.forEach((res, i) => {
        manuscriptS3Duration.add(res.timings.duration);
        const ok = check(res, { [`[s3 manuscript ${i + 1}] status 200`]: (r) => r.status === 200 });
        if (!ok) uploadFailCount.add(1);
        errorRate.add(!ok);
    });
    return responses;
}

// POST /api/files/manuscripts/ready
function markManuscriptsReady(token, fileObjectIds) {
    const res = timedPost(
        manuscriptMarkDuration,
        `${BASE_URL}/api/files/manuscripts/ready`,
        JSON.stringify({ fileObjectIds }),
        { headers: authHeaders(token), responseType: 'text' },
    );
    const ok = check(res, { '[manuscript ready] status 200': (r) => r.status === 200 });
    errorRate.add(!ok);
    if (!ok) fail(`원고 READY 마킹 실패: status=${res.status}`);
    return JSON.parse(res.body);
}

// ── 회차 등록 ──────────────────────────────────────────────────
// POST /api/episodes/create
function publishEpisode(token, episodeThumbnailFoid, snsThumbnailFoid, manuscriptItems, episodeNum) {
    const res = timedPost(
        createDuration,
        `${BASE_URL}/api/episodes/create`,
        JSON.stringify({
            creationId:          CREATION_ID,
            episodeNum:          episodeNum,
            title:               `부하테스트 ${episodeNum}화`,
            creatorNote:         `k6 부하 테스트 VU=${__VU} ITER=${__ITER}`,
            isCommentEnabled:    true,
            isPublic:            true,
            episodeFileObjectId: episodeThumbnailFoid,
            snsFileObjectId:     snsThumbnailFoid,
            manuscripts:         manuscriptItems,
        }),
        { headers: authHeaders(token), responseType: 'text' },
    );
    const ok = check(res, {
        '[create episode] status 200':     (r) => r.status === 200,
        '[create episode] episodeId 존재': (r) => { try { return !!JSON.parse(r.body).episodeId; } catch { return false; } },
    });
    errorRate.add(!ok);
    if (!ok) fail(`회차 등록 실패: status=${res.status} body=${res.body}`);
    return JSON.parse(res.body).episodeId;
}

// ── 메인 시나리오 ──────────────────────────────────────────────
export default function () {
    const flowStart = Date.now();

    // 회차 번호: VU별 범위 분리로 충돌 방지 (VU 1 → 1001, 1002, ...) 
    const episodeNum = EPISODE_NUM_START + (__VU * 1000) + __ITER;

    // 1) 토큰 확보
    getToken();
    sleep(0.3);

    // 2) 에피소드 썸네일 (202x120) presigned → S3 → READY
    const episodeJpeg = makeDummyJpeg('episode_thumb');
    const episodePresigned = withAuth((token) => requestEpisodeThumbnailPresigned(token, 'EPISODE', episodeJpeg));
    sleep(0.1);
    uploadToS3(episodePresigned.uploadUrl, episodeJpeg);
    sleep(0.1);
    withAuth((token) => markThumbnailReady(token, episodePresigned.fileObjectId, 'EPISODE'));
    sleep(0.1);

    // 3) SNS 썸네일 (600x315) presigned → S3 → READY
    const snsJpeg = makeDummyJpeg('sns_thumb');
    const snsPresigned = withAuth((token) => requestEpisodeThumbnailPresigned(token, 'SNS', snsJpeg));
    sleep(0.1);
    uploadToS3(snsPresigned.uploadUrl, snsJpeg);
    sleep(0.1);
    withAuth((token) => markThumbnailReady(token, snsPresigned.fileObjectId, 'SNS'));
    sleep(0.1);

    // 4) 원고 이미지 presigned 일괄 → S3 병렬 → READY 일괄
    const manuscripts = Array.from({ length: MANUSCRIPT_COUNT }, (_, i) => makeDummyJpeg(`manuscript_${i + 1}`));
    const presignedItems = withAuth((token) => requestManuscriptPresigned(token, manuscripts));
    sleep(0.1);

    uploadManuscriptsToS3(presignedItems, manuscripts);
    sleep(0.1);

    const fileObjectIds = presignedItems.map((item) => item.fileObjectId);
    withAuth((token) => markManuscriptsReady(token, fileObjectIds));
    sleep(0.1);

    // 5) 회차 등록
    const manuscriptItems = presignedItems.map((item, i) => ({
        fileObjectId: item.fileObjectId,
        displayOrder: i + 1,
    }));

    const episodeId = withAuth((token) => publishEpisode(
        token,
        episodePresigned.fileObjectId,
        snsPresigned.fileObjectId,
        manuscriptItems,
        episodeNum,
    ));

    totalFlowDuration.add(Date.now() - flowStart);
    console.log(`[VU ${__VU}] ITER=${__ITER} 회차 등록 완료 episodeId=${episodeId} episodeNum=${episodeNum}`);

    sleep(Math.random() * 2 + 1);
}
