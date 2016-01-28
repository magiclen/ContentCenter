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
import java.util.HashMap;
import java.util.Set;

/**
 * 表單的描述資料(Metadata, Schema)。
 *
 * @author Magic Len
 */
public class TableMetadata {

    // -----物件常數-----
    private final HashMap<String, ColumnMetadata> hmColumns = new HashMap<>();
    private final ArrayList<ConstraintMetadata> alConstraints = new ArrayList<>();

    // -----物件變數-----
    private String primaryKey = null;

    // -----建構子-----
    /**
     * 建構物件實體之後使用add和setter方法來修改表單內容。
     */
    public TableMetadata() {
    }

    // -----物件方法-----
    /**
     * 判斷是否為可用狀態。
     *
     * @return 傳回是否為可用狀態
     */
    public boolean isAvailable() {
	return hmColumns.size() >= 1;
    }

    /**
     * 新增新的欄位。
     *
     * @param columnName 傳入欄位名稱
     * @param type 傳入欄位資料類型
     * @param canNull 傳入欄位內容是否可以為null
     * @return 傳回欄位是否新增成功
     */
    public boolean addNewColumn(final String columnName, final ColumnType type, final boolean canNull) {
	return addNewColumn(columnName, type, false, canNull);
    }

    /**
     * 新增新的欄位。
     *
     * @param columnName 傳入欄位名稱
     * @param type 傳入欄位資料類型
     * @param hide 傳入欄位內容是否隱藏，隱藏的欄位無法做為關鍵欄位或是被約束
     * @param canNull 傳入欄位內容是否可以為null
     * @return 傳回欄位是否新增成功
     */
    public boolean addNewColumn(final String columnName, final ColumnType type, final boolean hide, final boolean canNull) {
	if (columnName == null || columnName.trim().length() == 0 || type == null || (type != ColumnType.TEXT && hide)) {
	    return false;
	} else if (columnName.contains("`")) {
	    return false;
	}
	if (hmColumns.containsKey(columnName)) {
	    return false;
	}
	final ColumnMetadata column = new ColumnMetadata();
	column.canNull(canNull);
	column.setColumnType(type);
	column.setHide(hide);
	hmColumns.put(columnName, column);
	return true;
    }

    /**
     * 加入約束。
     *
     * @param constraintType 傳入約束類型
     * @param conflictType 傳入衝突處理方式
     * @param columnName 傳入欄位名稱
     * @return 傳回約束是否新增成功
     */
    public boolean addConstraint(final ConstraintType constraintType, final ConflictType conflictType, final String... columnName) {
	if (constraintType == null || conflictType == null || columnName == null || columnName.length == 0) {
	    return false;
	}
	final ConstraintMetadata constraint = new ConstraintMetadata();
	constraint.setConflictType(conflictType);
	constraint.setConstraintType(constraintType);
	for (int i = 0; i < columnName.length; i++) {
	    final String name = columnName[i];
	    if (name.length() == 0 || name.contains("`") || !hmColumns.containsKey(name) || hmColumns.get(name).isHide()) {
		return false;
	    }
	    constraint.addNewColumn(name);
	}
	alConstraints.add(constraint);
	return true;
    }

    /**
     * <p>
     * 設定主要的關鍵欄位。
     * </p>
     *
     * <p>
     * 若是使用INTEGER型態的欄位作為主要的關鍵欄位，則自動會加上AUTOINCREMENT屬性，使其可以自動遞增。
     * </p>
     *
     * @param columnName 傳入欄位名稱
     * @return 傳回是否設定成功
     */
    public boolean setPrimaryKey(final String columnName) {
	if (columnName == null || columnName.trim().length() == 0) {
	    this.primaryKey = null;
	    return true;
	} else if (columnName.contains("`")) {
	    return false;
	}
	final ColumnMetadata column = hmColumns.get(columnName);
	if (column == null || column.isHide()) {
	    return false;
	}
	this.primaryKey = columnName;
	return true;
    }

    /**
     * 取得主要的關鍵欄位名稱。
     *
     * @return 傳回主要的關鍵欄位名稱
     */
    public String getPrimaryKey() {
	return primaryKey;
    }

    /**
     * 取的所有欄位名稱。
     *
     * @return 傳回所有欄位名稱
     */
    public String[] getColumnNames() {
	final Set<String> keySet = hmColumns.keySet();
	final String[] keys = new String[keySet.size()];
	int i = 0;
	for (final String key : keySet) {
	    keys[i++] = key;
	}
	return keys;
    }

    /**
     * 取得所有約束的描述資料。
     *
     * @return 傳回所有約束的描述資料
     */
    public ConstraintMetadata[] getConstraints() {
	final ConstraintMetadata[] constraints = new ConstraintMetadata[alConstraints.size()];
	int i = 0;
	for (ConstraintMetadata constraint : alConstraints) {
	    constraints[i++] = constraint;
	}
	return constraints;
    }

    /**
     * 取得指定的欄位描述資料。
     *
     * @param columnName 傳入欄位名稱
     * @return 傳回指定的欄位描述資料
     */
    public ColumnMetadata getColumnMetadata(final String columnName) {
	return hmColumns.get(columnName);
    }
}
