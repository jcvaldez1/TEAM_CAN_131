/*To run this program:
	javac PvmmCurveFitter.java
	java PvmmCurveFitter <file_name.csv>
*/

import java.util.ArrayList;
import Jama.*;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormat;

public class PvmmCurveFitter{
	CsvToMatrixParser ctmp;
	ArrayList<String[]> parseList;


	Matrix previousPriceVector;
	Matrix previousVolumeVector;
	Matrix momentum22Vector;
	Matrix momentum7Vector;

	Matrix finalMatrixA;
	Matrix closingPriceVectorB;
	Matrix finalVectorX;

	public static void main(String args[]){
		PvmmCurveFitter pcf = new PvmmCurveFitter();
		pcf.theRealMain(args);
	}

	public void theRealMain(String args[]){
		ctmp = new CsvToMatrixParser();
		//parseList = ctmp.parseCsvToList("ap_data.csv");
		try{
			parseList = ctmp.parseCsvToList(args[0]);
		}catch(ArrayIndexOutOfBoundsException e){
			System.out.println("No arguments given; Must run program like this: java PvmmCurveFitter <file_name.csv>");
			System.exit(1);
		}
		initVectorsAndMatrices();
		//printFinalMatrixA();
		//finalVectorX = finalMatrixA.solve(closingPriceVectorB);
		//finalVectorX.print(15, 12);//print final answer with minimum 15 digits, and 12 digits after decimal point
		printFinalAnswerX();
	}

	//initializes the matrix and vectos needed for Ax = b
	public void initVectorsAndMatrices(){
		closingPriceVectorB = getClosingPrice();
		previousPriceVector = getPreviousPrice();
		previousVolumeVector = getPreviousVolume();
		momentum22Vector = getMomentum(22);
		momentum7Vector = getMomentum(7);

		finalMatrixA = concatLR(previousPriceVector, previousVolumeVector);
		finalMatrixA = concatLR(finalMatrixA, momentum22Vector);
		finalMatrixA = concatLR(finalMatrixA, momentum7Vector);
	}

	//prints out final answer from vectorX
	public void printFinalAnswerX(){
		finalVectorX = finalMatrixA.solve(closingPriceVectorB);
		double pPrev = finalVectorX.get(0, 0);
		double vPrev = finalVectorX.get(1, 0);
		double mom22 = finalVectorX.get(2, 0);
		double mom7 = finalVectorX.get(3, 0);
		System.out.printf(
			"Pnow =\n\t%14.12f  Pprev\n"
			+ "  +\t%14.12f  Vprev\n"
			+ "  +\t%14.12f  mom22\n"
			+"  +\t%14.12f  mom7\n", pPrev, vPrev, mom22, mom7
		);
	}

	//prints out Matrix A in tabular format, for debugging
	public void printFinalMatrixA(){
		System.out.println("Pnow\tPprev\tVprev\t\tmom22\tmom7");
		for(int i = 0; i < finalMatrixA.getRowDimension(); i++){
			double pPrev = finalMatrixA.get(i, 0);
			double vPrev = finalMatrixA.get(i, 1);
			double mom22 = finalMatrixA.get(i, 2);
			double mom7 = finalMatrixA.get(i, 3);
			System.out.printf("%.2f\t%9.0f\t%.4f\t%.4f\n", pPrev, vPrev, mom22, mom7);
		}
	}

	//concats matrix a with matrix b on right, like: c = [a, b] in matlab
	public Matrix concatLR(Matrix a, Matrix b){
		if(a.getRowDimension() != b.getRowDimension()){
			System.out.println("matrix rows don't match");
			return null;
		}
		int rows = a.getRowDimension();
		int columns = a.getColumnDimension() + b.getColumnDimension();
		Matrix merge = new Matrix(rows, columns);
		merge.setMatrix(0, rows-1, 0, a.getColumnDimension()-1, a);
		merge.setMatrix(0, rows-1, a.getColumnDimension(), columns-1, b);
		return merge;
	}

	//returns closing price vector, requires parseList initalized first
	public Matrix getClosingPrice(){
		int colIndex[] = {0};
		return ctmp.parseListToMatrix(parseList, colIndex);
	}

	//returns previous price vector, requires parseList initalized first
	public Matrix getPreviousPrice(){
		int colIndex[] = {11};
		return ctmp.parseListToMatrix(parseList, colIndex);
	}

	//returns previous volume vector, requires parseList initalized first
	public Matrix getPreviousVolume(){
		int colIndex[] = {5};
		Matrix origV = ctmp.parseListToMatrix(parseList, colIndex);
		int numRows = origV.getRowDimension();
		Matrix prevV = new Matrix(numRows, 1);
		prevV.setMatrix(1, numRows-1, 0, 0, origV.getMatrix(0, numRows-2, 0, 0));
		return prevV;
	}

	//returns momentum vector at specificed delta, requires parseList and closingPriceVectorB to be initalized first
	public Matrix getMomentum(int delta){
		Matrix momTum = new Matrix(parseList.size(), 1);
		DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy");
		int flag;//will be 0 if central, 1 if forward, and -1 if backward

		for(int i = 0; i < momTum.getRowDimension(); i++){//giant loop through all i values of momTum matrix
			DateTime dNow = dtf.parseDateTime(parseList.get(i)[4]);
			flag = 0;

			int foreI = i, foreDelta = 0;
			for(int j = i+1; j < momTum.getRowDimension(); j++){//check forward days j until end
				DateTime dFore = dtf.parseDateTime(parseList.get(j)[4]);
				int dayDiff = Days.daysBetween(dNow, dFore).getDays();
				if(dayDiff >= delta){//when checking has reached far enough
					foreI = j;
					foreDelta = dayDiff;
					flag++;
					break;
				}
			}

			int backI = i, backDelta = 0;
			for(int j = i-1; j >= 0; j--){//check backward days j until start
				DateTime dBack = dtf.parseDateTime(parseList.get(j)[4]);
				int dayDiff = Days.daysBetween(dBack, dNow).getDays();
				if(dayDiff >= delta){//when checking has reached far enough
					backI = j;
					backDelta = dayDiff;
					flag--;
					break;
				}
			}

			double priceNow = 	closingPriceVectorB.get(i, 0);
			if(flag == 0){//central difference
				double priceFore = closingPriceVectorB.get(foreI, 0);
				double priceBack = closingPriceVectorB.get(backI, 0);
				double centralDiff = (priceFore - priceBack)/(foreDelta + backDelta);
				momTum.set(i, 0, centralDiff);
			}else if(flag > 0){//forward difference
				double priceFore = closingPriceVectorB.get(foreI, 0);
				double forwardDiff = (priceFore - priceNow)/(foreDelta);
				momTum.set(i, 0, forwardDiff);
			}else{//backward difference
				double priceBack = closingPriceVectorB.get(backI, 0);
				double backwardDiff = (priceNow - priceBack)/(backDelta);
				momTum.set(i, 0, backwardDiff);
			}
		}//end of huge for loop with index i

		return momTum;
	}

	//testing and debugging purposes
	void sandboxTesting(){
		//		How to use datetime formatter:
		DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy");
		DateTime dt1 = dtf.parseDateTime("05/25/2018");
		DateTime dt2 = dtf.parseDateTime("05/26/2017");
		System.out.println("Days between: " + Days.daysBetween(dt2, dt1).getDays());
	}
}
