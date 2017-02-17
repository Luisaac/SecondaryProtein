import java.util.*;
import java.io.*;


public class Lab2 {
	
	
	static ArrayList<String> content = null;
	static HashMap<Character, Integer> map = null;
	static double bias = -1;
	static double[][] table = new double[17][21];
	static double[] huOutput = new double[3];
	static double[] output = new double[3];
	// weight from input to hidden units
	static double[][] inToHu = new double[3][358];
	// weight from hidden units to output
	static double[][] huToOut = new double[3][4];
	static ArrayList<int[]> teacher = null;
	// for h,e,_
	static HashMap<Character, Integer> l = null;
	static ArrayList<int[]> prediction = new ArrayList<int[]>();
	static ArrayList<int[]> teacherForTest = null;
	static ArrayList<int[]> predictionForTest = null;
	static ArrayList<String> test = null;
	
	public static void main(String[] args){
		setup();
		initWeight();
		input(args[0]);
		int epoch = 0;
		while(epoch < 100){
			// every str is a protein
			run(content,prediction, false);
			//testTrain();
			//test
			run(test,predictionForTest, true);
			double count = 0;
			double correct = 0;
			for(int i = 0 ; i < predictionForTest.size(); i++){
				for(int j = 0; j < predictionForTest.get(i).length; j++){
					if(predictionForTest.get(i)[j]==teacherForTest.get(i)[j]) correct++;
					count++;
				}
			}
			if(epoch==0||epoch==99) System.out.println(correct/count);
//			for(int i = 0; i < 3;i++){
//				for(int j = 0; j < 4;j++){
//					System.out.println(huToOut[i][j]);
//				}
//			}
//			System.out.println();
			epoch++;
		}
	}
	public static void run(ArrayList<String> content, ArrayList<int[]> prediction, boolean testFlag){
		for(int l = 0; l < content.size(); l++){
			String str = content.get(l);
			String[] amino = str.split("-");
			for(int i = 0; i < amino.length; i++){
				int offset1 = i-8;
				int offset2 = i+8;
				// out of edge
				if(offset1<0&&offset2>=amino.length){
					for(int j = 0; j < Math.abs(offset1); j++){
						table[j][20] = 1;
					}
					for(int j = Math.abs(offset1); j < 17-(offset2-amino.length+1); j++){
						table[j][map.get(amino[j-Math.abs(offset1)].charAt(0))] = 1; 
					}
					for(int j = 17-(offset2-amino.length+1); j<17; j++){
						table[j][20] = 1;
					}
				}
				else if(offset1<0){
					int num = Math.abs(offset1);
					for(int j = 0; j < num; j++){
						table[j][20] = 1;
					}
					for(int j = num; j < 17; j++){
						table[j][map.get(amino[j-num].charAt(0))] = 1; 
					}
				}
				else if(offset2>=amino.length){
					int num = offset2-amino.length+1;
					for(int j = 0; j < 17-num; j++){
						table[j][map.get(amino[i-8+j].charAt(0))] = 1;
					}
					for(int j = 17-num; j < 17; j++){
						table[j][20] = 1;
					}
				}
				else{
					for(int j = 0; j < 17; j++){
						table[j][map.get(amino[i-8+j].charAt(0))] = 1;
					}
				}


				prediction.get(l)[i] = 	forward(l,i);
			//	forward(l,i);
				if(!testFlag) backward(l,i);




				for(int k = 0; k < 17; k++){
					for(int j = 0; j < 21; j++){
						table[k][j] = 0;
					}
				}
			}
		}
	}
	

//	public static void testSet(String file){
//		Scanner in = null;
//		try{
//			in = new Scanner(new File(file));
//		}
//		catch(FileNotFoundException e){
//			System.out.println("Cannot find file" + file);
//			System.exit(1);
//		}
//
//		// input every line to ArrayList
//		content = new ArrayList<String>();
//		String element = "";
//		while(in.hasNext()){
//			String line = in.nextLine();
//			if(line.length()==0) continue;
//			if(line.charAt(0)=='#') continue;
//			if(line.startsWith("<")||line.startsWith("end")){
//				if(element.length()!=0) content.add(element);
//				element = "";
//			}
//			else{
//				element+=(line+"-");
//			}
//		}
//		ArrayList<int[]> golden = new ArrayList<>();
//		ArrayList<int []> prediction2 = new ArrayList<>();
//		for(String str:content){
//			String[] amino = str.split("-");
//			int[] label = new int[amino.length];
//			for(int i = 0; i < amino.length; i++){
//				label[i] = l.get(amino[i].charAt(2));
//			}
//			golden.add(label);
//			prediction2.add(new int[amino.length]);
//		}
//
//
//
//	}

	public static void testTrain(){
		double correct = 0;
		double count = 0;
		for(int i = 0; i < teacher.size();i++){
			for(int j = 0; j < teacher.get(i).length;j++){
				count++;
				if(teacher.get(i)[j] == prediction.get(i)[j]){
					correct++;
				}
			}
		}
		System.out.println(correct/count);
	}
	//public static double 
	public static void backward(int protein, int amino){
		double deltaI = 0;
		for(int i = 0; i < 3; i++){
			deltaI = output[i]*(1-output[i])*(teacher.get(protein)[amino]-output[i]);
			double deltaJ = 0;
			for(int j = 0; j < 3; j++){
				if(huOutput[j]>0){
					double sum = 0;
					for(int num = 0; num < 3; num++){
						sum += huToOut[num][j];
					}
					deltaJ = deltaI*sum;
					// update weight ij
					huToOut[i][j] += 0.25*deltaI*huOutput[j];
				}
				for(int k = 0; k < 357; k++){
					int row = k/21;
					int col = k%21;
					inToHu[j][k] += 0.25*deltaJ*table[row][col];
				}
				//update bias
				inToHu[j][357] += 0.25*deltaJ*(-1);
			}
			// update bias
			huToOut[i][3] += 0.25*deltaI*(-1);
		}
		
	}
	
	
	public static int forward(int protein, int amino){
		// from input to hu
		double output_hu = 0;
		for(int k = 0; k < 3; k++){
			for(int i = 0; i < table.length; i++){
				for(int j = 0; j < table[0].length; j++){
					output_hu += table[i][j]*inToHu[k][i*21+j];
				}
			}
			output_hu += -1*inToHu[k][357];
			huOutput[k] = rectify(output_hu);
		}
		
		// from hu to output
		double output_out = 0;
		for(int k = 0; k < 3; k++){
			for(int i = 0; i < 3; i++){
				output_out += huOutput[i]*huToOut[k][i];
			}
			output_out += -1*huToOut[k][3];
			output[k] = sigmoid(output_out);
		}
		int out = -1;
		if(output[0]>=output[1] && output[0]>= output[2]) out = 0;
		else if (output[1]>=output[0] && output[1]>= output[2]) out = 1;
		else out = 2;
		
		//prediction.get(protein)[amino] = out;
		return out;
	}
	
	public static void initWeight(){
		for(int i = 0; i < inToHu.length; i++){
			for(int j = 0; j < inToHu[0].length; j++){
				inToHu[i][j] = -0.3+Math.random()*0.6;
			}
		}
		for(int i = 0; i < huToOut.length; i++){
			for(int j = 0; j < huToOut[0].length; j++){
				huToOut[i][j] = -0.3+Math.random()*0.6;
			}
		}
	}
	
	public static void setup(){
		map = new HashMap<Character, Integer>();
		map.put('A', 0);
		map.put('R', 1);
		map.put('N', 2);
		map.put('D', 3);
		map.put('C', 4);
		map.put('E', 5);
		map.put('Q', 6);
		map.put('G', 7);
		map.put('H', 8);
		map.put('I', 9);
		map.put('L', 10);
		map.put('K', 11);
		map.put('M', 12);
		map.put('F', 13);
		map.put('P', 14);
		map.put('S', 15);
		map.put('T', 16);
		map.put('W', 17);
		map.put('Y', 18);
		map.put('V', 19);
		
		l = new HashMap<Character, Integer>();
		l.put('h',0);
		l.put('e',1);
		l.put('_', 2);
		
		teacher = new ArrayList<int[]>();
	}
	
	
	public static void input(String file){
		Scanner in = null;
		try{
			in = new Scanner(new File(file));
		}
		catch(FileNotFoundException e){
			System.out.println("Cannot find file " + file);
			System.exit(1);
		}
		int count = 1;
		// input every line to ArrayList
		content = new ArrayList<String>();
		String element = "";
		test = new ArrayList<String>();
		teacherForTest = new ArrayList<int[]>();
		predictionForTest = new ArrayList<int[]>();
		while(in.hasNext()){
			String line = in.nextLine();
			if(line.length()==0) continue;
			if(line.charAt(0)=='#') continue;
			if(line.startsWith("<")||line.startsWith("end")){
				if(element.length()!=0){
					if(count%5==0){
						
					}
					else if((count-1)%5 == 0){
						test.add(element);
					}
					else content.add(element);
					count++;
				}
				element = "";
			}
			else{
				element+=(line+"-");
			}
		}
		
		for(String str:content){
			String[] amino = str.split("-");
			int[] label = new int[amino.length];
			for(int i = 0; i < amino.length; i++){
				label[i] = l.get(amino[i].charAt(2));
			}
			teacher.add(label);
			prediction.add(new int[amino.length]);
		}
		for(String str:test){
			String[] amino = str.split("-");
			int[] label = new int[amino.length];
			for(int i = 0; i < amino.length; i++){
				label[i] = l.get(amino[i].charAt(2));
			}
			teacherForTest.add(label);
			predictionForTest.add(new int[amino.length]);
		}
		
	}
	
	
	public static double rectify(double in){
		return Math.max(0, in);
	}
	
	
	public static double sigmoid(double in){
		return 1/(1+Math.pow(Math.E, (in*-1)));
	}
	
}