/*******************************************************************************
 * Copyright (c) 2011-2012 Cyan Spring Limited
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms specified by license file attached.
 *
 * Software distributed under the License is released on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 ******************************************************************************/
package com.cyanspring.cstw.gui;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.services.ISourceProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cyanspring.common.account.UserRole;
import com.cyanspring.common.event.AsyncEvent;
import com.cyanspring.common.event.IAsyncEventListener;
import com.cyanspring.cstw.business.Business;
import com.cyanspring.cstw.gui.command.auth.AuthProvider;
import com.cyanspring.cstw.gui.command.auth.ViewAuthListener;
import com.cyanspring.cstw.gui.command.auth.WorkbenchActionProvider;
import com.cyanspring.cstw.localevent.SelectUserAccountLocalEvent;
import com.cyanspring.cstw.session.CSTWSession;
import com.cyanspring.cstw.ui.views.LoginDialog;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor
		implements IAsyncEventListener {
	private static final Logger log = LoggerFactory
			.getLogger(ApplicationWorkbenchWindowAdvisor.class);
	private static final String title = "LTS Trader Workstation";

	public ApplicationWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	public ActionBarAdvisor createActionBarAdvisor(
			IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}

	@Override
	public void preWindowOpen() {
		LoginDialog loginDialog = new LoginDialog(null);
		loginDialog.open();
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(1024, 768));
		if (CSTWSession.getInstance().getUserGroup().getRole()
				.equals(UserRole.Admin))
			configurer.setShowCoolBar(true);
		else
			configurer.setShowCoolBar(false);
		configurer.setShowStatusLine(true);
		configurer.setTitle(title);
		Business.getInstance().getEventManager()
				.subscribe(SelectUserAccountLocalEvent.class, this);
	}

	@Override
	public void postWindowOpen() {
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();

		try {

			// add listener
			ViewAuthListener authListener = new ViewAuthListener();
			page.addPartListener(authListener);
			setUserAccount(CSTWSession.getInstance().getUserId(), CSTWSession
					.getInstance().getAccountId());

			// check login role
			log.info("fire account login menu change :{}", CSTWSession
					.getInstance().getUserGroup().getRole().name());
			ISourceProviderService sourceProviderService = (ISourceProviderService) window
					.getService(ISourceProviderService.class);
			AuthProvider commandStateService = (AuthProvider) sourceProviderService
					.getSourceProvider(CSTWSession.getInstance().getUserGroup()
							.getRole().name());
			commandStateService.fireAccountChanged();

			log.info("Business.getInstance().getUserGroup().getRole().equals(UserRole.Admin):"
					+ CSTWSession.getInstance().getUserGroup().getRole()
							.equals(UserRole.Admin));
			if (CSTWSession.getInstance().getUserGroup().getRole()
					.equals(UserRole.Admin))
				getWindowConfigurer().setShowCoolBar(true);

			// filter already open view
			IWorkbenchPage pages[] = getWindowConfigurer().getWindow()
					.getPages();
			for (IWorkbenchPage activePage : pages) {

				IViewReference vrs[] = activePage.getViewReferences();
				for (IViewReference vr : vrs) {
					if (!Business.getBusinessService().hasViewAuth(
							vr.getPartName())) {
						IViewPart part = activePage.findView(vr.getId());
						log.info("hide view - PartName():{},{}",
								vr.getPartName(), vr.getId());
						if (null == part) {
							log.info("part is null");
						} else {
							part.getViewSite().getShell().setVisible(false);
							activePage.hideView(vr);
						}
					} else {
						authListener.filterViewAllAction(vr.getPartName(),
								vr.getPart(true));
					}

				}
			}

			ToolBarManager newToolBarManager = WorkbenchActionProvider
					.getInstance().getWorkbenchCoolBarActions();
			ToolBarManager coolBarItemManager = WorkbenchActionProvider
					.getInstance().getToolBarManager(
							WorkbenchActionProvider.ID_TOOLBARMANAGER_COOLBAR);
			IContributionItem[] coolBarItems = coolBarItemManager.getItems();
			for (IContributionItem coolItem : coolBarItems) {
				try {
					ActionContributionItem actionItem = (ActionContributionItem) coolItem;
					if (actionItem.getId().equals("USER_INFO_ACTION")) {
						actionItem.getAction().setText(
								CSTWSession.getInstance().getUserId()
										+ " - "
										+ CSTWSession.getInstance().getUserGroup()
												.getRole().toString());
					}
					authListener.filterViewAction("Application View",
							actionItem);
					if (!actionItem.isEnabled())
						newToolBarManager.remove(actionItem.getId());

				} catch (Exception e) {
					log.error("e:" + e.getMessage());
				}

			}

			// filter this item or your coolbar will add gap when every login.
			IContributionItem items[] = getWindowConfigurer()
					.getActionBarConfigurer().getCoolBarManager().getItems();
			for (IContributionItem item : items) {
				if (!item.getClass().getSimpleName().contains("GroupMarker"))
					getWindowConfigurer().getActionBarConfigurer()
							.getCoolBarManager().remove(item);
			}
			getWindowConfigurer().getActionBarConfigurer().getCoolBarManager()
					.add(newToolBarManager);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public boolean preWindowShellClose() {
		try {
			Business.getInstance().stop();
			Thread.sleep(300);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return true;
	}

	private void setUserAccount(String user, String account) {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setTitle(title + " - " + user + "/" + account);
	}

	@Override
	public void onEvent(AsyncEvent event) {
		if (event instanceof SelectUserAccountLocalEvent) {
			setUserAccount(((SelectUserAccountLocalEvent) event).getUser(),
					((SelectUserAccountLocalEvent) event).getAccount());
		}
	}

}
