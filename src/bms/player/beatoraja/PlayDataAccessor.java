package bms.player.beatoraja;

import java.io.*;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.model.TimeLine;
import bms.player.beatoraja.gauge.GrooveGauge;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

/**
 * プレイデータアクセス用クラス
 * 
 * @author exch
 */
public class PlayDataAccessor {
	
	/**
	 * プレイヤー名
	 */
	private String player;
	/**
	 * スコアデータベースアクセサ
	 */
	private ScoreDatabaseAccessor scoredb;

	private static final String[] replay = {"", "C", "H"};

	public PlayDataAccessor(String player) {
		this.player = player;

		try {
			Class.forName("org.sqlite.JDBC");
			scoredb = new ScoreDatabaseAccessor(new File(".").getAbsoluteFile().getParent(), "/", "/");
			scoredb.createTable(player);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public PlayerData readPlayerData() {
		return scoredb.getPlayerDatas(player);
	}

	public void updatePlayerData(IRScoreData score, long time) {
		PlayerData pd = readPlayerData();
		pd.setEpg(pd.getEpg() + score.getEpg());
		pd.setLpg(pd.getLpg() + score.getLpg());
		pd.setEgr(pd.getEgr() + score.getEgr());
		pd.setLgr(pd.getLgr() + score.getLgr());
		pd.setEgd(pd.getEgd() + score.getEgd());
		pd.setLgd(pd.getLgd() + score.getLgd());
		pd.setEbd(pd.getEbd() + score.getEbd());
		pd.setLbd(pd.getLbd() + score.getLbd());
		pd.setEpr(pd.getEpr() + score.getEpr());
		pd.setLpr(pd.getLpr() + score.getLpr());
		pd.setEms(pd.getEms() + score.getEms());
		pd.setLms(pd.getLms() + score.getLms());

		pd.setPlaycount(pd.getPlaycount() + 1);
		if(score.getClear() > GrooveGauge.CLEARTYPE_FAILED) {
			pd.setClear(pd.getClear() + 1);
		}
		pd.setPlaytime(pd.getPlaytime() + time);
		scoredb.setPlayerData(player, pd);
	}

	/**
	 * スコアデータを読み込む
	 * @param model　対象のモデル
	 * @param lnmode LNモード
     * @return スコアデータ
     */
	public IRScoreData readScoreData(BMSModel model, int lnmode) {
		String hash = model.getSHA256();
		boolean ln = model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
				+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		return readScoreData(hash, ln, lnmode);
	}

	/**
	 * スコアデータを読み込む
	 * @param ln 対象のbmsがLNを含む場合はtrueを入れる
	 * @param lnmode LNモード
     * @return スコアデータ
     */
	public IRScoreData readScoreData(String hash, boolean ln, int lnmode) {
		return scoredb.getScoreData(player, hash, ln ? lnmode : 0, false);
	}

	/**
	 * スコアデータを書き込む
	 * @param newscore スコアデータ
	 * @param model　対象のモデル
	 * @param lnmode LNモード
	 * @param updateScore プレイ回数のみ反映する場合はfalse
     */
	public void writeScoreDara(IRScoreData newscore, BMSModel model, int lnmode, boolean updateScore) {
		String hash = model.getSHA256();
		boolean ln = model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
				+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		if (newscore == null) {
			return;
		}
		IRScoreData score = scoredb.getScoreData(player, hash, ln ? lnmode : 0, false);
		if (score == null) {
			score = new IRScoreData();
			score.setMode(ln ? lnmode : 0);
		}
		int clear = score.getClear();
		score.setSha256(hash);
		score.setNotes(model.getTotalNotes());

		if (newscore.getClear() > GrooveGauge.CLEARTYPE_FAILED) {
			score.setClearcount(score.getClearcount() + 1);
		}
		if (clear < newscore.getClear()) {
			score.setClear(newscore.getClear());
			score.setOption(newscore.getOption());
		}

		if (score.getExscore() < newscore.getExscore() && updateScore) {
			score.setEpg(newscore.getEpg());
			score.setLpg(newscore.getLpg());
			score.setEgr(newscore.getEgr());
			score.setLgr(newscore.getLgr());
			score.setEgd(newscore.getEgd());
			score.setLgd(newscore.getLgd());
			score.setEbd(newscore.getEbd());
			score.setLbd(newscore.getLbd());
			score.setEpr(newscore.getEpr());
			score.setLpr(newscore.getLpr());
			score.setEms(newscore.getEms());
			score.setLms(newscore.getLms());
		}
		if (score.getMinbp() > newscore.getMinbp() && updateScore) {
			score.setMinbp(newscore.getMinbp());
		}
		if (score.getCombo() < newscore.getCombo() && updateScore) {
			score.setCombo(newscore.getCombo());
		}
		score.setPlaycount(score.getPlaycount() + 1);
		score.setDate(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis() / 1000L);
		scoredb.setScoreData(player, score);

		int time = 0;
		for(TimeLine tl : model.getAllTimeLines()) {
			for(int i = 0;i < 18;i++) {
				if(tl.getNote(i) != null && tl.getNote(i).getState() != 0) {
					time =tl.getTime() / 1000;
				}
			}
		}
		updatePlayerData(newscore, time);
		Logger.getGlobal().info("スコアデータベース更新完了 ");

	}

	public IRScoreData readScoreData(String hash, boolean ln, int lnmode, int option) {
		return scoredb.getScoreData(player, hash, (ln ? lnmode : 0) + option * 10, false);
	}

	public IRScoreData readScoreData(BMSModel[] models, int lnmode, int option) {
		String[] hash = new String[models.length];
		boolean ln = false;
		for (int i = 0;i < models.length;i++) {
			hash[i] = models[i].getSHA256();
			ln |= models[i].getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
					+ models[i].getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		}
		return readScoreData(hash, ln, lnmode, option);
	}

	public IRScoreData readScoreData(String[] hashes, boolean ln, int lnmode, int option) {
		String hash = "";
		for (String s : hashes) {
			hash += s;
		}
		return readScoreData(hash, ln, lnmode, option);
	}

	public void writeScoreDara(IRScoreData newscore, BMSModel[] models, int lnmode, int option, boolean updateScore) {
		String hash = "";
		int totalnotes = 0;
		boolean ln = false;
		for (BMSModel model : models) {
			hash += model.getSHA256();
			totalnotes += model.getTotalNotes();
			ln |= model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
					+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		}
		if (newscore == null) {
			return;
		}
		IRScoreData score = scoredb.getScoreData(player, hash, (ln ? lnmode : 0) + option * 10, false);
		if (score == null) {
			score = new IRScoreData();
			score.setMode((ln ? lnmode : 0) + option * 10);
		}
		int clear = score.getClear();
		score.setSha256(hash);
		score.setNotes(totalnotes);

		if (newscore.getClear() != GrooveGauge.CLEARTYPE_FAILED) {
			score.setClearcount(score.getClearcount() + 1);
		}
		if (clear < newscore.getClear()) {
			score.setClear(newscore.getClear());
			score.setOption(newscore.getOption());
		}

		if (score.getExscore() < newscore.getExscore() && updateScore) {
			score.setEpg(newscore.getEpg());
			score.setLpg(newscore.getLpg());
			score.setEgr(newscore.getEgr());
			score.setLgr(newscore.getLgr());
			score.setEgd(newscore.getEgd());
			score.setLgd(newscore.getLgd());
			score.setEbd(newscore.getEbd());
			score.setLbd(newscore.getLbd());
			score.setEpr(newscore.getEpr());
			score.setLpr(newscore.getLpr());
			score.setEms(newscore.getEms());
			score.setLms(newscore.getLms());
		}
		if (score.getMinbp() > newscore.getMinbp() && updateScore) {
			score.setMinbp(newscore.getMinbp());
		}
		score.setPlaycount(score.getPlaycount() + 1);
		score.setDate(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis() / 1000L);
		scoredb.setScoreData(player, score);

		Logger.getGlobal().info("スコアデータベース更新完了 ");

	}

	public boolean existsReplayData(BMSModel model, int lnmode) {
		boolean ln = model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
				+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		return new File(this.getReplayDataFilePath(model.getSHA256(), ln, lnmode)).exists();
	}

	public boolean existsReplayData(String hash, boolean ln, int lnmode) {
		return new File(this.getReplayDataFilePath(hash, ln, lnmode)).exists();
	}

	public boolean existsReplayData(String[] hash, boolean ln, int lnmode) {
		String hashes = "";
		for(String s : hash) {
			hashes += s;
		}
		return new File(this.getReplayDataFilePath(hashes, ln, lnmode)).exists();
	}

	/**
	 * リプレイデータを読み込む
	 * @param model 対象のBMS
	 * @param lnmode LNモード
     * @return リプレイデータ
     */
	public ReplayData readReplayData(BMSModel model, int lnmode) {
		if (existsReplayData(model, lnmode)) {
			Json json = new Json();
			try {
				return json.fromJson(ReplayData.class,
						new FileReader(this.getReplayDataFilePath(model, lnmode)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * リプレイデータを書き込む
	 * @param rd リプレイデータ
	 * @param model 対象のBMS
	 * @param lnmode LNモード
     */
	public void wrireReplayData(ReplayData rd, BMSModel model, int lnmode) {
		File replaydir = new File("replay");
		if (!replaydir.exists()) {
			replaydir.mkdirs();
		}
		Json json = new Json();
		json.setOutputType(OutputType.json);
		try {
			FileWriter fw = new FileWriter(this.getReplayDataFilePath(model, lnmode));
			fw.write(json.prettyPrint(rd));
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public ReplayData[] readReplayData(BMSModel[] models, int lnmode) {
		String[] hashes = new String[models.length];
		boolean ln = false;
		for(int i = 0;i < models.length;i++) {
			hashes[i] = models[i].getSHA256();
			ln |= models[i].getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
					+ models[i].getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		}
		return this.readReplayData(hashes, ln, lnmode);
	}

	/**
	 * コースリプレイデータを読み込む
	 * @param hash 対象のBMSハッシュ群
	 * @param lnmode LNモード
	 * @return リプレイデータ
	 */
	public ReplayData[] readReplayData(String[] hash, boolean ln , int lnmode) {
		if (existsReplayData(hash, ln, lnmode)) {
			Json json = new Json();
			try {
				String hashes = "";
				for(String s : hash) {
					hashes += s;
				}
				return json.fromJson(ReplayData[].class,
						new FileReader(this.getReplayDataFilePath(hashes, ln, lnmode)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public void wrireReplayData(ReplayData[] rd, BMSModel[] models, int lnmode) {
		String[] hashes = new String[models.length];
		boolean ln = false;
		for(int i = 0;i < models.length;i++) {
			hashes[i] = models[i].getSHA256();
			ln |= models[i].getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
					+ models[i].getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		}
		this.wrireReplayData(rd, hashes, ln, lnmode);

	}

		/**
         * コースリプレイデータを書き込む
         * @param rd リプレイデータ
         * @param hash 対象のBMSハッシュ群
         * @param lnmode LNモード
         */
	public void wrireReplayData(ReplayData[] rd, String[] hash, boolean ln, int lnmode) {
		File replaydir = new File("replay");
		if (!replaydir.exists()) {
			replaydir.mkdirs();
		}
		Json json = new Json();
		json.setOutputType(OutputType.json);
		try {
			String hashes = "";
			for(String s : hash) {
				hashes += s;
			}
			FileWriter fw = new FileWriter(this.getReplayDataFilePath(hashes, ln, lnmode));
			fw.write(json.prettyPrint(rd));
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getReplayDataFilePath(BMSModel model, int lnmode) {
		boolean ln = model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
				+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		return getReplayDataFilePath(model.getSHA256(), ln, lnmode);
	}

	private String getReplayDataFilePath(String hash, boolean ln, int lnmode) {
		return "replay" + File.separatorChar +  (ln ? replay[lnmode] : "") + hash + ".json";
	}
}
