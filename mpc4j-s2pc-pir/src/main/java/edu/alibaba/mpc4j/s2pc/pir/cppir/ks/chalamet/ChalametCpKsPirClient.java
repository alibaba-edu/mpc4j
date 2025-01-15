package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.chalamet;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.fusefilter.Arity3ByteFusePosition;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.bobhash.BobIntHash;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.AbstractCpKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.HintCpKsPirClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.cppir.ks.chalamet.ChalametCpKsPirDesc.*;

/**
 * Chalamet client-specific preprocessing KSPIR client.
 *
 * @author Liqiang Peng
 * @date 2024/8/2
 */
public class ChalametCpKsPirClient<T> extends AbstractCpKsPirClient<T> implements HintCpKsPirClient<T> {
    /**
     * fuse position
     */
    private Arity3ByteFusePosition<T> arity3ByteFusePosition;
    /**
     * seed for Matrix A
     */
    private byte[] seedMatrixA;
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
    /**
     * filter length
     */
    private int filterLength;
    /**
     * hash
     */
    private Hash hash;

    public ChalametCpKsPirClient(Rpc clientRpc, Party serverParty, ChalametCpKsPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        hash = HashFactory.createInstance(envType, DIGEST_BYTE_L);
        List<byte[]> fuseFilterSeedPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_FUSE_FILTER_SEED.ordinal());
        MpcAbortPreconditions.checkArgument(fuseFilterSeedPayload.size() == 1);
        byte[] fuseFilterSeed = fuseFilterSeedPayload.get(0);
        arity3ByteFusePosition = new Arity3ByteFusePosition<>(envType, n, byteL + DIGEST_BYTE_L, fuseFilterSeed);
        filterLength = arity3ByteFusePosition.filterLength();
        stopWatch.stop();
        long fusePosTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, fusePosTime, "Client generates fuse position");

        stopWatch.start();
        List<byte[]> matrixSeedPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_SEED.ordinal());
        MpcAbortPreconditions.checkArgument(matrixSeedPayload.size() == 1);
        seedMatrixA = matrixSeedPayload.get(0);
        MpcAbortPreconditions.checkArgument(seedMatrixA.length == CommonConstants.BLOCK_BYTE_LENGTH);
        stopWatch.stop();
        long seedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, seedTime, "Client generates matrix A");

        stopWatch.start();
        List<byte[]> hintPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_HINT.ordinal());
        MpcAbortPreconditions.checkArgument(hintPayload.size() == N);
        IntVector[] hintVectors = hintPayload.stream()
            .map(IntUtils::byteArrayToIntArray)
            .map(IntVector::create)
            .toArray(IntVector[]::new);
        matrixM = IntMatrix.create(hintVectors);
        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, hintTime, "Client stores hints");

        updateKeys();

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] pir(ArrayList<T> keys) throws MpcAbortException {
        setPtoInput(keys);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            query(keys.get(i), i);
        }
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client generates query");

        stopWatch.start();
        byte[][] entries = new byte[batchNum][];
        for (int i = 0; i < batchNum; i++) {
            entries[i] = decode(keys.get(i), i);
        }
        stopWatch.stop();
        long recoverTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, recoverTime, "Client recovers answer");

        logPhaseInfo(PtoState.PTO_END);
        return entries;
    }

    private void query(T key, int i) {
        int[] xs = arity3ByteFusePosition.positions(key);
        IntVector e = IntVector.createTernary(filterLength, secureRandom);
        IntVector qu = bs[i].add(e);
        for (int x : xs) {
            qu.addi(x, 1 << (Integer.SIZE - Byte.SIZE));
        }
        List<byte[]> queryPayload = Collections.singletonList(IntUtils.intArrayToByteArray(qu.getElements()));
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryPayload);
    }

    private byte[] decode(T key, int i) throws MpcAbortException {
        List<byte[]> responsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
        MpcAbortPreconditions.checkArgument(responsePayload.size() == 1);
        IntVector ans = IntVector.create(IntUtils.byteArrayToIntArray(responsePayload.get(0)));
        ans.subi(cs[i]);
        MpcAbortPreconditions.checkArgument(ans.getNum() == byteL + DIGEST_BYTE_L);
        byte[] entry = new byte[byteL + DIGEST_BYTE_L];
        for (int entryIndex = 0; entryIndex < byteL + DIGEST_BYTE_L; entryIndex++) {
            int intEntry = ans.getElement(entryIndex);
            if ((intEntry & 0x00800000) > 0) {
                entry[entryIndex] = (byte) ((intEntry >>> (Integer.SIZE - Byte.SIZE)) + 1);
            } else {
                entry[entryIndex] = (byte) (intEntry >>> (Integer.SIZE - Byte.SIZE));
            }
        }
        byte[] actualDigest = BytesUtils.clone(entry, 0, DIGEST_BYTE_L);
        byte[] expectDigest = hash.digestToBytes(ObjectUtils.objectToByteArray(key));
        if (BytesUtils.equals(actualDigest, expectDigest)) {
            return BytesUtils.clone(entry, DIGEST_BYTE_L, byteL);
        } else {
            return null;
        }
    }

    @Override
    public void updateKeys() {
        stopWatch.start();
        bs = new IntVector[maxBatchNum];
        cs = new IntVector[maxBatchNum];
        // s ← (χ)^n
        IntVector[] ss = IntStream.range(0, maxBatchNum)
            .mapToObj(batchIndex -> IntVector.createTernary(N, secureRandom))
            .toArray(IntVector[]::new);
        IntHash intHash = new BobIntHash();
        int[][] bArrays = new int[maxBatchNum][filterLength];
        IntStream intStream = parallel ? IntStream.range(0, filterLength).parallel() : IntStream.range(0, filterLength);
        intStream.forEach(i -> {
            // b ← s · A, where A ∈ Z_q^{n×m}
            int[] col = IntStream.range(0, N)
                .map(j -> intHash.hash(seedMatrixA, j * filterLength + i))
                .toArray();
            IntVector colIntVector = IntVector.create(col);
            for (int batchIndex = 0; batchIndex < maxBatchNum; batchIndex++) {
                bArrays[batchIndex][i] = ss[batchIndex].innerMul(colIntVector);
            }
        });
        IntStream batchIndexStream = parallel ? IntStream.range(0, maxBatchNum).parallel() : IntStream.range(0, maxBatchNum);
        batchIndexStream.forEach(batchIndex -> {
            // b ← s · A, where A ∈ Z_q^{n×m}
            bs[batchIndex] = IntVector.create(bArrays[batchIndex]);
            // c ← s · M
            cs[batchIndex] = matrixM.leftMul(ss[batchIndex]);
        });
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, keyTime, "Client updates keys");
    }
}
