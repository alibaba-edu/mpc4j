package edu.alibaba.mpc4j.s2pc.pir.index.batch.naive;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.AbstractBatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirServer;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.batch.naive.NaiveBatchIndexPirPtoDesc.*;

/**
 * Naive Batch PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/7/14
 */
public class NaiveBatchIndexPirServer extends AbstractBatchIndexPirServer {

    /**
     * single index PIR server
     */
    private final SingleIndexPirServer server;

    public NaiveBatchIndexPirServer(Rpc serverRpc, Party clientParty, NaiveBatchIndexPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        server = SingleIndexPirFactory.createServer(serverRpc, clientParty, config.getSingleIndexPirConfig());
        addSubPto(server);
    }

    @Override
    public void init(NaiveDatabase database, int maxRetrievalSize) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);
        setInitInput(database, maxRetrievalSize);

        stopWatch.start();
        // init single index PIR server
        server.init(database);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive query
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> clientQueryPayload = rpc.receive(clientQueryHeader).getPayload();
        int querySize = server.getQuerySize();
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() % querySize == 0);

        // generate response
        stopWatch.start();
        int count = clientQueryPayload.size() / querySize;
        IntStream intStream = IntStream.range(0, count);
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> responsePayload = intStream
            .mapToObj(i -> {
                try {
                    return server.generateResponse(
                        clientQueryPayload.subList(i * querySize, (i + 1) * querySize)
                    );
                } catch (MpcAbortException e) {
                    e.printStackTrace();
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, responsePayload));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates response");

        logPhaseInfo(PtoState.PTO_END);
    }
}
