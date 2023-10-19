package edu.alibaba.mpc4j.s2pc.pir.payable;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * abstract payable PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/9/7
 */
public abstract class AbstractPayablePirClient extends AbstractTwoPartyPto implements PayablePirClient {
    /**
     * server element size
     */
    protected int serverElementSize;
    /**
     * value byte length
     */
    protected int valueByteLength;
    /**
     * bot element bytebuffer
     */
    protected ByteBuffer botElementByteBuffer;
    /**
     * client retrieval key list
     */
    protected ByteBuffer retrievalKey;

    protected AbstractPayablePirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, PayablePirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int serverElementSize, int valueByteLength) {
        MathPreconditions.checkPositive("labelByteLength", valueByteLength);
        this.serverElementSize = serverElementSize;
        MathPreconditions.checkPositive("valueByteLength", valueByteLength);
        this.valueByteLength = valueByteLength;
        byte[] botElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        initState();
    }

    protected void setPtoInput(ByteBuffer retrievalKey) {
        checkInitialized();
        this.retrievalKey = retrievalKey;
        extraInfo++;
    }
}
