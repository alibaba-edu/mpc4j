package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.HintCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirPtoDesc.PtoStep;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Simple client-specific preprocessing index PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/9/18
 */
public class SimpleCpIdxPirServer extends AbstractCpIdxPirServer implements HintCpIdxPirServer {
    /**
     * LWE dimension
     */
    private final int dimension;
    /**
     * columns
     */
    private int columns;
    /**
     * transpose database database
     */
    private IntMatrix[] tdbs;

    public SimpleCpIdxPirServer(Rpc serverRpc, Party clientParty, SimpleCpIdxPirConfig config) {
        super(SimpleCpIdxPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        GaussianLweParam gaussianLweParam = config.getGaussianLweParam();
        dimension = gaussianLweParam.getDimension();
    }

    @Override
    public void init(NaiveDatabase database, int matchBatchNum) throws MpcAbortException {
        setInitInput(database, matchBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // server generates and sends the seed for the random matrix A.
        byte[] seed = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        List<byte[]> seedPayload = Collections.singletonList(seed);
        sendOtherPartyPayload(PtoStep.SERVER_SEND_SEED.ordinal(), seedPayload);
        stopWatch.stop();
        long seedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, seedTime, "Server generates seed");

        stopWatch.start();
        int subByteL = Math.min(byteL, SimpleCpIdxPirPtoDesc.getMaxSubByteL(n));
        int[] sizes = SimpleCpIdxPirPtoDesc.getMatrixSize(n, byteL);
        int rows = sizes[0];
        columns = sizes[1];
        int partition = sizes[2];
        // create database
        IntMatrix[] dbs = IntStream.range(0, partition)
            .mapToObj(p -> IntMatrix.createZeros(rows, columns))
            .toArray(IntMatrix[]::new);
        int i = 0;
        int j = 0;
        for (int dataIndex = 0; dataIndex < database.rows(); dataIndex++) {
            byte[] element = database.getBytesData(dataIndex);
            assert element.length == byteL;
            byte[] paddingElement = BytesUtils.paddingByteArray(element, subByteL * partition);
            // encode each row into partition databases
            for (int entryIndex = 0; entryIndex < subByteL; entryIndex++) {
                for (int p = 0; p < partition; p++) {
                    dbs[p].set(i, j, (paddingElement[p * subByteL + entryIndex] & 0xFF));
                }
                i++;
                // change column index
                if (i == rows) {
                    i = 0;
                    j++;
                }
            }
        }
        // create hint
        IntMatrix matrixA = IntMatrix.createRandom(columns, dimension, seed);
        tdbs = new IntMatrix[partition];
        IntStream intStream = parallel ? IntStream.range(0, partition).parallel() : IntStream.range(0, partition);
        IntMatrix[] hint = intStream.mapToObj(p -> dbs[p].mul(matrixA)).toArray(IntMatrix[]::new);
        // transpose database
        IntStream.range(0, partition).forEach(p -> {
            IntStream hintIntStream = parallel ? IntStream.range(0, rows).parallel() : IntStream.range(0, rows);
            List<byte[]> hintPayload = hintIntStream
                .mapToObj(rowIndex -> IntUtils.intArrayToByteArray(hint[p].getRow(rowIndex).getElements()))
                .collect(Collectors.toList());
            sendOtherPartyPayload(PtoStep.SERVER_SEND_HINT.ordinal(), hintPayload);
            tdbs[p] = dbs[p].transpose();
        });
        stopWatch.stop();
        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, hintTime, "Server generates hints");

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
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == 1);
        // parse qu
        IntVector qu = IntVector.create(IntUtils.byteArrayToIntArray(clientQueryPayload.get(0)));
        MpcAbortPreconditions.checkArgument(qu.getNum() == columns);
        // generate response
        IntStream pIntStream = parallel ? IntStream.range(0, tdbs.length).parallel() : IntStream.range(0, tdbs.length);
        List<byte[]> responsePayload = pIntStream
            .mapToObj(p -> tdbs[p].leftMul(qu))
            .map(ans -> IntUtils.intArrayToByteArray(ans.getElements()))
            .toList();
        sendOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), responsePayload);
    }
}
