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
 *   @File: DisposableUdpServer.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-13 14:46:24
 */

package cn.zenliu.reactive.service.framework.internal;

import io.netty.channel.Channel;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableChannel;
import reactor.netty.DisposableServer;

import java.net.InetSocketAddress;
import java.time.Duration;

public class DisposableUdpServer implements DisposableServer {
    private final Connection conn;

    public static DisposableServer newDisposableServer(Connection conn) {
        return new DisposableUdpServer(conn);
    }

    DisposableUdpServer(Connection conn) {
        this.conn = conn;
    }

    @Override
    public InetSocketAddress address() {
        return conn.address();
    }

    @Override
    public Channel channel() {
        return conn.channel();
    }

    @Override
    public void dispose() {
        conn.dispose();
    }

    @Override
    public void disposeNow() {
        conn.disposeNow();
    }

    @Override
    public void disposeNow(Duration timeout) {
        conn.disposeNow(timeout);
    }

    @Override
    public CoreSubscriber<Void> disposeSubscriber() {
        return conn.disposeSubscriber();
    }

    @Override
    public boolean isDisposed() {
        return conn.isDisposed();
    }

    @Override
    public Mono<Void> onDispose() {
        return conn.onDispose();
    }

    @Override
    public DisposableChannel onDispose(Disposable onDispose) {
        return conn.onDispose(onDispose);
    }

    @Override
    public String host() {
        return this.address().getHostString();
    }

    @Override
    public int port() {
        return this.address().getPort();
    }
}
