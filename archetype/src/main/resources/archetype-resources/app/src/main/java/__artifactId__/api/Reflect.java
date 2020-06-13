#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.app.api;
import cn.zenliu.reactive.service.framework.spi.Api;
import cn.zenliu.reactive.service.framework.spi.Rest;
import cn.zenliu.reactive.service.framework.spi.RestApi;
import com.google.auto.service.AutoService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

@AutoService(Api.class)
@Rest
public class Reflect implements Api.HttpApi {
    @RestApi(url = "/echo",method=RestApi.RestMethod.POST)
    public Publisher<Void> someHandler(HttpServerRequest q, HttpServerResponse r) {
        return   q.receive()
            .aggregate()
            .asString()
            .as(r::sendString);
    }
}
