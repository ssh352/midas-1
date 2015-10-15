package com.cyanspring.cstw.trader.gui.composite.speeddepth.provider;

import java.util.List;

import com.cyanspring.cstw.trader.gui.basic.DefaultContentProviderAdapter;

/**
 * 
 * @author NingXiaoFeng
 * @create date 2015/10/08
 *
 */
public final class SpeedDepthContentProvider extends
		DefaultContentProviderAdapter {

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof List<?>) {
			return ((List<?>) inputElement).toArray();
		}
		return null;
	}

}