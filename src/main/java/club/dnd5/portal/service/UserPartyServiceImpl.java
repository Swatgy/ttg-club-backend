package club.dnd5.portal.service;

import club.dnd5.portal.dto.api.*;
import club.dnd5.portal.exception.ApiException;
import club.dnd5.portal.exception.PageNotFoundException;
import club.dnd5.portal.model.user.Role;
import club.dnd5.portal.model.user.User;
import club.dnd5.portal.model.user.UserParty;
import club.dnd5.portal.repository.UserPartyRepository;
import club.dnd5.portal.repository.user.UserRepository;
import club.dnd5.portal.util.PageAndSortUtil;
import club.dnd5.portal.util.SpecificationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPartyServiceImpl implements UserPartyService {
	private final UserPartyRepository userPartyRepository;
	private final UserRepository userRepository;
	private static final int MAX_GROUP_LIMIT_PER_USER = 5;
	private final EmailService emailService;
	private final InvitationServiceImpl invitationService;

	@Override
	public UserPartyApi createUserParty(UserPartyCreateApi userPartyDTO) {
		String userEmail = getAuthenticatedUserEmail();
		User user = userRepository.findByEmail(userEmail).orElseThrow(PageNotFoundException::new);

		if (!checkGroupLimitForUser(user)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "You have exceeded the limit of groups you can own. " +
				"Consider upgrading your subscription to enjoy unlimited group ownership privileges.");
		}

		UserParty userParty = convertFromUserPartyCreateToEntity(userPartyDTO);
		userParty.setOwnerId(user.getId());

		userParty.getUserList().add(user);
		user.getUserParties().add(userParty);
		userRepository.save(user);

		userParty = userPartyRepository.save(userParty);

		List<User> usersToSendEmail = userPartyDTO.getUserListIds().stream()
			.map(userRepository::findById)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());


		emailService.sendInvitationLink(usersToSendEmail,
			invitationService.generateLinkInvitation(userParty.getId()));

		return convertToUserPartyApi(userParty);
	}

	@Override
	public List<UserPartyApi> getAllUserParties(UserPartyRequestApi request) {
		if (request == null) {
			return Collections.emptyList();
		}
		Specification<UserParty> specification = null;

		if (!request.getSearch().getValue().isEmpty()) {
			specification = SpecificationUtil.getSearch(request);
		}

		UserPartyFilter userPartyFilter = request.getFilter();
		if (userPartyFilter != null) {
			if (userPartyFilter.getUserId() != null) {
				specification = SpecificationUtil.getAndSpecification(specification,
					(root, query, cb) -> root.join("userList").get("id").in(userPartyFilter.getUserId()));
			}
			if (userPartyFilter.isOnlyOwner()) {
				specification = SpecificationUtil.getAndSpecification(specification,
					(root, query, cb) -> cb.equal(root.get("ownerId"), userPartyFilter.getUserId()));
			}
			if (userPartyFilter.getPartyName() != null) {
				specification = SpecificationUtil.getAndSpecification(specification,
					(root, query, cb) -> cb.equal(root.get("groupName"), userPartyFilter.getPartyName()));
			}
			if (userPartyFilter.getStartDate() != null && userPartyFilter.getEndDate() != null) {
				specification = SpecificationUtil.getAndSpecification(specification,
					(root, query, cb) -> cb.between(root.get("creationDate"), userPartyFilter.getStartDate(), userPartyFilter.getEndDate()));
			}
			if (userPartyFilter.getMinMembers() != null) {
				specification = SpecificationUtil.getAndSpecification(specification,
					(root, query, cb) -> cb.greaterThanOrEqualTo(cb.size(root.get("userList")), userPartyFilter.getMinMembers()));
			}
			if (userPartyFilter.getMaxMembers() != null) {
				specification = SpecificationUtil.getAndSpecification(specification,
					(root, query, cb) -> cb.lessThanOrEqualTo(cb.size(root.get("userList")), userPartyFilter.getMaxMembers()));
			}
		}

		Pageable pageable = PageAndSortUtil.getPageable(request);
		return convertToUserPartyApiList(
			new ArrayList<>(userPartyRepository.findAll(specification, pageable).toList())
		);
	}

	@Override
	public UserPartyApi getUserPartyById(Long id) {
		Optional<UserParty> optionalUserParty = userPartyRepository.findById(id);
		return optionalUserParty.map(this::convertToUserPartyApi)
			.orElseThrow(PageNotFoundException::new);
	}

	@Override
	public List<UserApi> getUserPartyMembers(Long partyId) {
		Optional<UserParty> optionalUserParty = userPartyRepository.findById(partyId);
		List<UserApi> userApis = new ArrayList<>();
		if (optionalUserParty.isPresent()) {
			for (User user : optionalUserParty.get().getUserList()) {
				UserApi userApi = convertFromUserToUserApi(user);
				userApis.add(userApi);
			}
		}
		return userApis;
	}

	@Override
	public void updateUserParty(Long partyId, UserPartyApi userPartyDTO) {
		String userEmail = getAuthenticatedUserEmail();
		User user = userRepository.findByEmail(userEmail).orElseThrow(PageNotFoundException::new);

		Optional<UserParty> optionalUserParty = userPartyRepository.findById(partyId);
		optionalUserParty.ifPresent(userParty -> {
			if (!checkPermission(user, userParty)) {
				throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to update UserParty.");
			}

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
			String userEmail = getAuthenticatedUserEmail();
			User user = userRepository.findByEmail(userEmail).orElseThrow(PageNotFoundException::new);
			if (!checkPermission(user, optionalUserParty.get())){
				throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to delete UserParty.");
			}
			userPartyRepository.deleteById(id);
			return "Delete was successful";
		} else {
			return "User party with id " + id + " not found";
		}
	}

	@Override
	@Transactional
	public String leavingFromGroup(Long groupId) {
		UserParty userParty = userPartyRepository.findById(groupId).orElseThrow(PageNotFoundException::new);

		String userEmail = getAuthenticatedUserEmail();
		User user = userRepository.findByEmail(userEmail).orElseThrow(PageNotFoundException::new);

		if (userParty.getUserList().contains(user)) {
			userParty.getUserList().remove(user);
			user.getUserParties().remove(userParty);
			userRepository.save(user);
			userPartyRepository.save(userParty);
			return "Вы покинули группу";
		} else {
			throw new PageNotFoundException();
		}
	}

	@Override
	public String kickFromGroup(Long groupId, Long userId) {
		UserParty userParty = userPartyRepository.findById(groupId).orElseThrow(PageNotFoundException::new);

		String userEmail = getAuthenticatedUserEmail();
		User user = userRepository.findByEmail(userEmail).orElseThrow(PageNotFoundException::new);
		if (checkPermission(user, userParty)) {
			User userToBeKicked = userRepository.findById(userId).orElseThrow(PageNotFoundException::new);
			userParty.getUserList().remove(userToBeKicked);
			userToBeKicked.getUserParties().remove(userParty);

			userRepository.save(user);
			userPartyRepository.save(userParty);
			return "User with name " + userToBeKicked.getName() + " has been successfully kicked from the group.";
		}
		throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to kick users from this group.");
	}

	// Utility methods for conversion between API DTO and JPA Entity
	private UserPartyApi convertToUserPartyApi(UserParty userParty) {
		UserPartyApi userPartyApi = new UserPartyApi();
		userPartyApi.setId(userParty.getId());
		userPartyApi.setOwnerId(userParty.getOwnerId());
		userPartyApi.setGroupName(userParty.getGroupName());
		userPartyApi.setDescription(userParty.getDescription());
		userPartyApi.setUserApiList(userParty.getUserList().stream()
			.map(this::convertFromUserToUserApi)
			.collect(Collectors.toList()));
		userPartyApi.setCreationDate(userParty.getCreationDate());
		userPartyApi.setLastUpdateDate(userParty.getLastUpdateDate());
		return userPartyApi;
	}

	private UserParty convertFromUserPartyCreateToEntity(UserPartyCreateApi userPartyCreateApi) {
		UserParty userParty = new UserParty();
		userParty.setGroupName(userPartyCreateApi.getGroupName());
		userParty.setDescription(userPartyCreateApi.getDescription());
		userParty.setCreationDate(new Date());
		userParty.setLastUpdateDate(new Date());
		return userParty;
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

	private UserApi convertFromUserToUserApi(User user) {
		return UserApi.builder()
			.email(user.getEmail())
			.name(user.getName())
			.roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
			.username(user.getUsername())
			.build();
	}

	private boolean checkPermission(User user, UserParty userParty) {
		return user.getRoles().stream()
			.anyMatch(role -> role.getName().equals("ADMIN"))
			|| userParty.getOwnerId().equals(user.getId());
	}

	private boolean checkGroupLimitForUser(User user) {
		return user.getRoles().stream()
			.anyMatch(role -> role.getName().equals("ADMIN") || role.getName().equals("SUBSCRIBER"))
			|| user.getUserParties().stream()
			.filter(userParty -> userParty.getOwnerId().equals(user.getId()))
			.count() < MAX_GROUP_LIMIT_PER_USER;
	}
}
