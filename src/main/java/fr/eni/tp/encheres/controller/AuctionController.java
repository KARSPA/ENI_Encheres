package fr.eni.tp.encheres.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;

import fr.eni.tp.encheres.bll.AuctionService;
import fr.eni.tp.encheres.bll.FileService;
import fr.eni.tp.encheres.bo.Article;
import fr.eni.tp.encheres.bo.ArticleState;
import fr.eni.tp.encheres.bo.Category;
import fr.eni.tp.encheres.bo.PickupLocation;
import fr.eni.tp.encheres.bo.User;
import fr.eni.tp.encheres.bo.dto.SearchCriteria;
import fr.eni.tp.encheres.exception.BusinessException;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/auctions")
@SessionAttributes({ "userSession", "categoriesSession", "research" })
public class AuctionController {
	private static final Logger auctionLogger = LoggerFactory.getLogger(AuctionController.class);

	private static final int PAGE_SIZE = 6;

	private AuctionService auctionService;
	private FileService fileService;
	private MessageSource messageSource;

	public AuctionController(AuctionService auctionService, FileService fileService, MessageSource messageSource) {
		this.auctionService = auctionService;
		this.fileService = fileService;
		this.messageSource = messageSource;
	}

	@GetMapping
	public String showAuctionsPage(@ModelAttribute("research") SearchCriteria sessionResearch,
			@ModelAttribute("userSession") User userSession, Model model,
			@RequestParam(name = "currentPage", defaultValue = "1") int currentPage) {
		auctionLogger.info("Méthode showAuctionsPage");
		model.addAttribute("criteria", sessionResearch);
		Page<Article> articlesList = auctionService.selectArticles(sessionResearch, userSession.getUserId(),
				PageRequest.of(currentPage - 1, PAGE_SIZE));
		model.addAttribute("articles", articlesList);
		model.addAttribute("currentPage", currentPage);

		int totalPages = articlesList.getTotalPages();
		auctionLogger.info(
				"id utilisateur connecté : " + userSession.getUserId() + " affichage des enchères selon critères : "
						+ sessionResearch + " - nombre total de pages : " + totalPages);
		if (totalPages > 0) {
			List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
			model.addAttribute("pageNumbers", pageNumbers);
		}

		return "auctions";
	}

	@PostMapping
	public String showAuctions(@ModelAttribute("criteria") SearchCriteria research,
			@ModelAttribute("research") SearchCriteria sessionResearch, Model model,
			@ModelAttribute("userSession") User userSession,
			@RequestParam(name = "currentPage", defaultValue = "1") int currentPage) {
		auctionLogger.info("Méthode showAuctions");
		
		sessionResearch.setWordToFind(research.getWordToFind());
		sessionResearch.setCategory(research.getCategory());
		sessionResearch.setRadioButton(research.getRadioButton());
		sessionResearch.setFilters(research.getFilters());

		model.addAttribute("criteria", research);
		Page<Article> articlesList = auctionService.selectArticles(research, userSession.getUserId(),
				PageRequest.of(currentPage - 1, PAGE_SIZE));
		model.addAttribute("articles", articlesList);
		model.addAttribute("currentPage", currentPage);

		int totalPages = articlesList.getTotalPages();
		auctionLogger.info(
				"id utilisateur connecté : " + userSession.getUserId() + " - recherche des enchères selon critères : "
						+ sessionResearch + " - nombre total de pages : " + totalPages);
		if (totalPages > 0) {
			List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
			model.addAttribute("pageNumbers", pageNumbers);
		}

		return "auctions";
	}

	@GetMapping("/newArticle")
	public String showArticleCreation(Model model, @ModelAttribute("userSession") User userSession) {
		auctionLogger.info("Méthode showArticleCreation en Get");
		System.err.println("je suis dans newArticle");
		
		// Si utilisateur désactivé, on empèche l'enchère
		if (!userSession.isActivated()) {
			auctionLogger.info("id utilisateur connecté : " + userSession.getUserId()
				+ " utilisateur désactivé. Refus de la création d'article");
			return "redirect:/auctions";
		}

		Article article = new Article();
		PickupLocation defaultPickupLocation = new PickupLocation(userSession.getStreet(), userSession.getZipCode(),
				userSession.getCity());
		article.setPickupLocation(defaultPickupLocation);
		model.addAttribute("article", article);
		auctionLogger.info("id utilisateur connecté : " + userSession.getUserId()
				+ " - affichage du formulaire de création d'article");
		return "article-create";
	}

	@PostMapping("/newArticle")
	public String showArticleCreation(@Valid @ModelAttribute("article") Article article, BindingResult bindingResult,
			@ModelAttribute("userSession") User userSession,
			@RequestParam(name = "startDateTemp", required = false) LocalDate startDate,
			@RequestParam(name = "endDateTemp", required = false) LocalDate endDate,
			@RequestParam(name = "startTimeTemp", required = false) LocalTime startTime,
			@RequestParam(name = "endTimeTemp", required = false) LocalTime endTime,
			@RequestParam(name = "inputImage", required = false) MultipartFile fileImage, Locale locale) {
		auctionLogger.info("Méthode showArticleCreation en Post");
		
		LocalDateTime startDateTime;
		LocalDateTime endDateTime;

		if (bindingResult.hasErrors()) {
			bindingResult.getAllErrors().forEach(err -> auctionLogger.error("id utilisateur connecté : "
					+ userSession.getUserId() + " - erreur sur formulaire newArticle : " + err));
			return "article-create";

		} else {

			try {
				startDateTime = auctionService.convertDate(startDate, startTime);
				endDateTime = auctionService.convertDate(endDate, endTime);
				article.setAuctionStartDate(startDateTime);
				article.setAuctionEndDate(endDateTime);
				article.setSeller(userSession);
				article.setCurrentPrice(article.getBeginningPrice());
				// Sauvegarde de l'image dans le dossier static/uploadedImages
				fileService.saveFile(fileImage, article);
				auctionService.sell(article);
				auctionLogger.info("id utilisateur connecté : " + userSession.getUserId()
						+ " - mise en vente d'un nouvel article - articleId : " + article.getArticleId());
				return "redirect:/auctions";
			} catch (IOException err) {
				// ObjectError error = new ObjectError("globalError", e.getMessage());
				return "article-create";
			} catch (BusinessException e) {
				e.getErreurs().forEach(err -> {
					String errorMessage = messageSource.getMessage(err, null, locale);
					ObjectError error = new ObjectError("globalError", errorMessage);
					bindingResult.addError(error);

					auctionLogger.error("id utilisateur connecté : " + userSession.getUserId()
							+ " - erreur à la mise en vente d'un article " + err);
				});
				return "article-create";
			}

		}
	}

	@GetMapping("/modifyArticle")
	public String showArticleModifyPage(@RequestParam(name = "articleId", required = true) int articleId, Model model,
			@ModelAttribute("userSession") User userSession) {
		auctionLogger.info("Méthode showArticleModifyPage en Get");
		
		// Si utilisateur désactivé, on empèche l'enchère
		if (!userSession.isActivated()) {
			auctionLogger.info("id utilisateur connecté : " + userSession.getUserId()
			+ " utilisateur désactivé. Refus de la modification d'article");
			return "redirect:/auctions";
		}

		Article article = auctionService.findArticleById(articleId);

		// Si on est après la date de fin on ne montre pas la page
		// (modification/annulation impossible !)
		if (article.getAuctionEndDate().isBefore(LocalDateTime.now())) {
			auctionLogger.error("id utilisateur connecté : " + userSession.getUserId()
					+ " - tentative de modifier une enchère finie - articleId : " + article.getArticleId());
			return "redirect:/auctions";
		}
		;

		model.addAttribute("article", article);
		model.addAttribute("startDate", article.getAuctionStartDate().toLocalDate());
		model.addAttribute("startTime", article.getAuctionStartDate().toLocalTime());
		model.addAttribute("endDate", article.getAuctionEndDate().toLocalDate());
		model.addAttribute("endTime", article.getAuctionEndDate().toLocalTime());
		model.addAttribute("isCancelPossible",
				article.getState().equals(ArticleState.NOT_STARTED) || article.getState().equals(ArticleState.STARTED));
		model.addAttribute("imageSource", "/uploadedImages/" + article.getImageUUID());

		auctionLogger.info("id utilisateur connecté : " + userSession.getUserId()
				+ " - affichage page de modification de l'article - articleId : " + article.getArticleId());

		System.err.println("/uploadedImages/" + article.getImageUUID());

		return "article-modify";
	}

	@PostMapping("/modifyArticle")
	public String showArticleModifyPage(@Valid @ModelAttribute("article") Article article, BindingResult bindingResult,
			@ModelAttribute("userSession") User userSession,
			@RequestParam(name = "startDateTemp", required = false) LocalDate startDate,
			@RequestParam(name = "endDateTemp", required = false) LocalDate endDate,
			@RequestParam(name = "startTimeTemp", required = false) LocalTime startTime,
			@RequestParam(name = "endTimeTemp", required = false) LocalTime endTime,
			@RequestParam(name = "inputImage", required = false) MultipartFile fileImage, Model model, Locale locale) {
		auctionLogger.info("Méthode showArticleModifiyPage en Post");
		
		LocalDateTime startDateTime;
		LocalDateTime endDateTime;

		// On les repasse dans le modèle pour réafficher les valeurs par défaut en cas
		// d'erreurs
		model.addAttribute("startDate", startDate);
		model.addAttribute("startTime", startTime);
		model.addAttribute("endDate", endDate);
		model.addAttribute("endTime", endTime);

		if (bindingResult.hasErrors()) {
			bindingResult.getAllErrors().forEach(err -> auctionLogger.error("id utilisateur connecté : "
					+ userSession.getUserId() + " - erreur sur formulaire modifyArticle : " + err));
			return "article-modify";
		} else {

			try {
				startDateTime = auctionService.convertDate(startDate, startTime);
				endDateTime = auctionService.convertDate(endDate, endTime);
				article.setAuctionStartDate(startDateTime);
				article.setAuctionEndDate(endDateTime);
				article.setSeller(userSession);
				article.setCurrentPrice(article.getBeginningPrice());

				// Sauvegarde de l'image dans le dossier static/uploadedImages
				fileService.saveFile(fileImage, article);

				auctionService.updateArticle(article);
				auctionLogger.info("id utilisateur connecté : " + userSession.getUserId()
						+ " - modification d'un article - articleId : " + article.getArticleId());

				return "redirect:/auctions";

			} catch (IOException err) {
				// ObjectError error = new ObjectError("globalError", e.getMessage());
				return "article-modify";
			} catch (BusinessException e) {
				e.getErreurs().forEach(err -> {
					String errorMessage = messageSource.getMessage(err, null, locale);
					ObjectError error = new ObjectError("globalError", errorMessage);
					bindingResult.addError(error);
					auctionLogger.error("id utilisateur connecté : " + userSession.getUserId()
							+ " - erreur à la modification d'un article " + error);
				});
				return "article-modify";
			}

		}
	}
  
  
	@GetMapping("/retrieveArticle")
	public String retrieveArticle(@RequestParam(name="articleId", required=true) int articleId,
			@SessionAttribute("userSession") User userSession) {
		String redirectUrl = "redirect:/bid?articleId=" + articleId;
		
		Article articleToUpdate = auctionService.findArticleById(articleId);
		//check état article avant de l'update ?
		if(!articleToUpdate.getState().equals(ArticleState.FINISHED)) {
			System.err.println("Pas possible de retirer un article déjà retiré ou dont l'enchère n'est pas terminée  !");
			return redirectUrl;
		}
		auctionService.updateArticleState(ArticleState.RETRIEVED, articleId);
		
		
		
		return redirectUrl;
	}
	
	
	@GetMapping("/cancelArticle")
	public String cancelArticle(@RequestParam(name = "articleId") int articleId,
			@SessionAttribute("userSession") User userSession) {
		auctionLogger.info("Méthode cancelArticle");
		
		Article articleToCancel = auctionService.findArticleById(articleId);

		// On vérifie si c'est bien le vendeur de l'article qui accède à ce lien
		if (articleToCancel.getSeller().getUserId() == userSession.getUserId()) {

			auctionService.cancelArticle(articleToCancel);
			auctionLogger.info("id utilisateur connecté : " + userSession.getUserId()
					+ " - annulation d'une vente - articleId : " + articleToCancel.getArticleId());

		} else { // Sinon c'est un imposteur
			auctionLogger.error("id utilisateur connecté : " + userSession.getUserId()
					+ " - tentative d'annulation d'une vente d'un autre utilisateur - articleId : "
					+ articleToCancel.getArticleId() + " - id utilisateur légitime de cet article : "
					+ articleToCancel.getSeller().getUserId());
			return "redirect:/auctions";
		}

		return "redirect:/auctions";
	}

	@ModelAttribute("categoriesSession")
	public List<Category> loadCategories() {
		auctionLogger.info("création du ModelAttribute categoriesSession");
		List<Category> categories = auctionService.findCategories();
		return categories;
	}

	@ModelAttribute("research")
	public SearchCriteria research() {
		auctionLogger.info("création du ModelAttribute research");
		SearchCriteria research = new SearchCriteria();
		research.setWordToFind("");
		Category category = new Category();
		research.setCategory(category);
		research.setRadioButton("purchases");
		Map<String, Boolean> filters = new HashMap<String, Boolean>();
		filters.put("open", true);
		filters.put("current", false);
		filters.put("won", false);
		filters.put("currentVente", false);
		filters.put("notstarted", false);
		filters.put("finished", false);
		research.setFilters(filters);
		return research;
	}
}
