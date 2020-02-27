package com.fc.util;

import com.mks.api.response.APIException;
import com.mks.api.response.Response;
import com.mks.api.response.WorkItem;
import com.mks.api.response.WorkItemIterator;

public class APIExceptionUtil {

	public static String getMsg(APIException e) {
		String msg = e.getMessage();
		Response res = e.getResponse();
		if (res != null) {
			WorkItemIterator wit = res.getWorkItems();
			try {
				while (wit.hasNext()) {
					wit.next();
				}
			} catch (APIException e1) {
				String message = e1.getMessage();
				if (message != null) {
					msg = message;
				}
			}
		}
		return msg;
	}

	public static String getResult(Response res) {
		try {
			if (res.getResult() != null) {
				return res.getResult().getMessage();
			}
			WorkItemIterator wit = res.getWorkItems();
			while (wit.hasNext()) {
				WorkItem wi = wit.next();
				if (wi.getResult() != null) {
					return wi.getResult().getMessage();
				}
			}
			return null;
		} catch (Exception e) {
			return "";
		}
	}
}
