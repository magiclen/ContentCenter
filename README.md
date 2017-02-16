ContentCenter
=================================

# Introduction

**ContentCenter** is a Java framework for the content of applications. It includes [Mson](https://github.com/magiclen/MagicLenJSON "Mson") to support JSON data to access database, and includes [MagicStringHider](https://github.com/magiclen/MagicStringHider "MagicStringHider") to hide your data that are stored in the database. The database used in **ContentCenter** is SQLite, but programmers don't have to write any SQL to access it. **ContentCenter** also supports observers to refresh your views when the corresponding data are changed, and supports syncs to synchronize data between client and server. **ContentCenter** uses [sqlite-jdbc](https://bitbucket.org/xerial/sqlite-jdbc "sqlite-jdbc") and [SQLDroid](https://github.com/SQLDroid/SQLDroid "SQLDroid") JDBC drivers to access SQLite on PC(Linux, Mac OS X, Windows...) and Android.

# Usage

## ContentCenterListener Interface

**ContentCenterListener** interface is in the *org.magiclen.content.center.listener* package. You must implement it before you initialize a **ContentCenter** instance.

### Implement

The following is a template of implementing a **ContentCenterListener** instance.

    final ContentCenterListener listener = (final ContentCenterListener.EVENT event, final ContentCenterListener.NOTIFY notify, final JSONObject information) -> {
    	switch (event) {
        case INITIAL_PRAGMA:
    			// TODO Before building your database, there are some features of the database can be controled.
    			return true;
    		case INITIAL:
    			// TODO Build your database. Create tables, input presets, etc.
    			return true;
    		case AVAILABLE:
    			// TODO Check your database. Update tables, input new data, etc.
    			return true;
    		case INSERT:
    			// After inserting data into your database.
    			switch (notify) {
    				case OBSERVER:
    					// TODO Refresh your view. You may use notifyObserver static method.
    					return true;
    				case SYNC:
    					// TODO Sync the data to your server. You may use notifySyncToServer static method.
    					return true;
    			}
    			return false;
    		case UPDATE:
    			// After updating data into your database.
    			switch (notify) {
    				case OBSERVER:
    					// TODO Refresh your view. You may use notifyObserver static method.
    					return true;
    				case SYNC:
    					// TODO Sync the data to your server. You may use notifySyncToServer static method.
    					return true;
    			}
    			return false;
    		case DELETE:
    			// After deleting data into your database.
    			switch (notify) {
    				case OBSERVER:
    					// TODO Refresh your view. You may use notifyObserver static method.
    					return true;
    				case SYNC:
    					// TODO Sync the data to your server. You may use notifySyncToServer static method.
    					return true;
    			}
    			return false;
    		case REMOTESYNC:
    			// Sync the data from your server periodically.
    			// TODO Sync the data from your server.
    			// You don't have to return anything.
    	}
    	return false;
    };


## ContentCenter Class

**ContentCenter** class is in the *org.magiclen.content.center* package. Use it by calling its static methods. There is something you have to know first. That is, one application, one **ContentCenter**.

### Initialize

You should define where the database file you want to use and set a hiding key for hidden data when **ContentCenter** initializes. And you also need a **ContentCenterListener** instance to do something and return the status of events. You must implement your **ContentCenterListener** instance before you initialize **ContentCenter**.

    final File dbFile = new File("/home/magiclen/mydb.db");
    final String hideKey = "magic key";
    ContentCenter.initialContentCenter(dbFile, listener, hideKey);

### Create a table

The storage of **ContentCenter** is based on SQLite. Before you store data in your database, you should create tables for your database first. You can find **TableMetadata** class, **ConstraintMetadata** class, **ColumnType** enum, **ConflictType** enum, and **ConstraintType** enum in the *org.magiclen.content.database* package. Create a **TableMetadata** instance and use **createTable** static method in **ContentCenter**.

For example, to create a table named `students`,

    final TableMetadata students = new TableMetadata();
    students.addNewColumn("student_id", ColumnType.INTEGER, false);
    students.setPrimaryKey("student_id"); // Auto increament
    students.addNewColumn("student_name", ColumnType.TEXT, false);
    students.addNewColumn("student_national_id", ColumnType.TEXT, false);
    students.addConstraint(ConstraintType.UNIQUE, ConflictType.REPLACE, "student_national_id");
    students.addNewColumn("student_avg_score", ColumnType.REAL, true); // For graduates

    ContentCenter.createTable("students", students);

You can write your code in your **ContentCenterListener** instance when the event is `INITIAL`.

### Register your observers and syncs

If you want to refresh your views or sync your data to server. You can register your observers and syncs into **ContentCenter**.

To create an **Observer** instance, you have to implement the **Observer** interface in the *org.magiclen.content.observer* package. To create an **Sync** instance, you have to implement the **Sync** interface in the *org.magiclen.content.sync* package.

Use **notifyObserver** static method and **notifySyncToServer** static method in **ContentCenter** to register your observers and syncs.

    final Observer observerStudentList = () -> {
        // do something
        return true;
    };
    ContentCenter.registerObserver("student_list", observerStudentList);

    final Sync syncStudentList = (final JSONObject data) -> {
        // do something
        return true;
    };
    ContentCenter.registerSync("student_list", syncStudentList);

You can use your observers and syncs in your **ContentCenterListener** instance. For example,

    ...
    case INSERT:
      // After inserting data into your database.
      switch (notify) {
        case OBSERVER:
          return ContentCenter.notifyObserver("student_list");
        case SYNC:
          return ContentCenter.notifySyncToServer("student_list", information);
      }
    return false;
    ...

To turn the sync thread on, you have to call **runPeriodicSync** static method in **ContentCenter**. Conversely, call **stopPeriodicSync** static method in **ContentCenter** to stop it. You can use **setSyncPeriod** static method in **ContentCenter** to set the time interval for synchronizing between client and server periodically. The sync thread will also call **ContentCenterListener** that you set periodically as a `REMOTESYNC` event.

    final long period = 800; // milliseconds
    ContentCenter.setSyncPeriod(period);
    ContentCenter.runPeriodicSync();

### Insert data

All the data in **ContentCenter** are represented by JSON formated. So, if you want to add some students in your students table, you have to create **JSONObject** instances for these students. Use **insert** static method in **ContentCenter** to add them.

    final JSONObject studentMagicLen = new JSONObject();
    studentMagicLen.put("student_name", "Magic Len");
    studentMagicLen.put("student_national_id", "L181718292");
    studentMagicLen.put("student_avg_score", 99.9);
    ContentCenter.insert("students", studentMagicLen);

    final JSONObject studentHolaGuest = new JSONObject();
    studentHolaGuest.put("student_name", "Hola Guest");
    studentHolaGuest.put("student_national_id", "L145148883");
    ContentCenter.insert("students", studentHolaGuest);

    final JSONObject studentDean = new JSONObject();
    studentDean.put("student_name", "Dean");
    studentDean.put("student_national_id", "C137966763");
    studentDean.put("student_avg_score", 89.1);
    ContentCenter.insert("students", studentDean);

    final JSONObject studentDicky = new JSONObject();
    studentDicky.put("student_name", "Dicky");
    studentDicky.put("student_national_id", "G101810329");
    studentDicky.put("student_avg_score", 59.2);
    ContentCenter.insert("students", studentDicky);

If you want to notify your observers or syncs, you can pass `true` to the parameters. For example,

    ContentCenter.insert("students", studentMagicLen, true, true);

### Query data

Use **query** static method in **ContentCenter** to query data from your database. It returns a JSONArray instance as results. For example, to query all of the students from your students table,

    final JSONArray results = ContentCenter.query("students");
    System.out.println(results.toString(true));

The result is,

    [
    	{
    		"student_name" : "Magic Len",
    		"student_avg_score" : 99.9,
    		"student_national_id" : "L181718292",
    		"student_id" : 1
    	},
    	{
    		"student_name" : "Hola Guest",
    		"student_avg_score" : null,
    		"student_national_id" : "L145148883",
    		"student_id" : 2
    	},
    	{
    		"student_name" : "Dean",
    		"student_avg_score" : 89.1,
    		"student_national_id" : "C137966763",
    		"student_id" : 3
    	},
    	{
    		"student_name" : "Dicky",
    		"student_avg_score" : 59.2,
    		"student_national_id" : "G101810329",
    		"student_id" : 4
    	}
    ]

If you want to sort the result, you can use **Order** class and **OrderType** enum in the *org.magiclen.content.database* package to create an **Order** instance. For example, to sort the result with student_avg_score,

The result is,

    [
    	{
    		"student_name" : "Magic Len",
    		"student_avg_score" : 99.9,
    		"student_national_id" : "L181718292",
    		"student_id" : 1
    	},
    	{
    		"student_name" : "Dean",
    		"student_avg_score" : 89.1,
    		"student_national_id" : "C137966763",
    		"student_id" : 3
    	},
    	{
    		"student_name" : "Dicky",
    		"student_avg_score" : 59.2,
    		"student_national_id" : "G101810329",
    		"student_id" : 4
    	},
    	{
    		"student_name" : "Hola Guest",
    		"student_avg_score" : null,
    		"student_national_id" : "L145148883",
    		"student_id" : 2
    	}
    ]

If you just want few columns, input the names of columns when querying.

    final String[] columns = new String[]{"student_national_id", "student_avg_score"};
    final Order order = new Order();
    order.setOrder("student_avg_score", OrderType.DESC);
    final JSONArray results = ContentCenter.query("students", columns, order);
    System.out.println(results.toString(true));

The result is,

    [
    	{
    		"student_avg_score" : 99.9,
    		"student_national_id" : "L181718292"
    	},
    	{
    		"student_avg_score" : 89.1,
    		"student_national_id" : "C137966763"
    	},
    	{
    		"student_avg_score" : 59.2,
    		"student_national_id" : "G101810329"
    	},
    	{
    		"student_avg_score" : null,
    		"student_national_id" : "L145148883"
    	}
    ]

If you want to query with some conditions, you can use **Condition** class, **Conditions** class, **LogicType** enum, and **ConditionType** enum in the *org.magiclen.content.database* package to create a **ConditionInterface** instance. For example, to search the student whose score is between 40 and 90 and the last character of the name of the student is 'y',

    final String[] columns = new String[]{"student_name"};

    final Order order = new Order();
    order.setOrder("student_avg_score", OrderType.DESC);

    final Condition c1 = new Condition();
    c1.setCondition("student_avg_score", 40, ConditionType.BIGGER_THAN);
    final Condition c2 = new Condition();
    c2.setCondition("student_avg_score", 90, ConditionType.EQUAL_OR_SMALLER_THAN);
    final Condition c3 = new Condition();
    c3.setCondition("student_name", "%y", ConditionType.LIKE); // fuzzy search
    final Conditions c123 = new Conditions();
    c123.setConditions(LogicType.AND, c1, c2, c3);

    final JSONArray results = ContentCenter.query("students", columns, c123, order);
    System.out.println(results.toString(true));

The result is,

    [
    	{
    		"student_name" : "Dicky"
    	}
    ]

### Update data

Use **update** static method in **ContentCenter** to update data in your database. For example, to change the name of the student whose national ID is 'G101810329' to 'Micky',

    final JSONObject studentMicky = new JSONObject();
    studentMicky.put("student_name", "Micky");

    final Condition c = new Condition();
    c.setCondition("student_national_id", "G101810329", ConditionType.EQUAL);

    ContentCenter.update("students", studentMicky, c);

### Delete data

Use **delete** static method in **ContentCenter** to delete data in your database. For example, to delete the students who haven't graduated,

    final Condition c = new Condition();
    c.setCondition("student_avg_score", null, ConditionType.EQUAL);

    ContentCenter.delete("students", c);

### User Settings

You can quickly access strings by using **put**, **get**, and **remove** static methods. These strings stored in your database will be hidden by **MagicStringHider** automatically. This is a good way to store user settings of your application.

    ContentCenter.put("key", "value");
    System.out.println(ContentCenter.get("key"));
    ContentCenter.remove("key");

### Close ContentCenter

Before you close your application, you should use **closeContentCenter** static method in **ContentCenter** to make sure your database closed in a correct situation.

    ContentCenter.closeContentCenter();

# License

    Copyright 2015-2017 magiclen.org

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

# What's More?

Please check out our web page at

https://magiclen.org/contentcenter/
