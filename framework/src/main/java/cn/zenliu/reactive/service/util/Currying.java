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
 *   @File: Currying.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-21 12:33:42
 */

package cn.zenliu.reactive.service.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * not done
 * todo testing
 * todo performance improve
 *
 * @param <R>
 */
public interface Currying<R> extends Supplier<Optional<R>> {
    /**
     * apply one parameter by seq
     *
     * @param parameter value as parameter
     * @return self
     */
    Currying<R> apply(@NotNull Object parameter);

    /**
     * @return how many parameter left to apply
     */
    int remainParameterCount();

    /**
     * @return total parameter needed
     */
    int totalParameterCount();

    /**
     * will create new Currying with all captured parameters
     *
     * @param function composite to
     * @param <R1>     new return type
     * @return new Currying
     */
    @NotNull <R1> Currying<R1> composite(@NotNull final Function<R, R1> function);

    /**
     * will create new Currying with all captured parameters
     *
     * @param function composite to
     * @param <P>      new parameter type
     * @param <R1>     new return type
     * @return new Currying
     */
    @NotNull <P, R1> Currying<R1> composite(@NotNull final BiFunction<R, P, R1> function);

    /**
     * will create new Currying with all captured parameters (must only used with it is typesafe Currying)
     *
     * @param function composite to
     * @param <P>      new parameter type
     * @param <R1>     new return type
     * @return new Currying
     */
    @NotNull <P, R1> Currying<R1> composite(@NotNull final BiFunction<R, P, R1> function, Class<P> pClass);

    @NotNull Optional<Class<?>> nextParameter();

    /**
     * immutable list of needed parameter types
     *
     * @return immutable list
     */
    @NotNull List<Class<?>> remainParameters();

    /**
     * create currying with soft typed parameter (not real check type for parameters)
     */
    static <R> Currying<R> of(@NotNull final Function<?, R> function) {
        return new scope.CurryingImpl<R>(function);
    }

    /**
     * create currying with strong typed parameter
     */
    static <P, R> Currying<R> of(@NotNull final Function<P, R> function, Class<P> pClass) {
        return new scope.CurryingImpl<R>(function, pClass);
    }

    /**
     * create currying with strong typed parameter
     */
    static <P1, P2, R> Currying<R> of(@NotNull final BiFunction<P1, P2, R> function, Class<P1> p1Class,
                                      Class<P2> p2Class) {
        return new scope.CurryingImpl<R>(function, p1Class, p2Class);
    }
    /**
     * create currying with soft typed parameter (not real check type for parameters)
     */
    static <R> Currying<R> of(@NotNull final BiFunction<?, ?, R> function) {
        return new scope.CurryingImpl<R>(function);
    }

    @UtilityClass
    class scope {
        private interface CurryingWarp<P, R> extends Function<P, R> {
            void applyParam(Object value);

            int more();

            int total();
        }

        private class CurryingWarp1<R1, R> implements CurryingWarp<R1, R> {
            private final Function<R1, R> callable;

            CurryingWarp1(final Function<R1, R> callable) {
                this.callable = callable;
            }

            @Override
            public int total() {
                return 0;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void applyParam(final Object value) {
                throw new IllegalStateException("currying of warp one not applicable");
            }

            @Override
            public int more() {
                return 0;
            }

            @Override
            public R apply(R1 value) {
                return callable.apply(value);
            }
        }


        @SuppressWarnings("unchecked")
        private class CurryingWarp1a<P, R> implements CurryingWarp<P, R> {
            private volatile P param = null;
            private final Function<P, R> callable;

            CurryingWarp1a(final Function<P, R> callable) {
                this.callable = callable;
            }

            @Override
            public int more() {
                return param == null ? 1 : 0;
            }

            @Override
            public int total() {
                return 1;
            }

            @Override
            public void applyParam(final Object value) {
                if (param == null)
                    this.param = (P) value;
            }

            @Override
            public R apply(P value) {
                if (param == null) {
                    return null;
                }
                return callable.apply(param);
            }
        }

        private class CurryingWarp2<R1, P, R> implements CurryingWarp<R1, R> {
            private volatile P param = null;
            private final BiFunction<R1, P, R> callable;

            CurryingWarp2(final BiFunction<R1, P, R> callable) {
                this.callable = callable;
            }

            @Override
            public int more() {
                return param == null ? 1 : 0;
            }

            @Override
            public int total() {
                return 1;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void applyParam(final Object value) {
                if (param == null)
                    this.param = (P) value;

            }

            @Override
            public R apply(R1 value) {
                if (param == null) {
                    return null;
                }
                return callable.apply(value, param);
            }
        }

        private class CurryingWarp2a<P1, P2, R> implements CurryingWarp<P2, R> {
            private volatile P1 param = null;
            private volatile P2 param2 = null;
            private final BiFunction<P1, P2, R> callable;

            CurryingWarp2a(final BiFunction<P1, P2, R> callable) {
                this.callable = callable;
            }

            @Override
            public int more() {
                return param == null && param2 == null ? 2 : (
                    param2 == null ? 1 : 0
                );
            }

            @Override
            public int total() {
                return 2;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void applyParam(final Object value) {
                if (param == null)
                    this.param = (P1) value;
                else if (param2 == null)
                    this.param2 = (P2) value;

            }

            @Override
            public R apply(P2 value) {
                if (param == null || param2 == null) {
                    return null;
                }
                return callable.apply(param, param2);
            }
        }

        private class CurryingImpl<R> implements Currying<R> {
            private final List<CurryingWarp<Object, ?>> holder;
            private final List<Class<?>> parameters;
            private int pointer = 0;
            private final boolean typeSafe;

            CurryingImpl(final List<CurryingWarp<Object, ?>> holder, final List<Class<?>> parameters, final int pointer, final boolean typeSafe) {
                this.holder = holder;
                this.parameters = parameters;
                this.pointer = pointer;
                this.typeSafe = typeSafe;
            }

            @SuppressWarnings("unchecked")
            CurryingImpl(final Function<?, R> function) {
                this.pointer = 0;
                this.holder = new ArrayList<>();
                this.parameters = new ArrayList<>();
                this.parameters.add(Object.class);
                this.typeSafe = false;
                this.holder.add(new CurryingWarp1a<Object, R>((Function<Object, R>) function));
            }

            @SuppressWarnings("unchecked")
            <P> CurryingImpl(final Function<P, R> function, Class<P> pClass) {
                this.pointer = 0;
                this.holder = new ArrayList<>();
                this.parameters = new ArrayList<>();
                this.parameters.add(pClass);
                this.typeSafe = true;
                this.holder.add(new CurryingWarp1a<Object, R>((Function<Object, R>) function));
            }

            @SuppressWarnings("unchecked")
                // this will not type safe as lambda have erase type info
            CurryingImpl(final BiFunction<?, ?, R> function) {
                this.pointer = 0;
                this.holder = new ArrayList<>();
                this.parameters = new ArrayList<>();
            /*    Arrays.stream(function.getClass().getMethods())
                    .filter(m -> m.getName().equals("apply")).findFirst()
                    .map(Method::getParameterTypes)
                    .filter(e -> e.length == 2)
                    .ifPresent(m -> this.parameters.addAll(Arrays.asList(m)));*/
                this.parameters.add(Object.class);
                this.parameters.add(Object.class);
                this.typeSafe = false;
                this.holder.add(new CurryingWarp2a<Object, Object, R>((BiFunction<Object, Object, R>) function));
            }

            @SuppressWarnings("unchecked")
            <P1, P2> CurryingImpl(final @NotNull BiFunction<P1, P2, R> function, final Class<P1> p1Class, final Class<P2> p2Class) {
                this.pointer = 0;
                this.holder = new ArrayList<>();
                this.parameters = new ArrayList<>();
                this.parameters.add(p1Class);
                this.parameters.add(p2Class);
                this.typeSafe = true;
                this.holder.add(new CurryingWarp2a<Object, Object, R>((BiFunction<Object, Object, R>) function));
            }

            protected void doApply(@NotNull final Object value) {
                final CurryingWarp<?, ?> curryingWarp = holder.get(pointer);
                if (curryingWarp.more() < 1) {
                    pointer += 1;
                    doApply(value);
                } else {
                    curryingWarp.applyParam(value);
                    parameters.remove(0);
                }
            }

            @Override
            public Currying<R> apply(@NotNull final Object parameter) {
                if (parameters.isEmpty() || pointer >= holder.size()) {
                    throw new IllegalStateException(" no more parameter");
                } else if (typeSafe && !parameters.get(0).isInstance(parameter)) {
                    throw new IllegalStateException("wrong type of parameter: wanna " + parameters.get(0).getSimpleName() + " but get " + parameter.getClass().getSimpleName());
                } else {
                    doApply(parameter);
                }
                return this;
            }

            @Override
            public int remainParameterCount() {
                return parameters.size();
            }

            @Override
            public int totalParameterCount() {
                final int[] t = {0};
                holder.forEach(i -> t[0] += i.total());
                return t[0];
            }

            @SuppressWarnings("unchecked")
            @Override
            public @NotNull <R1> Currying<R1> composite(@NotNull final Function<R, R1> function) {
                this.holder.add(new CurryingWarp1<Object, R1>((Function<Object, R1>) function));
                return new CurryingImpl<>(holder, parameters, pointer, typeSafe);
            }

            @SuppressWarnings("unchecked")
            @Override
            public @NotNull <P, R1> Currying<R1> composite(@NotNull final BiFunction<R, P, R1> function) {
                this.holder.add(new CurryingWarp2<Object, P, R1>((BiFunction<Object, P, R1>) function));
                this.parameters.add(Object.class);
             /*   this.parameters.add(
                    Arrays.stream(function.getClass().getMethods())
                        .filter(m -> m.getName().equals("apply"))
                        .findFirst().map(m -> m.getParameterTypes()[0]).orElseThrow(() -> new IllegalStateException(""))
                );*/
                return new CurryingImpl<>(holder, parameters, pointer, typeSafe);
            }

            @SuppressWarnings("unchecked")
            @Override
            public @NotNull <P, R1> Currying<R1> composite(@NotNull final BiFunction<R, P, R1> function, Class<P> pClass) {
                this.holder.add(new CurryingWarp2<Object,P , R1>((BiFunction<Object,P, R1>) function));
                this.parameters.add(pClass);
             /*   this.parameters.add(
                    Arrays.stream(function.getClass().getMethods())
                        .filter(m -> m.getName().equals("apply"))
                        .findFirst().map(m -> m.getParameterTypes()[0]).orElseThrow(() -> new IllegalStateException(""))
                );*/
                return new CurryingImpl<>(holder, parameters, pointer, typeSafe);
            }

            @Override
            public @NotNull Optional<Class<?>> nextParameter() {
                return Optional.ofNullable(parameters.isEmpty() ? null : parameters.get(0));
            }

            @Override
            public @NotNull List<Class<?>> remainParameters() {
                return Collections.unmodifiableList(this.parameters);
            }

            @SuppressWarnings("unchecked")
            @Override
            public Optional<R> get() {
                if (parameters.isEmpty()) {
                    Object r = null;
                    for (final CurryingWarp<Object, ?> warp : holder) {
                        r = warp.apply(r);
                    }
                    return (Optional<R>) Optional.ofNullable(r);
                }
                return Optional.empty();
            }
        }

    }
}
