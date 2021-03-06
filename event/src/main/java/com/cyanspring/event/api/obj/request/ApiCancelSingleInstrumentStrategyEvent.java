package com.cyanspring.event.api.obj.request;

import com.cyanspring.apievent.request.CancelSingleInstrumentStrategyEvent;
import com.cyanspring.event.api.ApiResourceManager;
import com.cyanspring.common.transport.IUserSocketContext;
import com.cyanspring.common.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Description....
 * <ul>
 * <li> Description
 * </ul>
 * <p/>
 * Description....
 * <p/>
 * Description....
 * <p/>
 * Description....
 *
 * @author elviswu
 * @version %I%, %G%
 * @since 1.0
 */
public class ApiCancelSingleInstrumentStrategyEvent implements IApiRequest{

    private ApiResourceManager resourceManager;

    @Override
    public void sendEventToLts(Object event, IUserSocketContext ctx) {
        CancelSingleInstrumentStrategyEvent strategyEvent = (CancelSingleInstrumentStrategyEvent) event;

        String txId = IdGenerator.getInstance().getNextID();
        resourceManager.putPendingRecord(txId, strategyEvent.getTxId(), ctx);

        com.cyanspring.common.event.strategy.CancelSingleInstrumentStrategyEvent request =
                new com.cyanspring.common.event.strategy.CancelSingleInstrumentStrategyEvent(
                        strategyEvent.getKey(), strategyEvent.getReceiver(), txId);

        resourceManager.sendEventToManager(request);
    }

    @Override
    public void setResourceManager(ApiResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }
}
