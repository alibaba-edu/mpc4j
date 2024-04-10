package edu.alibaba.mpc4j.s2pc.opf.sqoprf.nr04;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.AbstractSqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.nr04.Nr04EccSqOprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * NR04 ECC single-query OPRF receiver.
 *
 * @author Qixian Zhou
 * @date 2023/4/12
 */
public class Nr04EccSqOprfReceiver extends AbstractSqOprfReceiver {
    /**
     * ecc
     */
    private final Ecc ecc;
    /**
     * Zp instance
     */
    private final Zp zp;
    /**
     * hash function
     */
    private final Hash hash;
    /**
     * COT receiver
     */
    private final CotReceiver cotReceiver;
    /**
     * g^{r_i^{-1}}
     */
    private ECPoint[] grInvPoints;

    public Nr04EccSqOprfReceiver(Rpc receiverRpc, Party senderParty, Nr04EccSqOprfConfig config) {
        super(Nr04EccSqOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, config.getCotConfig());
        addSubPto(cotReceiver);
        ecc = EccFactory.createInstance(envType);
        zp = ZpFactory.createInstance(envType, ecc.getN());
        hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public void init(int maxBatchSize) throws MpcAbortException {
        setInitInput(maxBatchSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init COTs, where max number of OTs = N_C^{max} * κ
        int maxCotNum = maxBatchSize * CommonConstants.BLOCK_BIT_LENGTH;
        cotReceiver.init(maxCotNum);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, cotTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SqOprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException {
        setPtoInput(inputs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // hash inputs
        boolean[][] binaryHashes = Arrays.stream(inputs)
            .map(hash::digestToBytes)
            .map(hashInput -> BinaryUtils.byteArrayToBinary(hashInput, CommonConstants.BLOCK_BIT_LENGTH))
            .toArray(boolean[][]::new);
        boolean[] flatBinaryHashes = new boolean[batchSize * CommonConstants.BLOCK_BIT_LENGTH];
        for (int i = 0; i < batchSize; i++) {
            System.arraycopy(binaryHashes[i], 0, flatBinaryHashes, i * CommonConstants.BLOCK_BIT_LENGTH,
                CommonConstants.BLOCK_BIT_LENGTH);
        }
        DataPacketHeader grInvHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_GR_INV.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> grInvPayload = rpc.receive(grInvHeader).getPayload();
        MpcAbortPreconditions.checkArgument(grInvPayload.size() == maxBatchSize);
        grInvPoints = grInvPayload.stream()
            .map(ecc::decode)
            .toArray(ECPoint[]::new);
        stopWatch.stop();
        long grInvTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, grInvTime, "Receiver handles g^{r^{-1}}.");

        stopWatch.start();
        CotReceiverOutput cotReceiverOutput = cotReceiver.receive(flatBinaryHashes);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, cotTime, "Sender runs COT");

        DataPacketHeader messageHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_MESSAGE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> messagePayload = rpc.receive(messageHeader).getPayload();

        stopWatch.start();
        // handle messages
        SqOprfReceiverOutput receiverOutput = handleMessagePayload(messagePayload, cotReceiverOutput);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, oprfTime, "Receiver generates OPRFs.");

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private SqOprfReceiverOutput handleMessagePayload(List<byte[]> messagePayload, CotReceiverOutput cotReceiverOutput)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(messagePayload.size() == 2 * batchSize * CommonConstants.BLOCK_BIT_LENGTH);
        byte[][] flatMessagePairs = messagePayload.toArray(new byte[0][]);
        Prg prg = PrgFactory.createInstance(envType, zp.getElementByteLength());
        Kdf kdf = KdfFactory.createInstance(envType);
        IntStream batchStream = IntStream.range(0, batchSize);
        batchStream = parallel ? batchStream.parallel() : batchStream;
        byte[][] prfs = batchStream
            .mapToObj(i -> {
                // C = R^{y_i[j]}
                BigInteger c = BigInteger.ONE;
                for (int j = 0; j < CommonConstants.BLOCK_BIT_LENGTH; j++) {
                    int ij = i * CommonConstants.BLOCK_BIT_LENGTH + j;
                    byte[] key = cotReceiverOutput.getRb(ij);
                    key = prg.extendToBytes(key);
                    boolean yi = cotReceiverOutput.getChoice(ij);
                    byte[] message = yi ? flatMessagePairs[2 * ij + 1] : flatMessagePairs[2 * ij];
                    BytesUtils.xori(message, key);
                    BigInteger ri = BigIntegerUtils.byteArrayToNonNegBigInteger(message);
                    c = zp.mul(c, ri);
                }
                // c * grInv
                return kdf.deriveKey(ecc.encode(ecc.multiply(grInvPoints[i], c), false));
            })
            .toArray(byte[][]::new);
        return new SqOprfReceiverOutput(inputs, prfs);
    }
}
