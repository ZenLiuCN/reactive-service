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
 *   @File: CaseTest.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-14 11:56:43
 */

package cn.zenliu.reactive.service.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

class CaseTest {
    @Test
    void voidCaseTest() {
        final int[] branch = {-1};
        Case.doMatch(1)
            .caseOf(long.class, i -> {
                branch[0] = 1;
            })
            .caseOf(2, i -> {
                branch[0] = 2;
            })
            .caseOf(i -> i == 2, i -> {
                branch[0] = 3;
            })
            .caseElse(i -> {
                branch[0] = 4;
            })
            .get();// this finally execute matched branch,match happens on ever case* method
        Assertions.assertEquals(3, branch[0]);
        //should be break
        branch[0] = -1;
        Case.doMatch(1)
            .caseOf(1, i -> {
                branch[0] = 1;
            })
            .caseOf(i -> i == 1, i -> {
                branch[0] = 2;
            })
            .caseElse(i -> {
                branch[0] = 3;
            })
            .get();
        Assertions.assertEquals(1, branch[0]);
    }
    @Test
    void valueCaseTest() {
        final Integer result = Case.doMatch(1, int.class)
            .caseOf(2, i -> 1)
            .caseOf(i -> i == 2, i -> 2)
            .caseElse(i -> 3)
            .get().orElse(-1);
        Assertions.assertEquals(3, result);
        final Integer result1 = Case.doMatch(1, int.class)
            .caseOf(2, i -> 1)
            .caseOf(i -> i == 2, i -> 2)
            .caseElse(i -> 3)
            .get().orElse(-1);
        Assertions.assertEquals(3, result1);
    }
    @Test
    void classCaseTest(){
        final Integer result = Case.doMatch(new ArrayList<Integer>(), int.class)
//            .caseOf(Collection.class, i -> 4)
            .caseOf(List.class, i -> 3)
            .caseOf(ArrayList::isEmpty, i -> 2)
            .caseElse(i -> 1)
            .get().orElse(-1);
        Assertions.assertEquals(3, result);
    }
}