package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.AbstractThreePartyOpfPto;

import java.util.Arrays;

/**
 * the abstract oblivious sorting and permutation generation party
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public abstract class AbstractPgSortParty extends AbstractThreePartyOpfPto implements PgSortParty {

    protected AbstractPgSortParty(PtoDesc ptoDesc, Abb3Party abb3Party, PgSortConfig config) {
        super(ptoDesc, abb3Party, config);
    }

    protected void checkInput(TripletLongVector[] input, int[] bitLens, TripletZ2Vector[] saveSortRes) {
        MathPreconditions.checkEqual("bitLens.length", "input.length", bitLens.length, input.length);
        for (int each : bitLens) {
            MathPreconditions.checkInRangeClosed("bit length", each, 1, 64);
        }
        int totalBitNum = Arrays.stream(bitLens).sum();
        MathPreconditions.checkEqual("saveSortRes.length", "totalBitNum", saveSortRes.length, totalBitNum);
    }


}
