package fr.eni.tp.encheres.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;

import fr.eni.tp.encheres.bll.UserService;
import fr.eni.tp.encheres.bo.User;

@Controller
@RequestMapping("/profil")
@SessionAttributes({"userSession"})
public class ProfilController {
	
	private UserService userService;
	
	public ProfilController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public String showProfilPage(@ModelAttribute("userSession") User userSession) {
		return "profil";
	}
	
	@GetMapping("/modify")
	public String showModifyProfilPage() {
		return "profil-modify";
	}
	
	@PostMapping("/modify")
	public String modifyUserInfos(@ModelAttribute("userForm") User userForm,
								@SessionAttribute("userSession") User userSession,
								@RequestParam(name="updatedPassword", required=false) String updatedPassword,
								@RequestParam(name="currentPassword", required=false) String currentPassword) {
		
		userForm.setUserId(userSession.getUserId());
		userForm.setCredit(userSession.getCredit());
		userForm.setPassword(updatedPassword);
		userSession.setPassword(currentPassword); //On met le mot de passe actuel renseigné dans le formulaire dans l'utilsateur en session pour le récupérer dans le service.
		
		userService.updateProfile(userForm, userSession);
		
		
		User userWithUpdates = userService.viewUserProfile(userForm.getUserId());
		userService.fillUserAttributes(userSession, userWithUpdates);
		
		userSession.setPassword(null); //On ne stocke pas le mot de passe de l'utilisateur en session
		
		return "redirect:/profil";
	}
}
