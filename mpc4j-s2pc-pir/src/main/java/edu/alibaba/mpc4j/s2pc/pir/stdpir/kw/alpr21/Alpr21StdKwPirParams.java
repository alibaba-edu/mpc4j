package edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.alpr21;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.StdKwPirParams;

/**
 * ALPR21 standard keyword PIR params.
 *
 * @author Liqiang Peng
 * @date 2023/7/4
 */
public class Alpr21StdKwPirParams implements StdKwPirParams {

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

    public Alpr21StdKwPirParams(int keywordPrfByteLength, int truncationByteLength) {
        assert keywordPrfByteLength >= CommonConstants.STATS_BYTE_LENGTH && keywordPrfByteLength >= truncationByteLength;
        this.keywordPrfByteLength = keywordPrfByteLength;
        this.truncationByteLength = truncationByteLength;
    }

    /**
     * default params
     */
    public static Alpr21StdKwPirParams DEFAULT_PARAMS = new Alpr21StdKwPirParams(
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