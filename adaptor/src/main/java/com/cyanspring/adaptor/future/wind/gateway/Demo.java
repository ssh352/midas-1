package com.cyanspring.adaptor.future.wind.gateway;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.com.wind.td.tdf.*;
/*
import cn.com.wind.td.tdf.DATA_TYPE_FLAG;
import cn.com.wind.td.tdf.TDFClient;
import cn.com.wind.td.tdf.TDF_CODE;
import cn.com.wind.td.tdf.TDF_CODE_RESULT;
import cn.com.wind.td.tdf.TDF_CONNECT_RESULT;
import cn.com.wind.td.tdf.TDF_ERR;
import cn.com.wind.td.tdf.TDF_LOGIN_RESULT;
import cn.com.wind.td.tdf.TDF_MSG;
import cn.com.wind.td.tdf.TDF_MSG_DATA;
import cn.com.wind.td.tdf.TDF_MSG_ID;
import cn.com.wind.td.tdf.TDF_OPEN_SETTING;
import cn.com.wind.td.tdf.TDF_OPTION_CODE;
import cn.com.wind.td.tdf.TDF_PROXY_SETTING;
import cn.com.wind.td.tdf.TDF_PROXY_TYPE;
*/
//import demo.tdfapi.DataInfo;

public class Demo {
	private final  boolean outputToScreen = true;  
	/***********************c***************************************/
	private String openMarket = ""; 
	private final int openData = 0;
	private final int openTime = 0;
	private StringBuilder  subscription = new StringBuilder("600000.SH;000001.SZ");
	private int openTypeFlags = DATA_TYPE_FLAG.DATA_TYPE_FUTURE_CX | DATA_TYPE_FLAG.DATA_TYPE_INDEX; // DATA_TYPE_FLAG.DATA_TYPE_ALL;
	private TDF_CONNECT_RESULT connectResult = null;
	private TDF_LOGIN_RESULT loginResult = null;
	private TDF_CODE_RESULT codeTableResult = null;
	private TDF_MARKET_CLOSE marketClose = null;
	private TDF_QUOTATIONDATE_CHANGE dateChange = null;
	private WindGateway windGateway = null;
	
	private static final Logger log = LoggerFactory
			.getLogger(com.cyanspring.adaptor.future.wind.gateway.Demo.class);	
	/***********************configuration***************************************/
	private String ip,username,password;
	private int port;
	TDFClient client = new TDFClient();
	ConcurrentLinkedQueue<WindRequest> requestQueue = new ConcurrentLinkedQueue<WindRequest>();
	boolean bServerReady = false;
	boolean dedicatedWindThread = false;
	boolean bWholeMarket = false;
	
	LinkedBlockingQueue<TDF_MSG> transactionQueue = new LinkedBlockingQueue<TDF_MSG>();
	LinkedBlockingQueue<TDF_MSG> futureQueue = new LinkedBlockingQueue<TDF_MSG>();
	LinkedBlockingQueue<TDF_MSG> quotationQueue = new LinkedBlockingQueue<TDF_MSG>();
	Thread trdQuotation,trdTransaction,trdFuture;
	String[] strMarkets = null;
	boolean bWindReconnect = false;	
	protected boolean quitFlag = true;
	
	MsgProcessor mpQuotation , mpFuture , mpTransaction;
	
	
	public int getPort() {
		return this.port;
	}
	Demo(String ip, int port, String username, String password , int typeFlags, WindGateway gateWay , boolean d , boolean wm,String mkts,boolean wr) {
		this.ip = ip;
		this.port = port;
		this.username = username;
		this.password = password;
		this.windGateway = gateWay;
		this.openTypeFlags = typeFlags;
		this.quitFlag = true;
		this.dedicatedWindThread = d;
		this.bWholeMarket = wm;
		this.openMarket = mkts;
		if(mkts.isEmpty() == false) {
			strMarkets = openMarket.split(";") ;
		}
		this.bWindReconnect = wr;

		
		if(d) {
			mpQuotation = new MsgProcessor(windGateway,quotationQueue);
			mpFuture = new MsgProcessor(windGateway,futureQueue);
			mpTransaction = new MsgProcessor(windGateway,transactionQueue);
			
			trdQuotation = new Thread( mpQuotation,"ProcessQuotation");			
			trdFuture    = new Thread( mpFuture,"ProcessFutures");
			trdTransaction = new Thread( mpTransaction,"ProcessTransaction");
			
			trdQuotation.start();
			trdFuture.start();
			trdTransaction.start();
		}

		/*
		int err = reconnect();
		if( err != TDF_ERR.TDF_ERR_SUCCESS)
		{
			//System.out.printf("Can't connect to %s:%d. �����˳���\n", ip, port);
			//System.exit(err);			
		}
		*/
	}
	
	public int reconnect()
	{

		TDF_OPEN_SETTING setting = new TDF_OPEN_SETTING();
		setting.setIp(ip);
		setting.setPort( Integer.toString(port));
		setting.setUser(username);
		setting.setPwd(password);

		setting.setReconnectCount(99999999);
		setting.setReconnectGap(10);
		setting.setProtocol(0);
		setting.setMarkets(openMarket);
		setting.setDate(openData);
		setting.setTime(openTime);
		requestQueue.clear();
		if(bWholeMarket) {
			subscription = new StringBuilder("");
		}
		setting.setSubScriptions(subscription.toString());
		setting.setTypeFlags(openTypeFlags);
		setting.setConnectionID(0);
		
		log.info("try to connect : " + this.ip + " " + this.port);
		int err = client.open(setting);
		if (err == TDF_ERR.TDF_ERR_SUCCESS) {
			this.quitFlag = false;
			log.info("Connected , subscription : " + (bWholeMarket ? "Whole Market" : subscription.toString()));			
		}		
		else
		{
			log.warn("Can't connect to " + ip + ":" + port  + " , err code : " + err);
			client.close();
		}
		log.debug("connecting result : " + err);
		return err;
	}	
	Demo(String ip, int port, String username, String password,
			String proxy_ip, int proxy_port, String proxy_user, String proxy_pwd) {
		
		this.quitFlag = false;		
		TDF_OPEN_SETTING setting = new TDF_OPEN_SETTING();
		setting.setIp(ip);
		setting.setPort( Integer.toString(port));
		setting.setUser(username);
		setting.setPwd(password);

		setting.setReconnectCount(99999999);
		setting.setReconnectGap(10);
		setting.setProtocol(0);
		setting.setMarkets(openMarket);
		setting.setDate(openData);
		setting.setTime(openTime);
		requestQueue.clear();
		setting.setSubScriptions(subscription.toString());
		setting.setTypeFlags(openTypeFlags);
		setting.setConnectionID(0);
		
		TDF_PROXY_SETTING proxySetting = new TDF_PROXY_SETTING();
		proxySetting.setProxyHostIp(proxy_ip);
		proxySetting.setProxyPort(Integer.toString(proxy_port));
		proxySetting.setProxyUser(proxy_user);
		proxySetting.setProxyPwd(proxy_pwd);
		proxySetting.setProxyType(TDF_PROXY_TYPE.TDF_PROXY_HTTP11);		

		int err = client.openProxy(setting, proxySetting);
		if (err!=TDF_ERR.TDF_ERR_SUCCESS) {
			log.warn("Can't connect to %s:%d. �����˳���\n", ip, port);
			System.exit(err);
		}		
	}	
	
	public boolean hadMarket(String sym) {
		if(strMarkets == null || strMarkets.length == 0) {
			return true;
		}
		for(String market : strMarkets) {
			if(sym.contains(market)) {
				return true;
			}
		}
		return false;
	}
	
	public void AddRequest(WindRequest wr)
	{
		if(wr.reqId == WindRequest.Subscribe && bWholeMarket == false) {
			StringBuilder sb = null;
			String[] syms = wr.strInfo.split(";");
			for(String sym : syms) {
				if(subscription.indexOf(sym) < 0)
				{
					if(hadMarket(sym) == false) {
						continue;
					}
					subscription.append(";" + sym);
					if(sb == null) {
						sb = new StringBuilder(sym);
					} else {
						sb.append(";" + sym);
					}
				}				
			}
			if(sb != null) {
				wr.strInfo = sb.toString();
				requestQueue.add(wr);	
			}			
			/*
			if(subscription.indexOf(wr.strInfo) < 0)
			{
				subscription.append(";" + wr.strInfo);
				requestQueue.add(wr);								
			}
			*/
		}
	}
	
	public void Stop() {
		setQuitFlag(true);
		mpQuotation.Stop();
		mpFuture.Stop();
		mpTransaction.Stop();
	}
	
	public void setQuitFlag(Boolean para){
		this.quitFlag = para;
	}
	public TDF_OPTION_CODE getOptionCodeInfo(String szWindCode)
	{
		return client.getOptionCodeInfo(szWindCode);
	}
	void printCodeTable() {		
		TDF_CODE[] codes = client.getCodeTable("CZC");		
		PrintHelper.printCodeTable(codes);		
	}
	
	boolean processRequest()
	{
		if(bServerReady == false)
		{
			return false;
		}
		if(requestQueue.size() > 0) {
			WindRequest wr = requestQueue.peek();
			if(wr != null)
			{
				requestQueue.remove(wr);
				if(wr.reqId == WindRequest.Subscribe)
				{
					if(bWholeMarket == false) {
						client.setSubscription(wr.strInfo, SUBSCRIPTION_STYLE.SUBSCRIPTION_ADD);				
						log.info("Add Subscription : " + wr.strInfo);
					}
				}				
			}
			return true;
		}		
		return false;
	}
	
	void run() {
		TDF_MSG_DATA data;
		

		while (!quitFlag) {
			
			TDF_MSG msg = client.getMessage(10);
			if (msg==null) {
				while(processRequest()) ;
				continue;
			}
			
			switch(msg.getDataType()) {
			//ϵͳ��Ϣ
			case TDF_MSG_ID.MSG_SYS_HEART_BEAT :	
				//System.out.println("Heart Beat");
				if(windGateway != null)
				{
					windGateway.receiveHeartBeat();
				}					
				break;
			case TDF_MSG_ID.MSG_SYS_DISCONNECT_NETWORK:				
				System.out.println("NETWORK DISCONNECT");
				log.info("Receive Wind NETWORK DISCONNECT");
				if(bWindReconnect == false) {
					quitFlag = true;
				}
				bServerReady = false;
				break;
			case TDF_MSG_ID.MSG_SYS_CONNECT_RESULT:{
				data = TDFClient.getMessageData(msg, 0);
				System.out.println("CONNECT RESULT");
				log.info("Receive Wind CONNECT RESULT");
				connectResult = data.getConnectResult();
				PrintHelper.printConnectResult(data.getConnectResult());
				break;
			}
			case TDF_MSG_ID.MSG_SYS_LOGIN_RESULT:{
				data = TDFClient.getMessageData(msg, 0);
				loginResult = data.getLoginResult();
				PrintHelper.printLoginResult(data.getLoginResult());
				break;
			}
			case TDF_MSG_ID.MSG_SYS_CODETABLE_RESULT:{
				System.out.println("CODE TABLE RESULT");
				log.info("Receive Wind CODE TABLE RESULT");
				data = TDFClient.getMessageData(msg, 0);
				codeTableResult = data.getCodeTableResult();
				PrintHelper.printCodeTableResult(data.getCodeTableResult());
				if(windGateway != null) {								
					String[] markets = data.getCodeTableResult().getMarket();  
					for(int i= 0; i < data.getCodeTableResult().getMarkets();i++) { // 因為 markets 會回超出數量的 array , 所以要用 getMarkets 來判斷有幾個 market					
						log.info("Receive Code Table - Market : " + markets[i] + " , Symbol Count : " + client.getCodeTable(markets[i]).length );
						windGateway.receiveCodeTable(markets[i], client.getCodeTable(markets[i]),codeTableResult.getCodeDate()[i]);						
					}
					windGateway.connectedWithWind(markets);
				}				
				if(subscription.toString().isEmpty() == false) {
					if(bWholeMarket) {
						subscription = new StringBuilder("");
					}
					client.setSubscription(subscription.toString(), SUBSCRIPTION_STYLE.SUBSCRIPTION_SET);				
					log.info("Server Ready , set Subscription : " + subscription.toString());
				} else {
					log.info("Server Ready , subscribe whole market");
				}
				bServerReady = true;
				//printCodeTable();
				//err = client.setSubscription("AG1506.SHF", 1);
				//System.out.println("Subscription Result : " + err);
				break;
			}
			case TDF_MSG_ID.MSG_SYS_MARKET_CLOSE:{
				data = TDFClient.getMessageData(msg, 0);
				marketClose = data.getMarketClose();
				PrintHelper.printMarketClose(data.getMarketClose());
				if(windGateway != null)
				{
					windGateway.receiveMarketClose(marketClose);
				}				
				break;
			}
			case TDF_MSG_ID.MSG_SYS_QUOTATIONDATE_CHANGE: {				
				data = TDFClient.getMessageData(msg, 0);
				dateChange = data.getDateChange();
				PrintHelper.printDateChange(data.getDateChange());
				if(windGateway != null)
				{
					windGateway.receiveQuotationDateChange(dateChange);
				}
				break;
			}
			//�����Ϣ
			case TDF_MSG_ID.MSG_DATA_MARKET:
				log.debug("MARKET DATA Count : " + msg.getAppHead().getItemCount());
				if(dedicatedWindThread) {
					quotationQueue.add(msg);
					break;
				}
				if(windGateway != null) {
					for (int i=0; i<msg.getAppHead().getItemCount(); i++) {
						data = TDFClient.getMessageData(msg, i); 
						//PrintHelper.printDataMarket(data.getMarketData());
						windGateway.receiveMarketData(data.getMarketData());
					}			
					windGateway.flushAllClientMsgPack();					
				}						
				/*
				if(System.currentTimeMillis() - LastPrintTime  > 10 * 1000 && msg.getAppHead().getItemCount()>0){
					PrintHelper.printDataMarket(TDFClient.getMessageData(msg, 0).getMarketData());
					System.gc();
					LastPrintTime = System.currentTimeMillis();
				}
				PrintHelper.SaveData( new DataInfo(msg,PrintHelper.GetCurrentTime()) );
				*/
								
				break;
			case TDF_MSG_ID.MSG_DATA_INDEX:
				log.debug("INDEX DATA Count : " + msg.getAppHead().getItemCount());
				if(dedicatedWindThread) {
					futureQueue.add(msg);
					break;
				}				
				if(windGateway != null) {
					for (int i=0; i<msg.getAppHead().getItemCount(); i++) {
							data = TDFClient.getMessageData(msg, i);
							windGateway.receiveIndexData(data.getIndexData());					
						//PrintHelper.printIndexData(data.getIndexData());
					}	
					windGateway.flushAllClientMsgPack();
				}
				/*
				if(System.currentTimeMillis() - LastPrintTime  > 10 * 1000 && msg.getAppHead().getItemCount()>0){
					PrintHelper.printIndexData(TDFClient.getMessageData(msg, 0).getIndexData());
					System.gc();
					LastPrintTime = System.currentTimeMillis();
				}
				PrintHelper.SaveData( new DataInfo(msg,PrintHelper.GetCurrentTime()) );
				*/				
				break;
			case TDF_MSG_ID.MSG_DATA_FUTURE:
				log.debug("FUTURE DATA Count : " + msg.getAppHead().getItemCount());
				if(dedicatedWindThread) {
					futureQueue.add(msg);
					break;
				}				
				if(windGateway != null) {
					for (int i=0; i<msg.getAppHead().getItemCount(); i++) {
						data = TDFClient.getMessageData(msg, i);
						//PrintHelper.printFutureData(data.getFutureData());						
							windGateway.receiveFutureData(data.getFutureData());						
					}					
					windGateway.flushAllClientMsgPack();
				}
				/*
				if(System.currentTimeMillis() - LastPrintTime  > 10 * 1000 && msg.getAppHead().getItemCount()>0){
					PrintHelper.printFutureData(TDFClient.getMessageData(msg, 0).getFutureData());
					System.gc();
					LastPrintTime = System.currentTimeMillis();
				}
				PrintHelper.SaveData( new DataInfo(msg,PrintHelper.GetCurrentTime()));
				*/				
				break;
			case TDF_MSG_ID.MSG_DATA_TRANSACTION:
				log.debug("TRANSACTION DATA Count : " + msg.getAppHead().getItemCount());
				if(dedicatedWindThread) {
					transactionQueue.add(msg);
					break;
				}				
				if(windGateway != null) {
					for (int i=0; i<msg.getAppHead().getItemCount(); i++) {
						data = TDFClient.getMessageData(msg, i);
						//PrintHelper.printTransaction(data.getTransaction());				
						windGateway.receiveTransaction(data.getTransaction());						
					}
					windGateway.flushAllClientMsgPack();

				}

				/*
				if(System.currentTimeMillis() - LastPrintTime  > 10 * 1000 && msg.getAppHead().getItemCount()>0){
					PrintHelper.printTransaction(TDFClient.getMessageData(msg, 0).getTransaction());
					System.gc();
					LastPrintTime = System.currentTimeMillis();
				}
				PrintHelper.SaveData( new DataInfo(msg,PrintHelper.GetCurrentTime()) );
				*/				
				break;
			case TDF_MSG_ID.MSG_DATA_ORDERQUEUE:
				log.debug("ORDERQUEUE DATA Count : " + msg.getAppHead().getItemCount());
				/*
				if (outputToScreen){					
					for (int i=0; i<msg.getAppHead().getItemCount(); i++) {
						TDF_MSG_DATA data = TDFClient.getMessageData(msg, i);
						PrintHelper.printOrderQueue(data.getOrderQueue());
					}										
				}
				*/
				/*
				if(System.currentTimeMillis() - LastPrintTime  > 10 * 1000 && msg.getAppHead().getItemCount()>0){
					PrintHelper.printOrderQueue(TDFClient.getMessageData(msg, 0).getOrderQueue());
					System.gc();
					LastPrintTime = System.currentTimeMillis();
				}
				PrintHelper.SaveData( new DataInfo(msg,PrintHelper.GetCurrentTime()) );
				*/				
				break;
			case TDF_MSG_ID.MSG_DATA_ORDER:
				log.debug("ORDER DATA Count : " + msg.getAppHead().getItemCount());	
				/*
				if (outputToScreen){					
					for (int i=0; i<msg.getAppHead().getItemCount(); i++) {
						TDF_MSG_DATA data = TDFClient.getMessageData(msg, i);
						PrintHelper.printOrder(data.getOrder());
					}							
				}
				*/
				/*
				if(System.currentTimeMillis() - LastPrintTime  > 10 * 1000 && msg.getAppHead().getItemCount()>0){
					PrintHelper.printOrder(TDFClient.getMessageData(msg, 0).getOrder());
					System.gc();
					LastPrintTime = System.currentTimeMillis();
				}
				PrintHelper.SaveData( new DataInfo(msg,PrintHelper.GetCurrentTime()) );
				*/				
				break;
			default:
				break;
			}
		}
		client.close();
	}
			
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if (args.length!=4 ) {
			System.out.println("usage:  Demo ip port user password");
			System.exit(1);
		}
		// Proxy Mode
		//Demo d = new Demo("10.100.7.18", 10001, "dev_test", "dev_test", 
		//			"10.100.6.125", 3128, "", "");
		Demo demo = new Demo(args[0], Integer.parseInt(args[1]), args[2], args[3], 0,null,false,false,"",true);
		DataHandler dh = new DataHandler (demo);
		Thread t1 = new Thread(dh);
		t1.start();	
		DataWrite dw = new DataWrite (demo);
		Thread t2 = new Thread ( dw );
		t2.start();
		System.out.println("press anything to quit the program.");
		try {
			System.in.read();	
			demo.setQuitFlag(true);
			dw.setQuitFlag(true);	
			t1.join();
			System.out.println("Thread1 Quit!");
			t2.join();
			System.out.println("Thread2 Quit!");
		} catch (Exception e) {
			e.printStackTrace();
		}		
		System.out.println("Quit the program!");
		System.exit(0);
	}
}





class DataHandler  implements Runnable {
	protected Demo demo;
	protected boolean quitFlag = false;
	
	private static final Logger log = LoggerFactory
			.getLogger(com.cyanspring.adaptor.future.wind.gateway.DataHandler.class);
	
	public DataHandler ( Demo  d) {
	    this.demo = d;
	}
	
	public void Stop() {
		this.quitFlag = true;
		demo.quitFlag = true;
	}
	
	public void run ( ) {
		while(quitFlag == false)
		{
			try {
				demo.reconnect();
				while(demo.quitFlag == false)
				{
					demo.run();
				}
				log.info("Disconnect with Wind at port : " +  demo.getPort());
				if(quitFlag == false) {
					log.info("will try to reconnect after 5 seconds.");
					Thread.sleep(5000);
				} 
			} catch (InterruptedException e) {
				log.warn(e.getMessage(),e);
			}			
		}
	}
}
class DataWrite  implements Runnable { //д���������ʱ����>WRITE_GAP && �����>LIST_LEN
	private static final int WRITE_GAP = 5;
	private static final int LIST_LEN = 20000;	
	
	public DataWrite ( Demo  d) {
		quitFlag = false;
		lastWriteTime = System.currentTimeMillis();
		this.demo = d;
	}
	private  Demo demo;
	protected Boolean quitFlag;	
	private long lastWriteTime;
	public void setQuitFlag(Boolean para){
		this.quitFlag = para;
		
	}
	public void run ( ) {
		while(!quitFlag ){
			if (System.currentTimeMillis() - lastWriteTime < WRITE_GAP * 1000){
				try{
				    Thread.sleep(2*1000);//�л������˳��ź�
				    continue;
				}catch(Exception e){
					e.printStackTrace();
					System.exit(0);
				}
			}
			if (PrintHelper.IsListFull(LIST_LEN)){
			    PrintHelper.WriteData(demo);
			}
			lastWriteTime = System.currentTimeMillis();
		}
		PrintHelper.WriteData(demo);
		PrintHelper.CloseDataFiles();
	}
}


class MsgProcessor implements Runnable {
	private WindGateway windGateway = null;
	LinkedBlockingQueue<TDF_MSG> queue = null;
	boolean quitFlag = false;
	int maxQueued = 0;
	private long ticks;
	
	private static final Logger log = LoggerFactory
			.getLogger(com.cyanspring.adaptor.future.wind.gateway.MsgProcessor.class);	
	
	public MsgProcessor(WindGateway w,LinkedBlockingQueue<TDF_MSG> q) {
		windGateway = w;
		queue = q;
		ticks = System.currentTimeMillis();
	}
	
	public void Stop() {
		quitFlag = true;
	}
	
	public void run() {		
		//TDF_MSG msg;
		ArrayList<TDF_MSG> msgList = new ArrayList<TDF_MSG>(); 
		int cnt;
		while(!quitFlag) {
			try {
				/*
				do {
					msg = queue.poll(5,TimeUnit.MILLISECONDS);
					if(msg != null) {					
						ProcessMessage(msg);
					}
					if(System.currentTimeMillis() >= ticks + 5000) {
						ticks = System.currentTimeMillis();
						if(queue.size() > maxQueued) {
							maxQueued = queue.size();
							log.info("Max Queued : " + maxQueued);
						}
					}					
				} while(msg != null);
				*/
				//do {
					msgList.clear();
					msgList.add(queue.take());
					cnt = queue.drainTo(msgList) + 1;
					if(cnt > 0) {
						for(TDF_MSG msg : msgList) {
							ProcessMessage(msg);
						}
						if(cnt > maxQueued) {
							maxQueued = cnt;
							log.info("Max Queued : " + maxQueued);
						}						
					} 					
				//} while(cnt != 0);
				//Thread.sleep(2);				
			} catch (Exception e) {
				log.error(e.getMessage(),e);				
			}
		}		
	}
	
	void ProcessMessage(TDF_MSG msg) {
		if(windGateway == null) {
			return;
		}
		TDF_MSG_DATA data;
		
		switch(msg.getDataType()) {
		case TDF_MSG_ID.MSG_DATA_MARKET:
			for (int i=0; i<msg.getAppHead().getItemCount(); i++) {
				data = TDFClient.getMessageData(msg, i); 				
				windGateway.receiveMarketData(data.getMarketData());						
			}			
								
							
			break;
		case TDF_MSG_ID.MSG_DATA_INDEX:			
			for (int i=0; i<msg.getAppHead().getItemCount(); i++) {
				data = TDFClient.getMessageData(msg, i);
				windGateway.receiveIndexData(data.getIndexData());				
			}	
							
			break;
		case TDF_MSG_ID.MSG_DATA_FUTURE:
			for (int i=0; i<msg.getAppHead().getItemCount(); i++) {
				data = TDFClient.getMessageData(msg, i);						
				windGateway.receiveFutureData(data.getFutureData());					
			}				
											
			break;
		case TDF_MSG_ID.MSG_DATA_TRANSACTION:
			for (int i=0; i<msg.getAppHead().getItemCount(); i++) {
				data = TDFClient.getMessageData(msg, i);					
				windGateway.receiveTransaction(data.getTransaction());										
			}
			break;						
		}		
		windGateway.flushAllClientMsgPack();
	}
}