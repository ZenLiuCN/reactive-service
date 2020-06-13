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
 *   @File: RequestUtil.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-13 14:46:24
 */

package cn.zenliu.reactive.service.framework.util;

import io.netty.handler.codec.http.QueryStringDecoder;
import lombok.experimental.UtilityClass;
import reactor.netty.http.server.HttpServerRequest;
import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.Map;

@UtilityClass
public class RequestUtil {

    /**
     * decode request parameters from request
     *
     * @param req request
     * @return
     */
    @Nullable
    public Map<String, List<String>> parseQueryParam(HttpServerRequest req) {
        return new QueryStringDecoder(req.uri()).parameters();
    }
}
