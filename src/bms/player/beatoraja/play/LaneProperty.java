package bms.player.beatoraja.play;

import bms.model.Mode;

public class LaneProperty {

	/**
	 * 入力キーからレーンへの対応
	 */
	private final int[] keyToLane;

	/**
	 * レーンから入力キー（複数）への対応
	 */
	private final int[][] laneToKey;

	/**
	 * レーンが何個目のスクラッチか
	 */
	private final int[] laneToScratch;

	/**
	 * レーンからスキンに使用する番号への対応
	 */
	private final int[] laneToSkinOffset;

	/**
	 * レーンからプレイヤー番号への対応
	 */
	private final int[] laneToPlayer;

	/**
	 * 各スクラッチを処理する入力キー（2個ずつ）
	 */
	private final int[][] scratchToKey;

	public LaneProperty(Mode mode) {
		switch (mode) {
		case BEAT_5K:
			keyToLane = new int[] { 0, 1, 2, 3, 4, -1, -1, 5, 5 };
			laneToKey = new int[][] { {0}, {1}, {2}, {3}, {4}, {7,8} };
			scratchToKey = new int[][] { {7,8} };
			break;
		case BEAT_7K:
			keyToLane = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 7 };
			laneToKey = new int[][] { {0}, {1}, {2}, {3}, {4}, {5}, {6}, {7,8} };
			scratchToKey = new int[][] { {7,8} };
			break;
		case BEAT_10K:
			keyToLane = new int[] { 0, 1, 2, 3, 4, -1, -1, 5, 5, 6, 7, 8, 9, 10, -1, -1, 11, 11 };
			laneToKey = new int[][] { {0}, {1}, {2}, {3}, {4}, {7,8}, {9}, {10}, {11}, {12}, {13}, {16,17} };
			scratchToKey = new int[][] { {7,8}, {16,17} };
			break;
		case BEAT_14K:
			keyToLane = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 7, 8, 9, 10, 11, 12, 13, 14, 15, 15 };
			laneToKey = new int[][] { {0}, {1}, {2}, {3}, {4}, {5}, {6}, {7,8}, {9}, {10}, {11}, {12}, {13}, {14}, {15}, {16,17} };
			scratchToKey = new int[][] { {7,8}, {16,17} };
			break;
		case POPN_9K:
			keyToLane = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
			laneToKey = new int[][] { {0}, {1}, {2}, {3}, {4}, {5}, {6}, {7}, {8} };
			scratchToKey = new int[][] { };
			break;
		default:
			keyToLane = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 7 };
			laneToKey = new int[][] { {0}, {1}, {2}, {3}, {4}, {5}, {6}, {7,8} };
			scratchToKey = new int[][] { {7,8} };
			break;
		}
		laneToScratch = new int[mode.key];
		laneToSkinOffset = new int[mode.key];
		laneToPlayer = new int[mode.key];
		for(int i = 0, sc = 0; i < laneToSkinOffset.length; i++) {
			laneToPlayer[i] = i / (mode.key / mode.player);
			if(mode.isScratchKey(i)) {
				laneToSkinOffset[i] = 0;
				laneToScratch[i] = sc;
				sc++;
			} else {
				laneToSkinOffset[i] = i % (mode.key / mode.player) + 1;
				laneToScratch[i] = -1;
			}
		}
	}

	public int[] getKeyLaneAssign() {
		return keyToLane;
	}

	public int[][] getLaneKeyAssign() {
		return laneToKey;
	}

	public int[] getLaneScratchAssign() {
		return laneToScratch;
	}

	public int[] getLaneSkinOffset() {
		return laneToSkinOffset;
	}

	public int[] getLanePlayer() {
		return laneToPlayer;
	}

	public int[][] getScratchKeyAssign() {
		return scratchToKey;
	}
}