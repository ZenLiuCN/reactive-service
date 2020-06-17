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
import cn.zenliu.reactive.service.util.Singleton;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import reactor.util.annotation.Nullable;

import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

public interface EbeanManager extends Plugin {
    //region SPI define
    void init(Properties conf, @Nullable Function<@NonNull DatabaseConfig, @NonNull DatabaseConfig> configurator);

    Optional<Database> getDatabase();
    //endregion

    //region SPI template
    static Singleton<EbeanManager> getSingleton() {
        return scope.singleton;
    }

    static EbeanManager getSoftInstance() {
        return scope.singleton.getSoftInstance()
            .orElseThrow(Plugin.scope::InstanceErrorSuppler);
    }

    static EbeanManager getHardInstance() {
        return scope.singleton.getHardReference()
            .orElseThrow(Plugin.scope::InstanceErrorSuppler);
    }

    //endregion
    @UtilityClass
    class scope {
        protected Singleton<EbeanManager> singleton =
            Singleton.generate(
                EbeanManagerImpl::new,
                EbeanManager.class
            );


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

        }
    }

}