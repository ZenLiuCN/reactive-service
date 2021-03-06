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
 *   @File: PluginManager.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-17 21:34:22
 */

package cn.zenliu.reactive.service.plugin;


import cn.zenliu.reactive.service.plugin.hikari.HikariManager;
import cn.zenliu.reactive.service.util.Singleton;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.ServiceLoader.load;

public interface PluginManager {
    //region SPI define
    <T extends Plugin> Optional<T> getPluginOf(Class<T> clz, boolean preferSPI);

    <T extends Plugin> T getPluginOfNeeded(Class<T> clz, boolean preferSPI) throws IllegalStateException;

    /**
     * initialization should be call before all actions
     *
     * @param pluginClass classes of plugins which are needed
     * @param preferSPI   if false ,will use reflect to generate instance from plugins
     *                    {@see use static method of getInstance(),eg {@link  HikariManager#getInstance()}}
     */
    void initialization(@NonNull Class<?>[] pluginClass, boolean preferSPI);
    //endregion

    //region SPI template
    static Singleton<PluginManager> getSingleton() {
        return scope.singleton;
    }


    static PluginManager getSoftInstance() {
        return scope.singleton.getSoftInstance().orElseThrow(Plugin.scope::InstanceErrorSuppler);
    }

    static PluginManager getHardInstance() {
        return scope.singleton.getHardReference().orElseThrow(Plugin.scope::InstanceErrorSuppler);
    }

    //endregion
    @UtilityClass
    class scope {
        protected Singleton<PluginManager> singleton =
            Singleton.generate(
                PluginManagerImpl::new,
                PluginManager.class
            );

        @Slf4j
        final class PluginManagerImpl implements PluginManager {

            //region SPI impl
            private final Map<Class<? extends Plugin>, SoftReference<? extends Plugin>> pluginCache = new ConcurrentHashMap<>();

            @Override
            public <T extends Plugin> Optional<T> getPluginOf(Class<T> clz, boolean preferSPI) {
                if (pluginCache.containsKey(clz)) {
                    @SuppressWarnings("unchecked") final T old = (T) pluginCache.get(clz).get();
                    if (old == null) {
                        final Optional<T> ins = (preferSPI ? spiInstanceOf(clz) : reflectInstanceOf(clz));
                        if (ins.isPresent()) {
                            pluginCache.put(clz, new SoftReference<>(ins.get()));
                            return ins;
                        }
                    } else return Optional.of(old);
                } else {
                    final Optional<T> ins = (preferSPI ? spiInstanceOf(clz) : reflectInstanceOf(clz));
                    if (ins.isPresent()) {
                        pluginCache.put(clz, new SoftReference<>(ins.get()));
                        return ins;
                    }
                }
                return Optional.empty();
            }

            @Override
            public <T extends Plugin> T getPluginOfNeeded(Class<T> clz, boolean preferSPI) throws IllegalStateException {
                final Optional<T> op = getPluginOf(clz, preferSPI);
                if (!op.isPresent())
                    throw new IllegalStateException("required plugin of " + clz.getCanonicalName() + " not found");
                return op.get();
            }

            @Override
            public void initialization(@NonNull Class<?>[] pluginClass, boolean preferSPI) {
                Arrays.asList(pluginClass)
                    .forEach(clz -> {
                        if (clz.isInterface() && Plugin.class.isAssignableFrom(clz)) {
                            @SuppressWarnings("unchecked") final Class<? extends Plugin> classI = (Class<? extends Plugin>) clz;
                            final Optional<? extends Plugin> opt = reflectInstanceOf(classI);
                            opt.ifPresent(t -> pluginCache.put(classI, new SoftReference<Plugin>(t)));
                        } else {
                            throw new IllegalArgumentException(clz.getCanonicalName() + " is not a plugin");
                        }
                    });
                if(preferSPI){
                    final Iterator<Plugin> itr = load(Plugin.class).iterator();
                    itr.forEachRemaining(p -> {
                        //if (!preferSPI) pluginCache.putIfAbsent(p.getClass(), new SoftReference<>(p));
                        //else pluginCache.put(p.getClass(), new SoftReference<>(p));
                         pluginCache.put(p.getClass(), new SoftReference<>(p));
                    });
                }

            }

            private <T extends Plugin> Optional<T> spiInstanceOf(Class<T> clz) {
                final Iterator<T> itr = load(clz).iterator();
                if (!itr.hasNext()) {
                    return Optional.empty();
                } else {
                    final T instance = itr.next();
                    pluginCache.put(clz, new SoftReference<>(instance));
                    return Optional.of(instance);
                }
            }
            @SuppressWarnings("unchecked")
            private <T extends Plugin> Optional<T> reflectInstanceOf(Class<T> clz) {
                /*try {
                    final Method callable = clz.getDeclaredMethod("getInstance");
                    @SuppressWarnings("unchecked") final T instance = (T) callable.invoke(null);
                    if (instance == null) return Optional.empty();
                    pluginCache.put(clz, new SoftReference<T>(instance));
                    return Optional.of(instance);
                } catch (Throwable t) {
                    return Optional.empty();
                }*/
                 try {
                    final Method callable = clz.getDeclaredMethod("getSingleton");
                    final Singleton<? extends Plugin> singleton = (Singleton<? extends Plugin>) callable.invoke(null);
                    if (singleton == null) return Optional.empty();
                     final Optional<? extends Plugin> hardReference = singleton.getHardReference();
                     if(hardReference.isPresent()){
                         final T instance=(T)hardReference.get();
                         pluginCache.put(clz, new SoftReference<T>(instance));
                         return Optional.of(instance);
                     }
                    return Optional.empty();
                } catch (Throwable t) {
                    return Optional.empty();
                }
            }

            //endregion

        }
    }
}