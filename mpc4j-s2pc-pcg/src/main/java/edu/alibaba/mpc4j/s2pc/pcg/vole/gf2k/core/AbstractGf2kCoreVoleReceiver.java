package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2kFactory;

/**
 * abstract GF2K-core-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/15
 */
public abstract class AbstractGf2kCoreVoleReceiver extends AbstractTwoPartyPto implements Gf2kCoreVoleReceiver {
    /**
     * field
     */
    protected Sgf2k field;
    /**
     * field L
     */
    protected int fieldL;
    /**
     * field byte L
     */
    protected int fieldByteL;
    /**
     * subfield
     */
    protected Gf2e subfield;
    /**
     * subfield L
     */
    protected int subfieldL;
    /**
     * subfield Byte L
     */
    protected int subfieldByteL;
    /**
     * r
     */
    protected int r;
    /**
     * Δ
     */
    protected byte[] delta;
    /**
     * num
     */
    protected int num;

    protected AbstractGf2kCoreVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2kCoreVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(int subfieldL, byte[] delta) {
        field = Sgf2kFactory.getInstance(envType, subfieldL);
        fieldL = field.getL();
        fieldByteL = field.getByteL();
        subfield = field.getSubfield();
        this.subfieldL = subfield.getL();
        subfieldByteL = subfield.getByteL();
        r = field.getR();
        Preconditions.checkArgument(field.validateElement(delta));
        this.delta = delta;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        extraInfo++;
    }
}
