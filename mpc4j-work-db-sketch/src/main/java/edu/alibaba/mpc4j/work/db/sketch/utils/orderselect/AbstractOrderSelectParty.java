package edu.alibaba.mpc4j.work.db.sketch.utils.orderselect;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.AbstractThreePartyOpfPto;

/**
 * abstract order select party.
 */
public abstract class AbstractOrderSelectParty extends AbstractThreePartyOpfPto implements OrderSelectParty {

    protected AbstractOrderSelectParty(PtoDesc ptoDesc, Abb3Party abb3Party, OrderSelectConfig config) {
        super(ptoDesc, abb3Party, config);
    }

    protected void checkInput(TripletLongVector[] input, int[] bitLens, int[] range) {
        MathPreconditions.checkEqual("bitLens.length", "input.length", bitLens.length, input.length);
        for (int each : bitLens) {
            MathPreconditions.checkInRangeClosed("bit length", each, 1, 64);
        }
        checkInput(input, range);
    }

    protected void checkInput(MpcVector[] input, int[] range) {
        int num = input[0].getNum();
        for (MpcVector vector : input) {
            MathPreconditions.checkEqual("num", "vector.getNum()", num, vector.getNum());
        }
        MathPreconditions.checkInRange("range[0]", range[0], 0, num);
        MathPreconditions.checkInRangeClosed("range[1]", range[1], range[0] + 1, num);
    }
}
