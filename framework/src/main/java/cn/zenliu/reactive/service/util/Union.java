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
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public interface Union {
    Class<?> getType();

    boolean isNull();

    boolean isType(Class<?> type);

    <T> Optional<T> getValue(Class<T> type);


    /**
     * guard method
     *
     * @param type     target type
     * @param consumer consumer of value
     * @param <T>      desired type
     */
    <T> Union caseDo(Class<T> type, Consumer<T> consumer);

    /**
     * default type guard method (should be last of all guards)
     *
     * @param consumer consumer of value
     */
    void caseElse(Consumer<Object> consumer);

    /**
     * create a Union with new value
     *
     * @param value must be one of the Union class type
     * @return new Union or throw RuntimeException
     */
    Union of(Object value);

    /**
     * check one Union is Same Union with other one
     *
     * @param other other Union
     * @return is same union
     */
    boolean is(Union other);

    interface Union2<T1, T2> extends Union {
        @Override
        Union2<T1, T2> of(Object value);
    }

    interface Union3<T1, T2, T3> extends Union {
        @Override
        Union3<T1, T2, T3> of(Object value);
    }


    static Union of(@Nullable Object t, Class<?>... types) {
        final HashSet<Class<?>> classes = new HashSet<>(Arrays.asList(types));
        if (t != null) classes.add(t.getClass());
        return new scope.UnionAll(t, classes);
    }

    static <T1, T2> Union2<T1, T2> of(@Nullable Object t, Class<T1> c1, Class<T2> c2) {
        if (c1.isInstance(t) || c2.isInstance(t))
            return new scope.Union2Impl<T1, T2>(t, c1, c2);
        else return new scope.Union2Impl<T1, T2>(null, c1, c2);
    }

    static <T1, T2, T3> Union3<T1, T2, T3> of(@Nullable Object t, Class<T1> c1, Class<T2> c2, Class<T3> c3) {
        if (c1.isInstance(t) || c2.isInstance(t) || c3.isInstance(t))
            return new scope.Union3Impl<T1, T2, T3>(t, c1, c2, c3);
        else return new scope.Union3Impl<T1, T2, T3>(null, c1, c2, c3);
    }

    @UtilityClass
    class scope {
        protected abstract class UnionImpl implements Union {
            private final List<Class<?>> holder = new ArrayList<>();
            private final Object value;
            private volatile boolean match = false;

            @Override
            public boolean isNull() {
                return value == null;
            }

            public UnionImpl(Object t, Collection<Class<?>> classes) {
                this.value = t;
                this.holder.addAll(classes);
            }

            @Override
            public int hashCode() {
                return holder.hashCode() + (value==null?31:31 * value.hashCode());
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof UnionImpl) {
                    final UnionImpl other = (UnionImpl) obj;
                    return other.holder.hashCode() == holder.hashCode() && value.equals(other.value);
                } else return false;
            }


            @SuppressWarnings("unchecked")
            @Override
            public <T> Union caseDo(Class<T> type, Consumer<T> consumer) {
                if (type.isInstance(value)) {
                    synchronized (this) {
                        match = true;
                        consumer.accept((T) value);
                    }
                }
                return this;
            }

            @Override
            public void caseElse(Consumer<Object> consumer) {
                if (!match) {
                    consumer.accept(value);
                }
            }


            @Override
            public boolean is(Union other) {
                return other instanceof UnionImpl && ((UnionImpl) other).holder.hashCode() == this.holder.hashCode();
            }

            @Override
            public boolean isType(Class<?> type) {
                return type.isInstance(value);
            }

            @Override
            public Class<?> getType() {
                return value.getClass();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> Optional<T> getValue(Class<T> type) {
                return Optional.ofNullable(type.isInstance(value) ? (T) value : null);
            }
        }

        protected final class UnionAll extends UnionImpl implements Union {
            public UnionAll(Object t, Collection<Class<?>> classes) {
                super(t, classes);
            }

            @Override
            public Union of(Object value) {
                return new UnionAll(value, super.holder);
            }
        }

        private final class Union2Impl<T1, T2> extends UnionImpl implements Union2<T1, T2> {
            private final Class<T1> clz1;
            private final Class<T2> clz2;

            Union2Impl(Object t, Class<T1> c1, Class<T2> c2) {
                super(t, Collections.emptyList());//todo performance
                this.clz1 = c1;
                this.clz2 = c2;
            }


            @SuppressWarnings("rawtypes")
            @Override
            public boolean is(Union other) {
                return (other instanceof Union2Impl) && ((Union2Impl) other).clz1 == clz1 && ((Union2Impl) other).clz2 == clz2;
            }

            @Override
            public Union2<T1, T2> of(Object value) {
                return Union.of(value, clz1, clz2);
            }
        }

        private final class Union3Impl<T1, T2, T3> extends UnionImpl implements Union3<T1, T2, T3> {
            private final Class<T1> clz1;
            private final Class<T2> clz2;
            private final Class<T3> clz3;

            Union3Impl(Object t, Class<T1> c1, Class<T2> c2, Class<T3> c3) {
                super(t, Collections.emptyList());//todo performance
                this.clz1 = c1;
                this.clz2 = c2;
                this.clz3 = c3;
            }


            @SuppressWarnings("rawtypes")
            @Override
            public boolean is(Union other) {
                return (other instanceof Union3Impl) && ((Union3Impl) other).clz1 == clz1 && ((Union3Impl) other).clz2 == clz2;
            }

            @Override
            public Union3<T1, T2, T3> of(Object value) {
                return Union.of(value, clz1, clz2, clz3);
            }
        }
    }
}
