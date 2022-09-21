package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * NC-2^l选1-OT协议发送方抽象类。
 *
 * @author Hanwen Feng
 * @date 2022/08/16
 */
public abstract class AbstractNcLotSender extends AbstractSecureTwoPartyPto implements NcLotSender {
    /**
     * 配置项
     */
    private final NcLotConfig config;
    /**
     * 数量
     */
    protected int num;
    /**
     * 输入比特长度
     */
    protected int inputBitLength;
    /**
     * 输入字节长度
     */
    protected int inputByteLength;

    protected AbstractNcLotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, NcLotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public NcLotFactory.NcLotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int inputBitLength, int num) {
        assert inputBitLength > 0: "input bit length must be greater than 0: " + inputBitLength;
        this.inputBitLength = inputBitLength;
        inputByteLength = CommonUtils.getByteLength(inputBitLength);
        assert num > 0 && num <= config.maxAllowNum() : "num must be in range: (0, " + config.maxAllowNum() + "]: " + num;
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
