package edu.alibaba.mpc4j.s2pc.opf.sqoprf.pssw09;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpFactory.OprpType;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpSender;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpSenderOutput;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.AbstractSqOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.pssw09.Pssw09SqOprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * PSSW09 single-query OPRF Sender.
 *
 * @author Qixian Zhou
 * @date 2023/4/17
 */
public class Pssw09SqOprfSender extends AbstractSqOprfSender {
    /**
     * Z2c sender
     */
    private final Z2cParty z2cSender;
    /**
     * OPRP Sender
     */
    private final OprpSender oprpSender;
    /**
     * OPRP type
     */
    private final OprpType oprpType;
    /**
     * key
     */
    private Pssw09SqOprfKey key;

    public Pssw09SqOprfSender(Rpc senderRpc, Party receiverParty, Pssw09SqOprfConfig config) {
        super(Pssw09SqOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        addSubPto(z2cSender);
        OprpConfig oprpConfig = config.getOprpConfig();
        oprpSender = OprpFactory.createSender(z2cSender, receiverParty, oprpConfig);
        addSubPto(oprpSender);
        oprpType = oprpConfig.getPtoType();
    }

    @Override
    public Pssw09SqOprfKey keyGen() {
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(key);
        return new Pssw09SqOprfKey(envType, key, oprpSender.getPrpType(), oprpSender.isInvPrp());
    }

    @Override
    public void init(int maxBatchSize, SqOprfKey key) throws MpcAbortException {
        setInitInput(maxBatchSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // set key
        this.key = (Pssw09SqOprfKey) key;
        z2cSender.init(Math.min(Integer.MAX_VALUE, (int) OprpFactory.expectZ2TripleNum(oprpType, maxBatchSize)));
        oprpSender.init(maxBatchSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void oprf(int batchSize) throws MpcAbortException {
        setPtoInput(batchSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        OprpSenderOutput oprpSenderOutput = oprpSender.oprp(key.getOprpKey(), batchSize);
        stopWatch.stop();
        long oprpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprpTime, "Sender runs OPRP");

        stopWatch.start();
        List<byte[]> sharesPayload = IntStream.range(0, batchSize)
            .mapToObj(oprpSenderOutput::getShare)
            .collect(Collectors.toList());
        DataPacketHeader sharesHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_SHARES.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sharesHeader, sharesPayload));
        stopWatch.stop();
        long sharesTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, sharesTime, "Sender sends shares.");

        logPhaseInfo(PtoState.PTO_END);
    }
}