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

/**
 * <p>
 * 條件比較的類型。
 * </p>
 *
 * <ol>
 * <li>
 * BIGGER_THAN：大於。
 * </li>
 * <li>
 * BIGGER_THAN：大於。
 * </li>
 * <li>
 * SMALLER_THAN：小於。
 * </li>
 * <li>
 * EQUAL_OR_BIGGER_THAN：大於等於。
 * </li>
 * <li>
 * EQUAL_OR_SMALLER_THAN：小於等於。
 * </li>
 * <li>
 * EQUAL：等於。
 * </li>
 * <li>
 * NOT_EQUAL：不等於。
 * </li>
 * <li>
 * LIKE：相似於。
 * </li>
 * </ol>
 *
 * @author Magic Len
 */
public enum ConditionType {

    BIGGER_THAN, SMALLER_THAN, LIKE, EQUAL, NOT_EQUAL, EQUAL_OR_BIGGER_THAN, EQUAL_OR_SMALLER_THAN;
}
