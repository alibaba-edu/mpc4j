package edu.alibaba.mpc4j.s2pc.pir.index.single;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * Abstract Single Index PIR client.
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public abstract class AbstractSingleIndexPirClient extends AbstractTwoPartyPto implements SingleIndexPirClient {
    /**
     * element bit length
     */
    protected int elementBitLength;
    /**
     * database size
     */
    protected int num;
    /**
     * partition byte length
     */
    protected int partitionByteLength;
    /**
     * partition bit-length
     */
    protected int partitionBitLength;
    /**
     * partition size
     */
    public int partitionSize;

    protected AbstractSingleIndexPirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty,
                                           SingleIndexPirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int num, int elementBitLength) {
        MathPreconditions.checkPositive("elementBitLength", elementBitLength);
        this.elementBitLength = elementBitLength;
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        initState();
    }

    protected void setPtoInput(int index) {
        checkInitialized();
        MathPreconditions.checkNonNegativeInRange("index", index, num);
        extraInfo++;
    }
}