package edu.alibaba.mpc4j.s2pc.pir.keyword;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * abstract keyword PIR client.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public abstract class AbstractKwPirClient extends AbstractTwoPartyPto implements KwPirClient {
    /**
     * server element size
     */
    protected int serverElementSize;
    /**
     * max client retrieval size
     */
    protected int maxRetrievalSize;
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
    protected List<ByteBuffer> retrievalKeyList;
    /**
     * client retrieval size
     */
    protected int retrievalKeySize;

    protected AbstractKwPirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, KwPirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int maxRetrievalSize, int serverElementSize, int valueByteLength) {
        MathPreconditions.checkPositive("labelByteLength", valueByteLength);
        this.serverElementSize = serverElementSize;
        MathPreconditions.checkPositive("valueByteLength", valueByteLength);
        this.valueByteLength = valueByteLength;
        MathPreconditions.checkPositive("maxRetrievalSize", maxRetrievalSize);
        this.maxRetrievalSize = maxRetrievalSize;
        byte[] botElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        initState();
    }

    protected void setPtoInput(Set<ByteBuffer> clientKeySet) {
        checkInitialized();
        retrievalKeySize = clientKeySet.size();
        MathPreconditions.checkPositiveInRangeClosed("retrievalSize", retrievalKeySize, maxRetrievalSize);
        retrievalKeyList = clientKeySet.stream()
            .map(ObjectUtils::objectToByteArray)
            .map(ByteBuffer::wrap)
            .peek(yi -> Preconditions.checkArgument(!yi.equals(botElementByteBuffer), "yi must not equal ⊥"))
            .collect(Collectors.toCollection(ArrayList::new));
        extraInfo++;
    }
}
