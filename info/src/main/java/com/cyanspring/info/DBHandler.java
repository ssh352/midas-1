package com.cyanspring.info;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cyanspring.common.marketdata.HistoricalPrice;

public class DBHandler 
{
	private static final Logger log = LoggerFactory
			.getLogger(DBHandler.class);
	private String     jdbcUrl;
	private Connection connect = null ;
	private Statement  stat = null ;
	DBHandler(String jdbcUrl, String driverClass) throws Exception
	{
		this.jdbcUrl = jdbcUrl ;
		Class.forName(driverClass);
		try 
		{
			connect = DriverManager.getConnection(jdbcUrl);
		} 
		catch (SQLException e) 
		{
			// TODO Auto-generated catch block
			log.error(e.toString(), e);
		}
	}
	public boolean isConnected()
    {
        try
        {
            if (connect != null && !connect.isClosed())
            {
                return true;
            }
        }
        catch (SQLException se)
        {
            log.error(se.toString(), se);
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
        	connect = DriverManager.getConnection(jdbcUrl);
        }
        catch (SQLException e)
        {
            log.error(e.toString(), e);
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
            log.error(e.toString(), e);
        }
    }
    public void updateSQL(String sqlcmd)
    {
        if (!isConnected())
        {
            reconnectSQL();
        }
        createStatement();
        try {
        	connect.setAutoCommit(false);
			stat = connect.createStatement();

			stat.executeUpdate(sqlcmd);

			connect.commit();
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
			try {
				connect.rollback();
			} catch (SQLException se) {
				log.error(se.getMessage(), se);
			}
		} finally {
			closeStatement();
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
            log.error(se.toString(), se);
            log.trace(sqlcmd);
        }
        return rs;
    }
    public void checkSQLConnect()
    {
    	if (!isConnected())
    	{
            reconnectSQL();
    	}
    	createStatement();
    	try 
    	{
			stat.executeQuery("SELECT 1;");
		} 
    	catch (SQLException e) 
    	{
            log.error(e.toString(), e) ;
		}
    	finally
    	{
    		closeStatement();
    	}
    }
    public void createStatement()
    {
    	try 
    	{
    		if (stat != null)
    		{
    			stat.close();
    			stat = null ;
    		}
			stat = connect.createStatement() ;
		} 
    	catch (SQLException e) 
    	{
            log.error(e.toString(), e) ;
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
				log.error(e.toString(), e);
			}
    	}
    }
    public void addBatch(String sqlcmd)
    {
        try
        {
            stat.addBatch(sqlcmd);
        }
        catch(SQLException se)
        {
        	log.error(se.toString(), se) ;
        }
    }
    public void executeBatch()
    {
        try 
        {
			stat.executeBatch();
		} 
        catch (SQLException e) 
        {
			log.error(e.toString(), e) ;
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
    	HistoricalPrice price = new HistoricalPrice(symbol, true) ;
    	String prefix = (market.equals("FX")) ? "0040" : market;
    	String strTable = String.format("%s_%s", prefix, type) ;
    	String sqlcmd = "" ;
    	boolean getPrice = false;
    	if (dir)
    	{
    		sqlcmd = String.format("select * from %s where SYMBOL = '%s' order by TRADEDATE limit 1 ;", 
    				strTable, symbol) ;
    	}
    	else
    	{
    		sqlcmd = String.format("select * from %s where SYMBOL = '%s' order by TRADEDATE desc limit 1 ;", 
    				strTable, symbol) ;
    	}
    	ResultSet rs = querySQL(sqlcmd) ;
    	try {
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			if (rs.next())
			{
				price.setTimestamp(sdf.parse(rs.getString("TRADEDATE")));
				price.setSymbol(rs.getString("SYMBOL")) ;
				price.setOpen(rs.getDouble("OPEN_PRICE"));
				price.setClose(rs.getDouble("CLOSE_PRICE"));
				price.setHigh(rs.getDouble("HIGH_PRICE"));
				price.setLow(rs.getDouble("LOW_PRICE"));
				price.setVolume(rs.getInt("VOLUME"));
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
			// TODO Auto-generated catch block
            log.error(e.toString(), e) ;
            log.trace(sqlcmd);
			return null ;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
            log.error(e.toString(), e) ;
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
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd 00:00:00") ;
		String sqlcmd = String.format("DELETE FROM %s WHERE SYMBOL='%s' AND TRADEDATE='%s';", 
				strTable, symbol, sdf.format(price.getTimestamp())) ;
		updateSQL(sqlcmd) ;
    }
}
