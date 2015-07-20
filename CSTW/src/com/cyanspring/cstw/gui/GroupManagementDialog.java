package com.cyanspring.cstw.gui;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cyanspring.common.account.Account;
import com.cyanspring.common.account.User;
import com.cyanspring.common.account.UserGroup;
import com.cyanspring.common.business.GroupManagement;
import com.cyanspring.common.event.AsyncEvent;
import com.cyanspring.common.event.IAsyncEventListener;
import com.cyanspring.common.event.RemoteAsyncEvent;
import com.cyanspring.common.event.account.CreateGroupManagementEvent;
import com.cyanspring.common.event.account.CreateGroupManagementReplyEvent;
import com.cyanspring.common.event.account.DeleteGroupManagementEvent;
import com.cyanspring.common.event.account.DeleteGroupManagementReplyEvent;
import com.cyanspring.common.event.account.GroupManageeReplyEvent;
import com.cyanspring.common.event.account.GroupManageeRequestEvent;
import com.cyanspring.common.message.MessageBean;
import com.cyanspring.common.message.MessageLookup;
import com.cyanspring.common.util.ArrayMap;
import com.cyanspring.common.util.IdGenerator;
import com.cyanspring.cstw.business.Business;
import com.cyanspring.cstw.common.ImageID;

public class GroupManagementDialog extends Dialog implements IAsyncEventListener{
	
	private static final Logger log = LoggerFactory
			.getLogger(GroupManagementDialog.class);

	private Button btnOk;
	private Button btnCancel;
	private Button btnAssign;
	private Button btnDeAssign;
	private Label lblManager;
	private Label lblMessage;
	private ImageRegistry imageRegistry;
	private String accountId;
	private String ID = IdGenerator.getInstance().getNextID();
	private UserGroup userGroup;
//	private ArrayMap<String, Account> accounts = new ArrayMap<String, Account>();
	private java.util.List<User> users = new ArrayList();
//	private ArrayList <Account>accountList = new ArrayList<Account>();
	private ArrayList <String>manageeList = new ArrayList<String>();
	private ArrayList <String>nonManageeList = new ArrayList<String>();
	private ListViewer manageeListView;
	private ListViewer nonManageeListView;
	private Composite parent;
	private ReentrantLock displayLock = new ReentrantLock();
	
	protected GroupManagementDialog(Shell parentShell,String accountId,java.util.List<User> users) {
		super(parentShell);
		this.accountId = accountId;
		this.users = users;
		for(User user:users){
			nonManageeList.add(user.getId());
		}
//		this.accounts = accounts;
//		accountList = accounts.toArray();		
//		for(Account account:accountList){
//			nonManageeList.add(account.getUserId());
//		}
		log.info("GroupManagementDialog set user:{}",accountId);
	}
	
	private void setManagerLabel(Label lbl,String account){
		lbl.setText("Manager : "+account);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		imageRegistry = Activator.getDefault().getImageRegistry();
		this.parent = parent;
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));
		container.getShell().setText("Managee Assign");
		Label lblManagee = new Label(container, SWT.NONE);
		lblManagee.setText("Managee");
		
		lblManager = new Label(container, SWT.NONE);
		setManagerLabel(lblManager,accountId);
		
		Label lblNonAssignAccont = new Label(container, SWT.NONE);
		lblNonAssignAccont.setText("Non Assign Trader");
		
		manageeListView = new ListViewer(container, SWT.BORDER | SWT.V_SCROLL);
		List list = manageeListView.getList();
		GridData gd_list = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		gd_list.widthHint = 112;
		gd_list.heightHint = 202;
		list.setLayoutData(gd_list);
		
		Composite composite = new Composite(container, SWT.NONE);
		GridData gd_composite = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_composite.widthHint = 115;
		gd_composite.heightHint = 98;
		composite.setLayoutData(gd_composite);
		composite.setLayout(new FillLayout(SWT.VERTICAL));
		
		btnDeAssign = new Button(composite, SWT.NONE);
		btnAssign = new Button(composite, SWT.NONE);	
		ImageDescriptor backward_imageDesc = imageRegistry
				.getDescriptor(ImageID.BACKWARD_ICON.toString());
		ImageDescriptor forward_imageDesc = imageRegistry
				.getDescriptor(ImageID.FORWARD_ICON.toString());
		btnDeAssign.setImage(forward_imageDesc.createImage());
		btnAssign.setImage(backward_imageDesc.createImage());
		
		btnDeAssign.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				doDeAssign();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
		
		btnAssign.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAssign();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
			
		nonManageeListView= new ListViewer(container, SWT.BORDER | SWT.V_SCROLL);
		List list_1 = nonManageeListView.getList();
		GridData gd_list_1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_list_1.heightHint = 207;
		gd_list_1.widthHint = 120;
		list_1.setLayoutData(gd_list_1);
				
		lblMessage = new Label(container, SWT.NONE);
		GridData gdLblMessage = new GridData(SWT.FILL, SWT.CENTER, true,
				false, 2, 1);
		gdLblMessage.horizontalIndent = 1;
		lblMessage.setLayoutData(gdLblMessage);
		lblMessage.setText("");
		Color red = parent.getDisplay().getSystemColor(SWT.COLOR_RED);
		lblMessage.setForeground(red);
		
		sendGroupManageeRequestEvent();
		
		return super.createDialogArea(parent);
	}
	private void sendGroupManageeRequestEvent(){
		GroupManageeRequestEvent event = new GroupManageeRequestEvent(ID, Business.getInstance().getFirstServer(), accountId);
		sendRemoteEvent(event);
	}
	protected void doAssign() {
		IStructuredSelection sel = (IStructuredSelection)nonManageeListView.getSelection();
		if( null == sel )
			return ;
		manageeListView.add(sel.getFirstElement());
		nonManageeListView.remove(sel.getFirstElement());
	}

	protected void doDeAssign() {
		IStructuredSelection sel = (IStructuredSelection)manageeListView.getSelection();
		if( null == sel )
			return ;
		nonManageeListView.add(sel.getFirstElement());
		manageeListView.remove(sel.getFirstElement());
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Composite buttonArea =  new Composite(parent, SWT.NONE);		
		GridLayout layoutButtons = new GridLayout(2, true);
		layoutButtons.marginRight = 30;
		layoutButtons.marginLeft = 30;
		buttonArea.setLayout(layoutButtons);
		buttonArea.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false, 1, 1));
		
		btnOk = new Button(buttonArea, SWT.PUSH);
		btnOk.setText("Confirm");
		btnOk.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		btnOk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sendManageeSet();
			}
		});
		
		btnCancel = new Button(buttonArea, SWT.PUSH);
		btnCancel.setText("Cancel");
		btnCancel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				GroupManagementDialog.this.close();
			}
		});

		return buttonArea;
	}
	
	protected void sendManageeSet() {
		log.info("into sendManageeSet");
		java.util.List<GroupManagement> groupList = new ArrayList<GroupManagement>();
		List manageeList = manageeListView.getList();
		for( int i = 0 ; i<manageeList.getItemCount() ; i++){		
			String managee = manageeList.getItem(i);
			GroupManagement gm = new GroupManagement(accountId,managee);
			groupList.add(gm);
		}
		
		
		java.util.List<UserGroup> originManageeList = userGroup.getNoneRecursiveManageeList();
		java.util.List<GroupManagement> addList = new ArrayList<GroupManagement>();
		java.util.List<GroupManagement> delList = new ArrayList<GroupManagement>();
		
		for(GroupManagement changed : groupList ){
			String managee = changed.getManaged();
			boolean isExist = false;;
			for(UserGroup ug : originManageeList){
				if(ug.getUser().equals(managee)){
					isExist = true;
					break;
				}
			}
			if(!isExist)
				addList.add(changed);
		}
		
		for( UserGroup ug : originManageeList){
			String managee = ug.getUser();
			boolean isExist = false;
			for(GroupManagement changed : groupList){
				if(changed.getManaged().equals(managee)){
					isExist = true;
					break;
				}
			}
			if(!isExist)
				delList.add(new GroupManagement(userGroup.getUser(),ug.getUser()));
		}
		
		
		
		for(GroupManagement gm :groupList){
			log.info("group manager:{}, managee:{}",gm.getManager(),gm.getManaged());
		}
		
		for(GroupManagement gm :addList){
			log.info("add manager:{}, managee:{}",gm.getManager(),gm.getManaged());
		}
		
		for(GroupManagement gm :delList){
			log.info("del manager:{}, managee:{}",gm.getManager(),gm.getManaged());
		}
		
		if(!addList.isEmpty()){
			CreateGroupManagementEvent addEvent = new CreateGroupManagementEvent(ID, Business.getInstance().getFirstServer(),addList);
			sendRemoteEvent(addEvent);
		}
		
		if(!delList.isEmpty()){
			DeleteGroupManagementEvent delEvent = new DeleteGroupManagementEvent(ID, Business.getInstance().getFirstServer(),delList);
			sendRemoteEvent(delEvent);
		}
	}

	@Override
	public int open() {
		subEvent(GroupManageeReplyEvent.class);
		subEvent(CreateGroupManagementReplyEvent.class);
		subEvent(DeleteGroupManagementReplyEvent.class);
		return super.open();
	}
	
	@Override
	public boolean close() {
		unSubEvent(GroupManageeReplyEvent.class);
		unSubEvent(CreateGroupManagementReplyEvent.class);
		unSubEvent(DeleteGroupManagementReplyEvent.class);
		return super.close();
	}
	
	@Override
	public void onEvent(AsyncEvent event) {
		if( event instanceof GroupManageeReplyEvent){
			log.info("receive GroupManageeReplyEvent");
			GroupManageeReplyEvent reply = (GroupManageeReplyEvent) event;
			if(reply.isOk()){
				this.userGroup = reply.getUserGroup();
				showManageeList();
			}else{
				final String msg = getReplyErrorMessage(reply.getMesssage());
				parent.getDisplay().asyncExec(new Runnable(){

					@Override
					public void run() {
						btnOk.setEnabled(false);
						lblMessage.setText(msg);	
					}
					
				});

			}
		}else if(event instanceof CreateGroupManagementReplyEvent){
			log.info("receive CreateGroupManagementReplyEvent");
			CreateGroupManagementReplyEvent reply = (CreateGroupManagementReplyEvent) event;
			showConfirmResultOnLabel(reply.getResult());
			if(reply.isOk()){
				sendGroupManageeRequestEvent();
			}else{
				pushMessageToLabel( getReplyErrorMessage(reply.getMessage()));
			}			
		}else if(event instanceof DeleteGroupManagementReplyEvent){
			log.info("receive DeleteGroupManagementReplyEvent");
			DeleteGroupManagementReplyEvent reply = (DeleteGroupManagementReplyEvent) event;
			showConfirmResultOnLabel(reply.getResult());

			if(reply.isOk()){
				sendGroupManageeRequestEvent();
			}else{
				pushMessageToLabel( getReplyErrorMessage(reply.getMessage()));
			}
		}
	}
	private String getReplyErrorMessage(String msg){
		 MessageBean bean = MessageLookup.getMsgBeanFromEventMessage(msg);
		 return bean.getLocalMsg();
	}
	
	private void pushMessageToLabel(final String msg){
		parent.getDisplay().asyncExec(new Runnable(){

			@Override
			public void run() {
				lblMessage.setText(msg+"\n"+lblMessage.getText());	
			}
			
		});
	}
	
	private void showConfirmResultOnLabel(Map <GroupManagement,String> resultMap){
		Set <Entry<GroupManagement,String>> entrys =  resultMap.entrySet();
		for(Entry <GroupManagement,String>entry:entrys){
			GroupManagement gm = entry.getKey();
			String msg = entry.getValue();
			pushMessageToLabel("Manager:"+gm.getManager()+" - Managee:"+gm.getManaged()+"  Message:"+msg);
		}
	}
	
	private void showManageeList() {
//		Set <UserGroup>manageeSet = userGroup.getManageeSet();
//		Iterator<UserGroup> mangeeIte = manageeSet.iterator();
//		final ArrayList <String>tempList = new ArrayList<String>();
//		while(mangeeIte.hasNext()){
//			UserGroup ug = mangeeIte.next();
//			manageeList.add(ug.getUser());
//		}
		
		try { 
			displayLock.lock();
			final ArrayList <String>tempList = new ArrayList<String>();
			java.util.List <UserGroup> manageeGroupList = userGroup.getNoneRecursiveManageeList();
			for(UserGroup ug : manageeGroupList){
				manageeList.add(ug.getUser());
			}
			
			for(String user:nonManageeList){
				if(!manageeList.contains(user)
						&& !accountId.equals(user)){
					tempList.add(user);
				}
			}
		
			parent.getDisplay().asyncExec(new Runnable(){

				@Override
				public void run() {			
					nonManageeListView.add(tempList.toArray(new String[tempList.size()]));
					manageeListView.add(manageeList.toArray(new String[manageeList.size()]));
				}
				
			});		
		}finally{
			displayLock.unlock();
		}

	}

	private void subEvent(Class<? extends AsyncEvent> clazz){
		Business.getInstance().getEventManager().subscribe(clazz,ID, this);		
	}
	
	private void unSubEvent(Class<? extends AsyncEvent> clazz){
		Business.getInstance().getEventManager().unsubscribe(clazz,ID, this);		
	}
	
	private void sendRemoteEvent(RemoteAsyncEvent event) {
		try {
			Business.getInstance().getEventManager().sendRemoteEvent(event);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
}
