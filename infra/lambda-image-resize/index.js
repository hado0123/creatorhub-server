const { S3Client, GetObjectCommand, PutObjectCommand } = require('@aws-sdk/client-s3')
const sharp = require('sharp')

const s3 = new S3Client({})

// 트리거 대상(가로형 대표)만 처리
const TRIGGER_SUFFIX = '_434x330.jpg'

// 기타 이미지 6개
const DERIVED_SIZES = [
   { w: 83, h: 90 },
   { w: 98, h: 79 },
   { w: 125, h: 101 },
   { w: 202, h: 164 },
   { w: 217, h: 165 },
   { w: 218, h: 120 },
]

// S3 GetObject Body(stream) → Buffer 변환
function streamToBuffer(stream) {
   return new Promise((resolve, reject) => {
      const chunks = []
      stream.on('data', (chunk) => chunks.push(chunk))
      stream.on('end', () => resolve(Buffer.concat(chunks)))
      stream.on('error', reject)
   })
}

exports.handler = async (event) => {
   const record = event?.Records?.[0]
   if (!record) return

   const bucket = record.s3.bucket.name
   const key = decodeURIComponent(record.s3.object.key.replace(/\+/g, ' '))

   // 434x330만 처리 (재귀/비용 방지)
   if (!key.endsWith(TRIGGER_SUFFIX)) return

   // 원본(=가로 대표) 읽기
   const obj = await s3.send(new GetObjectCommand({ Bucket: bucket, Key: key }))
   const inputBuffer = await streamToBuffer(obj.Body)

   // baseKey: .../upload_1234567890  (뒤 _434x330.jpg 제거)
   const baseKey = key.replace(/_434x330\.jpg$/i, '')

   // 6개 파생 생성 + 업로드
   await Promise.all(
      DERIVED_SIZES.map(async ({ w, h }) => {
         const outKey = `${baseKey}_${w}x${h}.jpg`

         const resizedBuffer = await sharp(inputBuffer)
            .resize(w, h, { fit: 'cover' }) // 꽉 채우기(네이버 스타일)
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
      })
   )
}
