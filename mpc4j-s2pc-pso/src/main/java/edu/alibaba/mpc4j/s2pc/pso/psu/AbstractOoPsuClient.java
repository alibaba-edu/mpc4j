package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * abstract Offline/Online PSU client.
 *
 * @author Feng Han
 * @date 2024/12/9
 */
public abstract class AbstractOoPsuClient extends AbstractPsuClient implements OoPsuClient {

    protected AbstractOoPsuClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, OoPsuConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void checkPrecomputeInput(int clientElementSize, int serverElementSize, int elementByteLength) {
        checkInitialized();
        MathPreconditions.checkGreater("clientElementSize", clientElementSize, 1);
        MathPreconditions.checkLessOrEqual("clientElementSize", clientElementSize, maxClientElementSize);
        MathPreconditions.checkGreater("serverElementSize", serverElementSize, 1);
        MathPreconditions.checkLessOrEqual("serverElementSize", serverElementSize, maxServerElementSize);
        MathPreconditions.checkGreaterOrEqual("elementByteLength", elementByteLength, CommonConstants.STATS_BYTE_LENGTH);
    }
}
