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

import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;


/**
 * <p>
 *  notes:
 *   1. this may cause a little more heap memory used as normal (none tail recursion ) call
 *   2. this speed is almost same as normal call
 * </p>
 * {@code
 *     private static TailRec<BigInteger> factorial(final BigInteger factorial, final int number) {
 *         if (number == 1) {
 *             return TailRec.done(factorial);
 *         } else return () -> factorial(factorial.multiply(BigInteger.valueOf(number)), number - 1);
 *     }
 *
 *     static TailRec<BigInteger> factorial(final int number) {
 *         return factorial(BigInteger.ONE, number);
 *     }
 * }
 * @param <T>
 */
@FunctionalInterface
public interface TailRec<T> extends Supplier<Optional<T>> {
    TailRec<T> apply();

    default boolean isComplete() {
        return false;
    }

    /**
     * <p>
     *     stack overview
     *     1. impl with stream
     *     at java.lang.Thread.dumpStack(Thread.java:1336)
     * 	   at cn.zenliu.reactive.service.util.TailRecTest.factorial(TailRecTest.java:15)
     * 	   at cn.zenliu.reactive.service.util.TailRecTest.lambda$factorial$0(TailRecTest.java:17)
     * 	   at java.util.stream.Stream$1.next(Stream.java:1033)
     * 	   at java.util.Spliterators$IteratorSpliterator.tryAdvance(Spliterators.java:1812)
     * 	   at java.util.stream.ReferencePipeline.forEachWithCancel(ReferencePipeline.java:126)
     * 	   at java.util.stream.AbstractPipeline.copyIntoWithCancel(AbstractPipeline.java:498)
     * 	   at java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:485)
     * 	   at java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:471)
     * 	   at java.util.stream.FindOps$FindOp.evaluateSequential(FindOps.java:152)
     * 	   at java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
     * 	   at java.util.stream.ReferencePipeline.findFirst(ReferencePipeline.java:464)
     * 	   at cn.zenliu.reactive.service.util.TailRec.get(TailRec.java:38)
     * 	   at cn.zenliu.reactive.service.util.TailRecTest.stackView(TailRecTest.java:56)
     *
     *     2. impl with while
     *     at cn.zenliu.reactive.service.util.TailRecTest.factorial(TailRecTest.java:15)
     * 	   at cn.zenliu.reactive.service.util.TailRecTest.lambda$factorial$0(TailRecTest.java:17)
     * 	   at cn.zenliu.reactive.service.util.TailRec.get(TailRec.java:33)
     * 	   at cn.zenliu.reactive.service.util.TailRecTest.stackView(TailRecTest.java:56)
     * </p>
     * @return final value
     */
   // @SuppressWarnings("OptionalGetWithoutIsPresent")
    default Optional<T> get() {
        TailRec<T> value=this.apply();
        while (!value.isComplete()){
            value=value.apply();
        }
        return value.get();
 /*      return Stream.iterate(this, TailRec::apply)
            .filter(TailRec::isComplete)
            .findFirst()
            .get()
            .get();*/
    }

    static <T> TailRec<T> done(final T value) {
        return new DoneCall<>(value);
    }

    final class DoneCall<T> implements TailRec<T> {
        private final T value;

        DoneCall(@Nullable final T value) {
            this.value = value;
        }

        @Override
        public TailRec<T> apply() {
            throw new Error("not implemented");
        }

        @Override
        public boolean isComplete() {
            return true;
        }

        @Override
        public Optional<T> get() {
            return Optional.ofNullable(value);
        }
    }
}
