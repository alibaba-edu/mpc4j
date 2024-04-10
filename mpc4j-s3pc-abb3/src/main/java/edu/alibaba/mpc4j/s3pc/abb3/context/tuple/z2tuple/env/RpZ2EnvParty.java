package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.env;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.AbstractAbbThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.MatrixUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.env.RpZ2EnvPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * party of replicated 3p sharing z2 basic environment
 *
 * @author Feng Han
 * @date 2024/01/24
 */
public class RpZ2EnvParty extends AbstractAbbThreePartyPto {
    /**
     * id in three party computing
     */
    private final int selfId;
    /**
     * correlated randomness provider
     */
    private final S3pcCrProvider crProvider;

    public RpZ2EnvParty(Rpc rpc, RpZ2EnvConfig config, S3pcCrProvider crProvider) {
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

    /**
     * open shared vectors.
     *
     * @param xiArray the clear vectors.
     */
    public BitVector[] open(TripletZ2Vector... xiArray) throws MpcAbortException {
        BitVector[] sendRightData = Arrays.stream(xiArray).map(x -> x.getBitVectors()[0]).toArray(BitVector[]::new);
        sendBitVectors(PtoStep.OPEN_SHARE.ordinal(), rightParty(), sendRightData);
        int[] bitNums = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::bitNum).toArray();
        BitVector[] data = receiveBitVectors(PtoStep.OPEN_SHARE.ordinal(), leftParty(), bitNums);
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            data[i].xori(xiArray[i].getBitVectors()[0]);
            data[i].xori(xiArray[i].getBitVectors()[1]);
        });
        extraInfo++;
        compareView(data);
        return data;
    }

    /**
     * verify the data is the same by sending data to the right party
     *
     * @param data to be checked
     * @throws MpcAbortException if the protocol is abort.
     */
    private void compareView(BitVector... data) throws MpcAbortException {
        List<byte[]> hash = Collections.singletonList(crProvider.genHash(data));
        send(PtoStep.COMPARE_VIEW.ordinal(), rightParty(), hash);
        byte[] recData = receive(PtoStep.COMPARE_VIEW.ordinal(), leftParty()).get(0);
        if (!Arrays.equals(hash.get(0), recData)) {
            throw new MpcAbortException("data is not consistent");
        }
    }

    /**
     * verify the data is the share of zeros
     *
     * @throws MpcAbortException if the protocol is abort.
     */
    public void compareView4Zero(TripletZ2Vector... data) throws MpcAbortException {
        // 1. generate hash for x1^x2, and send it to right party
        Stream<TripletZ2Vector> stream = parallel ? Arrays.stream(data).parallel() : Arrays.stream(data);
        BitVector[] xorRes = stream.map(x -> x.getBitVectors()[0].xor(x.getBitVectors()[1])).toArray(BitVector[]::new);
        byte[] xorHash = crProvider.genHash(xorRes);
        send(PtoStep.COMPARE_VIEW.ordinal(), rightParty(), Collections.singletonList(xorHash));
        // 2. generate hash for x2, and compare it with the data from the left party
        byte[] x2Hash = crProvider.genHash(Arrays.stream(data).map(x -> x.getBitVectors()[1]).toArray(BitVector[]::new));
        byte[] recData = receive(PtoStep.COMPARE_VIEW.ordinal(), leftParty()).get(0);
        if (!Arrays.equals(x2Hash, recData)) {
            throw new MpcAbortException("data is not 0");
        }
    }

    /**
     * verify the multiplication result is correct with tuples
     * we can not change the data of toBeVerified
     *
     * @param toBeVerified data to be verified in the form of [[x1, x2, ...], [y1, y2, ...], [z1, z2, ...]]
     * @param tuple        multiplication tuples in the form of [[a1, a2, ...], [b1, b2, ...], [c1, c2, ...]]
     * @throws MpcAbortException if the protocol is abort.
     */
    public void verifyMultipleGroup(TripletRpZ2Vector[][] toBeVerified, TripletRpZ2Vector[][] tuple) throws MpcAbortException {
        if (toBeVerified.length == 0) {
            return;
        }
        int arrayLen = toBeVerified[0].length;
        logPhaseInfo(PtoState.PTO_BEGIN);
        // 1. compute and open rho, sigma
        stopWatch.start();
        TripletRpZ2Vector[] rho = xor(toBeVerified[0], tuple[0]);
        TripletRpZ2Vector[] sigma = xor(toBeVerified[1], tuple[1]);
        BitVector[] openRes = open(MatrixUtils.flat(new TripletRpZ2Vector[][]{rho, sigma}));
        logStepInfo(PtoState.PTO_STEP, "verifyMultipleGroup", 1, 3, resetAndGetTime(), "compute and open rho, sigma");
        // 2. [delta] = [z] ^ [c] ^ sigma & [a] ^ rho & [b] ^ rho & sigma
        stopWatch.start();
        PlainZ2Vector[] openRho = Arrays.stream(Arrays.copyOf(openRes, arrayLen))
            .map(PlainZ2Vector::create).toArray(PlainZ2Vector[]::new);
        PlainZ2Vector[] openSigma = Arrays.stream(Arrays.copyOfRange(openRes, arrayLen, openRes.length))
            .map(PlainZ2Vector::create).toArray(PlainZ2Vector[]::new);
        andi(tuple[1], openRho);
        andi(tuple[0], openSigma);
        xori(tuple[2], toBeVerified[2]);
        xori(tuple[2], tuple[0]);
        xori(tuple[2], tuple[1]);
        IntStream intStream = parallel ? IntStream.range(0, arrayLen).parallel() : IntStream.range(0, arrayLen);
        intStream.forEach(i -> openRho[i].andi(openSigma[i]));
        xori(tuple[2], openRho);
        logStepInfo(PtoState.PTO_STEP, "verifyMultipleGroup", 2, 3, resetAndGetTime(), "locally computation");
        // 3. verify [delta] = 0
        stopWatch.start();
        compareView4Zero(tuple[2]);
        logStepInfo(PtoState.PTO_STEP, "verifyMultipleGroup", 3, 3, resetAndGetTime(), "compare view for zero");
        logPhaseInfo(PtoState.PTO_END);
    }

    public TripletRpZ2Vector[] xor(TripletRpZ2Vector[] xiArray, TripletRpZ2Vector[] yiArray){
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        return intStream.mapToObj(i -> TripletRpZ2Vector.create(IntStream.range(0, 2).mapToObj(j ->
            xiArray[i].getBitVectors()[j].xor(yiArray[i].getBitVectors()[j])).toArray(BitVector[]::new))).toArray(TripletRpZ2Vector[]::new);
    }


    /**
     * xi[i] = xi[i] ^ yi[i]
     *
     * @param xi secret values
     * @param yi plain or secret values
     */
    public void xori(TripletRpZ2Vector[] xi, MpcZ2Vector[] yi) {
        MathPreconditions.checkEqual("xi.length", "yi.length", xi.length, yi.length);
        IntStream intStream = parallel ? IntStream.range(0, xi.length).parallel() : IntStream.range(0, xi.length);
        intStream.forEach(i -> {
            if (yi[i].isPlain()) {
                if (selfId != 1) {
                    xi[i].getBitVectors()[selfId >> 1].xori(yi[i].getBitVector());
                }
            } else {
                xi[i].getBitVectors()[0].xori(yi[i].getBitVectors()[0]);
                xi[i].getBitVectors()[1].xori(yi[i].getBitVectors()[1]);
            }
        });
    }

    /**
     * xi[i] = xi[i] & yi[i]
     *
     * @param xiArray secret values
     * @param yiArray plain values
     */
    public void andi(TripletRpZ2Vector[] xiArray, PlainZ2Vector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        for (int i = 0; i < xiArray.length; i++) {
            assert (!xiArray[i].isPlain()) && yiArray[i].isPlain();
            assert xiArray[i].bitNum() == yiArray[i].bitNum();
        }
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            xiArray[i].getBitVectors()[0].andi(yiArray[i].getBitVector());
            xiArray[i].getBitVectors()[1].andi(yiArray[i].getBitVector());
        });
    }

    /**
     * return zi[i] = xi[i] ^ yi[i]
     *
     * @param xiArray secret values
     * @param yiArray secret values
     */
    public TripletRpZ2Vector[] and(TripletRpZ2Vector[] xiArray, TripletRpZ2Vector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        for (int i = 0; i < xiArray.length; i++) {
            assert xiArray[i].bitNum() == yiArray[i].bitNum();
        }
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        int[] bitNums = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::bitNum).toArray();
        BitVector[] zeroShares = crProvider.randZeroBitVector(bitNums);
        intStream.forEach(i -> {
            zeroShares[i].xori(xiArray[i].getBitVectors()[0].and(yiArray[i].getBitVectors()[0]));
            zeroShares[i].xori(xiArray[i].getBitVectors()[1].and(yiArray[i].getBitVectors()[0]));
            zeroShares[i].xori(xiArray[i].getBitVectors()[0].and(yiArray[i].getBitVectors()[1]));
        });
        sendBitVectors(PtoStep.AND_OP.ordinal(), leftParty(), zeroShares);
        BitVector[] fromRight = receiveBitVectors(PtoStep.AND_OP.ordinal(), rightParty(), bitNums);
        extraInfo++;
        intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        return intStream.mapToObj(i ->
            TripletRpZ2Vector.create(zeroShares[i], fromRight[i])).toArray(TripletRpZ2Vector[]::new);
    }
}
