package com.cyanspring.cstw.ui.brw.composite;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.cyanspring.cstw.service.common.RefreshEventType;
import com.cyanspring.cstw.service.iservice.IBasicService;
import com.cyanspring.cstw.service.iservice.ServiceFactory;
import com.cyanspring.cstw.service.iservice.riskmgr.ICurrentPositionService;
import com.cyanspring.cstw.ui.basic.BasicComposite;
import com.cyanspring.cstw.ui.brw.composite.table.BRWPositionTableComposite;

/**
 * @author Junfeng
 * @create 27 Oct 2015
 */
public class BRWPositionComposite extends BasicComposite {
	
	private ICurrentPositionService service;
	
	private BRWPositionTableComposite tableComposite;
	
	private Label lblAllMarketCapitalization;	// 总市值
	private Label lblUnrealizedPL;				// 浮盈
	private Label lblAllPNL;					// 总盈利
	
	public BRWPositionComposite(Composite parent, int style) {
		super(parent, style);
		initComponent();
		initQuery();
	}

	private void initComponent() {
		GridLayout grid = new GridLayout(1, false);
		grid.verticalSpacing = 0;
		grid.horizontalSpacing = 0;
		grid.marginWidth = 0;
		grid.marginHeight = 0;
		setLayout(grid);
		
		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayout(new GridLayout(8, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label lbl1 = new Label(composite, SWT.NONE);
		lbl1.setAlignment(SWT.RIGHT);
		GridData gd1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		lbl1.setLayoutData(gd1);
		lbl1.setText("Total Market: ");
		
		lblAllMarketCapitalization = new Label(composite, SWT.NONE);
		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		lblAllMarketCapitalization.setLayoutData(gd2);
		
		Label lbl2 = new Label(composite, SWT.NONE);
		lbl2.setAlignment(SWT.RIGHT);
		lbl2.setLayoutData(gd1);
		lbl2.setText("Unrealized PnL: ");
		
		lblUnrealizedPL = new Label(composite, SWT.NONE);
		lblUnrealizedPL.setLayoutData(gd2);
		
		Label lbl4= new Label(composite, SWT.NONE);
		lbl4.setAlignment(SWT.RIGHT);
		lbl4.setLayoutData(gd1);
		lbl4.setText("Total PnL: ");
		
		lblAllPNL = new Label(composite, SWT.NONE);
		lblAllPNL.setLayoutData(gd2);
		
		tableComposite = new BRWPositionTableComposite(this, SWT.NONE);
		GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		tableComposite.setLayoutData(gd_table);
		
	}

	private void initQuery() {
		service.queryOpenPosition();
	}

	
	@Override
	protected void processByType(RefreshEventType type) {
		if (type == RefreshEventType.RWCurrentPositionList) {
			tableComposite.setInput(service.getOpenPositionModelList());
			lblAllMarketCapitalization.setText(service.getAllMarketCapitalization());
			lblUnrealizedPL.setText(service.getUnrealizedPNL());
			lblAllPNL.setText(service.getAllPNL());
		}
	}

	@Override
	protected IBasicService createService() {
		service = ServiceFactory.createBWCurrentPositionService();
		return service;
	}

}
