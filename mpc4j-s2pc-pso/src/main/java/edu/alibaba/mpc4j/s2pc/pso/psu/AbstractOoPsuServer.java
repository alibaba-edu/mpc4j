package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * abstract Offline/Online PSU server.
 *
 * @author Feng Han
 * @date 2024/12/9
 */
public abstract class AbstractOoPsuServer extends AbstractPsuServer implements OoPsuServer {

    protected AbstractOoPsuServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, OoPsuConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void checkPrecomputeInput(int serverElementSize, int clientElementSize, int elementByteLength) {
        checkInitialized();
        MathPreconditions.checkGreater("serverElementSize", serverElementSize, 1);
        MathPreconditions.checkLessOrEqual("serverElementSize", serverElementSize, maxServerElementSize);
        MathPreconditions.checkGreater("clientElementSize", clientElementSize, 1);
        MathPreconditions.checkLessOrEqual("clientElementSize", clientElementSize, maxClientElementSize);
        MathPreconditions.checkGreaterOrEqual("elementByteLength", elementByteLength, CommonConstants.STATS_BYTE_LENGTH);
    }
}
