package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.HintCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.DoubleCpIdxPirPtoDesc.PtoStep;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Double client-preprocessing index PIR server.
 *
 * @author Weiran Liu
 * @date 2024/7/8
 */
public class DoubleCpIdxPirServer extends AbstractCpIdxPirServer implements HintCpIdxPirServer {
    /**
     * κ
     */
    private static final int KAPPA = Integer.SIZE / Byte.SIZE;
    /**
     * LWE dimension
     */
    private final int dimension;
    /**
     * rows, i.e., ℓ
     */
    private int rows;
    /**
     * columns, i.e., m
     */
    private int columns;
    /**
     * db ∈ Z_q^{ℓ × m}, we totally have [byteL] db, here ℓ is rows, m is columns, where we store the transposed db.
     */
    private IntMatrix[] tdb;
    /**
     * each hint_s ∈ Z_q^{n × ℓ}, we totally have [byteL]×[κ] hint_s, where κ = log(q) / log(p) = 32 / 8 = 4.
     */
    private IntMatrix[][] transposeExtendHintS;
    /**
     * matrix A2
     */
    private IntMatrix matrixA2;

    public DoubleCpIdxPirServer(Rpc serverRpc, Party clientParty, DoubleCpIdxPirConfig config) {
        super(DoubleCpIdxPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        GaussianLweParam gaussianLweParam = config.getGaussianLweParam();
        dimension = gaussianLweParam.getDimension();
    }

    @Override
    public void init(NaiveDatabase database, int matchBatchNum) throws MpcAbortException {
        setInitInput(database, matchBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // server send seeds
        byte[] seedMatrixA1 = BlockUtils.randomBlock(secureRandom);
        byte[] seedMatrixA2 = BlockUtils.randomBlock(secureRandom);
        List<byte[]> seedPayload = Arrays.asList(seedMatrixA1, seedMatrixA2);
        sendOtherPartyPayload(PtoStep.SERVER_SEND_SEED.ordinal(), seedPayload);
        stopWatch.stop();
        long seedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, seedTime);

        stopWatch.start();
        int[] sizes = DoubleCpIdxPirPtoDesc.getMatrixSize(n);
        rows = sizes[0];
        columns = sizes[1];
        tdb = new IntMatrix[byteL];
        transposeExtendHintS = new IntMatrix[byteL][KAPPA];
        byte[][] byteArrayDatabase = database.getBytesData();
        // A1 ∈ Z_q^{m × n}, here we directly generated the transposed version
        IntMatrix transposeMatrixA1 = IntMatrix.createRandom(dimension, columns, seedMatrixA1);
        // A2 ∈ Z_q^{ℓ × n}
        matrixA2 = IntMatrix.createRandom(rows, dimension, seedMatrixA2);
        // each hint_c ∈ Z_q^{n × n}, we totally have [byteL]×[κ] hint_c
        final IntMatrix[][] hintC = new IntMatrix[byteL][KAPPA];
        IntStream byteIndexIntStream = parallel ? IntStream.range(0, byteL).parallel() : IntStream.range(0, byteL);
        byteIndexIntStream.forEach(byteIndex -> {
            // create database
            IntMatrix db = IntMatrix.createZeros(rows, columns);
            for (int dataIndex = 0; dataIndex < n; dataIndex++) {
                int iRow = dataIndex % rows;
                int iColumn = dataIndex / rows;
                db.set(iRow, iColumn, (byteArrayDatabase[dataIndex][byteIndex] & 0xFF));
            }
            tdb[byteIndex] = db.transpose();
            // hint_s = Decomp(A_1^T * db^T).
            IntMatrix[] hintS = IntMatrix.decomposeToByteVector(transposeMatrixA1.mul(tdb[byteIndex]));
            for (int k = 0; k < KAPPA; k++) {
                hintC[byteIndex][k] = hintS[k].mul(matrixA2);
                IntMatrix extendHintS = hintS[k].concat(IntVector.createZeros(rows));
                transposeExtendHintS[byteIndex][k] = extendHintS.transpose();
            }
        });
        List<byte[]> hintPayload = IntStream.range(0, byteL * KAPPA)
            .mapToObj(t -> {
                IntMatrix hintMatrix = hintC[t / KAPPA][t % KAPPA];
                IntBuffer intBuffer = IntBuffer.allocate(dimension * dimension);
                for (int i = 0; i < dimension; i++) {
                    intBuffer.put(hintMatrix.getRow(i).getElements());
                }
                return IntUtils.intArrayToByteArray(intBuffer.array());
            })
            .toList();
        sendOtherPartyPayload(PtoStep.SERVER_SEND_HINT.ordinal(), hintPayload);
        stopWatch.stop();
        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, hintTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir(int batchNum) throws MpcAbortException {
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            answer();
        }
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    @Override
    public void answer() throws MpcAbortException {
        List<byte[]> clientQueryPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal());
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == byteL);
        int[][] qus = clientQueryPayload.stream()
            .map(IntUtils::byteArrayToIntArray)
            .toArray(int[][]::new);
        for (int byteIndex = 0; byteIndex < byteL; byteIndex++) {
            MpcAbortPreconditions.checkArgument(qus[byteIndex].length == rows + columns);
        }
        IntStream byteIndexIntStream = parallel ? IntStream.range(0, byteL).parallel() : IntStream.range(0, byteL);
        List<byte[]> responsePayload = byteIndexIntStream
            .mapToObj(byteIndex -> {
                // parse qu = (c_1, c_2)
                int[] c1c2 = qus[byteIndex];
                IntVector c1 = IntVector.create(Arrays.copyOfRange(c1c2, 0, columns));
                IntVector c2 = IntVector.create(Arrays.copyOfRange(c1c2, columns, columns + rows));
                // ans_1 ← Decomp(c_1^T · db^T) ∈ Z_q^{κ × ℓ}
                IntVector[] ans1 = IntVector.decomposeToByteVector(tdb[byteIndex].leftMul(c1));
                // h ← ans_1 · A_2 ∈ Z_q^{κ × n}
                IntVector[] hs = Arrays.stream(ans1).map(matrixA2::leftMul).toArray(IntVector[]::new);
                // [ans_h // ans_2] ← [hint_s // ans_1] · c_2 ∈ Z_q^{κ × (n+1)}
                IntVector[] ansh2 = IntStream.range(0, KAPPA)
                    .mapToObj(k -> {
                        IntMatrix transposeHintS1 = transposeExtendHintS[byteIndex][k].copy();
                        for (int t = 0; t < rows; t++) {
                            transposeHintS1.set(t, dimension, ans1[k].getElement(t));
                        }
                        return transposeHintS1.leftMul(c2);
                    })
                    .toArray(IntVector[]::new);
                IntBuffer ansBuffer = IntBuffer.allocate(KAPPA * dimension + KAPPA * (dimension + 1));
                for (int k = 0; k < KAPPA; k++) {
                    ansBuffer.put(hs[k].getElements());
                }
                for (int k = 0; k < KAPPA; k++) {
                    ansBuffer.put(ansh2[k].getElements());
                }
                return IntUtils.intArrayToByteArray(ansBuffer.array());
            })
            .toList();
        sendOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), responsePayload);
    }
}
