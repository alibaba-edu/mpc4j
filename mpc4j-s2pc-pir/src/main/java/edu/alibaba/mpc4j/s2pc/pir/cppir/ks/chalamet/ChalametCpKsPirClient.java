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
public class ChalametCpKsPirClient<T> extends AbstractCpKsPirClient<T> {
    /**
     * fuse position
     */
    private Arity3ByteFusePosition<T> arity3ByteFusePosition;
    /**
     * b = s · A
     */
    private IntVector b;
    /**
     * c = s · M
     */
    private IntVector c;
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
        byte[] matrixSeed = matrixSeedPayload.get(0);
        MpcAbortPreconditions.checkArgument(matrixSeed.length == CommonConstants.BLOCK_BYTE_LENGTH);
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
        IntMatrix matrixM = IntMatrix.create(hintVectors);
        // s ← (χ)^n
        IntVector s = IntVector.createTernary(N, secureRandom);
        // b ← s · A, where A ∈ Z_q^{n×m}
        IntHash intHash = new BobIntHash();
        IntStream intStream = parallel ? IntStream.range(0, filterLength).parallel() : IntStream.range(0, filterLength);
        int[] bArray = intStream
            .map(i -> {
                int[] col = IntStream.range(0, N)
                    .map(j -> intHash.hash(matrixSeed, j * filterLength + i))
                    .toArray();
                return s.innerMul(IntVector.create(col));
            })
            .toArray();
        b = IntVector.create(bArray);
        // c ← s · M
        c = matrixM.leftMul(s);
        stopWatch.stop();
        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, hintTime, "Client stores hints");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] pir(ArrayList<T> keys) throws MpcAbortException {
        setPtoInput(keys);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            query(keys.get(i));
        }
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client generates query");

        stopWatch.start();
        byte[][] entries = new byte[batchNum][];
        for (int i = 0; i < batchNum; i++) {
            entries[i] = decode(keys.get(i));
        }
        stopWatch.stop();
        long recoverTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, recoverTime, "Client recovers answer");

        logPhaseInfo(PtoState.PTO_END);
        return entries;
    }

    private void query(T key) {
        int[] xs = arity3ByteFusePosition.positions(key);
        IntVector e = IntVector.createTernary(filterLength, secureRandom);
        IntVector qu = b.add(e);
        qu.addi(e);
        for (int x : xs) {
            qu.addi(x, 1 << (Integer.SIZE - Byte.SIZE));
        }
        List<byte[]> queryPayload = Collections.singletonList(IntUtils.intArrayToByteArray(qu.getElements()));
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryPayload);
    }

    private byte[] decode(T key) throws MpcAbortException {
        List<byte[]> responsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
        MpcAbortPreconditions.checkArgument(responsePayload.size() == 1);
        IntVector ans = IntVector.create(IntUtils.byteArrayToIntArray(responsePayload.get(0)));
        ans.subi(c);
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
}
