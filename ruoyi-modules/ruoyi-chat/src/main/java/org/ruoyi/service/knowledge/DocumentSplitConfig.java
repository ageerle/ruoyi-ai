package org.ruoyi.service.knowledge;

import org.ruoyi.common.core.exception.ServiceException;

/** Immutable snapshot of the split settings used for one parse operation. */
public record DocumentSplitConfig(String separator, int blockSize, int overlap, String fileType) {

    public static final int DEFAULT_BLOCK_SIZE = 1000;
    public static final int DEFAULT_OVERLAP = 50;

    public DocumentSplitConfig {
        if (blockSize <= 0) {
            throw new ServiceException("文本块大小必须大于0");
        }
        if (overlap < 0 || overlap >= blockSize) {
            throw new ServiceException("重叠字符数必须大于等于0且小于文本块大小");
        }
        fileType = fileType == null ? "" : fileType.strip().replaceFirst("^\\.", "").toLowerCase();
    }
}
