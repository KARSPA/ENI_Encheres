package fr.eni.tp.encheres.controller;

import java.security.Principal;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import fr.eni.tp.encheres.bll.UserService;
import fr.eni.tp.encheres.bo.User;
import fr.eni.tp.encheres.exception.BusinessException;
import jakarta.validation.Valid;

@Controller
@SessionAttributes({ "userSession" })
public class LoginController {
	private static final Logger loginLogger = LoggerFactory.getLogger(LoginController.class);

	private UserService userService;
	private MessageSource messageSource;

	public LoginController(UserService userService, MessageSource messageSource) {
		this.userService = userService;
		this.messageSource = messageSource;
	}

	@GetMapping("/login")
	public String showLoginForm() {
		return "login";
	}

	@GetMapping("/")
	public String redirectToAuctions() {
		return "redirect:/auctions";
	}

	@GetMapping("/signup")
	public String showSignupPage(Model model) {
		User user = new User();
		model.addAttribute("user", user);

		return "signup";
	}

	@PostMapping("/signup")
	public String processSignup(@Valid @ModelAttribute("user") User user, BindingResult bindingResult, Model model, Locale locale) {
		if (bindingResult.hasErrors()) {
			bindingResult.getAllErrors().forEach(err -> loginLogger.error("erreur sur formulaire signup : " + err));
			return "signup";
		} else {
			try {
				userService.createAccount(user.getPseudo(), user.getName(), user.getFirstName(), user.getEmail(),
						user.getPhoneNumber(), user.getStreet(), user.getZipCode(), user.getCity(), user.getPassword(),
						user.getPasswordConfirm());
				loginLogger.info("inscription nouvel utilisateur - userId : "
						+ userService.getUserByEmail(user.getEmail()).getUserId());
				return "redirect:/login";

			} catch (BusinessException e) {
				e.getErreurs().forEach(err -> {
					String errorMessage = messageSource.getMessage(err, null, locale);
					ObjectError error = new ObjectError("globalError", errorMessage);
					bindingResult.addError(error);
					loginLogger.error(err);
				});
				return "signup";
			}
		}
	}

	@GetMapping("/session")
	public String fillUserSession(@ModelAttribute("userSession") User userSession, Principal principal) {
		System.out.println("début du getMapping session");
		String email;
		User userRecup;
		System.out.println("ppal " + principal);
		if (principal != null) {
			email = principal.getName();
			userRecup = userService.getUserByEmail(email);
		} else {
			userRecup = null;
			email = "";
		}
		if (userRecup != null) {
			userService.fillUserAttributes(userSession, userRecup);

			System.out.println(userSession);
		} else {
			userSession.setUserId(0);
			userSession.setEmail(email);
		}
		loginLogger.info("idUser mis en session : " + userSession.getUserId());

		return "redirect:/auctions";
	}

	@ModelAttribute("userSession")
	public User addUserSession() {
		User userSession = new User();

		return userSession;
	}
}