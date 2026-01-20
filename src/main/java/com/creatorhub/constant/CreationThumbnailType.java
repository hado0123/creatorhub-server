package com.creatorhub.constant;

public enum CreationThumbnailType implements ThumbnailType {
    POSTER {
        @Override
        public String resolveSuffix() {
            return ThumbnailKeys.POSTER_SUFFIX;
        }
    },
    HORIZONTAL {
        @Override
        public String resolveSuffix() {
            return ThumbnailKeys.HORIZONTAL_SUFFIX;
        }
    },
    DERIVED {
        @Override
        public String resolveSuffix() {
            return null;
        }
    },
    EXTRA {
        @Override
        public String resolveSuffix() {
            return null;
        }
    }
}

