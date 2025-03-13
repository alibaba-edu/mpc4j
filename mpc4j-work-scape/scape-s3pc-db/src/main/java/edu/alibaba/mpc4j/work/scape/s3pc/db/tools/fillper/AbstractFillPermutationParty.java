package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.AbstractThreePartyDbPto;

/**
 * Abstract FillPermutation Party
 *
 * @author Feng Han
 * @date 2025/2/17
 */
public abstract class AbstractFillPermutationParty extends AbstractThreePartyDbPto implements FillPermutationParty {
    /**
     * abb party
     */
    protected final Abb3Party abb3Party;

    public AbstractFillPermutationParty(PtoDesc ptoDesc, Abb3Party abb3Party, FillPermutationConfig config) {
        super(ptoDesc, abb3Party, config);
        this.abb3Party = abb3Party;
    }

    protected void checkInput(TripletLongVector index, TripletLongVector equalSign, int m){
        MathPreconditions.checkGreaterOrEqual("m", m, index.getNum());
        MathPreconditions.checkEqual("index.getNum()", "equalSign.getNum()", index.getNum(), equalSign.getNum());
    }

    protected void checkInput(TripletLongVector index1, TripletLongVector equalSign1,
                              TripletLongVector index2, TripletLongVector equalSign2, int m){
        checkInput(index1, equalSign1, m);
        checkInput(index2, equalSign2, m);
    }
}
