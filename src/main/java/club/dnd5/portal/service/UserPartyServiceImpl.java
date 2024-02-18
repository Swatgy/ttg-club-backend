package club.dnd5.portal.service;

import club.dnd5.portal.dto.api.UserPartyApi;
import club.dnd5.portal.exception.PageNotFoundException;
import club.dnd5.portal.model.user.User;
import club.dnd5.portal.model.user.UserParty;
import club.dnd5.portal.repository.UserPartyRepository;
import club.dnd5.portal.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPartyServiceImpl implements UserPartyService {
	private final UserPartyRepository userPartyRepository;

	private final UserRepository userRepository;

	private final EmailService emailService;

	private final InvitationServiceImpl invitationService;

	@Override
	public UserPartyApi createUserParty(UserPartyApi userPartyDTO) {
		String userEmail = getAuthenticatedUserEmail();
		User user = userRepository.findByEmail(userEmail).orElseThrow(PageNotFoundException::new);

		UserParty userParty = convertToUserPartyEntity(userPartyDTO);
		userParty.getUserList().add(user);
		userParty = userPartyRepository.save(userParty);

		emailService.sendInvitationLink(userParty.getUserList(),
			invitationService.getInviteByLink(userParty.getId()));

		return convertToUserPartyApi(userParty);
	}

	@Override
	public List<UserPartyApi> getAllUserParties() {
		List<UserParty> userParties = userPartyRepository.findAll();
		return convertToUserPartyApiList(userParties);
	}

	@Override
	public UserPartyApi getUserPartyById(Long id) {
		Optional<UserParty> optionalUserParty = userPartyRepository.findById(id);
		return optionalUserParty.map(this::convertToUserPartyApi)
			.orElseThrow(PageNotFoundException::new);
	}

	@Override
	public UserPartyApi getUserPartyByName(String name) {
		Optional<UserParty> optionalUserParty = userPartyRepository.findByGroupName(name);
		return optionalUserParty.map(this::convertToUserPartyApi)
			.orElseThrow(PageNotFoundException::new);
	}

	@Override
	public List<Long> getUserPartyMembers(Long partyId) {
		Optional<UserParty> optionalUserParty = userPartyRepository.findById(partyId);
		return optionalUserParty.map(userParty -> userParty.getUserList().stream()
				.map(User::getId)
				.collect(Collectors.toList()))
			.orElseThrow(PageNotFoundException::new);
	}

	@Override
	public void updateUserParty(Long partyId, UserPartyApi userPartyDTO) {
		Optional<UserParty> optionalUserParty = userPartyRepository.findById(partyId);
		optionalUserParty.ifPresent(userParty -> {
			userParty.setGroupName(userPartyDTO.getGroupName());
			userParty.setDescription(userPartyDTO.getDescription());
			userParty.setLastUpdateDate(new Date());
			userPartyRepository.save(userParty);
		});
	}

	@Override
	public String deleteUserPartyById(Long id) {
		Optional<UserParty> optionalUserParty = userPartyRepository.findById(id);
		if (optionalUserParty.isPresent()) {
			userPartyRepository.deleteById(id);
			return "Delete was successful";
		} else {
			return "User party with id " + id + " not found";
		}
	}

	// Utility methods for conversion between API DTO and JPA Entity
	private UserPartyApi convertToUserPartyApi(UserParty userParty) {
		UserPartyApi userPartyApi = new UserPartyApi();
		userPartyApi.setId(userParty.getId());
		userPartyApi.setOwnerId(userParty.getOwnerId());
		userPartyApi.setGroupName(userParty.getGroupName());
		userPartyApi.setDescription(userParty.getDescription());
		userPartyApi.setUserListIds(userParty.getUserList().stream()
			.map(User::getId)
			.collect(Collectors.toList()));
		userPartyApi.setCreationDate(userParty.getCreationDate());
		userPartyApi.setLastUpdateDate(userParty.getLastUpdateDate());
		return userPartyApi;
	}

	private UserParty convertToUserPartyEntity(UserPartyApi userPartyDTO) {
		List<User> userList = userPartyDTO.getUserListIds().stream()
			.map(userId -> userRepository.findById(userId).orElse(null))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());

		return UserParty.builder()
			.ownerId(userPartyDTO.getOwnerId())
			.groupName(userPartyDTO.getGroupName())
			.description(userPartyDTO.getDescription())
			.userList(userList)
			.creationDate((new Date()))
			.lastUpdateDate(userPartyDTO.getLastUpdateDate())
			.build();
	}

	private List<UserPartyApi> convertToUserPartyApiList(List<UserParty> userParties) {
		List<UserPartyApi> userPartyApis = new ArrayList<>();
		for (UserParty userParty : userParties) {
			userPartyApis.add(convertToUserPartyApi(userParty));
		}
		return userPartyApis;
	}

	private String getAuthenticatedUserEmail() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof AnonymousAuthenticationToken)) {
			return authentication.getName();
		}
		throw new IllegalStateException("User is not authenticated");
	}
}
