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
package org.magiclen.content.center;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.magiclen.content.center.listener.ContentCenterListener;
import org.magiclen.content.center.listener.ContentCenterListener.EVENT;
import org.magiclen.content.center.listener.ContentCenterListener.NOTIFY;
import org.magiclen.content.database.ColumnMetadata;
import org.magiclen.content.database.ColumnType;
import org.magiclen.content.database.Condition;
import org.magiclen.content.database.ConditionInterface;
import org.magiclen.content.database.ConditionType;
import org.magiclen.content.database.ConflictType;
import org.magiclen.content.database.ConstraintMetadata;
import org.magiclen.content.database.ConstraintType;
import org.magiclen.content.database.Order;
import org.magiclen.content.database.OrderType;
import org.magiclen.content.database.TableMetadata;
import org.magiclen.content.observer.Observer;
import org.magiclen.content.sync.Sync;
import org.magiclen.json.JSONArray;
import org.magiclen.json.JSONException;
import org.magiclen.json.JSONObject;
import org.magiclen.magicstringhider.StringHider;

/**
 * ContentCenter，內容中心，整合資料庫(Database)、觀察者(Observer)和線上同步(Sync)的功能，以最輕量的方式，完全不用任何的SQL語法，來管控整個程式所用到的資料(Data)。
 *
 * @author Magic Len
 * @see SyncCenter
 * @see Printer
 * @see ObserverCenter
 * @see ContentCenterListener
 */
public class ContentCenter {

    // -----類別變數-----
    /**
     * 是否要印出ContentCenter的錯誤訊息。
     */
    public static boolean printError = true;

    /**
     * 是否要印出ContentCenter自動執行的SQL語法。
     */
    public static boolean printStatement = false;

    // -----類別常數-----
    /**
     * 作業系統。需要在編譯前更改這個常數的值。
     */
    private static final OperatingSystems OS = OperatingSystems.PC;

    /**
     * 資料庫的預設編碼。
     */
    private static final String DB_DEFAULT_ENCODING = "UTF-8";

    /**
     * Android上使用的JDBC驅動路徑。
     */
    private static final String DB_DRIVER_ANDROID = "org.sqldroid.SQLDroidDriver";

    /**
     * PC上使用的JDBC驅動路徑。
     */
    private static final String DB_DRIVER_PC = "org.sqlite.JDBC";

    /**
     * Android上，資料庫檔案URL的前綴。
     */
    private static final String DB_URL_ANDROID_PREFIX = "jdbc:sqldroid";

    /**
     * PC上，資料庫檔案URL的前綴。
     */
    private static final String DB_URL_PC_PREFIX = "jdbc:sqlite";

    /**
     * 隱藏欄位的表單名稱。
     */
    private static final String DB_HIDE_COLUMN = "magiclen_cc_hide";

    /**
     * 隱藏欄位的表單名稱。
     */
    private static final String DB_HIDE_COLUMN_TABLE = "t_name";

    /**
     * 隱藏欄位的欄位名稱。
     */
    private static final String DB_HIDE_COLUMN_COLUMN = "c_name";

    /**
     * 同步的表單名稱。
     */
    private static final String DB_SYNC = "magiclen_cc_sync";

    /**
     * 同步的ID名稱。
     */
    private static final String DB_SYNC_DB_ID = "_id";

    /**
     * 同步的內容名稱。
     */
    private static final String DB_SYNC_TEXT = "sync_text";

    /**
     * 設定的表單名稱。
     */
    private static final String DB_SETTING = "magiclen_cc_settings";

    /**
     * 設定的Key名稱。
     */
    private static final String DB_SETTING_KEY = "k";

    /**
     * 設定的Value名稱。
     */
    private static final String DB_SETTING_VALUE = "v";

    /**
     * 儲存需要隱藏資料的欄位。
     */
    private static final HashSet<String> hsHideColumn = new HashSet<>();

    /**
     * 預設的計時器間隔時間。
     */
    private static final long DEFAULT_PERIOD = 600;

    /**
     * 自動同步的專用執行緒。
     */
    private static final Runnable syncThread = new Runnable() {
        /**
         * 自動同步時要執行的程式。
         */
        @Override
        public void run() {
            while (available) {
                try {
                    final long nowTime = System.currentTimeMillis();
                    if (run && nowTime - last_time >= period) {
                        last_time = nowTime;
                        Printer.print("periodic sync");

                        //Local端資料同步到Server
                        final Order order = new Order();
                        order.setOrder(DB_SYNC_DB_ID, OrderType.ASC);
                        final JSONArray localSyncArray = query(DB_SYNC, order);
                        if (localSyncArray == null) {
                            Printer.err("syncThread: cannot sync local to server");
                        } else {
                            final int localSyncArrayLength = localSyncArray.length();
                            if (localSyncArrayLength > 0) {
                                for (int i = 0; i < localSyncArrayLength; ++i) {
                                    final JSONObject jsonSyncTuple = localSyncArray.getJSONObject(i);
                                    final JSONObject jsonSync = new JSONObject(jsonSyncTuple.getString(DB_SYNC_TEXT));
                                    EVENT event = null;
                                    String type = jsonSync.getString(JSONStringInterface.JSON_TYPE);
                                    switch (type) {
                                        case JSONStringInterface.TYPE_INSERT:
                                            event = EVENT.INSERT;
                                            break;
                                        case JSONStringInterface.TYPE_UPDATE:
                                            event = EVENT.UPDATE;
                                            break;
                                        case JSONStringInterface.TYPE_DELETE:
                                            event = EVENT.DELETE;
                                            break;
                                    }
                                    if (callBack(event, NOTIFY.SYNC, jsonSync)) { //同步成功
                                        final long id = jsonSyncTuple.getLong(DB_SYNC_DB_ID);
                                        final Condition c = new Condition();
                                        c.setCondition(DB_SYNC_DB_ID, id, ConditionType.EQUAL);
                                        delete(DB_SYNC, c); //刪除同步成功的暫存資料
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }

                        //Server端資料同步到Client
                        callBack(EVENT.REMOTESYNC, NOTIFY.SYNC, null);
                    }
                } catch (final JSONException ex) {
                    Printer.err("syncThread: periodic sync ran exception, ".concat(ex.getMessage()));
                }
                try {
                    Thread.sleep(DEFAULT_PERIOD); //睡覺
                } catch (final InterruptedException ex) {
                    Printer.err("syncThread: periodic sync cannot sleep for delay, ".concat(ex.getMessage()));
                }
            }
        }
    };

    // -----類別介面-----
    /**
     * ContentCenterListener的JSON通用欄位。
     */
    private static interface JSONStringInterface {

        /**
         * TYPE欄位為此筆Json資料所表示的更新類型，有插入(INSERT)、更新(UPDATE)、刪除(DELETE)這幾種，內容型態為String。
         */
        public static final String JSON_TYPE = "type";

        /**
         * ID欄位可用來辨識此筆Json資料的用途，內容型態為long。
         */
        public static final String JSON_ID = "id";

        /**
         * TYPE欄位，插入(INSERT)。
         */
        public static final String TYPE_INSERT = "insert";

        /**
         * TYPE欄位，更新(UPDATE)。
         */
        public static final String TYPE_UPDATE = "update";

        /**
         * TYPE欄位，刪除(DELETE)。
         */
        public static final String TYPE_DELETE = "delete";

        /**
         * TABLE NAME欄位用來表示此筆Json資料所影響的表單。
         */
        public static final String JSON_TABLE_NAME = "table_name";
    }

    /**
     * ContentCenterListener的JSON插入(INSERT)事件欄位。
     */
    public static interface JSONInsert extends JSONStringInterface {

        /**
         * INSERT CONTENT欄位為插入的內容。
         */
        public static final String JSON_INSERT_CONTENT = "content";
    }

    /**
     * ContentCenterListener的JSON更新(UPDATE)事件欄位。
     */
    public static interface JSONUpdate extends JSONStringInterface {

        /**
         * UPDATE CONTENT欄位為更新的內容。
         */
        public static final String JSON_UPDATE_CONTENT = "content";

        /**
         * UPDATE CONDITION欄位為更新的條件。
         */
        public static final String JSON_UPDATE_CONDITION = "condition";
    }

    /**
     * ContentCenterListener的JSON刪除(DELETE)事件欄位。
     */
    public static interface JSONDelete extends JSONStringInterface {

        /**
         * DELETE CONDITION欄位為刪除的條件。
         */
        public static final String JSON_DELETE_CONDITION = "condition";
    }

    // -----類別列舉-----
    /**
     * 作業系統，分為PC和ANDROID，PC可以享用ContentCenter的所有功能，ANDROID只能使用AutoCommit來操作資料庫並無法過濾掉無效的更新。
     */
    private static enum OperatingSystems {

        PC, ANDROID;
    }

    // -----類別變數-----
    /**
     * 儲存ContentCenter是否可用。
     */
    private static boolean available = false;

    /**
     * 儲存DB檔案存放的位置。
     */
    private static File dbFile = null;

    /**
     * 儲存DB檔案存放的位置。
     */
    private static File dbFileJournal = null;

    /**
     * 隱藏欄位資料時使用的密鑰。
     */
    private static String hideKey = "";

    /**
     * 是否使用autoCommit。
     */
    private static boolean autoCommit = true;

    /**
     * 儲存ContentCenter的監聽者。
     */
    private static ContentCenterListener listener = null;

    /**
     * 儲存資料庫連結。
     */
    private static Connection conn;

    /**
     * 使用者定義的同步間隔時間。
     */
    private static long period = DEFAULT_PERIOD;

    /**
     * 上一次同步的時間。
     */
    private static long last_time = System.currentTimeMillis();

    /**
     * 是否正在使用自動同步功能。
     */
    private static boolean run = false;

    // -----類別方法-----
    /**
     * 設定Printer。
     *
     * @param printer 傳入要使用的printer
     * @return 傳回是否設定成功
     */
    public static boolean setPrinter(final Printer printer) {
        return Printer.setPrinter(printer);
    }

    /**
     * 使用預設的Printer，訊息會使用標準輸出串流來輸出。
     */
    public static void useDefaultPrinter() {
        Printer.useDefaultPrinter();
    }

    /**
     * 檢查是否正在執行自動同步。
     *
     * @return 傳回是否正在執行自動同步
     */
    public static boolean isSyncPeriodRunning() {
        return run;
    }

    /**
     * 取得自動同步的週期。
     *
     * @return 傳回自動同步的週期(毫秒)
     */
    public static long getSyncPeriod() {
        return period;
    }

    /**
     * 設定自動同步的週期。
     *
     * @param period 傳入要自動同步的週期(毫秒)，必須要大於等於1
     * @return 傳回是否設定成功
     */
    public static boolean setSyncPeriod(final long period) {
        if (!available) {
            Printer.err("setSyncPeriod: not available");
            return false;
        }
        if (period < 1) {
            Printer.err("setSyncPeriod: period cannot be less than 1");
            return false;
        }
        ContentCenter.period = period;
        return true;
    }

    /**
     * 啟動自動同步。
     *
     * @return 傳回是否呼叫成功
     */
    public static boolean runPeriodicSync() {
        if (run) {
            Printer.err("runPeriodicSync: periodic sync is running");
            return false;

        }
        run = true;
        return true;
    }

    /**
     * 停止自動同步。
     *
     * @return 傳回是否呼叫成功
     */
    public static boolean stopPeriodicSync() {
        if (!run) {
            Printer.err("runPeriodicSync: periodic sync is not running");
            return false;
        }
        run = false;
        return true;
    }

    /**
     * 檢查ContentCenter是否可用。
     *
     * @return 傳回ContentCenter是否可用
     */
    public static boolean isAvailable() {
        return available;
    }

    /**
     * 取得DB檔案。
     *
     * @return 傳回DB檔案
     */
    public static File getDBFile() {
        return dbFile;
    }

    /**
     * 取得隱藏欄位使用的密鑰。
     *
     * @return 傳回密鑰
     */
    public static String getHideKey() {
        return hideKey;
    }

    /**
     * 取得ContentCenter的監聽物件。
     *
     * @return 傳回ContentCenter的監聽物件。
     */
    public static ContentCenterListener getContentCenterListener() {
        return listener;
    }

    /**
     * 註冊同步物件。
     *
     * @param syncID 傳入同步物件要使用的ID
     * @param sync 傳入同步物件
     * @return 傳回同步物件是否註冊成功
     */
    public static boolean registerSync(final String syncID, final Sync sync) {
        return registerSync(syncID, sync, false);
    }

    /**
     * 註冊同步物件。
     *
     * @param syncID 傳入同步物件要使用的ID
     * @param sync 傳入同步物件
     * @param overlap 覆蓋之前註冊的物件
     * @return 傳回同步物件是否註冊成功
     */
    public static boolean registerSync(final String syncID, final Sync sync, final boolean overlap) {
        if (!available) {
            Printer.err("registerSync: not available");
            return false;
        }
        return SyncCenter.registerSync(syncID, sync, overlap);
    }

    /**
     * 通知同步物件將資料更新到伺服器。
     *
     * @param syncID 傳入同步物件的ID
     * @param json 傳入要更新的相關資料
     * @return 傳回是否更新成功
     */
    public static boolean notifySyncToServer(final String syncID, final JSONObject json) {
        if (!available) {
            Printer.err("notifySyncToServer: not available");
            return false;
        }
        try {
            return SyncCenter.notifySyncToServer(syncID, json);
        } catch (final Exception ex) {
            Printer.err("notifySyncToServer: exception, ".concat(ex.getMessage()));
            return false;
        }
    }

    /**
     * 註冊觀察者物件。
     *
     * @param observerID 傳入觀察者物件要使用的ID
     * @param observer 傳入觀察者物件
     * @return 傳回觀察者物件是否註冊成功
     */
    public static boolean registerObserver(final String observerID, final Observer observer) {
        return registerObserver(observerID, observer, false);
    }

    /**
     * 註冊觀察者物件。
     *
     * @param observerID 傳入觀察者物件要使用的ID
     * @param observer 傳入觀察者物件
     * @param overlap 覆蓋之前註冊的物件
     * @return 傳回觀察者物件是否註冊成功
     */
    public static boolean registerObserver(final String observerID, final Observer observer, final boolean overlap) {
        if (!available) {
            Printer.err("registerObserver: not available");
            return false;
        }
        return ObserverCenter.registerObserver(observerID, observer, overlap);
    }

    /**
     * 通知觀察者物件將資料更新到元件上，使其顯示出來。
     *
     * @param observerID 傳入觀察者物件的ID
     * @return 傳回是否更新成功
     */
    public static boolean notifyObserver(final String observerID) {
        if (!available) {
            Printer.err("notifyObserver: not available");
            return false;
        }
        try {
            return ObserverCenter.notifyObserver(observerID);
        } catch (final Exception ex) {
            Printer.err("notifyObserver: exception, ".concat(ex.getMessage()));
            return false;
        }
    }

    /**
     * 檢查ContentCenter是否使用AutoCommit。
     *
     * @return 傳回ContentCenter是否使用AutoCommit。
     */
    public static boolean isAutoCommit() {
        return autoCommit;
    }

    /**
     * 初始化ContentCenter，在監聽物件的INITIAL事件中可以建立資料庫一開始的表單，AVAILABLE事件中可以檢查資料庫的狀態是否正確。
     *
     * @param dbFile 傳入DB檔案路徑
     * @param listener 傳入ContentCenter監聽物件
     * @param hideKey 傳入隱藏欄位時用的密鑰
     * @return 傳回ContentCenter使否初始化成功。
     */
    public static boolean initialContentCenter(final String dbFile, final ContentCenterListener listener, final String hideKey) {
        return initialContentCenter(new File(dbFile), listener, hideKey);
    }

    /**
     * 初始化ContentCenter，在監聽物件的INITIAL事件中可以建立資料庫一開始的表單，AVAILABLE事件中可以檢查資料庫的狀態是否正確。
     *
     * @param dbFile 傳入DB檔案路徑
     * @param listener 傳入ContentCenter監聽物件
     * @return 傳回ContentCenter使否初始化成功。
     */
    public static boolean initialContentCenter(final String dbFile, final ContentCenterListener listener) {
        return initialContentCenter(dbFile, listener, hideKey);
    }

    /**
     * 初始化ContentCenter，在監聽物件的INITIAL事件中可以建立資料庫一開始的表單，AVAILABLE事件中可以檢查資料庫的狀態是否正確。
     *
     * @param dbFile 傳入DB檔案
     * @param listener 傳入ContentCenter監聽物件
     * @return 傳回ContentCenter使否初始化成功。
     */
    public static boolean initialContentCenter(final File dbFile, final ContentCenterListener listener) {
        return initialContentCenter(dbFile, listener, hideKey);
    }

    /**
     * 初始化ContentCenter，在監聽物件的INITIAL事件中可以建立資料庫一開始的表單，AVAILABLE事件中可以檢查資料庫的狀態是否正確。
     *
     * @param dbFile 傳入DB檔案
     * @param listener 傳入ContentCenter監聽物件
     * @param hideKey 傳入隱藏欄位時用的密鑰
     * @return 傳回ContentCenter使否初始化成功。
     */
    public static boolean initialContentCenter(final File dbFile, final ContentCenterListener listener, final String hideKey) {
        if (available) {
            Printer.err("initialContentCenter: already available");
            return false;
        }
        available = true;
        boolean newTable = false;
        if (dbFile == null || listener == null || hideKey == null) {
            available = false;
            Printer.err("initialContentCenter: input can't be null");
            return false;
        } else if (!dbFile.isFile() || !dbFile.exists()) {
            try {
                dbFile.createNewFile();
                newTable = true;
            } catch (final IOException ex) {
                available = false;
                Printer.err("initialContentCenter: can't create new database file, ".concat(ex.getMessage()));
                return false;
            }
        } else if (!dbFile.canWrite()) {
            available = false;
            Printer.err("initialContentCenter: can't read or write database file");
            return false;
        }
        ContentCenter.listener = listener;
        ContentCenter.dbFile = dbFile;
        ContentCenter.dbFileJournal = new File(dbFile.getAbsolutePath().concat("-journal"));
        ContentCenter.hideKey = hideKey;
        // 建立或是開啟資料庫
        try {
            final String db = getDBURL(dbFile);
            final String driver = getDBDriver();
            Class.forName(driver);
            conn = DriverManager.getConnection(db);
            conn.setAutoCommit(OS == OperatingSystems.ANDROID || autoCommit);
            try (PreparedStatement stat = conn.prepareStatement("PRAGMA encoding =\"" + DB_DEFAULT_ENCODING + "\"")) {
                stat.execute();
            }
            if (callBack(EVENT.INITIAL_PRAGMA, null, new JSONObject("{\"auto_vacuum\":1}"))) {
                try (PreparedStatement stat = conn.prepareStatement("PRAGMA auto_vacuum = 1")) {
                    stat.execute();
                }
            }
            //檢查資料庫檔案順便插入必要表單
            if (newTable) {
                final TableMetadata table_hide = new TableMetadata();
                table_hide.addNewColumn(DB_HIDE_COLUMN_TABLE, ColumnType.TEXT, false);
                table_hide.addNewColumn(DB_HIDE_COLUMN_COLUMN, ColumnType.TEXT, false);
                table_hide.addConstraint(ConstraintType.UNIQUE, ConflictType.IGNORE, DB_HIDE_COLUMN_TABLE, DB_HIDE_COLUMN_COLUMN);

                final TableMetadata table_sync = new TableMetadata();
                table_sync.addNewColumn(DB_SYNC_DB_ID, ColumnType.INTEGER, false, false);
                table_sync.addNewColumn(DB_SYNC_TEXT, ColumnType.TEXT, true, false);
                table_sync.setPrimaryKey(DB_SYNC_DB_ID);

                final TableMetadata table_setting = new TableMetadata();
                table_setting.addNewColumn(DB_SETTING_KEY, ColumnType.TEXT, false);
                table_setting.addNewColumn(DB_SETTING_VALUE, ColumnType.TEXT, true, false);
                table_setting.addConstraint(ConstraintType.UNIQUE, ConflictType.REPLACE, DB_SETTING_KEY);
                if (!(ContentCenter.createTable(DB_HIDE_COLUMN, table_hide) && ContentCenter.createTable(DB_SYNC, table_sync) && ContentCenter.createTable(DB_SETTING, table_setting))) {
                    throw new Exception("construct fail!");
                }

                if (!callBack(EVENT.INITIAL, null, null)) {
                    throw new Exception("listener got a false return when initial");
                }
            } else {
                final JSONArray array = query(DB_HIDE_COLUMN);
                if (array == null) {
                    throw new Exception("db is crash");
                }
                final int l = array.length();
                for (int i = 0; i < l; ++i) {
                    final JSONObject json = array.getJSONObject(i);
                    hsHideColumn.add(getAbsolutePath(json.getString(DB_HIDE_COLUMN_TABLE), json.getString(DB_HIDE_COLUMN_COLUMN)));
                }
            }
            if (!callBack(EVENT.AVAILABLE, null, null)) {
                throw new Exception("listener got a false return when available");
            }
        } catch (final Exception ex) {
            available = false;
            try {
                conn.close();
            } catch (final Exception exx) {

            }
            Printer.err("initialContentCenter: can't create database, ".concat(ex.getMessage()));
            try {
                final JSONObject exceptionObject = new JSONObject();
                exceptionObject.put("message", ex.getMessage());
                if (!callBack(EVENT.INITIAL_FAIL, null, exceptionObject)) {
                    dbFile.delete();
                    ContentCenter.dbFileJournal.delete();
                }
            } catch (final Exception exxx) {

            }
            return false;
        }
        new Thread(syncThread).start();
        return true;
    }

    /**
     * 將新的欄位插入現有的表單，新的欄位無法設為Key，且必定canNull，此方法為進階用法，不建議直接使用。
     *
     * @param tableName 傳入表單名稱
     * @param metadata 傳入表單的描述資料(Metadata, Schema)
     * @return 傳回表單是否更新
     */
    public static boolean alterTableAddColumn(final String tableName, final TableMetadata metadata) {
        if (!available) {
            Printer.err("alterTableAddColumn: not available");
            return false;
        }
        if (tableName == null || tableName.trim().length() == 0 || metadata == null) {
            Printer.err("alterTableAddColumn: tableName or metadata is empty");
            return false;
        } else if (!metadata.isAvailable()) {
            Printer.err("alterTableAddColumn: metadata is not available");
            return false;
        } else if (tableName.contains("`")) {
            Printer.err("alterTableAddColumn: tableName has illegal character");
            return false;
        }
        try {
            final StringBuilder sb = new StringBuilder("");
            final String[] columnNames = metadata.getColumnNames();
            for (final String columnName : columnNames) {
                final ColumnMetadata cm = metadata.getColumnMetadata(columnName);
                sb.append("ALTER TABLE `").append(tableName).append("` ADD `").append(columnName).append("` ").append(cm.getColumnType().toString()).append(";");
            }
            final String statement = sb.toString();
            Printer.print("alterTableAddColumn: ".concat(statement));
            final PreparedStatement stat = conn.prepareStatement(statement);
            executeUpdate(stat);
            return true;
        } catch (final Exception ex) {
            Printer.err("alterTableAddColumn: cannot alter table, ".concat(ex.getMessage()));
            return false;
        }
    }

    /**
     * 建立新的資料庫表單。
     *
     * @param tableName 傳入表單名稱
     * @param metadata 傳入表單的描述資料(Metadata, Schema)
     * @return 傳回表單是否建立成功
     */
    public static boolean createTable(final String tableName, final TableMetadata metadata) {
        if (!available) {
            Printer.err("createTable: not available");
            return false;
        }
        if (tableName == null || tableName.trim().length() == 0 || metadata == null) {
            Printer.err("createTable: tableName or metadata is empty");
            return false;
        } else if (!metadata.isAvailable()) {
            Printer.err("createTable: metadata is not available");
            return false;
        } else if (tableName.contains("`")) {
            Printer.err("createTable: tableName has illegal character");
            return false;
        }
        try {
            final StringBuilder sb = new StringBuilder("CREATE TABLE `");
            sb.append(tableName).append("` (");
            final String[] columnNames = metadata.getColumnNames();
            final String primary = metadata.getPrimaryKey();
            final int l = columnNames.length - 1;
            for (int i = 0; i <= l; ++i) {
                final String columnName = columnNames[i];
                final ColumnMetadata column = metadata.getColumnMetadata(columnName);
                sb.append("`").append(columnName).append("` ").append(column.getColumnType().toString());
                if (!column.canNull()) {
                    sb.append(" NOT NULL");
                }
                if (primary != null) {
                    if (columnName.equals(primary)) {
                        sb.append(" PRIMARY KEY");
                        if (column.getColumnType() == ColumnType.INTEGER) {
                            sb.append(" AUTOINCREMENT");
                        }
                    }
                }
                if (i != l) {
                    sb.append(",");
                }

                //隱藏欄位
                if (column.isHide()) {
                    final JSONObject json = new JSONObject();
                    json.put(DB_HIDE_COLUMN_TABLE, tableName);
                    json.put(DB_HIDE_COLUMN_COLUMN, columnName);
                    insert(DB_HIDE_COLUMN, json);
                    hsHideColumn.add(getAbsolutePath(tableName, columnName));
                }
            }

            final ConstraintMetadata[] constraints = metadata.getConstraints();
            final int constraintsLength = constraints.length;
            for (int i = 0; i < constraintsLength; ++i) {
                final ConstraintMetadata constraint = constraints[i];
                sb.append(", CONSTRAINT `cons_name_").append(i).append("` ").append(constraint.getConstraintType().toString()).append("(");
                final String[] names = constraint.getColumnNames();
                final int namesLengthDec = names.length - 1;
                for (int j = 0; j <= namesLengthDec; j++) {
                    sb.append("`").append(names[j]).append("`");
                    if (j != namesLengthDec) {
                        sb.append(",");
                    }
                }
                sb.append(") ON CONFLICT ").append(constraint.getConflictType().toString());
            }
            sb.append(")");
            final String statement = sb.toString();
            Printer.print("createTable: ".concat(statement));
            final PreparedStatement stat = conn.prepareStatement(statement);
            executeUpdate(stat);
            return true;
        } catch (final Exception ex) {
            Printer.err("createTable: cannot create table, ".concat(ex.getMessage()));
            return false;
        }
    }

    /**
     * 重組資料庫，建議在AVAILABLE事件時使用。
     *
     * @return 傳回資料庫是否重組成功
     */
    public static boolean vacuum() {
        if (!available) {
            Printer.err("vacuum: not available");
            return false;
        }
        try {
            try (PreparedStatement stat = conn.prepareStatement("vacuum")) {
                stat.execute();
                return true;
            }
        } catch (final Exception ex) {
            Printer.err("vacuum: cannot vacuum, ".concat(ex.getMessage()));
        }
        return false;
    }

    /**
     * 將資料庫回到上一個狀態。
     *
     * @return 傳回是否恢復成功
     */
    public static boolean rollBack() {
        if (!available) {
            Printer.err("rollBack: not available");
            return false;
        }
        if (autoCommit) {
            Printer.err("rollBack: in the auto commit mode");
            return false;
        }
        try {
            conn.rollback();
            Printer.print("rollBack");
            return true;
        } catch (final SQLException ex) {
            Printer.err("rollBack: cannot commit, ".concat(ex.getMessage()));
            return false;
        }
    }

    /**
     * 設定ContentCenter是否使用AutoCommit。
     *
     * @param autoCommit 傳入ContentCenter是否使用AutoCommit
     * @return 傳回是否設定成功
     */
    public static boolean setAutoCommit(final boolean autoCommit) {
        if (!available) {
            Printer.err("setAutoCommit: not available");
            return false;
        }
        if (OS == OperatingSystems.ANDROID) {
            Printer.err("setAutoCommit: not support for android");
            return false;
        }
        try {
            conn.setAutoCommit(autoCommit);
            ContentCenter.autoCommit = autoCommit;
            return true;
        } catch (final SQLException ex) {
            Printer.err("setAutoCommit: cannot set commit mode at this time");
            return false;
        }
    }

    /**
     * 手動儲存資料庫目前的狀態(ANDROID作業系統將無效果)。
     *
     * @return 傳回是否儲存成功
     */
    public static boolean commit() {
        if (!available) {
            Printer.err("commit: not available");
            return false;
        }
        if (autoCommit) {
            Printer.err("commit: in the auto commit mode");
            return false;
        }
        try {
            conn.commit();
            Printer.print("commit");
            return true;
        } catch (final SQLException ex) {
            Printer.err("commit: cannot commit, ".concat(ex.getMessage()));
            return false;
        }
    }

    /**
     * 清空表單的資料。
     *
     * @param tableName 傳入表單名稱
     * @return 傳回表單是否清除成功
     */
    public static boolean deleteTable(final String tableName) {
        return deleteTable(tableName, false, false);
    }

    /**
     * 清空表單的資料。
     *
     * @param tableName 傳入表單名稱
     * @param notifyObserver 傳入是否通知給觀察者
     * @param notifySync 傳入是否通知同步到伺服器
     * @return 傳回表單是否清除成功
     */
    public static boolean deleteTable(final String tableName, final boolean notifyObserver, final boolean notifySync) {
        return deleteTable(tableName, notifyObserver, notifySync, System.currentTimeMillis());
    }

    /**
     * 清空表單的資料。
     *
     * @param tableName 傳入表單名稱
     * @param notifyObserver 傳入是否通知給觀察者
     * @param notifySync 傳入是否通知同步到伺服器
     * @param notifyID 傳入通知的ID
     * @return 傳回表單是否清除成功
     */
    public static boolean deleteTable(final String tableName, final boolean notifyObserver, final boolean notifySync, final long notifyID) {
        if (!available) {
            Printer.err("deleteTable: not available");
            return false;
        }
        if (tableName == null || tableName.trim().length() == 0) {
            Printer.err("deleteTable: tableName is empty");
            return false;
        } else if (tableName.contains("`")) {
            Printer.err("deleteTable: tableName has illegal character");
            return false;
        }
        try {
            final StringBuilder sb = new StringBuilder("DELETE FROM `");
            sb.append(tableName).append("`");

            final String statement = sb.toString();
            Printer.print("deleteTable: ".concat(statement));
            final PreparedStatement stat = conn.prepareStatement(statement);
            final int n = executeUpdate(stat);
            if (n > 0) {
                final JSONObject notify = new JSONObject();
                notify.put(JSONDelete.JSON_TYPE, JSONInsert.TYPE_DELETE);
                notify.put(JSONDelete.JSON_ID, notifyID);
                notify.put(JSONDelete.JSON_TABLE_NAME, tableName);
                if (notifyObserver) {
                    if (!callBack(EVENT.DELETE, NOTIFY.OBSERVER, notify)) {
                        Printer.err("deleteTable: cannot notify observer");
                    }
                }
                if (notifySync) {
                    final JSONObject sync = new JSONObject();
                    sync.put(DB_SYNC_TEXT, notify.toString());
                    insert(DB_SYNC, sync);
                }
            }
            return true;
        } catch (final Exception ex) {
            Printer.err("deleteTable: cannot delete table, ".concat(ex.getMessage()));
            return false;
        }
    }

    /**
     * 刪除資料庫的表單。
     *
     * @param tableName 傳入表單名稱
     * @return 傳回表單是否刪除成功
     */
    public static boolean dropTable(final String tableName) {
        if (!available) {
            Printer.err("dropTable: not available");
            return false;
        }
        if (tableName == null || tableName.trim().length() == 0) {
            Printer.err("dropTable: tableName is empty");
            return false;
        } else if (tableName.contains("`")) {
            Printer.err("dropTable: tableName has illegal character");
            return false;
        }
        try {
            final StringBuilder sb = new StringBuilder("DROP TABLE `");
            sb.append(tableName).append("`");

            final String statement = sb.toString();
            Printer.print("dropTable: ".concat(statement));
            final PreparedStatement stat = conn.prepareStatement(statement);
            executeUpdate(stat);
            final Condition c1 = new Condition();
            c1.setCondition(DB_HIDE_COLUMN_TABLE, tableName, ConditionType.EQUAL);
            final JSONArray array = query(DB_HIDE_COLUMN, c1);
            int l = array.length();
            for (int i = 0; i < l; ++i) {
                final JSONObject json = array.getJSONObject(i);
                String t = json.getString(DB_HIDE_COLUMN_TABLE);
                String c = json.getString(DB_HIDE_COLUMN_COLUMN);
                hsHideColumn.remove(getAbsolutePath(t, c));
            }
            delete(DB_HIDE_COLUMN, c1);
            return true;
        } catch (final Exception ex) {
            Printer.err("dropTable: cannot drop, ".concat(ex.getMessage()));
            return false;
        }
    }

    /**
     * 刪除表單內的資料。
     *
     * @param tableName 傳入表單名稱
     * @param condition 傳入刪除的資料條件
     * @return 傳回資料是否刪除成功
     */
    public static boolean delete(final String tableName, final ConditionInterface condition) {
        return delete(tableName, condition, false, false);
    }

    /**
     * 刪除表單內的資料。
     *
     * @param tableName 傳入表單名稱
     * @param condition 傳入刪除的資料條件
     * @param notifyObserver 傳入是否通知給觀察者
     * @param notifySync 傳入是否通知同步到伺服器
     * @return 傳回資料是否刪除成功
     */
    public static boolean delete(final String tableName, final ConditionInterface condition, boolean notifyObserver, boolean notifySync) {
        return delete(tableName, condition, notifyObserver, notifySync, System.currentTimeMillis());
    }

    /**
     * 刪除表單內的資料。
     *
     * @param tableName 傳入表單名稱
     * @param condition 傳入刪除的資料條件
     * @param notifyObserver 傳入是否通知給觀察者
     * @param notifySync 傳入是否通知同步到伺服器
     * @param notifyID 傳入通知的ID
     * @return 傳回資料是否刪除成功
     */
    public static boolean delete(final String tableName, final ConditionInterface condition, boolean notifyObserver, boolean notifySync, long notifyID) {
        if (!available) {
            Printer.err("delete: not available");
            return false;
        }
        if (tableName == null || tableName.trim().length() == 0) {
            Printer.err("delete: tableName is empty");
            return false;
        } else if (condition == null) {
            Printer.err("delete: condition is null");
            return false;
        } else if (tableName.contains("`")) {
            Printer.err("delete: tableName has illegal character");
            return false;
        }
        try {
            final StringBuilder sb = new StringBuilder("DELETE FROM `");
            final LinkedList list = new LinkedList();
            sb.append(tableName).append("`");

            final String conditionString = condition.toConditionString(list);
            sb.append(" WHERE ").append(conditionString);
            final String statement = sb.toString();
            Printer.print("delete: ".concat(statement));
            final PreparedStatement stat = conn.prepareStatement(statement);
            prepareStatement(stat, list);
            final int n = executeUpdate(stat);
            if (n > 0) {
                final JSONObject notify = new JSONObject();
                notify.put(JSONDelete.JSON_TYPE, JSONInsert.TYPE_DELETE);
                notify.put(JSONDelete.JSON_ID, notifyID);
                notify.put(JSONDelete.JSON_TABLE_NAME, tableName);
                notify.put(JSONDelete.JSON_DELETE_CONDITION, conditionString);
                if (notifyObserver) {
                    if (!callBack(EVENT.DELETE, NOTIFY.OBSERVER, notify)) {
                        Printer.err("delete: cannot notify observer");
                    }
                }
                if (notifySync) {
                    final JSONObject sync = new JSONObject();
                    sync.put(DB_SYNC_TEXT, notify.toString());
                    insert(DB_SYNC, sync);
                }
            }
            return true;
        } catch (final Exception ex) {
            Printer.err("delete: cannot delete, ".concat(ex.getMessage()));
            return false;
        }
    }

    /**
     * 更新表單內的資料。
     *
     * @param tableName 傳入表單名稱
     * @param content 傳入更新的資料內容
     * @param condition 傳入更新的資料條件
     * @return 傳回表單資料是否更新成功
     */
    public static boolean update(final String tableName, final JSONObject content, final ConditionInterface condition) {
        return update(tableName, content, condition, false, false);
    }

    /**
     * 更新表單內的資料。
     *
     * @param tableName 傳入表單名稱
     * @param json 傳入更新的資料內容
     * @param condition 傳入更新的資料條件
     * @param notifyObserver 傳入是否通知給觀察者
     * @param notifySync 傳入是否通知同步到伺服器
     * @return 傳回表單資料是否更新成功
     */
    public static boolean update(final String tableName, final JSONObject json, final ConditionInterface condition, final boolean notifyObserver, final boolean notifySync) {
        return update(tableName, json, condition, notifyObserver, notifySync, System.currentTimeMillis());
    }

    /**
     * 更新表單內的資料。
     *
     * @param tableName 傳入表單名稱
     * @param json 傳入更新的資料內容
     * @param condition 傳入更新的資料條件
     * @param notifyObserver 傳入是否通知給觀察者
     * @param notifySync 傳入是否通知同步到伺服器
     * @param notifyID 傳入通知的ID
     * @return 傳回表單資料是否更新成功
     */
    public static boolean update(final String tableName, final JSONObject json, final ConditionInterface condition, boolean notifyObserver, boolean notifySync, long notifyID) {
        if (!available) {
            Printer.err("update: not available");
            return false;
        }
        if (tableName == null || tableName.trim().length() == 0) {
            Printer.err("update: tableName is empty");
            return false;
        } else if (json == null) {
            Printer.err("update: json is null");
            return false;
        } else if (condition == null) {
            Printer.err("update: condition is null");
            return false;
        } else if (tableName.contains("`")) {
            Printer.err("update: tableName has illegal character");
            return false;
        }
        try {
            final StringBuilder sb = new StringBuilder("UPDATE `");
            final LinkedList<Object> list = new LinkedList<>();
            sb.append(tableName).append("` SET ");

            final Set<String> columns = json.keySet();
            int i = 0;
            int l = columns.size();
            for (final String column : columns) {
                if (column.contains("`")) {
                    throw new Exception("columnName has illegal character");
                }
                final Object obj = json.get(column);
                sb.append("`").append(column).append("`").append(" = ");
                if (obj instanceof String) {
                    String sValue = obj.toString();
                    if (hsHideColumn.contains(getAbsolutePath(tableName, column))) {
                        list.add(StringHider.compression(StringHider.hideString(sValue, hideKey, false)));
                    } else {
                        list.add(obj);
                    }
                } else {
                    list.add(obj);
                }
                sb.append("?");

                if (i != l - 1) {
                    sb.append(", ");
                }
                ++i;
            }
            final LinkedList conditionRightValue = new LinkedList();
            final String conditionString = condition.toConditionString(conditionRightValue);
            sb.append(" WHERE ").append(conditionString);
            final String statement = sb.toString();
            Printer.print("update: ".concat(statement));
            final PreparedStatement stat = conn.prepareStatement(statement);
            list.addAll(conditionRightValue);
            prepareStatement(stat, list);
            int n = executeUpdate(stat);
            if (n > 0) {
                final JSONObject notify = new JSONObject();
                notify.put(JSONUpdate.JSON_TYPE, JSONInsert.TYPE_UPDATE);
                notify.put(JSONUpdate.JSON_ID, notifyID);
                notify.put(JSONUpdate.JSON_TABLE_NAME, tableName);
                notify.put(JSONUpdate.JSON_UPDATE_CONTENT, tableName);
                notify.put(JSONUpdate.JSON_UPDATE_CONDITION, conditionString);
                if (notifyObserver) {
                    if (!callBack(EVENT.UPDATE, NOTIFY.OBSERVER, notify)) {
                        Printer.err("update: cannot notify observer");
                    }
                }
                if (notifySync) {
                    final JSONObject sync = new JSONObject();
                    sync.put(DB_SYNC_TEXT, notify.toString());
                    insert(DB_SYNC, sync);
                }
            }
            return true;
        } catch (final Exception ex) {
            Printer.err("update: cannot update, ".concat(ex.getMessage()));
            return false;
        }
    }

    /**
     * 查詢表單的資料。
     *
     * @param tableName 傳入表單名稱
     * @param orders 傳入結果的排序方法
     * @return 傳回查詢結果，若Array大小為0，表示沒找到結果；若Array為null，表示查詢失敗
     */
    public static JSONArray query(final String tableName, final Order... orders) {
        return query(tableName, null, null, orders);
    }

    /**
     * 查詢表單的資料。
     *
     * @param tableName 傳入表單名稱
     * @param columnNames 傳入要留下的欄位(Project)
     * @param orders 傳入結果的排序方法
     * @return 傳回查詢結果，若Array大小為0，表示沒找到結果；若Array為null，表示查詢失敗
     */
    public static JSONArray query(final String tableName, final String[] columnNames, final Order... orders) {
        return query(tableName, columnNames, null, orders);
    }

    /**
     * 查詢表單的資料。
     *
     * @param tableName 傳入表單名稱
     * @param condition 傳入查詢的條件
     * @param orders 傳入結果的排序方法
     * @return 傳回查詢結果，若Array大小為0，表示沒找到結果；若Array為null，表示查詢失敗
     */
    public static JSONArray query(final String tableName, final ConditionInterface condition, final Order... orders) {
        return query(tableName, null, condition, orders);
    }

    /**
     * 查詢表單的資料。
     *
     * @param tableName 傳入表單名稱
     * @param condition 傳入查詢的條件
     * @param limit 傳入數量限制，小於等於零不限制
     * @param reverse 傳入是否要反向
     * @return 傳回查詢結果，若Array大小為0，表示沒找到結果；若Array為null，表示查詢失敗
     */
    public static JSONArray query(final String tableName, final ConditionInterface condition, final int limit, final boolean reverse) {
        return query(tableName, null, condition, limit, reverse);
    }

    /**
     * 查詢表單的資料。
     *
     * @param tableName 傳入表單名稱
     * @param condition 傳入查詢的條件
     * @param limit 傳入數量限制，小於等於零不限制
     * @param reverse 傳入是否要反向
     * @param orders 傳入結果的排序方法
     * @return 傳回查詢結果，若Array大小為0，表示沒找到結果；若Array為null，表示查詢失敗
     */
    public static JSONArray query(final String tableName, final ConditionInterface condition, final int limit, final boolean reverse, final Order... orders) {
        return query(tableName, null, condition, limit, reverse, orders);
    }

    /**
     * 查詢表單的資料。
     *
     * @param tableName 傳入表單名稱
     * @param columnNames 傳入要留下的欄位(Project)
     * @param condition 傳入查詢的條件
     * @param orders 傳入結果的排序方法
     * @return 傳回查詢結果，若Array大小為0，表示沒找到結果；若Array為null，表示查詢失敗
     */
    public static JSONArray query(final String tableName, final String[] columnNames, final ConditionInterface condition, final Order... orders) {
        return query(tableName, columnNames, condition, 0, false, orders);
    }

    /**
     * 查詢表單的資料。
     *
     * @param tableName 傳入表單名稱
     * @param columnNames 傳入要留下的欄位(Project)
     * @param condition 傳入查詢的條件
     * @param limit 傳入數量限制，小於等於零不限制
     * @param reverse 傳入是否要反向
     * @param orders 傳入結果的排序方法
     * @return 傳回查詢結果，若Array大小為0，表示沒找到結果；若Array為null，表示查詢失敗
     */
    public static JSONArray query(final String tableName, final String[] columnNames, final ConditionInterface condition, final int limit, final boolean reverse, final Order... orders) {
        return query(tableName, columnNames, condition, 0, limit, reverse, orders);
    }

    /**
     * 查詢表單的資料。
     *
     * @param tableName 傳入表單名稱
     * @param columnNames 傳入要留下的欄位(Project)
     * @param condition 傳入查詢的條件
     * @param offset 傳入一開始要略過的筆數，小於等於零不略過
     * @param limit 傳入數量限制，小於等於零不限制
     * @param reverse 傳入是否要反向
     * @param orders 傳入結果的排序方法
     * @return 傳回查詢結果，若Array大小為0，表示沒找到結果；若Array為null，表示查詢失敗
     */
    public static JSONArray query(final String tableName, final String[] columnNames, final ConditionInterface condition, final int offset, final int limit, final boolean reverse, final Order... orders) {
        if (!available) {
            Printer.err("query: not available");
            return null;
        }
        if (tableName == null || tableName.trim().length() == 0) {
            Printer.err("query: tableName is empty");
            return null;
        } else if (tableName.contains("`")) {
            Printer.err("query: tableName has illegal character");
            return null;
        }

        try {
            final StringBuilder sb = new StringBuilder("SELECT ");
            final LinkedList list = new LinkedList();
            if (columnNames == null) {
                sb.append("*");
            } else {
                final int columnNamesLengthDec = columnNames.length - 1;
                if (columnNamesLengthDec < 0) {
                    sb.append("*");
                } else {
                    for (int i = 0; i <= columnNamesLengthDec; ++i) {
                        final String name = columnNames[i];
                        if (name == null || name.trim().length() == 0) {
                            throw new Exception("project column name empty");
                        } else if (name.contains("`")) {
                            throw new Exception("columnName has illegal character");
                        }
                        sb.append("`").append(name).append("`");
                        if (i != columnNamesLengthDec) {
                            sb.append(",");
                        }
                    }
                }
            }
            sb.append(" FROM `").append(tableName).append("`");
            if (condition != null) {
                sb.append(" WHERE ").append(condition.toConditionString(list));
            }
            if (orders != null) {
                final int ordersLengthDec = orders.length - 1;
                if (ordersLengthDec >= 0) {
                    sb.append(" ORDER BY ");
                    for (int i = 0; i <= ordersLengthDec; ++i) {
                        final Order order = orders[i];
                        if (order == null) {
                            throw new Exception("order null");
                        }
                        sb.append(order.toOrderString());
                        if (i != ordersLengthDec) {
                            sb.append(",");
                        }
                    }
                }
            }
            if (limit > 0) {
                sb.append(" LIMIT ").append(limit);
            }
            if (offset > 0) {
                sb.append(" OFFSET ").append(offset);
            }
            final String statement = sb.toString();
            Printer.print("select: ".concat(statement));
            final PreparedStatement stat = conn.prepareStatement(statement);
            prepareStatement(stat, list);
            final JSONArray array = executeQuery(stat, tableName, reverse);
            return array;
        } catch (final Exception ex) {
            Printer.err("query: cannot select, ".concat(ex.getMessage()));
            return null;
        }
    }

    /**
     * 查詢表單的資料數量。
     *
     * @param tableName 傳入表單名稱
     * @param condition 傳入查詢的條件
     * @return 傳回查詢結果的數量，若為0，表示沒找到結果；若為-1，表示查詢失敗
     */
    public static long count(final String tableName, final ConditionInterface condition) {
        if (!available) {
            Printer.err("count: not available");
            return -1;
        }
        if (tableName == null || tableName.trim().length() == 0) {
            Printer.err("count: tableName is empty");
            return -1;
        } else if (tableName.contains("`")) {
            Printer.err("count: tableName has illegal character");
            return -1;
        }

        try {
            final StringBuilder sb = new StringBuilder("SELECT count(*)");
            final LinkedList list = new LinkedList();
            sb.append(" FROM `").append(tableName).append("`");
            if (condition != null) {
                sb.append(" WHERE ").append(condition.toConditionString(list));
            }
            final String statement = sb.toString();
            Printer.print("select: ".concat(statement));
            final PreparedStatement stat = conn.prepareStatement(statement);
            prepareStatement(stat, list);
            final JSONArray array = executeQuery(stat, tableName, false);
            if (array == null || array.length() == 0) {
                return -1;
            }
            final JSONObject object = array.getJSONObject(0);
            return object.getLong("count(*)");
        } catch (final Exception ex) {
            Printer.err("count: cannot select, ".concat(ex.getMessage()));
            return -1;
        }
    }

    /**
     * 插入資料到表單。
     *
     * @param tableName 傳入表單名稱
     * @param content 傳入插入的資料內容
     * @return 傳回資料是否插入成功
     */
    public static boolean insert(final String tableName, final JSONObject content) {
        return insert(tableName, content, false, false);
    }

    /**
     * 插入資料到表單。
     *
     * @param tableName 傳入表單名稱
     * @param content 傳入插入的資料內容
     * @param notifyObserver 傳入是否通知給觀察者
     * @param notifySync 傳入是否通知同步到伺服器
     * @return 傳回資料是否插入成功
     */
    public static boolean insert(final String tableName, final JSONObject content, final boolean notifyObserver, final boolean notifySync) {
        return insert(tableName, content, notifyObserver, notifySync, System.currentTimeMillis());
    }

    /**
     * 插入資料到表單。
     *
     * @param tableName 傳入表單名稱
     * @param content 傳入插入的資料內容
     * @param notifyObserver 傳入是否通知給觀察者
     * @param notifySync 傳入是否通知同步到伺服器
     * @param notifyID 傳入通知的ID
     * @return 傳回資料是否插入成功
     */
    public static boolean insert(final String tableName, final JSONObject content, final boolean notifyObserver, final boolean notifySync, final long notifyID) {
        if (!available) {
            Printer.err("insert: not available");
            return false;
        }
        if (tableName == null || tableName.trim().length() == 0) {
            Printer.err("insert: tableName is empty");
            return false;
        } else if (content == null) {
            Printer.err("insert: json is null");
            return false;
        } else if (tableName.contains("`")) {
            Printer.err("insert: tableName has illegal character");
            return false;
        }
        try {
            final StringBuilder sb = new StringBuilder("INSERT INTO `");
            final LinkedList<Object> list = new LinkedList<>();
            sb.append(tableName).append("` (");

            final StringBuilder value = new StringBuilder();
            final Set<String> columns = content.keySet();
            int i = 0;
            final int l = columns.size();
            for (final String column : columns) {
                if (column.contains("`")) {
                    throw new Exception("columnName has illegal character");
                }
                final Object obj = content.get(column);
                if (obj instanceof String) {
                    final String sValue = obj.toString();
                    if (hsHideColumn.contains(getAbsolutePath(tableName, column))) {
                        list.add(StringHider.compression(StringHider.hideString(sValue, hideKey, false)));
                    } else {
                        list.add(obj);
                    }
                } else {
                    list.add(obj);
                }
                value.append("?");
                sb.append("`").append(column).append("`");
                if (i != l - 1) {
                    sb.append(",");
                    value.append(",");
                }
                ++i;
            }
            sb.append(") VALUES (").append(value).append(")");
            final String statement = sb.toString();
            Printer.print("insert: ".concat(statement));
            final PreparedStatement stat = conn.prepareStatement(statement);
            prepareStatement(stat, list);
            int n = executeUpdate(stat);
            if (n > 0) {
                final JSONObject notify = new JSONObject();
                notify.put(JSONInsert.JSON_TYPE, JSONInsert.TYPE_INSERT);
                notify.put(JSONInsert.JSON_ID, notifyID);
                notify.put(JSONInsert.JSON_TABLE_NAME, tableName);
                notify.put(JSONInsert.JSON_INSERT_CONTENT, content);
                if (notifyObserver) {
                    if (!callBack(EVENT.INSERT, NOTIFY.OBSERVER, notify)) {
                        Printer.err("insert: cannot notify observer");
                    }
                    if (notifySync) {
                        final JSONObject sync = new JSONObject();
                        sync.put(DB_SYNC_TEXT, notify.toString());
                        insert(DB_SYNC, sync);
                    }
                }
            }
            return true;
        } catch (final Exception ex) {
            Printer.err("insert: cannot insert, ".concat(ex.getMessage()));
            return false;
        }
    }

    /**
     * 插入多筆資料到表單。
     *
     * @param tableName 傳入表單名稱
     * @param columnsReference 傳入欄位參考資料(要使用的欄位)
     * @param content 傳入插入的資料內容
     * @return 傳回資料是否插入成功
     */
    public static boolean multiInsert(final String tableName, final JSONObject columnsReference, final JSONArray content) {
        return multiInsert(tableName, columnsReference, content, 0, Integer.MAX_VALUE);
    }

    /**
     * 插入多筆資料到表單。
     *
     * @param tableName 傳入表單名稱
     * @param columnsReference 傳入欄位參考資料(要使用的欄位)
     * @param content 傳入插入的資料內容
     * @param offset 傳入插入的資料位移量
     * @param length 傳入插入的資料數量
     * @return 傳回資料是否插入成功
     */
    public static boolean multiInsert(final String tableName, final JSONObject columnsReference, final JSONArray content, int offset, int length) {
        if (!available) {
            Printer.err("multiInsert: not available");
            return false;
        }
        if (tableName == null || tableName.trim().length() == 0) {
            Printer.err("multiInsert: tableName is empty");
            return false;
        } else if (columnsReference == null) {
            Printer.err("multiInsert: columns reference is null");
            return false;
        } else if (content == null) {
            Printer.err("multiInsert: json is null");
            return false;
        } else if (tableName.contains("`")) {
            Printer.err("multiInsert: tableName has illegal character");
            return false;
        }
        if (offset < 0) {
            offset = 0;
        }
        if (length < 0) {
            length = 0;
        }
        try {
            final StringBuilder sb = new StringBuilder("INSERT INTO `");
            final LinkedList<Object> list = new LinkedList<>();
            sb.append(tableName).append("` (");

            final int contentLength = content.length();
            final int contentLength_dec = contentLength - 1;
            final int min, max;
            if (offset > contentLength_dec) {
                min = contentLength_dec;
            } else {
                min = offset;
            }
            final int maxTemp = min + length;
            if (maxTemp > contentLength) {
                max = contentLength;
            } else {
                max = maxTemp;
            }
            length = max - min;
            final int length_dec = length - 1;
            final StringBuilder[] values = new StringBuilder[length];
            for (int i = 0; i < length; ++i) {
                values[i] = new StringBuilder("");
            }

            final Set<String> columns = columnsReference.keySet();
            int i = 0;
            final int l = columns.size();
            final int l_dec = l - 1;
            final ArrayList<String> columnList = new ArrayList<>();
            for (final String column : columns) {
                if (column.contains("`")) {
                    throw new Exception("columnName has illegal character");
                }
                sb.append("`").append(column).append("`");
                columnList.add(column);

                if (i != l_dec) {
                    sb.append(",");
                }
                ++i;
            }
            sb.append(") VALUES ");

            for (int j = min; j < max; ++j) {
                final JSONObject row = content.getJSONObject(j);
                final StringBuilder value = values[j - min];

                i = 0;
                for (final String column : columnList) {
                    final Object obj = row.get(column);

                    if (obj != null) {
                        if (obj instanceof String) {
                            final String sValue = obj.toString();
                            if (hsHideColumn.contains(getAbsolutePath(tableName, column))) {
                                list.add(StringHider.compression(StringHider.hideString(sValue, hideKey, false)));
                            } else {
                                list.add(obj);
                            }
                        } else {
                            list.add(obj);
                        }
                        value.append("?");
                    }
                    if (i != l_dec) {
                        value.append(",");
                    }
                    ++i;
                }
            }
            for (i = 0; i < length; ++i) {
                sb.append("(").append(values[i]).append(")");
                if (i != length_dec) {
                    sb.append(",");
                }
            }
            final String statement = sb.toString();
            Printer.print("multiInsert: ".concat(statement));
            final PreparedStatement stat = conn.prepareStatement(statement);
            prepareStatement(stat, list);
            executeUpdate(stat);
            return true;
        } catch (final Exception ex) {
            Printer.err("multiInsert: cannot insert, ".concat(ex.getMessage()));
            return false;
        }
    }

    /**
     * 取得指定Key值的內容。
     *
     * @param key 傳入Key
     * @return 傳回指定Key值的內容
     */
    public static String get(final String key) {
        if (!available) {
            Printer.err("get: not available");
            return null;
        }
        if (key == null || key.trim().length() == 0) {
            Printer.err("get: key is empty");
            return null;
        }
        final Condition c = new Condition();
        c.setCondition(DB_SETTING_KEY, key, ConditionType.EQUAL);
        final JSONArray array = query(DB_SETTING, c);
        if (array.length() > 0) {
            return array.getJSONObject(0).getString(DB_SETTING_VALUE);
        } else {
            return null;
        }
    }

    /**
     * 刪除指定的key值。
     *
     * @param key 傳入Key
     * @return 傳回是否刪除成功
     */
    public static boolean remove(final String key) {
        if (!available) {
            Printer.err("remove: not available");
            return false;
        }
        if (key == null || key.trim().length() == 0) {
            Printer.err("remove: key is empty");
            return false;
        }
        return put(key, null);
    }

    /**
     * 在指定的key值內放入內容。
     *
     * @param key 傳入Key
     * @param value 傳入內容
     * @return 傳回內容是否放入成功
     */
    public static boolean put(final String key, final String value) {
        if (!available) {
            Printer.err("put: not available");
            return false;
        }
        if (key == null || key.trim().length() == 0) {
            Printer.err("put: key is empty");
            return false;
        } else if (value == null) {
            final Condition c = new Condition();
            c.setCondition(DB_SETTING_KEY, key, ConditionType.EQUAL);
            return delete(DB_SETTING, c);
        } else {
            final JSONObject json = new JSONObject();
            json.put(DB_SETTING_KEY, key);
            json.put(DB_SETTING_VALUE, value);
            return insert(DB_SETTING, json);
        }
    }

    /**
     * 關閉ContentCenter。
     *
     * @return 傳回ContentCenter是否關閉成功
     */
    public static boolean closeContentCenter() {
        if (!available) {
            Printer.err("closeContentCenter: not available");
            return false;
        }
        try {
            conn.close();
            available = false;
            return true;
        } catch (final SQLException ex) {
            Printer.err("closeContentCenter: cannot close database, ".concat(ex.getMessage()));
            return false;
        }
    }

    /**
     * '
     * 執行SQL查詢敘述。
     *
     * @param statement SQL敘述
     * @param tableName 傳入表單名稱
     * @param reverse 傳入是否要將結果反向
     * @return 傳回執行SQL查詢後的結果
     * @throws Exception 拋出例外
     */
    private static JSONArray executeQuery(final PreparedStatement statement, final String tableName, final boolean reverse) throws Exception {
        final ResultSet result;
        final LinkedList<JSONObject> list = new LinkedList<>();
        result = statement.executeQuery();
        final ResultSetMetaData rsmd = result.getMetaData();
        final int l = rsmd.getColumnCount();
        while (result.next()) {
            final JSONObject tuple = new JSONObject();
            for (int i = 1; i <= l; ++i) {
                final String columnName = rsmd.getColumnName(i);
                final int type = rsmd.getColumnType(i);
                final Object o = result.getObject(i);
                if (o == null) {
                    tuple.put(columnName, JSONObject.NULL);
                } else {
                    switch (type) {
                        case 0: //實作不完整的SQL程式可能會無法判定type
                            if (o instanceof String) {
                                String s = (String) o;
                                if (hsHideColumn.contains(getAbsolutePath(tableName, columnName))) {
                                    s = StringHider.recoverString(StringHider.decompression(s), hideKey);
                                }
                                tuple.put(columnName, s);
                            } else if (o instanceof Long) {
                                tuple.put(columnName, (Long) o);
                            } else if (o instanceof Number) {
                                tuple.put(columnName, (Double) o);
                            }
                            break;
                        case 4: //integer
                            tuple.put(columnName, result.getLong(columnName));
                            break;
                        case 6: //float
                        case 7:
                            tuple.put(columnName, result.getDouble(columnName));
                            break;
                        case 12: //text
                            String s = result.getString(columnName);
                            if (hsHideColumn.contains(getAbsolutePath(tableName, columnName))) {
                                s = StringHider.recoverString(StringHider.decompression(s), hideKey);
                            }
                            tuple.put(columnName, s);
                            break;
                    }
                }
            }
            if (reverse) {
                list.add(0, tuple);
            } else {
                list.add(tuple);
            }
        }
        final JSONArray array = new JSONArray();
        for (final JSONObject obj : list) {
            array.put(obj);
        }
        result.close();
        statement.close();
        return array;
    }

    /**
     * 執行SQL修改敘述。
     *
     * @param statement SQL敘述
     * @return 傳回執行SQL修改後的結果
     * @throws Exception 拋出例外
     */
    private static int executeUpdate(final PreparedStatement statement) throws Exception {
        final int result = statement.executeUpdate();
        statement.close();
        if (OS == OperatingSystems.ANDROID && result == -1) {
            return 1;
        }
        return result;
    }

    /**
     * 呼叫監聽物件。
     *
     * @param event 傳入事件
     * @param notify 傳入通知類型
     * @param information 傳入要提供的訊息
     * @return 監聽物件是否執行成功
     */
    private static boolean callBack(final EVENT event, final NOTIFY notify, final JSONObject information) {
        if (listener != null) {
            try {
                return listener.onActionPerforming(event, notify, information);
            } catch (Exception ex) {
                Printer.err("callBack: onActionPerforming throw exception, ".concat(ex.getMessage()));
                return false;
            }
        } else {
            Printer.err("callBack: must add a ContentCenterListener");
            return false;
        }
    }

    /**
     * 完成PreparedStatement
     *
     * @param statement 傳入未完成的PreparedStatement
     * @param list 傳入PreparedStatement的參數
     */
    private static void prepareStatement(final PreparedStatement statement, final LinkedList<Object> list) {
        final int l = list.size();
        for (int i = 0; i < l; ++i) {
            final Object obj = list.get(i);
            final int index = i + 1;
            try {
                if (obj instanceof Long) {
                    statement.setLong(index, (Long) obj);
                } else if (obj instanceof Double) {
                    statement.setDouble(index, (Double) obj);
                } else if (obj == null) {
                    statement.setNull(index, Types.NULL);
                } else {
                    statement.setString(index, obj.toString());
                }
            } catch (final Exception ex) {

            }
        }
    }

    /**
     * 取得完整的表單欄位路徑。
     *
     * @param tableName 傳入表單名稱
     * @param columnName 傳入欄位名稱
     * @return 傳回完整的表單欄位路徑。
     */
    private static String getAbsolutePath(final String tableName, final String columnName) {
        if (tableName == null || columnName == null) {
            return null;
        }
        return tableName.concat(".").concat(columnName);
    }

    /**
     * 取得JDBC的Driver路徑。
     *
     * @return 傳回JDBC的Driver路徑。
     */
    private static String getDBDriver() {
        if (OS == null) {
            return null;
        } else if (OS == OperatingSystems.ANDROID) {
            return DB_DRIVER_ANDROID;
        } else {
            return DB_DRIVER_PC;
        }
    }

    /**
     * 取得DB的URL。
     *
     * @param dbFile 傳入DB檔案
     * @return 傳回DB的URL
     */
    private static String getDBURL(final File dbFile) {
        if (dbFile == null || OS == null) {
            return null;
        } else {
            final StringBuilder sb = new StringBuilder();
            switch (OS) {
                case ANDROID:
                    sb.append(DB_URL_ANDROID_PREFIX);
                    break;
                case PC:
                    sb.append(DB_URL_PC_PREFIX);
            }
            sb.append(":").append(dbFile.getAbsolutePath());
            return sb.toString();
        }
    }

    // -----建構子-----
    /**
     * 私有建構子，無法直接實體化。
     */
    private ContentCenter() {

    }
}
