package edu.alibaba.mpc4j.s2pc.opf.sqoprf.nr04;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.AbstractSqOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.nr04.Nr04EccSqOprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * NR04 ECC single-query OPRF sender.
 *
 * @author Qixian Zhou
 * @date 2023/4/12
 */
public class Nr04EccSqOprfSender extends AbstractSqOprfSender {
    /**
     * ecc
     */
    private final Ecc ecc;
    /**
     * Zp instance
     */
    private final Zp zp;
    /**
     * use compressed encode
     */
    private final boolean compressEncode;
    /**
     * COT sender
     */
    private final CotSender coreSender;
    /**
     * key
     */
    private Nr04EccSqOprfKey key;
    /**
     * R_{i,j}^0, length: n * N_C^{max}
     */
    private BigInteger[][] r0Array;
    /**
     * R_{i,j}^1, length: n * N_C^{max}
     */
    private BigInteger[][] r1Array;

    public Nr04EccSqOprfSender(Rpc senderRpc, Party receiverParty, Nr04EccSqOprfConfig config) {
        super(Nr04EccSqOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        ecc = EccFactory.createInstance(envType);
        zp = ZpFactory.createInstance(envType, ecc.getN());
        compressEncode = config.getCompressEncode();
        coreSender = CotFactory.createSender(senderRpc, receiverParty, config.getCotConfig());
        addSubPto(coreSender);
    }

    @Override
    public SqOprfKey keyGen() {
        BigInteger[] a0Array = new BigInteger[CommonConstants.BLOCK_BIT_LENGTH];
        BigInteger[] a1Array = new BigInteger[CommonConstants.BLOCK_BIT_LENGTH];
        for (int i = 0; i < CommonConstants.BLOCK_BIT_LENGTH; i++) {
            a0Array[i] = zp.createNonZeroRandom(secureRandom);
            a1Array[i] = zp.createNonZeroRandom(secureRandom);
        }
        return new Nr04EccSqOprfKey(envType, a0Array, a1Array);
    }

    @Override
    public void init(int maxBatchSize, SqOprfKey key) throws MpcAbortException {
        setInitInput(maxBatchSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // sets the key
        this.key = (Nr04EccSqOprfKey) key;
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, initTime);

        stopWatch.start();
        // init COTs, where max number of OTs = N_C^{max} * κ
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        int maxOtNum = maxBatchSize * CommonConstants.BLOCK_BIT_LENGTH;
        secureRandom.nextBytes(delta);
        coreSender.init(delta, maxOtNum);
        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initCotTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void oprf(int batchSize) throws MpcAbortException {
        setPtoInput(batchSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // sender calculates and sends g^{r^{-1}}
        List<byte[]> grInvPayload = generateGrInvPayload();
        DataPacketHeader grInvHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_GR_INV.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(grInvHeader, grInvPayload));
        stopWatch.stop();
        long grInvTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, grInvTime, "Sender sends g^{r^{-1}}.");

        stopWatch.start();
        CotSenderOutput cotSenderOutput = coreSender.send(batchSize * CommonConstants.BLOCK_BIT_LENGTH);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, cotTime, "Sender runs COT");

        stopWatch.start();
        // sender sends encrypted m0 and m1
        List<byte[]> messagePayload = generateMessagePayload(cotSenderOutput);
        DataPacketHeader messageHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_MESSAGE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(messageHeader, messagePayload));
        long messageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, messageTime, "Sender sends messages");

        logPhaseInfo(PtoState.PTO_END);
    }

    private List<byte[]> generateGrInvPayload() {
        r0Array = new BigInteger[batchSize][CommonConstants.BLOCK_BIT_LENGTH];
        r1Array = new BigInteger[batchSize][CommonConstants.BLOCK_BIT_LENGTH];
        IntStream maxBatchIntStream = IntStream.range(0, maxBatchSize);
        maxBatchIntStream = parallel ? maxBatchIntStream.parallel() : maxBatchIntStream;
        return maxBatchIntStream
            .mapToObj(i -> {
                BigInteger ri = BigInteger.ONE;
                for (int j = 0; j < CommonConstants.BLOCK_BIT_LENGTH; j++) {
                    BigInteger r = zp.createRandom(secureRandom);
                    r0Array[i][j] = zp.mul(r, key.getA0Array(j));
                    r1Array[i][j] = zp.mul(r, key.getA1Array(j));
                    ri = zp.mul(ri, r);
                }
                return zp.inv(ri);
            })
            .map(riInv -> ecc.encode(ecc.multiply(ecc.getG(), riInv), compressEncode))
            .collect(Collectors.toList());
    }


    private List<byte[]> generateMessagePayload(CotSenderOutput cotSenderOutput) {
        int elementByteLength = zp.getElementByteLength();
        Prg prg = PrgFactory.createInstance(envType, elementByteLength);
        IntStream batchStream = IntStream.range(0, batchSize);
        batchStream = parallel ? batchStream.parallel() : batchStream;
        return batchStream
            .mapToObj(i -> {
                byte[][] messagePairs = new byte[CommonConstants.BLOCK_BIT_LENGTH * 2][];
                for (int j = 0; j < CommonConstants.BLOCK_BIT_LENGTH; j++) {
                    int ij = i * CommonConstants.BLOCK_BIT_LENGTH + j;
                    byte[] key0 = cotSenderOutput.getR0(ij);
                    key0 = prg.extendToBytes(key0);
                    byte[] key1 = cotSenderOutput.getR1(ij);
                    key1 = prg.extendToBytes(key1);
                    // encrypt R0 and R1
                    messagePairs[j * 2] = BigIntegerUtils.nonNegBigIntegerToByteArray(r0Array[i][j], elementByteLength);
                    BytesUtils.xori(messagePairs[j * 2], key0);
                    messagePairs[j * 2 + 1] = BigIntegerUtils.nonNegBigIntegerToByteArray(r1Array[i][j], elementByteLength);
                    BytesUtils.xori(messagePairs[j * 2 + 1], key1);
                }
                return messagePairs;
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
    }
}
