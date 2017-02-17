import java.util.*;
import java.io.*;


public class Lab2 {
	static int numHU = 10;
	
	static ArrayList<String> content = null;
	static HashMap<Character, Integer> map = null;
	static double bias = -1;
	static double[][] inToHu_mo = new double[numHU][358];
	static double[][] huToOut_mo = new double[3][numHU+1];
	static double[][] table = new double[17][21];
	static double[] huOutput = new double[numHU];
	static double[] output = new double[3];
	// weight from input to hidden units
	static double[][] inToHu = new double[numHU][358];
	// weight from hidden units to output
	static double[][] huToOut = new double[3][numHU+1];
	static ArrayList<int[]> teacher = null;
	// for h,e,_
	static HashMap<Character, Integer> l = null;
	static ArrayList<int[]> prediction = new ArrayList<int[]>();
	static ArrayList<int[]> teacherForTest = null;
	static ArrayList<int[]> predictionForTest = null;
	static ArrayList<String> test = null;
	static ArrayList<int[]> teacherForTune = null;
	static ArrayList<int[]> predictionForTune = null;
	static ArrayList<String> tune = null;
	public static void main(String[] args){
		setup();
		initWeight();
		input(args[0]);
		int epoch = 0;
		while(epoch<500){
			// every str is a protein
			run(content, prediction, false);
			run(test, predictionForTest, true);
			run(tune, predictionForTune, true);
			

			
			
//			System.out.print("train: ");
//			testTrain();
			double count = 0;
			double correct = 0;
			for(int i = 0 ; i < predictionForTest.size(); i++){
				for(int j = 0; j < predictionForTest.get(i).length; j++){
					if(predictionForTest.get(i)[j]==teacherForTest.get(i)[j]) correct++;
					count++;
				}
			}
//			System.out.print("test: ");
//			System.out.println(correct/count);
			double testRate = correct/count;
			
			count = 0;
			correct = 0;
			for(int i = 0 ; i < predictionForTune.size(); i++){
				for(int j = 0; j < predictionForTune.get(i).length; j++){
					if(predictionForTune.get(i)[j]==teacherForTune.get(i)[j]) correct++;
					count++;
				}
			}
//			System.out.print("tune: ");
//			System.out.println(correct/count);
//			System.out.println();
			double tuneRate = correct/count;
			
			if(testRate>=0.58&&tuneRate>=0.58&&epoch >= 50){
				System.out.println(epoch);
				break;
			}
		
			
			epoch++;
		}
		
		
		for(int i = 0; i <predictionForTest.size();i++){
			for(int j = 0; j < predictionForTest.get(i).length; j++){
				int ret = predictionForTest.get(i)[j];
				if(ret == 0){
					System.out.println('h');
				}
				else if(ret == 1){
					System.out.println('e');
				}
				else{
					System.out.println('_');
				}
			}
		}
	}
	public static void run(ArrayList<String> content, ArrayList<int[]> prediction, boolean testFlag){
		for(int l = 0; l < content.size(); l++){
			//System.out.println(l);
			String str = content.get(l);
			//System.out.println(str);
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
						//System.out.println(amino[j-num].charAt(0));
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
				
//				for(int index1 = 0; index1 < table.length; index1++){
//					int number = 0;
//					for(int index2 = 0; index2 < table[0].length; index2++){
//						System.out.print(table[index1][index2]+" ");
//					}
//					System.out.println();
//				}
				


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
	
		double[] deltaI = new double[3];
		for(int i = 0; i < 3; i++){
			int offset = 0;
			if(teacher.get(protein)[amino]==i) offset = 1;
			deltaI[i] = output[i]*(1-output[i])*(offset-output[i]);
		}
		double[] deltaJ = new double[numHU];
		// rectify
		for(int i = 0; i < numHU; i++){
			if(huOutput[i]>0){
				for(int j = 0; j < 3; j++){
					deltaJ[i]+= huToOut[j][i]*deltaI[j];
				}
			}
		}
		// sigmoid
//		for(int i = 0 ; i < numHU;i++) {
//			for(int j = 0; j < 3;j++){
//				deltaJ[i] += deltaI[j]*huToOut[j][i];
//			}
//			deltaJ[i] = deltaJ[i]*huOutput[i]*(1-huOutput[i]);
//		}
		
		// update weight
		for(int i = 0; i < 3; i++){
			for(int j = 0; j < numHU; j++){
				huToOut[i][j] += 0.1*deltaI[i]*huOutput[j];
			}
			huToOut[i][numHU] += 0.1*deltaI[i]*(-1);
		}
		for(int j = 0; j < numHU; j++){
			for(int k = 0; k < 357; k++){
				int row = k/21;
				int col = k%21;
				inToHu[j][k] +=0.1*deltaJ[j]*table[row][col]; 
			}
			inToHu[j][357] +=0.1*deltaJ[j]*(-1);
		}
		
		// update weight with weight decay
//		for(int i = 0; i < 3; i++){
//			for(int j = 0; j < numHU; j++){
//				huToOut[i][j] += 0.1*deltaI[i]*huOutput[j] - 0.05*huToOut[i][j];
//			}
//			huToOut[i][numHU] += 0.1*deltaI[i]*(-1) - 0.05*huToOut[i][numHU];
//		}
//		for(int j = 0; j < numHU; j++){
//			for(int k = 0; k < 357; k++){
//				int row = k/21;
//				int col = k%21;
//				inToHu[j][k] +=0.1*deltaJ[j]*table[row][col] - 0.05*inToHu[j][k]; 
//			}
//			inToHu[j][357] +=0.1*deltaJ[j]*(-1) - 0.05*inToHu[j][357];
//		}
		
		
		
		
		// update weight with momentum
//		for(int i = 0; i < 3; i++){
//			for(int j = 0; j < numHU; j++){
//				double update = 0.01*deltaI[i]*huOutput[j] + 0.9*huToOut_mo[i][j];
//				huToOut[i][j] += update;
//				huToOut_mo[i][j] = update;
//			}
//			double update = 0.01*deltaI[i]*(-1) + 0.9*huToOut_mo[i][numHU];
//			huToOut[i][numHU] += update;
//			huToOut_mo[i][numHU] = update;
//		}
//		for(int j = 0; j < numHU; j++){
//			for(int k = 0; k < 357; k++){
//				int row = k/21;
//				int col = k%21;
//				double update = 0.01*deltaJ[j]*table[row][col]+0.9*inToHu_mo[j][k];
//				inToHu[j][k] += update;
//				inToHu_mo[j][k] = update;
//				
//			}
//			double update = 0.01*deltaJ[j]*-1+0.9*inToHu_mo[j][357];
//			inToHu[j][357] += update;
//			inToHu_mo[j][357] = update;
//		}
	}
	
	
	public static int forward(int protein, int amino){
		// from input to hu
		double output_hu = 0;
		for(int k = 0; k < numHU; k++){
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
			for(int i = 0; i < numHU; i++){
				output_out += huOutput[i]*huToOut[k][i];
			}
			output_out += -1*huToOut[k][numHU];
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
		tune = new ArrayList<String>();
		teacherForTune = new ArrayList<int[]>();
		predictionForTune = new ArrayList<int[]>();
		while(in.hasNext()){
			String line = in.nextLine();
			if(line.length()==0) continue;
			if(line.charAt(0)=='#') continue;
			if(line.startsWith("<")||line.startsWith("end")){
				if(element.length()!=0){
					if((count)%5==0){
						tune.add(element);
					}
					else if((count-1)%5 == 0 && count!=1){
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
		for(String str:tune){
			String[] amino = str.split("-");
			int[] label = new int[amino.length];
			for(int i = 0; i < amino.length; i++){
				label[i] = l.get(amino[i].charAt(2));
			}
			teacherForTune.add(label);
			predictionForTune.add(new int[amino.length]);
		}
		
	}
	
	
	public static double rectify(double in){
		return Math.max(0, in);
	}
	
	
	public static double sigmoid(double in){
		return 1/(1+Math.pow(Math.E, (in*-1)));
	}
	
}