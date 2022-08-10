/*
 * Copyright 2015 NICTA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package edu.alibaba.mpc4j.crypto.phe;

/**
 * {@code CryptoRuntimeException}是crypto-core中{@code RuntimeException}的超类（super class）。源码来自：
 * <p>
 * https://github.com/n1analytics/javallier/blob/master/src/main/java/com/n1analytics/paillier/PaillierRuntimeException.java
 * </p>
 *
 * @author Mentari Djatmiko, Weiran Liu
 * @date 2016/01/08
 */
public class CryptoRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 6030736579421587829L;

    /**
     * Constructs a new {@code CryptoRuntimeException} without a specific message.
     */
    public CryptoRuntimeException() {
        super();
    }

    /**
     * Constructs a new {@code CryptoRuntimeException} with a specific message.
     *
     * @param message the detail message.
     */
    public CryptoRuntimeException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code CryptoRuntimeException} with the exception cause.
     *
     * @param cause the cause.
     */
    public CryptoRuntimeException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code CryptoRuntimeException} with a specific message and the exception cause.
     *
     * @param message the detail message,
     * @param cause   the cause.
     */
    public CryptoRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code CryptoRuntimeException} with a specific message and the exception cause.
     *
     * @param message            the detail message.
     * @param cause              the cause.
     * @param enableSuppression  whether suppression is enabled or disabled.
     * @param writableStackTrace whether the stack trace should be writable.
     */
    protected CryptoRuntimeException(String message, Throwable cause,
                                     boolean enableSuppression,
                                     boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
