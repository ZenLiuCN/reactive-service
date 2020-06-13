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
 *   @File: MonoTest.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-13 15:56:30
 */

package cn.zenliu.reactive.service.util;

import org.junit.jupiter.api.Test;

class MonoTest {
    @Test
    void multiGetOnValueTest() {
        final Monad<String> stringMonad = Monad
            .unit(2)
            .map(i -> i + i)
            .map(Object::toString)
            .flatMap(s -> Monad.unitFrom(() -> s + "11"));

        String result = stringMonad.get()
            .orElseThrow(() -> new RuntimeException("failed"));

        assert result.equals("411");

        try {
            result = stringMonad.get()
                .orElseThrow(() -> new RuntimeException("failed"));
        } catch (Exception e) {
            assert e instanceof RuntimeException;
        }

    }

    @Test
    void multiGetTest() {
        final int[] inc={-1};
        final Monad<String> stringMonad = Monad
            .unitFrom(() -> {
                inc[0]=inc[0]+1;
                return inc[0]+ 2;
            })
            .map(i -> i + i)
            .map(Object::toString)
            .flatMap(s -> Monad.unitFrom(() -> s + "11"));

        String result = stringMonad.get()
            .orElseThrow(() -> new RuntimeException("failed"));

        assert result.equals("411");
        result = stringMonad.get()
            .orElseThrow(() -> new RuntimeException("failed"));
        assert result.equals("611");

        assert inc[0]==1; //how many  counter now


    }

    @Test
    void flatTest() {
        final String result = Monad
            .unit(2)
            .map(i -> i + i)
            .map(Object::toString)
            .flatMap(s -> Monad.unitFrom(() -> s + "11"))
            .get()
            .orElseThrow(() -> new RuntimeException("failed"));
        assert result.equals("211");
    }

    @Test
    void valueTest() {
        final String result = Monad
            .unit(2)
            .map(i -> i + i)
            .map(Object::toString)
            .get()
            .orElseThrow(() -> new RuntimeException("failed"));
        assert result.equals("4");
    }

    @Test
    void generatorTest() {
        final String result = Monad
            .unitFrom(() -> 2)
            .map(i -> i + i)
            .map(Object::toString)
            .get()
            .orElseThrow(() -> new RuntimeException("failed"));
        assert result.equals("4");

    }
}