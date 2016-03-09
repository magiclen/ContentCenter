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
 * 排序方式。
 *
 * @author Magic Len
 */
public class Order {

    // -----物件變數-----
    private OrderType type = OrderType.ASC;
    private boolean noCase = false;
    private String columnName = null;

    // -----建構子-----
    /**
     * 建構物件實體之後使用setOrder來修改欄位描述資料內容。
     */
    public Order() {

    }

    // -----物件方法-----
    /**
     * 設定排序方式，預設為遞增。
     *
     * @param columnName 傳入欄位名稱
     * @return 傳回是否設定成功
     */
    public boolean setOrder(final String columnName) {
	return setOrder(columnName, OrderType.ASC);
    }

    /**
     * 設定排序方式。
     *
     * @param columnName 傳入欄位名稱
     * @param orderType 傳入排序方式
     * @return 傳回是否設定成功
     */
    public boolean setOrder(final String columnName, final OrderType orderType) {
	return setOrder(columnName, orderType, false);
    }

    /**
     * 設定排序方式。
     *
     * @param columnName 傳入欄位名稱
     * @param orderType 傳入排序方式
     * @param noCase 傳入是否要忽略大小寫(僅在傳入的欄位為TEXT型態才有用)
     * @return 傳回是否設定成功
     */
    public boolean setOrder(final String columnName, final OrderType orderType, final boolean noCase) {
	if (columnName == null || columnName.trim().length() == 0 || columnName.contains("`") || type == null) {
	    return false;
	}
	this.columnName = columnName;
	this.type = orderType;
	this.noCase = noCase;
	return true;
    }

    /**
     * 判斷是否要忽略大小寫。
     *
     * @return 傳回是否要忽略大小寫
     */
    public boolean isNoCase() {
	return false;
    }

    /**
     * 取得欄位名稱。
     *
     * @return 傳回欄位名稱
     */
    public String getColumnName() {
	return columnName;
    }

    /**
     * 取得排序方式。
     *
     * @return 傳回排序方法
     */
    public OrderType getOrderType() {
	return type;
    }

    /**
     * 取得排序。
     *
     * @return 傳回排序字串
     */
    public String toOrderString() {
	final StringBuilder sb = new StringBuilder();
	sb.append("`").append(columnName).append("`");
	if (noCase) {
	    sb.append(" COLLATE NOCASE");
	}
	sb.append(" ").append(type.toString());
	return sb.toString();
    }

    /**
     * 取得字串。
     *
     * @return 傳回字串。
     */
    @Override
    public String toString() {
	return toOrderString();
    }
}
