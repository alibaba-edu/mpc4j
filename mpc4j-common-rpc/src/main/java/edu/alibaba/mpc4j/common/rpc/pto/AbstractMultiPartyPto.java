package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Abstract multi-party protocol.
 *
 * @author Weiran Liu
 * @date 2022/4/29
 */
public abstract class AbstractMultiPartyPto implements MultiPartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiPartyPto.class);
    /**
     * display log level.
     */
    private static final int DISPLAY_LOG_LEVEL = 2;
    /**
     * maximal number of sub-protocols. Note that some protocols would have many levels (e.g., PSU based on SKE).
     */
    protected static final int MAX_SUB_PROTOCOL_NUM = 4;
    /**
     * maximal tree level
     */
    protected static final int MAX_TREE_LEVEL = (int) Math.floor(Math.log(Integer.MAX_VALUE) / Math.log(MAX_SUB_PROTOCOL_NUM + 1));
    /**
     * the PRF used to extend the task ID.
     */
    protected final Prf taskIdPrf;
    /**
     * protocol description.
     */
    protected final PtoDesc ptoDesc;
    /**
     * the invoked rpc instance.
     */
    protected final Rpc rpc;
    /**
     * other parties' information.
     */
    private final Party[] otherParties;
    /**
     * the stopwatch, used to record times for each step.
     */
    protected final StopWatch stopWatch;
    /**
     * sub protocols
     */
    private final List<MultiPartyPto> subPtos;
    /**
     * tree level
     */
    private int treeLevel;
    /**
     * row level
     */
    private int rowLevel;
    /**
     * task ID
     */
    private int taskId;
    /**
     * the tree ID
     */
    private int treeId;
    /**
     * encode task ID
     */
    protected long encodeTaskId;
    /**
     * the log prefix for beginning a task
     */
    private String ptoBeginLogPrefix;
    /**
     * the log prefix for each step
     */
    private String ptoStepLogPrefix;
    /**
     * the log prefix for ending a task
     */
    private String ptoEndLogPrefix;
    /**
     * the extra information
     */
    protected long extraInfo;
    /**
     * party state
     */
    protected PartyState partyState;
    /**
     * environment
     */
    protected final EnvType envType;
    /**
     * secure random state
     */
    protected SecureRandom secureRandom;
    /**
     * parallel computing
     */
    protected boolean parallel;

    protected AbstractMultiPartyPto(PtoDesc ptoDesc, MultiPartyPtoConfig config, Rpc rpc, Party... otherParties) {
        // verify other parties are all valid.
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
        subPtos = new ArrayList<>(MAX_SUB_PROTOCOL_NUM);
        treeLevel = 0;
        rowLevel = 0;
        treeId = 0;
        taskId = 0;
        encodeTaskId = 0L;
        ptoBeginLogPrefix = "↘";
        ptoStepLogPrefix = "    ↓";
        ptoEndLogPrefix = "↙";
        extraInfo = 0;
        partyState = PartyState.NON_INITIALIZED;
        envType = config.getEnvType();
        secureRandom = new SecureRandom();
        parallel = false;
    }

    protected void addSubPtos(MultiPartyPto subPto) {
        int rowLevel = subPtos.size();
        subPtos.add(subPto);
        MathPreconditions.checkLessOrEqual("# of sub-protocols", subPtos.size(), MAX_SUB_PROTOCOL_NUM);
        subPto.addTreeLevel(rowLevel, taskId, treeId);
    }

    @Override
    public void addTreeLevel(int rowLevel, int taskId, int parentTreeId) {
        MathPreconditions.checkNonNegativeInRange("rowLevel", rowLevel, MAX_SUB_PROTOCOL_NUM);
        treeLevel++;
        MathPreconditions.checkNonNegativeInRange("treeLevel", treeLevel, MAX_TREE_LEVEL);
        this.rowLevel = rowLevel;
        this.taskId = taskId;
        treeId = (int) Math.pow(MAX_SUB_PROTOCOL_NUM, treeLevel) * (rowLevel + 1) + parentTreeId;
        encodeTaskId = (((long) treeId) << Integer.SIZE) + taskId;
        ptoBeginLogPrefix = "    " + ptoBeginLogPrefix;
        ptoStepLogPrefix = "    " + ptoStepLogPrefix;
        ptoEndLogPrefix = "    " + ptoEndLogPrefix;
        // set sub-protocols
        for (int subPtoIndex = 0; subPtoIndex < subPtos.size(); subPtoIndex++) {
            subPtos.get(subPtoIndex).addTreeLevel(subPtoIndex, taskId, treeId);
        }
    }

    @Override
    public void setTaskId(int taskId) {
        // taskId >= 0
        MathPreconditions.checkNonNegative("taskId", taskId);
        // only the root protocol (treeId = 0) can set the task ID.
        MathPreconditions.checkEqual("treeId", "0", treeId, 0);

        this.taskId = taskId;
        encodeTaskId = (((long) treeId) << Integer.SIZE) + taskId;
        // set sub-protocols
        for (MultiPartyPto subPto : subPtos) {
            subPto.setEncodeTaskId(taskId, treeId);
        }
    }

    @Override
    public int getTaskId() {
        return taskId;
    }

    @Override
    public void setEncodeTaskId(int taskId, int parentTreeId) {
        // taskId >= 0
        MathPreconditions.checkNonNegative("taskId", taskId);
        // parentTreeId >= 0
        MathPreconditions.checkNonNegative("treeId", treeId);
        // tree level must be greater than 0, so that it is not the root protocol
        MathPreconditions.checkPositive("treeLevel", treeLevel);

        this.taskId = taskId;
        treeId = (int) Math.pow(MAX_SUB_PROTOCOL_NUM, treeLevel) * (rowLevel + 1) + parentTreeId;
        encodeTaskId = (((long) treeId) << Integer.SIZE) + taskId;
        // set sub-protocols
        for (MultiPartyPto subPto : subPtos) {
            subPto.setEncodeTaskId(taskId, treeId);
        }
    }

    @Override
    public long getEncodeTaskId() {
        return encodeTaskId;
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
    public String getPtoName() {
        return ptoDesc.getPtoName();
    }

    @Override
    public Party[] otherParties() {
        return otherParties;
    }

    @Override
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
        // set sub-protocols
        for (MultiPartyPto subPto : subPtos) {
            subPto.setParallel(parallel);
        }
    }

    @Override
    public void setSecureRandom(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
        // set sub-protocols
        for (MultiPartyPto subPto : subPtos) {
            subPto.setSecureRandom(secureRandom);
        }
    }

    @Override
    public boolean getParallel() {
        return parallel;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    /**
     * init, check and update party state.
     */
    protected void initState() {
        // we cannot automatically initialize sub-protocols, since each sub-protocol would have distinct initialize API.
        switch (partyState) {
            case NON_INITIALIZED:
            case INITIALIZED:
                partyState = PartyState.INITIALIZED;
                return;
            case DESTROYED:
            default:
                throw new IllegalStateException("Party state must not be " + PartyState.DESTROYED);
        }
    }

    @Override
    public void checkInitialized() {
        switch (partyState) {
            case INITIALIZED:
                // check sub-protocols
                for (MultiPartyPto subPto : subPtos) {
                    subPto.checkInitialized();
                }
                return;
            case NON_INITIALIZED:
            case DESTROYED:
            default:
                throw new IllegalStateException("Party state must not be " + PartyState.DESTROYED);
        }
    }

    @Override
    public void destroy() {
        switch (partyState) {
            case NON_INITIALIZED:
            case INITIALIZED:
                partyState = PartyState.DESTROYED;
                // destroy sub-protocols
                for (MultiPartyPto subPto : subPtos) {
                    subPto.destroy();
                }
                return;
            case DESTROYED:
            default:
                throw new IllegalStateException("Party state must not be " + PartyState.DESTROYED);
        }
    }

    protected void logPhaseInfo(PtoState ptoState) {
        switch (ptoState) {
            case INIT_BEGIN:
                info("{}{} {} Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName());
                break;
            case INIT_END:
                info("{}{} {} Init end", ptoEndLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName());
                break;
            case PTO_BEGIN:
                info("{}{} {} Pto begin", ptoBeginLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName());
                break;
            case PTO_END:
                info("{}{} {} Pto end", ptoEndLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName());
                break;
            default:
                throw new IllegalStateException("Invalid " + PtoState.class.getSimpleName() + ": " + ptoState);
        }
    }

    protected void logPhaseInfo(PtoState ptoState, String description) {
        switch (ptoState) {
            case INIT_BEGIN:
                info(
                    "{}{} {} Init begin: {}",
                    ptoBeginLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName(), description
                );
                break;
            case INIT_END:
                info(
                    "{}{} {} Init end: {}",
                    ptoEndLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName(), description
                );
                break;
            case PTO_BEGIN:
                info(
                    "{}{} {} Pto begin: {}",
                    ptoBeginLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName(), description
                );
                break;
            case PTO_END:
                info(
                    "{}{} {} Pto end: {}",
                    ptoEndLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName(), description
                );
                break;
            default:
                throw new IllegalStateException("Invalid " + PtoState.class.getSimpleName() + ": " + ptoState);
        }
    }

    protected void logStepInfo(PtoState ptoState, int stepIndex, int totalStepIndex, long time) {
        assert stepIndex >= 0 && stepIndex <= totalStepIndex
            : "step index must be in range [0, " + totalStepIndex + "]: " + stepIndex;
        switch (ptoState) {
            case INIT_STEP:
                info("{}{} {} init Step {}/{} ({}ms)",
                    ptoStepLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName(),
                    stepIndex, totalStepIndex, time
                );
                break;
            case PTO_STEP:
                info("{}{} {} Step {}/{} ({}ms)",
                    ptoStepLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName(),
                    stepIndex, totalStepIndex, time
                );
                break;
            default:
                throw new IllegalStateException("Invalid " + PtoState.class.getSimpleName() + ": " + ptoState);
        }
    }

    protected void logStepInfo(PtoState ptoState, int stepIndex, int totalStepIndex, long time, String description) {
        assert stepIndex >= 0 && stepIndex <= totalStepIndex
            : "step index must be in range [0, " + totalStepIndex + "]: " + stepIndex;
        switch (ptoState) {
            case INIT_STEP:
                info("{}{} {} init Step {}/{} ({}ms): {}",
                    ptoStepLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName(),
                    stepIndex, totalStepIndex, time, description
                );
                break;
            case PTO_STEP:
                info("{}{} {} Step {}/{} ({}ms): {}",
                    ptoStepLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName(),
                    stepIndex, totalStepIndex, time, description
                );
                break;
            default:
                throw new IllegalStateException("Invalid " + PtoState.class.getSimpleName() + ": " + ptoState);
        }
    }

    protected void logSubStepInfo(PtoState ptoState, int stepIndex, int subStepIndex, int totalSubStepIndex, long time) {
        assert stepIndex >= 0 : "step index must be non-negative: " + stepIndex;
        assert subStepIndex >= 0 && subStepIndex <= totalSubStepIndex
            : "current step index must be in range [0, " + totalSubStepIndex + "]: " + stepIndex;
        switch (ptoState) {
            case INIT_STEP:
                info("{}{} {} init Step {}.{}/{}.{} ({}ms)",
                    ptoStepLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName(),
                    stepIndex, stepIndex, subStepIndex, stepIndex, totalSubStepIndex, time
                );
                break;
            case PTO_STEP:
                info("{}{} {} Step {}.{}/{}.{} ({}ms)",
                    ptoStepLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName(),
                    stepIndex, stepIndex, subStepIndex, stepIndex, totalSubStepIndex, time
                );
                break;
            default:
                throw new IllegalStateException("Invalid " + PtoState.class.getSimpleName() + ": " + ptoState);
        }
    }

    protected void logSubStepInfo(PtoState ptoState, int stepIndex, int subStepIndex, int totalSubStepIndex, long time,
                                  String description) {
        assert stepIndex >= 0 : "step index must be non-negative: " + stepIndex;
        assert subStepIndex >= 0 && subStepIndex <= totalSubStepIndex
            : "current step index must be in range [0, " + totalSubStepIndex + "]: " + stepIndex;
        switch (ptoState) {
            case INIT_STEP:
                info("{}{} {} init Step {}.{}/{}.{} ({}ms): {}",
                    ptoStepLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName(),
                    stepIndex, stepIndex, subStepIndex, stepIndex, totalSubStepIndex, time, description
                );
                break;
            case PTO_STEP:
                info("{}{} {} Step {}.{}/{}.{} ({}ms): {}",
                    ptoStepLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName(),
                    stepIndex, stepIndex, subStepIndex, stepIndex, totalSubStepIndex, time, description
                );
                break;
            default:
                throw new IllegalStateException("Invalid " + PtoState.class.getSimpleName() + ": " + ptoState);
        }
    }

    /**
     * Log a message at the INFO level if {@code logLevel} is not greater than {@code DISPLAY_LOG_LEVEL}.
     *
     * @param message the message string to be logged.
     */
    protected void info(String message) {
        if (treeLevel < DISPLAY_LOG_LEVEL) {
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
        if (treeLevel < DISPLAY_LOG_LEVEL) {
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
        if (treeLevel < DISPLAY_LOG_LEVEL) {
            LOGGER.info(format, arguments);
        }
    }
}
