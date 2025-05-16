package edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiCpIdxPirPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Pai client-specific preprocessing index PIR server.
 *
 * @author Weiran Liu
 * @date 2023/9/24
 */
public class PaiCpIdxPirServer extends AbstractCpIdxPirServer {
    /**
     * rows
     */
    private int rows;
    /**
     * columns
     */
    private int columns;
    /**
     * padding database
     */
    private ZlDatabase paddingDatabase;
    /**
     * final database
     */
    private Map<ByteBuffer, byte[]> finalDatabase;

    public PaiCpIdxPirServer(Rpc serverRpc, Party clientParty, PaiCpIdxPirConfig config) {
        super(PaiCpIdxPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(NaiveDatabase database, int maxBatchNum) throws MpcAbortException {
        setInitInput(database, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        rows = PaiCpIdxPirUtils.getRowNum(n);
        columns = PaiCpIdxPirUtils.getColumnNum(n);
        assert rows * columns >= n
            : "rows * columns must be greater than or equal to n (" + n + "): " + rows * columns;
        // pad the database
        byte[][] paddingData = new byte[rows * columns][byteL];
        for (int x = 0; x < n; x++) {
            paddingData[x] = database.getBytesData(x);
        }
        for (int x = n; x < rows * columns; x++) {
            paddingData[x] = BytesUtils.randomByteArray(byteL, l, secureRandom);
        }
        paddingDatabase = ZlDatabase.create(l, paddingData);
        stopWatch.stop();
        long paddingTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 1, 3, paddingTime,
            String.format(
                "Server sets params: n = %d, Rows = %d, Columns = %d, n (pad) = %d", n, rows, columns, rows * columns
            )
        );

        // preprocessing
        preprocessing();

        logPhaseInfo(PtoState.INIT_END);
    }

    private void preprocessing() throws MpcAbortException {
        stopWatch.start();

        // init the final database
        finalDatabase = new HashMap<>(rows * columns);
        // init a template key matrix and a value matrix
        byte[][][] medKeyMatrix = new byte[rows][columns][CommonConstants.BLOCK_BYTE_LENGTH];
        byte[][][] medValueMatrix = new byte[rows][columns][CommonConstants.BLOCK_BYTE_LENGTH + byteL];
        // stream handling rows
        for (int iRow = 0; iRow < rows; iRow++) {
            // concatenate each column into a whole column
            ByteBuffer rowByteBuffer = ByteBuffer.allocate(byteL * columns);
            for (int iColumn = 0; iColumn < columns; iColumn++) {
                rowByteBuffer.put(paddingDatabase.getBytesData(iRow * columns + iColumn));
            }
            List<byte[]> rowRequestPayload = Collections.singletonList(rowByteBuffer.array());
            sendOtherPartyPayload(PtoStep.SERVER_SEND_ROW_STREAM_DATABASE_REQUEST.ordinal(), rowRequestPayload);

            // receive response
            List<byte[]> medResponsePayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_MED_STREAM_DATABASE_RESPONSE.ordinal());
            MpcAbortPreconditions.checkArgument(medResponsePayload.size() == 1);
            byte[] medDataByteArray = medResponsePayload.get(0);
            // each med contains encrypted key +(random IV + encrypted value)
            MpcAbortPreconditions.checkArgument(
                medDataByteArray.length == (CommonConstants.BLOCK_BYTE_LENGTH * 2 + byteL) * columns
            );
            // split the stream database
            ByteBuffer medByteBuffer = ByteBuffer.wrap(medDataByteArray);
            for (int iColumn = 0; iColumn < columns; iColumn++) {
                // read encrypted key
                medByteBuffer.get(medKeyMatrix[iRow][iColumn]);
                // read encrypted value
                medByteBuffer.get(medValueMatrix[iRow][iColumn]);
            }
        }
        stopWatch.stop();
        long streamRowTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, streamRowTime, "Server handles " + rows + " rows");

        stopWatch.start();
        // stream handling columns
        for (int iColumn = 0; iColumn < columns; iColumn++) {
            // concatenate each row into a whole row
            ByteBuffer columnByteBuffer = ByteBuffer.allocate((CommonConstants.BLOCK_BYTE_LENGTH * 2 + byteL) * rows);
            for (int iRow = 0; iRow < rows; iRow++) {
                columnByteBuffer.put(medKeyMatrix[iRow][iColumn]);
                columnByteBuffer.put(medValueMatrix[iRow][iColumn]);
            }
            List<byte[]> columnRequestPayload = Collections.singletonList(columnByteBuffer.array());
            sendOtherPartyPayload(PtoStep.SERVER_SEND_COLUMN_STREAM_DATABASE_REQUEST.ordinal(), columnRequestPayload);

            // receive response
            List<byte[]> finalResponsePayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_FINAL_STREAM_DATABASE_RESPONSE.ordinal());
            MpcAbortPreconditions.checkArgument(finalResponsePayload.size() == 1);
            byte[] finalDataByteArray = finalResponsePayload.get(0);
            // each final contains encrypted key + (random IV + encrypted value)
            MpcAbortPreconditions.checkArgument(
                finalDataByteArray.length == (CommonConstants.BLOCK_BYTE_LENGTH * 2 + byteL) * rows
            );
            ByteBuffer finalByteBuffer = ByteBuffer.wrap(finalDataByteArray);
            for (int iRow = 0; iRow < rows; iRow++) {
                // final key
                byte[] finalKey = BlockUtils.zeroBlock();
                finalByteBuffer.get(finalKey);
                // final value
                byte[] finalValue = new byte[CommonConstants.BLOCK_BYTE_LENGTH + byteL];
                finalByteBuffer.get(finalValue);
                ByteBuffer finalKeyByteBuffer = ByteBuffer.wrap(finalKey);
                assert !finalDatabase.containsKey(finalKeyByteBuffer);
                finalDatabase.put(finalKeyByteBuffer, finalValue);
            }
        }
        stopWatch.stop();
        long streamColumnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, streamColumnTime, "Server handles " + columns + " columns");
    }

    @Override
    public void pir(int batchNum) throws MpcAbortException {
        setPtoInput(batchNum);

        for (int i = 0; i < batchNum; i++) {
            List<byte[]> queryRequestPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal());
            int queryRequestSize = queryRequestPayload.size();
            MpcAbortPreconditions.checkArgument(queryRequestSize == 0 || queryRequestSize == 1);

            if (queryRequestSize == 0) {
                // response empty query
                responseEmptyQuery();
            } else {
                // response actual query
                respondActualQuery(queryRequestPayload);
            }
        }
    }

    private void responseEmptyQuery() {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        sendOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), new LinkedList<>());
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Server responses empty query");
    }

    private void respondActualQuery(List<byte[]> queryRequestPayload) {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        byte[] key = queryRequestPayload.get(0);
        // response the value
        ByteBuffer keyByteBuffer = ByteBuffer.wrap(key);
        assert finalDatabase.containsKey(keyByteBuffer);
        byte[] value = finalDatabase.get(keyByteBuffer);
        List<byte[]> queryResponsePayload = Collections.singletonList(value);
        sendOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), queryResponsePayload);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Server responses query");

        logPhaseInfo(PtoState.PTO_END);
    }
}
