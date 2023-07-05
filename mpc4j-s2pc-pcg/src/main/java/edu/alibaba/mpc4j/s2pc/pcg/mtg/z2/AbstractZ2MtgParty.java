package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * abstract Z2 multiplication triple generation party.
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/02/07
 */
public abstract class AbstractZ2MtgParty extends AbstractTwoPartyPto implements Z2MtgParty {
    /**
     * config
     */
    protected final Z2MtgConfig config;
    /**
     * update num
     */
    protected long updateNum;
    /**
     * num
     */
    protected int num;
    /**
     * byte num
     */
    protected int byteNum;

    public AbstractZ2MtgParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, Z2MtgConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    protected void setInitInput(int updateNum) {
        MathPreconditions.checkPositive("updateNum", updateNum);
        this.updateNum = updateNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        byteNum = CommonUtils.getByteLength(num);
        extraInfo += num;
    }
}
