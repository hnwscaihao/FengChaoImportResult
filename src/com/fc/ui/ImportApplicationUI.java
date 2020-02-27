package com.fc.ui;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import org.apache.log4j.Logger;

import com.fc.service.ExcelUtil;
import com.fc.service.MyRunnable;
import com.fc.util.APIExceptionUtil;
import com.fc.util.Constants;
import com.fc.util.MKSCommand;
import com.mks.api.response.APIException;
import javax.swing.SwingConstants;

public class ImportApplicationUI extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static JPanel contentPane;
	private JTabbedPane tabbedPane;
	JComboBox<String> comboBox; // 下拉选择导入的类型，导入到那种文档
	private String importType; // 导入的类型
	private JTable tableMapper;
	private JButton backBtn;
	private JButton nextBtn;
	private JButton doneBtn;
	private static JTextArea textArea;
	private JLabel pathText;
	private static MKSCommand cmd;
	private static final Map<String, String> ENVIRONMENTVAR = System.getenv();
	public static final Logger logger = Logger.getLogger(ImportApplicationUI.class.getName());
	private static String defaultUser = "admin"; 
	private static JLabel helloText;
	public String documentTitle = null;// 用来存放文档标题
	static String project =null;
	private File excelFile;
	private JTextField testSuiteField;
	private String testSuiteID;
	private List<Map<String, Object>> data;
	private List<Map<String, Object>> realData ;
	private ExcelUtil excelUtil = new ExcelUtil();
	private static final String EMPTY_IMPORT_TYPE = "Please Select a Type";
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());// 界面风格
					ImportApplicationUI frame = new ImportApplicationUI();
					frame.setVisible(true);
					frame.setLocationRelativeTo(null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * 初始化MKSCommand中的参数
	 */
	public void setMksConfig() {
		try {
			String host = ImportApplicationUI.ENVIRONMENTVAR.get(Constants.MKSSI_HOST);
			if(host==null || host.length()==0) {
				host = "192.168.6.130";
			}
			String portStr = ENVIRONMENTVAR.get(Constants.MKSSI_PORT);
			Integer port = portStr!=null && !"".equals(portStr)? Integer.valueOf(portStr) : 7001;
			defaultUser = ENVIRONMENTVAR.get(Constants.MKSSI_USER);
			String pwd = "";
			if(defaultUser == null || "".equals(defaultUser) ){
				defaultUser = "admin";
				pwd = "admin";
			}
			cmd = new MKSCommand(host, port, defaultUser, pwd, 4, 16);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(ImportApplicationUI.contentPane, "Can not get a connection!", "Message",
					JOptionPane.WARNING_MESSAGE);
			ImportApplicationUI.logger.info("Can not get a connection!");
			System.exit(0);
		}
	}

//	/**
//	 * 获得连接
//	 * 
//	 * @param c
//	 */
//	public boolean getConnect() {
//		try {
//			
//			this.setMksConfig();
//			
//		} catch (Exception e) {
//			JOptionPane.showMessageDialog(this, "Can not get a connection!", "Message", JOptionPane.WARNING_MESSAGE);
//			return false;
//		}
//		return true;
//	}

	/**
	 * Create the frame.
	 * @throws Exception 
	 */
	public ImportApplicationUI() throws Exception {
		
		setTitle("Excel Import");
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 849, 416);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setBounds(8, 8, 834, 322);
		contentPane.add(tabbedPane);

		comboBox = new JComboBox<String>();

		JPanel panel = new JPanel();
		panel.setForeground(Color.RED);
		panel.setToolTipText("Test Suite");
		tabbedPane.addTab(" Info ", null, panel, null);
		panel.setLayout(null);

		pathText = new JLabel("<Path to Excel File *.xls>");
		pathText.setBounds(25, 227, 546, 24);
		pathText.setBorder(BorderFactory.createEtchedBorder());
		panel.add(pathText);

		JButton browseBtn = new JButton("Browse");
		browseBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				logger.info("Start to load excel");
				helloText.setText("Loading Excel File");
				helloText.setForeground(Color.BLACK);
				JFileChooser fc = new JFileChooser();
				fc.setDialogTitle("Select Excel File");
				fc.setAcceptAllFileFilterUsed(true);
				fc.setMultiSelectionEnabled(false);
				int returnVal = fc.showOpenDialog(contentPane);
				if (returnVal == 0) {
					excelFile = fc.getSelectedFile();
					String path = excelFile.getAbsolutePath();
					if (!path.endsWith("xls") && !path.endsWith("xlsx")) {
						logger.error("Selected file is not a excel file!");
						JOptionPane.showMessageDialog(contentPane, "Please Choose Excel File",
								"Please Choose Excel File", JOptionPane.ERROR_MESSAGE);
						helloText.setText("Please Choose Excel File!");
						helloText.setForeground(Color.RED);
					} else {
						String suiteId = path.substring(path.lastIndexOf("-") + 1, path.lastIndexOf("."));
						if (suiteId.matches("[0-9]*")) {
							testSuiteID = suiteId;
							testSuiteField.setText(suiteId);
						}
					}
					try {
						importType = comboBox.getSelectedItem().toString();
						excelUtil.parsFieldMapping(importType);//解析配置文件 02/14
						parseExcel(importType);//解析excel模板
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		browseBtn.setBounds(613, 226, 89, 27);
		panel.add(browseBtn);

		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {

			}
		});
//		comboBox.setSelectedIndex(0);
		comboBox.setBounds(170, 33, 401, 42);
		panel.add(comboBox);

		JLabel lblImportType = new JLabel("Import Type   :");
		lblImportType.setBounds(25, 40, 139, 24);
		panel.add(lblImportType);

		JLabel lblTestSuite = new JLabel("Document ID   :");
		lblTestSuite.setBounds(25, 91, 139, 24);
		panel.add(lblTestSuite);

		testSuiteField = new JTextField();
		testSuiteField.setBounds(170, 90, 401, 27);
		panel.add(testSuiteField);
		testSuiteField.setColumns(10);

		JLabel lblNewLabel = new JLabel("Project       :");
		lblNewLabel.setBounds(25, 131, 139, 24);
		panel.add(lblNewLabel);

		JLabel label = new JLabel("*");
		label.setForeground(Color.RED);
		label.setBackground(Color.RED);
		label.setBounds(599, 47, 54, 15);
		panel.add(label);
		//把红色*注释掉,删除部门下拉框
/*		label_1 = new JLabel("*");
		label_1.setForeground(Color.RED);
		label_1.setBackground(Color.RED);
		label_1.setBounds(154, 47, 10, 15);
		panel.add(label_1);*/
		
		label_2 = new JLabel(
				"( The project must be fill in while importing new document. Format Example: /BMS/Test1 )");
		label_2.setVerticalAlignment(SwingConstants.TOP);
		label_2.setForeground(Color.RED);
		label_2.setBounds(25, 170, 822, 24);
		panel.add(label_2);

		label_3 = new JLabel("[First of all, please choose your own department.]");
		label_3.setForeground(Color.RED);
		label_3.setBounds(25, 0, 480, 21);
		panel.add(label_3);

//		comboBox_1 = new JComboBox();
//		comboBox_1.setBounds(25, 36, 123, 37);
//		panel.add(comboBox_1);


		
		comboBox_2 = new JComboBox<String>();
		
		comboBox_2.setBounds(170, 132, 401, 27);
		panel.add(comboBox_2);
		
//		setFristForPick();//该方法获取部门下拉框的值
		setValueForPick();
		JPanel panel_1 = new JPanel();
		tabbedPane.addTab(" Mapping ", null, panel_1, null);
		panel_1.setLayout(null);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(0, 0, 975, 378);
		panel_1.add(scrollPane);

		tableMapper = new JTable();
		tableMapper.setModel(
				new DefaultTableModel(new Object[][] { new Object[2], new Object[2], new Object[2], new Object[2] },
						new String[] { "Excel Headers", "Integrity Fields" }));
		scrollPane.setViewportView(tableMapper);

		JPanel panel_2 = new JPanel();
		tabbedPane.addTab(" Logger ", null, panel_2, null);
		panel_2.setLayout(null);

		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(-1, -1, 977, 385);
		panel_2.add(scrollPane_1);

		textArea = new JTextArea();
		textArea.setLineWrap(true);
		scrollPane_1.setViewportView(textArea);

		doneBtn = new JButton("Done");
		doneBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		doneBtn.setEnabled(false);
		doneBtn.setBounds(677, 345, 100, 27);
		contentPane.add(doneBtn);
		
		comboBox.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e) {
				importType = comboBox.getSelectedItem().toString();
			}
		});
		
		nextBtn = new JButton("Next");
		nextBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				nextAction(1);
			}
		});
		nextBtn.setBounds(558, 345, 100, 27);
		contentPane.add(nextBtn);

		backBtn = new JButton("back");
		backBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				nextAction(-1);
			}
		});
		backBtn.setEnabled(false);
		backBtn.setBounds(428, 345, 100, 27);
		contentPane.add(backBtn);

		helloText = new JLabel("Hello :)");
		helloText.setBounds(31, 345, 337, 18);
		contentPane.add(helloText);

		
		try {
			setMksConfig();
			
			setProjectList();
			project = comboBox_2.getSelectedItem().toString();
		} catch (Exception e1) {
			JOptionPane.showMessageDialog(this, e1.getMessage());
			e1.printStackTrace();
		}
		for (int i = 0; i < tabbedPane.getMouseListeners().length; i++) {
			tabbedPane.removeMouseListener(tabbedPane.getMouseListeners()[i]);
		}
	}
	public void initMksCommand2() {
		try {
			String host = ImportApplicationUI.ENVIRONMENTVAR.get(Constants.MKSSI_HOST);
			if(host==null || host.length()==0) {
				host = "192.168.6.100";
			}
			cmd = new MKSCommand(host, 7001, "admin", "admin", 4, 16);
//			cmd.getSession();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(ImportApplicationUI.contentPane, "Can not get a connection!", "Message",
					JOptionPane.WARNING_MESSAGE);
			ImportApplicationUI.logger.info("Can not get a connection!");
			System.exit(0);
		}
	}

//	public void setFristForPick() throws Exception {
//		try {
//			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
//					.parse(ExcelUtil.class.getClassLoader().getResourceAsStream( "Dept" + FIELD_CONFIG_FILE));
//			Element root = doc.getDocumentElement();
//			NodeList deptLists = root.getElementsByTagName("depts");
//			for (int i = 0; i < deptLists.getLength(); i++) {
//				Element deptList = (Element) deptLists.item(i);
//				NodeList  depts = deptList.getElementsByTagName("dept");
//				for (int j = 0; j < depts.getLength(); j++) {
//					Element dept = (Element) depts.item(j);
//					String deptName = dept.getAttribute("name");
//					Depts.add(deptName);
//				}
//			}
//			DEPT = Depts.get(0);
//		} catch (Exception e) {
//			JOptionPane.showMessageDialog(this, "Could Not Parse XML Config, Please Contant Adminstrator!");
//		}
////		comboBox_1.setModel(new DefaultComboBoxModel<String>(Depts.toArray(new String[Depts.size()])));
//		setValueForPick();
//		try {
//			excelUtil.parsFieldMapping(DEPT, null);
//		} catch (Exception e1) {
//			e1.printStackTrace();
//		}
//		comboBox_1.addItemListener(new ItemListener() {
//			@Override
//			public void itemStateChanged(ItemEvent e) {
//				DEPT = comboBox_1.getSelectedItem().toString();
//				setValueForPick();
//			}
//		});
//	}

	/**
	 * Description: 解析Excel数据
	 * 
	 * @param importType
	 * @return
	 * @throws Exception
	 */
	public void parseExcel(String importType) throws Exception {
		if (importType == null || importType.equals("")) {
			throw new Exception("importType is null");
		}

		pathText.setText(excelFile.getAbsolutePath());
		logger.info("Load excel : " + excelFile.getAbsolutePath());
		try {
			data = excelUtil.parseExcel(excelFile, importType);
			helloText.setText("Success to parse excel!");
			helloText.setForeground(Color.BLACK);
			logger.info("Success to parse excel: " + data);
		} catch (Exception e1) {
			logger.error("Failed to parse excel!");
			JOptionPane.showMessageDialog(contentPane, e1.getMessage(), "Failed to parse excel",
					JOptionPane.ERROR_MESSAGE);
			helloText.setText("Failed to parse excel!");
			helloText.setForeground(Color.RED);
		}
		
	}

	public void nextAction(int plus) {
		//initMksCommand2();
//		try {
//			setProjectList();
//		} catch (APIException e2) {
//			// TODO Auto-generated catch block
//			e2.printStackTrace();
//		}
		int curIdx = tabbedPane.getSelectedIndex();
		int maxIdx = tabbedPane.getComponentCount() - 1;
		int newIdx = curIdx + plus;
		boolean pass = true;
		if (newIdx == 1) {
			
			// 进入选择界面，需要判断MKS是否输入
			logger.info("==> into Mapper panel");
			// 检查excel是否解析成功
			if (excelFile == null) {
				JOptionPane.showConfirmDialog(contentPane, "Please select a excel!");
				return;
			}

			// 判断是否选择模板类型
//			importType = comboBox.getSelectedItem().toString(); // 在进入类型是否存在判断时先拿到最新的类型
			if (EMPTY_IMPORT_TYPE.equals(importType)) {
				JOptionPane.showConfirmDialog(contentPane, "Please choose the template type!");
				return;
			}
			// 解析Excel
			testSuiteID = testSuiteField.getText();
			/*
			 * if(testSuiteID == null || "".equals(testSuiteID)) {
			 * JOptionPane.showConfirmDialog(contentPane,
			 * "Test Suite ID can't Be Empty!"); return; }
			 */
			
			if (testSuiteID == null || "".equals(testSuiteID)) {
				if ( documentTitle == null || "".equals(documentTitle)) {
					documentTitle = JOptionPane.showInputDialog(
							"Document ID Is Empty, So Please Enter [ Document Short Title ] to Create It!", documentTitle);
					if (documentTitle == null || documentTitle.equals("")) {
						JOptionPane.showInputDialog(
								"Document ID and  [ Document Short Title ] Counld Not Be Empty Simultaneously");
					}		
				}
				project=comboBox_2.getSelectedItem().toString();
				if (project == "Please select a Project" || "Please select a Project".equals(project)) {
					JOptionPane.showMessageDialog(this, "Please select a Project!");
					comboBox_2.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent arg0) {
							project=comboBox_2.getSelectedItem().toString();
						}
					});
					return;
				} else {
					boolean projectHas = false;
					try {
						projectHas = cmd.checkProject(project);
					} catch (APIException e) {
						logger.info(e.getMessage());
					}
					if (!projectHas) {
						JOptionPane.showMessageDialog(this, "Project is not exist, Please Re-Input It!");
						return;
					}
				}
			} else {
				try {
					if (!cmd.docIDIsRight( testSuiteID, ExcelUtil.DOC_TYPE_MAP.get(importType)) ) {// 此处要修改，  判断类型
						JOptionPane.showConfirmDialog(contentPane,
								"Your input Test Suite ID is not correctly, Please Re-Input It!");
						return;
					}
				} catch (Exception e1) {
					JOptionPane.showConfirmDialog(contentPane,
							"Your input Test Suite ID is not correctly, Please Re-Input It!");
					return;
				}
			}

			if (data == null || data.size() == 0) {
				JOptionPane.showConfirmDialog(contentPane, "Counld not prase excel! Please check the excel format!");
				return;
			} 
			// setFocus(newIdx);
			try {
//				String deptment = comboBox_1.getSelectedItem().toString();
				String realImportType = new String(importType);
				excelUtil.parsFieldMapping(realImportType);
				tableMapper.setModel(new DefaultTableModel(excelUtil.tableFields,
						new String[] { "Excel Headers", "Integrity Fields" }));
				Map<String,String> errorRecord = new HashMap<String,String>();



				realData = excelUtil.checkExcelData(data, errorRecord, realImportType, cmd);
				String checkMessage = errorRecord.get("error");
				if(checkMessage != null && !"".equals(checkMessage)){
					JOptionPane.showMessageDialog(this, checkMessage);
					return;
				}
//				else {
//					// 处理校验完数据    创建  token 
//					TOKEN = UUID.randomUUID().toString().replaceAll("-", "");
//				}
			} catch (APIException e) {
				APIExceptionUtil.getMsg(e);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		if (newIdx == 2) {
			// 进入Logger界面
			logger.info("==> into logger panel");
			// 开始线程导入数据
			r.cmd = cmd;
			r.data = realData;
			r.testSuiteId = testSuiteID;
			r.importType = importType;
			r.excelUtil = excelUtil;
			r.project = comboBox_2.getSelectedItem().toString();
			r.shortTitle = documentTitle;
			t = new Thread(r);
			t.start();// t查询线程,开启
			configTimeArea(j);
		}
		if (pass) {
			if (newIdx > 0) {
				backBtn.setEnabled(true);
			} else {
				backBtn.setEnabled(false);
			}
			if (newIdx < maxIdx) {
				nextBtn.setEnabled(true);
				doneBtn.setEnabled(false);
			}
			if (newIdx == maxIdx) {
				nextBtn.setEnabled(false);
			}
			if (newIdx <= maxIdx) {
				setFocus(newIdx);
			}
		}
	}

	/**
	 * 选择tab
	 * 
	 * @param idx
	 */
	private void setFocus(final int idx) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				tabbedPane.setSelectedIndex(idx);
			}
		});
	}

	/**
	 * 选择tab
	 * 
	 * @param
	 */
	public static void showLogger(final String logger) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				textArea.append(logger + "\n");
			}
		});
	}

	/**
	 * 选择tab
	 * 
	 * @param
	 */
	public static void showProgress(final int sheetNum, final int totalSheetCount, final int caseNum,
			final int totalCaseCount) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				helloText.setText("Task : " + sheetNum + "/" + totalSheetCount + ", Progress : " + caseNum + "/"
						+ totalCaseCount);
			}
		});
	}

	private int ONE_SECOND = 1000;
	private MyRunnable r = new MyRunnable();
	private Thread t = new Thread();// 查询线程
	private JLabelTimerTask j = new JLabelTimerTask();
	private JLabel label_2;
	private JLabel label_3;
	private static JComboBox<String> comboBox_2;

	/**
	 * 这个方法创建 a timer task 每秒更新一次 the time
	 */
	private void configTimeArea(JLabelTimerTask j) {
		Timer tmr = new Timer();
		tmr.scheduleAtFixedRate(j, new Date(), ONE_SECOND);
	}

	/**
	 * Timer task 更新时间显示区
	 * 
	 */
	protected class JLabelTimerTask extends TimerTask {
		@Override
		public void run() {
			if (!t.isAlive()) {
				doneBtn.setEnabled(true);
			} else {
				doneBtn.setEnabled(false);
			}
		}
	}

	/**
	 * 解析配置文件为Pick设置数据
	 */
	public void setValueForPick() {
		List<String> result = new ArrayList<>();
		result.add(new String(EMPTY_IMPORT_TYPE));
		try {
			result.addAll(excelUtil.parsFieldMapping(null)); // 获得解析后的集合。
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Could Not Parse XML Config, Please Contant Adminstrator!");
		}
		comboBox.setModel(new DefaultComboBoxModel<String>(result.toArray(new String[result.size()])));

	}
	public static void setProjectList() throws APIException{
		List<String> projects = cmd.getProjects(defaultUser);
		projects.add(0, "Please select a Project");
		comboBox_2.setModel(new DefaultComboBoxModel<String>(projects.toArray(new String[projects.size()])));
	}
}
