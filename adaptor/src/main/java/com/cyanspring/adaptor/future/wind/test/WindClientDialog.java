package com.cyanspring.adaptor.future.wind.test;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;

import com.cyanspring.id.Library.Frame.FrameUtil;
import com.cyanspring.id.Library.Frame.IFrameClose;
import com.cyanspring.id.Library.Frame.InfoLabel;
import com.cyanspring.id.Library.Frame.InfoString;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.SimpleDateFormat;

@SuppressWarnings("serial")
public class WindClientDialog extends JFrame {

	final static int FRAME_WIDTH = 350;
	final static int FRAME_HEIGHT = 400;
	final static int FRAME_UI_01_HEIGHT = 60;
	
	final static int LIST_MAX_LINE = 500;
	final static int LIST_CLEAR_COUNT = 50;
	
			

	WindClientDialog mainFrame;
	public boolean isUsingMsg = false;
	private JPanel defaultPanel, inputPanel;
	JLabel statusbar;
	IFrameClose callback;
	JScrollPane scrollLog, scrollMsg;

	private JSplitPane splitPaneV, splitPaneV2;
	private DefaultListModel<InfoString> listLogModel = null;
	private JList<InfoString> listLog = null;
	private DefaultListModel<String> listMsgModel = null;
	private JList<String> listMsg = null;

	@SuppressWarnings("unchecked")
	public WindClientDialog(IFrameClose srcCallback, boolean bUsingMsg) {
		callback = srcCallback;
		isUsingMsg = bUsingMsg;
		setBackground(Color.LIGHT_GRAY);

		defaultPanel = new JPanel((LayoutManager) new FlowLayout(FlowLayout.LEFT));
		defaultPanel.setLayout(new BoxLayout(defaultPanel, BoxLayout.LINE_AXIS));
		defaultPanel.setPreferredSize(new Dimension(FRAME_WIDTH, 30));

		inputPanel = new JPanel((LayoutManager) new FlowLayout(FlowLayout.LEFT));
		inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.LINE_AXIS));
		inputPanel.setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_UI_01_HEIGHT - 30));

		splitPaneV2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, defaultPanel, inputPanel);
		FrameUtil.flattenSplitPane(splitPaneV2);

		if (isUsingMsg) {
			listMsgModel = new DefaultListModel<String>();
			listMsg = new JList<String>(listMsgModel);
			scrollMsg = createMsgScrollPanel(listMsg);
			scrollMsg.setSize(FRAME_WIDTH, 150);

			listLogModel = new DefaultListModel<InfoString>();
			listLog = new JList<InfoString>(listLogModel);
			listLog.setCellRenderer(new InfoLabel());
			scrollLog = createLogScrollPanel(listLog);

			final JSplitPane splitPaneV3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollMsg, scrollLog);

			splitPaneV = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPaneV2, splitPaneV3);

			splitPaneV3.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					splitPaneV3.setDividerLocation(1.0 / 2.0);
				}
			});

		} else {
			listLogModel = new DefaultListModel<InfoString>();
			listLog = new JList<InfoString>(listLogModel);
			listLog.setCellRenderer(new InfoLabel());
			scrollLog = createLogScrollPanel(listLog);

			splitPaneV = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPaneV2, scrollLog);
		}

		getContentPane().add(splitPaneV);

		setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));

		ImageIcon icon2 = new ImageIcon("Image/idclient.jpg");
		this.setIconImage(icon2.getImage());

		statusbar = new JLabel(" Statusbar");
		statusbar.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		add(statusbar, BorderLayout.SOUTH);

		final WindClientDialog dialog = this;
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				callback.onCloseAction();
				dialog.dispose();
			}
		});
	}

	public void addButton(JButton button) {
		inputPanel.add(button);
	}

	public JLabel addLabel(JLabel label) {
		inputPanel.add(label);
		return label;
	}

	public void addCheckBox(JCheckBox check) {
		inputPanel.add(check);
	}	
	
	public void addTextField(JTextField text) {
		inputPanel.add(text);
	}

	public void addSpace() {
		inputPanel.add(Box.createHorizontalGlue());
	}

	public void setStatus(String strTxt) {
		final String strStatusMsg = strTxt;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				statusbar.setText(strStatusMsg);
			}
		});

	}

	public void createDefaultPanel() {
		defaultPanel.add(Box.createHorizontalGlue());
		JButton close = new JButton("Close");
		close.setToolTipText("A button component : Close");
		final WindClientDialog dialog = this;
		close.addActionListener(new ActionListener() {			
			public void actionPerformed(ActionEvent event) {
				callback.onCloseAction();
				dialog.dispose();
			}
		});
		defaultPanel.add(close);
		defaultPanel.add(new JLabel("  "));

	}

	public JScrollPane createLogScrollPanel(JList<InfoString> listLog) {
		listLog.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listLog.setSelectedIndex(listLogModel.size() - 1);

		return new JScrollPane(listLog);

	}

	public JScrollPane createMsgScrollPanel(JList<String> listMsg) {
		listMsg.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listMsg.setSelectedIndex(listMsgModel.size() - 1);
		return new JScrollPane(listMsg);

	}

	public void addLog(String f, Object... args) {
		addLogNoTime(String.format(f, args));
	}
	
	public void addLogNoTime(String s) {
		final InfoString value = new InfoString(InfoString.Info, s);
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (listLogModel != null)
					if (listLogModel.size() > LIST_MAX_LINE) {
						for (int i = 0; i < LIST_CLEAR_COUNT; i++) {
							listLogModel.removeElementAt(0);				
						}
					}
					listLogModel.addElement(value);
				if (listLog != null) {
					int lastIndex = listLog.getModel().getSize() - 1;
					if (lastIndex > 0) {
						listLog.setSelectedIndex(lastIndex);
						listLog.ensureIndexIsVisible(lastIndex);
					}
				}
			}
		});	
	}
	
	public void addLog(Integer nLevel, String f, Object... args) {

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String s = String.format("[%s] %-10s %s", sdf.format(new java.util.Date()), InfoString.getString(nLevel),
				String.format(f, args));
		
		final InfoString value = new InfoString(nLevel, s);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (listLogModel != null) {
					if (listLogModel.size() > LIST_MAX_LINE) {
						for (int i = 0; i < LIST_CLEAR_COUNT; i++) {
							listLogModel.removeElementAt(0);				
						}
					}
					listLogModel.addElement(value);
				}
				if (listLog != null) {
					int lastIndex = listLog.getModel().getSize() - 1;
					if (lastIndex > 0) {
						listLog.setSelectedIndex(lastIndex);
						listLog.ensureIndexIsVisible(lastIndex);
					}
				}
			}
		});
	}

	public void clearAll() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (listLogModel != null) {
					listLogModel.removeAllElements();
				}
				if (listLog != null) {
					listLog.removeAll();
				}
			}
		});
	}
	
	public void addMsg(String f, Object... args) {

		final String strMsg = String.format(f, args);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (listMsgModel != null){
					if (listMsgModel.size() > LIST_MAX_LINE) {
						for (int i = 0; i < LIST_CLEAR_COUNT; i++) {
							listMsgModel.removeElementAt(0);			
						}
					}
					listMsgModel.addElement(strMsg);
				}
				
				if (listMsg != null) {
					int lastIndex = listMsg.getModel().getSize() - 1;
					if (lastIndex > 0) {
						listMsg.setSelectedIndex(lastIndex);
						listMsg.ensureIndexIsVisible(lastIndex);
					}
				}
			}
		});
	}

	public static WindClientDialog Instance(IFrameClose caller, String strTitle) {
		try {

			// UIManager
			// .setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

		} catch (Exception evt) {
		}

		if (strTitle == null || strTitle.isEmpty()) {
			strTitle = "IDForexFeed";
		}
		
		final WindClientDialog mainFrame = new WindClientDialog(caller, false);
		mainFrame.setTitle(strTitle);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				mainFrame.createDefaultPanel();
				mainFrame.pack();
				mainFrame.setVisible(true);
				mainFrame.setLocationRelativeTo(null);
				//mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			}
		});
		return mainFrame;
	}
}