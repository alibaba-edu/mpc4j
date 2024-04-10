package edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.replicate;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.circuit.z2.adder.Adder;
import edu.alibaba.mpc4j.common.circuit.z2.adder.AdderFactory;
import edu.alibaba.mpc4j.common.circuit.z2.adder.AdderFactory.AdderTypes;
import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.AbstractAbbThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.MatrixUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

/**
 * The abstract party of Replicated-sharing type conversion
 *
 * @author Feng Han
 * @date 2024/01/17
 */
public abstract class AbstractAby3ConvParty extends AbstractAbbThreePartyPto implements Aby3ConvParty {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractAby3ConvParty.class);
    /**
     * id of self
     */
    protected final int selfId;
    /**
     * adder type
     */
    protected final AdderTypes adderType;
    /**
     * adder instance
     */
    protected final Adder adder;
    /**
     * z2c party
     */
    protected final TripletZ2cParty z2cParty;
    /**
     * zl64c party
     */
    protected final TripletLongParty zl64cParty;
    /**
     * TripletProvider
     */
    protected final S3pcCrProvider crProvider;
    /**
     * integer circuit
     */
    public final Z2IntegerCircuit circuit;

    protected AbstractAby3ConvParty(TripletZ2cParty z2cParty, TripletLongParty zl64cParty, Aby3ConvConfig config) {
        super(Aby3ConvPtoDesc.getInstance(), z2cParty.getRpc(), z2cParty.leftParty(), z2cParty.rightParty(), config);
        this.selfId = z2cParty.getRpc().ownParty().getPartyId();
        this.z2cParty = z2cParty;
        this.zl64cParty = zl64cParty;
        crProvider = z2cParty.getTripletProvider().getCrProvider();
        circuit = new Z2IntegerCircuit(z2cParty);
        adderType = config.getAdderType();
        adder = AdderFactory.createAdder(config.getAdderType(), circuit);
    }

    @Override
    public void init(){
        z2cParty.init();
        zl64cParty.init();
    }

    @Override
    public TripletProvider getProvider(){
        return z2cParty.getTripletProvider();
    }

    @Override
    public TripletZ2cParty getZ2cParty(){
        return z2cParty;
    }

    @Override
    public TripletLongParty getZl64cParty(){
        return zl64cParty;
    }

    @Override
    public TripletRpZ2Vector[] a2b(MpcLongVector data, int bitNum) throws MpcAbortException {
        Preconditions.checkArgument(bitNum <= 64);
        if (bitNum == 1) {
            return new TripletRpZ2Vector[]{MatrixUtils.shiftOneBit((TripletRpLongVector) data)};
        }
        TripletRpZ2Vector[][] twoBinary = transIntoSumOfTwoBinary(data, bitNum);
        MpcZ2Vector[] tmp = adder.add(twoBinary[0], twoBinary[1], false);
        return IntStream.range(1, tmp.length).mapToObj(i -> (TripletRpZ2Vector)tmp[i]).toArray(TripletRpZ2Vector[]::new);
    }

    /**
     * transfer the arithmetic sharing into the sum of two binary sharing
     *
     * @param data   data
     * @param bitNum the number of bit should be converted
     * @throws MpcAbortException the protocol failure aborts.
     */
    abstract TripletRpZ2Vector[][] transIntoSumOfTwoBinary(MpcLongVector data, int bitNum) throws MpcAbortException;

    @Override
    public TripletRpZ2Vector bitExtraction(MpcLongVector a, int index) throws MpcAbortException {
        if(index == 63){
            return a2b(a, 1)[0];
        }
        TripletRpZ2Vector[][] data = transIntoSumOfTwoBinary(a, 64 - index);
        if(index == 62){
            return z2cParty.xor(z2cParty.xor(z2cParty.and(data[0][1], data[1][1]), data[0][0]), data[1][0]);
        }
        int dim = data[0].length;
        if (adderType.equals(AdderTypes.RIPPLE_CARRY)) {
            return (TripletRpZ2Vector) adder.add(data[0], data[1], false)[0];
        }
        // change the order
        TripletRpZ2Vector[][] in = new TripletRpZ2Vector[2][dim - 1];
        for (int i = 0; i < dim - 1; i++) {
            in[0][i] = data[0][dim - 1 - i];
            in[1][i] = data[1][dim - 1 - i];
        }
        int[][] plan = addPlan(dim - 1);
        // P[i:i] = A[i] ^ B[i]
        // G[i:i] = A[i] & B[i]
        TripletRpZ2Vector[] g = (TripletRpZ2Vector[]) z2cParty.and(in[0], in[1]);
        z2cParty.xori(in[0], in[1]);
        in[1] = null;
        // P[i] = P[0:i] = P[j:i] & P[0:j-1].
        // G[i] = G[0:i] = G[j:i] or (G[0:j-1] & P[j:i])
        //               = G[j:i] ^  (G[0:j-1] & P[j:i])
        for (int i = 0; i < plan.length; i++) {
            int step = 1 << i;
            int bigStep = step<<1;
            int[] currentPlan = plan[i];
            int andNum = (currentPlan.length <<1) - 1;
            TripletRpZ2Vector[] left = new TripletRpZ2Vector[andNum];
            TripletRpZ2Vector[] right = new TripletRpZ2Vector[andNum];
            // get the input for AND
            for (int targetIndex = 0; targetIndex < currentPlan.length; targetIndex++) {
                int pTarget = currentPlan[targetIndex];
                int resNum = (pTarget + 1) % bigStep;
                int shouldBe = resNum == 0 ? pTarget - step : pTarget / step * step - 1;
                left[targetIndex] = in[0][pTarget];
                right[targetIndex] = g[shouldBe];
                if(targetIndex > 0){
                    left[targetIndex + currentPlan.length - 1] = in[0][pTarget];
                    right[targetIndex + currentPlan.length - 1] = in[0][shouldBe];
                }
            }
            // update values
            TripletRpZ2Vector[] andRes = (TripletRpZ2Vector[]) z2cParty.and(left, right);
            for (int j = 0; j < currentPlan.length; j++) {
                z2cParty.xori(g[currentPlan[j]], andRes[j]);
            }
            for (int j = 1; j < currentPlan.length; j++) {
                in[0][currentPlan[j]] = andRes[j + currentPlan.length - 1];
            }
        }
        TripletRpZ2Vector lastSign = z2cParty.xor(data[0][0], data[1][0]);
        z2cParty.xori(lastSign, g[dim - 2]);
        return lastSign;
    }

    /**
     * get the adder plan
     *
     * @param len   the length of bits
     */
    private int[][] addPlan(int len) {
        MathPreconditions.checkGreaterOrEqual("len >= 2", len, 2);
        int level = LongUtils.ceilLog2(len);
        // record what should be computed in each step: p and g
        int[][] res = new int[level][];
        for (int i = 0; i < level - 1; i++) {
            int step = 1 << i;
            int bigSkip = step << 1;
            // g, need g[l - 1]
            int fullNum = len / bigSkip;
            boolean resFlag = len % bigSkip > step;
            int gNum = fullNum + (resFlag ? 1 : 0);
            res[i] = new int[gNum];
            for(int j = 0; j < fullNum; j++){
                res[i][j] = (j + 1) * bigSkip - 1;
            }
            if(resFlag){
                res[i][fullNum] = len - 1;
            }
        }
        res[level - 1] = new int[]{len - 1};
        return res;
    }
}
