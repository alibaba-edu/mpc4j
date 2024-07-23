package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * abstract no-choice GF2K-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
public abstract class AbstractGf2kNcVoleReceiver extends AbstractTwoPartyPto implements Gf2kNcVoleReceiver {
    /**
     * config
     */
    private final Gf2kNcVoleConfig config;
    /**
     * field
     */
    protected Sgf2k field;
    /**
     * subfield
     */
    protected Gf2e subfield;
    /**
     * subfield L
     */
    protected int subfieldL;
    /**
     * Δ
     */
    protected byte[] delta;
    /**
     * num
     */
    protected int num;

    protected AbstractGf2kNcVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2kNcVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput(int subfieldL, byte[] delta, int num) {
        field = Sgf2kFactory.getInstance(envType, subfieldL);
        subfield = field.getSubfield();
        this.subfieldL = subfieldL;
        Preconditions.checkArgument(field.validateElement(delta));
        this.delta = BytesUtils.clone(delta);
        MathPreconditions.checkPositiveInRangeClosed("num", num, config.maxNum());
        this.num = num;
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
