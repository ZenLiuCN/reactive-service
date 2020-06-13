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
 *   @File: ServerConfigurator.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-13 14:46:24
 */

package cn.zenliu.reactive.service.framework.spi;


import cn.zenliu.reactive.service.framework.config.ServerType;
import lombok.NonNull;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.TcpServer;
import reactor.netty.udp.UdpServer;
import reactor.util.annotation.Nullable;

import java.util.Map;

/**
 * this SPI to configure specificity server before server been started
 */
public interface ServerConfigurator {
    /**
     * @return this configurator name for human reading
     */
    @NonNull String getName();

    /**
     * @return (not null, empty means try on every server) map for server name and server type
     */
    @NonNull
    Map<String, ServerType> getConfigurableServer();

    /**
     * configure for http server
     *
     * @param server source server
     * @return server after configuration ,can be null(which will be drop)
     */
    @Nullable
    HttpServer configureHTTP(HttpServer server);

    /**
     * configure for TCP server
     *
     * @param server source server
     * @return server after configuration ,can be null(which will be drop)
     */
    @Nullable
    TcpServer configureTCP(TcpServer server);

    /**
     * configure for UDP server
     *
     * @param server source server
     * @return server after configuration ,can be null(which will be drop)
     */
    @Nullable
    UdpServer configureUDP(UdpServer server);
}
