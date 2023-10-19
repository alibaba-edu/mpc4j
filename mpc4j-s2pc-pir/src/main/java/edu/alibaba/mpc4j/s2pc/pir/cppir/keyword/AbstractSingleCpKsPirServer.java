package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * abstract single client-specific preprocessing KSPIR server.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public abstract class AbstractSingleCpKsPirServer<T> extends AbstractTwoPartyPto implements SingleCpKsPirServer<T> {
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
    /**
     * ByteBuffer for ⊥
     */
    protected ByteBuffer botByteBuffer;

    protected AbstractSingleCpKsPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty,
                                          SingleCpKsPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(Map<T, byte[]> keyValueMap, int valueBitLength) {
        MathPreconditions.checkPositive("value_bit_length", valueBitLength);
        l = valueBitLength;
        byteL = CommonUtils.getByteLength(l);
        MathPreconditions.checkPositive("# of keywords", keyValueMap.size());
        n = keyValueMap.size();
        byte[] bot = new byte[byteL];
        Arrays.fill(bot, (byte) 0xFF);
        BytesUtils.reduceByteArray(bot, l);
        botByteBuffer = ByteBuffer.wrap(bot);
        keyValueMap.forEach((keyword, value) -> {
            ByteBuffer keywordByteBuffer = ByteBuffer.wrap(ObjectUtils.objectToByteArray(keyword));
            Preconditions.checkArgument(!keywordByteBuffer.equals(botByteBuffer), "k_i must not equal ⊥");
            Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(value, byteL, l));
        });
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
