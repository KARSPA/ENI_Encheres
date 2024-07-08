package fr.eni.tp.encheres.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fr.eni.tp.encheres.bll.PaperCutEmailService;

@Controller
public class PaperCutEmailController {
	
	@Autowired
	private PaperCutEmailService emailService;
	
	@GetMapping("/forgot-password")
	public String forgotPasswordForm() {
		return "forgot-password";
	}

	
	
}
