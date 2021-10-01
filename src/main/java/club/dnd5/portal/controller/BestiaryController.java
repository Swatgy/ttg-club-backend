package club.dnd5.portal.controller;

import javax.naming.directory.InvalidAttributesException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.thymeleaf.util.StringUtils;

import club.dnd5.portal.repository.datatable.BestiaryDatatableRepository;

@Controller
public class BestiaryController {
	@Autowired
	private BestiaryDatatableRepository repository;
	
	@GetMapping("/bestiary")
	public String getCreatures() {
		return "bestiary";
	}
	
	@GetMapping("/bestiary/{name}")
	public String getCreature(Model model, @PathVariable String name) {
		model.addAttribute("selectedCreature", StringUtils.capitalizeWords(repository.findByEnglishName(name.replace("_", " ")).getName().toLowerCase()));
		return "bestiary";
	}
	
	@GetMapping("/bestiary/fragment/{id}")
	public String getSpellFragmentById(Model model, @PathVariable Integer id) throws InvalidAttributesException {
		model.addAttribute("creature", repository.findById(id).orElseThrow(InvalidAttributesException::new));
		model.addAttribute("firstElement", new FirstElement());
		return "fragments/creature :: view";
	}
	
	public static class FirstElement{
		private boolean first = true;
		public boolean isFirst() {
			boolean first = this.first;
			this.first = false;
			return first;
		}
	}
}