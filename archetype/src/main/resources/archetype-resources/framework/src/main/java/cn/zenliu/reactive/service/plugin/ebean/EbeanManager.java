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
 *   @File: EbeanManager.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-13 14:46:24
 */

package cn.zenliu.reactive.service.plugin.ebean;

import cn.zenliu.reactive.service.plugin.Plugin;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import lombok.NonNull;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import reactor.util.annotation.Nullable;

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import static java.util.ServiceLoader.load;

public interface EbeanManager extends Plugin {
    //region SPI define
    void init(Properties conf, @Nullable Function<@NonNull DatabaseConfig, @NonNull DatabaseConfig> configurator);

    Optional<Database> getDatabase();
    //endregion

    //region SPI template
    static EbeanManager getInstance() {
        return EbeanManagerImpl.hardInstance();
    }

    static EbeanManager getSoftInstance() {
        return EbeanManagerImpl.softInstance();
    }

    static EbeanManager getHardInstance() {
        return EbeanManagerImpl.hardInstance();
    }
    //endregion

    @Slf4j
    final class EbeanManagerImpl implements EbeanManager {
        //region SPI impl
        private Database ds;

        @Override
        public void init(Properties conf, @Nullable Function<@NonNull DatabaseConfig, @NonNull DatabaseConfig> configurator) {
            DatabaseConfig cfg = new DatabaseConfig();
            cfg.loadFromProperties();
            cfg.loadFromProperties(conf);
            if (configurator != null) cfg = configurator.apply(cfg);
            ds = DatabaseFactory.create(cfg);
        }

        @Override
        public Optional<Database> getDatabase() {
            return Optional.ofNullable(ds);
        }
        //endregion

        //region SPI template
        @Synchronized
        static EbeanManager softInstance() {
            if (softHolder != null) {
                final EbeanManager impl = softHolder.get();
                if (impl != null) return impl;
            }
            EbeanManager impl = loadService();
            if (impl == null) {
                log.debug("not found other impl of EbeanManager via SPI, use default impl!");
                impl = new EbeanManagerImpl();
            }
            softHolder = new SoftReference<>(impl);
            return impl;
        }

        static SoftReference<EbeanManager> softHolder;

        private static EbeanManager loadService() {
            try {
                final Iterator<EbeanManager> itr = load(EbeanManager.class).iterator();
                return itr.hasNext() ? itr.next() : null;
            } catch (Exception e) {
                return null;
            }
        }

        @Synchronized
        static EbeanManager hardInstance() {
            if (holder != null) return holder;
            holder = loadService();
            if (holder == null) {
                log.debug("not found other impl of EbeanManager via SPI, use default impl!");
                holder = new EbeanManagerImpl();
            }
            return holder;
        }

        static EbeanManager holder;

        //endregion
    }
}