package fr.eni.tp.encheres.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fr.eni.tp.encheres.bll.AuctionService;
import fr.eni.tp.encheres.bll.UserService;
import fr.eni.tp.encheres.bo.Article;
import fr.eni.tp.encheres.bo.ArticleState;
import fr.eni.tp.encheres.bo.User;
import fr.eni.tp.encheres.exception.BusinessException;

@Controller
@RequestMapping("/bid")
@SessionAttributes({"userSession"})
public class BidController {
	
	private AuctionService auctionService;
	private UserService userService;
	
	public BidController(AuctionService auctionService, UserService userService) {
		this.auctionService = auctionService;
		this.userService =userService;
	}

	
	@GetMapping
	public String showArticleToBidOn(@RequestParam(name="articleId", required=true) int articleId,
									@SessionAttribute("userSession") User userSession,
									Model model) {
		Article articleToDisplay = auctionService.findArticleById(articleId);
		System.out.println(articleToDisplay);
		
		
		boolean isBidPossible = articleToDisplay.getState().equals(ArticleState.STARTED);
		boolean isBeforeStart = articleToDisplay.getState().equals(ArticleState.NOT_STARTED);
		
		boolean isChangePossible = isBeforeStart || isBidPossible;
		
		boolean isAuctionCanceled = articleToDisplay.getState().equals(ArticleState.CANCELED);
		
		boolean isAuctionFinished = articleToDisplay.getState().equals(ArticleState.FINISHED)
				|| articleToDisplay.getState().equals(ArticleState.RETRIEVED);
		
		
		model.addAttribute("articleDisplay", articleToDisplay);
		model.addAttribute("userSession", userSession);
		model.addAttribute("isChangePossible", isChangePossible);
		
		
		model.addAttribute("isBidPossible", isBidPossible);
		model.addAttribute("isBeforeStart", isBeforeStart);
		model.addAttribute("isAuctionFinished", isAuctionFinished);
		model.addAttribute("isAuctionCanceled", isAuctionCanceled);
		
		
		// Ajout de la date au bon format !
		String dateDisplayFormat = "dd/MM/yyyy - HH:mm";
		DateTimeFormatter dtFormater = DateTimeFormatter.ofPattern(dateDisplayFormat);
		
		String startDateDisplay = dtFormater.format(articleToDisplay.getAuctionStartDate());
		String endDateDisplay = dtFormater.format(articleToDisplay.getAuctionEndDate());
		
		model.addAttribute("startDateDisplay", startDateDisplay);
		model.addAttribute("endDateDisplay", endDateDisplay);
		
		return "bid-article-detail";
	}
	
	
	@PostMapping
	public String createBidOnArticle(@RequestParam(name="articleId", required=true) int articleId,
									@RequestParam(name="bidOffer", required=true) int bidOffer,
									@SessionAttribute("userSession") User userSession, RedirectAttributes redirectAttributes) {
		
		String redirectUrl = "redirect:/bid?articleId=" + articleId;
		
		try {
			auctionService.newAuction(articleId, bidOffer, userSession);
			
		} catch (BusinessException e) {
			e.getErreurs().forEach(err -> redirectAttributes.addFlashAttribute("globalError", err));
		}
		return redirectUrl;
		
			
	}
}
