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
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class TailRecTest {
    private static TailRec<BigInteger> factorial(final BigInteger factorial, final int number) {
        if (number == 1) {
            return TailRec.done(factorial);
        } else return () -> factorial(factorial.multiply(BigInteger.valueOf(number)), number - 1);
    }

    static TailRec<BigInteger> factorial(final int number) {
        return factorial(BigInteger.ONE, number);
    }
    static BigInteger factorialRec(final BigInteger factorial, final int number) {
        if (number == 1) {
            return factorial;
        } else return factorialRec(factorial.multiply(BigInteger.valueOf(number)) , number - 1);
    }

    /**
     * create more thread is create stack frame
     * {@code
     *   final FutureTask<BigInteger> task =
     *                 new FutureTask<>(() -> factorialFuture(factorial.multiply(BigInteger.valueOf(number)),number-1));
     *   new Thread(task);
     *   task.run();
     *   return task.get();
     * }
     *
     */
    static BigInteger factorialFuture(final BigInteger factorial, final int number) throws ExecutionException, InterruptedException {
        if (number == 1) {
            return factorial;
        } else {
            final FutureTask<BigInteger> task =
                new FutureTask<>(() -> factorialFuture(factorial.multiply(BigInteger.valueOf(number)),number-1));
            new Thread(task);
            task.run();
            return task.get();
        }
    }

    /**
     * 1000 =>1.1263
     * 5000 =>1.0922
     * 8000 =>1.0924
     * 10000 =>1.0954
     * 12000 =>1.0954
     * 20000 =>overflow
     */
    @Test
    void benchmark() throws ExecutionException, InterruptedException {
        System.out.println("begin at "+ Instant.now());
        final int times=12000;
        double x=0;
        for (int i = 0; i < 1000; i++) {
            x+=timeCheck(times);
        }
        System.out.println(x/1000.0);
    }
    //limit 500m =>46.727
    //limit 1G =>45.442
    //limit 2G =>45.393
    @Test
    void stackView(){
        final int times=15000;
        final long ts1=System.nanoTime();
        for (int i = 0; i <1000 ; i++) {
            factorial(times).get().orElse(BigInteger.ZERO);
        }
        final long ts2=System.nanoTime();

        System.out.println("escape:"+(ts2-ts1)/1000000/1000.0);
    }
    void benchTime(Runnable task){
        final long ts1=System.nanoTime();
        task.run();
        final long ts2=System.nanoTime();
        System.out.println("time escape:"+(ts2-ts1)/1000000+"ms");
    }
    long benchTimeReturn(Runnable task){
        final long ts1=System.nanoTime();
        task.run();
        final long ts2=System.nanoTime();
       return (ts2-ts1);
    }
    double timeCheck(final int times) throws ExecutionException, InterruptedException {
        final long ts=System.nanoTime();
        final BigInteger result = factorial(times).get().orElse(BigInteger.ZERO);
        final long ts1=System.nanoTime();
        final long ts2=System.nanoTime();
        final BigInteger result1 = factorialRec(BigInteger.ONE,times);
        final long ts3=System.nanoTime();
        assertEquals(result,result1);
        final double rate = (ts1 - ts + 0.0) / (ts3 - ts2 + 0.0);
        System.out.println("x2:"+(ts3-ts2)/1000000+"x1:"+(ts1-ts)/1000000+" rate:"+ rate);
       return rate;
    }
    @Test
    void futureTest() {
        final BigInteger[] bigInteger = new BigInteger[1];
        benchTime(()->{
            try {
                bigInteger[0] =  factorialFuture(BigInteger.ONE, 12000);
            } catch (Exception e) {
                e.printStackTrace();
                bigInteger[0] =null;
            }
        });
        System.out.println(bigInteger[0]);
    }
}