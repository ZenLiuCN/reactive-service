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
 *   @File: Server.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-13 14:46:24
 */

package cn.zenliu.reactive.service.framework.application;


import cn.zenliu.reactive.service.framework.config.ServerConf;
import cn.zenliu.reactive.service.framework.config.ServerType;
import cn.zenliu.reactive.service.framework.internal.DisposableUdpServer;
import cn.zenliu.reactive.service.framework.spi.Api;
import cn.zenliu.reactive.service.framework.spi.Rest;
import cn.zenliu.reactive.service.framework.spi.RestApi;
import cn.zenliu.reactive.service.framework.spi.TlsConfigurator;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import lombok.NonNull;
import lombok.Synchronized;
import org.reactivestreams.Publisher;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpServer;
import reactor.netty.udp.UdpServer;
import reactor.util.annotation.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;


public class Server {
    static final char EMPTY_CHAR = '\0';
    static final char DIVIDER_CHAR = '/';
    private TcpServer tcp;
    private HttpServer http;
    private UdpServer udp;
    private final ServerConf conf;
    private final String name;
    private DisposableServer instance = null;
    private final InternalLogger log = InternalLoggerFactory.getInstance(this.getClass());
    private volatile int reflectApiMode = 0;

    @NonNull
    static Optional<Server> createFromConfig(String name, ServerConf conf) {
        if (conf.getType() == ServerType.HTTP)
            return Optional.of(new Server(name, conf, null, conf.getHttpServer(), null));
        else if (conf.getType() == ServerType.TCP)
            return Optional.of(new Server(name, conf, conf.getTcpServer(), null, null));
        else if (conf.getType() == ServerType.UDP)
            return Optional.of(new Server(name, conf, null, null, conf.getUdpServer()));
        else return Optional.empty();
    }

    Server(@NonNull String name, @NonNull ServerConf conf, TcpServer tcp, HttpServer http, UdpServer udp) {
        this.name = name;
        this.conf = conf;
        if (tcp != null) {
            this.tcp = tcp;
            this.http = null;
            this.udp = null;

        } else if (http != null) {
            this.http = http;
            this.tcp = null;
            this.udp = null;

        } else if (udp != null) {
            this.http = null;
            this.tcp = null;
            this.udp = udp;
        } else {
            this.http = null;
            this.tcp = null;
            this.udp = null;
        }
    }

    public ServerType getType() {
        if (http != null) return ServerType.HTTP;
        else if (tcp != null) return ServerType.TCP;
        else if (udp != null) return ServerType.UDP;
        else return ServerType.INVALID;
    }

    public ServerConf getConf() {
        return conf;
    }

    public void close() {
        if (instance != null) instance.disposeNow();
    }

    /**
     * config for tls
     *
     * @param sslProviderBuilder ssl provider
     * @throws IllegalStateException when is a UDP server
     */
    public void configTls(Function<ServerConf, Consumer<? super SslProvider.SslContextSpec>> sslProviderBuilder) throws IllegalStateException {
        if (isRunning()) return;
        if (http != null) {
            http = http.secure(sslProviderBuilder.apply(conf));
        } else if (tcp != null) {
            tcp = tcp.secure(sslProviderBuilder.apply(conf));
        } else if (udp != null) {
            //not support for tls
            throw new IllegalStateException("udp server not support tls");
        }
    }

    public void autoConfigTLS() {
        if (isRunning()) return;
        final Iterator<TlsConfigurator> configurators = ServiceLoader.load(TlsConfigurator.class).iterator();
        if (!configurators.hasNext()) return;
        if (http != null) {
            configurators.forEachRemaining(e -> {
                final Set<String> names = e.registerOn();
                if (names.isEmpty() || names.contains(this.name))
                    http = http.secure(spec -> e.config(spec, conf));
            });
        } else if (tcp != null) {
            configurators.forEachRemaining(e -> {
                final Set<String> names = e.registerOn();
                if (names.isEmpty() || names.contains(this.name))
                    tcp = tcp.secure(spec -> e.config(spec, conf));
            });
        }
    }

    public boolean isRunning() {
        return instance != null && !instance.isDisposed();
    }

    private Optional<String> urlBuilder(@NonNull String parent, String child) {
        try {
            final StringBuilder current = new StringBuilder();
            final String joined = parent.isEmpty() ? child : parent + DIVIDER_CHAR + child;
            char last = EMPTY_CHAR;
            for (char c : joined.toCharArray()) {
                switch (last) {
                    case EMPTY_CHAR:
                        current.append(DIVIDER_CHAR);
                        last = DIVIDER_CHAR;
                        break;
                    case DIVIDER_CHAR:
                        if (c == DIVIDER_CHAR) continue;
                        else {
                            current.append(c);
                            last = c;
                        }
                        break;
                    default:
                        current.append(c);
                        last = c;
                }
            }
            return Optional.of(current.toString());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void registerApi(Api api) {
        if (http != null && api instanceof Api.HttpApi) {
            Class<? extends Api> clz = api.getClass();
            if (clz.isAnnotationPresent(Rest.class)) {
                final List<Method> methods = Arrays.stream(clz.getMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers())//must accessible
                        && m.isAnnotationPresent(RestApi.class)  //must with @RestApi annotation
                        && m.getReturnType() == Publisher.class //must return Publisher<Void>
                        //&& m.getGenericReturnType() ==//must return Publisher<Void>
                        && m.getParameterCount() == 2 //must 2 param
                    )
                    .peek(m -> m.setAccessible(true))
                    .collect(Collectors.toList());
                if (methods.isEmpty()) {
                    log.warn("[API]<Rest> " + api.getName() + " have no method marked with @RestApi");
                } else if (reflectApiMode != 0 && reflectApiMode != 2) {
                    throw new RuntimeException(
                        "use both Reflect Api Mode (with @Rest) and Register Mode (via register method) is not allowed!");
                } else {
                    final String parent = clz.getAnnotation(Rest.class).value().trim();
                    http = http.route(r -> {
                        methods.forEach(m -> {
                            final RestApi ann = m.getAnnotation(RestApi.class);
                            final Optional<String> opt = urlBuilder(parent, ann.url());
                            final String url = opt.orElse(null);
                            if (url == null || url.isEmpty()) {
                                log.warn("[API]<Rest> " + api.getName() + "#" + m.getName() + " have no valid url with @RestApi");
                            } else {

                                switch (ann.method()) {
                                    case GET:
                                        r.get(url, (rq, rs) -> {
                                            try {
                                                //noinspection unchecked
                                                return (Publisher<Void>) m.invoke(api, rq, rs);
                                            } catch (Throwable e) {
                                                throw new RuntimeException(e);
                                            }
                                        });
                                        break;
                                    case POST:
                                        r.post(url, (rq, rs) -> {
                                            try {        //noinspection unchecked
                                                return (Publisher<Void>) m.invoke(api, rq, rs);
                                            } catch (Throwable e) {
                                                throw new RuntimeException(e);
                                            }
                                        });
                                        break;
                                    case PUT:
                                        r.put(url, (rq, rs) -> {
                                            try {        //noinspection unchecked
                                                return (Publisher<Void>) m.invoke(api, rq, rs);
                                            } catch (Throwable e) {
                                                throw new RuntimeException(e);
                                            }
                                        });
                                        break;
                                    case DELETE:
                                        r.delete(url, (rq, rs) -> {
                                            try {        //noinspection unchecked
                                                return (Publisher<Void>) m.invoke(api, rq, rs);
                                            } catch (Throwable e) {
                                                throw new RuntimeException(e);
                                            }
                                        });
                                        break;
                                    case WEBSOCKET:
                                        final Parameter[] params = m.getParameters();
                                        if (params.length != 2
                                            || params[0].getType() != WebsocketInbound.class
                                            || params[1].getType() != WebsocketOutbound.class
                                        ) {
                                            log.warn("[API]<Rest> " + api.getName() + "#" + m.getName() + " is invalid websocket handler");
                                        } else r.ws(url, (wi, wo) -> {
                                            try {
                                                //noinspection unchecked
                                                return (Publisher<Void>) m.invoke(api, wi, wo);
                                            } catch (Throwable e) {
                                                throw new RuntimeException(e);
                                            }
                                        });
                                        break;
         /*                           case INDEX:
                                        r.index(url, (rq, rs) -> {
                                            try {
                                                m.invoke(api, rq, rs);
                                                return Mono.empty();
                                            } catch (Throwable e) {
                                                throw new RuntimeException(e);
                                            }
                                        });
                                        break;*/
                                    case HEAD:
                                        r.head(url, (rq, rs) -> {
                                            try {
                                                //noinspection unchecked
                                                return (Publisher<Void>) m.invoke(api, rq, rs);
                                            } catch (Throwable e) {
                                                throw new RuntimeException(e);
                                            }
                                        });
                                        break;
                                }
                                log.debug("[API]<Rest> " + api.getName() + "#" + m.getName() + " register on " + name + "[" + ann.method().name() + " " + url + "]");
                            }
                        });
                    });
                    reflectApiMode = 2;
                }
                log.debug("[API]<Rest> " + api.getName() + " register on " + name);
            } else {
                if (reflectApiMode != 1 && reflectApiMode != 0) {
                    throw new RuntimeException(
                        "use both Reflect Api Mode (with @Rest) and Register Mode (via register method) is not allowed!");
                }
                final Api.HttpApi AP = (Api.HttpApi) api;
                http = http.route(AP::register);
                log.debug("[API] " + api.getName() + " register on " + name);
                reflectApiMode = 1;
            }

        } else if (tcp != null && api instanceof Api.TcpApi) {
            tcp = tcp.handle(((Api.TcpApi) api)::handle);
            log.debug("[API] " + api.getName() + " register on " + name);
        } else if (udp != null && api instanceof Api.UdpApi) {
            udp = udp.handle(((Api.UdpApi) api)::handle);
            log.debug("[API] " + api.getName() + " register on " + name);
        } else {
            log.warn("[API] " + api.getName() + " ignore to register on " + name + " for api type not compatible");
        }

    }

    public void start(@Nullable Duration timeout) {
        Duration t = timeout;
        if (t == null) {
            t = conf.getStartTimeout();
        }
        startOnNewThread(t);
        log.info(serverStartInfo(false));
    }

    @Synchronized
    void startWithoutNewThread(@NonNull final Duration timeout) {
        if (http != null) {
            http.bindUntilJavaShutdown(timeout, d -> this.instance = d);
        } else if (tcp != null) {
            tcp.bindUntilJavaShutdown(timeout, d -> this.instance = d);
        } else if (udp != null) {
            this.instance = DisposableUdpServer.newDisposableServer(udp.bindNow(timeout));
        }
    }

    void startOnNewThread(@NonNull final Duration timeout) {
        new Thread(() -> startWithoutNewThread(timeout)).start();
    }

    private String serverStartInfo(boolean blocking) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[SERVER] ");
        if (blocking) sb.append("{BLOCKING} ");
        sb.append(name);
        sb.append(" <");
        sb.append(getType());
        sb.append(">");
        sb.append(" listen on ");
        sb.append(this.serverListenOn());
        return sb.toString();
    }

    private String serverListenOn() {
        if (instance != null) {
            return instance.host() + ":" + instance.port();
        } else {
            return conf.getHost() + ":" + conf.getPort();
        }
    }

    @Synchronized
    public void startBlock(Duration timeout) {
        Duration t = timeout;
        if (t == null) {
            t = conf.getStartTimeout();
        }
        log.info(serverStartInfo(true));
        this.startWithoutNewThread(t);
        this.instance.onDispose().block();
    }
}
