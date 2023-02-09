package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.alsz13;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.AbstractZ2CoreMtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotSender;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * ALSZ13-核布尔三元组生成协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/4/6
 */
public class Alsz13Z2CoreMtgReceiver extends AbstractZ2CoreMtgParty {
    /**
     * NC-COT协议接收方
     */
    private final NcCotReceiver ncCotReceiver;
    /**
     * NC-COT协议发送方
     */
    private final NcCotSender ncCotSender;
    /**
     * 偏置量
     */
    private int offset;
    /**
     * a1
     */
    private byte[] a1;
    /**
     * b1
     */
    private byte[] b1;
    /**
     * c1
     */
    private byte[] c1;

    public Alsz13Z2CoreMtgReceiver(Rpc receiverRpc, Party senderParty, Alsz13Z2CoreMtgConfig config) {
        super(Alsz13Z2CoreMtgPtoDesc.getInstance(), receiverRpc, senderParty, config);
        ncCotReceiver = NcCotFactory.createReceiver(receiverRpc, senderParty, config.getNcCotConfig());
        ncCotReceiver.addLogLevel();
        ncCotSender = NcCotFactory.createSender(receiverRpc, senderParty, config.getNcCotConfig());
        ncCotSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        ncCotReceiver.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        ncCotSender.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        ncCotReceiver.setParallel(parallel);
        ncCotSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        ncCotReceiver.addLogLevel();
        ncCotSender.addLogLevel();
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 初始化COT接收方
        ncCotReceiver.init(maxNum);
        // 初始化COT发送方
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        ncCotSender.init(delta, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Z2Triple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        initParam();
        stopWatch.stop();
        long initParamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initParamTime);

        stopWatch.start();
        firstRound();
        stopWatch.stop();
        long firstRoundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), firstRoundTime);

        stopWatch.start();
        secondRound();
        stopWatch.stop();
        long secondRoundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 3/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), secondRoundTime);

        stopWatch.start();
        // Finally, each Pi sets ci = (ai ⊙ bi) ⊕ ui ⊕ vi. This is the remaining part.
        byte[] temp = BytesUtils.and(a1, b1);
        BytesUtils.xori(c1, temp);
        stopWatch.stop();
        long generateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 4/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), generateTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return Z2Triple.create(num, a1, b1, c1);
    }

    private void initParam() {
        // 不需要初始化a1，协议执行过程会初始化
        b1 = new byte[byteNum];
        c1 = new byte[byteNum];
        // 计算偏置量
        offset = byteNum * Byte.SIZE - num;
    }

    private void firstRound() throws MpcAbortException {
        // S and R perform a silent R-OT. R obtains bits a and xa as output.
        CotReceiverOutput cotReceiverOutput = ncCotReceiver.receive();
        cotReceiverOutput.reduce(num);
        RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfType.MMO, cotReceiverOutput);
        a1 = BinaryUtils.binaryToRoundByteArray(cotReceiverOutput.getChoices());
        // R sets u = xa
        byte[][] rbArray = rotReceiverOutput.getRbArray();
        IntStream.range(0, num).forEach(tripleIndex -> {
            // 只取每个ROT的最高位1比特
            BinaryUtils.setBoolean(c1, offset + tripleIndex, rbArray[tripleIndex][0] % 2 == 1);
        });
    }

    private void secondRound() throws MpcAbortException {
        // S and R perform a silent R-OT. S obtains bits x0, x1.
        CotSenderOutput cotSenderOutput = ncCotSender.send();
        cotSenderOutput.reduce(num);
        RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfType.MMO, cotSenderOutput);
        byte[][] r0Array = rotSenderOutput.getR0Array();
        byte[][] r1Array = rotSenderOutput.getR1Array();
        // S sets b = x0 ⊕ x1 and v = x0.
        byte[] cb = new byte[byteNum];
        byte[] x1 = new byte[byteNum];
        IntStream.range(0, num).forEach(tripleIndex -> {
            // 只取每一组ROT的最高位1比特
            BinaryUtils.setBoolean(cb, offset + tripleIndex, r0Array[tripleIndex][0] % 2 == 1);
            BinaryUtils.setBoolean(b1, offset + tripleIndex, r0Array[tripleIndex][0] % 2 == 1);
            BinaryUtils.setBoolean(x1, offset + tripleIndex, r1Array[tripleIndex][0] % 2 == 1);
        });
        BytesUtils.xori(b1, x1);
        // Finally, each Pi sets ci = (ai ⊙ bi) ⊕ ui ⊕ vi. This is the ui ⊕ vi part.
        BytesUtils.xori(c1, cb);
    }
}
