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
 *   @File: CurryingTest.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-21 15:34:06
 */

package cn.zenliu.reactive.service.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurryingTest {
    @Test
    void curryingTest() {
        final Currying<Integer> c = Currying.of((Integer a, Integer b) -> a + b * a);
        assertEquals(3, c.apply(1).apply(2).get().orElse(-1));
        final Currying<String> c2 = c.composite(Object::toString);
        assertEquals("3", c2.get().orElse("-1"));
        final Currying<String> c3 =  c2.composite((Integer a, String res) -> res + a.toString());
        assertEquals("31", c3.apply(1).get().orElse("-1"));
    }
}