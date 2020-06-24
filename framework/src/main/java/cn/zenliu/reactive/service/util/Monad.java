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


import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * this is just some simple impl , in some case that won't use Reactor
 *
 * @param <T>
 */
public interface Monad<T> extends Supplier<Optional<T>> {
    <R> Monad<R> map(final Function<T, R> factor);

    Monad<T> onError(final Consumer<Throwable> errorFactor);

    <R> Monad<R> flatMap(final Function<T, Monad<R>> factor);

    static <T> Monad<T> unitEager(@NotNull T value) {
        return new scope.MonadEager<T>(value);
    }

    static <T> Monad<T> unit(@NotNull T value) {
        return new scope.MonadLazy<T>(value);
    }

    static <T, R> Monad<R> bind(@NotNull T value, @NotNull Function<T, R> functor) {
        return new scope.MonadLazy<T>(value).map(functor);
    }

    static <T> Monad<T> unitFrom(@NotNull Supplier<T> value) {
        return new scope.MonadLazy<T>(value, null);
    }

    static <T> Monad<Optional<T>> unitNullable(@Nullable T value) {
        return new scope.MonadLazy<>(Optional.ofNullable(value));
    }

    @UtilityClass
    class scope {
        final class MonadEager<T> implements Monad<T> {
            private final Object value;
            private volatile Consumer<Throwable> erro = null;

            MonadEager(final T value) {
                this.value = value;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R> Monad<R> map(final Function<T, R> factor) {
                try {
                    return new MonadEager<>(factor.apply((T) value));
                } catch (Exception e) {
                    if (erro != null) {
                        erro.accept(e);
                        return (Monad<R>) this;//never happend
                    } else throw (RuntimeException) e;
                }

            }

            @Override
            public synchronized Monad<T> onError(final Consumer<Throwable> errorFactor) {
                this.erro = errorFactor;
                return this;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R> Monad<R> flatMap(final Function<T, Monad<R>> factor) {
                return factor.apply((T) value);
            }

            @SuppressWarnings("unchecked")
            @Override
            public Optional<T> get() {
                return Optional.ofNullable((T) value);
            }
        }

        final class MonadLazy<T> implements Monad<T> {
            //region Properties
            private final List<Function<Object, Object>> mapper = new ArrayList<>();
            private final Map<Integer, List<Function<Object, Object>>> flatMapper = new HashMap<>();
            private final Map<Integer, Consumer<Throwable>> erro = new HashMap<>();
            private final Supplier<Object> supplier;
            private volatile Object value;

            //endregion
            //region Constructor
            MonadLazy(@NotNull final Object value) {
                this.value = Objects.requireNonNull(value);
                this.supplier = null;
            }

            @SuppressWarnings("unchecked")
            MonadLazy(final Supplier<T> value, Object marker) {
                this.value = null;
                this.supplier = (Supplier<Object>) value;
            }

            @SuppressWarnings("unchecked")
            <A> MonadLazy(final Monad<A> old, final Function<A, T> functor, final Object marker) {
                if (old instanceof MonadLazy) {
                    this.value = ((MonadLazy<A>) old).value;
                    this.supplier = ((MonadLazy<A>) old).supplier;
                    this.mapper.addAll(((MonadLazy<A>) old).mapper);
                } else {
                    this.value = old.get();
                    this.supplier = null;
                }
                this.mapper.add((Function<Object, Object>) functor);
            }

            @SuppressWarnings("unchecked")
            <A> MonadLazy(final Monad<A> old, final Function<A, Monad<T>> functor) {
                if (old instanceof MonadLazy) {
                    this.value = ((MonadLazy<A>) old).value;
                    this.supplier = ((MonadLazy<A>) old).supplier;
                    this.mapper.addAll(((MonadLazy<A>) old).mapper);
                } else {
                    this.value = old.get();
                    this.supplier = null;
                }
                //reduce flatMapper
                final List<Function<Object, Object>> functions = this.flatMapper.get(mapper.size() - 1);
                if (functions != null) functions.add((Function<Object, Object>) (Function<?, ?>) functor);
                this.flatMapper.put(mapper.size() - 1, Optional.ofNullable(functions).orElseGet(() -> {
                    final ArrayList<Function<Object, Object>> lst = new ArrayList<>();
                    lst.add((Function<Object, Object>) (Function<?, ?>) functor);
                    return lst;
                }));
            }

            //endregion
            //region Instance bind
            @Override
            public <R> Monad<R> map(final Function<T, R> factor) {
                return new MonadLazy<R>(this, factor, null);
            }

            @Override
            public Monad<T> onError(final Consumer<Throwable> errorFactor) {
                this.erro.put(this.mapper.size() - 1, errorFactor);
                return this;
            }

            @Override
            public <R> Monad<R> flatMap(final Function<T, Monad<R>> factor) {
                return new MonadLazy<>(this, factor);
            }
            //endregion
            //region evaluation

            private synchronized void evalSupplier() {
                if (supplier != null) value = supplier.get();
            }

            private synchronized boolean stackMapping(int stack) {
                final Object[] result = {value};
                final boolean[] exceptionHappen = {false};
                Optional.ofNullable(mapper.get(stack)).map(f -> {
                    try {
                        result[0] = f.apply(result[0]);
                        return null;
                    } catch (Throwable ex) {
                        exceptionHappen[0] = true;
                        if (erro.get(stack) != null) {
                            erro.get(stack).accept(ex);
                        }
                        return null;
                    }
                });
                if (exceptionHappen[0]) return false;
                value = result[0];
                return true;
            }

            @SuppressWarnings("unchecked")
            private synchronized boolean flatMapping(int stack) {
                final Object[] result = {value};
                final boolean[] exceptionHappen = {false};
                Optional.ofNullable(flatMapper.get(stack))
                    .map(fs -> {
                        for (Function<Object, Object> func : fs) {
                            try {
                                final cn.zenliu.reactive.service.util.Monad<Object> apply = (cn.zenliu.reactive.service.util.Monad<Object>) func.apply(result[0]);
                                result[0] = apply.get().orElse(null);
                            } catch (Throwable ex) {
                                exceptionHappen[0] = true;
                                if (erro.get(stack) != null) {
                                    erro.get(stack).accept(ex);
                                }
                            }
                        }
                        return null;
                    });
                if (exceptionHappen[0]) return false;
                value = result[0];
                return true;
            }

            /**
             * value is only get once
             * Supplier can get many times
             *
             * @return result
             */
            @SuppressWarnings("unchecked")
            @Override
            public Optional<T> get() {
                if (mapper.isEmpty() && flatMapper.isEmpty() && supplier == null) {
                    return Optional.empty();
                } else if (mapper.isEmpty()) {
                    evalSupplier();
                    for (final Integer integer : flatMapper.keySet()) {
                        if (!flatMapping(integer)) return Optional.empty();
                    }
                } else {
                    for (int i = 0; i < mapper.size(); i++) {
                        if (i == 0) evalSupplier();
                        if (!stackMapping(i)) return Optional.empty();
                        flatMapping(i);
                    }
                }
                return Optional.ofNullable((T) value);
            }
            //endregion
        }

    }
}
