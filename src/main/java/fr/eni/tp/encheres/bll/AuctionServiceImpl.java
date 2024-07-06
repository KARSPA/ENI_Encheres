package fr.eni.tp.encheres.bll;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.eni.tp.encheres.bo.Article;
import fr.eni.tp.encheres.bo.ArticleState;
import fr.eni.tp.encheres.bo.Auction;
import fr.eni.tp.encheres.bo.Category;
import fr.eni.tp.encheres.bo.PickupLocation;
import fr.eni.tp.encheres.bo.User;
import fr.eni.tp.encheres.dal.ArticleDAO;
import fr.eni.tp.encheres.dal.AuctionDAO;
import fr.eni.tp.encheres.dal.CategoryDAO;
import fr.eni.tp.encheres.dal.PickUpLocationDAO;
import fr.eni.tp.encheres.dal.UserDAO;
import fr.eni.tp.encheres.exception.BusinessException;

@Service
public class AuctionServiceImpl implements AuctionService {

	private AuctionDAO auctionDAO;
	private CategoryDAO categoryDAO;
	private ArticleDAO articleDAO;
	private PickUpLocationDAO pickUpLocationDAO;
	private UserDAO userDAO;

	public AuctionServiceImpl(AuctionDAO auctionDAO, CategoryDAO categoryDAO, ArticleDAO articleDAO,
			PickUpLocationDAO pickUpLocationDAO, UserDAO userDAO) {
		this.auctionDAO = auctionDAO;
		this.categoryDAO = categoryDAO;
		this.articleDAO = articleDAO;
		this.pickUpLocationDAO = pickUpLocationDAO;
		this.userDAO = userDAO;

	}

	@Override
	public Article findArticleById(int articleId) {
		return articleDAO.read(articleId);
	}

	@Override
	public List<Article> findArticlesByName(String name) {
		return articleDAO.findByName(name);
	}

	@Override
	public List<Article> findArticlesByCategory(Category category) {
		return articleDAO.findByCategory(category.getCategoryId());
	}

	@Override
	public List<Article> findArticlesByCategoryAndName(Category category, String name) {
		return articleDAO.findByCategoryAndName(category.getCategoryId(), name);

	}

	@Override
	public List<Article> findArticles() {
		return articleDAO.findAll();
	}
	
	
	@Override
	public List<Article> selectArticlesBis(Article article, HashMap<String, Boolean> filters, String buyOrSale, int userId){
		//Récupérer les paramètres de filtre :
		String textFilter = article.getArticleName();
		int categoryId = article.getCategory().getCategoryId();
		System.out.println("userIdService : " +userId);
		// Construire la requête à envoyée en fonction des filtres
		String SQLQuery = "";

		//Corps commun de requête
		SQLQuery = SQLQuery.concat("SELECT DISTINCT av.no_article, nom_article, description, date_debut_encheres, date_fin_encheres, prix_initial, prix_vente, av.no_utilisateur, no_categorie, no_acheteur, etat_vente FROM ARTICLES_VENDUS av LEFT JOIN ENCHERES en ON av.no_article = en.no_article");
		
		//Permettra de savoir si il faudra enlever le OR ou AND à la fin de la requête.
		boolean containsConditions = false;
		
		
		//Si filters.containsValue(true) OU un categoryId est sélectionné OU le texte a recherché est vide, on ajoute WHERE
		if(filters.containsValue(true) || categoryId!=0 || !textFilter.isBlank()) {
			containsConditions = true;
			
			SQLQuery = SQLQuery.concat(" WHERE");
			
			//Ajout du filtre de catégorie
			if(categoryId!=0) {
				SQLQuery = SQLQuery.concat(" (no_categorie = :categoryId) AND");
			}
			//Ajout du filtre dans le nom
			if(!textFilter.isBlank()) {
				SQLQuery = SQLQuery.concat(" (nom_article LIKE '%:textFilter%' OR description LIKE '%:textFilter%') AND");
			}
			
			// On boucle à travers filters
			String[] filterSQL = {""}; // Astuce pour faire passer une variable dans une forEach ...
			filters.forEach((key, value)->{
				// on ajoute la condition de requête si la value est true !
				String conditionSQL = value ? mapToSQLCondition(key) : "";
				filterSQL[0] = filterSQL[0].concat(conditionSQL);
			});
			SQLQuery = SQLQuery.concat(filterSQL[0]);
		}
		
		//Il y aura un AND ou OR en trop si true
		if(containsConditions) {
			
			//Si le dernier caractère est un R, alors il y a un OR en trop
			if(SQLQuery.charAt(SQLQuery.length()-1)=='R') {
				SQLQuery = SQLQuery.substring(0, SQLQuery.length()-2);
				
			// Sinon c'est qu'il y a un AND en trop
			}else if(SQLQuery.charAt(SQLQuery.length()-1)=='D') {
				SQLQuery = SQLQuery.substring(0, SQLQuery.length()-3);
			}
			
		}
		//Appeler articleDAO avec la requête en paramètre
		List<Article> articleList = articleDAO.findWithFilters(SQLQuery, userId, categoryId, textFilter);

		
		return articleList;
			
	}
		
		
	private String mapToSQLCondition(String conditionTitle) {
		String conditionSQL = "";
		switch(conditionTitle) {
			case "open":
				conditionSQL = " (etat_vente = 2) OR";
				break;
			case "current":
				conditionSQL = " (etat_vente = 2 AND av.no_utilisateur = :userId) OR";
				break;
			case "won":
				conditionSQL = " (etat_vente IN (3,4) AND no_acheteur = :userId) OR";
				break;
			case "currentVente":
				conditionSQL = " (etat_vente = 2 AND av.no_utilisateur = :userId) OR";
				break;
			case "notstarted":
				conditionSQL = " (etat_vente = 1 AND av.no_utilisateur = :userId) OR";
				break;
			case "finished":
				conditionSQL = " (etat_vente IN (3,4) AND av.no_utilisateur = :userId) OR";
				break;
		}
		return conditionSQL;
	}
		
		
	

	@Override
	public List<Article> selectArticles(Article article, User user, boolean open, boolean current, boolean won,
			boolean currentVente, boolean notstarted, boolean finished, String buySale) {
		List<Article> articlesList;

		// si pas de mot dans l'input et pas de catégorie choisie
		if (article == null || (article.getArticleName().isEmpty()
				&& (article.getCategory() == null || article.getCategory().getCategoryId() == 0))) {
			articlesList = this.findArticles();
		} else if (article.getArticleName().isEmpty()) { // si pas de mot dans l'input
			articlesList = this.findArticlesByCategory(article.getCategory());
		} else if (article.getCategory() == null || article.getCategory().getCategoryId() == 0) { 	// si pas de catégorie
																									// choisie
			articlesList = this.findArticlesByName(article.getArticleName());
		} else {
			articlesList = this.findArticlesByCategoryAndName(article.getCategory(), article.getArticleName());
		}
		if (user != null && user.getUserId() != 0) {
			if (buySale != null && buySale.equals("sales")) {
				List<Article> tmpArticlesList = articlesList.stream()
						.filter(art -> art.getSeller().getUserId() == user.getUserId()).collect(Collectors.toList());

				LocalDateTime now = LocalDateTime.now();
				articlesList.clear();
				for (Article art : tmpArticlesList) {
					if (currentVente && art.getAuctionStartDate().isBefore(now)
							&& art.getAuctionEndDate().isAfter(now)) {
						articlesList.add(art);
					}
					if (notstarted && art.getAuctionStartDate().isAfter(now)) {
						articlesList.add(art);
					}
					if (finished && art.getAuctionEndDate().isBefore(now)) {
						articlesList.add(art);
					}
				}
			} else if (buySale != null && buySale.equals("purchases")) {
				List<Auction> auctionsList = this.findAuctionsByUser(user.getUserId());
				List<Article> tmpArticlesList = new ArrayList<Article>();
				LocalDateTime now = LocalDateTime.now();
				if (open) {
					for (Article art : articlesList) {
						if (art.getAuctionStartDate().isBefore(now) && art.getAuctionEndDate().isAfter(now)) {
							tmpArticlesList.add(art);
						}
					}
				}
				for (Article art : articlesList) {
					for (Auction auct : auctionsList) {
						if (art.getArticleId() == auct.getArticle().getArticleId()) {
							if (current && !open && art.getAuctionStartDate().isBefore(now)
									&& art.getAuctionEndDate().isAfter(now)) {
								tmpArticlesList.add(art);
							}
							if (won && art.getAuctionEndDate().isBefore(now)) {
								tmpArticlesList.add(art);
							}
						}

					}
				}
				articlesList.clear();
				articlesList = tmpArticlesList;
				// System.out.println(articlesList);
			}
		}
		/*
		 * Sur cette partie, il reste à :
		 * ne sélectionner que les enchères gagnées (pour l'instant on sélectionne toutes les enchères finies auxquelles l'utilisateur a participé. 
		 * il y a un risque de doublon si l'utilisateur a posé plusieurs enchères sur 1 article.
		 */
		return articlesList;
	}

	@Override
	@Transactional(rollbackFor = BusinessException.class)
	public void sell(Article article) throws BusinessException {
		BusinessException be = new BusinessException();
		boolean isValid = false;
		isValid = checkDates(article.getAuctionStartDate(), article.getAuctionEndDate(), be)
				&& checkPickUpLocation(article.getPickupLocation(), be);

		if (isValid) {
			try {
				articleDAO.create(article);
				pickUpLocationDAO.create(article.getPickupLocation(), article.getArticleId());

			} catch (DataAccessException e) {
				e.printStackTrace();
				be.add("Un problème est survenu lors de l'accès à la base de données");
				throw be;
			}
		} else {
			throw be;
		}
	}

	private boolean checkDates(LocalDateTime startDate, LocalDateTime endDate, BusinessException be) {
		boolean isValid = false;
		// On enlève 2 minutes pour se laisser le temps du traitement.
		LocalDateTime now = LocalDateTime.now().minusMinutes(2);
		if (startDate == null || endDate == null) {
			be.add("Vente impossible. Les dates de début et de fin d'enchères doivent être renseignées");
		} else if (startDate.isBefore(now)) {
			be.add("Vente impossible. Les enchères ne peuvent pas commencer avant maintenant");
		} else if (startDate.isAfter(endDate)) {
			be.add("Vente impossible. Les enchères doivent finir après avoir commencé");
		} else {
			isValid = true;
		}
		return isValid;
	}

	private boolean checkPickUpLocation(PickupLocation pickupLocation, BusinessException be) {
		boolean isValid = false;
		if (pickupLocation.getStreet().isEmpty() || pickupLocation.getZipCode().isEmpty()
				|| pickupLocation.getCity().isEmpty()) {
			be.add("Vente impossible. Remplissez tous les champs du lieu de retrait de l'article");
		} else {
			isValid = true;
		}
		return isValid;
	}

	@Override
	public void deleteArticle(int articleId) {
		articleDAO.delete(articleId);
	}

	@Override
	@Transactional(rollbackFor = BusinessException.class)
	public void updateArticle(Article article) throws BusinessException {
		BusinessException be = new BusinessException();
		boolean isValid = false;
		isValid = checkDates(article.getAuctionStartDate(), article.getAuctionEndDate(), be)
				&& checkPickUpLocation(article.getPickupLocation(), be);

		if (isValid) {
			try {
				articleDAO.updateArticle(article);
				pickUpLocationDAO.updatePickUpLocationByArticleId(article.getArticleId(), article.getPickupLocation());

			} catch (DataAccessException e) {
				e.printStackTrace();
				be.add("Un problème est survenu lors de l'accès à la base de données");
				throw be;
			}
		} else {
			throw be;
		}
	}

	@Override
	public Category findCategoryById(int categoryId) {
		Category category = categoryDAO.readById(categoryId);
		return category;
	}

	@Override
	public List<Category> findCategories() {
		List<Category> categories = categoryDAO.findAll();
		return categories;
	}

	@Override
	public void newCategory(Category category) {
		categoryDAO.create(category);
	}

	@Override
	public void updateCategory(Category category) {
		categoryDAO.update(category);
	}

	@Override
	public List<Auction> findAuctions(int userId, int articleId) {
		List<Auction> auctions = auctionDAO.read(userId, articleId);
		auctions.forEach(auction -> {
			auction.setArticle(articleDAO.read(articleId));
			auction.setUser(userDAO.readById(userId));
		});
		return auctions;
	}

	@Override
	public List<Auction> findAuctionsByUser(int userId) {
		List<Auction> auctions = auctionDAO.findByUser(userId);
		auctions.forEach(auction -> {
			auction.setArticle(articleDAO.read(auction.getArticle().getArticleId()));
			auction.setUser(userDAO.readById(userId));
		});
		return auctions;
	}

	@Override
	public List<Auction> findAllAuctions(int articleId) {
		List<Auction> auctions = auctionDAO.findByArticle(articleId);
		auctions.forEach(auction -> {
			auction.setArticle(articleDAO.read(articleId));
			auction.setUser(userDAO.readById(auction.getUser().getUserId()));
		});
		return auctions;
	}

	@Override
	@Transactional(rollbackFor = BusinessException.class)
	public void newAuction(int articleId, int bidOffer, User userSession) throws BusinessException {
		BusinessException be = new BusinessException();
		Auction newAuction = new Auction();
		
		Article article = findArticleById(articleId);
		User currentBuyer = article.getCurrentBuyer(); //Stocke de l'utilisateur courant
		newAuction.setAuctionDate(LocalDateTime.now());
		newAuction.setUser(userSession);
		newAuction.setArticle(article);
		newAuction.setBidAmount(bidOffer);
		
		boolean isValid = checkCredit(userSession, bidOffer, be) && checkBidAmount(newAuction, be) && checkBidDate(newAuction, be);

		if (isValid) { // Si ok, on crée l'enchère
			try {
				auctionDAO.create(newAuction);
				articleDAO.updateSellPriceAndBuyer(newAuction.getArticle().getArticleId(), newAuction.getBidAmount(),
						newAuction.getUser().getUserId());
				currentBuyer.setCredit(currentBuyer.getCredit()+article.getCurrentPrice());
				userSession.setCredit(userSession.getCredit()-bidOffer);
				userDAO.updateCredit(currentBuyer);
				userDAO.updateCredit(userSession);
			} catch (DataAccessException e) {
				e.printStackTrace();
				be.add("Un problème est survenu lors de l'accès à la base de données");
				throw be;
			}
		} else {
			throw be;
		}
	}

	private boolean checkCredit(User user, int bidOffer, BusinessException be) {
		boolean isValid = false;
		if (user.getCredit() >= bidOffer) {
			isValid = true;
		} else {
			be.add("Vous n'avez pas suffisamment de crédit pour enchérir à un tel niveau.");
		}
		return isValid;
	}
	
	private boolean checkBidAmount(Auction auction, BusinessException be) {
		boolean isValid = false;
		if (auction.getArticle().getCurrentPrice() < auction.getBidAmount()) {
			isValid = true;
		} else {
			be.add("Votre offre est inférieure à la meilleure offre.");
		}
		return isValid;
	}

	private boolean checkBidDate(Auction auction, BusinessException be) {
		boolean isValid = false;
		if (auction.getAuctionDate().isBefore(auction.getArticle().getAuctionEndDate())) {
			isValid = true;
		} else {
			be.add("L'enchère est finie. Il n'est pas possible de surenchérir.");
		}
		return isValid;
	}

	@Override
	public void deleteAuction(Auction auction) {
		auctionDAO.delete(auction.getUser().getUserId(), auction.getArticle().getArticleId(), auction.getAuctionDate());
	}

	@Override
	public LocalDateTime convertDate(LocalDate date, LocalTime time) throws BusinessException {
		BusinessException be = new BusinessException();
		LocalDateTime dateTime;
		if (date != null && time != null) {
			dateTime = LocalDateTime.of(date, time);
		} else {
			be.add("Les dates et heures doivent être renseignées");
			throw be;
		}
		return dateTime;
	}
	
	
	@Override
	public void cancelArticle(Article article) {
		if(article.getState().equals(ArticleState.NOT_STARTED)) {
			
			//personne à rembourser si pas commencée
			article.setState(ArticleState.CANCELED);
			articleDAO.updateArticle(article);
			
		}else if(article.getState().equals(ArticleState.STARTED)){
			
			// modifié article ET remboursé l'acheteur courant
			User currentBuyer = article.getCurrentBuyer();
			currentBuyer.setCredit(currentBuyer.getCredit() + article.getCurrentPrice());
			userDAO.updateCredit(currentBuyer);
			
			article.setState(ArticleState.CANCELED);
			articleDAO.updateArticle(article);
			
			
		}else {
			//Trouver un moyen de notifié l'utilisateur que ce n'est pas possible
			//Après l'utilisateur ne devrai pas avoir accès au bouton d'annulation d'une vente donc ....
			System.err.println("Impossible d'annuler la vente ! Déjà annulée ou terminée !");
		}
	}

}
