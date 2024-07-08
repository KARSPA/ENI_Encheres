package fr.eni.tp.encheres.bll;

public interface PaperCutEmailService {
	
	void sendEmail(String to, String subject, String text);

}
