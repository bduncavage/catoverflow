package org.duncavage.catoverflow.util;

public class CatUtil {
	public static <T> void knuthShuffle(T[] list) {
		if (list.length < 2) {
			return;
		}
		
		for(int i = 0; i < list.length; i++) {
			int randIndex = (int)(Math.random() * (double)((list.length)));
			T tmp = list[randIndex];
			list[randIndex] = list[i];
			list[i] = tmp;
		}
	}
}
