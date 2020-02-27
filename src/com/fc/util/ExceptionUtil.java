package com.fc.util;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import com.mks.api.response.APIException;
import com.mks.api.response.Response;
import com.mks.api.response.WorkItemIterator;

public class ExceptionUtil {

	private static Logger logger = Logger.getLogger(ExceptionUtil.class.getName());
	public static String catchException(APIException e) {
		String message = e.getMessage();
		if (message == null || message.isEmpty() || message.equals("null")) {
			Response res = e.getResponse();
			WorkItemIterator wk = res.getWorkItems();
			try {
				while (wk.hasNext()) {
					APIException e1 = wk.next().getAPIException();
					if (e1 != null) {
						message = e1.getMessage();
						if (!message.isEmpty() && !message.equals("null")) {
							break;
						}
					}
				}
			} catch (APIException e1) {
				message = e1.getMessage();
			} finally {
				String me = "Failed! Because of " + message;
				if (message == null || message.equals("null")) {
					me = "Failed!";
				}
				message = me;
				JOptionPane.showMessageDialog(null, me, "Failed", JOptionPane.WARNING_MESSAGE);
				logger.info("====  Failed : " + message);
			}
		} else {
			JOptionPane.showMessageDialog(null, "Failed! Because of " + message, "Failed",
					JOptionPane.WARNING_MESSAGE);
			logger.info("====  Failed : " + message);
		}
		return message;
	}
}
