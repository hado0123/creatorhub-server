import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

/**
 * [회차 단일 조회 부하 테스트 - 특정 회차 집중]
 * 하나의 회차에 다수 사용자가 동시 접근하는 상황을 재현 (인기 회차 핫스팟 시뮬레이션)
 */

// 커스텀 메트릭
const errorRate = new Rate('error_rate');
const responseTime = new Trend('response_time', true);

// 부하 테스트 시나리오 설정
export const options = {
    scenarios: {
        ramp_up: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 50 },
                { duration: '1m',  target: 200 },
                { duration: '2m',  target: 300 },
                { duration: '30s', target: 0 },
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95%는 500ms 이하
        error_rate: ['rate<0.01'],                       // 에러율 1% 미만
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8000';

// 집중 테스트할 특정 회차 — 환경변수로 지정 (기본값: creationId=1, episodeId=1)
const CREATION_ID = parseInt(__ENV.CREATION_ID || '1');
const EPISODE_ID  = parseInt(__ENV.EPISODE_ID  || '1');

export default function () {
    const creationId = CREATION_ID;
    const episodeId  = EPISODE_ID;

    const url = `${BASE_URL}/api/episodes/${creationId}/detail/${episodeId}`;

    const res = http.get(url, {
        tags: { creationId: String(creationId), episodeId: String(episodeId) },
    });

    // 200 또는 404(존재하지 않는 회차)는 정상 케이스로 처리
    const success = check(res, {
        'status is 200':         (r) => r.status === 200,
        'response has content':  (r) => r.body && r.body.length > 0,
    });

    errorRate.add(!success);
    responseTime.add(res.timings.duration);

    // 200 응답이면 응답 구조 검증
    if (res.status === 200) {
        try {
            const body = JSON.parse(res.body);

            check(res, {
                'has episodeNum':           () => body.episodeNum != null,
                'has title':                () => typeof body.title === 'string',
                'has manuscriptImageUrls':  () => Array.isArray(body.manuscriptImageUrls),
            });
        } catch (_) {
            // JSON 파싱 실패는 무시
        }
    }
}
