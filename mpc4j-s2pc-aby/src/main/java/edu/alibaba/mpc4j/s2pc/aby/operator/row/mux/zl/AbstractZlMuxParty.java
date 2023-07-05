package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * abstract Zl mux party.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public abstract class AbstractZlMuxParty extends AbstractTwoPartyPto implements ZlMuxParty {
    /**
     * max num
     */
    protected int maxNum;
    /**
     * num
     */
    protected int num;
    /**
     * Zl instance
     */
    protected Zl zl;
    /**
     * l in bytes
     */
    protected int byteL;

    public AbstractZlMuxParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlMuxConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(SquareZ2Vector xi, SquareZlVector yi) {
        MathPreconditions.checkEqual("xi.num", "yi.num", xi.getNum(), yi.getNum());
        num = xi.getNum();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        zl = yi.getZl();
        byteL = zl.getByteL();
    }
}
