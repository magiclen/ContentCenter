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
package org.magiclen.content.center;

/**
 * 預設的Printer，使用標準輸出串流。
 *
 * @author Magic Len
 * @see Printer
 */
class DefaultPrinter extends Printer {

    // -----物件方法-----
    /**
     * 印出一般訊息。
     *
     * @param s 傳入要印出的字串
     */
    @Override
    protected void printStatement(final String s) {
	System.out.println(s);
    }

    /**
     * 印出錯誤訊息。
     *
     * @param s 傳入要印出的字串
     */
    @Override
    protected void printError(final String s) {
	System.err.println(s);
    }

}
