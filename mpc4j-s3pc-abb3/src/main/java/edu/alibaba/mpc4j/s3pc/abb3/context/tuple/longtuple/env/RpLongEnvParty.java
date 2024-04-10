package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.env;

import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.circuit.zlong.PlainLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.AbstractAbbThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.MatrixUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3.Aby3LongCpPtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.env.RpZ2EnvPtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.env.RpLongEnvPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * party of replicated 3p sharing zl64 basic environment
 *
 * @author Feng Han
 * @date 2024/01/24
 */
public class RpLongEnvParty extends AbstractAbbThreePartyPto {
    /**
     * id in three party computing
     */
    private final int selfId;
    /**
     * correlated randomness provider
     */
    private final S3pcCrProvider crProvider;

    public RpLongEnvParty(Rpc rpc, RpLongEnvConfig config, S3pcCrProvider crProvider) {
        super(RpZ2EnvPtoDesc.getInstance(), rpc, config);
        selfId = rpc.ownParty().getPartyId();
        this.crProvider = crProvider;
    }

    public void init() {
        if (partyState.equals(PartyState.INITIALIZED)) {
            return;
        }
        crProvider.init();
        initState();
    }

    public S3pcCrProvider getCrProvider() {
        return crProvider;
    }

    public LongVector[] open(MpcLongVector... xiArray) throws MpcAbortException {

        LongVector[] sendData = Arrays.stream(xiArray).map(x -> x.getVectors()[0]).toArray(LongVector[]::new);
        sendLongVectors(PtoStep.OPEN_SHARE.ordinal(), rightParty(), sendData);

        LongVector[] data = receiveLongVectors(PtoStep.OPEN_SHARE.ordinal(), leftParty());
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            data[i].addi(xiArray[i].getVectors()[0]);
            data[i].addi(xiArray[i].getVectors()[1]);
        });

        extraInfo++;
        compareView(data);
        return data;
    }

    /**
     * verify the data is the same by sending data to the right party. The input data should be formatted
     *
     * @param data to be checked
     * @throws MpcAbortException if the protocol is abort.
     */
    protected void compareView(LongVector... data) throws MpcAbortException {
        List<byte[]> hash = Collections.singletonList(crProvider.genHash(data));
        send(PtoStep.COMPARE_VIEW.ordinal(), rightParty(), hash);
        byte[] recData = receive(PtoStep.COMPARE_VIEW.ordinal(), leftParty()).get(0);
        if (!Arrays.equals(hash.get(0), recData)) {
            throw new MpcAbortException("data is not consistent");
        }
    }

    /**
     * verify the multiplication result is correct with tuples
     *
     * @param toBeVerified data to be verified in the form of [[x1, x2, ...], [y1, y2, ...], [z1, z2, ...]]
     * @param tuple        multiplication tuples in the form of [[a1, a2, ...], [b1, b2, ...], [c1, c2, ...]]
     * @throws MpcAbortException if the protocol is abort.
     */
    public void verifyMultipleGroup(TripletRpLongVector[][] toBeVerified, TripletRpLongVector[][] tuple) throws MpcAbortException {
        if (toBeVerified.length == 0) {
            return;
        }
        logPhaseInfo(PtoState.PTO_BEGIN);
        int arrayLen = toBeVerified[0].length;
        // 1. compute and open: rho = x - a, sigma = y - b; since the data won't be used again, we use in-place operation
        stopWatch.start();
        TripletRpLongVector[] rho = sub(toBeVerified[0], tuple[0]);
        TripletRpLongVector[] sigma = sub(toBeVerified[1], tuple[1]);
        LongVector[] openRes = open(MatrixUtils.flat(new TripletRpLongVector[][]{rho, sigma}));
        logStepInfo(PtoState.PTO_STEP, "verifyMultipleGroup", 1, 3, resetAndGetTime(), "compute and open rho, sigma");
        // 2. [delta] = [z] - [c] - sigma * [a] - rho * [b] - rho * sigma
        stopWatch.start();
        tuple[2] = sub(toBeVerified[2], tuple[2]);
        PlainLongVector[] openRho = IntStream.range(0, arrayLen).mapToObj(i ->
                PlainLongVector.create(openRes[i]))
            .toArray(PlainLongVector[]::new);
        PlainLongVector[] openSigma = IntStream.range(0, arrayLen).mapToObj(i ->
                PlainLongVector.create(openRes[i + arrayLen]))
            .toArray(PlainLongVector[]::new);
        muli(tuple[1], openRho);
        muli(tuple[0], openSigma);
        subi(tuple[2], tuple[0]);
        subi(tuple[2], tuple[1]);
        IntStream intStream = parallel ? IntStream.range(0, arrayLen).parallel() : IntStream.range(0, arrayLen);
        intStream.forEach(i -> openRho[i].getVectors()[0].muli(openSigma[i].getVectors()[0]));
        subi(tuple[2], openRho);
        logStepInfo(PtoState.PTO_STEP, "verifyMultipleGroup", 1, 3, resetAndGetTime(), "locally computation");
        // 3. format the values and verify [delta] = 0
        stopWatch.start();
        compareView4Zero(64, tuple[2]);
        logStepInfo(PtoState.PTO_STEP, "verifyMultipleGroup", 3, 3, resetAndGetTime(), "compare view for zero");
        logPhaseInfo(PtoState.PTO_END);
    }

    public void compareView4Zero(int validBitLen, TripletLongVector... data) throws MpcAbortException {
        // 1. generate hash for x1^x2, and send it to right party
        Stream<TripletLongVector> stream = parallel ? Arrays.stream(data).parallel() : Arrays.stream(data);
        LongVector[] addRes = stream.map(x -> {
            LongVector tmp = x.getVectors()[0].add(x.getVectors()[1]);
            tmp.format(validBitLen);
            return tmp;
        }).toArray(LongVector[]::new);
        byte[] addHash = crProvider.genHash(addRes);
        send(PtoStep.COMPARE_VIEW.ordinal(), rightParty(), Collections.singletonList(addHash));
        // 2. generate hash for x2, and compare it with the data from the left party
        byte[] x2Hash = crProvider.genHash(Arrays.stream(data).map(x -> {
            LongVector tmp = x.getVectors()[1].neg();
            tmp.format(validBitLen);
            return tmp;
        }).toArray(LongVector[]::new));
        byte[] recData = receive(PtoStep.COMPARE_VIEW.ordinal(), leftParty()).get(0);
        if (!Arrays.equals(x2Hash, recData)) {
            throw new MpcAbortException("data is not consistent");
        }
    }

    public TripletRpLongVector[] sub(TripletRpLongVector[] xiArray, TripletRpLongVector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        return intStream.mapToObj(i -> TripletRpLongVector.create(
            xiArray[i].getVectors()[0].sub(yiArray[i].getVectors()[0]),
            xiArray[i].getVectors()[1].sub(yiArray[i].getVectors()[1]))).toArray(TripletRpLongVector[]::new);
    }

    public void subi(TripletRpLongVector[] xiArray, MpcLongVector[] yiArray) {
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            if (yiArray[i].isPlain()) {
                if (selfId != 1) {
                    xiArray[i].getVectors()[selfId >> 1].subi(yiArray[i].getVectors()[0]);
                }
            } else {
                xiArray[i].getVectors()[0].subi(yiArray[i].getVectors()[0]);
                xiArray[i].getVectors()[1].subi(yiArray[i].getVectors()[1]);
            }
        });
    }

    public void muli(MpcLongVector[] xiArray, PlainLongVector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        for (int i = 0; i < xiArray.length; i++) {
            assert !xiArray[i].isPlain();
            assert xiArray[i].getNum() == yiArray[i].getNum();
        }
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            xiArray[i].getVectors()[0].muli(yiArray[i].getVectors()[0]);
            xiArray[i].getVectors()[1].muli(yiArray[i].getVectors()[0]);
        });
    }

    public TripletRpLongVector[] mul(TripletRpLongVector[] xiArray, TripletRpLongVector[] yiArray){
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        int[] bitNums = Arrays.stream(xiArray).mapToInt(MpcLongVector::getNum).toArray();
        LongVector[] zeroShares = crProvider.randZeroZl64Vector(bitNums);
        intStream.forEach(i -> {
            zeroShares[i].addi(xiArray[i].getVectors()[0].mul(yiArray[i].getVectors()[0]));
            zeroShares[i].addi(xiArray[i].getVectors()[1].mul(yiArray[i].getVectors()[0]));
            zeroShares[i].addi(xiArray[i].getVectors()[0].mul(yiArray[i].getVectors()[1]));
        });
        sendLongVectors(Aby3LongCpPtoDesc.PtoStep.MUL_OP.ordinal(), leftParty(), zeroShares);
        LongVector[] fromRight = receiveLongVectors(Aby3LongCpPtoDesc.PtoStep.MUL_OP.ordinal(), rightParty());
        extraInfo++;
        return IntStream.range(0, xiArray.length).mapToObj(i -> TripletRpLongVector.create(zeroShares[i], fromRight[i]))
            .toArray(TripletRpLongVector[]::new);
    }
}
