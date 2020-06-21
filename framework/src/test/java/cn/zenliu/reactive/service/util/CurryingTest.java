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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

class CurryingTest {
    @Test
    void curryingTypeSafeTest() {
        final Currying<Integer> c = Currying.of((Integer a, Integer b) -> a + b * a, Integer.class, Integer.class);
        assertEquals(2, c.remainParameterCount());
        assertEquals(3, c.apply(1).apply(2).get().orElse(-1));
        final Currying<String> c2 = c.composite(Object::toString);
        assertNull(c2.nextParameter().orElse(null));
        assertEquals(0, c2.remainParameterCount());
        assertEquals("3", c2.get().orElse("-1"));
        final Currying<Double> c3 = c2.composite((String res, Double a) -> Integer.parseInt(res) / a, Double.class);
        assertEquals(Double.class, c3.nextParameter().orElse(null));
        assertEquals(1, c2.remainParameterCount());
        assertEquals(3.0, c3.apply(1.0).get().orElse(-1.0));
        final Currying<Double> c4 = c3.composite((@NotNull BiFunction<Double, Integer, Double>) Double::sum, Integer.class);
        assertThrows(IllegalStateException.class, () -> c4.apply(1.2).get().orElse(-1.0));
        assertEquals(4.0, c4.apply(1).get().orElse(-1.0));
    }

    @Test
    void curryingTest() {
        final Currying<Integer> c = Currying.of((Integer a, Integer b) -> a + b * a);
        assertEquals(3, c.apply(1).apply(2).get().orElse(-1));
        final Currying<String> c2 = c.composite(Object::toString);
        assertNull(c2.nextParameter().orElse(null));
        assertEquals("3", c2.get().orElse("-1"));
        final Currying<Double> c3 = c2.composite((String res, Double a) -> Integer.parseInt(res) / a);
        assertEquals(3.0, c3.apply(1.0).get().orElse(-1.0));
    }
}