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

import java.util.*;
import java.util.function.Consumer;

public interface Union {
    Class<?> getType();

    boolean isType(Class<?> type);

    <T> Optional<T> getValue(Class<T> type);

    List<Class<?>> getMaybe();

    /**
     * guard method
     * @param type target type
     * @param consumer consumer of value
     * @param <T> desired type
     */
    <T> void caseDo(Class<T> type, Consumer<T> consumer);

    /**
     * default type guard method (should be last of all guards)
     * @param consumer consumer of value
     */
    void caseElse(Consumer<Object> consumer);

    /**
     * create a Union with new value
     * @param value must be one of the Union class type
     * @return new Union or throw RuntimeException
     */
    Union of(Object value);

    /**
     * check one Union is Same Union with other one
     * @param other other Union
     * @return is same union
     */
    boolean is(Union other);

    /**
     * create a new Union instance
     * @param t current union value
     * @param types support types
     * @return union
     */
    static Union of(@Nullable Object t, Class<?>... types) {
        final HashSet<Class<?>> classes = new HashSet<>(Arrays.asList(types));
        if(t!=null)  classes.add(t.getClass());
        return new scope.UnionImpl(t, classes);
    }

    final class scope {
        protected final static class UnionImpl implements Union {
            private final List<Class<?>> holder = new ArrayList<>();
            private final Object value;
            private volatile boolean match = false;

            public UnionImpl(Object t, Collection<Class<?>> classes) {
                this.value = t;
                this.holder.addAll(classes);
            }

            @Override
            public int hashCode() {
                return holder.hashCode() + 31 * value.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof UnionImpl) {
                    final UnionImpl other = (UnionImpl) obj;
                    return other.holder.hashCode() == holder.hashCode() && value.equals(other.value);
                } else return false;
            }

            @Override
            public List<Class<?>> getMaybe() {
                return Collections.unmodifiableList(holder);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> void caseDo(Class<T> type, Consumer<T> consumer) {
                if (type.isInstance(value)) {
                    synchronized (this) {
                        match = true;
                        consumer.accept((T) value);
                    }
                }
            }

            @Override
            public void caseElse(Consumer<Object> consumer) {
                if (!match) {
                    consumer.accept(value);
                }
            }

            @Override
            public Union of(Object value) {
                if (holder.contains(value.getClass()))
                    return new UnionImpl(value, holder);
                else
                    throw new RuntimeException("invalid value type of " + value.getClass() + " for Union " + holder.toString());
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
    }
}
