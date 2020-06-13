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
 *   @File: CaffeineManager.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-13 14:46:24
 */

package cn.zenliu.reactive.service.plugin.caffeine;

import cn.zenliu.reactive.service.plugin.Plugin;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.ServiceLoader;

public interface CaffeineManager extends Plugin {
    //region SPI define

    //endregion

    //region SPI template
    static void setUseSPI(boolean useSPI) {
        CaffeineManagerImpl.disableSPI = !useSPI;
    }

    static CaffeineManager getInstance() {
        return CaffeineManagerImpl.hardInstance();
        //return CaffeineManagerImpl.softInstance();
    }

    static CaffeineManager getSoftInstance() {
        return CaffeineManagerImpl.softInstance();
    }

    static CaffeineManager getHardInstance() {
        return CaffeineManagerImpl.hardInstance();
    }
    //endregion

    @Slf4j
    final class CaffeineManagerImpl implements CaffeineManager {

        //region SPI template
        @Setter
        @Getter
        static volatile boolean disableSPI = false;

        private static CaffeineManager loadService() {
            CaffeineManager impl = null;
            if (!disableSPI)
                try {
                    final Iterator<CaffeineManager> itr = ServiceLoader.load(CaffeineManager.class).iterator();
                    impl = itr.hasNext() ? itr.next() : null;
                } catch (Throwable t) {
                    log.debug("error on load service CaffeineManager via SPI", t);
                }
            if (impl == null) {
                if (!disableSPI) log.debug("not found other impl of CaffeineManager via SPI, use default impl!");
                impl = new CaffeineManagerImpl();
            }
            return impl;
        }

        @Synchronized
        static CaffeineManager softInstance() {
            if (softHolder != null) {
                final CaffeineManager impl = softHolder.get();
                if (impl != null) return impl;
            }
            final CaffeineManager impl = loadService();
            softHolder = new SoftReference<>(impl);
            return impl;
        }

        static SoftReference<CaffeineManager> softHolder;

        @Synchronized
        static CaffeineManager hardInstance() {
            if (hardHolder != null) return hardHolder;
            hardHolder = loadService();
            return hardHolder;
        }

        static CaffeineManager hardHolder;
        //endregion

        //region SPI impl

        //endregion
    }
}