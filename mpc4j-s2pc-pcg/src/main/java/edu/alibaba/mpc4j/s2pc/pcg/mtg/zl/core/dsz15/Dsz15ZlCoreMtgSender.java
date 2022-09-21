package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlTriple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.AbstractZlCoreMtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15.Dsz15ZlCoreMtgPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * DSZ15核l比特三元组生成协议发送方。
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/9/8
 */
public class Dsz15ZlCoreMtgSender extends AbstractZlCoreMtgParty {
    /**
     * COT协议接收方
     */
    private final CotReceiver cotReceiver;
    /**
     * COT协议发送方
     */
    private final CotSender cotSender;
    /**
     * 生成流密钥的伪随机数生成器
     */
    private final Prg prg;
    /**
     * 随机分量a0
     */
    private BigInteger[] a0;
    /**
     * 随机分量b0
     */
    private BigInteger[] b0;
    /**
     * 随机分量c0
     */
    private BigInteger[] c0;
    /**
     * 发送方第一次COT的选择比特
     */
    private boolean[] senderChoices;
    /**
     * 发送方第一次OT接收的消息
     */
    private BigInteger[] receiverMessageArray;
    /**
     * 发送方第二次OT消息对
     */
    private BigInteger[][] senderMessagesArray;

    public Dsz15ZlCoreMtgSender(Rpc senderRpc, Party receiverParty, Dsz15ZlCoreMtgConfig config) {
        super(Dsz15ZlCoreMtgPtoDesc.getInstance(), senderRpc, receiverParty, config);
        cotReceiver = CotFactory.createReceiver(senderRpc, receiverParty, config.getCotConfig());
        cotReceiver.addLogLevel();
        cotSender = CotFactory.createSender(senderRpc, receiverParty, config.getCotConfig());
        cotSender.addLogLevel();
        prg = PrgFactory.createInstance(envType, byteL);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        // NCCOT协议和PCOT协议需要使用不同的taskID
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        cotReceiver.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        cotSender.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        cotReceiver.setParallel(parallel);
        cotSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        cotReceiver.addLogLevel();
        cotSender.addLogLevel();
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        cotReceiver.init(maxNum * l, maxNum * l);
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        cotSender.init(delta, maxNum * l, maxNum * l);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public ZlTriple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        initParams();
        stopWatch.stop();
        long initParamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initParamTime);

        stopWatch.start();
        // 第一轮OT协议
        CotReceiverOutput cotReceiverOutput = cotReceiver.receive(senderChoices);
        DataPacketHeader receiverMessagesHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_MESSAGES.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> receiverMessagesPayload = rpc.receive(receiverMessagesHeader).getPayload();
        handleReceiverMessagesPayload(cotReceiverOutput, receiverMessagesPayload);
        stopWatch.stop();
        long receiveTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), receiveTime);

        stopWatch.start();
        // 第二轮OT协议
        CotSenderOutput cotSenderOutput = cotSender.send(num * l);
        List<byte[]> senderMessagesPayload = generateSenderMessagesPayload(cotSenderOutput);
        DataPacketHeader senderMessagesHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_MESSAGES.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(senderMessagesHeader, senderMessagesPayload));
        stopWatch.stop();
        long sendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 3/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), sendTime);

        stopWatch.start();
        ZlTriple senderOutput = computeTriples();
        stopWatch.stop();
        long tripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 4/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), tripleTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private void initParams() {
        // 构建三元组缓存区
        a0 = new BigInteger[num];
        b0 = new BigInteger[num];
        c0 = new BigInteger[num];
        // 构建发送方OT的选择比特和发送消息
        senderChoices = new boolean[num * l];
        senderMessagesArray = new BigInteger[num * l][2];
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(arrayIndex -> {
            // 生成num组长度为l比特的随机大整数
            a0[arrayIndex] = new BigInteger(l, secureRandom);
            b0[arrayIndex] = new BigInteger(l, secureRandom);
            // 为每组三元组生成l个随机大整数，并计算第二次OT时的消息
            int offset = arrayIndex * l;
            IntStream.range(0, l).forEach(bitIndex -> {
                senderMessagesArray[offset + bitIndex][0] = new BigInteger(l, secureRandom);
                senderMessagesArray[offset + bitIndex][1] = a0[arrayIndex]
                    .shiftLeft(l - 1 - bitIndex).and(mask)
                    .add(senderMessagesArray[offset + bitIndex][0]).and(mask);
            });
            byte[] byteChoices = BigIntegerUtils.nonNegBigIntegerToByteArray(b0[arrayIndex], byteL);
            boolean[] binaryChoices = BinaryUtils.byteArrayToBinary(byteChoices, l);
            System.arraycopy(binaryChoices, 0, senderChoices, offset, l);
        });
    }

    private void handleReceiverMessagesPayload(CotReceiverOutput cotReceiverOutput, List<byte[]> receiverMessagesPayload)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(receiverMessagesPayload.size() == num * l * 2);
        byte[][] messagePairArray = receiverMessagesPayload.toArray(new byte[0][]);
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        receiverMessageArray = indexIntStream
            .mapToObj(arrayIndex ->
                IntStream.range(0, l)
                    .mapToObj(bitIndex -> {
                        int offset = arrayIndex * l + bitIndex;
                        byte[] message = cotReceiverOutput.getRb(offset);
                        message = prg.extendToBytes(message);
                        if (cotReceiverOutput.getChoice(offset)) {
                            BytesUtils.xori(message, messagePairArray[2 * offset + 1]);
                        } else {
                            BytesUtils.xori(message, messagePairArray[2 * offset]);
                        }
                        return BigIntegerUtils.byteArrayToNonNegBigInteger(message);
                    })
                    .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .toArray(BigInteger[]::new);
        senderChoices = null;
    }

    private List<byte[]> generateSenderMessagesPayload(CotSenderOutput cotSenderOutput) {
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        return indexIntStream
            .mapToObj(arrayIndex ->
                IntStream.range(0, l)
                    .mapToObj(bitIndex -> {
                        int offset = arrayIndex * l + bitIndex;
                        byte[][] ciphertexts = new byte[2][];
                        ciphertexts[0] = prg.extendToBytes(cotSenderOutput.getR0(offset));
                        byte[] message0 = BigIntegerUtils.nonNegBigIntegerToByteArray(senderMessagesArray[offset][0], byteL);
                        BytesUtils.xori(ciphertexts[0], message0);
                        ciphertexts[1] = prg.extendToBytes(cotSenderOutput.getR1(offset));
                        byte[] message1 = BigIntegerUtils.nonNegBigIntegerToByteArray(senderMessagesArray[offset][1], byteL);
                        BytesUtils.xori(ciphertexts[1], message1);
                        return ciphertexts;
                    })
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private ZlTriple computeTriples() {
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(arrayIndex -> {
            // 先循环加上客户端生成的前两元的乘积，再加上第一次OTE中服务端选择的结果并减去第二次OTE中服务端的输入
            int offset = arrayIndex * l;
            c0[arrayIndex] = a0[arrayIndex].multiply(b0[arrayIndex]).and(mask);
            IntStream.range(0, l).forEach(bitIndex ->
                c0[arrayIndex] = c0[arrayIndex]
                    .subtract(senderMessagesArray[offset + bitIndex][0])
                    .add(receiverMessageArray[offset + bitIndex]).and(mask)
            );
        });
        ZlTriple zlTriple = ZlTriple.create(l, num, a0, b0, c0);
        a0 = null;
        b0 = null;
        c0 = null;
        receiverMessageArray = null;
        senderMessagesArray = null;
        return zlTriple;
    }
}
