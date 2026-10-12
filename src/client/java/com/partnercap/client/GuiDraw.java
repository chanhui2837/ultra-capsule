package com.partnercap.client;

import java.lang.reflect.Method;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 관리창 장식용 사각형 그리기.
 * 26.3의 GuiGraphicsExtractor에서 fill(int,int,int,int,int)을 리플렉션으로 찾아 쓴다.
 * 메서드를 못 찾으면 장식만 생략되고 화면 기능(버튼·미리보기)은 그대로 동작한다.
 */
final class GuiDraw {

	private static boolean init;
	private static Method fill;
	private static boolean broken;

	private GuiDraw() {
	}

	private static void ensure() {
		if (init) {
			return;
		}
		init = true;
		for (Method m : GuiGraphicsExtractor.class.getMethods()) {
			if (!m.getName().equals("fill")) {
				continue;
			}
			Class<?>[] p = m.getParameterTypes();
			if (p.length == 5 && p[0] == int.class && p[1] == int.class && p[2] == int.class
					&& p[3] == int.class && p[4] == int.class) {
				fill = m;
				return;
			}
		}
	}

	/** 사각형 채우기 (x1,y1 포함 ~ x2,y2 미포함), ARGB */
	static void fill(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int argb) {
		if (broken || x2 <= x1 || y2 <= y1) {
			return;
		}
		ensure();
		if (fill == null) {
			return;
		}
		try {
			fill.invoke(g, x1, y1, x2, y2, argb);
		} catch (Exception e) {
			broken = true;
		}
	}

	/** 1px 테두리 */
	static void border(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int argb) {
		fill(g, x1, y1, x2, y1 + 1, argb);
		fill(g, x1, y2 - 1, x2, y2, argb);
		fill(g, x1, y1 + 1, x1 + 1, y2 - 1, argb);
		fill(g, x2 - 1, y1 + 1, x2, y2 - 1, argb);
	}
}
