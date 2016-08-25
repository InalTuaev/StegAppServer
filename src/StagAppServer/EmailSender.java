package StagAppServer;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {
	
	public void send(String subject, String text, String fromEmail, String toEmail) {
		Thread sendEmailThread = new Thread(){

			@Override
			public void run() {
				Properties props;
				String userName = "stegapp777@gmail.com";
				String password = "vpered1973";
				
				props = new Properties();
//				props.put("mail.smtp.host", "smtp.gmail.com");
//				props.put("mail.smtp.socketFactory.port", "465");
//				props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
				props.put("mail.smtp.auth", "true");
				props.put("mail.smtp.starttls.enable", "true");
				props.put("mail.smtp.host", "smtp.gmail.com");
				props.put("mail.smtp.port", "587");
				
				Session session = Session.getInstance(props, new Authenticator(){
					protected PasswordAuthentication getPasswordAuthentication(){
						return new PasswordAuthentication(userName, password);
					}
				});
				try{
					Message message = new MimeMessage(session);
					message.setFrom(new InternetAddress(userName));
					message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
					message.setSubject(subject);
					message.setText(text);
					Transport.send(message);
				} catch(MessagingException e) {
					throw new RuntimeException(e);
				}
				System.out.println("plea oops");
				
			}
			
		};
		sendEmailThread.start();
		
	}

}
