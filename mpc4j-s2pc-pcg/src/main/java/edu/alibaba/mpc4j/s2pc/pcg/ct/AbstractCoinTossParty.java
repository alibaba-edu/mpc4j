package edu.alibaba.mpc4j.s2pc.pcg.ct;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * abstract coin-tossing protocol party.
 *
 * @author Weiran Liu
 * @date 2023/5/6
 */
public abstract class AbstractCoinTossParty extends AbstractTwoPartyPto implements CoinTossParty {
    /**
     * num
     */
    protected int num;
    /**
     * bit length for each coin
     */
    protected int bitLength;
    /**
     * byte length for each coin
     */
    protected int byteLength;

    protected AbstractCoinTossParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, CoinTossConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(int num, int bitLength) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        MathPreconditions.checkPositive("bitLength", bitLength);
        this.bitLength = bitLength;
        byteLength = CommonUtils.getByteLength(bitLength);
        extraInfo++;
    }
}
