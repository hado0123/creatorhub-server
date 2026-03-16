import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

/**
 * [작품 목록 조회 부하 테스트]
 * 요일별로 인기순(좋아요순), 조회순, 별점순 조회
 */

// 커스텀 메트릭
const errorRate = new Rate('error_rate');
const responseTime = new Trend('response_time', true);

// 부하 테스트 시나리오 설정
export const options = {
    scenarios: {
        // 점진적 부하 증가
        ramp_up: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 50 }, // 워밍업
                { duration: '1m',  target: 200 }, // 부하 증가
                { duration: '2m',  target: 300 }, // 최대 부하 유지
                { duration: '30s', target: 0 }, // 부하 감소
            ]
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95%는 500ms 이하
        error_rate: ['rate<0.01'],                       // 에러율 1% 미만
    },
};

// 요일 목록 (PublishDay enum 기준)
const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

// 정렬 기준 (CreationSort enum 기준)
const SORTS = ['VIEWS', 'POPULAR', 'RATING'];

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8000';

export default function () {
    // 랜덤으로 요일과 정렬 기준 선택
    const day  = DAYS[Math.floor(Math.random() * DAYS.length)];
    const sort = SORTS[Math.floor(Math.random() * SORTS.length)];

    const url = `${BASE_URL}/api/creations/by-days?day=${day}&sort=${sort}&size=21`;

    const res = http.get(url, {
        tags: { day, sort },
    });

    // 응답 검증
    const success = check(res, {
        'status is 200':         (r) => r.status === 200,
        'response has content':  (r) => r.body && r.body.length > 0
    });

    errorRate.add(!success);
    responseTime.add(res.timings.duration);

    // 페이지네이션 테스트: 첫 번째 응답에서 cursor 정보가 있으면 다음 페이지도 요청
    if (res.status === 200) {
        try {
            const body = JSON.parse(res.body);

            if (body.hasNext && body.nextCursor) {
                const cursor = body.nextCursor;
                let nextUrl = `${BASE_URL}/api/creations/by-days?day=${day}&sort=${sort}&size=21`;

                if (sort === 'RATING') {
                    nextUrl += `&cursorAvg=${cursor.value}&cursorRatingCount=${cursor.tie}&cursorId=${cursor.id}`;
                } else {
                    nextUrl += `&cursorValue=${cursor.value}&cursorId=${cursor.id}`;
                }

                const nextRes = http.get(nextUrl, {
                    tags: { day, sort, page: 'next' },
                });

                check(nextRes, {
                    'next page status is 200': (r) => r.status === 200,
                });

                errorRate.add(nextRes.status !== 200);
                responseTime.add(nextRes.timings.duration);
            }
        } catch (_) {
            // JSON 파싱 실패는 무시
        }
    }

    // sleep(Math.random() + 0.5); // 0.5 ~ 1.5초 랜덤 대기
}
