package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import org.apache.commons.lang3.builder.HashCodeBuilder;
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
     * default display log level
     */
    private static final int DEFAULT_DISPLAY_LOG_LEVEL = 2;
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
     * pto path
     */
    private int[] ptoPath;
    /**
     * task ID
     */
    private int taskId;
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
    protected String ptoStepLogPrefix;
    /**
     * the log prefix for ending a task
     */
    private String ptoEndLogPrefix;
    /**
     * display log level
     */
    private int displayLogLevel;
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
        stopWatch = new StopWatch();
        subPtos = new ArrayList<>();
        ptoPath = new int[]{0};
        taskId = 0;
        updateEncodeId();
        ptoBeginLogPrefix = "↘";
        ptoStepLogPrefix = "    ↓";
        ptoEndLogPrefix = "↙";
        extraInfo = 0;
        partyState = PartyState.NON_INITIALIZED;
        envType = config.getEnvType();
        secureRandom = new SecureRandom();
        parallel = false;
        displayLogLevel = DEFAULT_DISPLAY_LOG_LEVEL;
    }

    private void updateEncodeId() {
        int hashCode = Math.abs(new HashCodeBuilder().append(ptoPath).hashCode());
        encodeTaskId = ((long) hashCode << Integer.SIZE) + taskId;
    }

    protected void addSubPto(MultiPartyPto subPto) {
        int subPtoIndex = subPtos.size();
        subPtos.add(subPto);
        int ptoPathLength = ptoPath.length;
        int[] subPtoPath = new int[ptoPathLength + 1];
        System.arraycopy(ptoPath, 0, subPtoPath, 0, ptoPathLength);
        subPtoPath[ptoPathLength] = subPtoIndex;
        subPto.updatePtoPath(subPtoPath);
    }

    @Override
    public void updatePtoPath(int[] ptoPath) {
        // we cannot find a way to verify that this is called internally.
        this.ptoPath = ptoPath;
        updateEncodeId();
        ptoBeginLogPrefix = "    " + ptoBeginLogPrefix;
        ptoStepLogPrefix = "    " + ptoStepLogPrefix;
        ptoEndLogPrefix = "    " + ptoEndLogPrefix;
        // set sub-protocols
        int ptoPathLength = ptoPath.length;
        for (int subPtoIndex = 0; subPtoIndex < subPtos.size(); subPtoIndex++) {
            int[] subPtoPath = new int[ptoPathLength + 1];
            System.arraycopy(ptoPath, 0, subPtoPath, 0, ptoPathLength);
            subPtoPath[ptoPathLength] = subPtoIndex;
            subPtos.get(subPtoIndex).updatePtoPath(subPtoPath);
        }
    }

    @Override
    public void setTaskId(int taskId) {
        // taskId >= 0
        MathPreconditions.checkNonNegative("taskId", taskId);
        // only the root protocol can set task ID.
        MathPreconditions.checkEqual("ptoPath.length", "1", ptoPath.length, 1);
        this.taskId = taskId;
        updateEncodeId();
        // set sub-protocols
        for (MultiPartyPto subPto : subPtos) {
            subPto.setEncodeTaskId(taskId);
        }
    }

    @Override
    public void setEncodeTaskId(int taskId) {
        // this can be only called internally.
        MathPreconditions.checkGreater("ptoPath.length", ptoPath.length, 1);
        this.taskId = taskId;
        updateEncodeId();
        // set sub-protocols
        for (MultiPartyPto subPto : subPtos) {
            subPto.setEncodeTaskId(taskId);
        }
    }

    @Override
    public int getTaskId() {
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

    @Override
    public void setDisplayLogLevel(int displayLogLevel) {
        // display_log_level >= 0
        MathPreconditions.checkNonNegative("display_log_level", displayLogLevel);
        // only the root protocol can set task ID.
        MathPreconditions.checkEqual("ptoPath.length", "1", ptoPath.length, 1);
        this.displayLogLevel = displayLogLevel;
        for (MultiPartyPto subPto : subPtos) {
            subPto.setDisplayLogLevel(displayLogLevel);
        }
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
                throw new IllegalStateException("Party state must not be " + partyState);
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
                throw new IllegalStateException("Party state must not be " + partyState);
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
                    stepIndex, subStepIndex, stepIndex, totalSubStepIndex, time
                );
                break;
            case PTO_STEP:
                info("{}{} {} Step {}.{}/{}.{} ({}ms)",
                    ptoStepLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName(),
                    stepIndex, subStepIndex, stepIndex, totalSubStepIndex, time
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
                    stepIndex, subStepIndex, stepIndex, totalSubStepIndex, time, description
                );
                break;
            case PTO_STEP:
                info("{}{} {} Step {}.{}/{}.{} ({}ms): {}",
                    ptoStepLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName(),
                    stepIndex, subStepIndex, stepIndex, totalSubStepIndex, time, description
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
        if (ptoPath.length <= displayLogLevel) {
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
        if (ptoPath.length <= displayLogLevel) {
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
        if (ptoPath.length <= displayLogLevel) {
            LOGGER.info(format, arguments);
        }
    }
}
