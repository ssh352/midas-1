package com.cyanspring.server.alert;
import java.io.DataOutputStream;
import java.net.URL;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParseThread extends Thread{
	private String strThreadId = "";
	private String strParseApplicationId = "";
	private String strParseRESTAPIKey = "";
	ThreadStatus threadStatus ;
	private int timeoutSecond;
	private int retryTimes;
	private ConcurrentLinkedQueue<ParseData> ParseDataQueue ;
	
	private boolean startThread;
	private static final Logger log = LoggerFactory
			.getLogger(ParseThread.class);	

	//********************************************************************************************
    //Dev
    //X-Parse-Application-Id = Rmttn76LwwEockQ9x6CiWaTqNyMuqfu9OMUC1Ejt
    //X-Parse-REST-API-KEY = RzndFe8IHT4iVszcpUAedsqGpors63ADlAvQIdsG
    //Test
    //X-Parse-Application-Id = G8513GzfsKMt1my24Ozq6w7RF2tEuJGYOXGWCV0D
    //X-Parse-REST-API-KEY = kb1xamZbPR2sbh7eIuKGCVQtIIyKXsFivLJWB2v0
    //UAT
	//X-Parse-Application-Id = Ek9KqTvVcJYTtZPzsUF6KxqaZJ5vRrYJ88UkrIbX 
	//X-Parse-REST-API-KEY = iNKzjbL02cEiZapYGcCEiWlsvutdQp89vEfHcZOm
    //Prod
    //X-Parse-Application-Id = 74bgubpHxaoHYAPqx5jgNIa4x6G85NPZ6pBvJwp2 
    //X-Parse-REST-API-KEY = asld9nAPrLLPJQM9qlS3SegDE4sTkMfroUEVqBEp
    //********************************************************************************************	
	
	public ParseThread(String strThreadId, ConcurrentLinkedQueue<ParseData> parseDataQueue, int timeoutSecond, int retryTimes, String ParseApplicationId, String ParseRESTAPIKey)	
	{
		try
		{
			if (ParseApplicationId == "" || ParseRESTAPIKey == "")
			{
				return ;
			}
			this.startThread = true ;
	        this.strParseApplicationId = ParseApplicationId;
	        this.strParseRESTAPIKey = ParseRESTAPIKey;
	        this.ParseDataQueue = parseDataQueue ;
	        this.timeoutSecond = timeoutSecond;
	        this.strThreadId = strThreadId;
	        this.retryTimes = retryTimes ;
	        this.threadStatus = new ThreadStatus();
			setDaemon(true);
		}
		catch(Exception e)
		{
			log.warn("ParseThread Exception : " + e.getMessage()) ;
		}
	}
	
	@Override
	public void run()
	{
		try
		{
			boolean bReSend = false ;
			log.info(this.strThreadId + " Start.");
			int iRetrytimes = 0;
			ParseData PD  = null;
			while (startThread)
			{
				if (!bReSend)
				{
					PD = ParseDataQueue.poll();
				}
				if (PD == null)
				{
					threadStatus.setThreadState(ThreadState.IDLE) ;
					threadStatus.UpdateTime();
					Thread.sleep(300);		
					continue;
				}
				try
				{
					bReSend = false ;
//					log.debug("[ParseThread "+strThreadId+"] sending"+PD.strpushMessage);
					threadStatus.setThreadState(ThreadState.SENDDING) ;
					threadStatus.UpdateTime();
					sendPost(PD) ;
				}
				catch (Exception ec)
				{
					log.warn(strThreadId + " SendPost Exception : " + ec.getMessage());
					iRetrytimes ++ ;
					if (iRetrytimes <= retryTimes)
					{
						log.warn("[ParseThread "+strThreadId+"] sending again "+PD.strpushMessage);
						threadStatus.setThreadState(ThreadState.SENDDING) ;
						threadStatus.UpdateTime();
						bReSend = true ;
					}
					else
					{
						log.warn(strThreadId + " Retrytimes out : " + PD.getMsg());
					}
				}
			}
		}
		catch (Exception e)
		{
			log.warn(strThreadId + " Exception : " + e.getMessage());
		}
	}
	
	protected void sendPost(ParseData PD) throws Exception 
	{   	 		
		URL obj = new URL("https://api.parse.com/1/push") ;
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
		
		String strPoststring = "{ \"where\": {\"userId\": \"" + PD.strUserId + "\" , \"deviceType\": { \"$in\": [ \"ios\", \"android\", \"winphone\", \"js\" ] }}, " +
                "\"data\": {\"alert\": \"" + PD.strpushMessage + "\",\"8004\":\"" + PD.strMsgId + "\",\"8006\":\"" + PD.strMsgType +
                "\",\"action\":\"" + "com.hkfdt.activity.UPDATE_STATUS" + ((PD.strLocalTime.length() > 0) ? ("\",\"1014\":\"" + PD.strLocalTime) : "") + 
                ((PD.strKeyValue.length() > 0) ? ("\",\"8007\":\"" + PD.strKeyValue) : "") + "\"" +
                ", \"badge\": \"Increment\"}}";
 
		log.debug("Parse message: " + strPoststring);
		//add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("X-Parse-Application-Id", strParseApplicationId);
		con.setRequestProperty("X-Parse-REST-API-KEY", strParseRESTAPIKey);
		con.setRequestProperty("Content-type", "application/json");
		con.setRequestProperty("Content-Length", Integer.toString(strPoststring.length()));
		con.setConnectTimeout(timeoutSecond);
		con.setReadTimeout(timeoutSecond);
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(strPoststring);
		wr.flush();
		wr.close();
		
		int responseCode = con.getResponseCode();
		con.disconnect();
		
		log.debug("Return code: " + responseCode);
	}
	
	public String getThreadId()
	{
		return strThreadId;
	}
	
	public void setstartThread(boolean startThread)
	{
		this.startThread = startThread  ;
	}
	
	public ThreadStatus getThreadStatus()
	{
		return threadStatus ;
	}
	
//	public static void main(String[] args)
//	{
//		ParseThread PT = new ParseThread("Rmttn76LwwEockQ9x6CiWaTqNyMuqfu9OMUC1Ejt", "RzndFe8IHT4iVszcpUAedsqGpors63ADlAvQIdsG") ;
//		PT.PushNotification("david", "", "Test Hi David", "rqgergqerg", "2", "2015-01-14 10:49:55", "ewfwe") ;
//		
//		while(true)
//		{
//			
//		}
//	}
}
