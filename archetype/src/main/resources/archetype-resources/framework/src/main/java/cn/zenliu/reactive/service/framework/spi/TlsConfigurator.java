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
 *   @File: TlsConfigurator.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-13 14:46:24
 */

package cn.zenliu.reactive.service.framework.spi;

import cn.zenliu.reactive.service.framework.config.ServerConf;
import io.netty.handler.ssl.SslContextBuilder;
import org.jetbrains.annotations.NotNull;
import reactor.netty.tcp.SslProvider;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * this interface have all default impl,can be used directly
 */
public interface TlsConfigurator {
    /**
     * this method will use to filter which server name should use this (server must be http or tcp)
     *
     * @return set of server name,empty means try all
     */
    default @NotNull Set<String> registerOn() {
        return new HashSet<>();
    }

    default void config(SslProvider.SslContextSpec spec, @NotNull ServerConf conf) {
        try {
            final File key = new File(conf.getTlsKey());
            final File cert = new File(conf.getTlsCert());
            spec.sslContext(SslContextBuilder.forServer(cert, key));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
