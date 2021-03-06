package com.cyanspring.event.api.obj.reply;

import com.cyanspring.apievent.obj.OpenPosition;
import com.cyanspring.event.api.ApiResourceManager;
import com.cyanspring.common.event.account.OpenPositionUpdateEvent;
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
public class ApiOpenPositionUpdateEvent implements IApiReply{

    private ApiResourceManager resourceManager;

    @Override
    public void sendEventToClient(Object event) {
        OpenPositionUpdateEvent updateEvent = (OpenPositionUpdateEvent) event;
        resourceManager.sendEventToUser(updateEvent.getPosition().getUser(),
                new com.cyanspring.apievent.reply.OpenPositionUpdateEvent(
                        updateEvent.getKey(),
                        updateEvent.getReceiver(),
                        setPositionsData(updateEvent.getPosition())
                ));
    }

    private OpenPosition setPositionsData(com.cyanspring.common.account.OpenPosition position){
            OpenPosition newPosition = new OpenPosition();
            newPosition.setAccount(position.getAccount());
            newPosition.setAcPnL(position.getAcPnL());
            newPosition.setCreated(position.getCreated());
            newPosition.setId(position.getId());
            newPosition.setMargin(position.getMargin());
            newPosition.setPnL(position.getPnL());
            newPosition.setPrice(position.getPrice());
            newPosition.setQty(position.getQty());
            newPosition.setSymbol(position.getSymbol());
            newPosition.setUser(position.getUser());
        return newPosition;
    }

    @Override
    public void setResourceManager(ApiResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }
}
