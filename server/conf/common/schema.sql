DROP TABLE ANYONE.EXECUTIONS;
CREATE TABLE ANYONE.EXECUTIONS(OBJECT_ID VARCHAR(40) NOT NULL PRIMARY KEY, SYMBOL VARCHAR(30), SIDE VARCHAR(20), QUANTITY DOUBLE, PRICE DOUBLE,
	ORDER_ID VARCHAR(40), PARENT_ID VARCHAR(40), STRATEGY_ID VARCHAR(40), EXEC_ID VARCHAR(40), MODIFIED TIMESTAMP, CREATED TIMESTAMP, SEQ_ID VARCHAR(40), SERVER_ID VARCHAR(40), TRADER VARCHAR(40), ACCOUNT VARCHAR(40), ROUTE VARCHAR(40));

DROP TABLE ANYONE.ACTIVE_CHILD_ORDERS;
CREATE TABLE ANYONE.ACTIVE_CHILD_ORDERS(OBJECT_ID VARCHAR(40) NOT NULL PRIMARY KEY, SYMBOL VARCHAR(30), SIDE VARCHAR(20), QUANTITY DOUBLE, PRICE DOUBLE,
	CUM_QTY DOUBLE, AVG_PX DOUBLE, ORDER_TYPE VARCHAR(20), ORD_STATUS VARCHAR(20), PARENT_ID VARCHAR(40), STRATEGY_ID VARCHAR(40), 
	MODIFIED TIMESTAMP, CREATED TIMESTAMP, SEQ_ID VARCHAR(40), CLORDER_ID VARCHAR(40), SERVER_ID VARCHAR(40), TRADER VARCHAR(40), ACCOUNT VARCHAR(40), ROUTE VARCHAR(40), EXCHANGE_ORDER_ID VARCHAR(40));

DROP TABLE ANYONE.CHILD_ORDER_AUDIT;
CREATE TABLE ANYONE.CHILD_ORDER_AUDIT(AUDIT_ID VARCHAR(40) NOT NULL PRIMARY KEY,OBJECT_ID VARCHAR(40), EXEC_TYPE VARCHAR(20), SYMBOL VARCHAR(30), SIDE VARCHAR(20), QUANTITY DOUBLE, PRICE DOUBLE,
	CUM_QTY DOUBLE, AVG_PX DOUBLE, ORDER_TYPE VARCHAR(20), ORD_STATUS VARCHAR(20), PARENT_ID VARCHAR(40), STRATEGY_ID VARCHAR(40), 
	MODIFIED TIMESTAMP, CREATED TIMESTAMP, SEQ_ID VARCHAR(40), CLORDER_ID VARCHAR(40), SERVER_ID VARCHAR(40), TRADER VARCHAR(40), ACCOUNT VARCHAR(40), ROUTE VARCHAR(40), EXCHANGE_ORDER_ID VARCHAR(40));

DROP TABLE ANYONE.TEXT_OBJECTS;
CREATE TABLE ANYONE.TEXT_OBJECTS(OBJECT_ID VARCHAR(40), OBJECT_TYPE VARCHAR(40), STRATEGY_STATE VARCHAR(20), LINE_NO INTEGER, 
	TIME_STAMP TIMESTAMP, XML_TEXT VARCHAR(4096), 
	SERVER_ID VARCHAR(40), TRADER VARCHAR(40), ACCOUNT VARCHAR(40), ROUTE VARCHAR(40),
	CONSTRAINT ST_PRIMARY_KEY PRIMARY KEY (OBJECT_ID, OBJECT_TYPE, LINE_NO));
CREATE INDEX ST_INDEX ON ANYONE.TEXT_OBJECTS (OBJECT_ID);

DROP TABLE ANYONE.USERS;
CREATE TABLE ANYONE.USERS(USER_ID VARCHAR(40) NOT NULL PRIMARY KEY, USER_NAME VARCHAR(40), PASSWORD VARCHAR(40),
	EMAIL VARCHAR(80), PHONE VARCHAR(40), CREATED TIMESTAMP, LAST_LOGIN TIMESTAMP, USER_TYPE VARCHAR(20), DEFAULT_ACCOUNT VARCHAR(40),USER_ROLE VARCHAR(20) DEFAULT 'Trader');

DROP TABLE ANYONE.ACCOUNTS;
CREATE TABLE ANYONE.ACCOUNTS(ACCOUNT_ID VARCHAR(40) NOT NULL PRIMARY KEY, USER_ID VARCHAR(40), MARKET VARCHAR(40), 
	CASH DOUBLE, MARGIN DOUBLE, PNL DOUBLE, ALL_TIME_PNL DOUBLE, UR_PNL DOUBLE, 
	CASH_DEPOSITED DOUBLE, ROLL_PRICE DOUBLE, UNIT_PRICE DOUBLE,
	ACTIVE BOOLEAN, CREATED TIMESTAMP, CURRENCY VARCHAR(10), CASH_AVAILABLE DOUBLE DEFAULT 0, MARGIN_HELD DOUBLE DEFAULT 0,START_ACCOUNT_VALUE DOUBLE DEFAULT 0, STATE VARCHAR(20) DEFAULT 'ACTIVE');
	
DROP TABLE ANYONE.ACCOUNTS_DAILY;
CREATE TABLE ANYONE.ACCOUNTS_DAILY(ACCOUNT_ID VARCHAR(40), USER_ID VARCHAR(40), MARKET VARCHAR(40), 
	CASH DOUBLE, MARGIN DOUBLE, PNL DOUBLE, ALL_TIME_PNL DOUBLE, UR_PNL DOUBLE, 
	CASH_DEPOSITED DOUBLE, ROLL_PRICE DOUBLE, UNIT_PRICE DOUBLE,
	ACTIVE BOOLEAN, CREATED TIMESTAMP, CURRENCY VARCHAR(10), ON_DATE DATE, ON_TIME TIMESTAMP, TRADE_DATE VARCHAR(20),
	CASH_AVAILABLE DOUBLE DEFAULT 0, MARGIN_HELD DOUBLE DEFAULT 0,
	CONSTRAINT AD_PRIMARY_KEY PRIMARY KEY (ACCOUNT_ID, ON_DATE));
CREATE INDEX AD_INDEX ON ANYONE.ACCOUNTS_DAILY (ACCOUNT_ID);
	
DROP TABLE ANYONE.OPEN_POSITIONS;
CREATE TABLE ANYONE.OPEN_POSITIONS(POSITION_ID VARCHAR(40) NOT NULL PRIMARY KEY, USER_ID VARCHAR(40), ACCOUNT_ID VARCHAR(40),
	SYMBOL VARCHAR(40), QTY DOUBLE,	PRICE DOUBLE, PNL DOUBLE, AC_PNL DOUBLE, CREATED TIMESTAMP, MARGIN DOUBLE DEFAULT 0);

DROP TABLE ANYONE.CLOSED_POSITIONS;
CREATE TABLE ANYONE.CLOSED_POSITIONS(POSITION_ID VARCHAR(80) NOT NULL PRIMARY KEY, USER_ID VARCHAR(40), 
	ACCOUNT_ID VARCHAR(40), SYMBOL VARCHAR(40), 
	QTY DOUBLE,	BUY_PRICE DOUBLE, SELL_PRICE DOUBLE, PNL DOUBLE, AC_PNL DOUBLE, CREATED TIMESTAMP);	
	
DROP TABLE ANYONE.ACCOUNT_SETTINGS;
CREATE TABLE ANYONE.ACCOUNT_SETTINGS(ACCOUNT_ID VARCHAR(40) NOT NULL PRIMARY KEY, DEFAULT_QTY DOUBLE DEFAULT NULL, STOP_LOSS_VALUE DOUBLE DEFAULT NULL, COMPANY_SL_VALUE DOUBLE DEFAULT 0, MARGIN DOUBLE DEFAULT 0, 
ROUTE VARCHAR(40), LEVERAGE_RATE DOUBLE DEFAULT 0, COMMISSION DOUBLE DEFAULT 0, DAILY_STOPLOSS DOUBLE DEFAULT 0 ,TRAILING_STOP DOUBLE DEFAULT 0,STOP_LOSS_PERCENT DOUBLE DEFAULT 0, FREEZE_PERCENT DOUBLE DEFAULT 0,TERMINATE_PERCENT DOUBLE DEFAULT 0,LIVE_TRADING BOOLEAN DEFAULT false ,USER_LIVE_TRADING BOOLEAN DEFAULT false,LIVE_TRADING_TYPE VARCHAR(30) DEFAULT 'DEFAULT',LIVE_TRADING_SETTED_DATE VARCHAR(20) DEFAULT '',FREEZE_VALUE DOUBLE DEFAULT 0,TERMINATE_VALUE DOUBLE DEFAULT 0,DEFAULT_QTY2 VARCHAR(60),LTS_API_PERM BOOLEAN DEFAULT true);

DROP TABLE ANYONE.POSITION_PEAK_PRICE;
CREATE TABLE ANYONE.POSITION_PEAK_PRICE(ACCOUNT_ID VARCHAR(40), SYMBOL VARCHAR(40),POSITION DOUBLE DEFAULT 0,PRICE DOUBLE DEFAULT 0,
CONSTRAINT PPP_PRIMARY_KEY PRIMARY KEY (ACCOUNT_ID, SYMBOL));

DROP TABLE ANYONE.GROUP_MANAGEMENT;
CREATE TABLE ANYONE.GROUP_MANAGEMENT(MANAGER VARCHAR(40), MANAGED VARCHAR(40),CONSTRAINT GM_PRIMARY_KEY PRIMARY KEY (MANAGER, MANAGED));

DROP TABLE ANYONE.CASH_AUDIT;
CREATE TABLE ANYONE.CASH_AUDIT(CASH_ID VARCHAR(40), ACCOUNT_ID VARCHAR(40), TYPE VARCHAR(40), CASH_DEPOSITED DOUBLE DEFAULT 0, ADD_CASH DOUBLE DEFAULT 0, ADD_TIME TIMESTAMP);

DROP TABLE ANYONE.COIN_CONTROL;
CREATE TABLE ANYONE.COIN_CONTROL(ACCOUNT_ID VARCHAR(40) NOT NULL PRIMARY KEY,CHECK_POSITION_STOP_LOSS_START TIMESTAMP ,CHECK_POSITION_STOP_LOSS_END TIMESTAMP ,CHECK_DAILY_STOP_LOSS_START TIMESTAMP ,CHECK_DAILY_STOP_LOSS_END TIMESTAMP,CHECK_TRAILING_STOP_START TIMESTAMP ,CHECK_TRAILING_STOP_END TIMESTAMP,CHECK_DAY_TRADING_MODE_START TIMESTAMP, CHECK_DAY_TRADING_MODE_END TIMESTAMP,MODIFY_TIME TIMESTAMP);
