package club.dnd5.portal.model.foundary;

import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import club.dnd5.portal.model.ArmorType;
import club.dnd5.portal.model.creature.Action;
import club.dnd5.portal.model.creature.ActionDataType;
import club.dnd5.portal.model.creature.ActionType;
import club.dnd5.portal.model.creature.CreatureFeat;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FItemSystem {
	private FDiscription description;
	private String source = "";
	private int quantity = 1;
	private float weight;
	private FPrice price = new FPrice();
	private boolean attuned;
	private boolean equipped = true;
	private String rarity;
	private boolean  identified = true;
	private FActivation activation;
	private FDuration duration;
	private FTarget target;
	private FRange range;
	private FUses uses;
	private FConsume consume;
	private String ability = "";
	private String actionType= "";
    private byte attackBonus;
    private String chatFlavor = "";
    private String critical;
    private FItemDamage damage;
    private FCharge recharge;
    private String formula = "";
    private FSave save;
    private FArmor armor;
    private FIHP hp;
    private String weaponType = "natural";
    private FWeaponProperties properties;
    private boolean proficient = true;
    private String cptooltipmode = "hide";

	FItemSystem(CreatureFeat feat) {
		parseRecharge(feat.getName());
		description = new FDiscription(feat.getDescription().replace("href=\"/", "href=\"/http://ttg.club/"));
		activation = new FActivation();
		duration = new FDuration();
		target = new FTarget();
		range = new FRange();
		uses = new FUses();
		consume = new FConsume();
		save = new FSave();
		armor = new FArmor();
		hp = new FIHP();
		properties = new FWeaponProperties();
		if (feat.getDescription().contains("спасброс")) {
			save = new FSave();
			if (feat.getDescription().contains("Силы")) {
				save.setAbility("str");
			} else if (feat.getDescription().contains("Ловкости")) {
				save.setAbility("dex");
			} else if (feat.getDescription().contains("Телосложения")) {
				save.setAbility("con");
			} else if (feat.getDescription().contains("Мудрости")) {
				save.setAbility("wiz");
			} else if (feat.getDescription().contains("Интеллекта")) {
				save.setAbility("int");
			} else if (feat.getDescription().contains("Харизмы")) {
				save.setAbility("cha");
			}
			Pattern dcMatcher =
				Pattern.compile("(Сл|Сложностью)\\s\\d+");
			Matcher matcher = dcMatcher.matcher(feat.getDescription());
			if (matcher.find()) {
				String dc = matcher.group();
				dc = dc.replaceAll("\\D+", "");
				save.setDc(Integer.parseInt(dc.trim()));
				save.setScaling("flat");
			}
			activation = new FActivation(ActionType.ACTION.name().toLowerCase(), (byte) 1, "");
			actionType = ActionDataType.parse(feat.getDescription());
			damage = new FItemDamage();

			Queue<String> damageTypes = FDamageType.parse(feat.getDescription());
			Pattern patternDamageFormula = Pattern.compile("\\d+к\\d+(\\s\\+\\s\\d+){0,}");
			matcher = patternDamageFormula.matcher(feat.getDescription());
			while (matcher.find()) {
				String damageFormula = matcher.group().replace("к", "d").replace("−", "-");
				String damageType = damageTypes.poll();
				damage.addDamage(damageFormula, damageType);
			}
		}
	}

	public FItemSystem(Action action) {
		parseRecharge(action.getName());
		description = new FDiscription(action.getDescription().replace("href=\"/", "href=\"/https://ttg.club/"));
		activation = new FActivation(action.getActionType().name().toLowerCase(), (byte) 1, "");
		duration = new FDuration();
		target = new FTarget();
		range = new FRange();
		uses = new FUses();
		consume = new FConsume();
		actionType = ActionDataType.parse(action.getDescription());
		if (action.getDescription().contains("спасброс")) {
			save = new FSave();
			if (action.getDescription().contains("Силы")) {
				save.setAbility("str");
			} else if (action.getDescription().contains("Ловкости")) {
				save.setAbility("dex");
			} else if (action.getDescription().contains("Телосложения")) {
				save.setAbility("con");
			} else if (action.getDescription().contains("Мудрости")) {
				save.setAbility("wiz");
			} else if (action.getDescription().contains("Интеллекта")) {
				save.setAbility("int");
			} else if (action.getDescription().contains("Харизмы")) {
				save.setAbility("cha");
			}
			Pattern dcMatcher =
					Pattern.compile("(Сл|Сложностью)\\s\\d+");
			Matcher matcher = dcMatcher.matcher(action.getDescription());
			if (matcher.find()) {
				String dc = matcher.group();
				dc = dc.replaceAll("\\D+", "");
				save.setDc(Integer.parseInt(dc.trim()));
				save.setScaling("flat");
			}
		}
		armor = new FArmor();
		damage = new FItemDamage();

		Queue<String> damageTypes = FDamageType.parse(action.getDescription());
		Pattern patternDamageFormula = Pattern.compile("\\d+к\\d+(\\s\\+\\s\\d+){0,}");
		Matcher matcher = patternDamageFormula.matcher(action.getDescription());
		while (matcher.find()) {
			String damageFormula = matcher.group().replace("к", "d").replace("−", "-");
			if (damage.getParts().isEmpty()) {
				String damageType = damageTypes.poll();
				damage.addDamage(damageFormula, damageType);
			} else {
				String damageType = damageTypes.poll();
				damage.addDamage(damageFormula, damageType);
			}
		}
		hp = new FIHP();
		properties = new FWeaponProperties();
	}

	public FItemSystem(ArmorType armorType) {
		description = new FDiscription(armorType.getCyrillicName());
		activation = new FActivation();
		duration = new FDuration();
		target = new FTarget();
		range = new FRange();
		uses = new FUses();
		consume = new FConsume();
		save = new FSave();
		armor = new FArmor(armorType.getArmorClass(), armorType.getArmorType(), armorType.getArmorDexBonus());
		hp = new FIHP();
		properties = new FWeaponProperties();
	}

	private void parseRecharge(String name){
		if (name.contains("перезарядка")) {
			int value = 4;
			if (name.contains("5")) {
				value = 5;
			} else if (name.contains("6")) {
				value = 5;
			}
			recharge = new FCharge();
			recharge.setValue(value);
		}
	}
}
