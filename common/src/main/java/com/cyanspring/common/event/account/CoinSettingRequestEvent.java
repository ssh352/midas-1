package com.cyanspring.common.event.account;

import java.util.Date;

import com.cyanspring.common.business.CoinType;
import com.cyanspring.common.event.RemoteAsyncEvent;

public class CoinSettingRequestEvent extends RemoteAsyncEvent {

    private static final long serialVersionUID = 1L;
    private String txId;
    private String userId;
    private Date endDate;
    private CoinType coinType;
    private String clientId;
    private String market;

    // used by AppServer
    public CoinSettingRequestEvent(String key, String receiver, String txId,
                                   String userId, Date endDate, CoinType coinType, String clientId) {
        this(key, receiver, txId, userId, endDate, coinType, clientId, null);
    }

    // used by batch
    public CoinSettingRequestEvent(String key, String receiver, String txId,
                                   String userId, Date endDate, CoinType coinType, String clientId, String market) {
        super(key, receiver);
        this.txId = txId;
        this.userId = userId;
        this.endDate = endDate;
        this.coinType = coinType;
        this.clientId = clientId;
        this.market = market;
    }

    public String getTxId() {
        return txId;
    }

    public Date getEndDate() {
        return endDate;
    }

    public CoinType getCoinType() {
        return coinType;
    }

    public String getUserId() {
        return userId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getMarket() {
        return market;
    }

    @Override
    public String toString() {
        return "CoinSettingRequestEvent{" +
                "txId='" + txId + '\'' +
                ", userId='" + userId + '\'' +
                ", endDate=" + endDate +
                ", coinType=" + coinType +
                ", clientId='" + clientId + '\'' +
                ", market='" + market + '\'' +
                '}';
    }
}
