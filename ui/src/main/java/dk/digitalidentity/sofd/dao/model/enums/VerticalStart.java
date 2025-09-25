package dk.digitalidentity.sofd.dao.model.enums;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

@Getter
public enum VerticalStart {
	NONE(0, "html.enum.verticalstart.none"),
	LEVEL1(1, "html.enum.verticalstart.1"),
	LEVEL2(2, "html.enum.verticalstart.2"),
	LEVEL3(3, "html.enum.verticalstart.3"),
	LEVEL4(4, "html.enum.verticalstart.4"),
	LEVEL5(5, "html.enum.verticalstart.5"),
	LEVEL6(6, "html.enum.verticalstart.6"),
	LEVEL7(7, "html.enum.verticalstart.7"),
	LEVEL8(8, "html.enum.verticalstart.8"),
	LEVEL9(9, "html.enum.verticalstart.9"),
	LEVEL10(10, "html.enum.verticalstart.10");
	
	private int level;
	private String messageId;
	private VerticalStart(int level, String messageId) {
		this.level = level;
		this.messageId = messageId;
	}


	public boolean isIncluded(int otherLevel) {
		if (this.level == 0) {
			return true;
		}
		return this.level >= otherLevel;
	}
	
	private static final Map<Integer, VerticalStart> map;
	static {
		map = new HashMap<Integer, VerticalStart>();
		for (VerticalStart v : VerticalStart.values()) {
			map.put(v.level, v);
		}
	}

	public static VerticalStart findByKey(int i) {
		return map.get(i);
	}

	public String getMessageId() {
		return messageId;
	}

}
