package edu.alibaba.mpc4j.work.payable.psi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Set;

/**
 * Abstract Payable PSI server.
 *
 * @author Liqiang Peng
 * @date 2024/7/1
 */
public abstract class AbstractPayablePsiServer extends AbstractTwoPartyPto implements PayablePsiServer {

    /**
     * max server element size
     */
    private int maxServerElementSize;
    /**
     * max client element size
     */
    private int maxClientElementSize;
    /**
     * server element list
     */
    protected ArrayList<ByteBuffer> serverElementArrayList;
    /**
     * server element size
     */
    protected int serverElementSize;
    /**
     * client element size
     */
    protected int clientElementSize;

    protected AbstractPayablePsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, PayablePsiConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(int maxServerElementSize, int maxClientElementSize) {
        MathPreconditions.checkPositive("maxServerElementSize", maxServerElementSize);
        this.maxServerElementSize = maxServerElementSize;
        MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
        this.maxClientElementSize = maxClientElementSize;
        initState();
    }

    protected void setPtoInput(Set<ByteBuffer> serverElementSet, int clientElementSize) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("serverElementSize", serverElementSet.size(), maxServerElementSize);
        serverElementSize = serverElementSet.size();
        serverElementArrayList = new ArrayList<>(serverElementSet);
        MathPreconditions.checkPositiveInRangeClosed("clientElementSize", clientElementSize, maxClientElementSize);
        this.clientElementSize = clientElementSize;
        extraInfo++;
    }
}