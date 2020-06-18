/*
 *  Copyright (c) 2020.  Zen.Liu .
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *   @Project: reactive-service-framework
 *   @Module: reactive-service-framework
 *   @File: Chain.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-13 15:06:41
 */

package cn.zenliu.reactive.service.util;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TailRecTest {
    //fibonacci 0,1,1,2,3,5...
    private static TailRec<BigInteger> fibonacci(
        final int number,
        final BigInteger current,
        final BigInteger next
    ) {
        return number == 0 ? TailRec.done(current) : () -> fibonacci(number - 1, next, current.add(next));
    }

    static TailRec<BigInteger> Fibonacci(final int number) {
        return fibonacci(number, BigInteger.ZERO, BigInteger.ONE);
    }

    static BigInteger fibonacciRec(
        final int number,
        final BigInteger current,
        final BigInteger next
    ) {
        return number == 0 ? current : fibonacciRec(number - 1, next, current.add(next));
    }

    static BigInteger fibonacciFor(final int number) {
        BigInteger before = BigInteger.ZERO, behind = BigInteger.ZERO;
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < number; i++) {
            if (i == 0) {
                result = BigInteger.ONE;
            } else {
                result = before.add(behind);
                before = behind;

            }
            behind = result;
        }
        return result;

    }

    /**
     * create more thread is create stack frame
     * {@code
     * final FutureTask<BigInteger> task =
     * new FutureTask<>(() -> factorialFuture(factorial.multiply(BigInteger.valueOf(number)),number-1));
     * new Thread(task);
     * task.run();
     * return task.get();
     * }
     */
    static BigInteger factorialFuture(final BigInteger factorial, final int number) throws ExecutionException, InterruptedException {
        if (number == 1) {
            return factorial;
        } else {
            final FutureTask<BigInteger> task =
                new FutureTask<>(() -> factorialFuture(factorial.multiply(BigInteger.valueOf(number)), number - 1));
            new Thread(task);
            task.run();
            return task.get();
        }
    }

    /**
     * --vs for---
     * 150 --> 1.67
     * 1500 --> 1.18
     * 15000 --> 1.014
     * 150000 --> 1.014
     */
    @Test
    void benchmark() {
        System.out.println("TailRec vs For on fibonacci");
        System.out.println("begin at " + Instant.now());
        int max = 150000;
        while (max>10){
            double x = 0;
            for (int i = 0; i < 1000; i++) {
                x += timeCheck(max);
            }
            System.out.println("max="+max+" -->"+(x / 1000.0));
            System.out.println("end at " + Instant.now());
            max=max/10;
        }
        System.out.println("all end at " + Instant.now());
    }

    //limit 500m =>46.727
    //limit 1G =>45.442
    //limit 2G =>45.393
    @Test
    void stackView() {
        final int times = 15000;
        final long ts1 = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            Fibonacci(times).get().orElse(BigInteger.ZERO);
        }
        final long ts2 = System.nanoTime();

        System.out.println("escape:" + (ts2 - ts1) / 1000000 / 1000.0);
    }

    void benchTime(Runnable task) {
        final long ts1 = System.nanoTime();
        task.run();
        final long ts2 = System.nanoTime();
        System.out.println("time escape:" + (ts2 - ts1) / 1000000 + "ms");
    }

    long benchTimeReturn(Runnable task) {
        final long ts1 = System.nanoTime();
        task.run();
        final long ts2 = System.nanoTime();
        return (ts2 - ts1);
    }

    double timeCheck(final int times) {
        final long ts1 = System.nanoTime();
        final BigInteger result = Fibonacci(times).get().orElse(BigInteger.ZERO);
        final long ts2 = System.nanoTime();
//        final BigInteger result1 = factorialRec(BigInteger.ONE,times);
        final BigInteger result1 = fibonacciFor(times);
        final long ts3 = System.nanoTime();
        assertEquals(result, result1);
        final double rate = (ts2 - ts1 + 0.0) / (ts3 - ts2 + 0.0);
       // System.out.println("x2:" + (ts3 - ts2) / 1000000 + "x1:" + (ts1 - ts) / 1000000 + " rate:" + rate);
        return rate;
    }

    @Test
    void futureTest() {
        final BigInteger[] bigInteger = new BigInteger[1];
        benchTime(() -> {
            try {
                bigInteger[0] = factorialFuture(BigInteger.ONE, 12000);
            } catch (Exception e) {
                e.printStackTrace();
                bigInteger[0] = null;
            }
        });
        System.out.println(bigInteger[0]);
    }
}