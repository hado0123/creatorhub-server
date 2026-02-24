package com.creatorhub.dto.creation;

public record SeekCursor(
        Long id,        // tie-breaker
        Double value,   // sort value (views/likes는 Long을 Double로 담아도 됨)

        // 별점순만으로는 동점이 많이 나오므로 아래와 같이 별점순 정렬 규칙 지정
        // 1. ratingAverage DESC (가장 중요)
        // 2. ratingCount DESC (동점이면 평가 많은 게 위) => tie가 바로 2번 값인 ratingCount 를 의미
        // 3. id DESC (완전 동점이면 최신/큰 id가 위)
        Long tie
) {}
