package club.dnd5.portal.service;

import club.dnd5.portal.dto.api.spells.SpellLSS;
import club.dnd5.portal.exception.PageNotFoundException;
import club.dnd5.portal.model.JsonType;
import club.dnd5.portal.model.classes.HeroClass;
import club.dnd5.portal.model.exporter.JsonStorage;
import club.dnd5.portal.model.splells.Spell;
import club.dnd5.portal.repository.JsonStorageRepository;
import club.dnd5.portal.repository.datatable.SpellRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LssServiceImp implements LssService {
	private final SpellRepository spellRepository;
	private final JsonStorageRepository jsonStorageRepository;

	@Override
	public SpellLSS findByName(String name) {
		JsonStorage jsonStorage = jsonStorageRepository.findByName(name).orElseThrow(PageNotFoundException::new);
		return convertFromJsonStorageToSpellLSS(jsonStorage);
	}

	@Override
	public List<SpellLSS> getAllSpellForLSS() {
		return jsonStorageRepository.findAllByTypeJsonAndVersionFoundry(JsonType.SPELL, 11)
			.stream()
			.map(this::convertFromJsonStorageToSpellLSS)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	private SpellLSS convertFromJsonStorageToSpellLSS(JsonStorage jsonStorage) {
		SpellLSS spellLSS = new SpellLSS();
		spellLSS.setJsonData(jsonStorage.getJsonData());

		int spellId = jsonStorage.getRefId();
		Optional<Spell> optionalSpell = spellRepository.findById(spellId);
		if (optionalSpell.isPresent()) {
			List<String> classes = new ArrayList<>();
			Spell spell = optionalSpell.get();
			for (HeroClass heroClass : spell.getHeroClass()) {
				classes.add(heroClass.getEnglishName());
			}
			spellLSS.setClasses(classes);
			return spellLSS;
		} else {
			return null;
		}
	}
}
