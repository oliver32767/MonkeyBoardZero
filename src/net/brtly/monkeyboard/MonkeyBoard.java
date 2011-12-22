package net.brtly.monkeyboard;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;

import com.android.chimpchat.ChimpChat;
import com.android.chimpchat.core.IChimpDevice;
import com.android.chimpchat.core.TouchPressType;
import com.android.ddmlib.Log;
import com.android.ddmlib.ShellCommandUnresponsiveException;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.border.MatteBorder;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;

import java.awt.Cursor;
import java.awt.Event;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.Toolkit;
import javax.swing.JMenuBar;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTextPane;
import java.awt.Dimension;
import java.awt.Component;
import javax.swing.SwingConstants;
import java.awt.Font;
import javax.swing.JScrollPane;
import java.awt.event.InputEvent;

public class MonkeyBoard {
	private DefaultListModel listModel = new DefaultListModel();
	private JList listView = null;
	private JButton btnMonkeyBoard = null;
	private JTextPane textConsole = null;
	private JFrame frmMonkeyboard;
	
    private static final String ANDROID_SDK = "/Users/obartley/Library/android-sdk-macosx-r15/";
    private static final String ADB = ANDROID_SDK + "platform-tools/adb";
    
    private static final long TIMEOUT = 5000;
    private static final int REFRESH_INTERVAL = 3000;
    
    private ChimpChat mChimpChat;
    private IChimpDevice mDevice; 
    private String connectedDeviceId = null;
    
	private int keyPressStatus; //increased everytime there's a keyPress event, -- on keyReleased	
	
	/**
	 * Create the application.
	 */
	public MonkeyBoard() {
		initialize();

//		// set up the timer to refresh the device list
//		ActionListener timerTick = new ActionListener() {
//		      public void actionPerformed(ActionEvent e) {
//		          refreshDeviceList();
//		      }
//		};
//		new Timer(REFRESH_INTERVAL, timerTick).start();
		refreshDeviceList();
		TreeMap<String, String> options = new TreeMap<String, String>();
        options.put("backend", "adb");
        options.put("adbLocation", ADB);
		mChimpChat = ChimpChat.getInstance(options);
		
		
	}
	
	public void toConsole(String arg0) {
		try {
			Document d = textConsole.getDocument();
			SimpleAttributeSet attributes = new SimpleAttributeSet();
			d.insertString(d.getLength(), '\n' + arg0, attributes);
		} catch (Exception e) {
			System.err.println("Error instering:" + arg0);
			e.printStackTrace();
		}
	}
	
	private Map<String, String> getAdbStatus() {
		Map<String, String> rv = new HashMap<String, String>();
		// Capture output from `adb devices` and map connected deviceIds to their status
		// TODO: remove absolute path to adb or make dynamic
		
		String cmd = ADB + " devices";
		Runtime run = Runtime.getRuntime();
		Process pr = null;
		
		// execute cmd
		try {
			pr = run.exec(cmd);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			pr.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// parse output
		BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		String line = "";
		try {
			while ((line=buf.readLine())!=null) {
				
				if ( ! (line.startsWith("List") || line.length() <= 1) ) {
					String[] s = line.split("\\s+"); //it's a tab separated list, dude!
					//System.out.println(line + ":" + Integer.toString(s.length));
					rv.put(s[0], s[1]); // add deviceId, status to Map
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return rv;
	}	
	
	private void refreshDeviceList() {
		Map<String, String> adb = getAdbStatus();
		Iterator<Entry<String, String>> adbDevices = adb.entrySet().iterator();
		Boolean foundConnectedDevice = false;
		
		// iterate over the items in adb
		listModel.clear();
		while (adbDevices.hasNext()) {
			Entry<String, String> dev =  (Entry<String, String>) adbDevices.next();

			String devId = dev.getKey().trim();
			String devStatus = dev.getValue().trim();
			Map <String, String> elem = new HashMap<String, String>();


			// build list element
			if ( devId.equals(connectedDeviceId) ) {
				devStatus = "connected";
				foundConnectedDevice = true;
			}
			//System.out.println("(" + this.connectedDeviceId + ")" + devId + ":" + devStatus);
			elem.put("deviceId", devId);
			elem.put("deviceStatus", devStatus);
			
			// TODO: make elements persist between refreshes
//			// check to see if there's already an entry in listModel
//			if ( ! listModel.contains(elem)) {
//				listModel.addElement(elem);
//			}
			listModel.addElement(elem);
		}
		
		if ( ! foundConnectedDevice) {
			// a deviceId matching connectedDevcieId was not found, reset connection
			connectedDeviceId = null;
			if (mDevice != null)
				mDevice.dispose();
			mDevice = null;
		}
		
		// now we scrub the list for stuff that isn't in the deviceList anymore
		if ( listModel.getSize() > 0) {
			for (int i = 0; i == listModel.getSize(); i++) {
				if (adb.keySet().contains(listModel.getElementAt(i)))
					listModel.remove(i);
			}
		}
	}
	
	private void connectToDevice(String deviceId) {
		//TODO: maybe make this threaded, so an unresponsive emulator/device
		// doesn't block the main thread
		toConsole("Connecting to: " + deviceId);
		try {
	        mDevice = mChimpChat.waitForConnection(TIMEOUT, deviceId);
	        if ( mDevice == null ) {
	                throw new RuntimeException("Couldn't connect.");
	        }
	        mDevice.wake();

	        this.connectedDeviceId = deviceId;
		} catch (Exception e) {
			e.printStackTrace();
			mDevice = null;
			connectedDeviceId = null;
		}
        this.refreshDeviceList();
	}
	
	private void sendKeyToDevice(int keyCode, int modifiers) {
		String code = KeyCodeTable.lookup(keyCode, modifiers);
		      
		toConsole("[" + Integer.toString(keyCode) + ":" + Integer.toString(modifiers) + "]" + code);
		if (this.connectedDeviceId != null) {
		      mDevice.press(code, TouchPressType.DOWN_AND_UP);//down?MonkeyDevice.DOWN : MonkeyDevice.UP);
		} 
	}
	
	
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MonkeyBoard window = new MonkeyBoard();
					window.frmMonkeyboard.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}



	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmMonkeyboard = new JFrame();
		frmMonkeyboard.setMinimumSize(new Dimension(512, 360));
		frmMonkeyboard.setTitle("MonkeyBoard");
		frmMonkeyboard.setBounds(100, 100, 512, 360);
		frmMonkeyboard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// moved JList declaration to class-level declarartions
		listView = new JList(listModel);
		listView.setAlignmentY(Component.TOP_ALIGNMENT);
		listView.setAlignmentX(Component.LEFT_ALIGNMENT);
		listView.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		listView.setCellRenderer(new DeviceListRenderer());
		JButton btnRefresh = new JButton("Refresh");
		btnRefresh.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				toConsole("refreshing device list...");
				refreshDeviceList();
			}
		});
		
		JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		btnConnect.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					HashMap<String, String> v = (HashMap<String, String>) listView.getSelectedValue();
					connectToDevice(v.get("deviceId"));
				} catch (NullPointerException err) {
					//Null pointer just means there isn't anything selected
				}
			}
		});
		
		btnMonkeyBoard = new JButton("");
		btnMonkeyBoard.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				// if the button has focus, then clicking it will give focus to something else, like a toggle switch.
				if ( btnMonkeyBoard.hasFocus() ) {
					textConsole.requestFocus();
				}
			}
		});
		btnMonkeyBoard.setHorizontalTextPosition(SwingConstants.CENTER);
		btnMonkeyBoard.setFocusTraversalKeysEnabled(false);
		btnMonkeyBoard.setAlignmentX(Component.CENTER_ALIGNMENT);
		btnMonkeyBoard.setBorderPainted(false);
		btnMonkeyBoard.setIconTextGap(0);
		btnMonkeyBoard.setPressedIcon(new ImageIcon(MonkeyBoard.class.getResource("/res/android_large_sel.png")));
		btnMonkeyBoard.setSelectedIcon(new ImageIcon(MonkeyBoard.class.getResource("/res/android_large_sel.png")));
		btnMonkeyBoard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnMonkeyBoard.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
//				keyPressStatus++;
//				btnMonkeyBoard.setSelected(true);
				
				sendKeyToDevice(e.getKeyCode(), e.getModifiers());
			}
			@Override
			public void keyReleased(KeyEvent e) {
//				keyPressStatus--;
//				if ( keyPressStatus <= 0 ) {
//					btnMonkeyBoard.setSelected(false);
//				}
			}
		});
		btnMonkeyBoard.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent arg0) {
				//keyPressStatus = 0;
				btnMonkeyBoard.setSelected(false);
			}
			@Override
			public void focusGained(FocusEvent arg0) {
				//keyPressStatus = 0;
				btnMonkeyBoard.setSelected(true);
			}
		});	
		btnMonkeyBoard.setBorder(null);
		btnMonkeyBoard.setIcon(new ImageIcon(MonkeyBoard.class.getResource("/res/android_large.png")));
		
		JScrollPane scrollPane = new JScrollPane();

		GroupLayout groupLayout = new GroupLayout(frmMonkeyboard.getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 438, Short.MAX_VALUE)
							.addContainerGap())
						.addGroup(groupLayout.createSequentialGroup()
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addGroup(groupLayout.createSequentialGroup()
									.addComponent(btnRefresh)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(btnConnect)
									.addGap(0, 0, Short.MAX_VALUE))
								.addComponent(listView, GroupLayout.DEFAULT_SIZE, 193, Short.MAX_VALUE))
							.addGap(3)
							.addComponent(btnMonkeyBoard, GroupLayout.PREFERRED_SIZE, 246, Short.MAX_VALUE)
							.addGap(2))))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(listView, GroupLayout.PREFERRED_SIZE, 244, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(btnConnect)
								.addComponent(btnRefresh)))
						.addComponent(btnMonkeyBoard, GroupLayout.PREFERRED_SIZE, 279, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 92, Short.MAX_VALUE)
					.addContainerGap())
		);
		
		textConsole = new JTextPane();
		textConsole.setText("ready");
		textConsole.setForeground(Color.GREEN);
		textConsole.setFont(new Font("Monospaced", Font.PLAIN, 15));
		textConsole.setEditable(false);
		textConsole.setBackground(Color.BLACK);
		scrollPane.setViewportView(textConsole);
		frmMonkeyboard.getContentPane().setLayout(groupLayout);
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.setBorder(null);
		frmMonkeyboard.setJMenuBar(menuBar);
		
		JMenu mnActions = new JMenu("Actions");
		menuBar.add(mnActions);
		
		JMenuItem mntmRestartAdbServer = new JMenuItem("Restart adb server");
		mntmRestartAdbServer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.META_MASK));
		mnActions.add(mntmRestartAdbServer);		
		
		JMenuItem mntmRefreshDeviceList = new JMenuItem("Refresh Device List");
		mntmRefreshDeviceList.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.META_MASK));
		mntmRefreshDeviceList.setMnemonic('r');
		mntmRefreshDeviceList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			toConsole("refreshing device list...");
			refreshDeviceList();
			}
		});
		mnActions.add(mntmRefreshDeviceList);
				
		JMenuItem mntmConnectToDevice = new JMenuItem("Connect To Device");
		mntmConnectToDevice.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.META_MASK));
		mnActions.add(mntmConnectToDevice);
		
		mnActions.add(new JSeparator());
		
		JMenuItem mntmInstallapk = new JMenuItem("Install *.apk...");
		mntmInstallapk.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.META_MASK));
		mnActions.add(mntmInstallapk);
		
		JMenuItem mntmUninstallPackage = new JMenuItem("Uninstall Package...");
		mntmUninstallPackage.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.META_MASK));
		mnActions.add(mntmUninstallPackage);
		
		JMenuItem mntmExecuteShellCommand = new JMenuItem("Execute Shell Command...");
		mntmExecuteShellCommand.setMnemonic('x');
		mntmExecuteShellCommand.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Event.META_MASK));
		mnActions.add(mntmExecuteShellCommand);

		mnActions.add(new JSeparator());
		
		JMenuItem mntmSaveScreenshot = new JMenuItem("Save Screenshot");
		mntmSaveScreenshot.setMnemonic('s');
		mntmSaveScreenshot.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.META_MASK));
		mnActions.add(mntmSaveScreenshot);
		
		JMenuItem mntmSaveScreenshotAs = new JMenuItem("Save Screenshot As...");
		mntmSaveScreenshotAs.setMnemonic('a');
		mntmSaveScreenshotAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.META_MASK));
		mnActions.add(mntmSaveScreenshotAs);
		
		JMenuItem mntmDisplayScreenshot = new JMenuItem("Display Screenshot...");
		mntmDisplayScreenshot.setMnemonic('d');
		mntmDisplayScreenshot.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Event.META_MASK));
		mnActions.add(mntmDisplayScreenshot);
		
		mnActions.add(new JSeparator());
		
		JMenuItem mntmQuit = new JMenuItem("Quit");
		mntmQuit.setMnemonic('q');
		mntmQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.META_MASK)); 
		mnActions.add(mntmQuit);
		
		JMenu mnKeys = new JMenu("Keys");
		menuBar.add(mnKeys);
		
		JMenuItem mntmHome = new JMenuItem("Home");
		mntmHome.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.META_MASK));
		mnKeys.add(mntmHome);
		
		JMenuItem mntmMenu = new JMenuItem("Menu");
		mntmMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.META_MASK));
		mnKeys.add(mntmMenu);
		
		JMenuItem mntmSearch = new JMenuItem("Search");
		mntmSearch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, InputEvent.META_MASK));
		mnKeys.add(mntmSearch);
		
		JMenuItem mntmPower = new JMenuItem("Power");
		mntmPower.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, InputEvent.META_MASK));
		mnKeys.add(mntmPower);
		
		JMenuItem mntmCamera = new JMenuItem("Camera");
		mntmCamera.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.META_MASK));
		mnKeys.add(mntmCamera);
		
		JSeparator separator = new JSeparator();
		mnKeys.add(separator);
		
		JMenuItem mntmVolumeUp = new JMenuItem("Volume Up");
		mntmVolumeUp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.META_MASK));
		mnKeys.add(mntmVolumeUp);
		
		JMenuItem mntmVolumeDown = new JMenuItem("Volume Down");
		mntmVolumeDown.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.META_MASK));
		mnKeys.add(mntmVolumeDown);
	}
}
