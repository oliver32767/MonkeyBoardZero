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
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;

import com.android.chimpchat.ChimpChat;
import com.android.chimpchat.core.IChimpDevice;

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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class MonkeyBoard {
	private DefaultListModel listModel = new DefaultListModel();
	
    //private static final String ADB = "/Users/diego/opt/android-sdk/platform-tools/adb";
	private String connectedDeviceId = null; 
    private static final long TIMEOUT = 5000;
    private ChimpChat mChimpchat;
    
	private Map<String, IChimpDevice> mDevices = new HashMap<String, IChimpDevice>();
	
	private JFrame frmMonkeyboard;

	private Map<String, String> getAdbStatus() {
		Map<String, String> rv = new HashMap<String, String>();
		// Capture output from `adb devices` and map connected deviceIds to their status
		// TODO: remove absolute path to adb or make dynamic
		
		String cmd = "/Users/obartley/Library/android-sdk-macosx-r15/platform-tools/adb devices";
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
				
				if ( ! (line.startsWith("List") || line.length() == 0) ) {
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
	
	private void connectToDevice(String deviceId) {
		System.out.println("Connected to: " + deviceId);
		this.connectedDeviceId = deviceId;
		this.refreshDeviceList();
	}
	
	private void refreshDeviceList() {
		Map<String, String> adb = getAdbStatus();
		Iterator<Entry<String, String>> adbDevices = adb.entrySet().iterator();
		
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
			}
			System.out.println("(" + this.connectedDeviceId + ")" + devId + ":" + devStatus);
			elem.put("deviceId", devId);
			elem.put("deviceStatus", devStatus);
			listModel.addElement(elem);
			// TODO: make elements persist between refreshes
//			// check to see if there's already an entry in listModel
//			if ( ! listModel.contains(elem)) {
//				listModel.addElement(elem);
//			}
		}
		
		// now we scrub the list for stuff that isn't in the deviceList anymore
		if ( listModel.getSize() > 0) {
			for (int i = 0; i == listModel.getSize(); i++) {
				if (adb.keySet().contains(listModel.getElementAt(i)))
					listModel.remove(i);
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
	 * Create the application.
	 */
	public MonkeyBoard() {
		initialize();
	
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmMonkeyboard = new JFrame();
		frmMonkeyboard.setTitle("MonkeyBoard");
		frmMonkeyboard.setResizable(false);
		frmMonkeyboard.setBounds(100, 100, 654, 411);
		frmMonkeyboard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JList list = new JList(listModel);
		list.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Available Devices", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		list.setCellRenderer(new DeviceListRenderer());
		JButton btnRefresh = new JButton("Refresh");
		btnRefresh.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				refreshDeviceList();
			}
		});
		
		JButton btnConnect = new JButton("Connect");
		btnConnect.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				connectToDevice("emulator-5556");
			}
		});
		
		JLabel lblMonkeyboard = new JLabel("");
		lblMonkeyboard.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent arg0) {
			}
			@Override
			public void focusLost(FocusEvent e) {
			}
		});
		lblMonkeyboard.setIcon(new ImageIcon("/Users/obartley/Projects/MonkeyBoard/res/android_keyboard.png"));
		GroupLayout groupLayout = new GroupLayout(frmMonkeyboard.getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
						.addComponent(list, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
							.addComponent(btnRefresh)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(btnConnect)))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(lblMonkeyboard, GroupLayout.DEFAULT_SIZE, 520, Short.MAX_VALUE)
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
						.addComponent(lblMonkeyboard, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 567, Short.MAX_VALUE)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(list, GroupLayout.DEFAULT_SIZE, 532, Short.MAX_VALUE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(btnRefresh)
								.addComponent(btnConnect))))
					.addContainerGap())
		);
		frmMonkeyboard.getContentPane().setLayout(groupLayout);
	}
}
