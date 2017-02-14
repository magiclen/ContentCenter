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
package org.magiclen.content.center.listener;

import org.magiclen.json.JSONObject;
import org.magiclen.content.center.ContentCenter;

/**
 * ContentCenter的監聽者。
 *
 * @see ContentCenter
 * @author Magic Len
 */
public interface ContentCenterListener {

    // -----類別列舉-----
    /**
     * <p>
     * 事件。
     * </p>
     *
     * <ul>
     * <li>
     * INITIAL_PRAGMA：一開始建立資料庫前要啟用的資料庫特性。
     * </li>
     * <li>
     * INITIAL：一開始建立資料庫時。
     * </li>
     * <li>
     * AVAILABLE：資料庫載入成功時。
     * </li>
     * <li>
     * DELETE：刪除資料庫中的資料後。
     * </li>
     * <li>
     * UPDATE：更新資料庫的資料後。
     * </li>
     * <li>
     * SYNC：執行自動同步並取得Server最新資料時。
     * </li>
     * </ul>
     */
    static enum EVENT {

        INITIAL_PRAGMA, INITIAL, AVAILABLE, INSERT, DELETE, UPDATE, REMOTESYNC;
    }

    /**
     * <p>
     * 通知。
     * </p>
     *
     * <ul>
     * <li>
     * OBSERVER：通知觀察者。
     * </li>
     * <li>
     * SERVER：通知Sync並將資料同步到Server上。
     * </li>
     * </ul>
     *
     */
    static enum NOTIFY {

        OBSERVER, SYNC;
    }

    //-----------------物件方法-----------------
    /**
     * ContentCenter的監聽者監聽時做的工作。
     *
     * @param event 事件
     * @param notify 通知
     * @param information 資料
     * @return 傳回處理結果
     * @throws Exception 拋出例外
     */
    public boolean onActionPerforming(final EVENT event, final NOTIFY notify, final JSONObject information) throws Exception;
}
