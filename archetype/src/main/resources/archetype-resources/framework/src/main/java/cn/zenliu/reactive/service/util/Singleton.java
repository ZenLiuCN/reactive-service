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
 *   @File: SingletonHolder.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-13 14:48:31
 */

package cn.zenliu.reactive.service.util;


import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.SoftReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * this is just singleton container,use to warp any class as ServiceProvider|Factory singleton container
 *
 * @param <T>
 * @author Zen.Liu
 */
public interface Singleton<T> {
    /**
     * get first (maybe random) instance  from SPI (service provider interface)
     *
     * @param wanna : if not null ,means wanna same as this instance
     * @return available when 1. construct with service provider interface Class. 2. no error happen when use service
     * loader
     */
    <F extends T> Optional<F> oneFromSPI(@Nullable Class<F> wanna);

    /**
     * get all instance  from SPI (service provider interface)
     *
     * @return available when 1. construct with service provider interface Class. 2. no error happen when use service
     * loader
     */
    Optional<List<T>> allFromSPI();

    /**
     * get soft referenced instance
     *
     * @return available when construct with instance supplier
     */
    Optional<T> getSoftInstance();

    /**
     * get hard referenced instance
     *
     * @return available when construct with instance supplier
     */
    Optional<T> getHardReference();

    static <T> Singleton<T> generate(
        @Nullable final Supplier<T> instanceSupplier,
        @Nullable final Class<? extends T> spi) {
        return new scope.SingletonImpl<>(instanceSupplier, spi);
    }

    @UtilityClass
    class scope {
       protected final class SingletonImpl<T> implements Singleton<T> {
            private SoftReference<T> softReference;
            private T hardReference;
            private final Supplier<T> instance;
            private final Class<? extends T> serviceProvider;

           SingletonImpl(final Supplier<T> instance, final Class<? extends T> serviceProvider) {
                this.instance = instance;
                this.serviceProvider = serviceProvider;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <F extends T> Optional<F> oneFromSPI(@Nullable Class<F> wanna) {
                if (serviceProvider == null) return Optional.empty();
                try {
                    final Iterator<? extends T> iterator = ServiceLoader.load(serviceProvider).iterator();
                    if (wanna != null) {
                        final AtomicReference<F> instance = new AtomicReference<F>(null);
                        iterator.forEachRemaining(i -> {
                            if (wanna.isInstance(i)) instance.set((F) i);
                        });
                        return Optional.ofNullable(instance.get());
                    } else
                        return Optional.ofNullable(iterator.hasNext() ? (F) iterator.next() : null);
                } catch (Exception e) {
                    e.printStackTrace();
                    return Optional.empty();
                }

            }

            @Override
            public Optional<List<T>> allFromSPI() {
                if (serviceProvider == null) return Optional.empty();
                try {
                    final Iterator<? extends T> iterator = ServiceLoader.load(serviceProvider).iterator();
                    final List<T> result = new ArrayList<>();
                    iterator.forEachRemaining(result::add);
                    return Optional.ofNullable(result.isEmpty() ? null : result);
                } catch (Exception e) {
                    e.printStackTrace();
                    return Optional.empty();
                }
            }

            @Override
            public Optional<T> getSoftInstance() {
                if (instance == null) return Optional.empty();
                if (softReference == null || softReference.get() == null) {
                    softReference = new SoftReference<>(instance.get());
                }
                return Optional.ofNullable(softReference.get());
            }

            @Override
            public Optional<T> getHardReference() {
                if (instance == null) return Optional.empty();
                if (hardReference == null) {
                    hardReference = instance.get();
                }
                return Optional.ofNullable(hardReference);
            }

        }
    }

}
