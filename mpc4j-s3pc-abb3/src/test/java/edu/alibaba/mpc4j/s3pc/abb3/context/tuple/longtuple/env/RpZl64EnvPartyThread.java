package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.env;

import edu.alibaba.mpc4j.common.circuit.zlong.PlainLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.env.RpZl64EnvTest.DyadicOperator;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

import java.util.Arrays;

/**
 * basic s3 zLong circuit thread
 *
 * @author Feng Han
 * @date 2024/01/30
 */
public class RpZl64EnvPartyThread extends Thread {
    private final RpLongEnvParty party;

    private final DyadicOperator op;

    private PlainLongVector[] rightPlainData;

    private TripletRpLongVector[] leftShareData;

    private TripletRpLongVector[] rightShareData;

    private LongVector[][] result;

    RpZl64EnvPartyThread(RpLongEnvParty party, DyadicOperator op) {
        this.party = party;
        this.op = op;
    }

    public void setData(LongVector[] originData, TripletRpLongVector[] shareData) {
        int dim = originData.length / 2;
        rightPlainData = new PlainLongVector[dim];
        leftShareData = new TripletRpLongVector[dim];
        rightShareData = new TripletRpLongVector[dim];
        for (int i = 0; i < dim; i++) {
            rightPlainData[i] = PlainLongVector.create(originData[i + dim]);
            leftShareData[i] = shareData[i];
            rightShareData[i] = shareData[i + dim];
        }
    }

    public LongVector[][] getResult() {
        return result;
    }

    @Override
    public void run() {
        try {
            party.init();
            switch (op) {
                case MUL: {
                    TripletRpLongVector[] tmp = party.mul(leftShareData, rightShareData);
                    result = new LongVector[][]{party.open(tmp)};
                    break;
                }
                case MULI: {
                    party.muli(leftShareData, rightPlainData);
                    result = new LongVector[][]{party.open(leftShareData)};
                    break;
                }
                case SUBI: {
                    TripletRpLongVector[] leftShareCopy = Arrays.stream(leftShareData)
                        .map(TripletRpLongVector::copy).toArray(TripletRpLongVector[]::new);
                    party.subi(leftShareCopy, rightPlainData);
                    party.subi(leftShareData, rightShareData);
                    result = new LongVector[][]{party.open(leftShareCopy), party.open(leftShareData)};
                    break;
                }
                case SUB: {
                    TripletRpLongVector[] tmp = party.sub(leftShareData, rightShareData);
                    result = new LongVector[][]{party.open(tmp)};
                    break;
                }
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
