package com.cyanspring.common.livetrading;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cyanspring.common.account.Account;
import com.cyanspring.common.account.OpenPosition;
import com.cyanspring.common.account.OrderReason;
import com.cyanspring.common.business.OrderField;
import com.cyanspring.common.business.ParentOrder;
import com.cyanspring.common.event.IRemoteEventManager;
import com.cyanspring.common.event.order.CancelStrategyOrderEvent;
import com.cyanspring.common.event.order.ClosePositionRequestEvent;
import com.cyanspring.common.marketdata.IQuoteChecker;
import com.cyanspring.common.marketdata.PriceQuoteChecker;
import com.cyanspring.common.marketdata.Quote;
import com.cyanspring.common.order.RiskOrderController;
import com.cyanspring.common.position.PositionKeeper;
import com.cyanspring.common.util.IdGenerator;
import com.cyanspring.common.util.PriceUtils;

public class TradingUtil {
	private static final Logger log = LoggerFactory
			.getLogger(TradingUtil.class);

	private static IQuoteChecker quoteChecker = new PriceQuoteChecker();

	private static boolean isValidQuote(Quote quote) {
		if (null != quoteChecker && !quoteChecker.check(quote))
			return false;
		return !quote.isStale();
	}

	public static void cancelAllOrders(Account account,
			PositionKeeper positionKeeper, IRemoteEventManager eventManager,
			OrderReason orderReason, RiskOrderController riskOrderController) {

		List<ParentOrder> orderList = positionKeeper.getParentOrders(account
				.getId());
		if (orderList.size() > 0)
			log.info("{} Cancelling of orders:{} ", account.getId(),
					orderList.size());
		for (ParentOrder order : orderList) {
			if (order.getOrdStatus().isReady()) {

				if (!TradingUtil.checkRiskOrderCount(riskOrderController,
						account.getId()))
					return;

				String source = order.get(String.class,
						OrderField.SOURCE.value());
				String txId = order.get(String.class,
						OrderField.CLORDERID.value());

				CancelStrategyOrderEvent cancel = new CancelStrategyOrderEvent(
						order.getId(), order.getSender(), txId, source,
						orderReason, false);
				eventManager.sendEvent(cancel);
			}
		}
	}

	public static void closeOpenPositions(Account account,
			PositionKeeper positionKeeper, IRemoteEventManager eventManager,
			boolean checkValidQuote, OrderReason orderReason,
			RiskOrderController riskOrderController) {

		List<OpenPosition> positionList = positionKeeper
				.getOverallPosition(account);
		if (positionList.size() <= 0)
			return;
		log.info("{} Closing of positions:{} ", account.getId(),
				positionList.size());
		for (OpenPosition position : positionList) {
			if (PriceUtils.Equal(position.getAvailableQty(), 0)) {
				log.info("Position: " + position.getSymbol()
						+ ", available qty is zero, skip it");
				continue;
			}
			Quote quote = positionKeeper.getQuote(position.getSymbol());
			if (checkValidQuote && !isValidQuote(quote)) {
				log.info(" invalid quote:{}", quote.getSymbol());
				continue;
			}

			if (positionKeeper.checkAccountPositionLock(account.getId(),
					position.getSymbol())) {
				log.debug("Position stop loss over threshold but account is locked: "
						+ account.getId() + ", " + position.getSymbol());
				continue;
			}

			if (!TradingUtil.checkRiskOrderCount(riskOrderController,
					account.getId()))
				return;

			log.info("Position loss over threshold, cutting loss: "
					+ position.getAccount() + ", " + position.getSymbol()
					+ ", " + position.getAcPnL() + ", " + quote);
			ClosePositionRequestEvent event = new ClosePositionRequestEvent(
					position.getAccount(), null, position.getAccount(),
					position.getSymbol(), 0.0, orderReason, IdGenerator
							.getInstance().getNextID(), true);

			eventManager.sendEvent(event);
		}
	}

	public static double getMinValue(double x, double y) {

		if (PriceUtils.isZero(x)) {
			return y;
		} else if (PriceUtils.isZero(y)) {
			return x;
		} else {
			return Math.min(x, y);
		}

	}

	public static boolean checkRiskOrderCount(
			RiskOrderController riskOrderController, String account) {
		if (null == riskOrderController || riskOrderController.check(account))
			return true;

		return false;
	}

	public static void closeAllPositoinAndOrder(Account account,
			PositionKeeper positionKeeper, IRemoteEventManager eventManager,
			boolean checkValidQuote, OrderReason orderReason,
			RiskOrderController riskOrderController) {

		TradingUtil.cancelAllOrders(account, positionKeeper, eventManager,
				orderReason, riskOrderController);
		TradingUtil.closeOpenPositions(account, positionKeeper, eventManager,
				checkValidQuote, orderReason, riskOrderController);
	}

}
