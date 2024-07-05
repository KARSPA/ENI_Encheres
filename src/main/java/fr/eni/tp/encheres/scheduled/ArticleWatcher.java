package fr.eni.tp.encheres.scheduled;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.eni.tp.encheres.dal.ArticleDAO;

@Component
public class ArticleWatcher {
	
	private ArticleDAO articleDAO;
	
	public ArticleWatcher(ArticleDAO articleDAO) {
		this.articleDAO = articleDAO;
	}
	
	//@Scheduled(fixedDelay = 3000)
	public void printArticleCount() {
		int articleCount = articleDAO.countArticles();
		System.err.println("Il y a actuellement "+articleCount+" articles en base de donnée");
	}

}
