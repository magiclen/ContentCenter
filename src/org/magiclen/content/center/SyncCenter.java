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

import java.util.HashMap;
import org.magiclen.content.sync.Sync;
import org.magiclen.json.JSONObject;

/**
 * 同步中心。
 *
 * @author Magic Len
 */
class SyncCenter {

    // -----類別常數-----
    /**
     * 儲存同步物件。
     */
    private final static HashMap<String, Sync> hmSync = new HashMap<>();

    // -----類別方法-----
    /**
     * 註冊同步物件。
     *
     * @param syncID 傳入同步要使用的ID
     * @param sync 傳入同步物件
     * @param overlap 覆蓋之前註冊的物件
     * @return 傳回同步物件是否註冊成功
     */
    static boolean registerSync(final String syncID, final Sync sync, final boolean overlap) {
	if (syncID == null || sync == null) {
	    Printer.err("registerSync: input can't be null");
	    return false;
	} else if (hmSync.containsKey(syncID) && !overlap) {
	    Printer.err("registerSync: duplicate syncID");
	    return false;
	}
	hmSync.put(syncID, sync);
	return true;
    }

    /**
     * 通知同步物件同步資料到伺服器。
     *
     * @param syncID 傳入同步物件的ID
     * @param json 傳入同步時提供的訊息
     * @return 傳回同步物件是否同步成功
     */
    static boolean notifySyncToServer(final String syncID, final JSONObject json) throws Exception {
	if (syncID == null || json == null) {
	    Printer.err("notifySyncToServer: input can't be null");
	    return false;
	}
	final Sync sync = hmSync.get(syncID);
	if (sync == null) {
	    Printer.err("notifySyncToServer: can't find sync");
	    return false;
	}
	return sync.syncToServer(json);
    }

    // -----建構子-----
    /**
     * 私有建構子，無法直接實體化。
     */
    private SyncCenter() {

    }
}
