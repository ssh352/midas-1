package com.cyanspring.cstw.ui.basic;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.cyanspring.cstw.preference.PreferenceStoreManager;
import com.cyanspring.cstw.service.iservice.IExportCsvService;
import com.cyanspring.cstw.service.iservice.ServiceFactory;
import com.cyanspring.cstw.ui.common.TableType;
import com.cyanspring.cstw.ui.utils.TableUtils;

/**
 * 
 * @author NingXiaoFeng
 * @create date 2015/05/19
 *
 */
public abstract class BasicTableComposite extends Composite {

	private TableType tableType;

	private Table table;

	protected TableViewer tableViewer;

	private TableColumnLayout tclayout;

	private Menu headerMenu;

	private Menu bodyMenu;

	private Object selectedObject;
	
	private Action exportAction;
	
	private IExportCsvService exportCsvService;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public BasicTableComposite(Composite parent, int style, TableType tableType) {
		super(parent, style);
		this.tableType = tableType;
		initCompoments();
		initTableColumn();
		initAction();
		initMenu();
		initProvider();
		initListener();
		initService();
		initOthers();
	}

	private void initCompoments() {
		tclayout = new TableColumnLayout();
		this.setLayout(tclayout);

		if (tableType.isCheckStyle()) {
			table = new Table(this, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
			tableViewer = new CheckboxTableViewer(table);
		} else {
			tableViewer = new TableViewer(this, SWT.BORDER | SWT.FULL_SELECTION);
			table = tableViewer.getTable();
		}

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		table.setBounds(0, 0, 85, 85);

		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}

	private void initTableColumn() {
		for (int i = 0; i < tableType.getColumnTiles().length; i++) {
			TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer,
					SWT.NONE);
			TableColumn column = viewerColumn.getColumn();
			column.setMoveable(true);
			initColumnTitle(column, i);
			initTableColumnWidth(column, i);
			if (tableType.isSortable()) {
				TableUtils.addSorter(tableViewer, column);
			}
			BasicEditingSupport editingSupport = getEditingSupport(i);
			if (editingSupport != null) {
				viewerColumn.setEditingSupport(editingSupport);
			}
		}
		table.pack();
	}

	private void initColumnTitle(final TableColumn column, int columnIndex) {
		column.setText(tableType.getColumnTiles()[columnIndex]);
	}

	private void initTableColumnWidth(final TableColumn column, int columnIndex) {
		String columnName = tableType.getColumnTiles()[columnIndex];
		final String tableColumnName = tableType.getTableName() + "/"
				+ columnName;
		int width = PreferenceStoreManager.getInstance().getTableColumnWidth(
				tableColumnName);

		if (width <= 0) {
			width = tableType.getColumnWidths()[columnIndex];
		}
		// column.setWidth(width);
		tclayout.setColumnData(column, new ColumnWeightData(120, 120, true));
		column.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (column.getWidth() == 0) {
					return;
				}
				PreferenceStoreManager.getInstance().setTableColumnWidth(
						tableColumnName, column.getWidth());
			}
		});

		column.addListener(SWT.Move, new Listener() {
			@Override
			public void handleEvent(Event event) {
				// TODO
			}
		});

	}

	private void initMenu() {
		headerMenu = new Menu(this.getShell(), SWT.POP_UP);
		for (int i = 0; i < tableType.getColumnTiles().length; i++) {
			String title = tableType.getColumnTiles()[i];
			final MenuItem item = new MenuItem(headerMenu, SWT.CHECK);
			item.setText(title);
			if (PreferenceStoreManager.getInstance().istTableColumnVisible(
					tableType.getTableName() + "/" + title)) {
				item.setSelection(true);
				hiddenColumn(i, false);
			} else {
				item.setSelection(false);
				hiddenColumn(i, true);
			}
			final int index = i;
			item.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (item.getSelection()) {
						hiddenColumn(index, false);
					} else {
						hiddenColumn(index, true);
					}

				}
			});
		}

		// Provide the input to the ContentProvider
		table.addListener(SWT.MenuDetect, new Listener() {
			public void handleEvent(Event event) {
				Display display = BasicTableComposite.this.getDisplay();
				Point pt = display
						.map(null, table, new Point(event.x, event.y));
				Rectangle clientArea = table.getClientArea();
				boolean isHeader = clientArea.y <= pt.y
						&& pt.y < (clientArea.y + table.getHeaderHeight());
				if (isHeader) {
					table.setMenu(headerMenu);
				} else {
					bodyMenu = new Menu(BasicTableComposite.this.getShell(),
							SWT.POP_UP);
					initBodyMenu(bodyMenu);
					table.setMenu(bodyMenu);
				}
			}
		});
	}

	private void hiddenColumn(int index, boolean isHidden) {
		TableColumn tableColumn = table.getColumn(index);
		if (isHidden) {
			tableColumn.setWidth(0);
			tableColumn.setResizable(false);
		} else {
			tableColumn.setResizable(true);
			String tableColumnName = tableType.getTableName() + "/"
					+ tableType.getColumnTiles()[index];
			int width = PreferenceStoreManager.getInstance()
					.getTableColumnWidth(tableColumnName);
			if (width <= 0) {
				width = tableType.getColumnWidths()[index];
			}
			tclayout.setColumnData(tableColumn, new ColumnWeightData(width, 25,
					true));

		}
		PreferenceStoreManager.getInstance().setTableColumnVisible(
				tableType.getTableName() + "/"
						+ tableType.getColumnTiles()[index], !isHidden);

	}

	private void initProvider() {
		tableViewer.setContentProvider(new DefaultContentProvider());
		tableViewer.setLabelProvider(createLabelProvider());
	}

	protected void initOthers() {
		// to insert code
	}

	private void initListener() {
		tableViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						ISelection selection = event.getSelection();
						selectedObject = ((IStructuredSelection) selection)
								.getFirstElement();
					}
				});
	}
	
	private void initAction() {
		exportAction = new Action() {
			@Override
			public void run() {
				FileDialog fileDialog = new FileDialog(getShell());
				fileDialog.setText("Export CSV");
				fileDialog.setFilterExtensions(new String[] { "*.csv", "*.*" });
				fileDialog.setFileName("NewCSVFile.csv");
				String file = fileDialog.open();
				if (file != null) {
					if(!file.endsWith(".csv")) {
						file += ".csv";
					}
					exportCsvService.exportCsv(tableViewer.getTable(), file);
				}				
			}
		};
		
	}
	
	private void initService() {
		exportCsvService = ServiceFactory.createExportCsvService();
	}
	
	public void setInput(List<?> input) {
		// 判断当前Table是否需要排序。
		if (input != null && tableViewer.getTable().getSortColumn() != null) {
			String text = tableViewer.getTable().getSortColumn().getText();
			final int columnIndex = tableType.getColumnIndex(text);
			final int direction = tableViewer.getTable().getSortDirection();
			final ITableLabelProvider provider = (ITableLabelProvider) tableViewer
					.getLabelProvider();
			Collections.sort(input, new Comparator<Object>() {
				@Override
				public int compare(Object o1, Object o2) {
					String s1 = provider.getColumnText(o1, columnIndex);
					String s2 = provider.getColumnText(o2, columnIndex);
					Collator comparator = Collator.getInstance(Locale
							.getDefault());
					if (direction == SWT.UP) {
						return comparator.compare(s1, s2);
					} else {
						return comparator.compare(s1, s2) * -1;
					}
				}
			});
		}
		tableViewer.setInput(input);
	}
	
	public TableViewer getTableViewer() {
		return tableViewer;
	}

	/**
	 * 
	 * @param listener
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		tableViewer.addSelectionChangedListener(listener);

	}

	public Object getSelectedObject() {
		return selectedObject;
	}

	public Object[] getCheckedElements() {
		if (tableType.isCheckStyle()) {
			CheckboxTableViewer checkboxTableViewer = (CheckboxTableViewer) tableViewer;
			return checkboxTableViewer.getCheckedElements();
		}
		return null;
	}

	/**
	 * to hidden column.
	 * 
	 * @param cloumnIndex
	 *            int
	 */
	public void disableColumn(int cloumnIndex) {
		if (table.getColumn(cloumnIndex) != null) {
			table.getColumn(cloumnIndex).setWidth(0);
			table.getColumn(cloumnIndex).setResizable(false);
		}
	}

	protected abstract IBaseLabelProvider createLabelProvider();

	/**
	 * to be override if need.
	 * 
	 * @param ColumnIndex
	 * @return
	 */
	protected BasicEditingSupport getEditingSupport(int ColumnIndex) {
		return null;
	}

	protected void initBodyMenu(Menu menu) {
		final MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText("Export as CSV...");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportAction.run();
			}
		});
		
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
}
