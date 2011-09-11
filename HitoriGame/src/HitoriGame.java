import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * GDD2011Japan DevQuiz
 * 一人ゲーム回答用ソース
 * @author ryosms
 */
public class HitoriGame {
	private static final String filePath = "HitoriGame.question";
	private static final String resultPath = "HitoriGame.result";

	public static void main(String[] args) {
		// TODO 動作環境を変える場合は要修正（↓はEclipseで実行する場合）
		String path = new File(".").getAbsoluteFile().getParent();
		if(!path.endsWith(File.separator)) {
			path = path + File.separator;
		}
		path = path + "res";
		path = path + File.separator;
		int dataCount = 0;
		FileReader fileReader = null;
		BufferedReader buffer = null;
		try {
			fileReader = new FileReader(path + filePath);
			buffer = new BufferedReader(fileReader);
			String count = buffer.readLine();
			if(count == null || !Pattern.matches("^[0-9]+$", count)) {
				buffer.close();
				fileReader.close();
				System.out.println("file invalid1");
				return;
			}
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
		} catch (IOException ex) {
			System.out.println("file writer error");
			return;
		}
		
		for(int i = 0; i < dataCount; i++) {
			try {
				String inputCount = buffer.readLine();
				if(inputCount == null || !Pattern.matches("^[0-9]+$", inputCount)) {
					buffer.close();
					System.out.println("file invalid2");
					return;
				}
				String inputData = buffer.readLine();
				if(inputData == null || !Pattern.matches("^[0-9 ]+$", inputData)) {
					buffer.close();
					System.out.println("file invalid3");
					return;
				}
				Solver solver = new Solver(Integer.valueOf(inputCount));
				int calcCount = solver.Solve(inputData.split(" "));
				writer.println(calcCount);
			} catch(IOException ex) {
				System.out.println("file read error");
				return;
			}
		}
		
		try {
			buffer.close();
			writer.close();
			bw.close();
			fileWriter.close();
		} catch(IOException ex) {
		}
		System.out.println("End");
	}
	
	private static class Solver {
		public int paramCount = 0;
		
		public Solver(int paramCount) {
			this.paramCount = paramCount;
		}
		
		public int Solve(String[] params) {
			int count = 0;
			int[] original = new int[paramCount];
			for(int i = 0; i < paramCount; i++) {
				original[i] = Integer.valueOf(params[i]);
			}
			
			ArrayList<int[]> current = new ArrayList<int[]>();
			ArrayList<int[]> next = new ArrayList<int[]>();
			next.add(original);
			for(;;) {
				count++;
				current.clear();
				int size = next.size();
				for(int i = 0; i < size; i++) {
					current.add(next.get(i));
				}
				next.clear();
				for(int i = 0; i < size; i++) {
					int[] parent = current.get(i);
					if(CanRemove(parent)) {
						int[] check = Remove(parent);
						if(IsEnd(check)) return count;
						next.add(Remove(parent));
					}
					next.add(Half(parent));
				}
				
			}
		}
		
		private boolean CanRemove(int[] params) {
			for(int i = 0; i < paramCount; i++) {
				if(params[i] % 5 == 0) return true;
			}
			return false;
		}
		
		private boolean IsEnd(int[] params) {
			for(int i = 0; i < paramCount; i++) {
				if(params[i] != -1) return false;
			}
			return true;
		}
		
		private int[] Half(int[] params) {
			int[] ret = new int[paramCount];
			for(int i = 0; i < paramCount; i++) {
				if(params[i] == -1) {
					ret[i] = -1;
				} else {
					ret[i] = (int)Math.floor(params[i] / 2);
				}
			}
			
			return ret;
		}
		
		private int[] Remove(int[] params) {
			int[] ret = new int[paramCount];
			for(int i = 0; i < paramCount; i++) {
				if(params[i] % 5 == 0) {
					ret[i] = -1;
				} else {
					ret[i] = params[i];
				}
			}
			return ret;
		}
	}

}
