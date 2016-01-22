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
 * 表單欄位的描述資料(Metadata, Schema)。
 *
 * @author Magic Len
 */
public class ColumnMetadata {

    // -----物件變數-----
    private boolean canNull = true;
    private ColumnType type = null;
    private boolean hide = false;

    // -----建構子-----
    /**
     * 建構物件實體之後使用setter來修改欄位描述資料內容。
     */
    protected ColumnMetadata() {

    }

    // -----物件方法-----
    /**
     * 設定欄位內容可否是null。
     *
     * @param canNull 傳入欄位內容可否是null。
     * @return 傳回設定是否成功
     */
    protected boolean canNull(final boolean canNull) {
	this.canNull = canNull;
	return true;
    }

    /**
     * 設定欄位內容是否隱藏。
     *
     * @param hide 傳入欄位內容是否隱藏。
     * @return 傳回是否設定成功
     */
    protected boolean setHide(final boolean hide) {
	this.hide = hide;
	return true;
    }

    /**
     * 設定欄位的資料型態。
     *
     * @param type 傳入欄位的資料型態
     * @return 傳回是否設定成功
     */
    protected boolean setColumnType(final ColumnType type) {
	if (type == null) {
	    return false;
	}
	this.type = type;
	return true;
    }

    /**
     * 取得欄位是否可以為null。
     *
     * @return 傳回欄位是否可以為null
     */
    public boolean canNull() {
	return canNull;
    }

    /**
     * 檢查欄位內容是否是隱藏的。
     *
     * @return 傳回欄位內容是否是隱藏的
     */
    public boolean isHide() {
	return hide;
    }

    /**
     * 取得欄位內容的資料型態。
     *
     * @return 傳回欄位內容的資料型態。
     */
    public ColumnType getColumnType() {
	return type;
    }
}
