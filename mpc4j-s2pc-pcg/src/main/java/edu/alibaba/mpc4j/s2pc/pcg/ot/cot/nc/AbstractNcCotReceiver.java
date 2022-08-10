package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory.NcCotType;

/**
 * NC-COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/01/26
 */
public abstract class AbstractNcCotReceiver extends AbstractSecureTwoPartyPto implements NcCotReceiver {
    /**
     * 配置项
     */
    private final NcCotConfig config;
    /**
     * 数量
     */
    protected int num;

    protected AbstractNcCotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, NcCotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    @Override
    public NcCotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int num) {
        assert num > 0 && num <= config.maxAllowNum()
            : "num must be in range: (0, " + config.maxAllowNum() + "]: " + num;
        this.num = num;
        initialized = false;
    }

    protected void setPtoInput() {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        extraInfo++;
    }
}
