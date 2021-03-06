﻿using Common.Adaptor;
using Common.Basic;
using Common.Logging;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Common.Adaptor
{
    public class DownStreamManager : IPlugin, IDownStreamManager
    {
        private static ILog logger = LogManager.GetLogger(typeof(DownStreamManager));

        public virtual List<IDownStreamAdaptor> adaptors { set; get; }
        private Dictionary<string, IDownStreamAdaptor> adaptorsMap = new Dictionary<string, IDownStreamAdaptor>();

        public DownStreamManager(List<IDownStreamAdaptor> adaptors)
        {
            this.adaptors = adaptors;
            foreach(IDownStreamAdaptor adaptor in adaptors)
            {
                adaptorsMap.Add(adaptor.id, adaptor);
            }
        }

        public void Init()
        {
            //debug
            logger.Info("Init DownStreamManager...");
            foreach(IDownStreamAdaptor adaptor in adaptors )
            {
                adaptor.init();
            }
        }

        public void UnInit()
        {
            foreach (IDownStreamAdaptor adaptor in adaptors)
            {
                adaptor.addListener(null);
                adaptor.uninit();
            }
        }

        public IDownStreamAdaptor getAdaptorById(string id)
        {
            if (!adaptorsMap.ContainsKey(id))
            {
                return null;
            }
            return adaptorsMap[id];
        }
    }
}
