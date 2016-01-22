/*
 *
 * Copyright 2015-2016 magiclen.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.magiclen.content.database;

import java.util.LinkedList;

/**
 * 條件介面。
 *
 * @author Magic Len
 */
public interface ConditionInterface {

    // -----物件方法-----
    /**
     * 取得條件式。
     *
     * @param rightValues 暫存右邊的值使用的LinkedList
     * @return 傳回條件式字串
     */
    public String toConditionString(final LinkedList<Object> rightValues);
}
