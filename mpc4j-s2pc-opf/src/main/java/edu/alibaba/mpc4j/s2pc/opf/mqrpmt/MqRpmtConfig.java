package edu.alibaba.mpc4j.s2pc.opf.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory.MqRpmtType;

/**
 * multi-query RPMT config.
 *
 * @author Weiran Liu
 * @date 2022/9/10
 */
public interface MqRpmtConfig extends MultiPartyPtoConfig {
    /**
     * Gets type.
     *
     * @return type.
     */
    MqRpmtType getPtoType();

    /**
     * Gets output vector length.
     *
     * @param serverElementSize server element size.
     * @param clientElementSize client element size.
     * @return output vector length.
     */
    int getVectorLength(int serverElementSize, int clientElementSize);
}
