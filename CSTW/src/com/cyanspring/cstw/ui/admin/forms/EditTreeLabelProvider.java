package com.cyanspring.cstw.ui.admin.forms;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.cyanspring.cstw.common.ImageID;
import com.cyanspring.cstw.gui.Activator;
import com.cyanspring.cstw.model.admin.ExchangeAccountModel;
import com.cyanspring.cstw.model.admin.SubAccountModel;

/**
 * @author Junfeng
 * @create 9 Nov 2015
 */
public class EditTreeLabelProvider extends LabelProvider {

	@Override
	public String getText(Object element) {
		if (element instanceof ExchangeAccountModel) {
			return ((ExchangeAccountModel) element).getName();
		} 
		if (element instanceof SubAccountModel) {
			return ((SubAccountModel) element).getName();
		}
		return super.getText(element);
	}
	
	@Override
	public Image getImage(Object element) {
		if (element instanceof ExchangeAccountModel) {
			return Activator.getDefault().getImageRegistry()
					.getDescriptor(ImageID.EXCHANGEACCOUNT_ICON.toString())
					.createImage();

		}
		if (element instanceof SubAccountModel) {
			return Activator.getDefault().getImageRegistry()
					.getDescriptor(ImageID.SUBACCOUNT_ICON.toString())
					.createImage();
		}
		return super.getImage(element);
	}
}
