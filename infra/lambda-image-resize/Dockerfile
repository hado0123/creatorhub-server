FROM public.ecr.aws/lambda/nodejs:20

# sharp + AWS SDK 설치
RUN npm install --omit=dev sharp @aws-sdk/client-s3

# 코드 복사
COPY index.js ${LAMBDA_TASK_ROOT}/index.js

# 핸들러 지정 (index.js의 exports.handler)
CMD [ "index.handler" ]