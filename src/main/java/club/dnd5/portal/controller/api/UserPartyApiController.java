package club.dnd5.portal.controller.api;

import club.dnd5.portal.dto.api.PartyMember;
import club.dnd5.portal.dto.api.UserPartyApi;
import club.dnd5.portal.dto.api.UserPartyCreateApi;
import club.dnd5.portal.dto.api.UserPartyRequestApi;
import club.dnd5.portal.service.UserPartyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user-parties")
@RequiredArgsConstructor
public class UserPartyApiController {
	private final UserPartyService userPartyService;

	@Operation(summary = "Получение группы по айди")
	@GetMapping("/{partyId}")
	@ResponseStatus(HttpStatus.OK)
	public UserPartyApi getUserPartyById(@PathVariable Long partyId) {
		return userPartyService.getUserPartyById(partyId);
	}

	@Operation(summary = "Создание группы")
	@PostMapping
	@SecurityRequirement(name = "Bearer Authentication")
	@ResponseStatus(HttpStatus.CREATED)
	public UserPartyApi createUserParty(@RequestBody UserPartyCreateApi userPartyDTO) {
		return userPartyService.createUserParty(userPartyDTO);
	}

	@Operation(summary = "Обновление группы")
	@PutMapping("/{partyId}")
	@SecurityRequirement(name = "Bearer Authentication")
	@ResponseStatus(HttpStatus.OK)
	public void updateUserParty(@PathVariable Long partyId, @RequestBody UserPartyApi userPartyDTO) {
		userPartyService.updateUserParty(partyId, userPartyDTO);
	}

	@Operation(summary = "Удаление группы")
	@DeleteMapping("/{partyId}")
	@SecurityRequirement(name = "Bearer Authentication")
	@ResponseStatus(HttpStatus.OK)
	public String deleteUserParty(@PathVariable Long partyId) {
		return userPartyService.deleteUserPartyById(partyId);
	}

	@Operation(summary = "Получение юзеров, которые находятся в группе")
	@GetMapping("/members/{partyId}")
	@ResponseStatus(HttpStatus.OK)
	public List<PartyMember> getUserPartyMembers(@PathVariable Long partyId) {
		return userPartyService.getUserPartyMembers(partyId);
	}

	@Operation(summary = "Получение краткого списка всех групп")
	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public List<UserPartyApi> getAllUserParties(@RequestBody UserPartyRequestApi request) {
		return userPartyService.getAllUserParties(request);
	}

	@Operation(summary = "Удаление участника группы, создателем группы")
	@DeleteMapping("/members/{partyId}/kick/{userId}")
	@SecurityRequirement(name = "Bearer Authentication")
	@ResponseStatus(HttpStatus.OK)
	public String kickFromGroup(@PathVariable("partyId") Long groupId, @PathVariable("userId") Long userId) {
		return userPartyService.kickFromGroup(groupId, userId);
	}

	@Operation(summary = "Покидание группы")
	@DeleteMapping("/leaving/{partyId}")
	@SecurityRequirement(name = "Bearer Authentication")
	@ResponseStatus(HttpStatus.OK)
	public String leavingFromGroup(@PathVariable("partyId") Long groupId) {
		return userPartyService.leavingFromGroup(groupId);
	}

	@Operation(summary = "Подтверждение участника")
	@PutMapping("/{groupId}/confirm/{userId}")
	@SecurityRequirement(name = "Bearer Authentication")
	@ResponseStatus(HttpStatus.OK)
	public String confirmUser(@PathVariable Long groupId, @PathVariable Long userId) {
		return null;
	}

}
