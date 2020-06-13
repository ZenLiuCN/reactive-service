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
 *   @File: ServerConf.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-13 14:46:24
 */

package cn.zenliu.reactive.service.framework.config;


import com.typesafe.config.Optional;
import io.netty.channel.ChannelOption;
import lombok.Data;
import lombok.NonNull;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.TcpServer;
import reactor.netty.udp.UdpServer;

import java.time.Duration;

@Data
public class ServerConf {
    @NonNull
    @Optional
    private ServerType type = ServerType.HTTP;
    @Optional
    private String host = "0.0.0.0";
    @Optional
    private int port;
    @Optional
    private String tlsKey;
    @Optional
    private Conf raw;
    @Optional
    private String tlsCert;
    @Optional
    private boolean metrics = false;
    @Optional
    private boolean forwarded = false;
    @Optional
    private boolean wiretap = false;
    @Optional
    private int compress = 0;
    @Optional
    private Duration startTimeout = Duration.ofSeconds(5);
    @Optional
    private UDPBroadcast broadcast = null;

    public HttpServer getHttpServer() {
        if (type != ServerType.HTTP) {
            throw new IllegalArgumentException("type is not http Server");
        }

        HttpServer server = HttpServer.create()
                .host(host)
                .port(port)
                .metrics(metrics)
                .forwarded(forwarded);
        server = (compress > 0 ? server.compress(compress) : server);
        return server;
    }

    public UdpServer getUdpServer() {
        if (type != ServerType.UDP) {
            throw new IllegalArgumentException("type is not UDP Server");
        }
        return UdpServer.create()
                //.host(host)
                .port(port)
                .metrics(metrics)
                .bootstrap(b -> {
                    b.option(ChannelOption.AUTO_READ, true);
                    //  b.remoteAddress(InetSocketAddress.createUnresolved("255.255.255.255", port));
                   /* if(broadcast!=null){
                        b.option(ChannelOption.SO_BROADCAST,true);
                        try {
                            b.option(ChannelOption.IP_MULTICAST_ADDR, InetAddress.getByName(broadcast.getAddr()));
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                            System.exit(-1);
                        }
                        if(broadcast.getTtl()>0) b.option(ChannelOption.IP_MULTICAST_TTL, broadcast.getTtl());
                    }
*/
                    return b;
                })
                .wiretap(wiretap);

    }


    public TcpServer getTcpServer() {
        if (type != ServerType.TCP) {
            throw new IllegalArgumentException("type is not TCP Server");
        }
        return TcpServer.create()
                .host(host)
                .port(port)
                .metrics(metrics)
                .wiretap(wiretap);
    }
}
