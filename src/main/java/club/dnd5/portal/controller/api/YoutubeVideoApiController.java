package club.dnd5.portal.controller.api;

import club.dnd5.portal.dto.api.ResponseApi;
import club.dnd5.portal.dto.api.spells.Order;
import club.dnd5.portal.dto.api.youtube.YoutubeVideoApi;
import club.dnd5.portal.model.YoutubeVideo;
import club.dnd5.portal.model.user.Role;
import club.dnd5.portal.model.user.User;
import club.dnd5.portal.repository.YoutubeVideosRepository;
import club.dnd5.portal.repository.user.UserRepository;
import club.dnd5.portal.util.SortUtil;
import club.dnd5.portal.util.SpecificationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Youtube", description = "The Youtube API")
@RestController
@RequestMapping(value = "/api/v1/youtube")
public class YoutubeVideoApiController {
	private static final Set<String> ROLES = new HashSet<>(Arrays.asList("MODERATOR", "ADMIN"));

	@Autowired
	private YoutubeVideosRepository youtubeVideosRepository;

	@Autowired
	private UserRepository userRepository;

	@Operation(summary = "Get added video")
	@GetMapping
	public ResponseEntity<ResponseApi<YoutubeVideoApi>> getVideos(
		@RequestParam(required = false, defaultValue = "0") Integer page,
		@RequestParam(required = false, defaultValue = "-1") Integer limit,
		@RequestParam(required = false) String search,
		@RequestParam(required = false) List<String> order,
		@RequestParam(required = false) Boolean onlyActive
	) {
		Sort sort;

		if (!CollectionUtils.isEmpty(order)) {
			sort = Sort.by(
				order
					.stream()
					.filter(Objects::nonNull)
					.map(Order::new)
					.map(SortUtil::getOrder)
					.collect(Collectors.toList())
			);
		} else {
			sort = Sort.unsorted();
		}

		Pageable pageable = null;

		if (page != null && limit != null && limit != -1) {
			pageable = PageRequest.of(page, limit, sort);
		}

		Specification<YoutubeVideo> specification = null;

		if (search != null) {
			specification = (root, query, cb) -> cb.like(root.get("name"), "%" + search + "%");
		}

		if (onlyActive != null) {
			specification = SpecificationUtil.getAndSpecification(
				specification,
				(root, query, cb) -> cb.equal(root.get("active"), onlyActive)
			);
		}

		long count;

		if (onlyActive != null) {
			count = youtubeVideosRepository.countByActive(onlyActive);
		} else {
			count = youtubeVideosRepository.count();
		}

		Collection<YoutubeVideo> videos;

		if (pageable == null) {
			videos = youtubeVideosRepository.findAll(specification, sort);
		} else {
			videos = youtubeVideosRepository.findAll(specification, pageable).toList();
		}

		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				new ResponseApi<>(
					count,
					videos
						.stream()
						.map(YoutubeVideoApi::new)
						.collect(Collectors.toList())
				)
			);
	}

	@Operation(summary = "Adding new video")
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping
	public ResponseEntity<?> addVideo(@RequestBody YoutubeVideoApi videoApi) {
		User user = getCurrentUser();

		if (!user.getRoles().stream().map(Role::getName).anyMatch(ROLES::contains)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission");
		}

		if (videoApi.getId().isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Video ID is incorrect");
		}

		if (videoApi.getName().isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Video name is incorrect");
		}

		Optional<YoutubeVideo> oldVideo = youtubeVideosRepository.findById(videoApi.getId());

		if (oldVideo.isPresent()) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("Video with current ID is already exist");
		}

		YoutubeVideo newVideo = new YoutubeVideo();

		newVideo.setId(videoApi.getId());
		newVideo.setName(videoApi.getName());
		newVideo.setUser(user);
		newVideo.setActive(Boolean.TRUE);
		newVideo.setCreated(LocalDateTime.now());

		YoutubeVideo saved = youtubeVideosRepository.save(newVideo);

		return ResponseEntity.status(HttpStatus.OK).body(new YoutubeVideoApi(saved));
	}

	@Operation(summary = "Update video")
	@SecurityRequirement(name = "Bearer Authentication")
	@PatchMapping
	public ResponseEntity<?> updateVideos(@RequestBody YoutubeVideoApi videoApi) {
		User user = getCurrentUser();

		if (!user.getRoles().stream().map(Role::getName).anyMatch(ROLES::contains)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission!");
		}

		if (videoApi.getId() == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Video hasn't ID");
		}

		Optional<YoutubeVideo> oldVideo = youtubeVideosRepository.findById(videoApi.getId());

		if (!oldVideo.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Video with current ID is not exist");
		}

		YoutubeVideo video = oldVideo.get();

		video.setName(videoApi.getName());

		return ResponseEntity.status(HttpStatus.OK).body(new YoutubeVideoApi(youtubeVideosRepository.save(video)));
	}

	@Operation(summary = "Remove video")
	@SecurityRequirement(name = "Bearer Authentication")
	@DeleteMapping
	public ResponseEntity<?> removeVideo(@RequestParam String id) {
		User user = getCurrentUser();

		if (!user.getRoles().stream().map(Role::getName).anyMatch(ROLES::contains)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission!");
		}

		if (id.length() != 11) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Youtube video ID is incorrect!");
		}

		youtubeVideosRepository.deleteById(id);

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	private User getCurrentUser() {
		SecurityContext context = SecurityContextHolder.getContext();
		String userName = context.getAuthentication().getName();
		return userRepository.findByEmailOrUsername(userName, userName)
			.orElseThrow(() -> new UsernameNotFoundException(userName));
	}
}
