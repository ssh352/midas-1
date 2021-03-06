package com.cyanspring.cstw.ui.views;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cyanspring.common.Default;
import com.cyanspring.common.account.Account;
import com.cyanspring.common.account.AccountState;
import com.cyanspring.common.event.AsyncEvent;
import com.cyanspring.common.event.AsyncTimerEvent;
import com.cyanspring.common.event.IAsyncEventListener;
import com.cyanspring.common.event.account.AccountDynamicUpdateEvent;
import com.cyanspring.common.event.account.AccountUpdateEvent;
import com.cyanspring.common.event.account.AllAccountSnapshotReplyEvent;
import com.cyanspring.common.event.account.AllAccountSnapshotRequestEvent;
import com.cyanspring.common.event.account.ResetAccountRequestEvent;
import com.cyanspring.common.event.order.StrategySnapshotRequestEvent;
import com.cyanspring.common.util.ArrayMap;
import com.cyanspring.common.util.IdGenerator;
import com.cyanspring.cstw.business.Business;
import com.cyanspring.cstw.common.ImageID;
import com.cyanspring.cstw.gui.Activator;
import com.cyanspring.cstw.gui.common.ColumnProperty;
import com.cyanspring.cstw.gui.common.DynamicTableViewer;
import com.cyanspring.cstw.gui.common.StyledAction;
import com.cyanspring.cstw.localevent.AccountSelectionLocalEvent;
import com.cyanspring.cstw.localevent.SelectUserAccountLocalEvent;
import com.cyanspring.cstw.localevent.ServerStatusLocalEvent;
import com.cyanspring.cstw.session.CSTWSession;

public class AccountView extends ViewPart implements IAsyncEventListener {
	private static final Logger log = LoggerFactory
			.getLogger(AccountView.class);
	public static final String ID = "com.cyanspring.cstw.gui.AccountViewer";
	private DynamicTableViewer viewer;
	private ArrayMap<String, Account> accounts = new ArrayMap<String, Account>();
	private Account currentAccount = null;
	private boolean columnCreated;
	private Menu menu;
	private CreateUserDialog createUserDialog;
	private AddCashDialog addCashDialog;
	private Action createUserAction;
	private Action addCashAction;
	private Action createCountAccountAction;
	private Action createManualRefreshAction;
	private Action createSearchIdAction;
	private Action createChangeAccountStateAction;

	private final String ID_CREATE_USER_ACTION = "CREATE_USER";
	private final String ID_ADD_CASH_ACTION = "ADD_CASH";
	private final String ID_COUNT_ACCOUNT_ACTION = "COUNT_ACCOUNT";
	private final String ID_MANUAL_REFRESH_ACTION = "MANUAL_REFRESH";
	private final String ID_SEARCH_ID_ACTION = "SEARCH_ID";
	private final String ID_CHANGE_ACCOUNT_STATE_ACTION = "CHANGE_ACCOUNT_STATE";

	private Composite searchBarComposite;
	private GridData gd_searchBar;
	private Text searchText;
	private Button searchButton;
	private ImageRegistry imageRegistry;
	private AsyncTimerEvent timerEvent = new AsyncTimerEvent();
	private long maxRefreshInterval = 300;
	private final int autoRefreshLimitUser = 1000;
	private boolean show;

	private final RGB PURPLE = new RGB(171, 130, 255);
	private final RGB WHITE = new RGB(255, 255, 255);
	private final RGB GRAY = new RGB(181, 181, 181);
	private final Color FROZEN_COLOR = new Color(Display.getCurrent(), PURPLE);
	private final Color TERMINATE_COLOR = new Color(Display.getCurrent(), GRAY);
	private final Color NORMAL_COLOR = new Color(Display.getCurrent(), WHITE);
	private final String COLUMN_STATE = "State";
	private Composite parentComposite = null;
	private int currentFindNum = 0;

	@Override
	public void createPartControl(Composite parent) {
		log.info("Creating account view");

		// create ImageRegistery
		imageRegistry = Activator.getDefault().getImageRegistry();
		parentComposite = parent;
		// create parent layout
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);

		initSearchBar(parent);

		String strFile = "AccountTable.xml";
		viewer = new DynamicTableViewer(parent, SWT.MULTI | SWT.FULL_SELECTION
				| SWT.H_SCROLL | SWT.V_SCROLL, strFile);
		viewer.init();

		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 1;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		viewer.getControl().setLayoutData(gridData);

		final Table table = viewer.getTable();
		table.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TableItem item = table.getItem(table.getSelectionIndex());
				Object obj = item.getData();
				if (obj instanceof Account) {
					Account account = (Account) obj;
					currentAccount = account;
					Business.getInstance()
							.getEventManager()
							.sendEvent(
									new SelectUserAccountLocalEvent(account
											.getUserId(), account.getId()));
					Business.getInstance()
							.getEventManager()
							.sendEvent(
									new AccountSelectionLocalEvent(account
											.getId()));

					try {
						Business.getInstance()
								.getEventManager()
								.sendRemoteEvent(
										new StrategySnapshotRequestEvent(
												account.getId(), Business
														.getBusinessService()
														.getFirstServer(), null));
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			}
		});
		createManualRefreshToggleAction(parent);
		createAddCashAction(parent);
		createUserAccountAction(parent);
		createCountAccountAction(parent);
		createResetAccountAction(parent);
		createSearchIdAction(parent);
		createChangeAccountStateAction(parent);

		if (Business.getBusinessService().isFirstServerReady())
			sendSubscriptionRequest(Business.getBusinessService().getFirstServer());
		else
			Business.getInstance().getEventManager()
					.subscribe(ServerStatusLocalEvent.class, this);

		log.info("no auto refresh account version");
		// Business.getInstance().getScheduleManager().scheduleRepeatTimerEvent(maxRefreshInterval,
		// this, timerEvent);

	}

	private void createChangeAccountStateAction(final Composite parent) {
		createChangeAccountStateAction = new StyledAction("",
				org.eclipse.jface.action.IAction.AS_PUSH_BUTTON) {
			public void run() {
				if (null == currentAccount) {
					showMessageBox("You need select a account", parent);
				} else {
					if (!StringUtils.hasText(currentAccount.getId())) {
						showMessageBox("account id is empty", parent);
						return;
					}
					log.info("currentAccount:{}", currentAccount.getId());
					currentAccount = accounts.get(currentAccount.getId());
					ChangeAccountStateDialog dialog = new ChangeAccountStateDialog(
							parent.getShell(), currentAccount);
					dialog.open();

					Account tempAccount = dialog.getAccount();
					Account account = accounts.get(tempAccount.getId());
					account.setState(tempAccount.getState());
					accounts.put(tempAccount.getId(), account);
					show = true;
					showAccounts();

				}
			}
		};
		createChangeAccountStateAction.setId(ID_CHANGE_ACCOUNT_STATE_ACTION);
		createChangeAccountStateAction.setText("Change Account State");
		createChangeAccountStateAction.setToolTipText("Change Account State");

		ImageDescriptor imageDesc = imageRegistry
				.getDescriptor(ImageID.AMEND_OPTIONS_ICON.toString());
		createChangeAccountStateAction.setImageDescriptor(imageDesc);
		IActionBars bars = getViewSite().getActionBars();
		bars.getToolBarManager().add(createChangeAccountStateAction);
	}

	private void initSearchBar(Composite parent) {
		searchBarComposite = new Composite(parent, SWT.NONE);
		gd_searchBar = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		searchBarComposite.setLayoutData(gd_searchBar);
		searchBarComposite.setLayout(new GridLayout(2, false));

		searchText = new Text(searchBarComposite, SWT.BORDER);
		GridData gd_text = new GridData(SWT.FILL, SWT.CENTER, false, false, 1,
				1);
		gd_text.widthHint = 350;
		searchText.setLayoutData(gd_text);

		searchText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR) {
					searchText();
				}
			}
		});

		searchButton = new Button(searchBarComposite, SWT.NONE);
		searchButton.setText("search");
		searchButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				searchText();
			}
		});
		searchBarComposite.setVisible(false);
		gd_searchBar.heightHint = 0;
		parent.layout();
	}

	private void searchText() {
		String textValue = searchText.getText();
		boolean found = false;

		for (int i = currentFindNum; i < viewer.getTable().getItemCount(); i++) {
			Account account = (Account) viewer.getTable().getItem(i).getData();
			if (account.getUserId().toUpperCase()
					.startsWith(textValue.toUpperCase())) {
				viewer.getTable().setSelection(i);
				found = true;
				currentFindNum = ++i;
				break;
			}
		}

		if (false == found)
			currentFindNum = 0;
	}

	private void createManualRefreshToggleAction(final Composite parent) {

		createManualRefreshAction = new StyledAction("",
				org.eclipse.jface.action.IAction.AS_CHECK_BOX) {
			public void run() {
				if (!createManualRefreshAction.isChecked()) {
					Business.getInstance().getScheduleManager()
							.cancelTimerEvent(timerEvent);
				} else {
					Business.getInstance()
							.getScheduleManager()
							.scheduleRepeatTimerEvent(maxRefreshInterval,
									AccountView.this, timerEvent);
				}

			}
		};
		createManualRefreshAction.setId(ID_MANUAL_REFRESH_ACTION);
		createManualRefreshAction.setChecked(false);
		createManualRefreshAction.setText("AutoRefresh");
		createManualRefreshAction.setToolTipText("AutoRefresh");

		ImageDescriptor imageDesc = imageRegistry
				.getDescriptor(ImageID.REFRESH_ICON.toString());
		createManualRefreshAction.setImageDescriptor(imageDesc);
		IActionBars bars = getViewSite().getActionBars();
		bars.getToolBarManager().add(createManualRefreshAction);
	}

	private void createAddCashAction(final Composite parent) {
		addCashAction = new Action() {
			public void run() {
				addCashDialog = new AddCashDialog(parent.getShell(),
						currentAccount);
				addCashDialog.open();
			}
		};
		addCashAction.setId(ID_ADD_CASH_ACTION);
		addCashAction.setText("Add Cash");
		addCashAction.setToolTipText("Add cash");

		ImageDescriptor img = imageRegistry.getDescriptor(ImageID.MONEY_ICON
				.toString());

		addCashAction.setImageDescriptor(img);
		IActionBars bars = getViewSite().getActionBars();
		bars.getToolBarManager().add(addCashAction);
	}

	private void createUserAccountAction(final Composite parent) {
		createUserDialog = new CreateUserDialog(parent.getShell());
		createUserDialog.onlyTraderUserRole(true);
		// create local toolbars
		createUserAction = new Action() {
			public void run() {
				// orderDialog.open();
				createUserDialog.open();
			}
		};
		createUserAction.setId(ID_CREATE_USER_ACTION);
		createUserAction.setText("Creat a user & account");
		createUserAction.setToolTipText("Create a user & account");
		ImageDescriptor imageDesc = imageRegistry
				.getDescriptor(ImageID.PLUS_ICON.toString());
		createUserAction.setImageDescriptor(imageDesc);

		IActionBars bars = getViewSite().getActionBars();
		bars.getToolBarManager().add(createUserAction);

	}

	private void showMessageBox(final String msg, Composite parent) {
		parent.getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				MessageBox messageBox = new MessageBox(parentComposite
						.getShell(), SWT.ICON_INFORMATION);
				messageBox.setText("Info");
				messageBox.setMessage(msg);
				messageBox.open();
			}

		});
	}

	private void createCountAccountAction(final Composite parent) {
		// create local toolbars
		createCountAccountAction = new Action() {
			public void run() {
				MessageBox messageBox = new MessageBox(parent.getShell(),
						SWT.ICON_INFORMATION);
				messageBox.setText("Info");
				messageBox.setMessage("Number of accounts: " + accounts.size());
				messageBox.open();
			}
		};
		createCountAccountAction.setId(ID_COUNT_ACCOUNT_ACTION);
		createCountAccountAction.setText("Check number of accounts");
		createCountAccountAction.setToolTipText("Check number of accounts");

		ImageDescriptor imageDesc = imageRegistry
				.getDescriptor(ImageID.TRUE_ICON.toString());
		createCountAccountAction.setImageDescriptor(imageDesc);

		IActionBars bars = getViewSite().getActionBars();
		bars.getToolBarManager().add(createCountAccountAction);
	}

	private void createResetAccountAction(final Composite parent) {
		// create local toolbars
		createCountAccountAction = new Action() {
			public void run() {
				Table table = viewer.getTable();
				TableItem items[] = table.getSelection();
				Account account = null;
				if (items.length > 0) {
					TableItem item = items[0];
					Object obj = item.getData();
					if (obj instanceof Account) {
						account = (Account) obj;
					}
				}

				if (null == account)
					return;

				boolean result = MessageDialog.openConfirm(parent.getShell(),
						"Reset account", "Are you sure to reset account: "
								+ account.getId());

				if (result) {
					try {
						Business.getInstance()
								.getEventManager()
								.sendRemoteEvent(
										new ResetAccountRequestEvent(ID,
												Business.getBusinessService()
														.getFirstServer(),
												account.getId(), IdGenerator
														.getInstance()
														.getNextID(), account
														.getUserId(), Default
														.getMarket(), null));
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}

			}
		};
		createCountAccountAction.setText("reset account");
		createCountAccountAction.setToolTipText("Reset account");

		ImageDescriptor imageDesc = imageRegistry
				.getDescriptor(ImageID.CANCEL_ICON.toString());
		createCountAccountAction.setImageDescriptor(imageDesc);

		IActionBars bars = getViewSite().getActionBars();
		bars.getToolBarManager().add(createCountAccountAction);

	}

	private void createSearchIdAction(final Composite parent) {
		createSearchIdAction = new Action() {
			public void run() {
				searchBarComposite.setVisible(!searchBarComposite.isVisible());
				if (searchBarComposite.isVisible()) {
					currentFindNum = 0;
					gd_searchBar.heightHint = 40;
				} else {
					gd_searchBar.heightHint = 0;
				}
				parent.layout();
			}
		};
		createSearchIdAction.setId(ID_SEARCH_ID_ACTION);
		createSearchIdAction.setToolTipText("Search Id from table");
		ImageDescriptor imageDesc = imageRegistry
				.getDescriptor(ImageID.FILTER_ICON.toString());
		createSearchIdAction.setImageDescriptor(imageDesc);

		IActionBars bars = getViewSite().getActionBars();
		bars.getToolBarManager().add(createSearchIdAction);
	}

	private void createMenu(Composite parent) {
		menu = new Menu(viewer.getTable().getShell(), SWT.POP_UP);
		MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText("Set current account");
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				try {
					Table table = viewer.getTable();
					TableItem items[] = table.getSelection();
					for (TableItem item : items) {
						Object obj = item.getData();
						if (obj instanceof Account) {
							Account account = (Account) obj;
							Business.getInstance()
									.getEventManager()
									.sendEvent(
											new SelectUserAccountLocalEvent(
													account.getUserId(),
													account.getId()));
						}
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		});
		viewer.setBodyMenu(menu);
	}

	private void sendSubscriptionRequest(String server) {

		Business.getInstance().getEventManager()
				.subscribe(AccountUpdateEvent.class, this);
		Business.getInstance().getEventManager()
				.subscribe(AccountDynamicUpdateEvent.class, this);
		Business.getInstance().getEventManager()
				.subscribe(AllAccountSnapshotReplyEvent.class, ID, this);

		AllAccountSnapshotRequestEvent request = new AllAccountSnapshotRequestEvent(
				ID, server);
		log.debug("AllAccountSnapshotRequestEvent sent");
		try {
			Business.getInstance().getEventManager().sendRemoteEvent(request);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void createOpenPositionColumns(List<Account> list) {
		if (!columnCreated) {
			Object obj = list.get(0);
			List<ColumnProperty> properties = viewer
					.setObjectColumnProperties(obj);
			viewer.setSmartColumnProperties(obj.getClass().getName(),
					properties);
			viewer.setInput(list);
			columnCreated = true;
		}
	}

	private void setBackgroundColorFromState() {
		if (viewer.getControl().getDisplay().isDisposed()) {
			return;
		}
		viewer.getControl().getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				if (viewer.getTable().isDisposed())
					return;
				TableColumn columns[] = viewer.getTable().getColumns();
				int stateColumn = -1;
				for (int i = 0; i < columns.length; i++) {
					if (COLUMN_STATE.equals(columns[i].getText())) {
						stateColumn = i;
						break;
					}
				}
				if (stateColumn == -1) {
					log.error("can't find state column");
					return;
				}

				for (TableItem item : viewer.getTable().getItems()) {
					String state = item.getText(stateColumn);
					if (AccountState.FROZEN.name() == state) {
						item.setBackground(FROZEN_COLOR);
					} else if (AccountState.TERMINATED.name() == state) {
						item.setBackground(TERMINATE_COLOR);
					} else {
						item.setBackground(NORMAL_COLOR);
					}
				}
				viewer.refresh();
			}
		});
	}

	private void showAccounts() {
		if (!show)
			return;
		viewer.getControl().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				synchronized (viewer) {
					if (viewer.isViewClosing())
						return;
					if (accounts.toArray().size() > 0)
						createOpenPositionColumns(accounts.toArray());

					viewer.refresh();
				}
			}
		});
		show = false;
		setBackgroundColorFromState();
	}

	@Override
	public void dispose() {
		Business.getInstance().getScheduleManager()
				.cancelTimerEvent(timerEvent);
		unSubEvent(AccountUpdateEvent.class);
		unSubEvent(AccountDynamicUpdateEvent.class);
		unSubEvent(AllAccountSnapshotReplyEvent.class);

		super.dispose();
	}

	private void unSubEvent(Class<? extends AsyncEvent> clazz) {
		Business.getInstance().getEventManager().unsubscribe(clazz, ID, this);
	}

	@Override
	public void setFocus() {

	}

	private void processAccountUpdate(Account account) {

		if (inAuthList(account)) {
			accounts.put(account.getId(), account);
		}
		show = true;
	}

	private boolean inAuthList(Account account) {
		if (CSTWSession.getInstance().getUserId().equals(account.getUserId())
				|| isManagee(account.getUserId())) {
			return true;
		}
		return false;
	}

	private boolean isManagee(String account) {
		if (CSTWSession.getInstance().getUserGroup().isAdmin()
				|| CSTWSession.getInstance().getUserGroup()
						.isGroupPairExist(account)
				|| CSTWSession.getInstance().getUserGroup()
						.isManageeExist(account)) {
			return true;
		}
		return false;
	}

	@Override
	public void onEvent(AsyncEvent event) {
		if (event instanceof ServerStatusLocalEvent) {
			sendSubscriptionRequest(((ServerStatusLocalEvent) event)
					.getServer());
		} else if (event instanceof AllAccountSnapshotReplyEvent) {
			AllAccountSnapshotReplyEvent evt = (AllAccountSnapshotReplyEvent) event;
			for (Account account : evt.getAccounts()) {
				if (inAuthList(account)) {
					accounts.put(account.getId(), account);
				}
			}
			log.info("Loaded accounts: " + evt.getAccounts().size());
			show = true;

			if (accounts.size() <= autoRefreshLimitUser) {
				if (!createManualRefreshAction.isChecked())
					createManualRefreshAction.setChecked(true);
				createManualRefreshAction.run();
			}

			showAccounts();
		} else if (event instanceof AccountUpdateEvent) {
			processAccountUpdate(((AccountUpdateEvent) event).getAccount());
		} else if (event instanceof AccountDynamicUpdateEvent) {
			processAccountUpdate(((AccountDynamicUpdateEvent) event)
					.getAccount());
		} else if (event instanceof AsyncTimerEvent) {
			show = true;
			showAccounts();
		}
	}

}
