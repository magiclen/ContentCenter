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

import java.util.ArrayList;

/**
 * 約束的描述資料。
 *
 * @author Magic Len
 */
public class ConstraintMetadata {

    // -----物件變數-----
    private ConstraintType constraintType = null;
    private ConflictType conflictType = null;
    private final ArrayList<String> alColumns = new ArrayList<>();

    // -----建構子-----
    /**
     * 建構物件實體之後使用add和setter來修改約束描述資料內容。
     */
    protected ConstraintMetadata() {

    }

    // -----物件方法-----
    /**
     * 設定約束類型。
     *
     * @param type 傳入約束類型
     * @return 傳回是否設定成功
     */
    protected boolean setConstraintType(final ConstraintType type) {
	if (type == null) {
	    return false;
	}
	this.constraintType = type;
	return true;
    }

    /**
     * 加入新的欄位。
     *
     * @param columnName 傳入欄位名稱
     * @return 傳回欄位是否加入成功
     */
    protected boolean addNewColumn(final String columnName) {
	if (columnName == null || columnName.length() == 0 || columnName.contains("`") || alColumns.contains(columnName)) {
	    return false;
	}
	alColumns.add(columnName);
	return true;
    }

    /**
     * 設定衝突處理方式。
     *
     * @param type 傳入衝突處理方式
     * @return 傳回是否設定成功
     */
    protected boolean setConflictType(final ConflictType type) {
	if (type == null) {
	    return false;
	}
	this.conflictType = type;
	return true;
    }

    /**
     * 取得所有欄位的名稱。
     *
     * @return 傳回所有欄位的名稱
     */
    public String[] getColumnNames() {
	final String[] names = new String[alColumns.size()];
	int i = 0;
	for (final String name : alColumns) {
	    names[i++] = name;
	}
	return names;
    }

    /**
     * 取得約束類型。
     *
     * @return 傳回約束類型
     */
    public ConstraintType getConstraintType() {
	return constraintType;
    }

    /**
     * 取得衝突處理方式。
     *
     * @return 傳回衝突處理方式
     */
    public ConflictType getConflictType() {
	return conflictType;
    }
}
