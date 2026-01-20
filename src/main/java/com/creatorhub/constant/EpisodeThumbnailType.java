package com.creatorhub.constant;

public enum EpisodeThumbnailType implements ThumbnailType {
    EPISODE {
        @Override
        public String resolveSuffix() {
            return ThumbnailKeys.EPISODE_SUFFIX;
        }
    },
    SNS {
        @Override
        public String resolveSuffix() {
            return ThumbnailKeys.SNS_SUFFIX;
        }
    }
}
