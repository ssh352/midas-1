/**
 * 
 */
package com.cyanspring.cstw.ui.rw.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cyanspring.cstw.ui.rw.views.RWInfoView;

/**
 * @author Junfeng
 *
 */
public class RWInfoViewCommand extends AbstractHandler{
	private static final Logger log = LoggerFactory
			.getLogger(RWInfoViewCommand.class);
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		try {
			page.showView(RWInfoView.ID);
		} catch (PartInitException e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

}
