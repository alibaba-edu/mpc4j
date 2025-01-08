package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32SowOprfFactory.F32SowOprfType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32WprfMatrixFactory.F32WprfMatrixType;

/**
 * (F3, F2)-sowOPRF config.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
public interface F32SowOprfConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    F32SowOprfType getPtoType();

    /**
     * Gets matrix type.
     *
     * @return matrix type.
     */
    F32WprfMatrixType getMatrixType();

    /**
     * Gets input length n, where the input is x ∈ F_3^n.
     *
     * @return input length.
     */
    default int getInputLength() {
        return F32Wprf.getInputLength();
    }

    /**
     * Gets output length t (in byte), where the output is F_2^{t}.
     *
     * @return output length t (in byte).
     */
    default int getOutputByteLength() {
        return F32Wprf.getOutputByteLength();
    }
}
