package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * 抽象多方计算协议。
 *
 * @author Weiran Liu
 * @date 2022/4/29
 */
public abstract class AbstractMultiPartyPto implements MultiPartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiPartyPto.class);
    /**
     * 显示日志层级
     */
    private static final int DISPLAY_LOG_LEVEL = 2;
    /**
     * 任务ID的PRF
     */
    protected final Prf taskIdPrf;
    /**
     * 协议描述信息
     */
    protected final PtoDesc ptoDesc;
    /**
     * 通信接口
     */
    protected final Rpc rpc;
    /**
     * 其他参与方
     */
    private final Party[] otherParties;
    /**
     * 秒表，用于记录时间
     */
    protected final StopWatch stopWatch;
    /**
     * 任务ID
     */
    protected long taskId;
    /**
     * 协议日志层数
     */
    private int logLevel;
    /**
     * 协议开始日志前缀
     */
    protected String ptoBeginLogPrefix;
    /**
     * 协议中间步骤前缀
     */
    protected String ptoStepLogPrefix;
    /**
     * 协议结束日志前缀
     */
    protected String ptoEndLogPrefix;
    /**
     * 额外信息
     */
    protected long extraInfo;

    /**
     * 构建两方计算协议。虽然Rpc可以得到所有参与方信息，但实际协议有可能不会用到全部的参与方，且协议执行时会用到参与方的顺序。
     * 为此，要通过{@code otherParties}指定其他参与方。
     *
     * @param ptoDesc      协议描述信息。
     * @param rpc          通信接口。
     * @param otherParties 其他参与方。
     */
    protected AbstractMultiPartyPto(PtoDesc ptoDesc, Rpc rpc, Party... otherParties) {
        // 验证其他参与方均在通信参与方之中
        Set<Party> partySet = rpc.getPartySet();
        for (Party otherParty : otherParties) {
            assert partySet.contains(otherParty) : otherParty.toString() + " does not in the Party Set";
        }
        this.ptoDesc = ptoDesc;
        this.rpc = rpc;
        this.otherParties = otherParties;
        // 为了保证所有平台都能够使用，这里强制要求用JDK的Prf
        taskIdPrf = PrfFactory.createInstance(PrfFactory.PrfType.JDK_AES_CBC, Long.BYTES);
        taskIdPrf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        stopWatch = new StopWatch();
        taskId = 0L;
        logLevel = 0;
        ptoBeginLogPrefix = "↘";
        ptoStepLogPrefix = "    ↓";
        ptoEndLogPrefix = "↙";
        extraInfo = 0;
    }

    @Override
    public void addLogLevel() {
        logLevel++;
        ptoBeginLogPrefix = "    " + ptoBeginLogPrefix;
        ptoStepLogPrefix = "    " + ptoStepLogPrefix;
        ptoEndLogPrefix = "    " + ptoEndLogPrefix;
    }

    /**
     * Log a message at the INFO level if {@code logLevel} is not greater than {@code DISPLAY_LOG_LEVEL}.
     *
     * @param message the message string to be logged.
     */
    protected void info(String message) {
        if (logLevel < DISPLAY_LOG_LEVEL) {
            LOGGER.info(message);
        }
    }

    /**
     * Log a message at the INFO level according to the specified format and arguments, if {@code logLevel} is not
     * greater than {@code DISPLAY_LOG_LEVEL}.
     *
     * @param format the format string.
     * @param arg0   the first argument.
     * @param arg1   the second argument.
     */
    protected void info(String format, Object arg0, Object arg1) {
        if (logLevel < DISPLAY_LOG_LEVEL) {
            LOGGER.info(format, arg0, arg1);
        }
    }

    /**
     * Log a message at the INFO level according to the specified format and arguments, if {@code logLevel} is not
     * greater than {@code DISPLAY_LOG_LEVEL}.
     *
     * @param format    the format string.
     * @param arguments a list of 3 or more arguments
     */
    protected void info(String format, Object... arguments) {
        if (logLevel < DISPLAY_LOG_LEVEL) {
            LOGGER.info(format, arguments);
        }
    }

    @Override
    public void setTaskId(long taskId) {
        assert taskId >= 0;
        this.taskId = taskId;
    }

    @Override
    public long getTaskId() {
        return taskId;
    }

    @Override
    public Rpc getRpc() {
        return rpc;
    }

    @Override
    public PtoDesc getPtoDesc() {
        return ptoDesc;
    }

    @Override
    public Party[] otherParties() {
        return otherParties;
    }
}
