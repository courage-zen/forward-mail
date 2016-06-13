package zen.courage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import com.sun.mail.pop3.POP3Folder;

public class ForwardMail {

	public static int MAX_UNREAD_MESSAGE_COUNT = 99;
	private String pop3_host;
	private String smtp_host;
	private String username;
	private String password;
	private String forward_to;
	private String log_dir;
	private String save_filename;
	private boolean file_ok;
	private Date today;
	private HashSet<String> save_set;

	public boolean isFileOK() {
		return file_ok;
	}

	public ForwardMail(String pop3_host, String smtp_host, String username,
			String password, String forward_to, String log_dir) {
		super();
		this.pop3_host = pop3_host;
		this.smtp_host = smtp_host;
		this.username = username;
		this.password = password;
		this.forward_to = forward_to;
		if (log_dir.endsWith("/")) {
			this.log_dir = log_dir;
		} else {
			this.log_dir = log_dir + "/";
		}
		this.file_ok = false;
		this.save_set = new HashSet<String>();

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		this.today = cal.getTime();
		SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd");
		String today_string = format.format(this.today);
		this.save_filename = this.log_dir + "save_" + today_string;

		File d = new File(log_dir);
		if (d.exists() && d.isDirectory()) {
			fileReady(d, today_string);
			this.file_ok = true;
		} else {
			System.out.println(this.log_dir + "not exists!");
		}

	}

	private void saveToFile() throws IOException {
		System.out.println("save to:" + this.save_filename);
		FileOutputStream out = new FileOutputStream(this.save_filename);
		PrintStream p = new PrintStream(out);
		for (String id : this.save_set) {
			p.println(id);
		}
		p.close();
		out.close();
	}

	private void fileReady(File dir, String today_string) {
		String fileHead = "save_";

		// read save file to save set
		File saveFile = new File(this.save_filename);
		if (saveFile.exists()) {
			BufferedReader br;
			try {
				FileReader fr = new FileReader(this.save_filename);
				br = new BufferedReader(fr);
				String line = br.readLine();
				while (line != null) {
					save_set.add(line);
					line = br.readLine();
				}
				br.close();
				fr.close();
			} catch (FileNotFoundException e) {

			} catch (IOException e) {
				System.out.println("File read error!");
			}
		}

		// delete all old files
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (File f : files) {
				if (f.isFile()) {
					String fn = f.getName();
					if (fn.startsWith(fileHead) && !fn.endsWith(today_string)) {
						f.delete();
					}
				}
			}
		}
	}

	private Message getForward(Session session, Message message)
			throws MessagingException, IOException {

		Message forward = new MimeMessage(session);

		forward.setSubject(MimeUtility.encodeText(message.getSubject(),
				"UTF-8", "B"));

		forward.setFrom(message.getFrom()[0]);
		forward.addRecipient(Message.RecipientType.TO, new InternetAddress(
				this.forward_to));

		String cc = InternetAddress.toString(message
				.getRecipients(Message.RecipientType.CC));
//		String to_cc;
//		if (cc == null || cc.isEmpty()) {
//			to_cc = "\nTO:"
//					+ InternetAddress.toString(message
//							.getRecipients(Message.RecipientType.TO)) + "\n";
//		} else {
//			to_cc = "\nTO:"
//					+ InternetAddress.toString(message
//							.getRecipients(Message.RecipientType.TO)) + "\nCC:"
//					+ cc + "\n";
//		}
//		System.out.println(to_cc);

		if (message.isMimeType("text/plain")) {
			forward.setContent((String) message.getContent(),
					"text/plain");
		} else if (message.isMimeType("text/html")) {
			forward.setContent((String) message.getContent(),
					"text/html");
		} else if (message.isMimeType("multipart/*")) {
			Multipart multipart = (Multipart) message.getContent();
//			BodyPart bpart = new MimeBodyPart();
//			bpart.setContent(MimeUtility.encodeText(to_cc, "UTF-8", "B"),
//					"text/plain;charset=utf-8");
//			multipart.addBodyPart(bpart);
			forward.setContent(multipart, message.getContentType());
		}

		forward.saveChanges();

		return forward;
	}

	public void receiveAndForwardMail() throws MessagingException, IOException {
		Properties props = new Properties();

		props.put("mail.store.protocol", "pop3");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.host", this.smtp_host);
		props.put("mail.smtp.port", "25");
		Session session = Session.getDefaultInstance(props, null);
		Store store = session.getStore("pop3");
		store.connect(this.pop3_host, this.username, this.password);
		Folder folder = store.getFolder("INBOX");
		folder.open(Folder.READ_ONLY);

		if (folder instanceof POP3Folder) {
			POP3Folder inbox = (POP3Folder) folder;
			Message messages[] = inbox.getMessages();

			int UnreadMessageCount = inbox.getUnreadMessageCount();
			int messageCount = messages.length;
			if (messageCount > 0) {
				System.out.println("MessageCount=" + messages.length);
				System.out.println("UnreadMessageCount=" + UnreadMessageCount);

				Transport t = session.getTransport("smtp");
				try {
					t.connect(this.username, this.password);
					int i = 0;
					for (Message m : messages) {
						MimeMessage mimeMessage = (MimeMessage) m;
						Date sentDate = mimeMessage.getSentDate();

						// just find today's mail when there is too many mails
						if (UnreadMessageCount <= MAX_UNREAD_MESSAGE_COUNT
								|| sentDate.after(this.today)) {

							String uid = inbox.getUID(mimeMessage);
							if (this.save_set.contains(uid)) {
								System.out.println("exists UID:" + uid);
							} else {
								i++;
								System.out.println("(" + i + ")forward UID:"
										+ uid + "sentDate:" + sentDate);
								Message forward = getForward(session, m);
								t.sendMessage(forward,
										forward.getAllRecipients());
								this.save_set.add(uid);
							}

						} else {
							String uid = inbox.getUID(mimeMessage);
							System.out.println("unforward UID:" + uid
									+ "sentDate:" + sentDate);
						}
					}
					if (i > 0) {
						System.out.println("ForwardMessageCount=" + i);
						saveToFile();
					}
				} finally {
					t.close();
				}
			}
		}
	}

	public static void main(String args[]) throws Exception {
		// String pop3_host = "132.228.56.206";
		// String smtp_host = "132.228.56.136";

		if (args.length < 6) {
			System.out.println("参数：POP3的主机 SMTP的主机 邮件账号 邮件密码 需要转发的邮箱 本地日志目录");
			return;
		}
		String pop3_host = args[0];
		String smtp_host = args[1];
		String username = args[2];
		String password = args[3];
		String forward_to = args[4];
		String log_dir = args[5];

		ForwardMail fm = new ForwardMail(pop3_host, smtp_host, username,
				password, forward_to, log_dir);
		if (fm.isFileOK()) {
			fm.receiveAndForwardMail();
		}
	}
}
