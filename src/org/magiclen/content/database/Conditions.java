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
 * 複合條件物件。
 *
 * @author Magic Len
 */
public class Conditions implements ConditionInterface {

    // -----物件常數-----
    private final LinkedList<ConditionInterface> conditions = new LinkedList();

    // -----物件變數-----
    private LogicType type = LogicType.AND;

    // -----建構子-----
    /**
     * 建構物件實體之後使用setConditions方法來修改條件內容。
     */
    public Conditions() {

    }

    // -----物件方法-----
    /**
     * 設定條件內容。
     *
     * @param logicType 傳入邏輯關係類型
     * @param conditions 傳入條件
     * @return 傳回條件是否設定成功
     */
    public boolean setConditions(final LogicType logicType, final ConditionInterface... conditions) {
	if (conditions == null || conditions.length < 2 || logicType == null) {
	    return false;
	}
	for (final ConditionInterface c : conditions) {
	    this.conditions.add(c);
	}
	type = logicType;
	return true;
    }

    /**
     * 取得(邏輯)關係類型。
     *
     * @return 傳回關係類型
     */
    public LogicType getLogicType() {
	return type;
    }

    /**
     * 取得條件。
     *
     * @return 傳回條件
     */
    public ConditionInterface[] getConditions() {
	final ConditionInterface[] cs = new ConditionInterface[this.conditions.size()];
	this.conditions.toArray(cs);
	return cs;
    }

    /**
     * 取得條件式。
     *
     * @param rightValues 暫存右邊的值使用的LinkedList
     * @return 傳回條件式字串
     */
    @Override
    public String toConditionString(final LinkedList<Object> rightValues) {
	final int l = conditions.size();
	if (l > 0) {
	    final StringBuilder sb = new StringBuilder("(");

	    final String[] conConditions = new String[l];
	    for (int i = 0; i < l; ++i) {
		final ConditionInterface c = conditions.get(i);
		conConditions[i] = c.toConditionString(rightValues);
	    }
	    sb.append(conConditions[0]);
	    final String typeString = type.toString();
	    for (int i = 1; i < l; ++i) {
		sb.append(" ").append(typeString).append(" ").append(conConditions[i]);
	    }
	    sb.append(")");
	    return sb.toString();
	}
	return "";
    }

    /**
     * 取得字串。
     *
     * @return 傳回字串。
     */
    @Override
    public String toString() {
	return toConditionString(new LinkedList<>());
    }
}
