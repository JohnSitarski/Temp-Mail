package com.jsitarski.net.tempmail;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.jsitarski.net.http.HttpRequest;
import com.jsitarski.net.tempmail.model.Email;

/**
 * This class wraps the web api provided by https://temp-mail.org/en/api
 */
public class TempMail {

	public static final String DELETE_MESSAGE_LINK = "http://api.temp-mail.ru/request/delete/id/md5/",
			VIEW_MESSAGE_LINK = "http://api.temp-mail.ru/request/mail/id/md5/",

			DOMAIN_LIST_LINK = "http://api.temp-mail.ru/request/domains";

	/**
	 * @return Returns a list of all the emails in the inbox of the address.
	 */
	public static List<Email> getEmails(final String md5hash)
			throws IOException, ParserConfigurationException, SAXException {
		if (md5hash == null || md5hash.length() != 32)
			throw new IllegalArgumentException("Argument provided not a md5 hash!");
		final ArrayList<Email> EMAIL_LIST = new ArrayList<Email>();
		final HttpRequest HTTP_REQUEST = new HttpRequest(VIEW_MESSAGE_LINK.replace("md5", md5hash));
		// must accept xml else server will reject request :)
		HTTP_REQUEST.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		try {
			HTTP_REQUEST.execute();
		} catch (FileNotFoundException fnfe) {
			// fnfe.printStackTrace();
			return EMAIL_LIST;
		}
		System.out.println(VIEW_MESSAGE_LINK.replace("md5", md5hash));

		final String OUTPUT = HTTP_REQUEST.getOutput();// our input
		final DocumentBuilderFactory FACTORY = DocumentBuilderFactory.newInstance();
		final DocumentBuilder BUILDER = FACTORY.newDocumentBuilder();
		final ByteArrayInputStream XML_INPUT = new ByteArrayInputStream(OUTPUT.getBytes("UTF-8"));
		final Document document = BUILDER.parse(XML_INPUT);

		final NodeList NODE_LIST = document.getElementsByTagName("item");

		for (int index = 0; index < NODE_LIST.getLength(); index++) {
			final Element EMAIL_NODE = (Element) NODE_LIST.item(index);
			final Email EMAIL = new Email();
			final NodeList EMAIL_ELEMENT_LIST = EMAIL_NODE.getChildNodes();
			// was going to use reflection to get fields from the email class
			// and compare then set their value but performance loss to great.
			for (int i = 0; i < EMAIL_ELEMENT_LIST.getLength(); i++) {
				final Node node = EMAIL_ELEMENT_LIST.item(i);
				final String NODE_NAME = EMAIL_ELEMENT_LIST.item(i).getNodeName();
				switch (NODE_NAME) {

				case "mail_id":
					EMAIL.setMailID(node.getTextContent());
					break;

				case "mail_address_id":
					EMAIL.setMailAddressID(node.getTextContent());
					break;

				case "mail_from":
					EMAIL.setMailFrom(node.getTextContent());
					break;

				case "mail_subject":
					EMAIL.setMailSubject(node.getTextContent());
					break;

				case "mail_preview":
					EMAIL.setMailPreview(node.getTextContent());
					break;

				case "mail_text_only":
					EMAIL.setMailTextOnly(node.getTextContent());
					break;

				case "mail_text":
					EMAIL.setMailText(node.getTextContent());
					break;

				case "mail_html":
					EMAIL.setMailHtml(node.getTextContent());
					break;

				case "mail_timestamp":
					EMAIL.setMailTimestamp(Double.parseDouble(node.getTextContent()));
					break;

				default:// unknown field
					break;
				}
			}
			if (EMAIL.getMailID() != null)
				EMAIL_LIST.add(EMAIL);
		}
		return EMAIL_LIST;
	}

	public static void main(String[] args)
			throws IOException, ParserConfigurationException, SAXException, NoSuchAlgorithmException {
		// example usage
		List<String> domains = TempMail.getDomains();
		String emailAddress = "billgates" + domains.get(0);
		// file not found thrown when 0 emails in inbox also
		List<Email> emails = TempMail.getEmails(TempMail.getMD5Hash(emailAddress));
		System.out.println("We have x emails: " + emails.size());

	}

	public static void deleteEmail(final String md5hash) throws IOException {
		if (md5hash == null || md5hash.length() != 32)
			throw new IllegalArgumentException("Argument provided not a md5 hash!");
		final HttpRequest HR = new HttpRequest(DELETE_MESSAGE_LINK.replace("md5", md5hash));
		HR.execute();

	}

	public static String getMD5Hash(final String input) throws NoSuchAlgorithmException {
		final MessageDigest md = MessageDigest.getInstance("MD5");
		final byte[] bytes = md.digest(input.getBytes());
		String result = "";
		for (int i = 0; i < bytes.length; ++i) {
			result += Integer.toHexString((bytes[i] & 0xFF) | 0x100).substring(1, 3);
		}
		return result;
	}

	/**
	 * @return Returns a list of all the valid domain names for emails on the
	 *         temp-mail service.
	 */
	public static List<String> getDomains() throws IOException, ParserConfigurationException, SAXException {
		final ArrayList<String> LIST = new ArrayList<String>();
		final HttpRequest HR = new HttpRequest(DOMAIN_LIST_LINK);
		HR.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		HR.execute();
		final String OUTPUT = HR.getOutput();// our input
		final DocumentBuilderFactory FACTORY = DocumentBuilderFactory.newInstance();
		final DocumentBuilder BUILDER = FACTORY.newDocumentBuilder();
		final ByteArrayInputStream XML_INPUT = new ByteArrayInputStream(OUTPUT.getBytes("UTF-8"));
		final Document document = BUILDER.parse(XML_INPUT);
		final NodeList NODE_LIST = document.getElementsByTagName("item");
		for (int index = 0; index < NODE_LIST.getLength(); index++) {
			final Node node = NODE_LIST.item(index);
			LIST.add(node.getTextContent());
		}
		return LIST;
	}

}
