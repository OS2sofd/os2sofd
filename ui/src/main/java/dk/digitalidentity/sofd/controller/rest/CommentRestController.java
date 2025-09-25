package dk.digitalidentity.sofd.controller.rest;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.mvc.dto.CommentDTO;
import dk.digitalidentity.sofd.dao.model.Comment;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.security.RequireControllerWriteAccess;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.CommentService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.UserService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequireControllerWriteAccess
@RestController
public class CommentRestController {

	@Autowired
	private PersonService personService;
	
	@Autowired
	private CommentService commentService;

	@Autowired
	private UserService userService;

	@PostMapping(value = "/rest/person/add/comment")
	@ResponseBody
	public HttpEntity<CommentDTO> addComment(@RequestHeader("uuid") String uuid, @RequestBody String commentText) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		Comment newComment = new Comment();
		newComment.setPersonUuid(person.getUuid());
		newComment.setTimestamp(new Date());
		newComment.setComment(commentText);

		String userId = SecurityUtil.getUser();
		if (userId != null) {
			User user = userService.findByUserIdAndUserType(userId, SupportedUserTypeService.getActiveDirectoryUserType());

			if (user != null) {
				Person authorPerson = personService.findByUser(user);

				newComment.setUserId(user.getId());
				newComment.setUserName(authorPerson.getFirstname() + " " + authorPerson.getSurname());
			}
			else {
				newComment.setUserId(0);
				newComment.setUserName(userId);
			}
		}
		else {
			log.warn("Unknown user tried to add comment!");

			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		commentService.save(newComment);

		CommentDTO commentDTO = CommentDTO.builder()
				.author(newComment.getUserName())
				.comment(newComment.getComment())
				.timestamp(newComment.getTimestamp())
				.build();

		return new ResponseEntity<>(commentDTO, HttpStatus.OK);
	}
}
