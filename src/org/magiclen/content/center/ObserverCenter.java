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
import org.magiclen.content.observer.Observer;

/**
 * 觀察者中心。
 *
 * @author Magic Len
 */
class ObserverCenter {

    // -----類別常數-----
    /**
     * 儲存觀察者物件。
     */
    private final static HashMap<String, Observer> hmObserver = new HashMap<>();

    // -----類別方法-----
    /**
     * 註冊觀察者物件。
     *
     * @param observerID 傳入觀察者要使用的ID
     * @param observer 傳入觀察者物件
     * @param overlap 覆蓋之前註冊的物件
     * @return 傳回觀察者是否註冊成功
     */
    static boolean registerObserver(final String observerID, final Observer observer, final boolean overlap) {
	if (observerID == null || observer == null) {
	    Printer.err("registerObserver: input can't be null");
	    return false;
	} else if (hmObserver.containsKey(observerID) && !overlap) {
	    Printer.err("registerObserver: duplicate observerID");
	    return false;
	}
	hmObserver.put(observerID, observer);
	return true;
    }

    /**
     * 通知觀察者去更新元件的畫面。
     *
     * @param observerID 傳入觀察者的ID
     * @return 傳回觀察者是否更新畫面成功
     */
    static boolean notifyObserver(final String observerID) throws Exception {
	if (observerID == null) {
	    Printer.err("notifyObserver: input can't be null");
	    return false;
	}
	final Observer observer = hmObserver.get(observerID);

	if (observer == null) {
	    Printer.err("notifyObserver: can't find observer");
	    return false;
	}
	return observer.refreshView();
    }

    // -----建構子-----
    /**
     * 私有建構子，無法直接實體化。
     */
    private ObserverCenter() {

    }
}
