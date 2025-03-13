package edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.AbstractThreePartyOpfPto;

/**
 * @author Feng Han
 * @date 2025/2/18
 */
public abstract class AbstractPermuteParty extends AbstractThreePartyOpfPto implements PermuteParty {

    protected AbstractPermuteParty(PtoDesc ptoDesc, Abb3Party abb3Party, PermuteConfig config) {
        super(ptoDesc, abb3Party, config);
    }

    protected void checkInput(TripletLongVector pai, TripletLongVector... sigma) {
        checkInitialized();
        Preconditions.checkArgument(sigma != null && pai != null);
        MathPreconditions.checkEqual("sigma.getNum()", "pai.getNum()", sigma[0].getNum(), pai.getNum());
    }

    protected void checkInput(TripletZ2Vector[] pai, TripletZ2Vector[] x) {
        checkInitialized();
        Preconditions.checkArgument(pai != null && x != null);
        MathPreconditions.checkEqual("x[0].getNum()", "pai[0].getNum()", pai[0].getNum(), x[0].getNum());
    }

    protected void checkInput(TripletLongVector pai, TripletZ2Vector[] x) {
        checkInitialized();
        Preconditions.checkArgument(pai != null && x != null);
        MathPreconditions.checkEqual("x[0].getNum()", "pai.getNum()", pai.getNum(), x[0].getNum());
    }
}
