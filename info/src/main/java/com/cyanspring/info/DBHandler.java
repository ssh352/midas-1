package com.cyanspring.info;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cyanspring.common.marketdata.HistoricalPrice;

public class DBHandler 
{
	private static final Logger log = LoggerFactory
			.getLogger(DBHandler.class);
	private final String createTable = "CREATE TABLE `%s` (`TRADEDATE`  date NULL DEFAULT NULL ,`KEYTIME`  datetime NOT NULL ,`DATATIME`  datetime NULL DEFAULT NULL ,`SYMBOL`  varchar(16) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL ,`OPEN_PRICE`  double NULL DEFAULT NULL ,`CLOSE_PRICE`  double NULL DEFAULT NULL ,`HIGH_PRICE`  double NULL DEFAULT NULL ,`LOW_PRICE`  double NULL DEFAULT NULL ,`VOLUME`  int(11) NULL DEFAULT NULL ,`TOTALVOLUME`  bigint(20) NULL DEFAULT NULL ,UNIQUE INDEX `TradeDate_Symbol` USING BTREE (`KEYTIME`, `SYMBOL`)) ENGINE=MyISAM DEFAULT CHARACTER SET=latin1 COLLATE=latin1_swedish_ci CHECKSUM=0 ROW_FORMAT=Dynamic DELAY_KEY_WRITE=0 ;";
	private String     jdbcUrl;
	private Connection connect = null ;
	private Statement  stat = null ;
	public DBHandler(String jdbcUrl, String driverClass) throws Exception
	{
		this.jdbcUrl = jdbcUrl ;
		Class.forName(driverClass);
		try 
		{
			connect = DriverManager.getConnection(jdbcUrl);
		} 
		catch (SQLException e) 
		{
			log.error(e.getMessage(), e);
		}
	}
	public boolean isConnected()
    {
        try
        {        	
            if (connect != null)
            {
            	if(connect.isValid(0)) 
            	{
            		return true;
            	}	
            	else 
            	{
            		log.warn("SQL connection is invalid, try to reconnect later");
            		return false;
            	}
            }
            else
            {
            	return false;
            }
        }
        catch (SQLException se)
        {
            log.error(se.getMessage(), se);
        }
        return false;
    }
    public void reconnectSQL()
    {
        try
        {
        	if (connect != null)
        	{
        		connect.close();
        	}
        	log.info("Reconnect to SQL, discard old connection");
        	connect = DriverManager.getConnection(jdbcUrl);
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
    }
    public void disconnectSQL()
    {
        try
        {
        	if (connect != null)
        	{
        		connect.close();
        	}
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
    }
    public void updateSQL(String sqlcmd)
    {
        if (!isConnected())
        {
            reconnectSQL();
        }
        Statement stat = null ;
        try {
        	connect.setAutoCommit(false);
			stat = connect.createStatement();

			stat.executeUpdate(sqlcmd);

			connect.commit();
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
			log.warn("Exception while: " + sqlcmd);
            this.reconnectSQL();
        }
        finally 
		{
			if (stat != null)
	    	{
	    		try 
	    		{
					stat.close();
					stat = null;
				} 
	    		catch (SQLException e) 
				{
					log.error(e.getMessage(), e);
				}
	    	}
		}
    }
    public ResultSet querySQL(String sqlcmd)
    {
        if (!isConnected())
        {
            reconnectSQL();
        }
        ResultSet rs = null;
        Statement stat = null ;
        try
        {
            stat = connect.createStatement();
        	connect.setAutoCommit(false);
            rs = stat.executeQuery(sqlcmd);
			connect.commit();
        }
        catch (SQLException se)
        {
            log.error(se.getMessage(), se);
			log.warn("Exception while: " + sqlcmd);
            this.reconnectSQL();
        }
        return rs;
    }
    public void checkSQLConnect()
    {
    	if (!isConnected())
    	{
    		if (this.stat != null)
    		{
    			closeStatement();
    		}
            reconnectSQL();
    	}
        Statement stat = null ;
        try
        {
            stat = connect.createStatement();
			stat.executeQuery("SELECT 1;");
		} 
    	catch (SQLException e) 
    	{
            log.error(e.getMessage(), e) ;
            this.reconnectSQL();
        }
    	finally
    	{
			if (stat != null)
	    	{
	    		try 
	    		{
					stat.close();
					stat = null;
				} 
	    		catch (SQLException e) 
				{
					log.error(e.getMessage(), e);
				}
	    	}
    	}
    }
    public void createStatement()
    {
		if (stat != null)
		{
			return;
		}
    	try 
    	{
			stat = connect.createStatement() ;
		} 
    	catch (SQLException e) 
    	{
            log.error(e.getMessage(), e) ;
		}
    }
    public void closeStatement()
    {
    	if (stat != null)
    	{
    		try 
    		{
				stat.close();
				stat = null;
			} 
    		catch (SQLException e) 
			{
				log.error(e.getMessage(), e);
			}
    	}
    }
    public void addBatch(String sqlcmd)
    {
    	this.checkSQLConnect();
    	createStatement();
        try
        {
            stat.addBatch(sqlcmd);
        }
        catch(SQLException se)
        {
        	log.error(se.getMessage(), se) ;
        }
    }
    public void executeBatch()
    {
    	this.checkSQLConnect();
        try 
        {
			stat.executeBatch();
		} 
        catch (SQLException e) 
        {
			log.error(e.getMessage(), e) ;
		}
        finally 
        {
        	closeStatement();
        }
    }
    public HistoricalPrice getLastValue(String market, String type, String symbol, boolean dir)
    {
    	if (market == null)
    	{
    		return null;
    	}
    	HistoricalPrice price = new HistoricalPrice() ;
    	String prefix = (market.equals("FX")) ? "0040" : market;
    	String strTable = String.format("%s_%s", prefix, type) ;
    	String sqlcmd = "" ;
    	boolean getPrice = false;
    	if (dir)
    	{
    		sqlcmd = String.format("select * from %s where `SYMBOL` = '%s' order by `KEYTIME` limit 1 ;", 
    				strTable, symbol) ;
    	}
    	else
    	{
    		sqlcmd = String.format("select * from %s where `SYMBOL` = '%s' order by `KEYTIME` desc limit 1 ;", 
    				strTable, symbol) ;
    	}
    	ResultSet rs = querySQL(sqlcmd) ;
    	try {
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			if (rs.next())
			{
				price.setTradedate(rs.getString("TRADEDATE"));
				if (rs.getString("KEYTIME") != null) price.setKeytime(sdf.parse(rs.getString("KEYTIME")));
				if (rs.getString("DATATIME") != null) price.setDatatime(sdf.parse(rs.getString("DATATIME")));
				price.setSymbol(rs.getString("SYMBOL")) ;
				price.setOpen(rs.getDouble("OPEN_PRICE"));
				price.setClose(rs.getDouble("CLOSE_PRICE"));
				price.setHigh(rs.getDouble("HIGH_PRICE"));
				price.setLow(rs.getDouble("LOW_PRICE"));
				price.setVolume(rs.getLong("VOLUME"));
				price.setTotalVolume(rs.getLong("TOTALVOLUME"));
				price.setTurnover(rs.getLong("TURNOVER"));
				getPrice = true;
			}
			if (getPrice)
			{
				return price ;
			}
			else
			{
				return null;
			}
		} catch (SQLException e) {
            log.error(e.getMessage(), e) ;
            log.trace(sqlcmd);
			return null ;
		} catch (ParseException e) {
            log.error(e.getMessage(), e) ;
			return null ;
		}
    }
    public List<HistoricalPrice> getPeriodValue(String market, String type, String symbol, Date startdate)
    {
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    	String prefix = (market.equals("FX")) ? "0040" : market;
    	String strTable = String.format("%s_%s", prefix, type) ;
    	String sqlcmd = "" ;
		sqlcmd = String.format("SELECT * FROM %s WHERE `SYMBOL`='%s' AND `KEYTIME`>='%s' ORDER BY `KEYTIME`;", 
				strTable, symbol, sdf.format(startdate));
    	ResultSet rs = querySQL(sqlcmd) ;
    	try {
    		HistoricalPrice price;
    		ArrayList<HistoricalPrice> retList = new ArrayList<HistoricalPrice>(); 
			while (rs.next())
			{
				price = new HistoricalPrice();
				price.setTradedate(rs.getString("TRADEDATE"));
				if (rs.getString("KEYTIME") != null) price.setKeytime(sdf.parse(rs.getString("KEYTIME")));
				if (rs.getString("DATATIME") != null) price.setDatatime(sdf.parse(rs.getString("DATATIME")));
				price.setSymbol(rs.getString("SYMBOL")) ;
				price.setOpen(rs.getDouble("OPEN_PRICE"));
				price.setClose(rs.getDouble("CLOSE_PRICE"));
				price.setHigh(rs.getDouble("HIGH_PRICE"));
				price.setLow(rs.getDouble("LOW_PRICE"));
				price.setVolume(rs.getLong("VOLUME"));
				price.setTotalVolume(rs.getLong("TOTALVOLUME"));
				price.setTurnover(rs.getLong("TURNOVER"));
				retList.add(price);
			}
			if (retList.isEmpty())
			{
				return null;
			}
			else
			{
				return retList;
			}
		} catch (SQLException e) {
            log.error(e.getMessage(), e) ;
            log.trace(sqlcmd);
			return null ;
		} catch (ParseException e) {
            log.error(e.getMessage(), e) ;
			return null ;
		}
    }
    public void deletePrice(String market, String type, String symbol, HistoricalPrice price)
    {
    	if (price == null)
    	{
    		return;
    	}
    	if (market == null)
    	{
    		return;
    	}
    	String prefix = (market.equals("FX")) ? "0040" : market;
    	String strTable = String.format("%s_%s", prefix, type) ;
		String sqlcmd = String.format("DELETE FROM %s WHERE `SYMBOL`='%s' AND `KEYTIME`='%s';", 
				strTable, symbol, price.getKeytime()) ;
		updateSQL(sqlcmd) ;
    }
    public void checkMarketExist(String market)
    {
    	String prefix = null;
    	if (market.equals("FX"))
    	{
    		prefix = "0040";
    	}
    	else
    	{
    		prefix = market;
    	}
    	checkTableExist(prefix + "_1");
    	checkTableExist(prefix + "_R");
    	checkTableExist(prefix + "_A");
    	checkTableExist(prefix + "_Q");
    	checkTableExist(prefix + "_H");
    	checkTableExist(prefix + "_6");
    	checkTableExist(prefix + "_T");
    	checkTableExist(prefix + "_D");
    	checkTableExist(prefix + "_W");
    	checkTableExist(prefix + "_M");
    }
    public void checkTableExist(String table)
    {
    	this.checkSQLConnect();
    	if (table == null)
    		return;
    	try 
    	{
        	DatabaseMetaData dbm = connect.getMetaData();
        	ResultSet tables = dbm.getTables(null, null, table, null);
        	if (tables.next()) {
        	  // Table exists
        	}
        	else {
				String sqlcmd = String.format(createTable, table);
				updateSQL(sqlcmd);
        	}
		} 
    	catch (SQLException e) {
			log.error(e.getMessage(), e);
		}
    }
}
