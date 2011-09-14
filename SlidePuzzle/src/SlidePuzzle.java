import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * GDD2011Japan DevQuiz
 * スライドパズル回答用ソース
 * @author ryosms
 */
public class SlidePuzzle {
	private static final int MOVE_UP = 0;
	private static final int MOVE_LEFT = 1;
	private static final int MOVE_RIGHT = 2;
	private static final int MOVE_DOWN = 3;
	private static final int MOVE_KIND = 4;
	private static final String[] MOVE_KIND_ASC = {"U", "L", "R", "D"};
	private static final String[] MOVE_KIND_DESC = {"D", "R", "L", "U"};
	// TODO ヒープサイズの設定に合わせてOOMが出ないように変更する
//	private static final long MAX_SAVE_COUNT = 1200000;		// -Xmx3072M 以上
	private static final long MAX_SAVE_COUNT = 100000;		// デフォルト（-Xmx128M？）

	public static void main(String[] args) {
		// TODO 動作環境（入出力ファイルのパス・実行パス）を変える場合は要修正（↓はEclipseで実行する場合）
		String filePath = "SlidePuzzle.question";
		String resultPath = "SlidePuzzle.result";
		String path = new File(".").getAbsoluteFile().getParent();
		if(!path.endsWith(File.separator)) {
			path = path + File.separator;
		}
		path = path + "res";
		path = path + File.separator;

		FileReader fileReader = null;
		BufferedReader buffer = null;
		int[] moveLimit = {0, 0, 0, 0};
		int dataCount = 0;
		try {
			fileReader = new FileReader(path + filePath);
			buffer = new BufferedReader(fileReader);
			String limit = buffer.readLine();
			String limits[] = limit.split(" ");
			moveLimit[MOVE_LEFT] = Integer.valueOf(limits[0]);
			moveLimit[MOVE_RIGHT] = Integer.valueOf(limits[1]);
			moveLimit[MOVE_UP] = Integer.valueOf(limits[2]);
			moveLimit[MOVE_DOWN] = Integer.valueOf(limits[3]);

			String count = buffer.readLine();
			dataCount = Integer.valueOf(count);
		} catch(IOException ex) {
			System.out.println("file read error");
			return;
		}

		File file = new File(path + resultPath);
		PrintWriter writer = null;
		FileWriter fileWriter = null;
		BufferedWriter bw = null;
		try {
			fileWriter = new FileWriter(file);
			bw = new BufferedWriter(fileWriter);
			writer = new PrintWriter(bw);
		} catch(IOException ex) {
			System.out.println("writer error");
			return;
		}

		// TODO ここで数値をいじれば最初のn件だけ処理できる
//		dataCount = 10;
		int clearCount = 0;
		int moveCount[] = {0, 0, 0, 0};
		Runtime runtime = Runtime.getRuntime();
		for(int i = 0; i < dataCount; i++) {
			String input = "";
			try {
				input = buffer.readLine();
			} catch(IOException ex) {
				System.out.println("file read error");
				return;
			}
			System.out.print(String.format("%4d:", i+1));
			String result = "";
			Solver solver = new Solver(input);
			// TODO 以下は全ソルバーを使用する場合のサンプル
			// 提出した回答はsolve2〜solve4を複合使用
			// まずは最短手順で探索
			result = solver.solve();
			if(result.length() == 0) {
				// 解けていない場合は軽い枝刈り
				result = solver.solve2();
			}
			if(result.length() == 0 && solver.canReboard()) {
				// 解けていない && ボードの再生成が可能な場合
				result = solver.solve3();
			}
			if(result.length() == 0) {
				// 最終的にはかなり大胆なボードの再生成をする
				result = solver.solve4();
			}
			writer.println(result);
			long memory = runtime.totalMemory() / 1024 / 1024;
			if(result.length() == 0) {
				System.out.println(String.format(" Skip...%dMB", memory));
			} else {
				clearCount++;
				System.out.println(String.format(" Clear! %dsteps(%dclears) %dMB", result.length(), clearCount, memory));
				int[] tmpMove = getMoveCounts(result);
				moveCount[MOVE_UP] += tmpMove[MOVE_UP];
				moveCount[MOVE_DOWN] += tmpMove[MOVE_DOWN];
				moveCount[MOVE_LEFT] += tmpMove[MOVE_LEFT];
				moveCount[MOVE_RIGHT] += tmpMove[MOVE_RIGHT];
			}
		}

		try {
			buffer.close();
			fileReader.close();
			writer.close();
			bw.close();
			fileWriter.close();
		} catch(IOException ex) {
		}

		System.out.println(String.format("clear:%d /%d, up:%d / %d, down:%d / %d, left:%d / %d, right:%d / %d"
										, clearCount, dataCount
										, moveCount[MOVE_UP], moveLimit[MOVE_UP]
										, moveCount[MOVE_DOWN], moveLimit[MOVE_DOWN]
										, moveCount[MOVE_LEFT], moveLimit[MOVE_LEFT]
										, moveCount[MOVE_RIGHT], moveLimit[MOVE_RIGHT]));
	}

	private static int[] getMoveCounts(String resultCode) {
		int[] ret = {0, 0, 0, 0};
		int length = resultCode.length();
		for(int i = 0; i < length; i++) {
			switch(resultCode.charAt(i)) {
				case 'U' :
					ret[MOVE_UP]++;
					break;
				case 'L' :
					ret[MOVE_LEFT]++;
					break;
				case 'R' :
					ret[MOVE_RIGHT]++;
					break;
				case 'D' :
					ret[MOVE_DOWN]++;
					break;
			}
		}

		return ret;
	}

	private static class Solver {
		private final int width;
		private final int height;
		private final String line;
		private boolean canReboard = false;
		private int matchCount = 0;
		private int verticalMatch = 0;

		public Solver(String input) {
			String[] params = input.split(",");
			width = Integer.valueOf(params[0]);
			height = Integer.valueOf(params[1]);
			line = params[2];
			System.out.print(String.format("%d x %d(%s)", width, height, line));
		}

		public boolean canReboard() { return this.canReboard; }

		// 特に対策なし（ただし最短手順になるはず）のソルバー
		public String solve() {
			HashMap<String, String> ascHash = new HashMap<String, String>();
			HashMap<String, String> descHash = new HashMap<String, String>();

			Board board = new Board(width, height, line);
			String clear = board.getClearLine();

			ascHash.put(line, "");
			descHash.put(clear, "");

			ArrayList<String> ascParent = new ArrayList<String>();
			ArrayList<String> descParent = new ArrayList<String>();
			ArrayList<String> ascNext = new ArrayList<String>();
			ArrayList<String> descNext = new ArrayList<String>();
			ascNext.add(line);
			descNext.add(clear);

			long ascCount = 0;
			long descCount = 0;
			long tmp = 10;
			for(;;) {
				if((ascHash.size() > MAX_SAVE_COUNT && descHash.size() > MAX_SAVE_COUNT)
						|| (ascHash.size() > MAX_SAVE_COUNT && descNext.size() == 0)
						|| (ascNext.size() == 0 && descHash.size() > MAX_SAVE_COUNT)) {
					System.out.print(String.format(" ascHash = %d, descHash = %d(count = %d)"
									, ascHash.size(), descHash.size(), ascCount + descCount));
					break;
				}
				if(ascNext.size() == 0 && descNext.size() == 0) {
					System.out.print(" zero... ");
					break;
				}
				if((ascCount + descCount) > 1000) {
					System.out.print(" count over... ");
					break;
				}
				if((ascCount + descCount) > tmp) {
					System.out.print(" *");
					tmp +=  10;
				}
				if(ascHash.size() <= MAX_SAVE_COUNT) {
					ascCount++;
					ascParent.clear();
					ascParent.addAll(ascNext);
					ascNext.clear();
					for(int i = 0; i < ascParent.size(); i++) {
						String parent = ascParent.get(i);
						String parentMove = ascHash.get(parent);
						for(int j = 0; j < MOVE_KIND; j++) {
							board.setBoard(parent);
							String moveKind = parentMove + MOVE_KIND_ASC[j];
							if(board.move(j)) {
								String nextLine = board.getLinearCode();
								if(nextLine.equals(clear)) {
									return moveKind;
								} else if(descHash.containsKey(nextLine)) {
									return moveKind + descHash.get(nextLine);
								} else if(!ascHash.containsKey(nextLine)) {
									ascHash.put(nextLine, moveKind);
									ascNext.add(nextLine);
								}
							}
						}
					}

				}

				if(descHash.size() < MAX_SAVE_COUNT) {
					descCount++;
					descParent.clear();
					descParent.addAll(descNext);
					descNext.clear();
					for(int i = 0; i < descParent.size(); i++) {
						String parent = descParent.get(i);
						String parentMove = descHash.get(parent);
						for(int j = 0; j < MOVE_KIND; j++) {
							board.setBoard(parent);
							String moveKind = MOVE_KIND_DESC[j] + parentMove;
							if(board.move(j)) {
								String nextLine = board.getLinearCode();
								if(nextLine.equals(line)) {
									return moveKind;
								} else if(ascHash.containsKey(nextLine)) {
									return ascHash.get(nextLine) + moveKind;
								} else if(!descHash.containsKey(nextLine)) {
									descHash.put(nextLine, moveKind);
									descNext.add(nextLine);
								}
							}
						}
					}
				}

			}

			return "";
		}

		// 簡単な枝刈りを入れただけのソルバー
		public String solve2() {
			HashMap<String, String> ascHash = new HashMap<String, String>();
			HashMap<String, String> descHash = new HashMap<String, String>();

			Board board = new Board(width, height, line);
			String clear = board.getClearLine();
			matchCount = board.getMatchCount();

			ascHash.put(line, "");
			descHash.put(clear, "");

			ArrayList<String> ascParent = new ArrayList<String>();
			ArrayList<String> descParent = new ArrayList<String>();
			ArrayList<String> ascNext = new ArrayList<String>();
			ArrayList<String> descNext = new ArrayList<String>();
			ascNext.add(line);
			descNext.add(clear);

			long ascCount = 0;
			long descCount = 0;
			long tmp = 10;
			for(;;) {
				if((ascHash.size() > MAX_SAVE_COUNT && descHash.size() > MAX_SAVE_COUNT)
						|| (ascHash.size() > MAX_SAVE_COUNT && descNext.size() == 0)
						|| (ascNext.size() == 0 && descHash.size() > MAX_SAVE_COUNT)) {
					System.out.print(String.format(" ascHash = %d, descHash = %d(count = %d)"
									, ascHash.size(), descHash.size(), ascCount + descCount));
					break;
				}
				if(ascNext.size() == 0 && descNext.size() == 0) {
					System.out.print(" zero... ");
					break;
				}
				if((ascCount + descCount) > 1000) {
					System.out.print(" count over... ");
					break;
				}
				if((ascCount + descCount) > tmp) {
					System.out.print(" *");
					tmp +=  10;
				}
				if(ascHash.size() <= MAX_SAVE_COUNT) {
					ascCount++;
					ascParent.clear();
					ascParent.addAll(ascNext);
					ascNext.clear();
					for(int i = 0; i < ascParent.size(); i++) {
						String parent = ascParent.get(i);
						String parentMove = ascHash.get(parent);
						for(int j = 0; j < MOVE_KIND; j++) {
							board.setBoard(parent);
							String moveKind = parentMove + MOVE_KIND_ASC[j];
							if(board.move(j)) {
								String nextLine = board.getLinearCode();
								if(nextLine.equals(clear)) {
									return moveKind;
								} else if(descHash.containsKey(nextLine)) {
									return moveKind + descHash.get(nextLine);
								} else if(!ascHash.containsKey(nextLine)) {
									int match = board.getMatchCount();
									if(match < matchCount - 1) continue;
									if(match > matchCount) matchCount = match;
									ascHash.put(nextLine, moveKind);
									ascNext.add(nextLine);
									if(!this.canReboard && board.isReBoard()) {
										canReboard = true;
									}
								}
							}
						}
					}

				}

				if(descHash.size() < MAX_SAVE_COUNT) {
					descCount++;
					descParent.clear();
					descParent.addAll(descNext);
					descNext.clear();
					for(int i = 0; i < descParent.size(); i++) {
						String parent = descParent.get(i);
						String parentMove = descHash.get(parent);
						for(int j = 0; j < MOVE_KIND; j++) {
							board.setBoard(parent);
							String moveKind = MOVE_KIND_DESC[j] + parentMove;
							if(board.move(j)) {
								String nextLine = board.getLinearCode();
								if(nextLine.equals(line)) {
									return moveKind;
								} else if(ascHash.containsKey(nextLine)) {
									return ascHash.get(nextLine) + moveKind;
								} else if(!descHash.containsKey(nextLine)) {
									descHash.put(nextLine, moveKind);
									descNext.add(nextLine);
								}
							}
						}
					}
				}

			}

			return "";
		}

		// 途中で行・列が揃ったら消していくタイプのソルバー
		public String solve3() {
			HashMap<String, String> ascHash = new HashMap<String, String>();
			HashMap<String, String> descHash = new HashMap<String, String>();

			Board board = new Board(width, height, line);
			String clear = board.getClearLine();
			matchCount = board.getMatchCount();

			ascHash.put(line, "");
			descHash.put(clear, "");

			ArrayList<String> ascParent = new ArrayList<String>();
			ArrayList<String> descParent = new ArrayList<String>();
			ArrayList<String> ascNext = new ArrayList<String>();
			ArrayList<String> descNext = new ArrayList<String>();
			ascNext.add(line);
			descNext.add(clear);

			long ascCount = 0;
			long descCount = 0;
			long tmp = 10;
			for(;;) {
				if((ascHash.size() > MAX_SAVE_COUNT && descHash.size() > MAX_SAVE_COUNT)
						|| (ascHash.size() > MAX_SAVE_COUNT && descNext.size() == 0)
						|| (ascNext.size() == 0 && descHash.size() > MAX_SAVE_COUNT)) {
					System.out.print(String.format(" ascHash = %d, descHash = %d(count = %d)"
									, ascHash.size(), descHash.size(), ascCount + descCount));
					break;
				}
				if(ascNext.size() == 0 && descNext.size() == 0) {
					System.out.print(" zero... ");
					break;
				}
				if((ascCount + descCount) > 1000) {
					System.out.print(" count over... ");
					break;
				}
				if((ascCount + descCount) > tmp) {
					System.out.print(" *");
					tmp +=  10;
				}
				if(ascHash.size() <= MAX_SAVE_COUNT) {
					ascCount++;
					ascParent.clear();
					ascParent.addAll(ascNext);
					ascNext.clear();
					for(int i = 0; i < ascParent.size(); i++) {
						String parent = ascParent.get(i);
						String parentMove = ascHash.get(parent);
						for(int j = 0; j < MOVE_KIND; j++) {
							board.setBoard(parent);
							String moveKind = parentMove + MOVE_KIND_ASC[j];
							if(board.move(j)) {
								String nextLine = board.getLinearCode();
								if(nextLine.equals(clear)) {
									return moveKind;
								} else if(descHash.containsKey(nextLine)) {
									return moveKind + descHash.get(nextLine);
								} else if(!ascHash.containsKey(nextLine)) {
									int match = board.getMatchCount();
									if(match < matchCount - 1) continue;
									if(match > matchCount) matchCount = match;
									ascHash.put(nextLine, moveKind);
									ascNext.add(nextLine);
									if(board.reBoard() > 0) {
										ascHash.clear();
										descHash.clear();
										ascNext.clear();
										ascParent.clear();
										descNext.clear();
										descParent.clear();
										String newLine = board.getLinearCode();
										System.out.print(String.format("\n          (%s)", newLine));
										ascHash.put(newLine, moveKind);
										ascNext.add(newLine);
										String newClear = board.getClearLine();
										descHash.put(newClear, "");
										descNext.add(newClear);
										descCount = 0;
										break;
									}
								}

							}
						}
					}

				}

				if(descHash.size() < MAX_SAVE_COUNT) {
					descCount++;
					descParent.clear();
					descParent.addAll(descNext);
					descNext.clear();
					for(int i = 0; i < descParent.size(); i++) {
						String parent = descParent.get(i);
						String parentMove = descHash.get(parent);
						for(int j = 0; j < MOVE_KIND; j++) {
							board.setBoard(parent);
							String moveKind = MOVE_KIND_DESC[j] + parentMove;
							if(board.move(j)) {
								String nextLine = board.getLinearCode();
								if(nextLine.equals(line)) {
									return moveKind;
								} else if(ascHash.containsKey(nextLine)) {
									return ascHash.get(nextLine) + moveKind;
								} else if(!descHash.containsKey(nextLine)) {
									descHash.put(nextLine, moveKind);
									descNext.add(nextLine);
								}
							}
						}
					}
				}

			}

			return "";
		}

		// 一致した箇所が増えたらそこから再探索するタイプのソルバー
		public String solve4() {
			HashMap<String, String> ascHash = new HashMap<String, String>();
			HashMap<String, String> descHash = new HashMap<String, String>();

			Board board = new Board(width, height, line);
			String clear = board.getClearLine();
			matchCount = board.getMatchCount();
			verticalMatch = board.getVerticalMatchCount();

			ascHash.put(line, "");
			descHash.put(clear, "");

			ArrayList<String> ascParent = new ArrayList<String>();
			ArrayList<String> descParent = new ArrayList<String>();
			ArrayList<String> ascNext = new ArrayList<String>();
			ArrayList<String> descNext = new ArrayList<String>();
			ascNext.add(line);
			descNext.add(clear);

			long ascCount = 0;
			long descCount = 0;
			long tmp = 10;
			for(;;) {
				if((ascHash.size() > MAX_SAVE_COUNT && descHash.size() > MAX_SAVE_COUNT)
						|| (ascHash.size() > MAX_SAVE_COUNT && descNext.size() == 0)
						|| (ascNext.size() == 0 && descHash.size() > MAX_SAVE_COUNT)) {
					System.out.print(String.format(" ascHash = %d, descHash = %d(count = %d)"
									, ascHash.size(), descHash.size(), ascCount + descCount));
					break;
				}
				if(ascNext.size() == 0 && descNext.size() == 0) {
					System.out.print(" zero... ");
				}
				if((ascCount + descCount) > 1000) {
					System.out.print(" count over... ");
					break;
				}
				if((ascCount + descCount) > tmp) {
					System.out.print(" *");
					tmp +=  10;
				}
				if(ascHash.size() <= MAX_SAVE_COUNT) {
					ascCount++;
					ascParent.clear();
					ascParent.addAll(ascNext);
					ascNext.clear();
					for(int i = 0; i < ascParent.size(); i++) {
						String parent = ascParent.get(i);
						String parentMove = ascHash.get(parent);
						for(int j = 0; j < MOVE_KIND; j++) {
							board.setBoard(parent);
							String moveKind = parentMove + MOVE_KIND_ASC[j];
							if(board.move(j)) {
								String nextLine = board.getLinearCode();
								if(nextLine.equals(clear)) {
									return moveKind;
								} else if(descHash.containsKey(nextLine)) {
									return moveKind + descHash.get(nextLine);
								} else if(!ascHash.containsKey(nextLine)) {
									int horizontal = board.getMatchCount();
									int vertical = board.getMatchCount();
									if(horizontal < matchCount - 1 || vertical < verticalMatch - 1) continue;
									if(horizontal > matchCount || vertical > verticalMatch) {
										matchCount = horizontal;
										verticalMatch = vertical;
										ascHash.clear();
										descHash.clear();
										ascNext.clear();
										ascParent.clear();
										descNext.clear();
										descParent.clear();
										ascHash.put(nextLine, moveKind);
										ascNext.add(nextLine);
										String newClear = board.getClearLine();
										descHash.put(newClear, "");
										descNext.add(newClear);
										descCount = 0;
										break;
									}
									ascHash.put(nextLine, moveKind);
									ascNext.add(nextLine);
								}

							}
						}
					}

				}

				if(descHash.size() < MAX_SAVE_COUNT) {
					descCount++;
					descParent.clear();
					descParent.addAll(descNext);
					descNext.clear();
					for(int i = 0; i < descParent.size(); i++) {
						String parent = descParent.get(i);
						String parentMove = descHash.get(parent);
						for(int j = 0; j < MOVE_KIND; j++) {
							board.setBoard(parent);
							String moveKind = MOVE_KIND_DESC[j] + parentMove;
							if(board.move(j)) {
								String nextLine = board.getLinearCode();
								if(nextLine.equals(line)) {
									return moveKind;
								} else if(ascHash.containsKey(nextLine)) {
									return ascHash.get(nextLine) + moveKind;
								} else if(!descHash.containsKey(nextLine)) {
									descHash.put(nextLine, moveKind);
									descNext.add(nextLine);
								}
							}
						}
					}
				}

			}

			return "";
		}
	
	
	}


 	private static class Board {
		private static final char[] CLEAR = {
			'1', '2', '3', '4', '5', '6', '7', '8', '9'
			, 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'
			, 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R'
			, 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', ' '
		};
		private int width;
		private int height;
		public char[][] board;
		private int zeroW = 0;
		private int zeroH = 0;
		private char[][] clear;

		public Board(int width, int height, String linear) {
			this.width = width;
			this.height = height;
			board = new char[height][width];
			setBoard(linear);
			clear = new char[height][width];
			setClear();
		}

		public void setBoard(String linear) {
			for(int i = 0; i < height; i++) {
				for(int j = 0; j < width; j++) {
					char cell = linear.charAt((i * width) + j);
					board[i][j] = cell;
					if(cell == '0' || cell == ' ') {
						zeroW = j;
						zeroH = i;
						board[i][j] = ' ';
					}
				}
			}
		}

		public boolean move(int moveKind) {
			boolean isMoved = false;
			switch(moveKind) {
				case MOVE_UP :
					if(zeroH > 0 && board[zeroH - 1][zeroW] != '=') {
						board[zeroH][zeroW] = board[zeroH - 1][zeroW];
						zeroH--;
						board[zeroH][zeroW] = ' ';
						isMoved = true;
					}
					break;
				case MOVE_LEFT :
					if(zeroW > 0 && board[zeroH][zeroW - 1] != '=') {
						board[zeroH][zeroW] = board[zeroH][zeroW - 1];
						zeroW--;
						board[zeroH][zeroW] = ' ';
						isMoved = true;
					}
					break;
				case MOVE_RIGHT :
					if(zeroW < width - 1 && board[zeroH][zeroW + 1] != '=') {
						board[zeroH][zeroW] = board[zeroH][zeroW + 1];
						zeroW++;
						board[zeroH][zeroW] = ' ';
						isMoved = true;
					}
					break;
				case MOVE_DOWN :
					if(zeroH < height - 1 && board[zeroH + 1][zeroW] != '='){
						board[zeroH][zeroW] = board[zeroH + 1][zeroW];
						zeroH++;
						board[zeroH][zeroW] = ' ';
						isMoved = true;
					}
					break;
			}

			return isMoved;
		}

		public String getLinearCode() {
			StringBuilder linear = new StringBuilder("");
			for(int i = 0; i < height; i++) {
				for(int j = 0; j < width; j++) {
					linear.append(board[i][j]);
				}
			}
			String ret = linear.toString();
			linear = null;
			return ret;
		}

		public String getClearLine() {
			StringBuilder clear = new StringBuilder("");
			for(int i = 0; i < height; i++) {
				for(int j = 0; j < width; j++) {
					clear.append(this.clear[i][j]);
				}
			}
			return clear.toString();
		}

		private void setClear() {
			for(int i = 0; i < height; i++) {
				for(int j = 0; j < width; j++) {
					if(board[i][j] == '=') {
						clear[i][j] = '=';
					} else {
						clear[i][j] = CLEAR[(i * width) + j];
					}
				}
			}
			clear[height - 1][width - 1] = ' ';
		}

		private int verticalLine = 0;
		public boolean checkVerticalLine() {
			if(width - verticalLine <= 3) return false;

			for(int i = 0; i < height; i++) {
				if(board[i][verticalLine] != '=' && board[i][verticalLine] != clear[i][verticalLine]) return false;
			}

			for(int i = 0; i < height; i++) {
				board[i][verticalLine] = '=';
			}

			// FIXME 全パネルが移動可能かチェックする
			// 本来ならここで
			// = = =
			// = n =
			// = = =
			// のようなことにならないようにチェックをする必要がありそうだが、無視してる

			verticalLine++;
			setClear();

			return true;
		}

		private int horizontalLine = 0;
		public boolean checkHorizontailLine() {
			if(height - horizontalLine <= 3) return false;
			for(int i = 0; i < width; i++) {
				if(board[horizontalLine][i] != '=' && board[horizontalLine][i] != clear[horizontalLine][i]) return false;
			}
			for(int i = 0; i < width; i++) {
				board[horizontalLine][i] = '=';
			}

			// FIXME 全パネルが移動可能かチェックする
			// checkVerticalLine() と同様

			horizontalLine++;
			setClear();
			return true;
		}

		private boolean canReBoard = false;
		public boolean isReBoard() {
			if(canReBoard) return true;
			if(!canVeticalReBoard() && !canHorizontalReboard()) return false;
			canReBoard = true;
			return false;
		}
		private boolean canVeticalReBoard() {
			if(width - verticalLine <= 3) return false;
			for(int i = 0; i < height; i++) {
				if(board[i][verticalLine] != '=' && board[i][verticalLine] != clear[i][verticalLine]) return false;
			}
			return true;
		}

		private boolean canHorizontalReboard() {
			if(height - horizontalLine <= 3) return false;
			for(int i = 0; i < width; i++) {
				if(board[horizontalLine][i] != '=' && board[horizontalLine][i] != clear[horizontalLine][i]) return false;
			}
			return true;
		}

		public int reBoard() {
			int ret = 0;
			while(true) {
				if(!checkVerticalLine()) break;
				ret++;
			}
			while(true) {
				if(!checkHorizontailLine()) break;
				ret++;
			}
			return ret;
		}

		public int getMatchCount() {
			int match = 0;
			for(int i = 0; i < height; i++) {
				for(int j = 0; j < width; j++) {
					if(board[i][j] != '=' && board[i][j] != clear[i][j]) return match;
					match++;
				}
			}
			return match;
		}
		public int getVerticalMatchCount() {
			int match = 0;
			for(int i = 0; i < width; i++) {
				for(int j = 0; j < height; j++) {
					if(board[j][i] != '=' && board[j][i] != clear[j][i]) return match;
					match++;
				}
			}
			return match;
		}
	}

}
