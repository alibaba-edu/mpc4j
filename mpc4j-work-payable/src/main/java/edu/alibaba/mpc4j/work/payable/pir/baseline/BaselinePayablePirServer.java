package edu.alibaba.mpc4j.work.payable.pir.baseline;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtClient;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;
import edu.alibaba.mpc4j.s2pc.pir.KeyPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.StdKsPirFactory;
import edu.alibaba.mpc4j.work.payable.pir.AbstractPayablePirServer;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.work.payable.pir.baseline.BaselinePayablePirPtoDesc.getInstance;

/**
 * Baseline payable PIR server.
 *
 * @author Liqiang Peng
 * @date 2024/7/2
 */
public class BaselinePayablePirServer extends AbstractPayablePirServer {

    /**
     * keyword PIR server
     */
    private final KeyPirServer<ByteBuffer> kwPirServer;
    /**
     * mqRPMT client
     */
    private final MqRpmtClient mqRpmtClient;

    public BaselinePayablePirServer(Rpc serverRpc, Party clientParty, BaselinePayablePirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        kwPirServer = StdKsPirFactory.createServer(serverRpc, clientParty, config.getKsPirConfig());
        addSubPto(kwPirServer);
        mqRpmtClient = MqRpmtFactory.createClient(serverRpc, clientParty, config.getMqRpmtConfig());
        addSubPto(mqRpmtClient);
    }

    @Override
    public void init(Map<ByteBuffer, byte[]> keyValueMap, int byteL) throws MpcAbortException {
        setInitInput(keyValueMap, byteL);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // encode map
        kwPirServer.init(keyValueMap, byteL * Byte.SIZE, 1);
        mqRpmtClient.init(n, 2);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public boolean pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        kwPirServer.pir();
        stopWatch.stop();
        long kwPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, kwPirTime, "Server executes keyword PIR");

        stopWatch.start();
        boolean[] mqRpmtOutput = mqRpmtClient.mqRpmt(new HashSet<>(keywordList), 2);
        stopWatch.stop();
        long mqRpmtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, mqRpmtTime, "Server executes mqRPMT");

        logPhaseInfo(PtoState.PTO_END);
        return IntStream.range(0, mqRpmtOutput.length).anyMatch(i -> mqRpmtOutput[i]);
    }
}
