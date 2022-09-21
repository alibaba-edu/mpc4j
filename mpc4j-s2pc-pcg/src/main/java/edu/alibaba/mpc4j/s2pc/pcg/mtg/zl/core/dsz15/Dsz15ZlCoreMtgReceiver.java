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
 * DSZ15核l比特三元组生成协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/9/8
 */
public class Dsz15ZlCoreMtgReceiver extends AbstractZlCoreMtgParty {
    /**
     * COT协议发送方
     */
    private final CotSender cotSender;
    /**
     * COT协议接收方
     */
    private final CotReceiver cotReceiver;
    /**
     * 生成流密钥的伪随机数生成器
     */
    private final Prg prg;
    /**
     * 随机分量a1
     */
    private BigInteger[] a1;
    /**
     * 随机分量b1
     */
    private BigInteger[] b1;
    /**
     * 随机分量c1
     */
    private BigInteger[] c1;
    /**
     * 接收方第一次OT消息对
     */
    private BigInteger[][] receiverMessagesArray;
    /**
     * 接收方第二次COT的选择比特
     */
    private boolean[] receiverChoices;
    /**
     * 接收方第二次OT接收的消息
     */
    private BigInteger[] senderMessageArray;

    public Dsz15ZlCoreMtgReceiver(Rpc receiverRpc, Party senderParty, Dsz15ZlCoreMtgConfig config) {
        super(Dsz15ZlCoreMtgPtoDesc.getInstance(), receiverRpc, senderParty, config);
        cotSender = CotFactory.createSender(receiverRpc, senderParty, config.getCotConfig());
        cotSender.addLogLevel();
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, config.getCotConfig());
        cotReceiver.addLogLevel();
        prg = PrgFactory.createInstance(envType, byteL);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        // NCCOT协议和PCOT协议需要使用不同的taskID
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        cotSender.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        cotReceiver.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        cotSender.setParallel(parallel);
        cotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        cotSender.addLogLevel();
        cotReceiver.addLogLevel();
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        cotSender.init(delta, maxNum * l, maxNum * l);
        cotReceiver.init(maxNum * l, maxNum * l);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public ZlTriple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        initParams();
        stopWatch.stop();
        long initParamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initParamTime);

        stopWatch.start();
        // 第一轮OT协议
        CotSenderOutput cotSenderOutput = cotSender.send(num * l);
        List<byte[]> receiverMessagesPayload = generateReceiverMessagesPayload(cotSenderOutput);
        DataPacketHeader receiverMessagesHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_MESSAGES.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(receiverMessagesHeader, receiverMessagesPayload));
        stopWatch.stop();
        long sendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), sendTime);

        stopWatch.start();
        // 第二轮OT协议
        CotReceiverOutput cotReceiverOutput = cotReceiver.receive(receiverChoices);
        DataPacketHeader senderMessagesHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_MESSAGES.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> senderMessagesPayload = rpc.receive(senderMessagesHeader).getPayload();
        handleSenderMessagesPayload(cotReceiverOutput, senderMessagesPayload);
        stopWatch.stop();
        long receiveTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 3/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), receiveTime);

        stopWatch.start();
        ZlTriple receiverOutput = computeTriples();
        stopWatch.stop();
        long tripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 4/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), tripleTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }

    private void initParams() {
        // 构建三元组缓存区
        a1 = new BigInteger[num];
        b1 = new BigInteger[num];
        c1 = new BigInteger[num];
        // 构建接收方OT的选择比特和发送消息
        receiverChoices = new boolean[num * l];
        receiverMessagesArray = new BigInteger[num * l][2];
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(arrayIndex -> {
            // 客户端生成num组随机的bitLength比特长大整数。
            a1[arrayIndex] = new BigInteger(l, secureRandom);
            b1[arrayIndex] = new BigInteger(l, secureRandom);
            // 客户端为每组三元组生成l个随机大整数，并计算第一次OT时客户端的输入
            int offset = arrayIndex * l;
            IntStream.range(0, l).forEach(bitIndex -> {
                receiverMessagesArray[offset + bitIndex][0] = new BigInteger(l, this.secureRandom);
                receiverMessagesArray[offset + bitIndex][1] = a1[arrayIndex]
                    .shiftLeft(l - 1 - bitIndex).and(mask)
                    .add(receiverMessagesArray[offset + bitIndex][0]).and(mask);
            });
            byte[] byteChoices = BigIntegerUtils.nonNegBigIntegerToByteArray(b1[arrayIndex], byteL);
            boolean[] binaryChoices = BinaryUtils.byteArrayToBinary(byteChoices, l);
            System.arraycopy(binaryChoices, 0, receiverChoices, offset, l);
        });
    }

    private List<byte[]> generateReceiverMessagesPayload(CotSenderOutput cotSenderOutput) {
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        return indexIntStream
            .mapToObj(arrayIndex ->
                IntStream.range(0, l)
                    .mapToObj(bitIndex -> {
                        int offset = arrayIndex * l + bitIndex;
                        byte[][] ciphertexts = new byte[2][];
                        ciphertexts[0] = prg.extendToBytes(cotSenderOutput.getR0(offset));
                        byte[] message0 = BigIntegerUtils.nonNegBigIntegerToByteArray(receiverMessagesArray[offset][0], byteL);
                        BytesUtils.xori(ciphertexts[0], message0);
                        ciphertexts[1] = prg.extendToBytes(cotSenderOutput.getR1(offset));
                        byte[] message1 = BigIntegerUtils.nonNegBigIntegerToByteArray(receiverMessagesArray[offset][1], byteL);
                        BytesUtils.xori(ciphertexts[1], message1);
                        return ciphertexts;
                    })
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private void handleSenderMessagesPayload(CotReceiverOutput cotReceiverOutput, List<byte[]> senderMessagesPayload)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(senderMessagesPayload.size() == num * l * 2);
        byte[][] messagePairArray = senderMessagesPayload.toArray(new byte[0][]);
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        senderMessageArray = indexIntStream
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
                    .collect(Collectors.toList())
            ).flatMap(Collection::stream)
            .toArray(BigInteger[]::new);
        receiverChoices = null;
    }

    private ZlTriple computeTriples() {
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(arrayIndex -> {
            // 先循环加上客户端生成的前两元的乘积，再减去第一次OTE中客户端的输入并加上第二次OTE中客户端选择的结果
            int offset = arrayIndex * l;
            c1[arrayIndex] = a1[arrayIndex].multiply(b1[arrayIndex]).and(mask);
            IntStream.range(0, l).forEach(bitIndex ->
                c1[arrayIndex] = c1[arrayIndex]
                    .subtract(receiverMessagesArray[offset + bitIndex][0])
                    .add(senderMessageArray[offset + bitIndex]).and(mask)
            );
        });
        ZlTriple zlTriple = ZlTriple.create(l, num, a1, b1, c1);
        a1 = null;
        b1 = null;
        c1 = null;
        senderMessageArray = null;
        receiverMessagesArray = null;
        return zlTriple;
    }
}
