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


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * this is just some simple impl , in some case that won't use Reactor
 * @param <T>
 */
public final class Monad<T> implements Supplier<Optional<T>> {

    public static <T> Monad<T> unit(@NotNull T value) {
        return new Monad<T>(value);
    }

    public static <T> Monad<T> bind(@NotNull T value, @NotNull Consumer<T> functor) {
        return new Monad<T>(value);
    }

    public static <T> Monad<T> unitFrom(@NotNull Supplier<T> value) {
        return new Monad<T>(value, null);
    }

    public static <T> Monad<Optional<T>> unitNullable(@Nullable T value) {
        return new Monad<>(Optional.ofNullable(value));
    }

    //region Properties
    private final List<Function<Object, Object>> mapper = new ArrayList<>();
    private final Map<Integer, List<Function<Object, Object>>> flatMapper = new HashMap<>();
    private final Map<Integer, Consumer<Throwable>> erro = new HashMap<>();
    private final Supplier<Object> supplier;
    private volatile Object value;
    //endregion


    //region Constructor
    Monad(@NotNull final Object value) {
        requireNonNull(value);
        this.value = value;
        this.supplier = null;
    }

    @SuppressWarnings("unchecked")
    Monad(final Supplier<T> value, Object marker) {
        this.value = null;
        this.supplier = (Supplier<Object>) value;
    }

    @SuppressWarnings("unchecked")
    <A> Monad(final Monad<A> old, final Function<A, T> functor, final Object marker) {
        this.value = old.value;
        this.supplier = old.supplier;
        this.mapper.addAll(old.mapper);
        this.mapper.add((Function<Object, Object>) functor);
    }

    @SuppressWarnings("unchecked")
    <A> Monad(final Monad<A> old, final Function<A, Monad<T>> functor) {
        this.value = old.value;
        this.supplier = old.supplier;
        this.mapper.addAll(old.mapper);
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
    public <R> Monad<R> map(final Function<T, R> factor) {
        return new Monad<R>(this, factor, null);
    }

    public  Monad<T> onError(final Consumer<Throwable> errorFactor) {
        this.erro.put(this.mapper.size() - 1, errorFactor);
        return this;
    }

    public <R> Monad<R> flatMap(final Function<T, Monad<R>> factor) {
        return new Monad<>(this, factor);
    }
    //endregion


    //region evaluation

    private synchronized void evalSupplier() {
        if (supplier!=null) value = ((Supplier<Object>) supplier).get();
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
                        final Monad<Object> apply = (Monad<Object>) func.apply(result[0]);
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
     * @return result
     */
    @SuppressWarnings("unchecked")
    @Override
    public Optional<T> get() {
        if (mapper.isEmpty() && flatMapper.isEmpty() && supplier==null) {
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


