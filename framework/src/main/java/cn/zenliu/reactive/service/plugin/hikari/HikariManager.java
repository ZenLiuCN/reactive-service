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
 *   @File: HikariManager.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-13 14:46:24
 */

package cn.zenliu.reactive.service.plugin.hikari;



import cn.zenliu.reactive.service.plugin.Plugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Function;

public interface HikariManager extends Plugin {
    //region SPI define
    void configuration(@Nullable Properties conf);

    DataSource getDataSource(Function<HikariConfig, HikariConfig> configurator);
    //endregion

    //region SPI template
    static void setUseSPI(boolean useSPI) {
        HikariManagerImpl.disableSPI = !useSPI;
    }

    static HikariManager getInstance() {
        return HikariManagerImpl.hardInstance();
    }

    static HikariManager getSoftInstance() {
        return HikariManagerImpl.softInstance();
    }

    static HikariManager getHardInstance() {
        return HikariManagerImpl.hardInstance();
    }
    //endregion

    @Slf4j
    final class HikariManagerImpl implements HikariManager {

        //region SPI template
        @Setter
        @Getter
        static volatile boolean disableSPI = false;

        private static HikariManager loadService() {
            HikariManager impl = null;
            if (!disableSPI)
                try {
                    final Iterator<HikariManager> itr = ServiceLoader.load(HikariManager.class).iterator();
                    impl = itr.hasNext() ? itr.next() : null;
                } catch (Throwable t) {
                    log.debug("error on load service HikariManager via SPI", t);
                }
            if (impl == null) {
                if (!disableSPI) log.debug("not found other impl of HikariManager via SPI, use default impl!");
                impl = new HikariManagerImpl();
            }
            return impl;
        }

        @Synchronized
        static HikariManager softInstance() {
            if (softHolder != null) {
                final HikariManager impl = softHolder.get();
                if (impl != null) return impl;
            }
            HikariManager impl = loadService();
            softHolder = new SoftReference<>(impl);
            return impl;
        }

        static SoftReference<HikariManager> softHolder;

        @Synchronized
        static HikariManager hardInstance() {
            if (hardHolder != null) return hardHolder;
            hardHolder = loadService();
            return hardHolder;
        }

        static HikariManager hardHolder;
        //endregion

        //region SPI impl
        private Properties conf;

        @Override
        public void configuration(Properties conf) {
            this.conf = conf;
        }

        private DataSource dsHolder;

        @Override
        public DataSource getDataSource(Function<HikariConfig, HikariConfig> configurator) {
            if (dsHolder == null) {
                HikariConfig config = new HikariConfig(conf);
                if (configurator != null) {
                    config = configurator.apply(config);
                }
                dsHolder = new HikariDataSource(config);
            }
            return dsHolder;
        }
        //endregion
    }
}