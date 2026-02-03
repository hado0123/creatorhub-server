const { S3Client, GetObjectCommand, PutObjectCommand } = require('@aws-sdk/client-s3')
const { SQSClient, SendMessageCommand } = require('@aws-sdk/client-sqs')
const sharp = require('sharp')
const crypto = require('crypto')

/**
 * ENV
 * - BACKEND_CALLBACK_URL: https://api.example.com/api/files/resize-complete
 * - CALLBACK_SECRET: (optional) HMAC 서명용 secret
 * - CALLBACK_FAIL_QUEUE_URL: (optional) 백엔드 콜백 실패 시 재시도/추적용 SQS Queue URL
 * - AWS_REGION: (optional) 기본 리전
 */
const BACKEND_CALLBACK_URL = process.env.BACKEND_CALLBACK_URL
const CALLBACK_SECRET = process.env.CALLBACK_SECRET
const CALLBACK_FAIL_QUEUE_URL = process.env.CALLBACK_FAIL_QUEUE_URL

const s3 = new S3Client({})
const sqs = new SQSClient({})

/** 트리거 대상(가로형 대표)만 처리 */
const TRIGGER_SUFFIX = '_434x330.jpg'

/** 파생 이미지 6개 */
const DERIVED_SIZES = [
    { w: 83, h: 90 },
    { w: 98, h: 79 },
    { w: 125, h: 101 },
    { w: 202, h: 164 },
    { w: 217, h: 165 },
    { w: 218, h: 120 },
]

/** S3 GetObject Body(stream) → Buffer 변환 */
function streamToBuffer(stream) {
    return new Promise((resolve, reject) => {
        const chunks = []
        stream.on('data', (chunk) => chunks.push(chunk))
        stream.on('end', () => resolve(Buffer.concat(chunks)))
        stream.on('error', reject)
    })
}

/** SQS record -> S3 event record 추출 */
function extractS3RecordsFromSqsRecord(sqsRecord) {
    if (!sqsRecord?.body) return []
    let body
    try {
        body = JSON.parse(sqsRecord.body)
    } catch (e) {
        console.log('SQS 메시지 본문이 JSON 형식이 아닙니다.:', sqsRecord.body)
        return []
    }

    // S3 event notification 형식: { Records: [ { s3: { bucket: {name}, object:{key} } } ] }
    const s3Records = body?.Records
    if (!Array.isArray(s3Records)) {
        console.log('예상하지 못한 SQS 메시지 본문 형식입니다.:', body)
        return []
    }
    return s3Records
}

/** URL-encoded key decode */
function decodeS3Key(rawKey) {
    // key는 URL-encoded일 수 있음 (+, %2F 등)
    return decodeURIComponent(String(rawKey).replace(/\+/g, ' '))
}

/** HMAC 서명 */
function hmacSha256Hex(secret, data) {
    return crypto.createHmac('sha256', secret).update(data).digest('hex')
}


/** 백엔드가 에러 바디를 최대 12000글자까지만 줄임(SQS 256KB 사이즈 제한으로 인한 메세지 전송 실패 방지) */
function truncate(str, max = 8000) {
    if (!str) return str
    str = String(str)
    return str.length > max ? str.slice(0, max) + '...<truncated>' : str
}


/**
 * 백엔드 콜백
 * - 성공: true
 * - 실패: false (백엔드 실패시 throw가 되면 람다가 재시도를 진행하고 리사이징이 또 발생하므로 절대 throw 하지 않음)
 */
async function safeNotifyBackend(payload) {
    if (!BACKEND_CALLBACK_URL) return { ok: true }

    // 서명 검증
    if (!CALLBACK_SECRET) {
        console.error('CALLBACK_SECRET가 존해하지 않습니다. 백엔드 콜백을 실행하지 않습니다.')
        return {
            ok: false,
            errorName: 'MissingCallbackSecret',
            errorMessage: 'CALLBACK_SECRET is missing'
        }
    }

    const bodyStr = JSON.stringify(payload)
    const timestamp = Date.now().toString()
    const canonical = `${timestamp}.${bodyStr}`
    const signature = CALLBACK_SECRET ? hmacSha256Hex(CALLBACK_SECRET, canonical) : null

    try {
        const res = await fetch(BACKEND_CALLBACK_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Timestamp': timestamp,
                ...(signature ? { 'X-Signature': signature } : {}),
            },
            body: bodyStr,
        })

        // 응답 바디 읽기 (JSON or 텍스트)
        const contentType = res.headers.get('content-type') || ''
        let responseBody

        try {
            if (contentType.includes('application/json')) {
                responseBody = await res.json()
            } else {
                const text = await res.text()
                responseBody = truncate(text, 12000)
            }
        } catch (e) {
            responseBody = `응답 바디를 읽는데 실패했습니다.: ${e?.message || e}`
        }

        if (!res.ok) {
            // 람다 로그에 '백엔드가 준 에러' 찍기
            console.error('백엔드 콜백 실패(HTTP):', {
                url: BACKEND_CALLBACK_URL,
                status: res.status,
                statusText: res.statusText,
                responseBody,
            })
        }

        return {
            ok: res.ok,
            status: res.status,
            statusText: res.statusText,
            responseBody,
        }
    } catch (e) {
        // 네트워크/타임아웃/DNS 등 fetch 자체 실패
        console.error('백엔드 콜백 실패(fetch error):', e)

        return {
            ok: false,
            errorMessage: e?.message || String(e),
            errorName: e?.name,
            errorStack: truncate(e?.stack, 12000),
        }
    }
}


/**
 * 콜백 실패 시 SQS에 적재
 */
async function enqueueCallbackFail(payload, reason, backendDetail) {
    if (!CALLBACK_FAIL_QUEUE_URL) return

    try {
        const message = {
            reason: reason || 'BACKEND_NOTIFY_FAILED',
            payload,
            backend: backendDetail || null,
            failedAt: new Date().toISOString(),
        }

        await sqs.send(new SendMessageCommand({
            QueueUrl: CALLBACK_FAIL_QUEUE_URL,
            MessageBody: JSON.stringify(message),
        }))
        console.log('콜백 실패 메시지를 큐에 넣었습니다.')
    } catch (e) {
        console.error('콜백 실패 메시지를 큐에 넣지 못했습니다.:', e)
    }
}

exports.handler = async (event) => {
    try {
        const sqsRecords = event?.Records
        if (!Array.isArray(sqsRecords) || sqsRecords.length === 0) return

        for (const sqsRecord of sqsRecords) {
            const s3Records = extractS3RecordsFromSqsRecord(sqsRecord)
            if (s3Records.length === 0) continue

            for (const r of s3Records) {
                const bucket = r?.s3?.bucket?.name
                const rawKey = r?.s3?.object?.key
                if (!bucket || !rawKey) {
                    console.log('S3 이벤트 레코드에 버킷/키가 없습니다.:', r)
                    continue
                }

                const key = decodeS3Key(rawKey)

                // 434x330만 처리 (재귀/비용 방지)
                if (!key.endsWith(TRIGGER_SUFFIX)) continue

                console.log('Processing:', { bucket, key })

                // 원본(=가로 대표) 읽기
                const obj = await s3.send(new GetObjectCommand({ Bucket: bucket, Key: key }))
                const inputBuffer = await streamToBuffer(obj.Body)

                // baseKey: .../upload/2026/01/12/{uuid}  (뒤 _434x330.jpg 제거)
                const baseKey = key.replace(/_434x330\.jpg$/i, '')

                // 6개 파생 생성 + 업로드 (+ sizeBytes 포함해서 리턴)
                const derivedFiles = await Promise.all(
                    DERIVED_SIZES.map(async ({ w, h }) => {
                        const outKey = `${baseKey}_${w}x${h}.jpg`

                        const resizedBuffer = await sharp(inputBuffer)
                            .resize(w, h, { fit: 'cover' })
                            .jpeg({ quality: 85 })
                            .toBuffer()

                        await s3.send(
                            new PutObjectCommand({
                                Bucket: bucket,
                                Key: outKey,
                                Body: resizedBuffer,
                                ContentType: 'image/jpeg',
                                CacheControl: 'public, max-age=31536000, immutable',
                            })
                        )

                        const sizeBytes = resizedBuffer.length
                        console.log('리사이징 이미지 생성, 업로드 완료:', outKey, 'sizeBytes:', sizeBytes)

                        return {
                            key: outKey,
                            width: w,
                            height: h,
                            sizeBytes,
                        }
                    })
                )

                // 리사이징 성공 이후 백엔드 콜백 (콜백 실패해도 throw 하지 않는다)
                const payload = {
                    bucket,
                    triggerKey: key, // _434x330.jpg
                    baseKey,
                    derivedFiles,
                    resizedAt: new Date().toISOString(),
                }

                const result = await safeNotifyBackend(payload)

                if (!result.ok) {
                    const reason =
                        result.status ? `BACKEND_HTTP_${result.status}` : 'BACKEND_FETCH_ERROR'

                    await enqueueCallbackFail(payload, reason, result)
                }
            }
        }
    } catch (err) {
        console.error('리사이징 실패:', err)
        throw err // SQS 재시도 (리사이징/업로드 실패만 여기로 떨어지게 구성됨)
    }
}
