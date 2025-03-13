package edu.alibaba.mpc4j.work.scape.s3pc.db.orderby;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.AbstractThreePartyDbPto;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.InputProcessUtils;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * Abstract order-by party
 *
 * @author Feng Han
 * @date 2025/3/4
 */
public abstract class AbstractOrderByParty extends AbstractThreePartyDbPto implements OrderByParty {
    /**
     * binary input
     */
    protected TripletZ2Vector[] bInput;
    /**
     * arithmetic input
     */
    protected TripletLongVector[] aInput;
    /**
     * the indexes of thes order-by key
     */
    protected int[] keyIndex;

    protected AbstractOrderByParty(PtoDesc ptoDesc, Abb3Party abb3Party, OrderByConfig config) {
        super(ptoDesc, abb3Party, config);
    }

    protected void preprocess(TripletZ2Vector[] table, int[] keyIndex) {
        for(int i = 0; i < keyIndex.length; i++) {
            MathPreconditions.checkInRange("keyIndex[" + i + "]", keyIndex[i], 0, table.length);
        }
        bInput = InputProcessUtils.reshapeInput(table, keyIndex);
        this.keyIndex = keyIndex;
    }

    protected void preprocess(TripletLongVector[] table, int[] keyIndex) {
        for(int i = 0; i < keyIndex.length; i++) {
            MathPreconditions.checkInRange("keyIndex[" + i + "]", keyIndex[i], 0, table.length);
        }
        aInput = InputProcessUtils.reshapeInput(table, keyIndex);
        this.keyIndex = keyIndex;
    }

    protected void postprocess(TripletZ2Vector[] sortKey, TripletZ2Vector[] payload, TripletZ2Vector[] inputArray) {
        TIntSet keySet = new TIntHashSet();
        for (int key : keyIndex) {
            keySet.add(key);
        }
        inputArray[bInput.length - 1] = sortKey[0];
        for (int i = 0; i < keyIndex.length; i++) {
            inputArray[keyIndex[i]] = sortKey[i + 1];
        }
        int sourceInd = 0;
        for(int targetInd = 0; targetInd < bInput.length - 1; targetInd++) {
            if (!keySet.contains(targetInd)) {
                inputArray[targetInd] = payload[sourceInd];
                sourceInd++;
            }
        }
        assert sourceInd == payload.length;
    }

    protected void postprocess(TripletLongVector[] sortKey, TripletLongVector[] payload, TripletLongVector[] inputArray) {
        TIntSet keySet = new TIntHashSet();
        for (int key : keyIndex) {
            keySet.add(key);
        }
        inputArray[aInput.length - 1] = sortKey[0];
        for (int i = 0; i < keyIndex.length; i++) {
            inputArray[keyIndex[i]] = sortKey[i + 1];
        }
        int sourceInd = 0;
        for(int targetInd = 0; targetInd < aInput.length - 1; targetInd++) {
            if (!keySet.contains(targetInd)) {
                inputArray[targetInd] = payload[sourceInd];
                sourceInd++;
            }
        }
        assert sourceInd == payload.length;
    }

}
