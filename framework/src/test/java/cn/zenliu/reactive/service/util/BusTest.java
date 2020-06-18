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
 *   @File: ReactiveBusTest.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-14 21:52:19
 */

package cn.zenliu.reactive.service.util;

import lombok.Data;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusTest {
    @Data
    static class EventImpl implements Bus.Event{
        final String message;
    }
    @Test
    void defaultTest() {
        final Bus<Bus.Event> bus = Bus.defaultBus.get()
            .orElseThrow(() -> new RuntimeException("hh"));
        assertEquals(bus, Bus.defaultBus.get().orElse(null));
    }
    @Test
    void default2Test() throws InterruptedException {
        final Bus<Bus.Event> bus = Bus.defaultBus.get()
            .orElseThrow(() -> new RuntimeException("hh"));
        final EventImpl ev=new EventImpl("ohoh");
       bus.subscribe(e->assertEquals(ev,e),null,null,null);
       bus.publish(ev);
       Thread.sleep(1000);
    }

}