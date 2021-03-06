﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Common.Transport;
using Apache.NMS.ActiveMQ;
using Apache.NMS;
using Common.Logging;

namespace Transport.Transport
{
    class ActiveMQService : ITransportService
    {
        private static ILog logger = LogManager.GetLogger(typeof(ActiveMQService));

        // ActiveMQ configuration parameters
        public string user{set;get;}
        public string password{set;get;}
        public string url{set;get;} = "nio://localhost:61616";
        public MsgDeliveryMode persistent { set; get; } = MsgDeliveryMode.NonPersistent;
        public bool transacted { set; get; }
        public AcknowledgementMode ackMode { set; get; } = AcknowledgementMode.AutoAcknowledge;
        private long memoryLimit = 128 * 1024 * 1024;

        // members
        protected IConnection connection;
        protected ISession session;


        // topic
        public string senderQueue { set; get; } = "q2";
        public string receiveQueue { set; get; } = "q1";

        public string publishTopic { set; get; } = "t2";
        public string subscribeTopic { set; get; } = "t1";

        protected Dictionary<string, IMessageConsumer> receivers = new Dictionary<string, IMessageConsumer>();
        protected Dictionary<string, IMessageProducer> senders = new Dictionary<string,IMessageProducer>();
        protected Dictionary<string, IMessageProducer> publishers = new Dictionary<string, IMessageProducer>();

        private Dictionary<string, List<IMessageListener>> subscribers = new Dictionary<string, List<IMessageListener>>(); 
        private Dictionary<IMessageListener, IMessageConsumer> consumers = new Dictionary<IMessageListener, IMessageConsumer>();

        private IMessageListener receiverListener ;

        class Sender : ISender
        {
            private ActiveMQService service_;
            private IMessageProducer producer_;
            public Sender(ActiveMQService service, IMessageProducer producer)
            {
                this.service_ = service;
                this.producer_ = producer;
            }

            public void SendMessage(string message)
            {
                ITextMessage txt = service_.session.CreateTextMessage(message);
                producer_.Send(txt);
            }
        }

        public void StartBroker()
        {
            throw new NotImplementedException();
        }

        public void CloseBroker()
        {
            throw new NotImplementedException();
        }

        public void StartService()
        {
            ConnectionFactory connectionFactory = new ConnectionFactory(url);
            connection = connectionFactory.CreateConnection(user, password);
            connection.Start();
            connection.ExceptionListener += new ExceptionListener(connection_ExceptionListener);
            session = connection.CreateSession(ackMode);
            logger.Info("Connection Start...");
        }

        private void connection_ExceptionListener(Exception e)
        {
            // log exception
        }

        public void CloseService()
        {
            removeAllReceivers();
            session.Close();
            connection.Close();
            logger.Info("Connection Closed...");
        }

        private void removeAllReceivers() {
            foreach (IMessageConsumer cum in receivers.Values)
            {
                cum.Listener -= consumer_MessageListener1;
            }
	    }

        public Common.Transport.ISender CreateSender()
        {
            if (!senders.ContainsKey(senderQueue))
            {
                IDestination dest = session.GetQueue(senderQueue);
                IMessageProducer newProducer = session.CreateProducer(dest);
                newProducer.DeliveryMode = persistent;
                senders.Add(senderQueue, newProducer);
            }
            IMessageProducer producer = senders[senderQueue];
            return new Sender(this, producer);
        }

        public void CreateReceiver(Common.Transport.IMessageListener listener)
        {
            if (!receivers.ContainsKey(receiveQueue))
            {
                IDestination dest = session.GetQueue(receiveQueue);
                IMessageConsumer newConsumer = session.CreateConsumer(dest);
                receivers.Add(receiveQueue, newConsumer);
            }
            IMessageConsumer consumer = receivers[receiveQueue];
            if (listener == null)
            {
                consumer.Listener -= consumer_MessageListener1;
            }
            else
            {
                this.receiverListener = listener;           
                consumer.Listener += new MessageListener(consumer_MessageListener1);
            }
        }

        private void consumer_MessageListener1(IMessage message)
        {
           
            if (message is ITextMessage)
            {
                ITextMessage txt = (ITextMessage) message;
                receiverListener.OnMessage(txt.Text);
            }
            else
            {
                // log error
            }
        }

        public void RemoveReceiver()
        {
            if (!receivers.ContainsKey(receiveQueue))
            {
                return;
            }
            IMessageConsumer consumer = receivers[receiveQueue];
            consumer.Listener -= consumer_MessageListener1;
        }

        public Common.Transport.ISender CreatePublisher()
        {
            if (!publishers.ContainsKey(publishTopic)) {
                IDestination dest = session.GetTopic(publishTopic);
                IMessageProducer newProducer = session.CreateProducer(dest);
                newProducer.DeliveryMode = persistent;
                publishers.Add(publishTopic, newProducer);
            }
            IMessageProducer producer = publishers[publishTopic];
            return new Sender(this, producer);
        }

        public void CreateSubscriber( Common.Transport.IMessageListener listener)
        {
            if (!subscribers.ContainsKey(subscribeTopic)) 
            {
                subscribers.Add(subscribeTopic, new List<IMessageListener>());
            }
            List<IMessageListener> listeners = subscribers[subscribeTopic];
            if (!listeners.Contains(listener))
            {
                IDestination dest = session.GetTopic(subscribeTopic);
                IMessageConsumer consumer = session.CreateConsumer(dest);
                consumer.Listener += new MessageListener(consumer_MessageListener2);
                listeners.Add(listener);
                consumers[listener] = consumer;
            }
            
        }

        private void consumer_MessageListener2(IMessage message)
        {
            if (message is ITextMessage)
            {
                ITextMessage txt = (ITextMessage)message;
                foreach(IMessageListener listener in subscribers[subscribeTopic])
                {
                    listener.OnMessage(txt.Text);
                }               
            }
            else
            {
                // log error
            }
        }

        public void RemoveSubscriber( Common.Transport.IMessageListener listener)
        {
            if (!subscribers.ContainsKey(subscribeTopic))
            {
                return;
            }
            List<IMessageListener> listeners = subscribers[subscribeTopic];
            if (listeners.Contains(listener))
            {
                if (!consumers.ContainsKey(listener))
                {
                    return;
                }
                IMessageConsumer consumer = consumers[listener];
                consumer.Listener -= consumer_MessageListener2;
                consumers.Remove(listener);
                listeners.Remove(listener);
            }
        }

        public void SendMessage( string message)
        {
            CreateSender().SendMessage(message);
        }

        public void PublishMessage( string message)
        {
            CreatePublisher().SendMessage(message);
        }

        
    }
}
