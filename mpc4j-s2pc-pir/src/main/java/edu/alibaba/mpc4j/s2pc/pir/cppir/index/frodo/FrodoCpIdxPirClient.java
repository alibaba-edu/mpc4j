package edu.alibaba.mpc4j.s2pc.pir.cppir.index.frodo;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.HintCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.frodo.FrodoCpIdxPirPtoDesc.PtoStep;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Frodo client-preprocessing index PIR client.
 *
 * @author Weiran Liu
 * @date 2024/7/24
 */
public class FrodoCpIdxPirClient extends AbstractCpIdxPirClient implements HintCpIdxPirClient {
    /**
     * matrix A
     */
    private IntMatrix matrixA;
    /**
     * hint matrix M
     */
    private IntMatrix matrixM;
    /**
     * b = s · A
     */
    private IntVector[] bs;
    /**
     * c = s · M
     */
    private IntVector[] cs;

    public FrodoCpIdxPirClient(Rpc clientRpc, Party serverParty, FrodoCpIdxPirConfig config) {
        super(FrodoCpIdxPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        List<byte[]> seedPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_SEED.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(seedPayload.size() == 1);
        byte[] seed = seedPayload.get(0);
        MpcAbortPreconditions.checkArgument(seed.length == CommonConstants.BLOCK_BYTE_LENGTH);
        // A ∈ Z_q^{n×m}
        matrixA = IntMatrix.createRandom(FrodoCpIdxPirPtoDesc.N, n, seed);
        stopWatch.stop();
        long seedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, seedTime, "Client generates matrix A");

        List<byte[]> hintPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_HINT.ordinal());
        MpcAbortPreconditions.checkArgument(hintPayload.size() == FrodoCpIdxPirPtoDesc.N);

        stopWatch.start();
        IntVector[] hintVectors = hintPayload.stream()
            .map(IntUtils::byteArrayToIntArray)
            .map(IntVector::create)
            .toArray(IntVector[]::new);
        matrixM = IntMatrix.create(hintVectors);
        stopWatch.stop();
        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, hintTime, "Client stores hints");

        updateKeys();

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] pir(int[] xs) throws MpcAbortException {
        setPtoInput(xs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            query(xs[i], i);
        }
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client generates query");

        stopWatch.start();
        byte[][] entries = new byte[batchNum][];
        for (int i = 0; i < batchNum; i++) {
            entries[i] = recover(xs[i], i);
        }
        stopWatch.stop();
        long recoverTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, recoverTime, "Client recovers answer");

        return entries;
    }

    @Override
    public void query(int x, int i) {
        // ~b = b + e + (0, ..., 0, q/p, ..., 0)
        IntVector e = IntVector.createTernary(n, secureRandom);
        IntVector qu = bs[i].add(e);
        qu.addi(x, 1 << (Integer.SIZE - Byte.SIZE));
        List<byte[]> queryPayload = Collections.singletonList(IntUtils.intArrayToByteArray(qu.getElements()));
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryPayload);
    }

    @Override
    public byte[] recover(int x, int i) throws MpcAbortException {
        List<byte[]> responsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
        MpcAbortPreconditions.checkArgument(responsePayload.size() == 1);
        IntVector ans = IntVector.create(IntUtils.byteArrayToIntArray(responsePayload.get(0)));
        ans.subi(cs[i]);
        MpcAbortPreconditions.checkArgument(ans.getNum() == byteL);
        byte[] entry = new byte[byteL];
        for (int entryIndex = 0; entryIndex < byteL; entryIndex++) {
            int intEntry = ans.getElement(entryIndex);
            if ((intEntry & 0x00800000) > 0) {
                entry[entryIndex] = (byte) ((intEntry >>> (Integer.SIZE - Byte.SIZE)) + 1);
            } else {
                entry[entryIndex] = (byte) (intEntry >>> (Integer.SIZE - Byte.SIZE));
            }
        }
        return entry;
    }

    @Override
    public void updateKeys() {
        stopWatch.start();
        bs = new IntVector[maxBatchNum];
        cs = new IntVector[maxBatchNum];
        IntStream batchIntStream = parallel ? IntStream.range(0, maxBatchNum).parallel() : IntStream.range(0, maxBatchNum);
        batchIntStream.forEach(batchIndex -> {
            // s ← (χ)^n
            IntVector s = IntVector.createTernary(FrodoCpIdxPirPtoDesc.N, secureRandom);
            // b ← s · A
            bs[batchIndex] = matrixA.leftMul(s);
            // c ← s · M
            cs[batchIndex] = matrixM.leftMul(s);
        });
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, keyTime, "Client updates keys");
    }
}
