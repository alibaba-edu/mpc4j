package edu.alibaba.mpc4j.common.rpc;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 验证传入参数是否满足给定的条件，如果不满足则抛出{@code MpcAbortException}异常。
 *
 * @author Weiran Liu
 * @date 2020/08/19
 */
@GwtCompatible
public final class MpcAbortPreconditions {
    /**
     * 单例模式
     */
    private MpcAbortPreconditions() {
        // empty
    }

    /**
     * 保证表达式{@code expression}的执行结果为真。
     *
     * @param expression 一个布尔表达式。
     * @throws MpcAbortException 如果{@code expression}为假。
     */
    public static void checkArgument(boolean expression) throws MpcAbortException {
        try {
            Preconditions.checkArgument(expression);
        } catch (IllegalArgumentException e) {
            throw new MpcAbortException(e.getMessage());
        }
    }

    /**
     * 保证表达式{@code expression}的执行结果为真。
     *
     * @param expression   一个布尔表达式。
     * @param errorMessage 如果{@code expression}为假，则抛出异常中所包含的描述信息。
     * @throws MpcAbortException 如果{@code expression}为假。
     */
    public static void checkArgument(boolean expression, @Nullable Object errorMessage) throws MpcAbortException {
        try {
            Preconditions.checkArgument(expression, errorMessage);
        } catch (IllegalArgumentException e) {
            throw new MpcAbortException(e.getMessage());
        }
    }

    /**
     * 保证表达式{@code expression}的执行结果为真。
     *
     * @param expression           一个布尔表达式。
     * @param errorMessageTemplate 如果{@code expression}为假，则抛出异常中所包含描述信息的模板。
     *                             异常消息要将模板中所有的{@code %s}符号替换为后面给定的描述参数。
     *                             所有描述参数要一一对应 - 第一个{@code %s}对应的是{@code errorMessageArgs[0]}，以此类推。
     *                             未找到对应关系的参数会放在异常信息之后，用中括号括起来。
     *                             未找到对应关系的{@code %s}会保持为%s不变。
     * @param errorMessageArgs     替换异常中描述信息{@code %s}符号的参数列表。
     * @throws MpcAbortException 如果{@code expression}为假。
     */
    public static void checkArgument(boolean expression, @Nullable String errorMessageTemplate,
        Object @Nullable ... errorMessageArgs) throws MpcAbortException {
        try {
            Preconditions.checkArgument(expression, errorMessageTemplate, errorMessageArgs);
        } catch (IllegalArgumentException e) {

            throw new MpcAbortException(e.getMessage());
        }
    }
}
