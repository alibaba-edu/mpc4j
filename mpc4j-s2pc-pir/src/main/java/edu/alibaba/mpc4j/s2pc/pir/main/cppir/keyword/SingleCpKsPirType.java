package edu.alibaba.mpc4j.s2pc.pir.main.cppir.keyword;

/**
 * client-preprocessing KSPIR type.
 *
 * @author Liqiang Peng
 * @date 2023/9/27
 */
enum SingleCpKsPirType {
    /**
     * PAI
     */
    PAI_CKS,
    /**
     * ALPR21 + PAI
     */
    ALPR21_PAI,
    /**
     * ALPR21 + SPAM
     */
    ALPR21_SPAM,
    /**
     * ALPR21 + PIANO
     */
    ALPR21_PIANO,
    /**
     * ALPR21 + SIMPLE
     */
    ALPR21_SIMPLE,
}
