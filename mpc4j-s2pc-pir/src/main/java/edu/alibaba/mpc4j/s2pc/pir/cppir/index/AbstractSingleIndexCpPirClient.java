package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * abstract Single Index Client-specific Preprocessing PIR client.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public abstract class AbstractSingleIndexCpPirClient extends AbstractTwoPartyPto implements SingleIndexCpPirClient {
    /**
     * database size
     */
    protected int n;
    /**
     * value bit length
     */
    protected int l;
    /**
     * value byte length
     */
    protected int byteL;

    protected AbstractSingleIndexCpPirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty,
                                             SingleIndexCpPirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int n, int l) {
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        initState();
    }

    protected void setPtoInput(int x) {
        // extra info is managed by the protocol itself
        checkInitialized();
        MathPreconditions.checkNonNegativeInRange("index", x, n);
    }
}
