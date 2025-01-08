package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.AbstractCpKsPirClient;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimplePgmCpKsPirDesc.*;

/**
 * PGM-index client-specific preprocessing KSPIR client.
 *
 * @author Liqiang Peng
 * @date 2024/8/2
 */
public class SimplePgmCpKsPirClient<T> extends AbstractCpKsPirClient<T> {
    /**
     * LWE dimension
     */
    private final int dimension;
    /**
     * σ
     */
    private final double sigma;
    /**
     * key hash
     */
    private Hash keyHash;
    /**
     * index hash
     */
    private Hash idxHash;
    /**
     * secret key s ← Z_q^n, As
     */
    private IntVector as;
    /**
     * hint · s
     */
    private IntVector[] hs;
    /**
     * rows
     */
    private int rows;
    /**
     * data rows
     */
    private int dataRows;
    /**
     * columns
     */
    private int columns;
    /**
     * partition count
     */
    private int partition;
    /**
     * PGM-index
     */
    private LongApproxPgmIndex pgmIndex;

    public SimplePgmCpKsPirClient(Rpc clientRpc, Party serverParty, SimplePgmCpKsPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        GaussianLweParam gaussianLweParam = config.getGaussianLweParam();
        dimension = gaussianLweParam.getDimension();
        sigma = gaussianLweParam.getSigma();
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        keyHash = HashFactory.createInstance(envType, DIGEST_BYTE_L);
        idxHash = HashFactory.createInstance(envType, Long.BYTES);
        List<byte[]> pgmIndexPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_PGM_INFO.ordinal());
        MpcAbortPreconditions.checkArgument(pgmIndexPayload.size() == 1);
        pgmIndex = LongApproxPgmIndex.fromByteArray(pgmIndexPayload.get(0));
        int[] sizes = SimplePgmCpKsPirDesc.getMatrixSize(n, byteL);
        // rows returned is rows + 2 * EPSILON + 3
        rows = sizes[0];
        dataRows = rows - (2 * EPSILON + 3);
        columns = sizes[1];
        partition = sizes[2];
        stopWatch.stop();
        long pgmIdxTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, pgmIdxTime, "Client init PGM index");

        stopWatch.start();
        List<byte[]> seedPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_SEED.ordinal());
        MpcAbortPreconditions.checkArgument(seedPayload.size() == 1);
        byte[] seed = seedPayload.get(0);
        MpcAbortPreconditions.checkArgument(seed.length == CommonConstants.BLOCK_BYTE_LENGTH);
        IntMatrix matrixA = IntMatrix.createRandom(columns, dimension, seed);
        IntMatrix transposeMatrixA = matrixA.transpose();
        IntVector s = IntVector.createRandom(dimension, secureRandom);
        as = transposeMatrixA.leftMul(s);
        IntMatrix[] transposeHint = new IntMatrix[partition];
        for (int p = 0; p < partition; p++) {
            List<byte[]> hintPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_HINT.ordinal());
            MpcAbortPreconditions.checkArgument(hintPayload.size() == rows);
            byte[][] hintBytes = hintPayload.toArray(new byte[0][]);
            IntVector[] hintVectors = IntStream.range(0, rows)
                .mapToObj(i -> IntUtils.byteArrayToIntArray(hintBytes[i]))
                .map(IntVector::create)
                .toArray(IntVector[]::new);
            IntMatrix hint = IntMatrix.create(hintVectors);
            transposeHint[p] = hint.transpose();
        }
        // generate s and s · hint
        IntStream intStream = parallel ? IntStream.range(0, partition).parallel() : IntStream.range(0, partition);
        hs = intStream.mapToObj(p -> transposeHint[p].leftMul(s)).toArray(IntVector[]::new);
        stopWatch.stop();
        long initSimplePirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initSimplePirTime, "Client stores hints");

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
        byte[] byteIdx = idxHash.digestToBytes(ObjectUtils.objectToByteArray(key));
        long keyIdx = LongUtils.byteArrayToLong(byteIdx);
        int[] index = pgmIndex.approximateIndexRangeOf(keyIdx);
        int colIdx = index[0] < 0 ? secureRandom.nextInt(columns) : index[0] / dataRows;
        // client generates qu, qu = A * s + e + q/p * u_i_col
        IntVector e = IntVector.createGaussian(columns, sigma, secureRandom);
        IntVector qu = as.add(e);
        qu.addi(colIdx, 1 << (Integer.SIZE - Byte.SIZE));
        List<byte[]> queryPayload = Collections.singletonList(IntUtils.intArrayToByteArray(qu.getElements()));
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryPayload);
    }

    private byte[] decode(T key) throws MpcAbortException {
        List<byte[]> responsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
        MpcAbortPreconditions.checkArgument(responsePayload.size() == partition);
        IntVector[] ansArray = responsePayload.stream()
            .map(ans -> IntVector.create(IntUtils.byteArrayToIntArray(ans)))
            .toArray(IntVector[]::new);
        for (IntVector ans : ansArray) {
            MpcAbortPreconditions.checkArgument(ans.getNum() == rows);
        }
        for (int i = 0; i < rows; i++) {
            ByteBuffer entryByteBuffer = ByteBuffer.allocate(partition);
            for (int p = 0; p < partition; p++) {
                IntVector d = ansArray[p].sub(hs[p]);
                byte partitionEntry;
                int element = d.getElement(i);
                if ((element & 0x00800000) > 0) {
                    partitionEntry = (byte) ((element >>> (Integer.SIZE - Byte.SIZE)) + 1);
                } else {
                    partitionEntry = (byte) (element >>> (Integer.SIZE - Byte.SIZE));
                }
                entryByteBuffer.put(partitionEntry);
            }
            byte[] entry = entryByteBuffer.array();
            byte[] actualDigest = BytesUtils.clone(entry, 0, DIGEST_BYTE_L);
            byte[] expectDigest = keyHash.digestToBytes(ObjectUtils.objectToByteArray(key));
            if (BytesUtils.equals(actualDigest, expectDigest)) {
                return BytesUtils.clone(entry, DIGEST_BYTE_L, byteL);
            }
        }
        return null;
    }
}
