package edu.alibaba.mpc4j.work.payable.pir;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.nio.ByteBuffer;

/**
 * Abstract payable PIR client.
 *
 * @author Liqiang Peng
 * @date 2024/7/2
 */
public abstract class AbstractPayablePirClient extends AbstractTwoPartyPto implements PayablePirClient {
    /**
     * server element size
     */
    protected int n;
    /**
     * value byte length
     */
    protected int byteL;
    /**
     * client retrieval key list
     */
    protected ByteBuffer retrievalKey;

    protected AbstractPayablePirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, PayablePirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int n, int byteL) {
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        MathPreconditions.checkPositive("byteL", byteL);
        this.byteL = byteL;
        initState();
    }

    protected void setPtoInput(ByteBuffer retrievalKey) {
        checkInitialized();
        this.retrievalKey = retrievalKey;
        extraInfo++;
    }
}

