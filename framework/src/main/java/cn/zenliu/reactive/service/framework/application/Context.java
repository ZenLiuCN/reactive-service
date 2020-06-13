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
 *   @File: Context.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-13 14:46:24
 */

package cn.zenliu.reactive.service.framework.application;


import cn.zenliu.reactive.service.framework.config.Conf;
import cn.zenliu.reactive.service.framework.config.ServerConf;
import cn.zenliu.reactive.service.framework.spi.Api;
import cn.zenliu.reactive.service.util.Tuple;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * application context
 * handle all runtime
 */
@UtilityClass
@Slf4j
public class Context {
    final List<Api> apis = new LinkedList<>();
    private Map<String, Server> servers;
    public final Conf config = Conf.load();

    private void findApi() {
        ServiceLoader.load(Api.class).forEach(apis::add);
    }

    private void configuration() {
        log.debug("init configuration of Netty Logger Factory");
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
    }

    private void parseServer() {
        servers = config
            .getConf("servers", 0)
            .entries()
            .stream()
            //convert to server
            .map(e -> {
                final ServerConf serverConf = e.getValue().mapToObject(ServerConf.class);
                if (serverConf == null) {
                    Optional<Server> opt = Optional.empty();
                    return Tuple.pairOf(e.getKey(), opt);
                } else {
                    serverConf.setRaw(e.getValue());
                    return Tuple.pairOf(e.getKey(), Server.createFromConfig(e.getKey(), serverConf));
                }
            })
            //find valid
            .filter(p -> p.getSecond().isPresent())
            //process server
            .map(p -> {
                final String name = p.getFirst();
                final Server server = p.getSecond().get();
                server.autoConfigTLS();
                apis.stream().filter(a -> a.registerOn().contains(name) || a.registerOn().isEmpty()).forEach(server::registerApi);
                return Tuple.pairOf(name, server);
            })
            .collect(Collectors.toMap(Tuple.Pair::getFirst, Tuple.Pair::getSecond));

    }

    private void startServer() {
        final int total = servers.size();

        final int[] idx = new int[]{1};

        servers.forEach((key, value) -> {
            if (value == null) {
                throw new IllegalStateException("server [" + key + "] invalid");
            } else if (total == idx[0]) {
                value.startBlock(null);
            } else {
                value.start(null);
            }
            idx[0] += 1;
        });
    }

    public void start(String[] args) {
        configuration();
        findApi();
        parseServer();
        if (servers.isEmpty()) {
            throw new IllegalArgumentException("server is empty!");
        }
        startServer();
    }

    public Optional<Server> getServer(String name) {
        return Optional.ofNullable(servers.get(name));
    }
}
