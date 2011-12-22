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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import java.awt.Rectangle;
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

    private ChimpChat mChimpChat;
    private IChimpDevice mDevice; 
    private String connectedDeviceId = null;
    
    private static final String ANDROID_SDK = "/Users/obartley/Library/android-sdk-macosx-r15/";
    private static final String ADB = ANDROID_SDK + "platform-tools/adb";
    private static final long TIMEOUT = 5000;
	
    // lookup table to translate from Java keycodes to Android
    private Map<Integer, String> keyCodeMap = new TreeMap<Integer, String>();
    
    // Set to track which keycodes are currently in a down state
    private Set<Integer> keysPressed = new HashSet<Integer>();
    
	/**
	 * Create the application.
	 */
	public MonkeyBoard() {
		initialize();
		initializeKeyCodeMap();
		refreshDeviceList();
		TreeMap<String, String> options = new TreeMap<String, String>();
        options.put("backend", "adb");
        options.put("adbLocation", ADB);
		mChimpChat = ChimpChat.getInstance(options);
	}
	
	/**
	 *  Append a String to the text in the console and force scrolling to the end of the doc
	 */
	public void toConsole(String arg0) {
		try {
			// get document from console and append arg0
			Document d = textConsole.getDocument();
			SimpleAttributeSet attributes = new SimpleAttributeSet();
			d.insertString(d.getLength(), '\n' + arg0, attributes);
			// force scrolling to end of output
			textConsole.scrollRectToVisible(new Rectangle(0, textConsole.getHeight()-2, 1, 1));
		} catch (Exception e) {
			System.err.println("Error instering:" + arg0);
			e.printStackTrace();
		}
	}
	
	/**
	 * Capture output from `adb devices`, parses it and returns data in a Map in the form of
	 * deviceId:key::status:value
	 * @return
	 */
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
					rv.put(s[0], s[1]); // add deviceId, status to Map
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return rv;
	}	
	
	/**
	 * refreshes data in listModel to reflect the current status of devices connected to adb
	 */
	private void refreshDeviceList() {
		Map<String, String> adb = getAdbStatus();
		Iterator<Entry<String, String>> adbDevices = adb.entrySet().iterator();
		Boolean foundConnectedDevice = false;
		
		// iterate over the items in adb
		// TODO: make listModel items persist and update the list on an interval
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
		try {
	        mDevice = mChimpChat.waitForConnection(TIMEOUT, deviceId);
	        if ( mDevice == null ) throw new RuntimeException("Couldn't connect.");
	        mDevice.wake();
	        this.connectedDeviceId = deviceId;
	        toConsole("connected to device: " + deviceId);
		} catch (Exception e) {
			e.printStackTrace();
			mDevice = null;
			connectedDeviceId = null;
        	toConsole("couldn't connect to device: " + deviceId);    
		}
        this.refreshDeviceList();
	}
	
	/**
	 * Handles keyPress and keyRelease events to be sent to connected device
	 * 
	 * @param keyCode
	 * @param modifiers
	 * @param type
	 */
	private void keyEventHandler(int keyCode, int modifiers, TouchPressType type) {
		String code = null;
		String stype = (type == TouchPressType.DOWN)?"DOWN":"UP";
		
		// don't stream multiple Back keyDowns, because only the first one matters 
		if (( keyCode == KeyEvent.VK_ESCAPE ) && 
				(keysPressed.contains(KeyEvent.VK_ESCAPE)) &&
				(type == TouchPressType.DOWN))
			return;
		
		// look up the Java keycode in the Map, exit early if a match can't be found
		try {
			code = keyCodeMap.get(keyCode);
		} catch (Exception e) {
			toConsole("[" + Integer.toString(keyCode) + ":" + Integer.toString(modifiers) + "] KEYCODE_UNKNOWN " + stype);
			return;
		}

		toConsole("[" + Integer.toString(keyCode) + ":" + Integer.toString(modifiers) + "] " + code + " " + stype);

		// make sure the state of the key is properly stored
		switch(type) {
		case DOWN:keysPressed.add(keyCode); break;
		case UP:keysPressed.remove(keyCode); break;
		}

		// DOWN_AND_UP is a special case reserved for use by resetKeysPressed()
		if (type == TouchPressType.DOWN_AND_UP) type = TouchPressType.UP;
		
		// actually send the key event if we're connected to a device
		if ((this.connectedDeviceId != null) && (type != TouchPressType.DOWN_AND_UP)) {
			mDevice.press(code, type);
		}

	}

	/**
	 * iterate over items in keysPressed to return all keys to an unpressed state
	 * this is useful as a deadfall switch to quickly return all keys issued a down command
	 * a matching up command in the event of lost focus
	 */
	private void resetKeysPressed() {
		Iterator<Integer> iter = keysPressed.iterator();
		int code;
	    while (iter.hasNext()) {
			if (this.connectedDeviceId != null) {
				code = iter.next();
				keyEventHandler(code, 0, TouchPressType.DOWN_AND_UP);
			} 
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
				keyEventHandler(e.getKeyCode(), e.getModifiers(), TouchPressType.DOWN);
			}
			@Override
			public void keyReleased(KeyEvent e) {
				keyEventHandler(e.getKeyCode(), e.getModifiers(), TouchPressType.UP);
			}
		});
		btnMonkeyBoard.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent arg0) {
				toConsole("key events released");
				btnMonkeyBoard.setSelected(false);
				resetKeysPressed();
			}
			@Override
			public void focusGained(FocusEvent arg0) {
				toConsole("key events trapped");
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
	
	public void initializeKeyCodeMap() {
		// modifiers
		keyCodeMap.put(KeyEvent.VK_SHIFT, "KEYCODE_SHIFT_LEFT");
		keyCodeMap.put(KeyEvent.VK_CONTROL, "KEYCODE_CTRL_LEFT");					
		keyCodeMap.put(KeyEvent.VK_ALT, "KEYCODE_ALT_LEFT");
	
		// alphanumeric
		keyCodeMap.put(KeyEvent.VK_A, "KEYCODE_A");
		keyCodeMap.put(KeyEvent.VK_B, "KEYCODE_B");
		keyCodeMap.put(KeyEvent.VK_C, "KEYCODE_C");
		keyCodeMap.put(KeyEvent.VK_D, "KEYCODE_D");
		keyCodeMap.put(KeyEvent.VK_E, "KEYCODE_E");
		keyCodeMap.put(KeyEvent.VK_F, "KEYCODE_F");
		keyCodeMap.put(KeyEvent.VK_G, "KEYCODE_G");
		keyCodeMap.put(KeyEvent.VK_H, "KEYCODE_H");
		keyCodeMap.put(KeyEvent.VK_I, "KEYCODE_I");
		keyCodeMap.put(KeyEvent.VK_J, "KEYCODE_J");
		keyCodeMap.put(KeyEvent.VK_K, "KEYCODE_K");
		keyCodeMap.put(KeyEvent.VK_L, "KEYCODE_L");
		keyCodeMap.put(KeyEvent.VK_M, "KEYCODE_M");
		keyCodeMap.put(KeyEvent.VK_N, "KEYCODE_N");
		keyCodeMap.put(KeyEvent.VK_O, "KEYCODE_O");
		keyCodeMap.put(KeyEvent.VK_P, "KEYCODE_P");
		keyCodeMap.put(KeyEvent.VK_Q, "KEYCODE_Q");
		keyCodeMap.put(KeyEvent.VK_R, "KEYCODE_R");
		keyCodeMap.put(KeyEvent.VK_S, "KEYCODE_S");
		keyCodeMap.put(KeyEvent.VK_T, "KEYCODE_T");
		keyCodeMap.put(KeyEvent.VK_U, "KEYCODE_U");
		keyCodeMap.put(KeyEvent.VK_V, "KEYCODE_V");
		keyCodeMap.put(KeyEvent.VK_W, "KEYCODE_W");
		keyCodeMap.put(KeyEvent.VK_X, "KEYCODE_X");
		keyCodeMap.put(KeyEvent.VK_Y, "KEYCODE_Y");
		keyCodeMap.put(KeyEvent.VK_Z, "KEYCODE_Z");
		keyCodeMap.put(KeyEvent.VK_0, "KEYCODE_0");
		keyCodeMap.put(KeyEvent.VK_1, "KEYCODE_1");
		keyCodeMap.put(KeyEvent.VK_2, "KEYCODE_2");
		keyCodeMap.put(KeyEvent.VK_3, "KEYCODE_3");
		keyCodeMap.put(KeyEvent.VK_4, "KEYCODE_4");
		keyCodeMap.put(KeyEvent.VK_5, "KEYCODE_5");
		keyCodeMap.put(KeyEvent.VK_6, "KEYCODE_6");
		keyCodeMap.put(KeyEvent.VK_7, "KEYCODE_7");
		keyCodeMap.put(KeyEvent.VK_8, "KEYCODE_8");
		keyCodeMap.put(KeyEvent.VK_9, "KEYCODE_9");
		
		// dpad
		keyCodeMap.put(KeyEvent.VK_UP, "KEYCODE_DPAD_UP");
		keyCodeMap.put(KeyEvent.VK_DOWN, "KEYCODE_DPAD_DOWN");
		keyCodeMap.put(KeyEvent.VK_LEFT, "KEYCODE_DPAD_LEFT");
		keyCodeMap.put(KeyEvent.VK_RIGHT, "KEYCODE_DPAD_RIGHT");
		//keyCodeMap.put(KeyEvent.VK_ENTER, "KEYCODE_DPAD_CENTER";
		
		//keyCodeMap.put(KeyEvent.VK_SOFT_LEFT, "KEYCODE_SOFT_LEFT";
		//keyCodeMap.put(KeyEvent.VK_SOFT_RIGHT, "KEYCODE_SOFT_RIGHT";
		keyCodeMap.put(KeyEvent.VK_HOME, "KEYCODE_HOME");
		keyCodeMap.put(KeyEvent.VK_ESCAPE, "KEYCODE_BACK");
		//keyCodeMap.put(KeyEvent.VK_CALL, "KEYCODE_CALL";
		//keyCodeMap.put(KeyEvent.VK_ENDCALL, "KEYCODE_ENDCALL";
		//keyCodeMap.put(KeyEvent.VK_STAR, "KEYCODE_STAR";
		//keyCodeMap.put(KeyEvent.VK_POUND, "KEYCODE_POUND";
		//keyCodeMap.put(KeyEvent.VK_VOLUME_UP, "KEYCODE_VOLUME_UP";
		//keyCodeMap.put(KeyEvent.VK_VOLUME_DOWN, "KEYCODE_VOLUME_DOWN";
		//keyCodeMap.put(KeyEvent.VK_POWER, "KEYCODE_POWER";
		//keyCodeMap.put(KeyEvent.VK_CAMERA, "KEYCODE_CAMERA";
		//keyCodeMap.put(KeyEvent.VK_CLEAR, "KEYCODE_CLEAR";
		keyCodeMap.put(KeyEvent.VK_COMMA, "KEYCODE_COMMA");
		keyCodeMap.put(KeyEvent.VK_PERIOD, "KEYCODE_PERIOD");
		keyCodeMap.put(KeyEvent.VK_TAB, "KEYCODE_TAB");
		keyCodeMap.put(KeyEvent.VK_SPACE, "KEYCODE_SPACE");
		keyCodeMap.put(KeyEvent.VK_ENTER, "KEYCODE_ENTER");
		keyCodeMap.put(KeyEvent.VK_DELETE, "KEYCODE_DEL");
		keyCodeMap.put(KeyEvent.VK_BACK_SPACE, "KEYCODE_DEL");
		keyCodeMap.put(KeyEvent.VK_DEAD_GRAVE, "KEYCODE_GRAVE");
		keyCodeMap.put(KeyEvent.VK_MINUS, "KEYCODE_MINUS");
		keyCodeMap.put(KeyEvent.VK_EQUALS, "KEYCODE_EQUALS");
		keyCodeMap.put(KeyEvent.VK_OPEN_BRACKET, "KEYCODE_LEFT_BRACKET");
		keyCodeMap.put(KeyEvent.VK_CLOSE_BRACKET, "KEYCODE_RIGHT_BRACKET");
		keyCodeMap.put(KeyEvent.VK_BACK_SLASH, "KEYCODE_BACKSLASH");
		keyCodeMap.put(KeyEvent.VK_SEMICOLON, "KEYCODE_SEMICOLON");
		keyCodeMap.put(KeyEvent.VK_SLASH, "KEYCODE_SLASH");
		keyCodeMap.put(KeyEvent.VK_AT, "KEYCODE_AT");
		keyCodeMap.put(KeyEvent.VK_PLUS, "KEYCODE_PLUS");
		keyCodeMap.put(KeyEvent.VK_PAGE_UP, "KEYCODE_PAGE_UP");
		keyCodeMap.put(KeyEvent.VK_PAGE_DOWN, "KEYCODE_PAGE_DOWN");
	}
}
