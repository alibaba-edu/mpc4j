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
import java.util.Arrays;

/**
 * abstract single client-specific preprocessing KSPIR client.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public abstract class AbstractSingleCpKsPirClient<T> extends AbstractTwoPartyPto implements SingleCpKsPirClient<T> {
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

    protected AbstractSingleCpKsPirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty,
                                          SingleCpKsPirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int n, int l) {
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        byte[] bot = new byte[byteL];
        Arrays.fill(bot, (byte) 0xFF);
        BytesUtils.reduceByteArray(bot, l);
        botByteBuffer = ByteBuffer.wrap(bot);
        initState();
    }

    protected void setPtoInput(T keyword) {
        checkInitialized();
        ByteBuffer keywordByteBuffer = ByteBuffer.wrap(ObjectUtils.objectToByteArray(keyword));
        Preconditions.checkArgument(!keywordByteBuffer.equals(botByteBuffer), "keyword must not equal ⊥");
        extraInfo++;
    }
}
