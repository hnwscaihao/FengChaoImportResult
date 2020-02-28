package com.fc.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import com.fc.service.ExcelUtil;
import com.fc.ui.ImportApplicationUI;

import org.apache.log4j.Logger;

import com.mks.api.CmdRunner;
import com.mks.api.Command;
import com.mks.api.IntegrationPoint;
import com.mks.api.IntegrationPointFactory;
import com.mks.api.MultiValue;
import com.mks.api.Option;
import com.mks.api.OptionList;
import com.mks.api.SelectionList;
import com.mks.api.Session;
import com.mks.api.response.APIException;
import com.mks.api.response.Field;
import com.mks.api.response.Item;
import com.mks.api.response.ItemList;
import com.mks.api.response.Response;
import com.mks.api.response.Result;
import com.mks.api.response.WorkItem;
import com.mks.api.response.WorkItemIterator;

public class MKSCommand {

	private static final Logger logger = Logger.getLogger(MKSCommand.class.getName());
	private Session mksSession = null;
	private IntegrationPointFactory mksIpf = null;
	private IntegrationPoint mksIp = null;
	private CmdRunner mksCmdRunner = null;
	private Command mksCommand = null;
	private Response mksResponse = null;
	private boolean success = false;
	private String currentCommand;
	private String hostname = null;
	private int port = 7001;
	private String user;
	private String password;
	private int APIMajor = 4;
	private int APIMinor = 16;
	private static String errorLog;

	public MKSCommand(String _hostname, int _port, String _user, String _password, int _apimajor, int _apiminor) {
		hostname = _hostname;
		port = _port;
		user = _user;
		password = _password;
		getSession();
//		createSession();
	}

	public MKSCommand(String args[]) {
		hostname = args[0];
//		port = Integer.parseInt(args[1]);
		user = args[2];
		password = args[3];
		APIMajor = Integer.parseInt(args[4]);
		APIMinor = Integer.parseInt(args[5]);
		getSession();
	}
	
	public void getSession() {
		try {
			mksIpf = IntegrationPointFactory.getInstance();
			mksIp = mksIpf.createLocalIntegrationPoint(APIMajor, APIMinor);
			mksIp.setAutoStartIntegrityClient(true);
			mksSession = mksIp.getCommonSession();
			mksCmdRunner = mksSession.createCmdRunner();
			mksCmdRunner.setDefaultHostname(hostname);
			mksCmdRunner.setDefaultUsername(user);
			mksCmdRunner.setDefaultPort(port);
			logger.info(" mksip ---" + mksSession.getDefaultHostname());
			logger.info(" mksport" + mksSession.getDefaultPort());
			logger.info(" defaultUser" + mksSession.getDefaultUsername());
			logger.info(" defaultPWD" + mksSession.getDefaultPassword());
		} catch (APIException ae) {
			logger.error(ae.toString(), ae);
		}
	}

	public void setCmd(String _type, String _cmd, ArrayList<Option> _ops, String _sel) {
		mksCommand = new Command(_type, _cmd);
		String cmdStrg = (new StringBuilder(String.valueOf(_type))).append(" ").append(_cmd).append(" ").toString();
		if (_ops != null && _ops.size() > 0) {
			for (int i = 0; i < _ops.size(); i++) {
				cmdStrg = (new StringBuilder(String.valueOf(cmdStrg))).append(_ops.get(i).toString()).append(" ")
						.toString();
				// Option o = new Option(_ops.get(i).toString());
				mksCommand.addOption(_ops.get(i));
			}

		}
		if (_sel != null && _sel != "") {
			cmdStrg = (new StringBuilder(String.valueOf(cmdStrg))).append(_sel).toString();
			mksCommand.addSelection(_sel);
		}
		currentCommand = cmdStrg;
		// logger.info((new StringBuilder("Command:
		// ")).append(cmdStrg).toString());
	}

	public String getCommandAsString() {
		return currentCommand;
	}

	public boolean getResultStatus() {
		return success;
	}

	public String getConnectionString() {
		String c = (new StringBuilder(String.valueOf(hostname))).append(" ").append(port).append(" ").append(user)
				.append(" ").append(password).toString();
		return c;
	}

	public void exec() {
		success = false;
		try {
			mksResponse = mksCmdRunner.execute(mksCommand);
			// logger.info((new StringBuilder("Exit Code:
			// ")).append(mksResponse.getExitCode()).toString());
			success = true;
		} catch (APIException ae) {
			logger.error(ae.getMessage());
			success = false;
			errorLog = ae.getMessage();
		} catch (NullPointerException npe) {
			success = false;
			logger.error(npe.getMessage(), npe);
			errorLog = npe.getMessage();
		}
	}

	public void release() throws IOException {
		try {
			if (mksSession != null) {
				mksCmdRunner.release();
				mksSession.release();
				mksIp.release();
				mksIpf.removeIntegrationPoint(mksIp);
			}
			success = false;
			currentCommand = "";
		} catch (APIException ae) {
			logger.error(ae.getMessage(), ae);
		}
	}

	public void createSession() {
		try {
			mksIpf = IntegrationPointFactory.getInstance();
			mksIp = mksIpf.createIntegrationPoint(hostname, port, APIMajor, APIMinor);
			mksSession = mksIp.createSession(user, password);
			mksCmdRunner = mksSession.createCmdRunner();
			mksCmdRunner.setDefaultHostname(hostname);
			mksCmdRunner.setDefaultPort(port);
			mksCmdRunner.setDefaultUsername(user);
			mksCmdRunner.setDefaultPassword(password);
		} catch (APIException ae) {
			logger.error(ae.getMessage(), ae);
		}
	}

	public String[] getResult() {
		String result[] = null;
		int counter = 0;
		try {
			WorkItemIterator mksWii = mksResponse.getWorkItems();
			result = new String[mksResponse.getWorkItemListSize()];
			while (mksWii.hasNext()) {
				WorkItem mksWi = mksWii.next();
				Field mksField;
				for (Iterator<?> mksFields = mksWi.getFields(); mksFields.hasNext();) {
					mksField = (Field) mksFields.next();
					result[counter] = mksField.getValueAsString();
				}

				counter++;
			}
		} catch (APIException ae) {
			logger.error(ae.toString(), ae);
			JOptionPane.showMessageDialog(null, ae.toString(), "ERROR", 0);
		} catch (NullPointerException npe) {
			logger.error(npe.toString(), npe);
			JOptionPane.showMessageDialog(null, npe.toString(), "ERROR", 0);
		}
		return result;
	}
	
	/**
	 * 根据Ids查询字段的值
	 * 
	 * @param ids
	 * @param fields
	 * @return
	 * @throws APIException
	 */
	public List<Map<String, String>> findItemsByIDs(List<String> ids, List<String> fields) throws Exception {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		Command cmd = new Command("im", "issues");
		MultiValue mv = new MultiValue();
		mv.setSeparator(",");
		for (String field : fields) {
			mv.add(field);
		}
		Option op = new Option("fields", mv);
		cmd.addOption(op);

		SelectionList sl = new SelectionList();
		for (String id : ids) {
			sl.add(id);
		}
		cmd.setSelectionList(sl);

		Response res = null;
		try {
			res = mksCmdRunner.execute(cmd);
			WorkItemIterator it = res.getWorkItems();
			while (it.hasNext()) {
				WorkItem wi = it.next();
				Map<String, String> map = new HashMap<String, String>();
				Iterator fieldIte = wi.getFields();
				while(fieldIte.hasNext()){
					Field field = (Field)fieldIte.next();
					map.put(field.getDisplayName(), field.getValueAsString());
				}
				list.add(map);
			}
		} catch (APIException e) {
			// success = false;
			logger.error(APIExceptionUtil.getMsg(e));
			throw new Exception(APIExceptionUtil.getMsg(e));
		}
		return list;
	}

	/**
	 * 根据Ids查询字段的值
	 * 
	 * @param ids
	 * @param fields
	 * @return
	 * @throws APIException
	 */
	public List<Map<String, String>> getItemByIds(List<String> ids, List<String> fields) throws Exception {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		Command cmd = new Command("im", "issues");
		MultiValue mv = new MultiValue();
		mv.setSeparator(",");
		for (String field : fields) {
			// if (field.equals("Description")) {
			// mv.add(field + "::rich");
			// } else {
			// mv.add(field);
			// }
			mv.add(field);
		}
		Option op = new Option("fields", mv);
		cmd.addOption(op);

		SelectionList sl = new SelectionList();
		for (String id : ids) {
			sl.add(id);
		}
		cmd.setSelectionList(sl);

		Response res = null;
		try {
			res = mksCmdRunner.execute(cmd);
			WorkItemIterator it = res.getWorkItems();
			while (it.hasNext()) {
				WorkItem wi = it.next();
				Map<String, String> map = new HashMap<String, String>();
				for (String field : fields) {
					if (field.contains("::")) {
						field = field.split("::")[0];
					}
					String value = wi.getField(field).getValueAsString();
					map.put(field, value);
				}
				list.add(map);
			}
		} catch (APIException e) {
			// success = false;
			logger.error(APIExceptionUtil.getMsg(e));
			throw new Exception(APIExceptionUtil.getMsg(e));
		}
		return list;
	}

	public boolean getResultState() {
		return success;
	}

	public String getErrorLog() {
		return errorLog;
	}

	public void getAllChild(List<String> ids, List<String> childs, String docId) throws Exception {
		List<Map<String, String>> itemByIds = getItemByIds(ids, Arrays.asList("ID", "Contains"));
		for (Map<String, String> map : itemByIds) {
			String contains = map.get("Contains");
			String id = map.get("ID");
			if (!id.equals(docId)) {
				childs.add(id);
			}
			if (contains != null && contains.length() > 0) {
				List<String> childIds = Arrays.asList(contains.replaceAll("ay", "").split(","));
				getAllChild(childIds, childs, docId);
			}
		}
	}

	public void editIssue(String id, Map<String, String> fieldValue, Map<String, String> richFieldValue)
			throws APIException {
		Command cmd = new Command(Command.IM, "editissue");
		if (fieldValue != null) {
			for (Map.Entry<String, String> entrty : fieldValue.entrySet()) {
				cmd.addOption(new Option("field", entrty.getKey() + "=" + entrty.getValue()));
			}
		}
		if (richFieldValue != null) {
			for (Map.Entry<String, String> entrty : richFieldValue.entrySet()) {
				cmd.addOption(new Option("richContentField", entrty.getKey() + "=" + entrty.getValue()));
			}
		}
		cmd.addSelection(id);
		mksCmdRunner.execute(cmd);
	}
	
	public SelectionList contains(SelectionList documents) throws APIException {
		return relationshipValues("Contains", documents);
	}

	public SelectionList relationshipValues(String fieldName, SelectionList ids) throws APIException {
		if (fieldName == null) {
			throw new APIException("invoke fieldValues() ----- fieldName is null.");
		}
		if (ids == null || ids.size() < 1) {
			throw new APIException("invoke fieldValues() ----- ids is null or empty.");
		}
		Command command = new Command(Command.IM, "issues");
		command.addOption(new Option("fields", fieldName));
		command.setSelectionList(ids);
		Response res = mksCmdRunner.execute(command);
		WorkItemIterator it = res.getWorkItems();
		SelectionList contents = new SelectionList();
		while (it.hasNext()) {
			WorkItem wi = it.next();
			ItemList il = (ItemList) wi.getField(fieldName).getList();
			if(il != null) {
				for (int i = 0; i < il.size(); i++) {
					Item item = (Item) il.get(i);
					String id = item.getId();
					contents.add(id);
				}
			}
		}
		return contents;
	}
	
	public List<String> allContents(String document) throws APIException ,Exception {
		List<String> returnResult = new ArrayList<>();
		Command command = new Command("im", "issues");
		command.addOption(new Option("fields", "contains"));
		command.addSelection(document);
		Response res = mksCmdRunner.execute(command);
		WorkItemIterator it = res.getWorkItems();
		SelectionList sl = new SelectionList();
		List<String> fields = new ArrayList<>();
		fields.add("ID");
		while (it.hasNext()) {
			WorkItem wi = it.next();
			ItemList il = (ItemList) wi.getField("contains").getList();
			for (int i = 0; i < il.size(); i++) {
				Item item = (Item) il.get(i);
				String id = item.getId();
				sl.add(id);
			}
		}
		SelectionList contents = null;
		if (sl != null && sl.size() >= 1) {
			contents = contains(sl);

			if (contents.size() > 0) {
				SelectionList contains = new SelectionList();
				contains.add(contents);
				while (true) {
					SelectionList conteins = contains(contains);
					if (conteins.size() < 1) {
						break;
					}
					contents.add(conteins);
					contains = new SelectionList();
					contains.add(conteins);
				}
			}
			contents.add(sl);
		}
		if(contents.size()>0){
			for(int i=0; i<contents.size(); i++){
				returnResult.add(contents.getSelection(i));
			}
		}
		return returnResult;
	}
	
	public List<Map<String, Object>> getResult(String sessionID, String suiteID, String type) throws APIException {
		List<Map<String, Object>> result = new ArrayList<>();
		Command cmd = new Command("tm", "results");
		cmd.addOption(new Option("sessionID", sessionID));
		if (type.equals("Test Suite")) {
			cmd.addOption(new Option("suiteID", suiteID));
		} else if (type.equals("Test Case")) {
			cmd.addSelection(suiteID);
		}
		List<String> fields = new ArrayList<>();
		fields.add("caseID");
		fields.add("sessionID");
		fields.add("verdict");
		fields.add("modifiedDate");
		fields.add("annotation");
		MultiValue mv = new MultiValue();
		mv.setSeparator(",");
		for (String field : fields) {
			mv.add(field);
		}
		Option op = new Option("fields", mv);
		cmd.addOption(op);
		Response res = null;
		if (type.equals("Test Suite")) {
			res = mksCmdRunner.execute(cmd);
			WorkItemIterator wk = res.getWorkItems();
			while (wk.hasNext()) {
				Map<String, Object> map = new HashMap<>();
				WorkItem wi = wk.next();
				for (String field : fields) {
					Object value = wi.getField(field).getValue();
					map.put(field, value);
				}
				result.add(map);
			}
			// logger.info("Query : --sessionID="+sessionID+", --suiteID="+suiteID);
		} else if (type.equals("Test Case")) {
			try {
				res = mksCmdRunner.execute(cmd);
				WorkItemIterator wk = res.getWorkItems();
				while (wk.hasNext()) {
					Map<String, Object> map = new HashMap<>();
					WorkItem wi = wk.next();
					for (String field : fields) {
						Object value = wi.getField(field).getValue();
						map.put(field, value);
					}
					result.add(map);
				}
				// logger.info("Query : --sessionID="+sessionID+" "+suiteID);
			} catch (Exception e) {
				// Map<String, Object> map = new HashMap<>();
				// map.put("caseID", suiteID);
				// result.add(map);
			}
		}
		return result;
	}

	/**
	 * 更改数据
	 * 
	 * @param id
	 * @param fieldValues
	 * @throws APIException
	 */
	public void editissue(String id, Map<String, String> fieldValues) throws APIException {
		Command cmd = new Command("im", "editissue");
		OptionList ol = new OptionList();
		for (String fieldName : fieldValues.keySet()) {
			String value = fieldValues.get(fieldName);
			if (value==null || value.equals("null")) {
				value = "";
			}
			Option op = null;
			if (ExcelUtil.RICH_FIELDS.contains(fieldName)) 
				op = new Option("richContentField", fieldName + "=" + value);
			else 
				op = new Option("field", fieldName + "=" + value);
			ol.add(op);
		}
		cmd.setOptionList(ol);
		cmd.addSelection(id);
		mksCmdRunner.execute(cmd);
	}

	/**
	 * 创建Content
	 * 
	 * @param parentId
	 * @param fields
	 * @param type
	 * @return
	 * @throws APIException 
	 */
	public String createContent(String parentId, Map<String, String> fields, String type, String beforeId) throws APIException {
		String id = null;
		Command cmd = new Command("im", "createcontent");
		cmd.addOption(new Option("parentID", parentId));
		if(parentId.equals(beforeId)){
			beforeId = null;
		}
		if(beforeId != null && beforeId.length() > 0 && beforeId.equals("first")){
			cmd.addOption(new Option("insertLocation", "first"));
		}
		else if (beforeId != null && beforeId.length() > 0) {
			cmd.addOption(new Option("insertLocation", "after:" + beforeId));
		} else {
			cmd.addOption(new Option("insertLocation", "last"));
		}
		cmd.addOption(new Option("Type", type));
		for (Map.Entry<String, String> entry : fields.entrySet()) {
			String value = entry.getValue();
			if(value==null || value.equals("null")){
				value = "";
			}
			Option op = null;
			if (ExcelUtil.RICH_FIELDS.contains(entry.getKey())) 
				op = new Option("richContentField", entry.getKey() + "=" + value);
			else 
				op = new Option("field", entry.getKey() + "=" + value);
			cmd.addOption(op);
		}
		currentCommand = Arrays.toString(cmd.toStringArray());
		Response res = null;

		res = mksCmdRunner.execute(cmd);
		Result result = res.getResult();
		if (result != null) {
			id = result.getField("resultant").getValueAsString();
		}

		return id;
	}

	public String createIssue(String type, Map<String, String> map, Map<String, String> richContentMap)
			throws APIException {
		String id = null;
		Command cmd = new Command(Command.IM, "createissue");
		cmd.addOption(new Option("type", type));
		if (map != null) {
			for (Map.Entry<String, String> entrty : map.entrySet()) {
				String value = entrty.getValue();
				if(value==null || value.equals("null")){
					value = "";
				}
				cmd.addOption(new Option("field", entrty.getKey() + "=" + value));
			}
		}
		if (richContentMap != null) {
			for (Map.Entry<String, String> entrty : map.entrySet()) {
				String value = entrty.getValue();
				if(value==null || value.equals("null")){
					value = "";
				}
				cmd.addOption(new Option("richContentField", entrty.getKey() + "=" + value));
			}
		}
		Response res = mksCmdRunner.execute(cmd);
		Result result = res.getResult();
		if (result != null) {
			id = result.getField("resultant").getValueAsString();
		}
		return id;
	}
	

	public void addRelationship(String id, String fieldName, String relateID) throws APIException {
		Command cmd = new Command("im", "editissue");
		OptionList ol = new OptionList();
		ol.add(new Option("addRelationships", fieldName + ":" + relateID));
		cmd.setOptionList(ol);
		cmd.addSelection(id);
		mksCmdRunner.execute(cmd);
	}
	
	public void moveContent(String parentId,String beforeId,String id) throws APIException {
		Command cmd = new Command("im", "movecontent");
		cmd.addOption(new Option("parentID",parentId));
		if(parentId.equals(beforeId)){
			beforeId = null;
		}
		if(beforeId != null && "first".equals(beforeId)) {
			cmd.addOption(new Option("insertLocation", "first"));
		} else if(beforeId != null && "last".equals(beforeId)){
			cmd.addOption(new Option("insertLocation", "last"));
		}else if(beforeId != null && beforeId.length()>0){
			cmd.addOption(new Option("insertLocation", "after:" + beforeId));
		}
		cmd.addSelection(id);
		mksCmdRunner.execute(cmd);
	}
	
	public void checkConnect() throws APIException{
		Command cmd = new Command("si", "connect");
		mksCmdRunner.execute(cmd);
	}
	
	/**
	 * 判断docId是该Type类型的数据
	 * @param docID
	 * @param type
	 * @return
	 * @throws Exception 
	 */
	public boolean docIDIsRight(String docID,String type) throws Exception{
		
		List<String> ids = new ArrayList<>();
		ids.add(docID);
		List<String> fields = new ArrayList<>();
		fields.add("Type");
		String docType = this.getItemByIds(ids, fields).get(0).get("Type");
		if(docType.equals(type)){
			return true;
		}else
			return false;
	}
	
	/**
	 * Description 校验Project是否合法
	 * @throws APIException 
	 */
	public boolean checkProject(String project) throws APIException{
		Command cmd = new Command("im", "viewproject");
		cmd.addSelection(project);
		try {
			Response res = mksCmdRunner.execute(cmd);
			boolean result = false;
			WorkItemIterator it = res.getWorkItems();
			while (it.hasNext()) {
				WorkItem wi = it.next();
				if(wi.getDisplayId().equals(project)){
					result = true;
				}
			}
			return result;
		} catch (APIException e) {
			logger.error(e.getMessage());
			throw e;
		}
	}


/*	public boolean viewIssue(String id) throws APIException{
		Command cmd = new Command("im", "viewissue");
		cmd.addSelection(id);
		try {
			Response res = mksCmdRunner.execute(cmd);
			boolean result = false;
			WorkItemIterator it = res.getWorkItems();
			while (it.hasNext()) {
				WorkItem wi = it.next();
				if(wi.getDisplayId().equals(project)){
					result = true;
				}
			}
			return result;
		} catch (APIException e) {
			logger.error(e.getMessage());
			throw e;
		}
	}*/
	
	
	/**
	 * 创建Test Suite
	 * @param type
	 * @param fieldsValue
	 * @return
	 */
	public String createDocument(String type,Map<String,String> fieldsValue) {
		success = true;
		String id = null;
		OptionList ol = new OptionList();
		Option option = new Option("Type", type);
		ol.add(option);
		Set<String> set = fieldsValue.keySet();
		for(String field : set){
			String value = fieldsValue.get(field);
			if(value!=null && !value.isEmpty()){
				Option option2 = new Option("field", field+"="+value);
				ol.add(option2);
			}
		}
		String commandName = "createsegment";
		Command cmd = new Command("im", commandName);
		cmd.setOptionList(ol);
		Response res = null;
		try {
			res = mksCmdRunner.execute(cmd);
			Result result = res.getResult();
			if (result != null) {
				id = result.getField("resultant").getValueAsString();
			}
		} catch (APIException e) {
			success = false;
			logger.info(e.getMessage());
		}
		return id;
	}
	
	/**
	 * Description 获取所有Pick Field的选项值
	 * @param field
	 * @return
	 * @throws APIException
	 */
	public List<String> getAllPickValues(String field) throws APIException {
		List<String> visiblePicks = new ArrayList<>();
		Command cmd = new Command("im", "fields");
		cmd.addOption(new Option("noAsAdmin"));
		cmd.addOption(new Option("fields", "picks"));
		cmd.addSelection(field);
		Response res = mksCmdRunner.execute(cmd);
		if (res != null) {
			WorkItem wi = res.getWorkItem(field);
			if (wi != null) {
				Field picks = wi.getField("picks");
				ItemList itemList = (ItemList) picks.getList();
				if (itemList != null) {
					for (int i = 0; i < itemList.size(); i++) {
						Item item = (Item) itemList.get(i);
						String visiblePick = item.getId();
						if (!visiblePicks.contains(visiblePick)) {
							visiblePicks.add(visiblePick);
						}
					}
				}
			}
		}
		return visiblePicks;
	}
	
	/**
	 * Description 获取所有user
//	 * @param field value
	 * @return
	 * @throws APIException
	 */

	public List <String> getAllUserIdAndName() throws APIException {
		List <String> returnMap = new ArrayList <String>();
		Command cmd = new Command("im", "users");
		cmd.addOption(new Option("fields", "fullname"));
		Response res = mksCmdRunner.execute(cmd);
		if (res != null) {
			WorkItemIterator workItemItera = res.getWorkItems();
			if (workItemItera != null) {
				while(workItemItera.hasNext()) {
					WorkItem workItem = workItemItera.next();
					String Id = workItem.getId();
					returnMap.add( Id.toLowerCase() );
				}
			}
		}
		return returnMap;
	}
	
	
	public Map<String,String> getAllFieldType(List<String> fields, Map<String,List<String>> PICK_FIELD_RECORD) throws APIException{
		Map<String,String> fieldTypeMap = new HashMap<String,String>();
		Command cmd = new Command("im", "fields");
		cmd.addOption(new Option("asAdmin"));
		cmd.addOption(new Option("fields", "picks,type,richContent"));
		for(String field : fields){
			cmd.addSelection(field);
		}
		Response res = mksCmdRunner.execute(cmd);
		if (res != null) {
			WorkItemIterator it = res.getWorkItems();
			while (it.hasNext()) {
				WorkItem wi = it.next();
				String field = wi.getId();
				String fieldType = wi.getField("Type").getValueAsString();
				fieldTypeMap.put(field, fieldType);
				if("pick".equals(fieldType) ){
					Field picks = wi.getField("picks");
					ItemList itemList = (ItemList) picks.getList();
					if (itemList != null) {
						List<String> pickVals = new ArrayList<String>();
						for (int i = 0; i < itemList.size(); i++) {
							Item item = (Item) itemList.get(i);
							String visiblePick = item.getId();
							pickVals.add(visiblePick);
						}
						PICK_FIELD_RECORD.put(field, pickVals);
					}
				}else if("fva".equals(fieldType)){
					
				}
				Field richField = wi.getField("richContent");
				if(richField!=null && richField.getBoolean()!=null && richField.getBoolean()){
					ExcelUtil.RICH_FIELDS.add(field);
				}
			}
		}
		return fieldTypeMap;
	}
	/**
	 * Description 查询所有Projects
	 * @return
	 * @throws APIException
	 */
	public List<String> getProjects(String user) throws APIException{
		List<String> projects = new ArrayList<String>();
		Command cmd = new Command("im", "issues");
		cmd.addOption(new Option("fields","Project"));
		String query = "((field[Type]=Project) )";
		cmd.addOption(new Option("queryDefinition",query));
		Response res = mksCmdRunner.execute(cmd);
		if (res != null) {
			WorkItemIterator it = res.getWorkItems();
			while (it.hasNext()) {
				WorkItem wi = it.next();
				String project = wi.getField("Project").getValueAsString();
				projects.add(project);
			}
		}
		return projects;
	}
	/**
	 * Description 获取所有Category
	 * @return
	 * @throws APIException
	 */
	public List<String> getCategories() throws APIException{
		List<String> allCategories = new ArrayList<>();
		Command cmd = new Command("im", "viewfield");
		cmd.addSelection("Shared Category");
		Response res = mksCmdRunner.execute(cmd);
		WorkItem wi = res.getWorkItem("Shared Category");
		ItemList li = (ItemList)wi.getField("picks").getList();
		for(int i=0; i< li.size(); i++){
			Item item = (Item) li.get(i);
			String cate = item.getField("label").getValueAsString();
			allCategories.add(cate);
		}
		return allCategories;
	}

	/**
	 * Description 校验类型、状态是否正确
	 * @return
	 * @throws APIException
	 */
	public String checkIssueType(List<String> ids, String type, String checkState) throws APIException{
		if(ids == null || ids.isEmpty())
			return "";
		Command cmd = new Command("im", "issues");
		cmd.addOption(new Option("fields","Type,ID,State"));
		SelectionList list = new SelectionList();
		for(String id : ids){
			list.add(id);
		}
		cmd.setSelectionList(list);
		Response res = mksCmdRunner.execute(cmd);
		StringBuffer typeErrorMessage = new StringBuffer("");
		StringBuffer stateErrorMessage = new StringBuffer("");
		boolean typeError = false;
		boolean stateError = false;
		if (res != null) {
			WorkItemIterator it = res.getWorkItems();
			while (it.hasNext()) {
				WorkItem wi = it.next();
				String actualType = wi.getField("Type").getValueAsString();
				String actualState = wi.getField("State").getValueAsString();
				String id = wi.getId();
				if(!actualType.equals(type)){
					typeErrorMessage.append(id + ";");
					typeError = true;
				}else if(!actualState.equals(checkState)){
					stateErrorMessage.append(id + ";");
					stateError = true;
				}
			}
		}
		if(typeError)
			typeErrorMessage.append(" is not [ " + type + " ] Type. Please check it. \n");
		if(stateError)
			stateErrorMessage.append("is not in [" + checkState + " ] state. Please Check it");
		return typeErrorMessage.toString() + stateErrorMessage.toString();
	}

	/**
	 * 创建测试结果
	 * 02/19
	 * @param sessionID
	 * @param verdict
	 * @param observedResult
	 * @param annotation
	 * @param serverity
	 * @param reproducibility
	 * @param SWVersion
	 * @param HWVerdion
	 * @param caseID
	 * @return
	 */
	public boolean createResult(String sessionID,String verdict,String annotation,
								String caseID, Map<String, String> fieldValues){
		Command cmd = new Command("tm", "createresult");
		cmd.addOption(new Option("sessionID",sessionID));
		cmd.addOption(new Option("Verdict",verdict));
		if (annotation != null && !annotation.equals("")){
			cmd.addOption(new Option("annotation",annotation));
		}
		for(String field : fieldValues.keySet()){
			cmd.addOption(new Option("field",field + "=" + fieldValues.get(field)));
		}
		cmd.addSelection(caseID);
		try{
			Response res = mksCmdRunner.execute(cmd);
		}catch(APIException e){
			ImportApplicationUI.logger.error(APIExceptionUtil.getMsg(e));
			return false;
		}
		return true;
	}

	/**
	 * 编辑测试结果
	 * 02/19
	 * @param sessionID
	 * @param verdict
	 * @param observedResult
	 * @param annotation
	 * @param serverity
	 * @param reproducibility
	 * @param SWVersion
	 * @param HWVerdion
	 * @param caseID
	 * @return
	 */
	public boolean editResult(String sessionID,String verdict,String annotation,
							  String caseID, Map<String, String> fieldValues){
		Command cmd = new Command("tm", "editresult");
		cmd.addOption(new Option("sessionID",sessionID));
		cmd.addOption(new Option("Verdict",verdict));
		if (annotation != null && !annotation.equals("")){
			cmd.addOption(new Option("annotation",annotation));
		}
		for(String field : fieldValues.keySet()){
			cmd.addOption(new Option("field",field + "=" + fieldValues.get(field)));
		}
		cmd.addSelection(caseID);
		try{
			Response res = mksCmdRunner.execute(cmd);
		}catch(APIException e){
			ImportApplicationUI.logger.error(APIExceptionUtil.getMsg(e));
			return false;
		}
		return true;
	}

	public List<Map<String, String>> getResult(String caseID) throws APIException {
		List<Map<String, String>> result = null;
		Command cmd = new Command("tm", "results");
		cmd.addOption(new Option("caseID", caseID));
		List<String> fields = new ArrayList<>();
		fields.add("sessionID");
		MultiValue mv = new MultiValue();
		mv.setSeparator(",");
		for (String field : fields) {
			mv.add(field);
		}
		Option op = new Option("fields", mv);
		cmd.addOption(op);
		Response res = null;
		try {
			res = mksCmdRunner.execute(cmd);
			WorkItemIterator wk = res.getWorkItems();
			result = new ArrayList<>();
			while (wk.hasNext()) {
				Map<String, String> map = new HashMap<>();
				WorkItem wi = wk.next();
				for (String field : fields) {
					String value = wi.getField(field).getValueAsString();
					map.put(field, value);
				}
				result.add(map);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}



}
