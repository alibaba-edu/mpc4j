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

import java.io.Serial;

/**
 * Thrown when a decoding operation fails in PHE.
 * <p>
 * The implementation comes from
 * <a href="https://github.com/n1analytics/javallier/blob/master/src/main/java/com/n1analytics/paillier/DecodeException.java">
 * DecodeException.java</a>.
 *
 * @author Mentari Djatmiko, Weiran Liu
 * @date 2016/01/08
 */
public class PheDecodeException extends PheRuntimeException {
    @Serial
    private static final long serialVersionUID = 2613143080120141130L;

    /**
     * Constructs a new {@code DecodeException} without a specific message.
     */
    public PheDecodeException() {
        super();
    }

    /**
     * Constructs a new {@code DecodeException} with a specific message.
     *
     * @param message the detail message.
     */
    public PheDecodeException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code DecodeException} with the exception cause.
     *
     * @param cause the cause.
     */
    public PheDecodeException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code DecodeException} with a specific message and the exception cause.
     *
     * @param message the detail message,
     * @param cause   the cause.
     */
    public PheDecodeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code DecodeException} with a specific message and the exception cause.
     *
     * @param message            the detail message.
     * @param cause              the cause.
     * @param enableSuppression  whether suppression is enabled or disabled.
     * @param writableStackTrace whether the stack trace should be writable.
     */
    public PheDecodeException(String message, Throwable cause, boolean enableSuppression,
                              boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
