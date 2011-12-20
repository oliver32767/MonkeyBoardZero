package net.brtly.monkeyboard;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
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
import java.awt.Cursor;
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

public class MonkeyBoard {
	private DefaultListModel listModel = new DefaultListModel();
	private JList listView = null;
	private JButton btnMonkeyBoard = null;
	private JLabel lblOutput = null;
	
    private static final String ADB = "/Users/obartley/Library/android-sdk-macosx-r15/platform-tools/adb";
	private String connectedDeviceId = null; 
    private static final long TIMEOUT = 5000;
    private static final int REFRESH_INTERVAL = 3000;
    private ChimpChat mChimpChat;
    private IChimpDevice mDevice; 
	
	private JFrame frmMonkeyboard;
	
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
					//TODO: replace with Log

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
		System.out.println("Connecting to: " + deviceId);
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
		if (this.connectedDeviceId != null) {
		      String code = "KEYCODE_UNKNOWN";
		      switch(keyCode){
		          //case KeyEvent.VK_SOFT_LEFT: code = "KEYCODE_SOFT_LEFT";
		          //case KeyEvent.VK_SOFT_RIGHT: code = "KEYCODE_SOFT_RIGHT";
		          case KeyEvent.VK_HOME: code = "KEYCODE_HOME"; break;
		          case KeyEvent.VK_ESCAPE: code = "KEYCODE_BACK"; break;
		          //case KeyEvent.VK_CALL: code = "KEYCODE_CALL";
		          //case KeyEvent.VK_ENDCALL: code = "KEYCODE_ENDCALL";
		          case KeyEvent.VK_0: code = "KEYCODE_0"; break;
		          case KeyEvent.VK_1: code = "KEYCODE_1"; break;
		          case KeyEvent.VK_2: code = "KEYCODE_2"; break;
		          case KeyEvent.VK_3: code = "KEYCODE_3"; break;
		          case KeyEvent.VK_4: code = "KEYCODE_4"; break;
		          case KeyEvent.VK_5: code = "KEYCODE_5"; break;
		          case KeyEvent.VK_6: code = "KEYCODE_6"; break;
		          case KeyEvent.VK_7: code = "KEYCODE_7"; break;
		          case KeyEvent.VK_8: code = "KEYCODE_8"; break;
		          case KeyEvent.VK_9: code = "KEYCODE_9"; break;
		          //case KeyEvent.VK_STAR: code = "KEYCODE_STAR";
		          //case KeyEvent.VK_POUND: code = "KEYCODE_POUND";
		          case KeyEvent.VK_UP: code = "KEYCODE_DPAD_UP"; break;
		          case KeyEvent.VK_DOWN: code = "KEYCODE_DPAD_DOWN"; break;
		          case KeyEvent.VK_LEFT: code = "KEYCODE_DPAD_LEFT"; break;
		          case KeyEvent.VK_RIGHT: code = "KEYCODE_DPAD_RIGHT"; break;
		          //case KeyEvent.VK_ENTER: code = "KEYCODE_DPAD_CENTER";
		          //case KeyEvent.VK_VOLUME_UP: code = "KEYCODE_VOLUME_UP";
		          //case KeyEvent.VK_VOLUME_DOWN: code = "KEYCODE_VOLUME_DOWN";
		          //case KeyEvent.VK_POWER: code = "KEYCODE_POWER";
		          //case KeyEvent.VK_CAMERA: code = "KEYCODE_CAMERA";
		          //case KeyEvent.VK_CLEAR: code = "KEYCODE_CLEAR";
		          case KeyEvent.VK_A: code = "KEYCODE_A"; break;
		          case KeyEvent.VK_B: code = "KEYCODE_B"; break;
		          case KeyEvent.VK_C: code = "KEYCODE_C"; break;
		          case KeyEvent.VK_D: code = "KEYCODE_D"; break;
		          case KeyEvent.VK_E: code = "KEYCODE_E"; break;
		          case KeyEvent.VK_F: code = "KEYCODE_F"; break;
		          case KeyEvent.VK_G: code = "KEYCODE_G"; break;
		          case KeyEvent.VK_H: code = "KEYCODE_H"; break;
		          case KeyEvent.VK_I: code = "KEYCODE_I"; break;
		          case KeyEvent.VK_J: code = "KEYCODE_J"; break;
		          case KeyEvent.VK_K: code = "KEYCODE_K"; break;
		          case KeyEvent.VK_L: code = "KEYCODE_L"; break;
		          case KeyEvent.VK_M: code = "KEYCODE_M"; break;
		          case KeyEvent.VK_N: code = "KEYCODE_N"; break;
		          case KeyEvent.VK_O: code = "KEYCODE_O"; break;
		          case KeyEvent.VK_P: code = "KEYCODE_P"; break;
		          case KeyEvent.VK_Q: code = "KEYCODE_Q"; break;
		          case KeyEvent.VK_R: code = "KEYCODE_R"; break;
		          case KeyEvent.VK_S: code = "KEYCODE_S"; break;
		          case KeyEvent.VK_T: code = "KEYCODE_T"; break;
		          case KeyEvent.VK_U: code = "KEYCODE_U"; break;
		          case KeyEvent.VK_V: code = "KEYCODE_V"; break;
		          case KeyEvent.VK_W: code = "KEYCODE_W"; break;
		          case KeyEvent.VK_X: code = "KEYCODE_X"; break;
		          case KeyEvent.VK_Y: code = "KEYCODE_Y"; break;
		          case KeyEvent.VK_Z: code = "KEYCODE_Z"; break;
		          case KeyEvent.VK_COMMA: code = "KEYCODE_COMMA"; break;
		          case KeyEvent.VK_PERIOD: code = "KEYCODE_PERIOD"; break;
		          case KeyEvent.VK_ALT: code = "KEYCODE_ALT_LEFT"; break;
		          //case KeyEvent.VK_ALT_RIGHT: code = "KEYCODE_ALT_RIGHT";
		          case KeyEvent.VK_SHIFT: code = "KEYCODE_SHIFT_LEFT"; break;
		          //case KeyEvent.VK_SHIFT_RIGHT: code = "KEYCODE_SHIFT_RIGHT";
		          case KeyEvent.VK_TAB: code = "KEYCODE_TAB"; break;
		          case KeyEvent.VK_SPACE: code = "KEYCODE_SPACE"; break;
		          //case KeyEvent.VK_SYM: code = "KEYCODE_SYM";
		          //case KeyEvent.VK_EXPLORER: code = "KEYCODE_EXPLORER";
		          //case KeyEvent.VK_ENVELOPE: code = "KEYCODE_ENVELOPE";
		          case KeyEvent.VK_ENTER: code = "KEYCODE_ENTER"; break;
		          case KeyEvent.VK_DELETE: code = "KEYCODE_DEL"; break;
		          case KeyEvent.VK_BACK_SPACE: code = "KEYCODE_DEL"; break;
		          case KeyEvent.VK_DEAD_GRAVE: code = "KEYCODE_GRAVE"; break;
		          case KeyEvent.VK_MINUS: code = "KEYCODE_MINUS"; break;
		          case KeyEvent.VK_EQUALS: code = "KEYCODE_EQUALS"; break;
		          case KeyEvent.VK_OPEN_BRACKET: code = "KEYCODE_LEFT_BRACKET"; break;
		          case KeyEvent.VK_CLOSE_BRACKET: code = "KEYCODE_RIGHT_BRACKET"; break;
		          case KeyEvent.VK_BACK_SLASH: code = "KEYCODE_BACKSLASH"; break;
		          case KeyEvent.VK_SEMICOLON: code = "KEYCODE_SEMICOLON"; break;
		          //case KeyEvent.VK_APOSTROPHE: code = "KEYCODE_APOSTROPHE";
		          case KeyEvent.VK_SLASH: code = "KEYCODE_SLASH"; break;
		          case KeyEvent.VK_AT: code = "KEYCODE_AT"; break;
		          //case KeyEvent.VK_NUM: code = "KEYCODE_NUM";
		          //case KeyEvent.VK_HEADSETHOOK: code = "KEYCODE_HEADSETHOOK";
		          case KeyEvent.VK_PLUS: code = "KEYCODE_PLUS"; break;
		          //case KeyEvent.VK_MENU: code = "KEYCODE_MENU";
		          //case KeyEvent.VK_NOTIFICATION: code = "KEYCODE_NOTIFICATION";
		          //case KeyEvent.VK_SEARCH: code = "KEYCODE_SEARCH";
		          //case KeyEvent.VK_MEDIA_PLAY_PAUSE: code = "KEYCODE_MEDIA_PLAY_PAUSE";
		          //case KeyEvent.VK_MEDIA_STOP: code = "KEYCODE_MEDIA_STOP";
		          //case KeyEvent.VK_MEDIA_NEXT: code = "KEYCODE_MEDIA_NEXT";
		          //case KeyEvent.VK_MEDIA_PREVIOUS: code = "KEYCODE_MEDIA_PREVIOUS";
		          //case KeyEvent.VK_MEDIA_REWIND: code = "KEYCODE_MEDIA_REWIND";
		          //case KeyEvent.VK_MEDIA_FAST_FORWARD: code = "KEYCODE_MEDIA_FAST_FORWARD";
		          //case KeyEvent.VK_MUTE: code = "KEYCODE_MUTE";
		          case KeyEvent.VK_PAGE_UP: code = "KEYCODE_PAGE_UP"; break;
		          case KeyEvent.VK_PAGE_DOWN: code = "KEYCODE_PAGE_DOWN"; break;
//		          case KeyEvent.VK_BUTTON_A: code = "KEYCODE_BUTTON_A";
//		          case KeyEvent.VK_BUTTON_B: code = "KEYCODE_BUTTON_B";
//		          case KeyEvent.VK_BUTTON_C: code = "KEYCODE_BUTTON_C";
//		          case KeyEvent.VK_BUTTON_X: code = "KEYCODE_BUTTON_X";
//		          case KeyEvent.VK_BUTTON_Y: code = "KEYCODE_BUTTON_Y";
//		          case KeyEvent.VK_BUTTON_Z: code = "KEYCODE_BUTTON_Z";
//		          case KeyEvent.VK_BUTTON_L1: code = "KEYCODE_BUTTON_L1";
//		          case KeyEvent.VK_BUTTON_R1: code = "KEYCODE_BUTTON_R1";
//		          case KeyEvent.VK_BUTTON_L2: code = "KEYCODE_BUTTON_L2";
//		          case KeyEvent.VK_BUTTON_R2: code = "KEYCODE_BUTTON_R2";
//		          case KeyEvent.VK_BUTTON_THUMBL: code = "KEYCODE_BUTTON_THUMBL";
//		          case KeyEvent.VK_BUTTON_THUMBR: code = "KEYCODE_BUTTON_THUMBR";
//		          case KeyEvent.VK_BUTTON_START: code = "KEYCODE_BUTTON_START";
//		          case KeyEvent.VK_BUTTON_SELECT: code = "KEYCODE_BUTTON_SELECT";
//		          case KeyEvent.VK_BUTTON_MODE: code = "KEYCODE_BUTTON_MODE";
		      }
		      lblOutput.setText("> [" + Integer.toString(keyCode) + ":" + Integer.toString(modifiers) + "]" + code);
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
		frmMonkeyboard.setTitle("MonkeyBoard");
		frmMonkeyboard.setResizable(false);
		frmMonkeyboard.setBounds(100, 100, 438, 443);
		frmMonkeyboard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// moved JList declaration to class-level declarartions
		listView = new JList(listModel);
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
		
		JTextPane textConsole = new JTextPane();
		textConsole.setText(">>>");
		textConsole.setForeground(Color.GREEN);
		textConsole.setBackground(Color.BLACK);
		textConsole.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));

		GroupLayout groupLayout = new GroupLayout(frmMonkeyboard.getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(textConsole)
						.addGroup(groupLayout.createSequentialGroup()
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
								.addGroup(groupLayout.createSequentialGroup()
									.addComponent(btnRefresh)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(btnConnect))
								.addComponent(listView, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(btnMonkeyBoard, GroupLayout.PREFERRED_SIZE, 219, GroupLayout.PREFERRED_SIZE)))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(listView, GroupLayout.PREFERRED_SIZE, 234, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(btnRefresh)
								.addComponent(btnConnect)))
						.addComponent(btnMonkeyBoard))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(textConsole, GroupLayout.PREFERRED_SIZE, 113, GroupLayout.PREFERRED_SIZE)
					.addContainerGap())
		);
		frmMonkeyboard.getContentPane().setLayout(groupLayout);
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.setBorder(null);
		frmMonkeyboard.setJMenuBar(menuBar);
		
		JMenu mnActions = new JMenu("Actions");
		menuBar.add(mnActions);
		
		JMenuItem mntmRefreshDeviceList = new JMenuItem("Refresh Device List");
		mnActions.add(mntmRefreshDeviceList);
		
		JMenuItem mntmConnectToDevice = new JMenuItem("Connect To Device");
		mnActions.add(mntmConnectToDevice);
		
		JMenuItem mntmInstallapk = new JMenuItem("Install *.apk...");
		mnActions.add(mntmInstallapk);
		
		JMenuItem mntmUninstallPackage = new JMenuItem("Uninstall Package...");
		mnActions.add(mntmUninstallPackage);
		
		JMenuItem mntmExecuteShellCommand = new JMenuItem("Execute Shell Command...");
		mnActions.add(mntmExecuteShellCommand);
		
		JMenuItem mntmSaveScreenshot = new JMenuItem("Save Screenshot");
		mnActions.add(mntmSaveScreenshot);
		
		JMenuItem mntmSaveScreenshotAs = new JMenuItem("Save Screenshot As...");
		mnActions.add(mntmSaveScreenshotAs);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mnActions.add(mntmExit);
	}
}
