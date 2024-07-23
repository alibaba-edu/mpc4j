package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpPbcIdxPirServer;
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
public class SimpleCpIdxPirServer extends AbstractCpIdxPirServer implements CpPbcIdxPirServer {
    /**
     * columns
     */
    private int columns;
    /**
     * transpose database database
     */
    private IntMatrix tdb;

    public SimpleCpIdxPirServer(Rpc serverRpc, Party clientParty, SimpleCpIdxPirConfig config) {
        super(SimpleCpIdxPirPtoDesc.getInstance(), serverRpc, clientParty, config);
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
        // we treat plaintext modulus as p = 2^8, so that the database can be seen as N rows and byteL columns.
        long entryNum = (long) byteL * n;
        int sqrt = (int) Math.ceil(Math.sqrt(entryNum));
        // ensure that each row can contain at least one element
        sqrt = Math.max(sqrt, byteL);
        // we make DB rows a little bit larger than rows to ensure each column can contain more data.
        int rowElementNum = CommonUtils.getUnitNum(sqrt, byteL);
        int rows = rowElementNum * byteL;
        columns = (int) Math.ceil((double) entryNum / rows);
        assert (long) rows * columns >= entryNum;
        // create database
        IntMatrix db = IntMatrix.createZeros(rows, columns);
        int i = 0;
        int j = 0;
        for (int dataIndex = 0; dataIndex < database.rows(); dataIndex++) {
            byte[] element = database.getBytesData(dataIndex);
            assert element.length == byteL;
            for (byte b : element) {
                db.set(i, j, (b & 0xFF));
                i++;
            }
            // change column index
            if (i == rows) {
                i = 0;
                j++;
            }
        }
        // create hint
        IntMatrix matrixA = IntMatrix.createRandom(columns, SimpleCpIdxPirPtoDesc.N, seed);
        IntMatrix hint = db.mul(matrixA);
        IntStream hintIntStream = parallel ? IntStream.range(0, rows).parallel() : IntStream.range(0, rows);
        List<byte[]> hintPayload = hintIntStream
            .mapToObj(rowIndex -> IntUtils.intArrayToByteArray(hint.getRow(rowIndex).getElements()))
            .collect(Collectors.toList());
        sendOtherPartyPayload(PtoStep.SERVER_SEND_HINT.ordinal(), hintPayload);
        // transpose database
        tdb = db.transpose();
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
        IntVector ans = tdb.leftMul(qu);
        List<byte[]> responsePayload = Collections.singletonList(IntUtils.intArrayToByteArray(ans.getElements()));
        sendOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), responsePayload);
    }
}
