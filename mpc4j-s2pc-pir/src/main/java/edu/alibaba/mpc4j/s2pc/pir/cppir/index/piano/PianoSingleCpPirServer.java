package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractSingleCpPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleCpPirDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * PIANO client-specific preprocessing PIR server.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class PianoSingleCpPirServer extends AbstractSingleCpPirServer {
    /**
     * chunk size
     */
    private int chunkSize;
    /**
     * chunk num
     */
    private int chunkNum;
    /**
     * padding database
     */
    private ZlDatabase paddingDatabase;
    /**
     * query num for each preprocessing round
     */
    private int roundQueryNum;
    /**
     * current query num
     */
    private int currentQueryNum;

    public PianoSingleCpPirServer(Rpc serverRpc, Party clientParty, PianoSingleCpPirConfig config) {
        super(PianoSingleCpPirDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(ZlDatabase database) throws MpcAbortException {
        setInitInput(database);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        chunkSize = PianoSingleCpPirUtils.getChunkSize(n);
        chunkNum = PianoSingleCpPirUtils.getChunkNum(n);
        assert chunkSize * chunkNum >= n
            : "chunkSize * chunkNum must be greater than or equal to n (" + n + "): " + chunkSize * chunkNum;
        // pad the database
        byte[][] paddingData = new byte[chunkSize * chunkNum][byteL];
        for (int x = 0; x < n; x++) {
            paddingData[x] = database.getBytesData(x);
        }
        for (int x = n; x < chunkSize * chunkNum; x++) {
            paddingData[x] = BytesUtils.randomByteArray(byteL, l, secureRandom);
        }
        paddingDatabase = ZlDatabase.create(l, paddingData);
        roundQueryNum = PianoSingleCpPirUtils.getRoundQueryNum(n);
        stopWatch.stop();
        long paddingTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 0, 1, paddingTime,
            String.format(
                "Server sets params: n = %d, ChunkSize = %d, ChunkNum = %d, n (pad) = %d, Q = %d",
                n, chunkSize, chunkNum, chunkSize * chunkNum, roundQueryNum
            )
        );

        // preprocessing
        preprocessing();

        logPhaseInfo(PtoState.INIT_END);
    }

    private void preprocessing() throws MpcAbortException {
        stopWatch.start();
        // stream sending the database
        for (int chunkId = 0; chunkId < chunkNum; chunkId++) {
            // concatenate database into the whole byte buffer
            ByteBuffer byteBuffer = ByteBuffer.allocate(byteL * chunkSize);
            for (int offset = 0; offset < chunkSize; offset++) {
                byteBuffer.put(paddingDatabase.getBytesData(chunkId * chunkSize + offset));
            }
            List<byte[]> streamRequestPayload = Collections.singletonList(byteBuffer.array());
            DataPacketHeader streamRequestHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_STREAM_DATABASE_REQUEST.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(streamRequestHeader, streamRequestPayload));

            // receive response
            DataPacketHeader streamResponseHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_STREAM_DATABASE_RESPONSE.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> streamResponsePayload = rpc.receive(streamResponseHeader).getPayload();
            MpcAbortPreconditions.checkArgument(streamResponsePayload.size() == 0);
            extraInfo++;
        }
        // reset current query num
        currentQueryNum = 0;
        stopWatch.stop();
        long streamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, streamTime, "Server handles " + chunkNum + " chunk");
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
        logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Server responses miss query");

        logPhaseInfo(PtoState.PTO_END);
    }

    private void respondActualQuery(List<byte[]> queryRequestPayload) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        byte[] queryByteArray = queryRequestPayload.get(0);
        MpcAbortPreconditions.checkArgument(queryByteArray.length == Short.BYTES * (chunkNum - 1));
        ByteBuffer queryByteBuffer = ByteBuffer.wrap(queryByteArray);
        int[] puncturedOffsets = new int[chunkNum - 1];
        for (int i = 0; i < chunkNum - 1; i++) {
            puncturedOffsets[i] = queryByteBuffer.getShort();
        }
        // Start to run PossibleParities
        // build the first guess assuming the punctured position is 0
        byte[] parity = new byte[byteL];
        for (int i = 0; i < chunkNum - 1; i++) {
            int chunkId = i + 1;
            int x = chunkId * chunkSize + puncturedOffsets[i];
            byte[] entry = paddingDatabase.getBytesData(x);
            BytesUtils.xori(parity, entry);
        }
        // init all guesses
        byte[][] guesses = new byte[chunkNum][byteL];
        // set the first guess
        guesses[0] = BytesUtils.clone(parity);
        // now build the rest of the guesses
        for (int misChunkId = 1; misChunkId < chunkNum; misChunkId++) {
            // The hole originally is in the (i-1)-th chunk. Now the hole should be in the i-th chunk.
            int offset = puncturedOffsets[misChunkId - 1];
            int oldX = misChunkId * chunkSize + offset;
            int newX = (misChunkId - 1) * chunkSize + offset;
            byte[] entryOld = paddingDatabase.getBytesData(oldX);
            byte[] entryNew = paddingDatabase.getBytesData(newX);
            BytesUtils.xori(parity, entryOld);
            BytesUtils.xori(parity, entryNew);
            guesses[misChunkId] = BytesUtils.clone(parity);
        }
        // respond the query
        ByteBuffer byteBuffer = ByteBuffer.allocate(chunkNum * byteL);
        for (int i = 0; i < chunkNum; i++) {
            byteBuffer.put(guesses[i]);
        }
        List<byte[]> queryResponsePayload = Collections.singletonList(byteBuffer.array());
        DataPacketHeader queryResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryResponseHeader, queryResponsePayload));
        // increase current query num
        currentQueryNum++;
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Server responses actual query");
        // when query num exceeds the maximum, rerun preprocessing.
        if (currentQueryNum >= roundQueryNum) {
            preprocessing();
        }
        logPhaseInfo(PtoState.PTO_END);
    }
}
