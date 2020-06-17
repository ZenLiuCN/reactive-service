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
import cn.zenliu.reactive.service.util.Singleton;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

public interface CaffeineManager extends Plugin {
    //region SPI define

    //endregion

    //region SPI template
    static Singleton<CaffeineManager> getSingleton() {
        return scope.singleton;
    }
    static CaffeineManager getSoftInstance() {
        return scope.singleton.getSoftInstance()
            .orElseThrow(Plugin.scope::InstanceErrorSuppler);
    }

    static CaffeineManager getHardInstance() {
        return scope.singleton.getHardReference()
            .orElseThrow(Plugin.scope::InstanceErrorSuppler);
    }
    //endregion
    @UtilityClass
    class scope{
        protected Singleton<CaffeineManager> singleton =
            Singleton.generate(
              CaffeineManagerImpl::new,
                CaffeineManager.class
            );
        @Slf4j
        final class CaffeineManagerImpl implements CaffeineManager {
            //region SPI impl

            //endregion
        }
    }

}