package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.mrr20;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.AbstractThreePartyDbPto;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.hzf22.Hzf22PkPkJoinPtoDesc;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.RandomEncodingFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.RandomEncodingParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpOperations;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpOperations.PrpFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpParty;

import java.util.Arrays;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.mrr20.Mrr20RandomEncodingConfig.THRESHOLD_REDUCE;

/**
 * MRR20 randomized encoding party.
 *
 * @author Feng Han
 * @date 2025/2/25
 */
public class Mrr20RandomEncodingParty extends AbstractThreePartyDbPto implements RandomEncodingParty {
    /**
     * soprp party
     */
    protected final SoprpParty soprpParty;

    public Mrr20RandomEncodingParty(Abb3Party abb3Party, Mrr20RandomEncodingConfig config) {
        super(Hzf22PkPkJoinPtoDesc.getInstance(), abb3Party, config);
        soprpParty = SoprpFactory.createParty(abb3Party, config.getSoprpConfig());
        addMultiSubPto(soprpParty);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        soprpParty.init();
        abb3Party.init();
        initState();
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public long[] setUsage(RandomEncodingFnParam... params) {
        long[] tuple = new long[]{0, 0};
        if (isMalicious) {
            for (RandomEncodingFnParam param : params) {
                int totalSize = (CommonUtils.getByteLength(param.leftTableLen) + CommonUtils.getByteLength(param.rightTableLen)) << 3;
                long[] tmp = soprpParty.setUsage(new PrpFnParam(SoprpOperations.PrpOp.ENC, totalSize, THRESHOLD_REDUCE));
                tuple[0] += tmp[0];
                if (param.withDummy) {
                    long bitTuple = (long) param.keyDim * totalSize;
                    abb3Party.updateNum(bitTuple, 0);
                    tuple[0] += bitTuple;
                }
            }
        }
        return tuple;
    }

    @Override
    public TripletZ2Vector[][] getEncodingForTwoKeys(TripletZ2Vector[] leftKeys, TripletZ2Vector leftFlag,
                                                     TripletZ2Vector[] rightKeys, TripletZ2Vector rightFlag, boolean withDummy) throws MpcAbortException {
        Preconditions.checkArgument(leftKeys.length == rightKeys.length);
        logPhaseInfo(PtoState.PTO_BEGIN, "getEncodingForTwoKeys");

        stopWatch.start();
        TripletZ2Vector[] lowMcRes;
        TripletZ2Vector[] shouldEnc = new TripletZ2Vector[leftKeys.length + (withDummy ? 1 : 0)];
        int totalBitsWithFill = (leftFlag.byteNum() + rightFlag.byteNum()) << 3;
        IntStream.range(0, leftKeys.length).forEach(i ->
            shouldEnc[i] = (TripletZ2Vector) z2cParty.mergeWithPadding(new TripletZ2Vector[]{leftKeys[i], rightKeys[i]}));
        if (withDummy) {
            shouldEnc[leftKeys.length] = (TripletZ2Vector) z2cParty.mergeWithPadding(new TripletZ2Vector[]{leftFlag, rightFlag});
            TripletZ2Vector[] shareIndex = (TripletZ2Vector[]) z2cParty.setPublicValues(Z2VectorUtils.getBinaryIndex(totalBitsWithFill));
            TripletZ2Vector[] extendFlag = new TripletZ2Vector[shareIndex.length];
            Arrays.fill(extendFlag, shouldEnc[leftKeys.length]);
            TripletZ2Vector[] partLeftKey = Arrays.copyOf(shouldEnc, shareIndex.length);
            TripletZ2Vector[] part4UniqueKey = z2cParty.xor(z2cParty.and(z2cParty.xor(shareIndex, partLeftKey), extendFlag), shareIndex);
            System.arraycopy(part4UniqueKey, 0, shouldEnc, 0, part4UniqueKey.length);
        }

        TripletZ2Vector[] prpInput = shouldEnc;
        if (shouldEnc.length > THRESHOLD_REDUCE) {
            TripletZ2Vector[] hashRes = new TripletZ2Vector[THRESHOLD_REDUCE];
            int[] bitsArray = new int[hashRes.length];
            Arrays.fill(bitsArray, shouldEnc.length);
            TripletZ2Vector[] forHash = abb3Party.getTripletProvider().getCrProvider().randRpShareZ2Vector(bitsArray);
            BitVector[] openMatrix = z2cParty.open(forHash);
            // the matrix to reduce the length of key when key.length > THRESHOLD_REDUCE
            boolean[][] choiceMatrix = Arrays.stream(openMatrix).map(ea -> BinaryUtils.byteArrayToBinary(ea.getBytes(), ea.bitNum())).toArray(boolean[][]::new);

            IntStream intStream = parallel ? IntStream.range(0, choiceMatrix.length).parallel() : IntStream.range(0, choiceMatrix.length);
            intStream.forEach(i -> {
                hashRes[i] = z2cParty.createShareZeros(shouldEnc[0].bitNum());
                for (int k = 0; k < choiceMatrix[i].length; k++) {
                    if (choiceMatrix[i][k]) {
                        z2cParty.xori(hashRes[i], shouldEnc[k]);
                    }
                }
            });
            prpInput = hashRes;
        }
        if (soprpParty.getInputDim() < prpInput.length) {
            throw new MpcAbortException("soprpParty.getInputDim(): " + soprpParty.getInputDim() + " <= prpInput.length: " + prpInput.length);
        }
        lowMcRes = soprpParty.enc(prpInput);

        int leftBitLen = leftFlag.bitNum(), rightBitLen = rightFlag.bitNum();
        TripletZ2Vector[][] res = new TripletZ2Vector[2][lowMcRes.length];
        IntStream.range(0, lowMcRes.length).forEach(i -> {
            TripletZ2Vector[] tmp = (TripletZ2Vector[]) lowMcRes[i].splitWithPadding(new int[]{leftBitLen, rightBitLen});
            res[0][i] = tmp[0];
            res[1][i] = tmp[1];
        });
        logStepInfo(PtoState.PTO_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END, "getEncodingForTwoKeys");
        return res;
    }
}
