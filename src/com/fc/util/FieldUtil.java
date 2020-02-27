package com.fc.util;

//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//
//import com.mks.api.Option;
//import com.mks.api.response.APIException;
//import com.mks.api.response.Field;
//import com.mks.api.response.Item;
//import com.mks.api.response.ItemList;
//import com.mks.api.response.Response;
//import com.mks.api.response.WorkItem;
//import com.mks.api.response.WorkItemIterator;

public class FieldUtil {

//	private static Log log = LogFactory.getLog(FieldUtil.class);
//
//	@Autowired
//	private Connection conn;
//
//	private static Map<String, List<String>> categories;
//
//	public synchronized Map<String, List<String>> getCategories() throws APIException {
//		if (categories == null || categories.isEmpty()) {
//			categories = categories();
//		}
//		return categories;
//	}
//
//	public String findAttachmentField(String fieldName) throws APIException {
//		Command cmd = new Command(Command.IM, "viewfield");
//		cmd.addSelection(fieldName);
//		Response res = conn.execute(cmd);
//		WorkItem wi = res.getWorkItem(fieldName);
//		String field = null;
//		try {
//			Field attachmentField = wi.getField("defaultattachmentfield");
//			if(attachmentField != null) {
//				field = attachmentField.getValueAsString();
//			}
//		} catch (Exception e) {
//			log.debug(e.getMessage());
//			if("Text".equalsIgnoreCase(fieldName)) {
//				return "Text Attachments";
//			}
//			if("fva".equalsIgnoreCase(wi.getField("type").getValueAsString())) {
//				Field backedBy = wi.getField("backedBy");
//				Item backedItem = backedBy.getItem();
//				String backedID = backedItem.getId();
//				field = backedID.substring(backedID.indexOf(".") + 1);
//			}
//		}
//		return field;
//	}
//
//	public List<String> getAllPickValues(String field) throws APIException {
//		List<String> visiblePicks = new ArrayList<>();
//		Command cmd = new Command("im", "fields");
//		cmd.addOption(new Option("noAsAdmin"));
//		cmd.addOption(new Option("fields", "picks"));
//		cmd.addSelection(field);
//		Response res = conn.execute(cmd);
//		if (res != null) {
//			WorkItem wi = res.getWorkItem(field);
//			if (wi != null) {
//				Field picks = wi.getField("picks");
//				ItemList itemList = (ItemList) picks.getList();
//				if (itemList != null) {
//					for (int i = 0; i < itemList.size(); i++) {
//						Item item = (Item) itemList.get(i);
//						String visiblePick = item.getId();
//						if (!visiblePicks.contains(visiblePick)) {
//							visiblePicks.add(visiblePick);
//						}
//					}
//				}
//			}
//		}
//		return visiblePicks;
//	}
//
//	public List<String> getActivePickValues(String field) throws APIException {
//		List<String> visiblePicks = new ArrayList<>();
//		Command cmd = new Command("im", "viewfield");
//		cmd.addSelection(field);
//		Response res = conn.execute(cmd);
//		if (res != null) {
//			WorkItem wi = res.getWorkItem(field);
//			if (wi != null) {
//				Field picks = wi.getField("picks");
//				ItemList itemList = (ItemList) picks.getList();
//				if (itemList != null) {
//					for (int i = 0; i < itemList.size(); i++) {
//						Item item = (Item) itemList.get(i);
//						String pickName = item.getId();
//						Field attribute = item.getField("active");
//						if (attribute != null && attribute.getValueAsString().equalsIgnoreCase("true")
//								&& !visiblePicks.contains(pickName)) {
//							visiblePicks.add(pickName);
//						}
//					}
//				}
//			}
//		}
//		return visiblePicks;
//	}
//
//	public List<PickVO> getPicks(String field) throws APIException {
//		List<PickVO> pickList = new ArrayList<>();
//		Command cmd = new Command("im", "viewfield");
//		cmd.addSelection(field);
//		Response res = conn.execute(cmd);
//		if (res != null) {
//			WorkItem wi = res.getWorkItem(field);
//			if (wi != null) {
//				Field picks = wi.getField("picks");
//				ItemList itemList = (ItemList) picks.getList();
//				if (itemList != null) {
//					for (int i = 0; i < itemList.size(); i++) {
//						Item item = (Item) itemList.get(i);
//						Field attribute = item.getField("active");
//						if (attribute != null && attribute.getBoolean()) {
//							PickVO pick = new PickVO();
//							pick.setName(item.getDisplayId());
//							pick.setValue(item.getId());
//							pickList.add(pick);
//						}
//					}
//				}
//			}
//		}
//		return pickList;
//	}
//
//	public List<PickVO> viewIbplByFields(String field, Map<String, String> relatedFields) throws APIException {
//		long start = System.currentTimeMillis();
//		Command cmd = new Command(Command.IM, "viewfield");
//		cmd.addSelection(field);
//		Response res = conn.execute(cmd);
//		WorkItem wi = res.getWorkItem(field);
//		Field type = wi.getField("backingtype");
//		Field backtext = wi.getField("backingtextformat");
//		String text = backtext.getString();
//		Pattern reg = Pattern.compile("\\{.*?\\}", Pattern.CASE_INSENSITIVE);
//		Matcher m = reg.matcher(text);
//		StringBuilder fields = new StringBuilder();
//		fields.append("ID");
//		while (m.find()) {
//			fields.append(",");
//			String fieldName = m.group();
//			fields.append(fieldName.substring(1, fieldName.length() - 1));
//		}
//		Field backstate = wi.getField("backingstates");
//		StringBuilder sb = new StringBuilder();
//		sb.append("(field[Type]=").append(type.getString()).append(")");
//		if(backstate.getValue() != null) {
//			sb.append(" and (field[State]=").append(backstate.getString().replace(", ", ",")).append(")");
//		}
//		if(relatedFields != null) {
//			for(String key : relatedFields.keySet()) {
//				sb.append(" and (field[").append(key).append("]=").append(relatedFields.get(key)).append(")");
//			}
//		} else {
//			sb.append(" and (field[ID]=0)");
//		}
//		log.info("viewIbplFields: " + (System.currentTimeMillis() - start));
//		List<PickVO> picks = findPicksByIbplField(fields.toString(), sb.toString());
//		return picks;
//	}
//
//	public List<PickVO> viewIbplByField(String field, String relatedField, String relatedValue) throws APIException {
//		Command cmd = new Command(Command.IM, "viewfield");
//		cmd.addSelection(field);
//		Response res = conn.execute(cmd);
//		WorkItem wi = res.getWorkItem(field);
//		Field type = wi.getField("backingtype");
//		Field backtext = wi.getField("backingtextformat");
//		String text = backtext.getString();
//		Pattern reg = Pattern.compile("\\{.*?\\}", Pattern.CASE_INSENSITIVE);
//		Matcher m = reg.matcher(text);
//		StringBuilder fields = new StringBuilder();
//		fields.append("ID");
//		while (m.find()) {
//			fields.append(",");
//			String fieldName = m.group();
//			fields.append(fieldName.substring(1, fieldName.length() - 1));
//		}
//
//		Field backstate = wi.getField("backingstates");
//		StringBuilder sb = new StringBuilder();
//		sb.append("(field[Type]=").append(type.getString()).append(")");
//		if(backstate.getValue() != null) {
//			sb.append(" and (field[State]=").append(backstate.getString().replace(", ", ",")).append(")");
//		}
//		sb.append(" and (field[").append(relatedField).append("]=").append(relatedValue).append(")");
//		List<PickVO> picks = findPicksByIbplField(fields.toString(), sb.toString());
//		return picks;
//	}
//
//	public List<PickVO> viewIbpl(String fieldName) throws APIException {
//		return viewIbpl(fieldName, null);
//	}
//
//	public List<PickVO> viewIbpl(String fieldName, String condition) throws APIException {
//		Command cmd = new Command(Command.IM, "viewfield");
//		cmd.addSelection(fieldName);
//		Response res = conn.execute(cmd);
//		WorkItem wi = res.getWorkItem(fieldName);
//		Field type = wi.getField("backingtype");
//		Field backtext = wi.getField("backingtextformat");
//		String text = backtext.getString();
//		Pattern reg = Pattern.compile("\\{.*?\\}", Pattern.CASE_INSENSITIVE);
//		Matcher m = reg.matcher(text);
//		StringBuilder fields = new StringBuilder();
//		while (m.find()) {
//			fields.append(",");
//			String group = m.group();
//			fields.append(group.substring(1, group.length() - 1));
//		}
//		Field backstate = wi.getField("backingstates");
//		StringBuilder sb = new StringBuilder();
//		sb.append("(field[Type]=").append(type.getString()).append(")");
//		if(backstate.getValue() != null) {
//			sb.append(" and (field[State]=").append(backstate.getString().replace(", ", ",")).append(")");
//		}
//		if(condition != null && !condition.trim().isEmpty()) {
//			sb.append(" and (field[").append(text).append("]=").append(condition).append(")");
//		}
//		List<PickVO> picks = findPicksByIbplField(fields.toString(), sb.toString());
//		return picks;
//	}
//
//	/**
//	 * item back picklist ibpl 
//	 * 下拉框联动；
//	 * @author vxot
//	 * @param field
//	 * @param query
//	 * @return
//	 * @throws APIException
//	 * 2017年10月16日
//	 */
//	public List<PickVO> findPicksByIbplField(String field, String query) throws APIException {
//		long start = System.currentTimeMillis();
//		Command command = new Command(Command.IM, "issues");
//		command.addOption(new Option("fields", field));
//		Option option = new Option("queryDefinition", "(" + query + ")");
//		command.addOption(option);
//		Response res = conn.execute(command);
//		WorkItemIterator workItems = res.getWorkItems();
//		List<PickVO> picks = new ArrayList<PickVO>();
//		while(workItems.hasNext()) {
//			WorkItem wi = workItems.next();
//			Iterator<?> it = wi.getFields();
//			PickVO pick = new PickVO();
//			StringBuffer sb = new StringBuffer();
//			while(it.hasNext()) {
//				Field pickName = (Field) it.next();
//				if("ID".equalsIgnoreCase(pickName.getName())) {
//					pick.setValue(pickName.getValueAsString());
//				} else {
//					sb.append(",");
//					sb.append(pickName.getValueAsString());
//				}
//			}
//			pick.setName(sb.substring(1));
//			picks.add(pick);
//		}
//		log.info("findPicksByIpblField: " + (System.currentTimeMillis() - start));
//		return picks;
//	}
//
//	public List<FieldVO> viewFields(List<String> fields) throws APIException {
//		List<FieldVO> vos = new ArrayList<>();
//		if(fields == null || fields.isEmpty()) {
//			return vos;
//		}
//		SelectionList sl = new SelectionList();
//		for(String field : fields) {
//			sl.add(field);
//		}
//		Command cmd = new Command(Command.IM, "fields");
//		cmd.addOption(new Option("fields", "name,displayName,picks,type"));
//		cmd.setSelectionList(sl);
//		Response res = conn.execute(cmd);
//		WorkItemIterator wit = res.getWorkItems();
//		while(wit.hasNext()) {
//			WorkItem wi = wit.next();
//			FieldVO vo = new FieldVO();
//			Iterator<?> it = wi.getFields();
//			while(it.hasNext()) {
//				Field field = (Field) it.next();
//				switch(field.getName()) {
//				case "name":
//					vo.setName(field.getValueAsString());
//					break;
//				case "displayName":
//					vo.setDisplayName(field.getValueAsString());
//					break;
//				case "type":
//					vo.setType(field.getValueAsString());
//					break;
//				case "picks":
//					List<PickVO> picklist = new ArrayList<>();
//					ItemList il = (ItemList) field.getList();
//					for(int i=0; i<il.size(); i++) {
//						Item item = (Item) il.get(i);
//						Field label = item.getField("label");
//						Field value = item.getField("value");
//						PickVO pick = new PickVO();
//						pick.setName(label.getValueAsString());
//						pick.setValue(value.getValueAsString());
//						picklist.add(pick);
//					}
//					vo.setPicklist(picklist);
//					break;
//				default:
//				}
//			}
//			vos.add(vo);
//		}
//		return vos;
//	}
//
//	public List<PickVO> getAllowedTypes(String fieldName, String currentType) throws APIException {
//		Command command = new Command(Command.IM, "viewfield");
//		command.addSelection(fieldName);
//		Response res = conn.execute(command);
//		WorkItem wi = res.getWorkItem(fieldName);
//		String type = wi.getField("type").getValueAsString();
//		List<PickVO> list = new ArrayList<PickVO>();
//		if("relationship".equalsIgnoreCase(type)) {
//			ItemList il = (ItemList) wi.getField("allowedtypes").getList();
//			Item item = il.getItem(currentType);
//			if(item != null) {
//				ItemList itemlist = (ItemList) item.getField("to").getList();
//				for (int i = 0; i < itemlist.size(); i++) {
//					PickVO pick = new PickVO();
//					Item to = (Item) itemlist.get(i);
//					pick.setName(to.getId());
//					pick.setValue(to.getId());
//					list.add(pick);
//				}
//			}
//		}
//		return list;
//	}
//
//	public String getFieldById(String id, String field) throws APIException {
//		Command command = new Command(Command.IM, "issues");
//		command.addOption(new Option("fields", field));
//		command.addSelection(id);
//		Response res = conn.execute(command);
//		WorkItem wi = res.getWorkItem(id);
//		String fieldValue = wi.getField(field).getItem().getId();
//		return fieldValue;
//	}
//
//	public String getTypeById(String id) throws APIException {
//		return getFieldById(id, "Type");
//	}
//
//	public String getProjectById(String id) throws APIException {
//		return getFieldById(id, "Project");
//	}
//
//	private Map<String, List<String>> categories() throws APIException {
//		Map<String, List<String>> map = new HashMap<>();
//		if (conn == null) {
//			log.warn("invoke categories() ----- connection is null.");
//			throw new APIException("invoke categories() ----- connection is null.");
//		}
//		Command command = new Command(Command.IM, Constants.VIEW_FIELD);
//		command.addSelection(Constants.SHARED_CATEGORY);
//		Response res = conn.execute(command);
//		WorkItem wi = res.getWorkItem(Constants.SHARED_CATEGORY);
//		ItemList il = (ItemList) wi.getField(Constants.PICKS).getList();
//		List<String> segments = new ArrayList<String>();
//		List<String> meaningfuls = new ArrayList<String>();
//		for (int i = 0; i < il.size(); i++) {
//			Item item = (Item) il.get(i);
//			String label = item.getField(Constants.LABEL).getValueAsString();
//			String phase = item.getField(Constants.PHASE).getValueAsString();
//			if (Constants.MEANINGFUL.equals(phase)) {
//				meaningfuls.add(label);
//			} else if (Constants.SEGMENT.equals(phase)) {
//				segments.add(label);
//			}
//		}
//		map.put(Constants.SEGMENT, segments);
//		map.put(Constants.MEANINGFUL, meaningfuls);
//		return map;
//	}

}
