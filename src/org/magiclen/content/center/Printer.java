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

/**
 * 標準字串輸出工具。
 *
 * @author Magic Len
 * @see ContentCenter
 */
public abstract class Printer {

    // -----類別變數-----
    /**
     * 儲存要使用的輸出工具。
     */
    private static Printer printer;

    // -----類別初始化-----
    static {
	useDefaultPrinter();
    }

    // -----類別方法-----
    /**
     * 設定Printer。
     *
     * @param printer 傳入要使用的printer
     * @return 傳回是否設定成功
     */
    static boolean setPrinter(final Printer printer) {
	if (printer == null) {
	    return false;
	}
	Printer.printer = printer;
	return true;
    }

    /**
     * 使用預設的Printer，訊息會使用標準輸出串流來輸出。
     */
    static void useDefaultPrinter() {
	setPrinter(new DefaultPrinter());
    }

    /**
     * 如果ContentCenter的printStatement有開啟，那就印出一般訊息。
     *
     * @param s 傳入要印出的字串
     * @return 傳回是否成功印出
     */
    static boolean print(final String s) {
	if (ContentCenter.printStatement) {
	    printer.printStatement(s);
	    return true;
	}
	return false;
    }

    /**
     * 如果ContentCenter的printError有開啟，那就印出錯誤訊息。
     *
     * @param s 傳入要印出的字串
     * @return 傳回是否成功印出
     */
    static boolean err(final String s) {
	if (ContentCenter.printError) {
	    printer.printError(s);
	    return true;
	}
	return false;
    }

    // -----抽象物件方法-----
    /**
     * 印出一般訊息。
     *
     * @param s 傳入要印出的字串
     */
    protected abstract void printStatement(final String s);

    /**
     * 印出錯誤訊息。
     *
     * @param s 傳入要印出的字串
     */
    protected abstract void printError(final String s);
}
