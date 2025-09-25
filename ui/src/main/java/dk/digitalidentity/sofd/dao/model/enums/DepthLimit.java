package dk.digitalidentity.sofd.dao.model.enums;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

@Getter
public enum DepthLimit {
	NONE(0, "html.enum.depthlimit.none"),
	LEVEL1(1, "html.enum.depthlimit.1"),
	LEVEL2(2, "html.enum.depthlimit.2"),
	LEVEL3(3, "html.enum.depthlimit.3"),
	LEVEL4(4, "html.enum.depthlimit.4"),
	LEVEL5(5, "html.enum.depthlimit.5"),
	LEVEL6(6, "html.enum.depthlimit.6"),
	LEVEL7(7, "html.enum.depthlimit.7"),
	LEVEL8(8, "html.enum.depthlimit.8"),
	LEVEL9(9, "html.enum.depthlimit.9"),
	LEVEL10(10, "html.enum.depthlimit.10");
	
	private int level;
	private String messageId;

	private DepthLimit(int level, String messageId) {
		this.level = level;
		this.messageId = messageId;
	}
	
	public boolean isIncluded(int otherLevel) {
		if (this.level == 0) {
			return true;
		}
		return this.level >= otherLevel;
	}

	private static final Map<Integer, DepthLimit> map;
	static {
		map = new HashMap<Integer, DepthLimit>();
		for (DepthLimit v : DepthLimit.values()) {
			map.put(v.level, v);
		}
	}

	public static DepthLimit findByKey(int i) {
		return map.get(i);
	}

	public String getMessageId() {
		return messageId;
	}
}
