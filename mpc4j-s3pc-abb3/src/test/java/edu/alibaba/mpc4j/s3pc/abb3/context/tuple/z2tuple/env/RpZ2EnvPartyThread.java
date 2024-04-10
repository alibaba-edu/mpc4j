package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.env;

import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.env.RpZ2EnvTest.DyadicOperator;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;

import java.util.Arrays;

/**
 * basic s3 z2 circuit thread
 *
 * @author Feng Han
 * @date 2024/01/30
 */
public class RpZ2EnvPartyThread extends Thread{
    private final RpZ2EnvParty party;

    private final DyadicOperator op;

    private PlainZ2Vector[] rightPlainData;

    private TripletRpZ2Vector[] leftShareData;

    private TripletRpZ2Vector[] rightShareData;

    private BitVector[][] result;

    RpZ2EnvPartyThread(RpZ2EnvParty party, DyadicOperator op) {
        this.party = party;
        this.op = op;
    }

    public void setData(BitVector[] originData, TripletRpZ2Vector[] shareData){
        int dim = originData.length / 2;
        rightPlainData = new PlainZ2Vector[dim];
        leftShareData = new TripletRpZ2Vector[dim];
        rightShareData = new TripletRpZ2Vector[dim];
        for(int i = 0; i < dim; i++){
            rightPlainData[i] = PlainZ2Vector.create(originData[i + dim]);
            leftShareData[i] = shareData[i];
            rightShareData[i] = shareData[i + dim];
        }
    }

    public BitVector[][] getResult(){
        return result;
    }

    @Override
    public void run() {
        try {
            party.init();
            switch (op){
                case AND:{
                    TripletRpZ2Vector[] tmp = party.and(leftShareData, rightShareData);
                    result = new BitVector[][]{party.open(tmp)};
                    break;
                }
                case ANDI:{
                    party.andi(leftShareData, rightPlainData);
                    result = new BitVector[][]{party.open(leftShareData)};
                    break;
                }
                case XORI:{
                    TripletRpZ2Vector[] leftShareCopy = Arrays.stream(leftShareData).map(TripletRpZ2Vector::copy).toArray(TripletRpZ2Vector[]::new);
                    party.xori(leftShareCopy, rightPlainData);
                    party.xori(leftShareData, rightShareData);
                    result = new BitVector[][]{party.open(leftShareCopy), party.open(leftShareData)};
                    break;
                }
                case XOR:{
                    TripletRpZ2Vector[] tmp = party.xor(leftShareData, rightShareData);
                    result = new BitVector[][]{party.open(tmp)};
                    break;
                }
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
