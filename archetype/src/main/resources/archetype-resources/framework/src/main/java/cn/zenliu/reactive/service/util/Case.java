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
 *   @Module: reactive-service-archetype
 *   @File: Case.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-14 12:21:03
 */

package cn.zenliu.reactive.service.util;


import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 *  <p>
 *      impl of Pattern Match in OO
 *      here is two kinds of case
 *      1. VoidCase: create by {@link Case#doMatch(Object)} or
 *      {@link Case#doMatch(Supplier)}  which only execute action when call {@link Case#get()}
 *      2. ValueCase: create by {@link Case#doMatch(Object, Class)} or {@link Case#doMatch(Supplier, Class)}
 *      which execute mapping function and return value   when call {@link Case#get()}
 *  </p>
 *  <code>
 *    Case.doMatch(1)
 *     .caseOf(2, System.out::println)//if not match any , this will be execute
 *     .caseOf(i -> i == 2, System.out::println) //if not match any , this will be execute
 *     .caseElse(System.out::println) // if not match any , this will be execute
 *     .get();// this finally execute matched branch,match happens on ever case* method
 *  </code>
 * @author Zen.Liu
 * @param <T> result type
 */
public interface Case<T> extends Supplier<Optional<T>> {
    /**
     * action case
     * @param value static value
     * @param <T> source value type
     * @return case
     */
    static <T> VoidCase<T> doMatch(T value) {
        return new VoidCase<>(value);
    }
    /**
     * eager eval supplier
     * @param supplier value
     * @param <T> source value type
     * @return case
     */
    static <T> VoidCase<T> doMatch(Supplier<T> supplier) {
        return new VoidCase<>(supplier.get());
    }
    /**
     *
     * @param value static value
     * @param wanna output value (just place holder for type ref)
     * @param <T> source value type
     * @param <R> result value type
     * @return case
     */
    static <T,R> ValueCase<T, R> doMatch(T value, Class<R> wanna) {
        return new ValueCase<>(value);
    }
    /**
     * eager eval supplier
     * @param supplier value
     * @param wanna result type  (just place holder for type ref)
     * @param <T> source value type
     * @param <R> result value type
     * @return case
     */
    static <T,R> ValueCase<T, R> doMatch(Supplier<T> supplier, Class<R> wanna) {
        return new ValueCase<>(supplier.get());
    }
    final class VoidCase<T> implements Case<Void> {
        private final T value;
        private Consumer<T> stack = null;

        VoidCase(@NotNull final T value) {
            this.value = value;
        }

        public VoidCase<T> caseOf(Class<?> clz, Consumer<T> consumer) {
            if (stack == null && clz.isInstance(value)) {
                stack = consumer;
            }
            return this;
        }

        public VoidCase<T> caseOf(T value, Consumer<T> consumer) {
            if (stack == null && Objects.equals(this.value, value)) {
                stack = consumer;
            }
            return this;
        }

        public VoidCase<T> caseOf(Predicate<T> condition, Consumer<T> consumer) {
            if (stack == null && condition.test(this.value)) {
                stack = consumer;
            }
            return this;
        }

        public VoidCase<T> caseElse(Consumer<T> consumer) {
            if (stack == null) {
                stack = consumer;
            }
            return this;
        }

        @Override
        public Optional<Void> get() {
            if (stack != null) {
                stack.accept(value);
            }
            return Optional.empty();
        }
    }
    final class ValueCase<R, T> implements Case<T> {
        private final R value;
        private Function<R, T> stack = null;

        ValueCase(final R value) {
            this.value = value;
        }

        public ValueCase<R, T> caseOf(Class<?> clz, Function<R, T> mapping) {
            if (stack == null && clz.isInstance(value)) {
                stack = mapping;
            }
            return this;
        }

        public ValueCase<R, T> caseOf(R value, Function<R, T> mapping) {
            if (stack == null && Objects.equals(this.value, value)){
                stack = mapping;
            }
            return this;
        }

        public ValueCase<R, T> caseOf(Predicate<R> condition, Function<R, T> mapping) {
            if (stack == null && condition.test(this.value)) {
                stack = mapping;
            }
            return this;
        }

        public ValueCase<R, T> caseElse(Function<R, T> mapping) {
            if (stack == null) {
                stack = mapping;
            }
            return this;
        }

        @Override
        public Optional<T> get() {
            if (stack != null) {
                try {
                    return Optional.ofNullable(stack.apply(value));
                } catch (Exception e) {
                    e.printStackTrace();
                    return Optional.empty();
                }
            }
            return Optional.empty();
        }
    }
}

