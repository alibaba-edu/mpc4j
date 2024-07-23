package edu.alibaba.mpc4j.work.payable.pir;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Abstract payable PIR server.
 *
 * @author Liqiang Peng
 * @date 2024/7/2
 */
public abstract class AbstractPayablePirServer extends AbstractTwoPartyPto implements PayablePirServer {
    /**
     * keyword list
     */
    protected List<ByteBuffer> keywordList;
    /**
     * server element size
     */
    protected int n;
    /**
     * label byte length
     */
    protected int byteL;

    protected AbstractPayablePirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, PayablePirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(Map<ByteBuffer, byte[]> keyValueMap, int byteL) {
        MathPreconditions.checkPositive("byteL", byteL);
        this.byteL = byteL;
        MathPreconditions.checkPositive("n", keyValueMap.size());
        n = keyValueMap.size();
        Iterator<Map.Entry<ByteBuffer, byte[]>> iterator = keyValueMap.entrySet().iterator();
        keywordList = new ArrayList<>();
        while (iterator.hasNext()) {
            Map.Entry<ByteBuffer, byte[]> entry = iterator.next();
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
