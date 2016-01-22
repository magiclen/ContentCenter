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
package org.magiclen.content.sync;

import org.magiclen.json.JSONObject;

/**
 * 同步類別，繼承這個類別來實作自己的同步物件。
 *
 * @author Magic Len
 */
public interface Sync {

    // -----物件方法-----
    /**
     * 同步資料到伺服器上。
     *
     * @param json 傳入要同步的資料
     * @return 傳回同步是否成功
     */
    public boolean syncToServer(final JSONObject json);
}
