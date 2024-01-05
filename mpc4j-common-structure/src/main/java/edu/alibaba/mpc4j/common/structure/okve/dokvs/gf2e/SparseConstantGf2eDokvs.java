package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

/**
 * constant sparse GF(2^e) DOKVS. The positions can be split into the sparse part and the dense part, and the sparse
 * part contains constant positions.
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
public interface SparseConstantGf2eDokvs<T> extends SparseGf2eDokvs<T> {
    /**
     * Gets the sparse num.
     *
     * @return the sparse num.
     */
    int sparsePositionNum();

    /**
     * Gets the maximal sparse num. Constant sparse DOKVS has constant sparse num. Therefore, max sparse position num
     * equals to sparse position num.
     *
     * @return the maximal sparse num.
     */
    @Override
    default int maxSparsePositionNum() {
        return sparsePositionNum();
    }
}
