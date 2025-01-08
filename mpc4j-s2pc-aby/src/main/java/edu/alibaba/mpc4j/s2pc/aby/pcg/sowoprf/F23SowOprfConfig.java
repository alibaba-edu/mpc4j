package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F23SowOprfFactory.F23SowOprfType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F23WprfMatrixFactory.F23WprfMatrixType;

/**
 * (F2, F3)-sowOPRF config.
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
public interface F23SowOprfConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    F23SowOprfType getPtoType();

    /**
     * Gets matrix type.
     *
     * @return matrix type.
     */
    F23WprfMatrixType getMatrixType();

    /**
     * Gets input length (in byte), where the input is x ∈ F_2^n.
     *
     * @return input length (in byte).
     */
    default int getInputByteLength() {
        return F23Wprf.getInputByteLength();
    }

    /**
     * Gets output length t, where the output is F_3^{t}.
     *
     * @return output length t.
     */
    default int getOutputLength() {
        return F23Wprf.getOutputLength();
    }
}
