#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.app.api;

import cn.zenliu.reactive.service.framework.spi.Api;
import com.google.auto.service.AutoService;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRoutes;

 //@AutoService(Api.class) //TODO not allow to use both Reflect mode and Register mod
public class Register implements Api.HttpApi {
    @Override
    public <R extends HttpServerRoutes> void register(final R route) {
        route.get("/", (q, r) -> r.sendString(Mono.just("hh")));
    }
}