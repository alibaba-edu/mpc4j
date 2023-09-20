package edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;

/**
 * ALPR21 keyword PIR params.
 *
 * @author Liqiang Peng
 * @date 2023/7/4
 */
public class Alpr21KwPirParams implements KwPirParams {

    /**
     * keyword byte length
     */
    public int keywordPrfByteLength;
    /**
     * truncation byte length
     */
    public int truncationByteLength;
    /**
     * max retrieval size
     */
    public int maxRetrievalSize;

    public Alpr21KwPirParams(int keywordPrfByteLength, int truncationByteLength) {
        assert keywordPrfByteLength >= CommonConstants.STATS_BYTE_LENGTH && keywordPrfByteLength >= truncationByteLength;
        this.keywordPrfByteLength = keywordPrfByteLength;
        this.truncationByteLength = truncationByteLength;
    }

    /**
     * default params
     */
    public static Alpr21KwPirParams DEFAULT_PARAMS = new Alpr21KwPirParams(
        CommonConstants.BLOCK_BYTE_LENGTH, CommonConstants.STATS_BYTE_LENGTH
    );

    public void setMaxRetrievalSize(int maxRetrievalSize) {
        assert maxRetrievalSize > 0;
        this.maxRetrievalSize = maxRetrievalSize;
    }

    @Override
    public int maxRetrievalSize() {
        return maxRetrievalSize;
    }
}