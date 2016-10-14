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

    private static final String EMAIL_VALIDATION_MESSAGE = "<!DOCTYPE html>\n" +
            "<html lang=\"en\" dir=\"ltr\" class=\"client-nojs\">\n" +
            "<head>\n" +
            "<meta charset=\"UTF-8\" />\n" +
            "<title>Splash!</title>\n" +
            "<body>\n" +
            "<p>%1$s, to confirm your email, please, <a href=\"http://188.225.39.117:8080/emailValidation?validCode=%2$s\">click here</a></p>" +
            "</body>\n" +
            "</html>";

    private static final String FORGOTEN_PASSWORD_MESSAGE = "<!DOCTYPE html>\n" +
            "<html lang=\"en\" dir=\"ltr\" class=\"client-nojs\">\n" +
            "<head>\n" +
            "<meta charset=\"UTF-8\" />\n" +
            "<title>Splash!</title>\n" +
            "<body>\n" +
            "<p><b>Login:</b> %1$s</p><br>\n" +
            "<p><b>Password:</b> %2$s</p>" +
            "</body>\n" +
            "</html>";

    private static EmailSender instance = new EmailSender();

    private EmailSender() {
    }

    public static EmailSender getInstance() {
        return instance;
    }

    public void send(String subject, String text, String fromEmail, String toEmail) {
        Thread sendEmailThread = new Thread() {

            @Override
            public void run() {
                Properties props;
                String userName = "stegapp777@gmail.com";
                String password = "vpered1973";

                props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                Session session = Session.getInstance(props, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(userName, password);
                    }
                });
                try {
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(userName));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                    message.setSubject(subject);
                    message.setText(text);
                    Transport.send(message);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }

            }

        };
        sendEmailThread.start();

    }

    public void sendValidationEmail(String profileId, String toEmail, String validCode) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                Properties props;
                String userName = "stegapp777@gmail.com";
                String password = "vpered1973";

                props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                Session session = Session.getInstance(props, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(userName, password);
                    }
                });
                try {
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(userName));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                    message.setSubject("Email Validation");
                    String emailMessage = String.format(EMAIL_VALIDATION_MESSAGE, profileId, validCode);
                    message.setContent(emailMessage ,"text/html; charset=utf-8");
                    Transport.send(message);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    public void sendForgotenPassword(String profileId, String email, String passwd){
        Thread thread = new Thread(){
            @Override
            public void run() {
                Properties props;
                String userName = "stegapp777@gmail.com";
                String password = "vpered1973";

                props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                Session session = Session.getInstance(props, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(userName, password);
                    }
                });
                try {
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(userName));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                    message.setSubject("User credentials");
                    String emailMessage = String.format(FORGOTEN_PASSWORD_MESSAGE, profileId, passwd);
                    message.setContent(emailMessage ,"text/html; charset=utf-8");
                    Transport.send(message);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

}
