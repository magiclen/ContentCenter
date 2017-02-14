/*
 *
 * Copyright 2015-2017 magiclen.org
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
 * 單一條件物件。
 *
 * @author Magic Len
 */
public class Condition implements ConditionInterface {

    // -----物件變數-----
    private ConditionType type = ConditionType.EQUAL;
    private Object l_value = "1";
    private boolean isColumnLeft;
    private Object r_value = 1;
    private boolean fuzzy = false;

    // -----建構子-----
    /**
     * 建構物件實體之後使用setCondition方法來修改條件內容。
     */
    public Condition() {

    }

    // -----物件方法-----
    /**
     * 設定條件內容。
     *
     * @param leftValue 傳入要檢查的欄位名稱
     * @param rightValue 傳入要比較的值
     * @param conditionType 傳入比較的類型
     * @return 傳回條件是否設定成功
     */
    public boolean setCondition(final Object leftValue, final Object rightValue, final ConditionType conditionType) {
	return setCondition(leftValue, true, rightValue, conditionType);
    }

    /**
     * 設定條件內容。
     *
     * @param leftValue 傳入要檢查的欄位名稱或是要比較的值
     * @param isColumnLeft 傳入是否為欄位名稱
     * @param rightValue 傳入要比較的值
     * @param conditionType 傳入比較的類型
     * @return 傳回條件是否設定成功
     */
    public boolean setCondition(final Object leftValue, final boolean isColumnLeft, final Object rightValue, final ConditionType conditionType) {
	return setCondition(leftValue, isColumnLeft, rightValue, conditionType, false);
    }

    /**
     * 設定條件內容。
     *
     * @param leftValue 傳入要檢查的欄位名稱或是要比較的值
     * @param isColumnLeft 傳入是否為欄位名稱
     * @param rightValue 傳入要比較的值
     * @param conditionType 傳入比較的類型
     * @param fuzzy 傳入是否模糊判斷(只有TEXT能用)
     * @return 傳回條件是否設定成功
     */
    public boolean setCondition(final Object leftValue, final boolean isColumnLeft, final Object rightValue, final ConditionType conditionType, final boolean fuzzy) {
	if (conditionType == null || (conditionType == ConditionType.LIKE && rightValue instanceof Number) || (fuzzy && conditionType != ConditionType.LIKE) || (isColumnLeft && (leftValue == null || leftValue.toString().contains("`"))) || (rightValue == null && conditionType != ConditionType.EQUAL && conditionType != ConditionType.NOT_EQUAL)) {
	    return false;
	}
	l_value = leftValue;
	r_value = rightValue;
	this.type = conditionType;
	this.fuzzy = fuzzy;
	this.isColumnLeft = isColumnLeft;
	return true;
    }

    /**
     * 取得要檢查的欄位名稱。
     *
     * @return 傳回要檢查的欄位名稱
     */
    public Object getLeftValue() {
	return l_value;
    }

    /**
     * 取得要比較的值。
     *
     * @return 傳回要比較的值
     */
    public Object getRightValue() {
	return r_value;
    }

    /**
     * 取得條件比較類型。
     *
     * @return 傳回條件比較類型。
     */
    public ConditionType getConditionType() {
	return type;
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

    /**
     * 取得條件式。
     *
     * @param rightValues 暫存右邊的值使用的LinkedList
     * @return 傳回條件式字串
     */
    @Override
    public String toConditionString(final LinkedList<Object> rightValues) {
	final StringBuilder sb = new StringBuilder();
	if (l_value == null) {
	    sb.append("NULL");
	} else if (isColumnLeft) {
	    sb.append("`").append(l_value).append("`");
	} else {
	    sb.append("?");
	    rightValues.add(l_value);
	}

	if (r_value == null) {
	    sb.append("IS ");
	    switch (type) {
		case NOT_EQUAL:
		    sb.append("NOT ");
		    break;
	    }
	    sb.append("NULL");
	} else {
	    switch (type) {
		case BIGGER_THAN:
		    sb.append(" > ");
		    break;
		case EQUAL:
		    sb.append(" = ");
		    break;
		case NOT_EQUAL:
		    sb.append(" <> ");
		    break;
		case EQUAL_OR_BIGGER_THAN:
		    sb.append(" >= ");
		    break;
		case EQUAL_OR_SMALLER_THAN:
		    sb.append(" <= ");
		    break;
		case LIKE:
		    sb.append(" LIKE ");
		    break;
		case SMALLER_THAN:
		    sb.append(" < ");
		    break;
	    }

	    sb.append("?");
	    if (fuzzy) {
		rightValues.add("%".concat(r_value.toString()).concat("%"));
	    } else {
		rightValues.add(r_value);
	    }
	}
	return sb.toString();
    }
}
