package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.Date;

import dk.digitalidentity.sofd.dao.model.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
	public CommentDTO(Comment c) {
		this.author = c.getUserName();
		this.comment = c.getComment();
		this.timestamp = c.getTimestamp();
	}

	private String author;
	private Date timestamp;
	private String comment;
}
