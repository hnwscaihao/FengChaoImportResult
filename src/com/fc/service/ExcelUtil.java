package com.fc.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fc.ui.ImportApplicationUI;
import com.fc.util.ExceptionUtil;
import com.fc.util.MKSCommand;
import com.mks.api.response.APIException;

public class ExcelUtil {

	private static List<String> caseFields = new ArrayList<>();
	private static List<String> stepFields = new ArrayList<>();
	private static List<String> resultFields = new ArrayList<>();// Test
																	// Result不做导入处理
	private static Map<String, List<String>> importHeadersMap = new HashMap<>();// 根据导入模板保存
																				// field
	public String[][] tableFields = null;
	public static Map<String, String> hasParentField = new HashMap<String, String>();
	// private static Map<String, Map<String, String>> headerConfig = new
	// HashMap<>();
	private static Map<String, Map<String, Map<String, String>>> headerConfigs = new HashMap<>();
	private static final String FIELD_CONFIG_FILE = "FieldMapping.xml";
	private static final String CATEGORY_CONFIG_FILE = "Category.xml";
	private static final String TEST_STEP = "Test Step";
	private static final String TEST_RESULT = "Test Result";
	private static final String PARENT_FIELD = "parentField";
	private static final String NEED_FIELD_SET = "needFieldSet";
	private static final String CHAPTER_FIELD = "ChapterField";
	private static final String TEST_CASE_ID = "Test Case ID";
	private static final String TEST_CASE_ID2 = "ID";
	private static final String TEST_STEP_CALL_DEPTH = "Call Depth";
	private static final String TEST_STEP_INPUT = "Input";
	private static final String TEST_STEP_OUTPUT = "Output";
	private static final String TEST_STEP_TEST_PRO = "Test Procedure";
	private static final String TEST_RESULT_CYCLE = "Cycle Test";
	private static final String TEST_CASE_TEXT = "Test Case Description";
	private static final String TEST_CASE_TEXT2 = "Text(Description)";
	private static final String TEST_CASE_TEXT3 = "Test Case";
	// private static final String STEP_ORDER = "Step Order";
	private static final String SOFTWARE_UNIT_TEST = "Software Unit Test Specification";
	private static final String SOFTWARE_INTEGRATION_TEST = "Software Integration Test Specification";
	private static final String SOFTWARE_QUALI_TEST = "Software Qualification Test Specification";
	private static final String SYSTEM_INTEGRATION_TEST = "System Integration Test Specification";
	private static final String SYSTEM_QUALI_TEST = "System Qualification Test Specification";
	private static final String DCT_TEST = "DCT Test Specification";

	// private static final List<String> allCategories = new
	// ArrayList<String>();

	private static final List<String> CURRENT_CATEGORIES = new ArrayList<String>();// 记录导入对象的正确Category

	private static final Map<String, List<String>> PICK_FIELD_RECORD = new HashMap<String, List<String>>();

	private static final Map<String, String> FIELD_TYPE_RECORD = new HashMap<String, String>();
	
	public static final List<String> RICH_FIELDS = new ArrayList<String>();

	private static String CONTENT_TYPE;

	public static final Map<String, String> DOC_TYPE_MAP = new HashMap<String, String>();

	private Map<String, CellRangeAddress> cellRangeMap = new HashMap<String, CellRangeAddress>();

	private static final List<String> USER_FULLNAME_RECORD = new ArrayList<String>();
	private static boolean IS_USER = false;
	private static boolean RELATIONSHIP_MISTAKEN = false;
	public static final Logger logger = Logger.getLogger(ExcelUtil.class);
	private boolean parentStructure = false;// 是否有父子级结构

	private static final String TEST_SESSION = "Test Session";
	private static final String SESSIONN_STATE = "In Testing";
	private static final String ExpectedResults = "Expected Results";

	private static final String SESSION_ID = "Session ID";
	private static final String VERDICT = "Verdict";
	private static final String OBSERVED_RESULT = "Observed Result";
	private static final String ANNOTATION = "Annotation";
	private static final String SERVERITY = "Serverity";
	private static final String REPRODUCIBILITY = "Reproducibility";
	private static final String SW_VERSION = "SW Version";
	private static final String HW_VERSION = "HW Version";


	/**
	 * 利用Jsoup解析配置文件，得到相应的参数，为Type选项和创建Document提供信息 (1)
	 * Document:Type,Project,State,Shared Category (2) Content:Type 负责人：汪巍
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<String> parsFieldMapping(String selectImportType) throws Exception {

		ExcelUtil.logger.info("start to parse xml : " + FIELD_CONFIG_FILE);
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(ExcelUtil.class.getClassLoader().getResourceAsStream(FIELD_CONFIG_FILE));
		Element root = doc.getDocumentElement();
		List<String> typeList = new ArrayList<String>();
		if (root == null)
			return typeList;
		// 得到xml配置
		NodeList importTypes = root.getElementsByTagName("importType"); // 拿到mapping里面所有的
																		// ImportType
		if (importTypes == null || importTypes.getLength() == 0) {
			throw new Exception("Can't not parse xml because of don't has \"importType\"");
		} else {
			// 循环 刚才拿到的所有ImportType
			for (int j = 0; j < importTypes.getLength(); j++) {
				Element importType = (Element) importTypes.item(j);
				// 获取XML 文件的name 和 Type
				String typeName = importType.getAttribute("name");
				String documentType = importType.getAttribute("type");
				DOC_TYPE_MAP.put(typeName, documentType);
				typeList.add(typeName);
				if (selectImportType != null && !"".equals(selectImportType) && typeName.equals(selectImportType)) {
					String structureStr = importType.getAttribute("structure");
					if (structureStr != null && !"".equals(structureStr)) {
						parentStructure = Boolean.valueOf(structureStr);
					}
					NodeList excelFields = importType.getElementsByTagName("excelField");
					Map<String, Map<String, String>> headerConfig = new HashMap<>();
					headerConfigs.put(typeName, headerConfig);
					List<String> testStepFields = new ArrayList<>();
					importHeadersMap.put(typeName + "-stepFields", testStepFields);
					try {
						if (excelFields == null || excelFields.getLength() == 0) {
							throw new Exception("Can't not parse xml because of don't has \"excelField\"");
						} else {
							tableFields = new String[excelFields.getLength()][2];
							for (int i = 0; i < excelFields.getLength(); i++) {
								Element fields = (Element) excelFields.item(i);
								String name = fields.getAttribute("name");
								Map<String, String> map = new HashMap<>();
								String type = fields.getAttribute("type");
								map.put("type", type);
								if (TEST_STEP.equals(type) && !stepFields.contains(name)) {
									stepFields.add(name);
								} else if (TEST_RESULT.equals(type) && !resultFields.contains(name)) {
									resultFields.add(name);
									// testResulFields.add(name);
								} else if (!TEST_STEP.equals(type) && !TEST_RESULT.equals(type)
										&& !caseFields.contains(name)) {
									caseFields.add(name);
									CONTENT_TYPE = type;
								}
								if (TEST_STEP.equals(type) && !testStepFields.contains(name)) {
									testStepFields.add(name);
								}
								String field = fields.getAttribute("field");
								map.put("field", field);
								// 获取 excelField 的 onlyCreate 属性 ， 若没有填写则默认为
								// false
								String onlyCreate = fields.getAttribute("onlyCreate");
								if (onlyCreate == null || onlyCreate.equals("")) {
									map.put("onlyCreate", "false");
								} else {
									map.put("onlyCreate", onlyCreate);
								}
								String overRide = fields.getAttribute("overRide");
								if (overRide == null || overRide.equals("")) {
									map.put("overRide", "true");
								} else {
									map.put("overRide", overRide);
								}
								tableFields[i][0] = name;
								tableFields[i][1] = field;
								if (fields.hasAttribute(PARENT_FIELD)) {
									String parentField = fields.getAttribute(PARENT_FIELD);
									map.put(PARENT_FIELD, parentField);
									tableFields[i][1] = parentField;
									hasParentField.put(field, parentField);
								}
								if (fields.hasAttribute(NEED_FIELD_SET)) {
									String needField = fields.getAttribute(NEED_FIELD_SET);
									map.put(NEED_FIELD_SET, needField);
								}
								if (fields.hasAttribute(CHAPTER_FIELD)) {
									String needField = fields.getAttribute(CHAPTER_FIELD);
									map.put(CHAPTER_FIELD, needField);
								}
								headerConfig.put(name, map);
							}
						}
					} catch (ParserConfigurationException e) {
						logger.error("parse config file exception", e);
					} catch (SAXException e) {
						logger.error("get config file exception", e);
					} catch (IOException e) {
						logger.error("io exception", e);
					} finally {
						logger.info("get info : \nheaderConfig : " + headerConfig);
					}
				}
			}
		}
		return typeList;
	}

	/**
	 * Description 查询当前要导入类型的 正确Category
	 * 
	 * @param documentType
	 * @throws Exception
	 */
	public void parseCurrentCategories(String documentType) throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(ExcelUtil.class.getClassLoader().getResourceAsStream(CATEGORY_CONFIG_FILE));
		Element root = doc.getDocumentElement();
		// 得到xml配置
		NodeList importTypes = root.getElementsByTagName("documentType");
		for (int j = 0; j < importTypes.getLength(); j++) {
			Element importType = (Element) importTypes.item(j);
			String typeName = importType.getAttribute("name");
			if (typeName.equals(documentType)) {
				NodeList categoryNodes = importType.getElementsByTagName("category");
				for (int i = 0; i < categoryNodes.getLength(); i++) {
					Element categoryNode = (Element) categoryNodes.item(i);
					CURRENT_CATEGORIES.add(categoryNode.getAttribute("name"));
				}
			}
		}
	}

	/**
	 * 获得Excel中的数据
	 * 
	 * @param
	 * @return
	 * @throws
	 * @throws IOException
	 */
	public List<Map<String, Object>> parseExcel(File file, String importType) throws Exception {
		// List<List<Map<String, Object>>> data = new ArrayList<>();
		Workbook wb = null;
		String fileName = file.getName();
		if (fileName.endsWith(".xlsx")) {
			wb = new XSSFWorkbook(file);
		} else if (fileName.endsWith(".xls")) {
			wb = new HSSFWorkbook(new FileInputStream(file));
		}
		List<Map<String, Object>> list = new ArrayList<>();
		Sheet sheet = wb.getSheetAt(0);
		int rowNum = this.getRealRowNum(sheet, importType);
		int colNum = this.getRealColNum(sheet);
		int row = 1;
		Row firstRow = sheet.getRow(0);
		Row secondRow = sheet.getRow(1);
		boolean needSecond = false;
		// if(SOFTWARE_UNIT_TEST.equals(importType) ||
		// SOFTWARE_INTEGRATION_TEST.equals(importType) ||
		// SOFTWARE_QUALI_TEST.equals(importType)
		// || SYSTEM_INTEGRATION_TEST.equals(importType) ||
		// SYSTEM_QUALI_TEST.equals(importType)) {
		// row = 2;
		// needSecond = true;
		// }
		int merge = getMergeRow(sheet);
		if (merge > 0) {
			row = row + merge;
			needSecond = true;
		}
		int endRow = row + rowNum;
		for (; row < endRow; row++) {
			Map<String, Object> map = new HashMap<>();
			Map<String, String> stepMap = null;
			Map<String,String> resultMap = null;
			List<Map<String, String>> stepList = new ArrayList<Map<String, String>>();
			List<Map<String,String>> resultList = new ArrayList<Map<String,String>>();
			boolean stepField = false;
			// case可关联多个Test Step信息，所以
			Row dataRow = sheet.getRow(row);
			int stepFieldStart = 0;
			int stepFieldEnd = 0;
			for (int col = 0; col < colNum; col++) {
				Cell fieldCell = firstRow.getCell(col);
				Cell secondCell = secondRow.getCell(col);
				String secondFieldVal = getCellVal(secondCell);//获取列头的值(第2行)
				Cell valueCell = dataRow.getCell(col);
				String valueVal = getCellVal(valueCell);//获取列的值(数据行)
				if (fieldCell == null && needSecond) {
					fieldCell = secondRow.getCell(col);
				}
				String field = getCellVal(fieldCell);
				if (stepField && stepFieldStart <= col && col <= stepFieldEnd) {
//					stepField = true;
					fieldCell = secondRow.getCell(col);
					field = getCellVal(fieldCell);//获取列头的值(第2行)
				}else if ("".equals(secondFieldVal) || "-".equals(secondFieldVal)){//如果第二行为空，就是Test Case数据  // 02/12
					Object value = map.get(field);
					if(value == null || "".equals(value)){
						map.put(field, valueVal);
					}else if (ExpectedResults.equals(field)){//如果是Expected Results，合并值
						String valueStr = (String) value;
						valueStr = valueStr + "\n" + valueVal;
					}
				}else if(secondFieldVal != null && resultFields.contains(secondFieldVal)){//循环处理Test Result  //02/12
					if(valueVal != null && !"".equals(valueVal)){
						if(SESSION_ID.equals(secondFieldVal)){
							resultMap = new HashMap<String,String>();
							resultList.add(resultMap);
						}
						resultMap.put(secondFieldVal, valueVal);
					}
				}else if (field.contains(TEST_RESULT_CYCLE)) {
					int fieldCol = fieldCell.getColumnIndex();
					int fieldRow = fieldCell.getRowIndex();
					CellRangeAddress cellRange = cellRangeMap.get(fieldRow + "-" + fieldCol);
					if (cellRange != null) {
						col = col + cellRange.getLastColumn() - cellRange.getFirstColumn();
					}
					continue;
				} else if (field.contains(TEST_STEP_CALL_DEPTH) || field.equals(TEST_STEP_INPUT)
						|| field.equals(TEST_STEP_OUTPUT) || field.equals(TEST_STEP_TEST_PRO)
						|| field.equals(TEST_STEP)) {// step字段直接处理 因 step会单行循环
					stepMap = new HashMap<String, String>();
					if (field.contains(TEST_STEP_CALL_DEPTH)) {
						field = "Call Depth";
					}
					if (field.equals(TEST_STEP_INPUT)) {
						field = "Test Input";
					}
					if (field.equals(TEST_STEP_OUTPUT)) {
						field = "Test Output";
					}
					if (field.equals(TEST_STEP_TEST_PRO)) {
						field = "Test Procedure";
					}
					if (!field.equals(TEST_STEP)) {
						stepMap.put("ParentField", field);
					}
					int fieldCol = fieldCell.getColumnIndex();
					int fieldRow = fieldCell.getRowIndex();
					CellRangeAddress cellRange = cellRangeMap.get(fieldRow + "-" + fieldCol);
					fieldCell = secondRow.getCell(col);
					stepFieldStart = cellRange.getFirstColumn();//Call Depth单元格的起始列号
					stepFieldEnd = cellRange.getLastColumn();//Call Depth单元格的结束列号
					field = getCellVal(fieldCell);
					stepList.add(stepMap);
					stepField = true;//第一次赋true值的位置
				} else {
					stepField = false;
				}

				String value = valueCell != null ? getCellVal(valueCell) : "";
				if (stepField && stepFields.contains(field)) {// Test Step 字段  把当初的||改成了现在的&& 02/15
					stepMap.put(field, value);
				}
				//02/15 存值冗余
				/*else {// Test Case字段放入Map
					map.put(field, value);
				}*/

			}
			if (!stepList.isEmpty()) {
				map.put(TEST_STEP, stepList);
			}
			if (!resultList.isEmpty()){
				map.put(TEST_RESULT, resultList);
			}
			System.out.println("Test Case No." + map.get("Test Case No."));
			list.add(map);
		}
		// data.add(list);
		return list;
	}

	@SuppressWarnings("deprecation")
	public String getCellVal(Cell cell) {
		String value = "";
		switch (cell.getCellType()) {
		case Cell.CELL_TYPE_STRING:
			value = cell.getStringCellValue();
			break;
		case Cell.CELL_TYPE_BLANK:
			break;
		case Cell.CELL_TYPE_FORMULA:
			value = String.valueOf(cell.getCellFormula());
			break;
		case Cell.CELL_TYPE_NUMERIC:
			value = String.valueOf(Math.round(cell.getNumericCellValue()));// 当前项目
																			// 没有Number类型，只有String。取整
			break;
		case Cell.CELL_TYPE_BOOLEAN:
			value = String.valueOf(cell.getBooleanCellValue());
			break;
		}
		return value;
	}

	/**
	 * 处理Excel中的数据，将Test Step信息和Test Case信息拆分开
	 * 
	 * @param data
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> dealExcelData(List<Map<String, Object>> data, String importType) {
		List<Map<String, Object>> newData = new ArrayList<>();
		Map<String, Object> newMap = null;
		Map<String, Map<String, String>> headerConfig = headerConfigs.get(importType);
		for (int i = 0; i < data.size(); i++) {
			Map<String, Object> rowMap = data.get(i);
			String caseID = (String) rowMap.get(TEST_CASE_ID);
			newMap = new HashMap<String, Object>();
			if (caseID != null && !"".equals(caseID)) {
				newMap.put("ID", caseID);
			}
			for (String header : caseFields) {
				Map<String, String> fieldConfig = headerConfig.get(header);
				if (fieldConfig != null) {
					String field = fieldConfig.get("field");
					String value = (String) rowMap.get(header);
					if (!"-".equals(field) && value != null && !"".equals(value)) {
						newMap.put(field, value);
					}
				}
			}
			if (rowMap.containsKey(TEST_STEP)) {// Test Case包含有 Test Step信息
				Object steps = rowMap.get(TEST_STEP);
				if (steps instanceof List) {
					List<String> importStepFields = importHeadersMap.get(importType + "-stepFields");
					List<Map<String, String>> currentSteps = (List<Map<String, String>>) steps;
					if (!currentSteps.isEmpty()) {// Test Case包含有 Test Step信息
						List<Map<String, String>> stepList = new ArrayList<Map<String, String>>();
						Map<String, Map<String, String>> mapRecord = new HashMap<String, Map<String, String>>();
						Map<String, String> stepMap = null;
						for (Map<String, String> map : currentSteps) {// 循环处理Test
																		// Step信息
							String acturalField = null;
							boolean existMap = false;
							String parentField = map.get("ParentField");
							map.remove("ParenetField");
							StringBuffer acturalFieldVal = null;
							boolean hasVal = false;
							for (String header : importStepFields) {
								if (map.containsKey(header)) {
									Map<String, String> fieldConfig = headerConfig.get(header);
									if (fieldConfig != null) {
										String field = fieldConfig.get("field");
										String value = (String) map.get(header);
										boolean needFieldSet = true;
										if (fieldConfig.containsKey(NEED_FIELD_SET)) {
											needFieldSet = Boolean.valueOf(fieldConfig.get(NEED_FIELD_SET));
										}
										if ("ID".equals(field)) {
											if (value != null && !"".equals(value)) {
												if (mapRecord.get(value) != null) {
													stepMap = mapRecord.get(value);
													existMap = true;
												} else {
													stepMap = new HashMap<String, String>();
													mapRecord.put(value, stepMap);
												}
											} else {
												Map<String, String> tempMap = mapRecord.get(value);
												if (tempMap != null) {
													if ((parentField != null && !"".equals(parentField)
															&& !tempMap.containsKey(parentField))
															|| (parentField == null || !"".equals(parentField)
															&& !tempMap.containsKey(field))) {
														stepMap = tempMap;
														existMap = true;
													}
												} else {
													stepMap = new HashMap<String, String>();
													mapRecord.put(value, stepMap);
												}
											}
										}
										if (fieldConfig.containsKey(PARENT_FIELD)) {
											if (acturalField == null) {
												acturalField = fieldConfig.get(PARENT_FIELD);
												if (!acturalField.equals(parentField)) {
													acturalField = parentField;
												}
												acturalFieldVal = new StringBuffer();
											}
											if (needFieldSet)
												acturalFieldVal.append(field).append(": ");
											acturalFieldVal.append(value);
											if (!value.endsWith("\n")) // 如果拼接字符串不是以\n结尾，拼接
												acturalFieldVal.append("\n");
										} else {
											if (value != null && !"".equals(value)) {
												stepMap.put(field, value);// 存放非拼接字段
											}
										}
										if (value != null && !"".equals(value))
											hasVal = true;
									}
								}
								// for (String header : stepFields) {
								// Map<String, String> fieldConfig =
								// headerConfig.get(header);
								//
								// }
							}
							if (acturalFieldVal != null) {// 实际存放Step值。数据不为空
								stepMap.put(acturalField, acturalFieldVal.toString());
							}
							if (!existMap && hasVal)
								stepList.add(stepMap);
						}
						newMap.put(TEST_STEP, stepList);
					}
				} else if (steps instanceof String) {
					String stepIds = (String) steps;
					if (stepIds != null && !"".equals(stepIds)) {
						stepIds = stepIds.replaceAll(";", ",").replaceAll("，", ",");
						newMap.put("Test Steps", stepIds);
					}
				}
			}else if (rowMap.containsKey(TEST_RESULT)){//Test Case包含有 Test Result信息  // 02/12
				Object steps = rowMap.get(TEST_RESULT);
				if(steps instanceof List){
					List<Map<String, String>> currentResults = (List<Map<String, String>> )steps;
					if(!currentResults.isEmpty()) {//Test Case包含有 Test Result信息
						List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
						Map<String,String> resultMap = null;
						boolean hasVal = false;
						for(Map<String, String> map : currentResults) {//循环处理Test Result信息
							hasVal = false;
							resultMap = new HashMap<String,String>();
							for(String header : resultFields) {
								if(map.containsKey(header)){
									Map<String, String> fieldConfig = headerConfig.get(header);
									if(fieldConfig != null) {
										String field = fieldConfig.get("field");
										String value = (String)map.get(header);
										if(value != null && !"".equals(value)) {
											resultMap.put(field, value);//存放非拼接字段
											hasVal = true;
										}
									}
								}
							}
							if(hasVal)
								resultList.add(resultMap);
						}
						newMap.put(TEST_RESULT, resultList);
					}
				}
			}
			newData.add(newMap);
		}
		return newData;
	}

	/**
	 * 获得真正的row数：<br/>
	 * <li>根据Test Case ID，整行数据确定真正的行数</li>
	 * 
	 * @param sheet
	 * @param
	 * @return
	 */
	public int getRealRowNum(Sheet sheet, String importType) throws Exception {
		int realRow = 0;
		int caseIDCol = 0;
		int textCol = 0;
		Row row1 = sheet.getRow(0);
		for (int i = 0; i < row1.getLastCellNum(); i++) {
			Cell cell = row1.getCell(i);

			if (cell != null) {
				String value = cell.getStringCellValue();
				if (TEST_CASE_ID.equalsIgnoreCase(value) || TEST_CASE_ID2.equalsIgnoreCase(value)) {
					caseIDCol = i;
				}
				if (TEST_CASE_TEXT.equalsIgnoreCase(value) || TEST_CASE_TEXT2.equalsIgnoreCase(value)
						|| TEST_CASE_TEXT3.equalsIgnoreCase(value) || "Text".equalsIgnoreCase(value)) {
					textCol = i;
				}
			}
		}
		int i = 1;
		// 这个几个模板 列名在第一行、第二行
		// if(SOFTWARE_UNIT_TEST.equals(importType) ||
		// SOFTWARE_INTEGRATION_TEST.equals(importType) ||
		// SOFTWARE_QUALI_TEST.equals(importType)
		// || SYSTEM_INTEGRATION_TEST.equals(importType) ||
		// SYSTEM_QUALI_TEST.equals(importType)) {
		// i = 2;
		// }
		int merge = getMergeRow(sheet);
		i = i + merge;// 如果有合并单元格，加上
		int titleCount = 1 + merge;
		for (; i <= sheet.getLastRowNum(); i++) {
			Row currentRow = sheet.getRow(i);
			Cell fieldCaseCell = currentRow != null ? currentRow.getCell(caseIDCol) : null;
			String caseIDValue = fieldCaseCell != null ? getCellVal(fieldCaseCell) : null;
			Cell textCell = currentRow != null ? currentRow.getCell(textCol) : null;
			String textValue = textCell != null ? getCellVal(textCell) : null;
			String nextTextValue = null;
			String nextCaseIDValue = null;
			if (i < sheet.getLastRowNum() - 1) {
				Row nextRow = sheet.getRow(i + 1);
				Cell nextTextCell = nextRow != null ? nextRow.getCell(textCol) : null;
				nextTextValue = nextTextCell != null ? getCellVal(nextTextCell) : null;
				Cell nextCaseCell = nextRow != null ? nextRow.getCell(caseIDCol) : null;
				nextCaseIDValue = nextCaseCell != null ? getCellVal(nextCaseCell) : null;
			}
			if ((caseIDValue == null || caseIDValue.length() == 0) && (textValue == null || textValue.length() == 0)) {
				if ((nextTextValue != null && nextTextValue.length() > 0)
						|| (nextCaseIDValue != null && nextCaseIDValue.length() > 0)) {
					throw new Exception("The data at " + i + " line is empty, Please write the Excel correctly");
				}
				break;
			}
			if ((caseIDValue == null || caseIDValue.length() == 0) && (textValue == null || textValue.length() == 0)) {
				throw new Exception(
						"The data at " + i + " line [ Test Case ID ] is Empty, the [ Text ] Can't Be empty!");
			}
			realRow = i + 1;
		}
		return (realRow - titleCount);
	}

	/**
	 * Description 判断列头是否有合并单元格
	 * 
	 * @param sheet
	 */
	public Integer getMergeRow(Sheet sheet) {
		int merge = 0;
		List<CellRangeAddress> list = sheet.getMergedRegions();
		if (list != null && !list.isEmpty()) {
			for (CellRangeAddress range : list) {
				int firstRow = range.getFirstRow();
				int lastRow = range.getLastRow();
				int firstCell = range.getFirstColumn();
				cellRangeMap.put(firstRow + "-" + firstCell, range);
				if (firstRow == 0 && lastRow > 0) {
					if (merge < (lastRow - firstRow)) {
						merge = lastRow - firstRow;
					}
				}
			}
		}
		return merge;
	}

	/**
	 * 获得真正的column数
	 * 
	 * @param sheet
	 * @return
	 */
	public int getRealColNum(Sheet sheet) {
		int num = 0;
		Row headRow = sheet.getRow(0);
		Row secondRow = sheet.getRow(1);
		num = headRow.getLastCellNum();
		if (num < secondRow.getLastCellNum()) {
			num = secondRow.getLastCellNum();
		}
		return num;
	}

	/**
	 * Description 校验下拉框输入
	 * 
	 * @return
	 * @throws APIException
	 */
	public String checkPickVal(String header, String field, String value, MKSCommand cmd) throws APIException {
		if (value == null || "".equals(value)) {
			return null;
		}
		List<String> valList = PICK_FIELD_RECORD.get(field);
		if (valList == null) {
			valList = cmd.getAllPickValues(field);
		}
		if (valList == null) {
			return "Column [" + (header != null ? header : field) + "] has no valid option value!";
		} 
//		else if (!valList.contains(value)) {
//			return "Value [" + value + "] is invalid for Column [" + (header != null ? header : field)
//					+ "], valid values is " + Arrays.toString(valList.toArray()) + "!";
//		} 
		else {
			String[] pickOfValue = value.split(",");
			for (String val : pickOfValue) {
				if(!valList.contains(val)){
					return "Value [" + val + "] is invalid for Column [" + (header != null ? header : field)
							+ "], valid values is " + Arrays.toString(valList.toArray()) + "!";
				}
			}
		}
		return null;
	}

	/**
	 * Description 校验关联字段输入
	 * 
	 * @return
	 */
	public String checkRelationshipVal() {

		return "";
	}

	/**
	 * Description 校验用户输入
	 * 
	 * @return
	 */
	public String checkUserVal(String value, String field) {
		int leftIndex = -1;
		int rightIndex = -1;
		boolean endFormat = false;
		if (value.indexOf("(") > -1) {
			leftIndex = value.indexOf("(");
		} else if (value.indexOf("（") > -1) {
			leftIndex = value.indexOf("（");
		}
		if (value.indexOf(")") > -1) {
			rightIndex = value.indexOf(")");
			endFormat = value.endsWith(")");
		} else if (value.indexOf("）") > -1) {
			rightIndex = value.indexOf("）");
			endFormat = value.endsWith("）");
		}
		String formatValue = null;
		if (leftIndex > 0 && rightIndex > 0 && endFormat) {
			formatValue = value.substring(leftIndex + 1, rightIndex);
		} else {
			formatValue = value;
		}
		if (USER_FULLNAME_RECORD.contains(formatValue.toLowerCase())) {
			IS_USER = true; // 若用户存在修改标识 ， 往下执行好判断
			return "";
		}
		return "Column [" + field + "] input value [" + value + "] is not exist";
	}

	/**
	 * Description 校验relationship 输入的ID 是否带[]，是的话去掉
	 * 
	 * @return
	 */
	public String checkRelationshipVal(String value) {
		if (value.startsWith("[") && value.endsWith("]")) {
			RELATIONSHIP_MISTAKEN = true;
		}
		return "";
	}

	/**
	 * Description 校验组输入
	 * 
	 * @return
	 */
	public String checkGroupVal() {

		return "";
	}

	/**
	 * Description 校验组输入
	 * 
	 * @return
	 */
	public String checkBooleanVal() {

		return "";
	}

	/**
	 * Description 校验输入值是否合法
	 * 
	 * @return
	 * @throws APIException
	 */
	public String checkFieldValue(String header, String field, String value, MKSCommand cmd) throws APIException {
		String fieldType = FIELD_TYPE_RECORD.get(field);

		if ("pick".equalsIgnoreCase(fieldType)) {
			return checkPickVal(header, field, value, cmd);
		}
		if ("Category".equalsIgnoreCase(field)) {
			return checkCategory(value);
		}
		if ("Date".equalsIgnoreCase(fieldType)) {
			return checkDate(value);
		}
		if ("User".equalsIgnoreCase(fieldType)) {
			return checkUserVal(value, field);
		}
		if ("relationship".equalsIgnoreCase(fieldType)) {
			return checkRelationshipVal(value); // 检查关联的ID是不是带 []
		}
		return null;
	}

	/**
	 * Description 校验Category
	 * 
	 * @return
	 */
	public String checkCategory(String value) {
		if (!CURRENT_CATEGORIES.contains(value)) {
			return "[" + value + "] is invalid for Category, valid values is "
					+ Arrays.toString(CURRENT_CATEGORIES.toArray()) + "!";
		}
		return null;
	}

	/**
	 * Description 校验时间格式
	 * 
	 * @return
	 */
	public String checkDate(String value) {
		value = value.trim();
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = null;
		try {
			date = sdf2.parse(value);
			if (date == null)
				date = sdf3.parse(value);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		if (date == null) {
			return "[" + value + "] input error, The date and date you entered is incorrectly formatted."
					+ "The Correct Format : [yyyy-MM-dd HH:mm:ss] [yyyy/MM/dd HH:mm:ss] ";
		}
		return null;

	}

	/**
	 * Description 处理数据，并校验
	 * 
	 * @param data
	 * @param importType
	 * @param cmd
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "static-access" })
	public List<Map<String, Object>> checkExcelData(List<Map<String, Object>> data, Map<String, String> errorRecord,
			String importType, MKSCommand cmd) throws Exception {
		Map<String, Map<String, String>> headerConfig = headerConfigs.get(importType);
		List<Map<String, Object>> resultData = new ArrayList<Map<String, Object>>();
		ImportApplicationUI.logger.info("Begin Deal Excel Data ,Data size is :" + data.size());
		if (FIELD_TYPE_RECORD == null || FIELD_TYPE_RECORD.isEmpty()) {
			/** 查询Field ，为Field校验做准备 */
			List<String> importFields = new ArrayList<String>();
			for (String header : caseFields) {
				Map<String, String> fieldConfig = headerConfig.get(header);
				if (fieldConfig != null) {
					String field = fieldConfig.get("field");
					if (!"-".equals(field)) {
						importFields.add(field);
					}
				}
			}
			FIELD_TYPE_RECORD.putAll(cmd.getAllFieldType(importFields, PICK_FIELD_RECORD));
		}
		if (CURRENT_CATEGORIES.isEmpty()) {

			parseCurrentCategories(DOC_TYPE_MAP.get(importType));
		}
		this.USER_FULLNAME_RECORD.addAll(cmd.getAllUserIdAndName()); // 查询出所有的user的name
																		// 和 Id
																		// 然后存放在
																		// USER_FULLNAME_RECORD
		Map<String, Object> newMap = null;
		StringBuffer allMessage = new StringBuffer();
		int j = 1;// test result循环下标
		List<String> sessionIds = null;
		Map<String, List<String>> sessionInfoRecord = new HashMap<>();// 只获取系统当前Session的信息。
		Map<String, String> testsRecords = new HashMap<>();
		Map<String, String> sysTestIdsMap = new HashMap<>();
		;
		for (int i = 0; i < data.size(); i++) {
			boolean hasError = false;// 校验出错误
			StringBuffer errorMessage = new StringBuffer();
			Map<String, Object> rowMap = data.get(i);
			String caseID = (String) rowMap.get(TEST_CASE_ID);
			newMap = new HashMap<String, Object>();
			if (caseID != null && !"".equals(caseID)) {
				newMap.put("ID", caseID);
			}
			for (String header : caseFields) {
				Map<String, String> fieldConfig = headerConfig.get(header);
				if (fieldConfig != null) {
					String field = fieldConfig.get("field");
					String value = (String) rowMap.get(header);
					if (!"-".equals(field) && value != null && !"".equals(value)) {
						String message = checkFieldValue(header, field, value, cmd);// 校验Test
																					// //
																					// Case字段值
						if (message == null || "".equals(message)) {
							// 在此已经判断用户是否存在 ， 若存在 IS_USER 标识为 ture , 若不存在为 false
							if (IS_USER) {
								// list.get(p).toString()
								// 判断导入的user类型的数据格式是不是 : 用户(ID) 是的话截取 ()内ID 。
								int leftIndex = -1;
								int rightIndex = -1;
								boolean endFormat = false;
								if (value.indexOf("(") > -1) {
									leftIndex = value.indexOf("(");
								} else if (value.indexOf("（") > -1) {
									leftIndex = value.indexOf("（");
								}
								if (value.indexOf(")") > -1) {
									rightIndex = value.indexOf(")");
									endFormat = value.endsWith(")");
								} else if (value.indexOf("）") > -1) {
									rightIndex = value.indexOf("）");
									endFormat = value.endsWith("）");
								}
								if (leftIndex > 0 && rightIndex > 0 && endFormat) {
									String userId = value.substring(leftIndex + 1, rightIndex);
									if (userId.matches("[G][W]\\d{0,9}") || userId.matches("[g][w]\\d{0,9}")
											|| userId.matches("[G][w]\\d{0,9}") || userId.matches("[g][W]\\d{0,9}")) {
										// 判断里面ID格式是不是 GW + 数字 是的话在之前查询的数据获取值
										newMap.put(field, userId);
									}

								} else if (value.matches("[G][W]\\d{0,9}") || value.matches("[g][w]\\d{0,9}")
										|| value.matches("[G][w]\\d{0,9}") || value.matches("[g][W]\\d{0,9}")) { // 判断如果不是用户(ID)的格式
									newMap.put(field, value);
								} else {
									errorMessage.append(" Field [" + field
											+ "]  data format should be \"name(Login ID)\" or \"Login ID\" \n");
									hasError = true;
								}
								IS_USER = false;
							} else if (RELATIONSHIP_MISTAKEN) { // 如果是Relationship类型的字段，并且数字前面带[]
																// ，就将中括号去掉
								value = value.substring(1, value.length() - 1);//
								newMap.put(field, value);
								RELATIONSHIP_MISTAKEN = false;
							} else {
								newMap.put(field, value);
							}
						} else {
							errorMessage.append("line " + (i + 3) + ": ").append(message).append("\n");
							hasError = true;
						}
					}
				}
			}
			if (hasError) {
				allMessage.append(errorMessage);
				continue;
			}
			if (rowMap.containsKey(TEST_STEP)) {// Test Case包含有 Test Step信息
				Object steps = rowMap.get(TEST_STEP);
				if (steps instanceof List) {
					List<String> importStepFields = importHeadersMap.get(importType + "-stepFields");
					List<Map<String, String>> currentSteps = (List<Map<String, String>>) steps;
					if (!currentSteps.isEmpty()) {// Test Case包含有 Test Step信息
						List<Map<String, String>> stepList = new ArrayList<Map<String, String>>();
						Map<String, Map<String, String>> mapRecord = new HashMap<String, Map<String, String>>();
						Map<String, String> stepMap = null;
						String parentRecord = null;
						for (int index = 0; index < currentSteps.size(); index++) {// 循环处理Test
																					// Step信息
							Map<String, String> map = currentSteps.get(index);
							String acturalField = null;
							boolean existMap = false;
							String parentField = map.get("ParentField");
							map.remove("ParenetField");
							StringBuffer acturalFieldVal = null;
							boolean hasVal = false;
							for (String header : importStepFields) {
								if (map.containsKey(header)) {
									Map<String, String> fieldConfig = headerConfig.get(header);
									if (fieldConfig != null) {
										String field = fieldConfig.get("field");
										String value = (String) map.get(header);
										boolean needFieldSet = true;
										if (fieldConfig.containsKey(NEED_FIELD_SET)) {
											needFieldSet = Boolean.valueOf(fieldConfig.get(NEED_FIELD_SET));
										}
										if ("ID".equals(field) || "Test Procedure".equals(field)) {
											if (value != null && !"".equals(value)) {
												if (mapRecord.get(value) != null) {
													stepMap = mapRecord.get(value);
													existMap = true;
												} else {
													stepMap = new HashMap<String, String>();
													mapRecord.put(value, stepMap);
												}
											} else {
												Map<String, String> tempMap = mapRecord.get(value + "_" + index);
												if (tempMap == null && parentRecord != null
														&& !parentField.equals(parentRecord)) {
													tempMap = mapRecord.get(value + "_" + (index - 1));
												}
												if (tempMap != null) {
													if ((parentField != null && !"".equals(parentField)
															&& !tempMap.containsKey(parentField))
															|| (parentField == null || !"".equals(parentField)
																	&& !tempMap.containsKey(field))) {
														stepMap = tempMap;
														existMap = true;
													}
												} else {
													stepMap = new HashMap<String, String>();
													mapRecord.put(value + "_" + index, stepMap);
												}
											}
										}
										if (fieldConfig.containsKey(PARENT_FIELD) && !header.equals("Test Procedure")) {
											if (acturalField == null) {
												acturalField = fieldConfig.get(PARENT_FIELD);
												if (!acturalField.equals(parentField)) {
													acturalField = parentField;
												}
												acturalFieldVal = new StringBuffer();
											}
											if (needFieldSet)
												acturalFieldVal.append(field).append(": ");
											acturalFieldVal.append(value);
											if (!value.endsWith("\n")) // 如果拼接字符串不是以\n结尾，拼接
												acturalFieldVal.append("\n");
										} else {
											if (value != null && !"".equals(value)) {
												stepMap.put(field, value);// 存放非拼接字段
											}
										}
										if (value != null && !"".equals(value))
											hasVal = true;
									}
								}
							}
							if (acturalFieldVal != null) {// 实际存放Step值。数据不为空
								stepMap.put(acturalField, acturalFieldVal.toString());
							}
							if (!existMap && hasVal)
								stepList.add(stepMap);
							parentRecord = parentField;
						}
						newMap.put(TEST_STEP, stepList);
					}
				} else if (steps instanceof String) {
					String stepIds = (String) steps;
					if (stepIds != null && !"".equals(stepIds)) {
						stepIds = stepIds.replaceAll(";", ",").replaceAll("，", ",");
						newMap.put("Test Steps", stepIds);
					}
				}
			}
			// 02/11
			if (rowMap.containsKey(TEST_RESULT)) {// Test Case包含有 Test Result信息
				Object results = rowMap.get(TEST_RESULT);
				if (results instanceof List) {
					List<Map<String, String>> currentResults = (List<Map<String, String>>) results;
					if (!currentResults.isEmpty()) {
						for (Map<String, String> map : currentResults) {// 循环校验Test
																		// Result信息
							String sessionId = map.get(SESSION_ID);
							if (sessionId == null || sessionId.equals(""))// 对sessionId做校验
								allMessage.append("line " + (i + 3) + "Session ID is Empty for Import Test Result! \n");
							List<String> caseList = sessionInfoRecord.get(sessionId);
							if (caseList == null) {// 当前Session未查询时，查询Session信息
								sessionIds = new ArrayList<>();
								sessionIds.add(sessionId);
								// 根据sessionID获取session信息
								Map<String, String> sessionInfo = cmd
										.getItemByIds(sessionIds, Arrays.asList("ID", "Tests", "State")).get(0);
								String sysTestsId = sessionInfo.get("Tests");// 从系统中获取到的suite
								caseList = new ArrayList<>();
								if (sysTestsId != null && !"".equals(sysTestsId)) {
									String[] sysTestsIdArr = sysTestsId.split(",");
									List<Map<String, String>> testsList = cmd.findItemsByIDs(Arrays.asList(sysTestsIdArr),
											Arrays.asList("ID,Type"));
									for (Map<String, String> testMap : testsList) {
										String type = testMap.get("Type");
										String ID = testMap.get("ID");
										if ("Test Case".equals(type)) {
											caseList.add(ID);
										} else {
											List<String> allContains = cmd.allContents(ID);
											if(allContains.size()>0){
												caseList.addAll(allContains);
											}
										}
									}
								}
								sessionInfoRecord.put(sessionId, caseList);
							}
							if (caseList != null) {
								// 02/18
								if (!caseList.contains(caseID)) {
									allMessage.append("第" + (i + 3) + "行" + "，" + "第" + j + "轮"
											+ "Test Session与当前测试用例未建立关联关系！ \n");
								}
							} else {
								allMessage.append(
										"第" + (i + 3) + "行" + "，" + "第" + j + "轮" + "Test Session ID不正确，未查询到对应信息！ \n");
							}
						}
						j++;
						newMap.put(TEST_RESULT, currentResults);
					}
				}
			}
			resultData.add(newMap);
		}
		allMessage.append(cmd.checkIssueType(sessionIds, TEST_SESSION, SESSIONN_STATE));// 校验test
																						// session的状态
		errorRecord.put("error", allMessage.toString());
		ImportApplicationUI.logger.info("End Deal Excel Data , all Data size is :" + resultData.size());
		return resultData;
	}

	/**
	 * Description 开始导入数据
	 * 
	 * @param data
	 * @param cmd
	 * @param importType
	 * @param shortTitle
	 * @param project
	 * @param testSuiteID
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void startImport(List<Map<String, Object>> data, MKSCommand cmd, String importType, String shortTitle,
			String project, String testSuiteID) throws Exception {
		// 删除Token
		// TestCaseImport.TOKEN = null;
		// 下面List用于收集操作信息，用于统计
		List<String> caseUpdate = new ArrayList<String>(), caseCreate = new ArrayList<String>(),
				stepUpdate = new ArrayList<String>(), stepCreate = new ArrayList<String>();
		List<String> caseUpdateF = new ArrayList<String>(), caseCreateF = new ArrayList<String>(),
				stepUpdateF = new ArrayList<String>(), stepCreateF = new ArrayList<String>();

		// int totalSheetNum = data.size();
		boolean hasStep = false;
		// 遍历信息

		boolean createTest = false;
		if (testSuiteID == null || "".equals(testSuiteID)) {
			Map<String, String> docInfo = new HashMap<String, String>();
			docInfo.put("Document Short Title", shortTitle);
			docInfo.put("Project", project);
			docInfo.put("State", "Open");
			String docType = DOC_TYPE_MAP.get(importType);
			if (docType.endsWith("Document"))
				docInfo.put("Shared Category", "Document");
			else if ("Test Suite".equals(docType))
				docInfo.put("Shared Category", "Suite");
			testSuiteID = cmd.createDocument(docType, docInfo);
			createTest = true;
		}
		Map<String, String> structureRecord = new HashMap<String, String>();
		// int sheetNum = 1;

		// 得到Project
		if (!createTest) {
			project = cmd.getItemByIds(Arrays.asList(testSuiteID), Arrays.asList("Project")).get(0).get("Project");
		}
		ImportApplicationUI.logger.info("Project for sheet 1 " + " is : " + project);

		// 将解析后的数据进行导入，因为不涉及结构，因此一条导入不成功，后续仍可导入
		String parentId = testSuiteID;// 涉及
		if (data.isEmpty()) {
			return;
		}
		int totalCaseNum = data.size();
		ImportApplicationUI.logger.info("Start Import Excel Data , all Data size is :" + totalCaseNum);
		for (int index = 0; index < totalCaseNum; index++) {
			Map<String, Object> testCaseData = data.get(index);
			logger.info("Now Deal row " + index + " data");
			int caseNum = index + 1;
			String caseId = null;
			if (testCaseData.containsKey("ID")) {
				caseId = testCaseData.get("ID").toString();
			}
			if (caseId == null || "".equals(caseId)) {
				ImportApplicationUI.showLogger(" \tStart to Create " + importType);
			} else {
				ImportApplicationUI.showLogger(" \tStart to deal " + importType + "  : " + caseId);
			}
			Map<String, String> newTestCaseData = new HashMap<>();
			List<String> newRelatedStepIds = new ArrayList<>();
			// 1. 先处理Test
			// Step信息(更新创建或删除)，遍历得到OPERATING_ACTION和EXPECTED_RESULTS信息塞入newTestCaseData中
			if (testCaseData.containsKey(TEST_STEP)) {
				this.getTestStep(newTestCaseData, newRelatedStepIds, testCaseData, project, cmd, stepCreate,
						stepCreateF, stepUpdate, stepUpdateF);
				hasStep = true;
			}
			// 把Test Result信息获取出来
			List<Map<String,String>> resultList = null;
			if(testCaseData.get(TEST_RESULT) != null ){
				resultList = (List<Map<String,String>>)testCaseData.get(TEST_RESULT);
				testCaseData.remove(TEST_RESULT);
			}
			// 2. 再处理Test Case的信息(更新或创建，不包括创建)
			String beforeId = null;// 涉及结构
			String strucetureVal = null;
			parentId = testSuiteID;
			if (parentStructure) {
				strucetureVal = (String) testCaseData.get("Contained By");
				beforeId = structureRecord.get(strucetureVal);
				if ("C".equals(strucetureVal)) {
					parentId = structureRecord.get("P");
					if (beforeId == null || "".equals(beforeId)) {
						beforeId = "first";
					}
				}
			}
			if (parentStructure && caseFields.contains("Parent") && parentId == null) {
				throw new Exception("Need to have a P-level " + importType);
			}
			caseId = this.getTestCase(parentId, newTestCaseData, testCaseData, project, cmd, caseId, beforeId,
					caseCreate, caseCreateF, caseUpdate, caseUpdateF, importType);
			testCaseData.put("ID", caseId);
			// 3. 关联Test Case与Test Step
			if (testCaseData.containsKey(TEST_STEP) && newRelatedStepIds.size() > 0) {
				this.relatedCaseAndStep(caseId, newRelatedStepIds, cmd);
			}
			// 4. 记录beforeID及结构
			if (parentStructure) {
				structureRecord.put(strucetureVal, caseId);
			}
			// 5. 导入测试结果
			dealTestResults(resultList, cmd, caseId);

			ImportApplicationUI.showProgress(1, 1, caseNum, totalCaseNum);
		}
		ImportApplicationUI.showLogger("End to deal " + importType + " : " + testSuiteID);
		ImportApplicationUI.showLogger("==============================================");
		ImportApplicationUI.showLogger("Create " + CONTENT_TYPE + ": success (" + caseCreate.size() + "," + caseCreate
				+ "), failed (" + caseCreateF.size() + ")");
		ImportApplicationUI.showLogger("Update " + CONTENT_TYPE + ": success (" + caseUpdate.size() + "," + caseUpdate
				+ "), failed (" + caseUpdateF.size() + "," + caseUpdateF + ")");
		if (hasStep) {
			ImportApplicationUI.showLogger("Create Test Step: success (" + stepCreate.size() + "," + stepCreate
					+ "), failed (" + stepCreateF.size() + ")");
			ImportApplicationUI.showLogger("Update Test Step: success (" + stepUpdate.size() + "," + stepUpdate
					+ "), failed (" + stepUpdateF.size() + "," + stepUpdateF + ")");
		}
	}

	/**
	 * 将Test Case与Test Step的关联关系进行更新
	 * 
	 * @param caseId
	 * @param newRelatedStepIds
	 * @param cmd
	 * @throws APIException
	 */
	public void relatedCaseAndStep(String caseId, List<String> newRelatedStepIds, MKSCommand cmd) throws APIException {
		if (caseId != null && caseId.length() > 0) {
			StringBuffer sb = new StringBuffer();
			for (String step : newRelatedStepIds) {
				sb.append(sb.toString().length() > 0 ? "," + step : step);
			}
			Map<String, String> map = new HashMap<>();
			map.put("Test Steps", sb.toString());
			cmd.editissue(caseId, map);
			// for (String step : newRelatedStepIds) {
			// try {
			// cmd.addRelationship(caseId, "Test Steps", step);
			// TestCaseImport.showLogger(" \tSuccess to addRelationship with
			// Test Case : " + caseId
			// + " with Test Step : " + step);
			// } catch (APIException e) {
			// TestCaseImport.showLogger(" \tFailed to addRelationship with Test
			// Case : " + caseId
			// + " with Test Step : " + step);
			// TestCaseImport.logger.error("Failed to addRelationship with Test
			// Case : " + caseId
			// + " with Test Step : " + step + ", " +
			// ExceptionUtil.catchException(e));
			// }
			// }
		}
	}

	/**
	 * 创建或更新Test Case
	 * 
	 * @param
	 *
	 * @param newTestCaseData
	 *            新的Case信息集合
	 * @param caseMap
	 *            原有的Case信息集合
	 * @param project
	 *            Suite的Project
	 * @param cmd
	 * @param caseId
	 * @param beforeId
	 * @param caseCreate
	 * @param caseCreateF
	 * @param caseUpdate
	 * @param caseUpdateF
	 * @throws Exception
	 */
	public String getTestCase(String parentId, Map<String, String> newTestCaseData, Map<String, Object> caseMap,
			String project, MKSCommand cmd, String caseId, String beforeId, List<String> caseCreate,
			List<String> caseCreateF, List<String> caseUpdate, List<String> caseUpdateF, String importType)
			throws Exception {

		logger.info("Data Of " + CONTENT_TYPE + " ID [" + caseId + "]");
		// for (String field : caseFiels) {
		// Object value = caseMap.get(field);
		// if (value != null && value.toString().length() > 0) {
		// newTestCaseData.put(field, value.toString());
		// }
		// }
		// 需修改
		String defaultCategory = "Comment";// 设置Category,默认为Comment，根据选择模板不同，设置不同默认值
		if (SOFTWARE_UNIT_TEST.equals(importType) || DCT_TEST.equals(importType)) {
			defaultCategory = "Software Unit Test";
		} else if (SOFTWARE_INTEGRATION_TEST.equals(importType)) {
			defaultCategory = "Software Integration Test";
		} else if (SOFTWARE_QUALI_TEST.equals(importType)) {
			defaultCategory = "Software Qualification Test";
		} else if (SYSTEM_INTEGRATION_TEST.equals(importType)) {
			defaultCategory = "System Integration Test";
		} else if (SYSTEM_QUALI_TEST.equals(importType)) {
			defaultCategory = "System Qualification Test";
		} else if ("DCT Test Specification".equals(importType)) {
			defaultCategory = "Software Unit Test";
		}
		for (Map.Entry<String, Object> entrty : caseMap.entrySet()) {
			String field = entrty.getKey();
			Object value = entrty.getValue();
			if(RICH_FIELDS.contains(field)){
				value = value.toString().replace("\n", "<br/>").replace("\r", "<br/>");
			}
			if (value != null && value.toString().length() > 0) {
				newTestCaseData.put(field, value.toString());
			}
			if (field.equals("Category")) {// 如果行数据内有Category字段，从行数据内获取Category值
				if (value != null && value.toString() != null && value.toString().length() > 0) {
					defaultCategory = value.toString().trim();
				}
			}
		}
		String containedBy = newTestCaseData.get("Contained By");
		newTestCaseData.remove("ID");
		newTestCaseData.remove("Document ID");
		newTestCaseData.remove("Test Step");
		newTestCaseData.remove("Contained By");
		if (caseId == null || caseId.length() == 0) {
			// 创建Test Case
			try {
				if (containedBy != null && !"".equals(containedBy) && containedBy.matches("[0-9]*")) {
					parentId = containedBy;
				}
				newTestCaseData.put("Category", defaultCategory);
				newTestCaseData.put("Project", project);
				newTestCaseData.put("State", "Active");
				caseId = cmd.createContent(parentId, newTestCaseData, CONTENT_TYPE, beforeId);
				caseCreate.add(caseId);
				ImportApplicationUI.showLogger(" \tSuccess to create " + CONTENT_TYPE + " : " + caseId);
			} catch (APIException e) {
				caseCreateF.add(caseId);
				ImportApplicationUI.showLogger(" \tFailed to create " + CONTENT_TYPE + " : " + caseId);
				logger.error("Failed to create test case : " + ExceptionUtil.catchException(e));
			}
		} else {
			// 更新Test Case
			// 遍历出所有 overRide为 true 的字段，
			Map<String, Map<String, String>> fieldMaps = headerConfigs.get(importType);
			Collection<Map<String, String>> fieldMapValues = fieldMaps.values();
			List<String> fields = new ArrayList<String>();
			for (Map<String, String> values : fieldMapValues) {
				if (values.get("overRide").equals("false")) {
					fields.add(values.get("field"));
				}
			}
			// 然后调用 mks命令查询出导入的 所有 ids 的内容。判断当前为true字段是否有值 ,
			// getItemByIds(List<String> ids,List<String> field) 此方法通过Id 获取字段的值
			List<String> ids = new ArrayList<String>();
			ids.add(caseId);
			List<Map<String, String>> data = cmd.getItemByIds(ids, fields);
			Map<String, String> dataMap = data.get(0);
			for (String field : fields) {
				String fieldValue = dataMap.get(field);
				// 有 ： 不更新 没有 ： 更新
				if (!"".equals(fieldValue) && null != fieldValue) {
					newTestCaseData.remove(field);
				}
			}
			// 判断当前条目中是否 含有 Text
			// 字段，如果有，检查此字段是否可以编辑更新（含有Text字段的条目，是否可以更新，在XML里有属性OnlyCreate 规定
			// 。false为可编辑，true为不可编辑）
			checkOnlyCreate(newTestCaseData, importType);
			try {
				cmd.editissue(caseId, newTestCaseData);
				caseUpdate.add(caseId);
				// 1.更新顺序
				if (parentStructure && beforeId != null && !"".equals(beforeId)) {
					cmd.moveContent(parentId, beforeId, caseId);
				}
				ImportApplicationUI.showLogger(" \tSuccess to update Test Case : " + caseId);
			} catch (APIException e) {
				caseUpdateF.add(caseId);
				ImportApplicationUI.showLogger(" \tFailed to update Test Case : " + caseId);
				logger.error("Failed to edit test case : " + ExceptionUtil.catchException(e));
			}
		}
		return caseId;
	}

	/**
	 * 检测当前要更新Case里面有没有 Text 字段 ， 并且判断该字段是否可以编辑 xml 中有 onlyCreate 属性规定是否可以更新
	 * 
	 * @param newTestCaseData
	 * @param importType
	 */
	private void checkOnlyCreate(Map<String, String> newTestCaseData, String importType) {
		Map<String, Map<String, String>> headerConfig = headerConfigs.get(importType);
		Collection<Map<String, String>> values = headerConfig.values();
		for (Map<String, String> map : values) {
			if (map.get("onlyCreate") != null) {
				boolean onlyCreate = Boolean.valueOf(map.get("onlyCreate"));
				if (onlyCreate) {
					String field = map.get("field");
					newTestCaseData.remove(field);
				}
			}
		}
	}

	/**
	 * 先处理Test Step信息(更新创建或删除)，
	 * 遍历得到OPERATING_ACTION和EXPECTED_RESULTS信息塞入newTestCaseData中, 并将创建和更新的Step
	 * ID塞于newRelatedStepIds中
	 * 
	 * @param newTestCaseData
	 *            新的Case信息集合
	 * @param newRelatedStepIds
	 *            创建Test Step的集合
	 * @param caseMap
	 *            原有Case信息集合
	 * @param project
	 *            Suite的Project信息
	 * @param cmd
	 * @param stepCreate
	 * @param stepCreateF
	 * @param stepUpdate
	 * @param stepUpdateF
	 */
	public void getTestStep(Map<String, String> newTestCaseData, List<String> newRelatedStepIds,
			Map<String, Object> caseMap, String project, MKSCommand cmd, List<String> stepCreate,
			List<String> stepCreateF, List<String> stepUpdate, List<String> stepUpdateF) {
		@SuppressWarnings("unchecked")
		List<Map<String, String>> testStepData = (List<Map<String, String>>) caseMap.get(TEST_STEP);
		int i = 1;
		if (testStepData != null && testStepData.size() > 0) {
			ImportApplicationUI.showLogger(" \t\tHas Test Step size  : " + testStepData.size());
			for (Map<String, String> stepMap : testStepData) {
				// if (i == 1) {// 将第一步的Test Step的Precondition加入到Test Case中
				// newTestCaseData.put(INITIAL_STATE_PRECODITION,
				// stepMap.get(PRECODITION) == null ? "" :
				// stepMap.get(PRECODITION));
				// }
				String stepId = stepMap.get("ID");
				stepMap.remove("ID");
				// 处理Step Order
				// stepMap.put(STEP_ORDER, i + "");
				// if (stepMap.get("Test Step") == null || stepMap.get("Test
				// Step").trim().length() == 0) {
				// stepMap.put("Test Step", i + "");
				// }
				if (stepId == null || stepId.length() == 0) {
					// 创建Test Step，并关联Test Case
					try {
						stepMap.put("Project", project);
						stepId = cmd.createIssue(TEST_STEP, stepMap, null);
						stepCreate.add(stepId);
						ImportApplicationUI.showLogger(" \t\tSuccess to create Test Step " + i + ", " + stepId);
					} catch (APIException e) {
						stepCreateF.add(stepId);
						ImportApplicationUI.showLogger(" \t\tFailed to create Test Step");
						logger.error("Failed to create test step : " + ExceptionUtil.catchException(e));
					}
				} else {
					try {
						cmd.editissue(stepId, stepMap);
						stepUpdate.add(stepId);
						ImportApplicationUI.showLogger(" \t\tSuccess to update Test Step " + i + ", " + stepId);
					} catch (APIException e) {
						stepUpdateF.add(stepId);
						ImportApplicationUI.showLogger(" \t\tFailed to update Test Step " + i + ", " + stepId);
						logger.error("Failed to edit test step : " + ExceptionUtil.catchException(e));
					}
				}
				newRelatedStepIds.add(stepId);
				i++;
			}
		}
	}

	/**
	 * 处理测试结果
	 * @param resultDatas
	 * @param cmd
	 * @param caseID
	 */
	public void dealTestResults(List<Map<String, String>> resultDatas, MKSCommand cmd, String caseID) throws APIException {
		if (resultDatas != null && !resultDatas.isEmpty()) {
			List<Map<String, String>> caseResult = cmd.getResult(caseID);
			Map<String,Map<String,String>> resultRecord = new HashMap<String,Map<String,String>>();
			if (caseResult != null && !caseResult.isEmpty()){
				for (Map<String, String> map : caseResult){
					resultRecord.put(map.get("sessionID"),map);
				}
			}
			for (Map<String, String> result : resultDatas) {
				String sessionId = result.get(SESSION_ID);
				String verdict = result.get(VERDICT);
				String observedResult = result.get(OBSERVED_RESULT);
				String annotation = result.get(ANNOTATION);
				String serverity = result.get(SERVERITY);
				String reproducibility = result.get(REPRODUCIBILITY);
				String SWVersion = result.get(SW_VERSION);
				String HWVerdion = result.get(HW_VERSION);
				if (resultRecord.get(sessionId) == null){//如果查到系统当前的test result为空，则把excel模板中读取到的test result数据导入 02/19
					cmd.createResult(sessionId,verdict,observedResult,annotation,serverity,reproducibility,SWVersion,HWVerdion,caseID);
				}else{//更新系统当前的test result
					cmd.editResult(sessionId,verdict,observedResult,annotation,serverity,reproducibility,SWVersion,HWVerdion,caseID);
				}
			}
		}
	}



}
