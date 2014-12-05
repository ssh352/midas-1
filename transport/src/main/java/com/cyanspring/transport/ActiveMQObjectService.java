/*******************************************************************************
 * Copyright (c) 2011-2012 Cyan Spring Limited
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms specified by license file attached.
 * 
 * Software distributed under the License is released on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 ******************************************************************************/
package com.cyanspring.transport;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import javassist.bytecode.annotation.ByteMemberValue;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;

import org.apache.activemq.command.ActiveMQBytesMessage;
import org.nustaq.serialization.simpleapi.DefaultCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cyanspring.common.account.Account;
import com.cyanspring.common.event.account.AccountUpdateEvent;
import com.cyanspring.common.transport.IObjectListener;
import com.cyanspring.common.transport.IObjectSender;
import com.cyanspring.common.transport.IObjectTransportService;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class ActiveMQObjectService extends ActiveMQService implements IObjectTransportService {
	static Logger log = LoggerFactory.getLogger(ActiveMQObjectService.class);
	private XStream xstream = new XStream(new DomDriver());
	DefaultCoder coder = new DefaultCoder();

    private HashMap<String, ArrayList<IObjectListener>> objSubscribers = new HashMap<String, ArrayList<IObjectListener>>();
    private HashMap<IObjectListener, MessageConsumer> objConsumers = new HashMap<IObjectListener, MessageConsumer>();

    
    class ObjectListenerAdaptor implements MessageListener {
    	
    	private IObjectListener listener;
    	public ObjectListenerAdaptor (IObjectListener listener) {
    		this.listener = listener;
    	}
    	
		@Override
		public void onMessage(Message message) {
            if (message instanceof TextMessage) {
            	try {
            		String str = ((TextMessage) message).getText();
            		log.debug("Received message: \n" + str);
            		Object obj = xstream.fromXML(str);
					listener.onMessage(obj);
				} catch (JMSException e) {
					log.error(e.getMessage(), e);
					e.printStackTrace();
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					e.printStackTrace();
				}
            }
            else if(message instanceof BytesMessage) {
            	try {
            		BytesMessage bms = (BytesMessage)message;
            		byte[] bs = new byte[(int)bms.getBodyLength()];
            		bms.readBytes(bs, bs.length);
            		Object obj = coder.toObject(bs);
					listener.onMessage(obj);
					
					if(log.isDebugEnabled())
					{
						String str = xstream.toXML(obj);
						log.debug("Received message: \n" + str);
					}
				} catch (JMSException e) {
					log.error(e.getMessage(), e);
					e.printStackTrace();
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					e.printStackTrace();
				}
            }
            else {
            	log.error("Unexpected text message: " + message);
            }
			
		}
    	
    }
    
    class ObjectSender implements IObjectSender {
    	private MessageProducer producer;
    	
    	public ObjectSender(MessageProducer producer) {
    		this.producer = producer;
    	}

		@Override
		public void sendMessage(Object obj) throws Exception {
			System.out.println(obj.getClass().getName());
			byte[] bs = coder.toByteArray(obj);
			BytesMessage message = session.createBytesMessage();
			message.writeBytes(bs);
			
			if(log.isDebugEnabled())
			{
				String xmlmsg = xstream.toXML(obj);
				log.debug("Sending message: \n" + xmlmsg);
			}
			//TextMessage txt = session.createTextMessage(message);
			producer.send(message);
		}
    	
    }
    
	@Override
	public void createReceiver(String subject, IObjectListener listener)
			throws Exception {
		//only one listener per subject allowed for point to point connection
		MessageConsumer consumer = receivers.get(subject);
		if (null == consumer) {
			Destination dest = session.createQueue(subject);
			consumer = session.createConsumer(dest);
			receivers.put(subject, consumer);
		}
		if (null == listener)
			consumer.setMessageListener(null);
		else
			consumer.setMessageListener(new ObjectListenerAdaptor(listener));
	}

	@Override
	public void createSubscriber(String subject, IObjectListener listener)
			throws Exception {
		//many listeners per subject allowed for publish/subscribe
		ArrayList<IObjectListener> listeners = objSubscribers.get(subject);
		if (null == listeners) {
			listeners = new ArrayList<IObjectListener>();
			objSubscribers.put(subject, listeners);
		}
		
		if (!listeners.contains(listener)) {
			Destination dest = session.createTopic(subject);
			MessageConsumer consumer = session.createConsumer(dest);
			consumer.setMessageListener(new ObjectListenerAdaptor(listener));
			listeners.add(listener);
			objConsumers.put(listener, consumer);
		}
	}

	@Override
	public void removeSubscriber(String subject, IObjectListener listener)
			throws Exception {
		ArrayList<IObjectListener> listeners = objSubscribers.get(subject);
		if (null == listeners)
			return;

		if (listeners.contains(listener)) {
			MessageConsumer consumer = objConsumers.get(listener);
			if (null == consumer)
				return;
			consumer.setMessageListener(null);
			objConsumers.remove(listener);
			listeners.remove(listener);
		}
		
	}

	@Override
	public void sendMessage(String subject, Object obj) throws Exception {
		createObjectSender(subject).sendMessage(obj);
		
	}

	@Override
	public void publishMessage(String subject, Object obj) throws Exception {
		createObjectPublisher(subject).sendMessage(obj);
		
	}

	@Override
	public IObjectSender createObjectSender(String subject) throws Exception {
		MessageProducer producer = senders.get(subject);
		if (null == producer) {
			Destination dest = session.createQueue(subject);
	        producer = session.createProducer(dest);
	        producer.setDeliveryMode(persistent);
	        senders.put(subject, producer);
        }

		return new ObjectSender(producer);
	}

	@Override
	public IObjectSender createObjectPublisher(String subject) throws Exception {
		MessageProducer producer = publishers.get(subject);
		if (null == producer) {
			Destination dest = session.createTopic(subject);
	        producer = session.createProducer(dest);
	        producer.setDeliveryMode(persistent);
	        publishers.put(subject, producer);
        }

		return new ObjectSender(producer);
	}

	/*
	public static void main(String[] argv)
	{
		DefaultCoder coder = new DefaultCoder();
		AccountUpdateEvent event = new AccountUpdateEvent("123", "456", new Account("abc", "abccc"));
		Object o;
		
		long pretime = Calendar.getInstance().getTimeInMillis();
		
		for(int i=0 ; i<1000 ; i++)
		{
			byte[] bs = coder.toByteArray(event);
			o = coder.toObject(bs);
		}
		
		long posttime = Calendar.getInstance().getTimeInMillis();
		System.out.println("FST=[" + (posttime-pretime) + "]");
		
		XStream xstream = new XStream(new DomDriver());
		
		pretime = Calendar.getInstance().getTimeInMillis();
		for(int i=0 ; i<1000 ; i++)
		{
			String xml = xstream.toXML(event);
			o = xstream.fromXML(xml);
		}
		posttime = Calendar.getInstance().getTimeInMillis();
		System.out.println("XML=[" + (posttime-pretime) + "]");
	}
	*/
}
