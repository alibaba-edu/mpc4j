package edu.alibaba.mpc4j.s2pc.pir.payable;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;

/**
 * abstract payable PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/9/7
 */
public abstract class AbstractPayablePirServer extends AbstractTwoPartyPto implements PayablePirServer {
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

    protected AbstractPayablePirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, PayablePirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(Map<ByteBuffer, byte[]> keywordLabelMap, int labelByteLength) {
        MathPreconditions.checkPositive("labelByteLength", labelByteLength);
        this.labelByteLength = labelByteLength;
        MathPreconditions.checkPositive("keywordNum", keywordLabelMap.size());
        keywordSize = keywordLabelMap.size();
        Iterator<Entry<ByteBuffer, byte[]>> iterator = keywordLabelMap.entrySet().iterator();
        keywordList = new ArrayList<>();
        while (iterator.hasNext()) {
            Entry<ByteBuffer, byte[]> entry = iterator.next();
            ByteBuffer item = entry.getKey();
            keywordList.add(item);
        }
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
