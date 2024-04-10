package edu.alibaba.mpc4j.s2pc.pir.keyword;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;

/**
 * abstract keyword PIR server.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public abstract class AbstractKwPirServer extends AbstractTwoPartyPto implements KwPirServer {
    /**
     * keyword list
     */
    protected List<ByteBuffer> keywordList;
    /**
     * server element size
     */
    protected int keywordSize;
    /**
     * label byte length
     */
    protected int labelByteLength;
    /**
     * bot element bytebuffer
     */
    protected ByteBuffer botElementByteBuffer;
    /**
     * max retrieval size
     */
    protected int maxRetrievalSize;

    protected AbstractKwPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, KwPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(Map<ByteBuffer, byte[]> keywordLabelMap, int maxRetrievalSize, int labelByteLength) {
        MathPreconditions.checkPositive("labelByteLength", labelByteLength);
        this.labelByteLength = labelByteLength;
        byte[] botElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte) 0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        MathPreconditions.checkPositive("keywordNum", keywordLabelMap.size());
        keywordSize = keywordLabelMap.size();
        Iterator<Entry<ByteBuffer, byte[]>> iterator = keywordLabelMap.entrySet().iterator();
        keywordList = new ArrayList<>();
        while (iterator.hasNext()) {
            Entry<ByteBuffer, byte[]> entry = iterator.next();
            ByteBuffer item = entry.getKey();
            Preconditions.checkArgument(!item.equals(botElementByteBuffer), "xi must not equal ⊥");
            keywordList.add(item);
        }
        MathPreconditions.checkPositive("maxRetrievalSize", maxRetrievalSize);
        this.maxRetrievalSize = maxRetrievalSize;
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
