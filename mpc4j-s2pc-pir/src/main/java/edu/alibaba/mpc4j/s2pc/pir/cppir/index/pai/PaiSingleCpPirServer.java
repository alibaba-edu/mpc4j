package edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractSingleCpPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiSingleCpPirDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Pai client-specific preprocessing PIR server.
 *
 * @author Weiran Liu
 * @date 2023/9/24
 */
public class PaiSingleCpPirServer extends AbstractSingleCpPirServer {
    /**
     * row num
     */
    private int rowNum;
    /**
     * column num
     */
    private int columnNum;
    /**
     * padding database
     */
    private ZlDatabase paddingDatabase;
    /**
     * final database
     */
    private Map<ByteBuffer, byte[]> finalDatabase;

    public PaiSingleCpPirServer(Rpc serverRpc, Party clientParty, PaiSingleCpPirConfig config) {
        super(PaiSingleCpPirDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(ZlDatabase database) throws MpcAbortException {
        setInitInput(database);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        rowNum = PaiSingleCpPirUtils.getRowNum(n);
        columnNum = PaiSingleCpPirUtils.getColumnNum(n);
        assert rowNum * columnNum >= n
            : "rowNum * columnNum must be greater than or equal to n (" + n + "): " + rowNum * columnNum;
        // pad the database
        byte[][] paddingData = new byte[rowNum * columnNum][byteL];
        for (int x = 0; x < n; x++) {
            paddingData[x] = database.getBytesData(x);
        }
        for (int x = n; x < rowNum * columnNum; x++) {
            paddingData[x] = BytesUtils.randomByteArray(byteL, l, secureRandom);
        }
        paddingDatabase = ZlDatabase.create(l, paddingData);
        stopWatch.stop();
        long paddingTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 1, 3, paddingTime,
            String.format(
                "Server sets params: n = %d, RowNum = %d, ColumnNum = %d, n (pad) = %d",
                n, rowNum, columnNum, rowNum * columnNum
            )
        );

        // preprocessing
        preprocessing();

        logPhaseInfo(PtoState.INIT_END);
    }

    private void preprocessing() throws MpcAbortException {
        stopWatch.start();

        // init the final database
        finalDatabase = new HashMap<>(rowNum * columnNum);
        // init a template key matrix and a value matrix
        byte[][][] medKeyMatrix = new byte[rowNum][columnNum][CommonConstants.BLOCK_BYTE_LENGTH];
        byte[][][] medValueMatrix = new byte[rowNum][columnNum][CommonConstants.BLOCK_BYTE_LENGTH + byteL];
        // stream handling rows
        for (int iRow = 0; iRow < rowNum; iRow++) {
            // concatenate each column into a whole column
            ByteBuffer rowByteBuffer = ByteBuffer.allocate(byteL * columnNum);
            for (int iColumn = 0; iColumn < columnNum; iColumn++) {
                rowByteBuffer.put(paddingDatabase.getBytesData(iRow * columnNum + iColumn));
            }
            List<byte[]> rowRequestPayload = Collections.singletonList(rowByteBuffer.array());
            DataPacketHeader rowRequestHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ROW_STREAM_DATABASE_REQUEST.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(rowRequestHeader, rowRequestPayload));

            // receive response
            DataPacketHeader medResponseHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_MED_STREAM_DATABASE_RESPONSE.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> medResponsePayload = rpc.receive(medResponseHeader).getPayload();
            MpcAbortPreconditions.checkArgument(medResponsePayload.size() == 1);
            byte[] medDataByteArray = medResponsePayload.get(0);
            // each med contains encrypted key +(random IV + encrypted value)
            MpcAbortPreconditions.checkArgument(
                medDataByteArray.length == (CommonConstants.BLOCK_BYTE_LENGTH * 2 + byteL) * columnNum
            );
            // split the stream database
            ByteBuffer medByteBuffer = ByteBuffer.wrap(medDataByteArray);
            for (int iColumn = 0; iColumn < columnNum; iColumn++) {
                // read encrypted key
                medByteBuffer.get(medKeyMatrix[iRow][iColumn]);
                // read encrypted value
                medByteBuffer.get(medValueMatrix[iRow][iColumn]);
            }
            extraInfo++;
        }
        stopWatch.stop();
        long streamRowTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, streamRowTime, "Server handles " + rowNum + " rows");

        stopWatch.start();
        // stream handling columns
        for (int iColumn = 0; iColumn < columnNum; iColumn++) {
            // concatenate each row into a whole row
            ByteBuffer columnByteBuffer = ByteBuffer.allocate((CommonConstants.BLOCK_BYTE_LENGTH * 2 + byteL) * rowNum);
            for (int iRow = 0; iRow < rowNum; iRow++) {
                columnByteBuffer.put(medKeyMatrix[iRow][iColumn]);
                columnByteBuffer.put(medValueMatrix[iRow][iColumn]);
            }
            List<byte[]> columnRequestPayload = Collections.singletonList(columnByteBuffer.array());
            DataPacketHeader columnRequestHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_COLUMN_STREAM_DATABASE_REQUEST.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(columnRequestHeader, columnRequestPayload));

            // receive response
            DataPacketHeader finalResponseHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_FINAL_STREAM_DATABASE_RESPONSE.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> finalResponsePayload = rpc.receive(finalResponseHeader).getPayload();
            MpcAbortPreconditions.checkArgument(finalResponsePayload.size() == 1);
            byte[] finalDataByteArray = finalResponsePayload.get(0);
            // each final contains encrypted key + (random IV + encrypted value)
            MpcAbortPreconditions.checkArgument(
                finalDataByteArray.length == (CommonConstants.BLOCK_BYTE_LENGTH * 2 + byteL) * rowNum
            );
            ByteBuffer finalByteBuffer = ByteBuffer.wrap(finalDataByteArray);
            for (int iRow = 0; iRow < rowNum; iRow++) {
                // final key
                byte[] finalKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                finalByteBuffer.get(finalKey);
                // final value
                byte[] finalValue = new byte[CommonConstants.BLOCK_BYTE_LENGTH + byteL];
                finalByteBuffer.get(finalValue);
                ByteBuffer finalKeyByteBuffer = ByteBuffer.wrap(finalKey);
                assert !finalDatabase.containsKey(finalKeyByteBuffer);
                finalDatabase.put(finalKeyByteBuffer, finalValue);
            }
            extraInfo++;
        }
        stopWatch.stop();
        long streamColumnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, streamColumnTime, "Server handles " + columnNum + " columns");
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();

        DataPacketHeader queryRequestHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryRequestPayload = rpc.receive(queryRequestHeader).getPayload();
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

    private void responseEmptyQuery() {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        DataPacketHeader queryResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryResponseHeader, new LinkedList<>()));
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
        DataPacketHeader queryResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryResponseHeader, queryResponsePayload));
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Server responses query");

        logPhaseInfo(PtoState.PTO_END);
    }
}
