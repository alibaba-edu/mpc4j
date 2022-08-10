/*
 * Original Work Copyright 2013 Square Inc.
 * Modified by Weiran Liu. Adjust the code based on Alibaba Java Code Guidelines.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.alibaba.mpc4j.common.jnagmp;

import java.math.BigInteger;

/**
 * 测试向量。
 *
 * @author Weiran Liu
 * @date 2022/4/6
 */
public class TestVector {
    /**
     * 底数
     */
    public final BigInteger base;
    /**
     * p
     */
    public final BigInteger p;
    /**
     * r_p
     */
    public final BigInteger rp;
    /**
     * base^{r_p} mod p
     */
    public final BigInteger resultP;
    /**
     * q
     */
    public final BigInteger q;
    /**
     * r_q
     */
    public final BigInteger rq;
    /**
     * base^{r_q} mod q
     */
    public final BigInteger resultQ;

    /**
     * 将hex编码表示的字符串转换为{@code BigInteger}。
     *
     * @param hex hex编码表示的字符串。
     * @return 转换结果。
     */
    private static BigInteger decode(String hex) {
        return new GmpBigInteger(hex, 16);
    }

    public TestVector(String base, String p, String resultP, String rp, String q, String rq, String resultQ) {
        this(decode(base), decode(p), decode(rp), decode(resultP), decode(q), decode(rq), decode(resultQ));
    }

    public TestVector(BigInteger base,
                      BigInteger p, BigInteger rp, BigInteger resultP,
                      BigInteger q, BigInteger rq, BigInteger resultQ) {
        this.p = p;
        this.rp = rp;
        this.q = q;
        this.rq = rq;
        this.base = base;
        this.resultP = resultP;
        this.resultQ = resultQ;
    }
}
