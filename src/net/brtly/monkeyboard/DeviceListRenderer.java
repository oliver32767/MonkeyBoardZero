package net.brtly.monkeyboard;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;

public class DeviceListRenderer extends DefaultListCellRenderer {
	// list of HashMap objects, keys:
	//	deviceId
	//	deviceStatus
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Map<Object, Icon> icons = null;
	
	public DeviceListRenderer() {
		// setup mappings for which icon to use for each value
		icons = new HashMap<Object, Icon>();
		ImageIcon ico = new ImageIcon(this.getClass().getResource("/res/android_device.png")); 
		icons.put("device",ico);
		
		ico = new ImageIcon(this.getClass().getResource("/res/android_connected.png"));
		icons.put("connected",ico);
		
		ico = new ImageIcon(this.getClass().getResource("/res/android_offline.png"));
		icons.put("offline",ico);
	}

	@Override
	public Component getListCellRendererComponent(
		JList list, Object value, int index,
		boolean isSelected, boolean cellHasFocus) {

		// Get the renderer component from parent class
		JLabel label =
			(JLabel) super.getListCellRendererComponent(list,
				value, index, isSelected, cellHasFocus);
		Icon icon = null;
		// Get icon to use for the list item value
		if(value instanceof HashMap<?, ?>) {
			@SuppressWarnings("unchecked")
			HashMap<String, String> v = (HashMap<String, String>)value;
			label.setText(v.get("deviceId"));
			icon = icons.get(v.get("deviceStatus"));
		} else {
			label.setText("unknown");
			icon = icons.get("offline");
		}
		
		// Set icon to display for value
		label.setIcon(icon);
		return label;
	}
}
