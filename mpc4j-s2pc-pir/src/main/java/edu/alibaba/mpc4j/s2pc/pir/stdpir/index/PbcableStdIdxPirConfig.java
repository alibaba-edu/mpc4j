package edu.alibaba.mpc4j.s2pc.pir.stdpir.index;

/**
 * probabilistic batch code (PBC) index PIR config.
 *
 * @author Liqiang Peng
 * @date 2024/7/16
 */
public interface PbcableStdIdxPirConfig extends StdIdxPirConfig {
    /**
     * standard index PIR params.
     *
     * @return standard index PIR params.
     */
    StdIdxPirParams getStdIdxPirParams();
}
