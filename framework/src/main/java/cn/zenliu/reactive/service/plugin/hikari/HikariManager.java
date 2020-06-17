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
import cn.zenliu.reactive.service.util.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.function.Function;

public interface HikariManager extends Plugin {
    //region SPI define
    void configuration(@Nullable Properties conf);

    DataSource getDataSource(Function<HikariConfig, HikariConfig> configurator);
    //endregion

    //region SPI template

    static Singleton<HikariManager> getSingleton() {
        return scope.singleton;
    }

    static HikariManager getSoftInstance() {
        return scope.singleton.getSoftInstance()
            .orElseThrow(Plugin.scope::InstanceErrorSuppler);
    }

    static HikariManager getHardInstance() {
        return scope.singleton.getHardReference()
            .orElseThrow(Plugin.scope::InstanceErrorSuppler);
    }
    //endregion
    @UtilityClass
    class scope {
        protected Singleton<HikariManager> singleton =
            Singleton.generate(
                HikariManager.scope.HikariManagerImpl::new,
                HikariManager.class
            );
    @Slf4j
    final class HikariManagerImpl implements HikariManager {

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
    }}
}