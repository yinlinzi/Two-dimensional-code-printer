package com.inspiry.demo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.json.JSONObject;

import com.inspiry.api.InspiryDeviceAPIFor532;

public class IndexFrame extends JFrame implements WindowListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Dimension screenSize = Toolkit.getDefaultToolkit()
			.getScreenSize();

	private JButton delDataButton;
	private JTextArea dataTextArea;
	private int startDeviceState;
	private Thread backgroundThread;
	private String macCode;
	private String k;
	private boolean isRunning = true;
	private int msgCount = 0;

	public IndexFrame() {
		super();
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
			}
		});
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("二维码扫描仪");
		setAlwaysOnTop(true);
		setBounds(screenSize.width / 2 - 200, screenSize.height / 2 - 320, 400,
				640);
		setResizable(false);
		addWindowListener(this);
		addComponent();
	}

	private void addComponent() {
		getContentPane().setLayout(
				new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getContentPane().add(getDelDataButton());
		getContentPane().add(getDataTextArea());
	}

	private JButton getDelDataButton() {
		if (delDataButton == null) {
			delDataButton = new JButton();
			delDataButton.setAlignmentX(Component.CENTER_ALIGNMENT);
			delDataButton.setText("清除");
			delDataButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					dataTextArea.setText("");
				}
			});
		}
		return delDataButton;
	}

	private JTextArea getDataTextArea() {
		if (dataTextArea == null) {
			dataTextArea = new JTextArea();
			dataTextArea.setText("");
		}
		return dataTextArea;
	}

	@Override
	public void windowClosing(WindowEvent e) {
		isRunning = false;
		try {
			if (backgroundThread != null && backgroundThread.isAlive()) {
				backgroundThread.join();
				Inspiry532Utils.ReleaseDevice();
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public void getMacCode() throws Exception {
		File f = new File("config.ini");
		InputStreamReader ir = new InputStreamReader(new FileInputStream(f),
				"UTF-8");
		BufferedReader br = new BufferedReader(ir);
		String line1 = br.readLine();
		String line2 = br.readLine();
		br.close();
		if (line1 == null || line2 == null) {
			throw new Exception("配置文件格式错误。");
		}
		macCode = line1;
		k = line2;
	}

	private void autoScan() {
		try {
			getMacCode();
		} catch (Exception e1) {
			e1.printStackTrace();
			addText("获取设备签名失败\n");
			return;
		}
		int resultCode = Inspiry532Utils.initInspiry532();
		if (resultCode == 1) {
			addText("设备初始化成功，请扫码。\n");
			System.out.println("Start thread.");
			while (isRunning) {
				Inspiry532Utils.start();
				String decodeString = Inspiry532Utils.getDecodeString();
				if (decodeString != null && decodeString.length() > 0) {
					Inspiry532Utils.stop();
					boolean success = true;
					try {
						String response = HttpUtil.send(decodeString, macCode,
								k);
						JSONObject obj = new JSONObject(response);
						String responseMsg = obj.getString("msg");
						if (!responseMsg.equals("success")) {
							String msg = String
									.format("扫码错误:%s\n", responseMsg);
							addText(msg);
							success = false;
						}
					} catch (Exception e) {
						e.printStackTrace();
						String msg = String.format("网络错误:%s\n", e.toString());
						addText(msg);
						success = false;
					}
					if (!success) {
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						continue;
					}
					// 扫码成功!
					String msg = String.format("扫码成功:%s\n", decodeString);
					addText(msg);
					try {
						Thread.sleep(1200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			addText("请插入设备再启动程序。\n");
		}
	}

	private void startMainWorkThread() {
		backgroundThread = new Thread(new Runnable() {
			@Override
			public void run() {
				autoScan();
			}
		});
		backgroundThread.start();
	}

	/**
	 * 窗体打开后，获取设备并启动设备
	 * 
	 */
	@Override
	public void windowOpened(WindowEvent e) {
		addText("正在初始化，请稍等。\n");
		startMainWorkThread();
	}

	private void addText(String msg) {
		if (msgCount++ > 20) {
			dataTextArea.setText("");
		}
		Date date = new Date();
		DateFormat format = new SimpleDateFormat("HH:mm:ss");
		String time = format.format(date);
		dataTextArea.setText(dataTextArea.getText() + time + ": " + msg);
	}

	/**
	 * 根据设备启动状态弹出提示信息
	 * 
	 * @param startDeviceState
	 */
	@SuppressWarnings("unused")
	private void showMessageDialogWithStartDeviceState() {
		switch (this.startDeviceState) {
		case 1:
			System.out.println("开启设备成功");
			break;
		case -1:
			JOptionPane.showMessageDialog(this, "设备已被启动");
			break;
		case -2:
			JOptionPane.showMessageDialog(this, "没有找到设备");
			break;
		case -3:
			JOptionPane.showMessageDialog(this, "设备初始化失败");
			break;

		default:
			break;
		}
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}

}
