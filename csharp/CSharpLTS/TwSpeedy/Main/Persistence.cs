﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Common.Utils;
using System.IO;
using System.Collections.Concurrent;
using Common.Adaptor;
using Common.Logging;

namespace Adaptor.TwSpeedy.Main
{
    class Persistence
    {
        private static ILog logger = LogManager.GetLogger(typeof(Persistence));

        public string dir { get; set; } = "data";
        public string file { get; set; } = "order.dat";
        private AsyncQueueProcessor<PersistItem> processor;
        private ConcurrentDictionary<string, PersistItem> items = new ConcurrentDictionary<string, PersistItem>();
        private StreamWriter stream;

        public void init()
        {
            processor = new AsyncQueueProcessor<PersistItem>(new AsyncQueueProcessor<PersistItem>.AsyncQueueHandler(process));
            processor.init();
            load();
        }

        public void uninit()
        {
            stream.Close();
            processor.uninit();
        }

        public void load()
        {
            items.Clear();
            Directory.CreateDirectory(dir);
            string fileName = dir + "\\" + file;
            if (File.Exists(fileName))
            {
                string line;
                long count = 0;
                System.IO.StreamReader file =
                   new System.IO.StreamReader(fileName);
                while ((line = file.ReadLine()) != null)
                {
                    ++count;
                    try
                    {
                        PersistItem item = PersistItem.deserialize(line);
                        if(null == item)
                        {
                            logger.Error("persist data error in line " + count);
                            continue;
                        }
                        else
                        {
                            if(item.time.Date == DateTime.Today)
                            {
                                items[item.exchangeOrderId] = item;
                            }
                        }

                    }
                    catch(Exception e)
                    {
                        logger.Error("persist data error in line " + count);
                        logger.Error(e);
                        logger.Error(e.StackTrace);
                    }
                } // end while
                file.Close();
            }

            FileStream fs = new FileStream(fileName, FileMode.Create);
            stream = new System.IO.StreamWriter(fs);
            // write it back so we can get rid of yesterday's order records
            foreach (var item in items)
            {
                stream.WriteLine(item.Value.serialize());
            }
            stream.Flush();
        }

        public void save(Order order)
        {
            if (items.ContainsKey(order.exchangeOrderId))
                return;
            PersistItem item = new PersistItem(DateTime.Now, order.exchangeOrderId, 
                order.orderId, order.symbol, order.ordStatus.ToString(), order.account);
            items.TryAdd(order.exchangeOrderId, item);
            processor.add(item);
        }

        private void process(PersistItem item)
        {
            stream.WriteLine(item.serialize());
            stream.Flush();
        }

        public PersistItem getItem(string exchangeOrderId)
        {
            PersistItem result = null;
            items.TryGetValue(exchangeOrderId, out result);
            return result;
        }
    }
}
