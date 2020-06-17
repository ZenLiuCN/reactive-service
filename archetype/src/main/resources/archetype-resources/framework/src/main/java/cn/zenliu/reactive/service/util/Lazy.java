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
 *   @File: Lazy.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-13 14:57:19
 */

package cn.zenliu.reactive.service.util;

import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * impl of Lazy pattern
 *
 * @param <T>
 * @author Zen.Liu
 */
public interface Lazy<T> extends Supplier<Optional<T>> {
    /**
     * @param supplier null when use {@link Lazy#laterComputed(Supplier)}
     *                 must nonnull when use {@link Lazy#laterSupplied()}
     * @return some Lazy<T>
     */
    T getOrCompute(Supplier<T> supplier);

    static <T> Lazy<T> laterSupplied() {
        return new LazySupplied<>();
    }

    static <T> Lazy<T> laterComputed(Supplier<T> supplier) {
        requireNonNull(supplier);
        return new LazyComputed<>(supplier);
    }

    /**
     * lazy supplied
     *
     * @param <T>
     */
    final class LazySupplied<T> implements Lazy<T> {
        private volatile T value;

        public T getOrCompute(Supplier<T> supplier) {
            final T result = value; // Just one volatile read
            return result == null ? maybeCompute(supplier) : result;
        }

        private synchronized T maybeCompute(Supplier<T> supplier) {
            if (value == null) {
                value = requireNonNull(supplier.get());
            }
            return value;
        }

        @Override
        public Optional<T> get() {
            return Optional.empty();
        }
    }

    /**
     * lazy computed
     *
     * @param <T>
     */
    final class LazyComputed<T> implements Lazy<T> {
        private volatile T value;
        private final Supplier<T> supplier;

        LazyComputed(final Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public T getOrCompute(Supplier<T> supplier) {
            final T result = value;
            return result == null ? maybeCompute() : result;
        }

        @Override
        public Optional<T> get() {
            return Optional.ofNullable(getOrCompute(null));
        }

        private synchronized T maybeCompute() {
            if (value == null) {
                value = requireNonNull(supplier.get());
            }
            return value;
        }
    }
}
