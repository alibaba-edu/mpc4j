package edu.alibaba.mpc4j.work.payable.pir.baseline;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtServer;
import edu.alibaba.mpc4j.s2pc.pir.KeyPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.StdKsPirFactory;
import edu.alibaba.mpc4j.work.payable.pir.AbstractPayablePirClient;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.work.payable.pir.baseline.BaselinePayablePirPtoDesc.getInstance;

/**
 * Baseline payable PIR client.
 *
 * @author Liqiang Peng
 * @date 2024/7/2
 */
public class BaselinePayablePirClient extends AbstractPayablePirClient {

    /**
     * keyword PIR client
     */
    private final KeyPirClient<ByteBuffer> kwPirClient;
    /**
     * mqRPMT server
     */
    private final MqRpmtServer mqRpmtServer;


    public BaselinePayablePirClient(Rpc clientRpc, Party serverParty, BaselinePayablePirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        kwPirClient = StdKsPirFactory.createClient(clientRpc, serverParty, config.getKsPirConfig());
        addSubPto(kwPirClient);
        mqRpmtServer = MqRpmtFactory.createServer(clientRpc, serverParty, config.getMqRpmtConfig());
        addSubPto(mqRpmtServer);
    }

    @Override
    public void init(int n, int byteL) throws MpcAbortException {
        setInitInput(n, byteL);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        kwPirClient.init(n, byteL * Byte.SIZE, 1);
        mqRpmtServer.init(2, n);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[] pir(ByteBuffer retrievalKey) throws MpcAbortException {
        setPtoInput(retrievalKey);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // run MP-OPRF
        stopWatch.start();
        byte[] pirResult = kwPirClient.pir(retrievalKey);
        stopWatch.stop();
        long kwPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, kwPirTime, "Client executes keyword PIR");

        stopWatch.start();
        Set<ByteBuffer> mqRpmtInput = new HashSet<>();
        mqRpmtInput.add(retrievalKey);
        byte[] randomItem = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(randomItem);
        mqRpmtInput.add(ByteBuffer.wrap(randomItem));
        mqRpmtServer.mqRpmt(mqRpmtInput, n);
        stopWatch.stop();
        long mqRpmtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, mqRpmtTime, "Client executes mqRPMT");

        logPhaseInfo(PtoState.PTO_END);
        return pirResult;
    }
}
