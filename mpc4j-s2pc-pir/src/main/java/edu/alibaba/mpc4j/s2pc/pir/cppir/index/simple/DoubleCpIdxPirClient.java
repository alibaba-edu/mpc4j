package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpPbcIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.DoubleCpIdxPirPtoDesc.PtoStep;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Double client-preprocessing index PIR client.
 *
 * @author Weiran Liu
 * @date 2024/7/8
 */
public class DoubleCpIdxPirClient extends AbstractCpIdxPirClient implements CpPbcIdxPirClient {
    /**
     * κ
     */
    private static final int KAPPA = Integer.SIZE / Byte.SIZE;
    /**
     * rows, i.e., ℓ
     */
    private int rows;
    /**
     * columns, i.e., m
     */
    private int columns;
    /**
     * each hint_c ∈ Z_q^{n × n}, we totally have [byteL]×[κ] hint_c
     */
    private IntMatrix[][] transposeExtendHintC;
    /**
     * transpose matrix A1
     */
    private IntMatrix transposeMatrixA1;
    /**
     * transpose matrix A2
     */
    private IntMatrix transposeMatrixA2;
    /**
     * s_1 ← Z_q^n, generate for each [byteL].
     */
    private IntVector[] s1s;
    /**
     * s_1 ← Z_q^n, generate for each [byteL].
     */
    private IntVector[] s2s;

    public DoubleCpIdxPirClient(Rpc clientRpc, Party serverParty, DoubleCpIdxPirConfig config) {
        super(DoubleCpIdxPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        List<byte[]> seedPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_SEED.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(seedPayload.size() == 2);
        // we treat plaintext modulus as p = 2^8, so that the database can be seen as N rows and 1 columns.
        rows = (int) Math.ceil(Math.sqrt(n));
        // ensure that each row can contain at least one element
        rows = Math.max(rows, 1);
        columns = (int) Math.ceil((double) n / rows);
        assert (long) rows * columns >= n;
        // A1 ∈ Z_q^{m × n}, here we directly generated the transposed version
        byte[] seedMatrixA1 = seedPayload.get(0);
        transposeMatrixA1 = IntMatrix.createRandom(DoubleCpIdxPirPtoDesc.N, columns, seedMatrixA1);
        // A2 ∈ Z_q^{ℓ × n}
        byte[] seedMatrixA2 = seedPayload.get(1);
        IntMatrix matrixA2 = IntMatrix.createRandom(rows, DoubleCpIdxPirPtoDesc.N, seedMatrixA2);
        transposeMatrixA2 = matrixA2.transpose();
        stopWatch.stop();
        long seedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, seedTime);

        List<byte[]> hintPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_HINT.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(hintPayload.size() == byteL * KAPPA);
        transposeExtendHintC = new IntMatrix[byteL][KAPPA];
        int[][] hintIntArray = hintPayload.stream()
            .map(IntUtils::byteArrayToIntArray)
            .toArray(int[][]::new);
        for (int[] hint : hintIntArray) {
            MpcAbortPreconditions.checkArgument(hint.length == DoubleCpIdxPirPtoDesc.N * DoubleCpIdxPirPtoDesc.N);
        }
        s1s = new IntVector[byteL];
        s2s = new IntVector[byteL];
        IntStream byteIndexIntStream = parallel ? IntStream.range(0, byteL).parallel() : IntStream.range(0, byteL);
        byteIndexIntStream.forEach(byteIndex -> {
            int offset = byteIndex * KAPPA;
            for (int k = 0; k < KAPPA; k++) {
                // parse hint_c
                int[] hint = hintIntArray[offset + k];
                IntMatrix hintC = IntMatrix.createZeros(DoubleCpIdxPirPtoDesc.N, DoubleCpIdxPirPtoDesc.N);
                for (int i = 0; i < DoubleCpIdxPirPtoDesc.N; i++) {
                    int innerOffset = i * DoubleCpIdxPirPtoDesc.N;
                    for (int j = 0; j < DoubleCpIdxPirPtoDesc.N; j++) {
                        hintC.set(i, j, hint[innerOffset + j]);
                    }
                }
                // extend and transpose hint
                IntMatrix extendHintC = hintC.concat(IntVector.createZeros(DoubleCpIdxPirPtoDesc.N));
                transposeExtendHintC[byteIndex][k] = extendHintC.transpose();
            }
            // generate s1 and s2
            s1s[byteIndex] = IntVector.createRandom(DoubleCpIdxPirPtoDesc.N, secureRandom);
            s2s[byteIndex] = IntVector.createRandom(DoubleCpIdxPirPtoDesc.N, secureRandom);
        });
        stopWatch.stop();
        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, hintTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] pir(int[] xs) throws MpcAbortException {
        setPtoInput(xs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            query(xs[i]);
        }
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client generates query");

        stopWatch.start();
        byte[][] entries = new byte[batchNum][];
        for (int i = 0; i < batchNum; i++) {
            entries[i] = recover(xs[i]);
        }
        stopWatch.stop();
        long recoverTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, recoverTime, "Client recovers answer");

        return entries;
    }

    @Override
    public void query(int x) {
        IntStream byteIndexIntStream = parallel ? IntStream.range(0, byteL).parallel() : IntStream.range(0, byteL);
        List<byte[]> queryPayload = byteIndexIntStream
            .mapToObj(byteIndex -> {
                // write i as a pair (i_row, i_col) ∈ [ℓ] × [m]
                int iRow = x % rows;
                int iColumn = x / rows;
                // Sample s1, e1, s2 and e2
                IntVector e1 = IntVector.createGaussian(columns, DoubleCpIdxPirPtoDesc.SIGMA);
                IntVector e2 = IntVector.createGaussian(rows, DoubleCpIdxPirPtoDesc.SIGMA);
                // Compute c1 ← (A1 · s1 + e1 + Δ · u_i_col)
                IntVector c1 = transposeMatrixA1.leftMul(s1s[byteIndex]);
                c1.addi(e1);
                c1.addi(iColumn, 1 << (Integer.SIZE - Byte.SIZE));
                // Compute c2 ← (A2 · s2 + e2 + Δ · u_i_row)
                IntVector c2 = transposeMatrixA2.leftMul(s2s[byteIndex]);
                c2.addi(e2);
                c2.addi(iRow, 1 << (Integer.SIZE - Byte.SIZE));
                int[] c1c2 = new int[columns + rows];
                System.arraycopy(c1.getElements(), 0, c1c2, 0, columns);
                System.arraycopy(c2.getElements(), 0, c1c2, columns, rows);
                return IntUtils.intArrayToByteArray(c1c2);
            })
            .toList();
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryPayload);
    }

    @Override
    public byte[] recover(int x) throws MpcAbortException {
        List<byte[]> responsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
        MpcAbortPreconditions.checkArgument(responsePayload.size() == byteL);
        byte[] entry = new byte[byteL];
        int[][] responseIntArray = responsePayload.stream()
            .map(IntUtils::byteArrayToIntArray)
            .toArray(int[][]::new);
        for (int byteIndex = 0; byteIndex < byteL; byteIndex++) {
            MpcAbortPreconditions.checkArgument(
                responseIntArray[byteIndex].length == KAPPA * DoubleCpIdxPirPtoDesc.N + KAPPA * (DoubleCpIdxPirPtoDesc.N + 1)
            );
        }
        IntStream byteIndexIntStream = parallel ? IntStream.range(0, byteL).parallel() : IntStream.range(0, byteL);
        byteIndexIntStream.forEach(byteIndex -> {
            // parse (h, ans_h, ans_2)
            IntBuffer intBuffer = IntBuffer.wrap(responseIntArray[byteIndex]);
            IntVector[] hs = new IntVector[KAPPA];
            for (int k = 0; k < KAPPA; k++) {
                int[] row = new int[DoubleCpIdxPirPtoDesc.N];
                intBuffer.get(row);
                hs[k] = IntVector.create(row);
            }
            IntVector[] ansh2 = new IntVector[KAPPA];
            for (int k = 0; k < KAPPA; k++) {
                int[] row = new int[DoubleCpIdxPirPtoDesc.N + 1];
                intBuffer.get(row);
                ansh2[k] = IntVector.create(row);
            }
            // compute [h1, a1]
            for (int k = 0; k < KAPPA; k++) {
                IntMatrix transposeHintCh = transposeExtendHintC[byteIndex][k].copy();
                for (int t = 0; t < DoubleCpIdxPirPtoDesc.N; t++) {
                    transposeHintCh.set(t, DoubleCpIdxPirPtoDesc.N, hs[k].getElement(t));
                }
                IntVector temp = transposeHintCh.leftMul(s2s[byteIndex]);
                ansh2[k].subi(temp);
                ansh2[k].shiftRighti(Integer.SIZE - Byte.SIZE);
            }
            IntVector h1a1 = IntVector.composeByteVector(ansh2);
            int d = h1a1.getElement(DoubleCpIdxPirPtoDesc.N);
            IntVector innerProduct = IntVector.create(Arrays.copyOfRange(h1a1.getElements(), 0, DoubleCpIdxPirPtoDesc.N));
            innerProduct.muli(s1s[byteIndex]);
            int sum = innerProduct.sum();
            d = d - sum;
            entry[byteIndex] = (byte) (d >>> (Integer.SIZE - Byte.SIZE));
        });
        return entry;
    }
}
