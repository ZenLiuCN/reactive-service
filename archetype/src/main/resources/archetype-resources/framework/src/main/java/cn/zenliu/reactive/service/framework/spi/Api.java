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
 *   @File: Api.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-13 14:46:24
 */

package cn.zenliu.reactive.service.framework.spi;

import lombok.NonNull;
import org.reactivestreams.Publisher;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.netty.http.server.HttpServerRoutes;
import reactor.netty.udp.UdpInbound;
import reactor.netty.udp.UdpOutbound;

import java.util.HashSet;
import java.util.Set;

/**
 * common interface for spi loading and common methods define
 * should not be direct impl
 */
public interface Api {
    /**
     * @return valid server name for this api to register on
     * could be empty to try register on every valid server type
     * server type must same as spec impl SPI type,such as HttpApi ,TcpApi or UdpApi
     */
    @NonNull
    default Set<String> registerOn() {
        return new HashSet<>();
    }

    /**
     * with default impl
     *
     * @return name of this api
     */
    @NonNull
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * interface for Http api define
     */
    interface HttpApi extends Api {
        default <R extends HttpServerRoutes> void register(R route) {

        }
    }

    /**
     * interface for UDP api define
     */
    interface UdpApi extends Api {
        @NonNull Publisher<Void> handle(@NonNull UdpInbound in, @NonNull UdpOutbound out);
    }

    /**
     * interface for TCP api define
     */
    interface TcpApi extends Api {
        @NonNull Publisher<Void> handle(@NonNull NettyInbound in, @NonNull NettyOutbound out);
    }
}
